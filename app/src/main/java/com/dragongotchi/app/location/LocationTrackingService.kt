package com.dragongotchi.app.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.dragongotchi.app.DragonGotchiApp
import com.dragongotchi.app.MainActivity
import com.dragongotchi.app.R
import com.dragongotchi.app.data.local.RoutePointEntity
import com.dragongotchi.app.data.preferences.AppLanguage
import com.dragongotchi.app.data.preferences.localizedContext
import com.dragongotchi.app.data.local.TrainingSessionEntity
import com.dragongotchi.app.domain.ActivityFilter
import com.dragongotchi.app.domain.ActivityType
import com.dragongotchi.app.domain.GeoPoint
import com.dragongotchi.app.domain.StatsCalculator
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ================================================================
 *  SERVICIO DE TRACKING GPS (Foreground Service)
 * ================================================================
 * - Vive FUERA de Compose y de la Activity: sigue grabando con la
 *   app minimizada, la pantalla apagada o el movil bloqueado.
 * - La notificacion persistente es obligatoria en Android para
 *   este tipo de servicio: es lo que impide que el sistema mate
 *   el proceso, y ademas muestra distancia/tiempo en vivo.
 * - Aplica GpsFilter a CADA punto antes de sumar distancia.
 * - Cuenta pasos con el podometro hardware (TYPE_STEP_COUNTER),
 *   que funciona en segundo plano y apenas gasta bateria.
 * - Al finalizar, guarda sesion + ruta en Room y concede la EXP.
 */
class LocationTrackingService : Service(), SensorEventListener {

    companion object {
        const val ACTION_START = "com.dragongotchi.app.action.START"
        const val ACTION_PAUSE = "com.dragongotchi.app.action.PAUSE"
        const val ACTION_RESUME = "com.dragongotchi.app.action.RESUME"
        const val ACTION_STOP = "com.dragongotchi.app.action.STOP"
        const val EXTRA_ACTIVITY_TYPE = "extra_activity_type"
        const val EXTRA_DRAGON_LEVEL = "extra_dragon_level"
        const val EXTRA_DRAGON_PET_ID = "extra_dragon_pet_id"
        const val EXTRA_DRAGON_SHINY = "extra_dragon_shiny"
        const val EXTRA_DRAGON_AURA = "extra_dragon_aura"
        private const val CHANNEL_ID = "training_channel"
        private const val NOTIFICATION_ID = 1001

        /**
         * La apariencia del dragon (nivel, mascota elegida, shiny y aura) se
         * captura en el momento de pulsar Iniciar y se congela para toda la
         * sesion: es el sprite que se mueve como marcador en el mapa.
         */
        fun start(
            context: Context,
            type: ActivityType,
            dragonLevel: Int = 1,
            dragonPetId: Int = 1,
            dragonShiny: Boolean = false,
            dragonAura: Boolean = false
        ) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ACTIVITY_TYPE, type.name)
                putExtra(EXTRA_DRAGON_LEVEL, dragonLevel.coerceIn(1, 15))
                putExtra(EXTRA_DRAGON_PET_ID, dragonPetId.coerceIn(1, 3))
                putExtra(EXTRA_DRAGON_SHINY, dragonShiny)
                putExtra(EXTRA_DRAGON_AURA, dragonAura)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun sendAction(context: Context, action: String) {
            val intent = Intent(context, LocationTrackingService::class.java).setAction(action)
            context.startService(intent)
        }
    }

    private val app get() = application as DragonGotchiApp
    private val trackingManager get() = app.container.trackingManager
    private val sessionRepository get() = app.container.sessionRepository
    private val petRepository get() = app.container.petRepository
    private val rewardsRepository get() = app.container.rewardsRepository
    private val userPreferences get() = app.container.userPreferences

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    private val gpsFilter = GpsFilter()
    private val routePoints = mutableListOf<GeoPoint>()

    private var timerJob: Job? = null
    private var startedAtElapsedRealtime = 0L
    private var accumulatedElapsedMs = 0L
    private var lastCounterSteps: Float? = null
    private var sessionSteps = 0
    private var stepsBeforePause = 0
    private var activityType: ActivityType = ActivityType.RUN
    private var dragonLevelAtStart: Int = 1
    private var dragonPetIdAtStart: Int = 1
    private var dragonShinyAtStart: Boolean = false
    private var dragonAuraAtStart: Boolean = false
    private var sessionStartWallTime = 0L
    private var wakeLock: PowerManager.WakeLock? = null

    /** Idioma elegido en Ajustes: las notificaciones y errores del
     * servicio se muestran en ese idioma (no en el del sistema). */
    @Volatile
    private var uiLanguage = AppLanguage.ES

    /** getString() resuelto en el idioma de la app. */
    private fun locStr(resId: Int, vararg args: Any): String =
        localizedContext(uiLanguage).getString(resId, *args)

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            userPreferences.appLanguage.collect { uiLanguage = it }
        }
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                activityType = runCatching {
                    ActivityType.valueOf(intent.getStringExtra(EXTRA_ACTIVITY_TYPE) ?: "RUN")
                }.getOrDefault(ActivityType.RUN)
                dragonLevelAtStart = (intent?.getIntExtra(EXTRA_DRAGON_LEVEL, 1) ?: 1).coerceIn(1, 15)
                dragonPetIdAtStart = (intent?.getIntExtra(EXTRA_DRAGON_PET_ID, 1) ?: 1).coerceIn(1, 3)
                dragonShinyAtStart = intent?.getBooleanExtra(EXTRA_DRAGON_SHINY, false) ?: false
                dragonAuraAtStart = intent?.getBooleanExtra(EXTRA_DRAGON_AURA, false) ?: false
                startTracking()
            }
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP -> stopTrackingAndSave()
        }
        // START_STICKY: si el sistema mata el servicio, lo recrea.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---------------------------------------------------------------
    // Inicio / pausa / reanudar / fin
    // ---------------------------------------------------------------
    private fun startTracking() {
        if (trackingManager.state.value.status != TrackingStatus.IDLE) return
        // PURGA TOTAL al iniciar una actividad NUEVA: ningun vestigio GPS
        // de la sesion anterior debe sobrevivir (punto congelado en el
        // ultimo tick, ruta vieja, camara...). Se corta cualquier flujo
        // de ubicaciones previo y el estado publicado vuelve a cero
        // antes de crear el estado RUNNING limpio de abajo.
        runCatching { fusedClient.removeLocationUpdates(locationCallback) }
        trackingManager.reset()
        gpsFilter.reset()
        routePoints.clear()
        sessionSteps = 0; stepsBeforePause = 0; lastCounterSteps = null
        accumulatedElapsedMs = 0L
        sessionStartWallTime = System.currentTimeMillis()
        startedAtElapsedRealtime = android.os.SystemClock.elapsedRealtime()

        trackingManager.update {
            LiveTrackingState(
                status = TrackingStatus.RUNNING,
                activityType = activityType,
                dragonLevelAtStart = dragonLevelAtStart,
                dragonPetIdAtStart = dragonPetIdAtStart,
                dragonShinyAtStart = dragonShinyAtStart,
                dragonAuraAtStart = dragonAuraAtStart,
                startTimeMillis = sessionStartWallTime
            )
        }

        startForegroundWithNotification(locStr(R.string.svc_preparing_gps))
        acquireWakeLock()
        startLocationUpdates()
        startStepSensor()
        startTimer()
    }

    private fun pauseTracking() {
        if (trackingManager.state.value.status != TrackingStatus.RUNNING) return
        accumulatedElapsedMs = currentElapsedMs()
        stepsBeforePause = sessionSteps
        lastCounterSteps = null
        fusedClient.removeLocationUpdates(locationCallback)
        sensorManager.unregisterListener(this)
        timerJob?.cancel()
        trackingManager.update { it.copy(status = TrackingStatus.PAUSED) }
        startForegroundWithNotification(locStr(R.string.svc_paused))
    }

    private fun resumeTracking() {
        if (trackingManager.state.value.status != TrackingStatus.PAUSED) return
        startedAtElapsedRealtime = android.os.SystemClock.elapsedRealtime()
        trackingManager.update { it.copy(status = TrackingStatus.RUNNING) }
        startLocationUpdates()
        startStepSensor()
        startTimer()
    }

    private fun stopTrackingAndSave() {
        fusedClient.removeLocationUpdates(locationCallback)
        sensorManager.unregisterListener(this)
        timerJob?.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }

        val state = trackingManager.state.value
        val finalSteps: Int
        val estimated: Boolean
        if (stepSensor != null) {
            finalSteps = sessionSteps; estimated = false
        } else {
            // Sin podometro hardware: estimamos pasos por distancia.
            finalSteps = StatsCalculator.estimateSteps(state.distanceM); estimated = true
        }

        serviceScope.launch {
            val weightKg = userPreferences.weightKg.first().toDouble()
            val kcal = StatsCalculator.caloriesBurned(weightKg, state.elapsedSec, state.distanceM, state.activityType)

            // ==========================================================
            // FILTRO DE ACTIVIDAD VALIDA: >=500 pasos Y >=250 segundos.
            // Si no se cumple AMBAS: la actividad NO cuenta. No se guarda
            // en el historial, no concede EXP y NO hay tirada de dados.
            // ==========================================================
            val isValidActivity = ActivityFilter.isValid(finalSteps, state.elapsedSec)
            var savedId: Long? = null
            var expGained = 0
            var levelsGained = 0
            var rollValue: Int? = null
            var unlockedRewardName: String? = null
            if (isValidActivity) {
                // La EXP sale de los PASOS (hardware), no del GPS: anti-trampas por diseno.
                // Se concede ANTES de guardar para que la sesion refleje la EXP real ganada
                // (incluye los pasos sobrantes de sesiones anteriores: resto 0-99).
                val reward = petRepository.grantSteps(finalSteps, state.distanceM)
                expGained = reward.expGained
                levelsGained = reward.levelsGained

                val entity = TrainingSessionEntity(
                    startTimeMillis = sessionStartWallTime,
                    endTimeMillis = System.currentTimeMillis(),
                    durationSec = state.elapsedSec,
                    distanceM = state.distanceM,
                    steps = finalSteps,
                    stepsAreEstimated = estimated,
                    kcal = kcal,
                    expGained = expGained,
                    avgPaceSecPerKm = StatsCalculator.paceSecPerKm(state.distanceM, state.elapsedSec),
                    maxSpeedKmh = state.maxSpeedKmh,
                    activityType = state.activityType.name,
                    acceptedGpsPoints = gpsFilter.acceptedCount,
                    rejectedGpsPoints = gpsFilter.rejectedCount,
                    isFlaggedSuspicious = gpsFilter.isSessionSuspicious
                )
                val points = routePoints.mapIndexed { index, p ->
                    RoutePointEntity(
                        sessionId = 0,
                        latitude = p.latitude,
                        longitude = p.longitude,
                        altitude = p.altitude,
                        timeMillis = p.timeMillis,
                        speedMps = p.speedMps,
                        accuracyM = p.accuracyM,
                        sequence = index
                    )
                }
                savedId = sessionRepository.saveSession(entity, points)

                // ================================================
                // DICE ROLL DE RECOMPENSAS (solo actividad valida)
                // Tirada 1..100 contra intervalos fijos y disjuntos:
                // es imposible que toquen 2 recompensas a la vez,
                // y las ya desbloqueadas tienen su intervalo anulado.
                // ================================================
                val roll = rewardsRepository.performRoll()
                rollValue = roll.value
                unlockedRewardName = roll.unlockedReward?.let { "${it.emoji} ${locStr(it.nameRes)}" }
            }

            trackingManager.update {
                it.copy(
                    status = TrackingStatus.IDLE,
                    hasJustFinished = true,
                    activityWasValid = isValidActivity,
                    rollValue = rollValue,
                    unlockedRewardName = unlockedRewardName,
                    finishedSessionId = savedId,
                    lastExpGained = expGained,
                    lastLevelsGained = levelsGained
                )
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    // ---------------------------------------------------------------
    // GPS
    // ---------------------------------------------------------------
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { location -> onNewLocation(location) }
        }
    }

    private fun onNewLocation(location: Location) {
        val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION") location.isFromMockProvider
        }
        val point = GeoPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = if (location.hasAltitude()) location.altitude else null,
            timeMillis = if (location.time > 0) location.time else System.currentTimeMillis(),
            speedMps = if (location.hasSpeed()) location.speed else null,
            accuracyM = if (location.hasAccuracy()) location.accuracy else null
        )

        var rejectedReason: String? = null
        var pointAccepted = false
        when (val filtered = gpsFilter.evaluate(point, isMock)) {
            is GpsFilter.Result.Accepted -> {
                pointAccepted = true
                if (filtered.distanceDeltaM > 0.0 || routePoints.isEmpty()) {
                    routePoints.add(filtered.point)
                }
            }
            is GpsFilter.Result.Rejected -> {
                rejectedReason = filtered.reason.name
            }
        }

        val current = trackingManager.state.value
        val elapsed = currentElapsedMs() / 1000
        val distance = gpsFilter.totalDistanceM
        // Solo los puntos ACEPTADOS alimentan velocidad actual/maxima:
        // un punto de coche o fake-GPS rechazado nunca contamina las estadisticas.
        val instSpeedKmh = if (pointAccepted) (point.speedMps?.toDouble() ?: 0.0) * 3.6 else 0.0
        val stepsNow = if (stepSensor != null) sessionSteps
                       else StatsCalculator.estimateSteps(distance)

        trackingManager.update {
            it.copy(
                elapsedSec = elapsed,
                distanceM = distance,
                steps = stepsNow,
                stepsAreEstimated = stepSensor == null,
                kcal = 0.0, // se recalcula abajo con el peso del usuario
                currentPaceSecPerKm = StatsCalculator.paceSecPerKm(distance, elapsed),
                currentSpeedKmh = instSpeedKmh,
                maxSpeedKmh = maxOf(it.maxSpeedKmh, instSpeedKmh),
                currentPoint = point,
                gpsAccuracyM = point.accuracyM,
                gpsError = null, // ya hay fix: cualquier error anterior queda resuelto
                routePoints = routePoints.toList(),
                acceptedPoints = gpsFilter.acceptedCount,
                rejectedPoints = gpsFilter.rejectedCount,
                suspiciousPoints = gpsFilter.suspiciousCount,
                lastRejectedReason = rejectedReason ?: it.lastRejectedReason
            )
        }
        recalculateCalories(elapsed, distance)
        updateNotification()
    }

    private fun recalculateCalories(elapsedSec: Long, distanceM: Double) {
        serviceScope.launch {
            val weightKg = userPreferences.weightKg.first().toDouble()
            val kcal = StatsCalculator.caloriesBurned(weightKg, elapsedSec, distanceM, activityType)
            trackingManager.update { if (it.status != TrackingStatus.IDLE) it.copy(kcal = kcal) else it }
        }
    }

    @Suppress("MissingPermission") // Los permisos se piden en la UI antes de iniciar.
    private fun startLocationUpdates() {
        // Si la ubicacion del DISPOSITIVO esta apagada, Fused no entrega
        // puntos nunca y sin aviso: lo detectamos y lo mostramos en la UI
        // en vez de parecer que "no hay senal GPS".
        val locationManager = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
        val anyProviderEnabled = runCatching {
            locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(true)
        if (!anyProviderEnabled) {
            trackingManager.update {
                it.copy(gpsError = locStr(R.string.svc_gps_disabled))
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
            .setMinUpdateIntervalMillis(1_000L)
            .setMinUpdateDistanceMeters(0f) // filtramos nosotros con GpsFilter
            .setWaitForAccurateLocation(false)
            .build()
        try {
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
                .addOnFailureListener { error ->
                    // Antes este fallo se tragaba en silencio (runCatching)
                    // y la app parecia buscar senal eternamente.
                    trackingManager.update {
                        it.copy(gpsError = locStr(R.string.svc_gps_start_failed, error.message ?: locStr(R.string.svc_unknown_error)))
                    }
                }
        } catch (securityError: SecurityException) {
            trackingManager.update {
                it.copy(gpsError = locStr(R.string.svc_gps_no_permission))
            }
        }

        // Primer fix ACTIVO de ESTA sesion. Antes se sembraba con
        // lastLocation (cache del sistema), que tras acabar una actividad
        // devolvia el ultimo punto de la sesion anterior y el dragon se
        // quedaba congelado ahi. Ahora se pide una ubicacion NUEVA y solo
        // se acepta si su hora es posterior al inicio de esta sesion.
        runCatching {
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                if (location != null &&
                    trackingManager.state.value.status == TrackingStatus.RUNNING &&
                    location.time >= sessionStartWallTime - 2_000L
                ) {
                    onNewLocation(location)
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Podometro hardware
    // ---------------------------------------------------------------
    private fun startStepSensor() {
        val sensor = stepSensor ?: return
        lastCounterSteps = null
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return
        // TYPE_STEP_COUNTER = pasos totales desde el ultimo reinicio del movil.
        val totalSinceBoot = event.values.firstOrNull() ?: return
        val baseline = lastCounterSteps
        if (baseline == null) {
            lastCounterSteps = totalSinceBoot
            return
        }
        if (totalSinceBoot < baseline) { // el movil se reinicio en mitad de la sesion
            lastCounterSteps = totalSinceBoot
            return
        }
        sessionSteps = stepsBeforePause + (totalSinceBoot - baseline).toInt()
        trackingManager.update { it.copy(steps = sessionSteps, stepsAreEstimated = false) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    // ---------------------------------------------------------------
    // Temporizador (1 tick / segundo, independiente de la UI)
    // ---------------------------------------------------------------
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                val elapsed = currentElapsedMs() / 1000
                trackingManager.update {
                    if (it.status == TrackingStatus.RUNNING) it.copy(elapsedSec = elapsed) else it
                }
                if (trackingManager.state.value.status == TrackingStatus.RUNNING) updateNotification()
                delay(1_000L)
            }
        }
    }

    private fun currentElapsedMs(): Long {
        val state = trackingManager.state.value
        return if (state.status == TrackingStatus.RUNNING) {
            accumulatedElapsedMs + (android.os.SystemClock.elapsedRealtime() - startedAtElapsedRealtime)
        } else {
            accumulatedElapsedMs
        }
    }

    // ---------------------------------------------------------------
    // Notificacion persistente + WakeLock
    // ---------------------------------------------------------------
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.notification_channel_desc) }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("DragonGotchiGPS - ${locStr(trackingManager.state.value.activityType.nameRes)}")
            .setContentText(text)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .build()
    }

    private fun startForegroundWithNotification(text: String) {
        val notification = buildNotification(text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val s = trackingManager.state.value
        val text = "${StatsCalculator.formatDuration(s.elapsedSec)} - " +
            "${StatsCalculator.formatDistanceKm(s.distanceM)} km - " +
            "${StatsCalculator.formatPace(s.currentPaceSecPerKm)} /km"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    @Suppress("WakelockTimeout")
    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DragonGotchi:tracking").apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    override fun onDestroy() {
        fusedClient.removeLocationUpdates(locationCallback)
        sensorManager.unregisterListener(this)
        timerJob?.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        serviceScope.cancel()
        super.onDestroy()
    }
}
