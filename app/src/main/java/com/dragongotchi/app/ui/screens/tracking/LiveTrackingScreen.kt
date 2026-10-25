package com.dragongotchi.app.ui.screens.tracking

import androidx.compose.ui.res.stringResource
import com.dragongotchi.app.R

import android.annotation.SuppressLint
import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dragongotchi.app.domain.ActivityFilter
import com.dragongotchi.app.domain.ActivityType
import com.dragongotchi.app.domain.GeoPoint
import com.dragongotchi.app.domain.RewardState
import com.dragongotchi.app.domain.StatsCalculator
import com.dragongotchi.app.location.LiveTrackingState
import com.dragongotchi.app.location.LocationTrackingService
import com.dragongotchi.app.location.TrackingStatus
import com.dragongotchi.app.ui.components.AdBanner
import com.dragongotchi.app.ui.components.RouteMap
import com.dragongotchi.app.ui.components.StatCard
import com.dragongotchi.app.ui.theme.dragonKingdomBackground
import com.dragongotchi.app.ui.screens.onboarding.hasPermission
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.StateFlow

/**
 * Pantalla EN VIVO. Solo PINTA el estado que publica el Foreground Service.
 * No pide ubicaciones, no calcula rutas y no guarda nada: si esta pantalla
 * desaparece (app minimizada / pantalla bloqueada), el tracking sigue.
 */
@Composable
fun LiveTrackingScreen(
    trackingStateFlow: StateFlow<LiveTrackingState>,
    petLevel: Int,
    rewardState: RewardState = RewardState(),
    onFinishedSession: (sessionId: Long?) -> Unit
) {
    val context = LocalContext.current
    val state by trackingStateFlow.collectAsState()

    // ---------------------------------------------------------------
    // VISTA PREVIA DE POSICION (solo para pintar el mapa en reposo).
    // El tracking de verdad sigue viviendo SOLO en el servicio; esto
    // es una unica consulta de cortesia —la ultima posicion conocida
    // y un fix puntual— para que al abrir "Entrenar" el mapa ya se
    // centre en ti como en otras apps, en vez de ensenar España y el
    // aviso de "esperando senal GPS" sin haber empezado siquiera.
    // ---------------------------------------------------------------
    var previewPoint by remember { mutableStateOf<GeoPoint?>(null) }
    LaunchedEffect(Unit) {
        if (context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            fetchPreviewPoint(context) { point -> previewPoint = point }
        }
    }

    var selectedType by remember { mutableStateOf(ActivityType.RUN) }
    var showStopDialog by remember { mutableStateOf(false) }
    // Mascota vigente, siempre actualizada para capturar su aspecto al Iniciar
    val currentPetLevel by rememberUpdatedState(petLevel)
    val currentRewardState by rememberUpdatedState(rewardState)

    fun startService() {
        // Nivel + mascota + shiny + aura se congelan AHORA para toda la sesion
        LocationTrackingService.start(
            context = context,
            type = selectedType,
            dragonLevel = currentPetLevel,
            dragonPetId = currentRewardState.selectedPetId,
            dragonShiny = currentRewardState.shinyEquipped,
            dragonAura = currentRewardState.auraEquipped
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            startService()
        } else {
            Toast.makeText(context, context.getString(R.string.toast_no_location), Toast.LENGTH_LONG).show()
        }
    }

    // Cuando el servicio termina, saltamos al resumen (o a inicio si la sesion
    // fue demasiado corta para guardarse). Una sola vez por finalizacion.
    val finishedId = state.finishedSessionId
    var finishHandled by remember { mutableStateOf(false) }
    LaunchedEffect(state.hasJustFinished, finishedId) {
        if (state.hasJustFinished && !finishHandled) {
            finishHandled = true
            val message = buildString {
                if (!state.activityWasValid) {
                    append(context.getString(R.string.toast_invalid_prefix))
                    append(missingActivityText(context, state.steps, state.elapsedSec))
                } else {
                    if (state.lastLevelsGained > 0) {
                        append(context.getString(R.string.toast_level_up, state.lastLevelsGained, state.lastExpGained))
                    } else if (state.lastExpGained > 0) {
                        append(context.getString(R.string.toast_exp_gain, state.lastExpGained))
                    }
                    val reward = state.unlockedRewardName
                    if (state.rollValue != null && reward != null) {
                        append(context.getString(R.string.toast_reward_unlocked, state.rollValue, reward))
                    } else if (state.rollValue != null) {
                        append(context.getString(R.string.toast_no_reward, state.rollValue))
                    }
                }
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            onFinishedSession(finishedId)
        }
    }

    Column(Modifier.fillMaxSize().dragonKingdomBackground().padding(16.dp)) {
        Text(
            text = when (state.status) {
                TrackingStatus.IDLE -> stringResource(R.string.track_new)
                TrackingStatus.RUNNING -> stringResource(R.string.track_recording)
                TrackingStatus.PAUSED -> stringResource(R.string.track_paused)
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        if (state.status == TrackingStatus.IDLE) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.track_type_label), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivityType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text("${type.emoji} ${stringResource(type.nameRes)}") }
                    )
                }
            }
        } else {
            Text(
                text = stringResource(
                    R.string.track_gps_stats,
                    state.gpsAccuracyM?.let { stringResource(R.string.track_gps_accuracy, it) }
                        ?: stringResource(R.string.track_gps_searching),
                    state.acceptedPoints, state.rejectedPoints
                ) + if (state.suspiciousPoints > 0) stringResource(R.string.track_gps_suspicious, state.suspiciousPoints) else "",
                style = MaterialTheme.typography.bodySmall,
                color = if (state.suspiciousPoints > 0) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            state.lastRejectedReason?.let {
                Text(
                    text = stringResource(R.string.track_last_rejected, rejectionReasonText(it)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (ActivityFilter.isValid(state.steps, state.elapsedSec)) {
                    stringResource(R.string.track_valid_yes)
                } else {
                    stringResource(R.string.track_valid_missing, missingActivityText(context, state.steps, state.elapsedSec))
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (ActivityFilter.isValid(state.steps, state.elapsedSec)) Color(0xFF2E7D32)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))
        // Mapa gratuito OpenStreetMap. Tu posicion la marca tu dragon en el
        // nivel capturado al iniciar la sesion (antes de empezar, el nivel
        // vigente en "Mi dragon"), siempre con el mismo tamano fijo de
        // 48 dp aunque cambies el PNG de drawable por uno gigante.
        RouteMap(
            points = state.routePoints,
            // La vista previa SOLO se pinta en reposo (IDLE). Al iniciar
            // una actividad nueva no se hereda ninguna posicion anterior:
            // el dragon aparece cuando llega el primer fix de la sesion.
            currentPoint = state.currentPoint
                ?: if (state.status == TrackingStatus.IDLE) previewPoint else null,
            dragonLevel = if (state.status == TrackingStatus.IDLE) petLevel else state.dragonLevelAtStart,
            dragonPetId = if (state.status == TrackingStatus.IDLE) rewardState.selectedPetId else state.dragonPetIdAtStart,
            dragonShiny = if (state.status == TrackingStatus.IDLE) rewardState.shinyEquipped else state.dragonShinyAtStart,
            isLiveTracking = true,
            // Nueva sesion = nuevo sessionKey: el mapa reinicia camara y
            // seguimiento, sin restos de la actividad anterior.
            sessionKey = state.startTimeMillis,
            gpsError = state.gpsError,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        Spacer(Modifier.height(12.dp))

        // Estadisticas en vivo (las actualiza el servicio cada segundo / cada punto GPS)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(stringResource(R.string.stat_time), StatsCalculator.formatDuration(state.elapsedSec), modifier = Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_distance), StatsCalculator.formatDistanceKm(state.distanceM), "km", Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_pace), StatsCalculator.formatPace(state.currentPaceSecPerKm), "/km", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(stringResource(R.string.stat_calories), StatsCalculator.formatKcal(state.kcal), "kcal", Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_steps), state.steps.toString(), modifier = Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_speed), "%.1f".format(state.currentSpeedKmh), "km/h", Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        when (state.status) {
            TrackingStatus.IDLE -> {
                Button(
                    onClick = {
                        if (context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                            startService()
                        } else {
                            val perms = mutableListOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                perms.add(Manifest.permission.ACTIVITY_RECOGNITION)
                            }
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionLauncher.launch(perms.toTypedArray())
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text(stringResource(R.string.track_btn_start, stringResource(selectedType.nameRes)), style = MaterialTheme.typography.titleMedium) }
                Text(
                    stringResource(R.string.track_valid_rule, ActivityFilter.MIN_STEPS, StatsCalculator.formatDuration(ActivityFilter.MIN_DURATION_SEC)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    stringResource(R.string.track_background_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            TrackingStatus.RUNNING -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { LocationTrackingService.sendAction(context, LocationTrackingService.ACTION_PAUSE) },
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text(stringResource(R.string.btn_pause)) }
                    Button(
                        onClick = { showStopDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text(stringResource(R.string.btn_finish)) }
                }
            }
            TrackingStatus.PAUSED -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { LocationTrackingService.sendAction(context, LocationTrackingService.ACTION_RESUME) },
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text(stringResource(R.string.btn_resume)) }
                    Button(
                        onClick = { showStopDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text(stringResource(R.string.btn_finish)) }
                }
            }
        }

        // Banner publicitario (AdMob) al pie de la pantalla del mapa.
        // Con los IDs de prueba de strings.xml muestra anuncios de ejemplo.
        Spacer(Modifier.height(12.dp))
        AdBanner(Modifier.fillMaxWidth())
    }

    if (showStopDialog) {
        // IMPORTANTE (bug idioma): los textos del dialogo se resuelven
        // AQUI, en la composicion de la pantalla, y no dentro de los
        // slots del AlertDialog. El contenido de un Dialog se compone
        // en su propia ventana, donde LocalContext/LocalResources caen
        // a los del sistema (idioma del movil) ignorando el contexto
        // localizado de la app: por eso «Finalizar y tirar/descartar»
        // salia en el idioma del telefono. Resueltos fuera con el
        // contexto localizado (val context), el dialogo siempre habla
        // el idioma elegido en Ajustes.
        val finishDialogTitle = context.getString(R.string.dialog_finish_title)
        val finishDialogBody = if (ActivityFilter.isValid(state.steps, state.elapsedSec)) {
            context.getString(R.string.dialog_finish_valid, state.steps, state.steps / 100)
        } else {
            context.getString(R.string.dialog_finish_invalid, missingActivityText(context, state.steps, state.elapsedSec))
        }
        val finishDialogConfirm = if (ActivityFilter.isValid(state.steps, state.elapsedSec)) {
            context.getString(R.string.dialog_btn_finish_roll)
        } else {
            context.getString(R.string.dialog_btn_finish_discard)
        }
        val finishDialogDismiss = context.getString(R.string.dialog_btn_continue)
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text(finishDialogTitle) },
            text = { Text(finishDialogBody) },
            confirmButton = {
                Button(onClick = {
                    showStopDialog = false
                    LocationTrackingService.sendAction(context, LocationTrackingService.ACTION_STOP)
                }) { Text(finishDialogConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) { Text(finishDialogDismiss) }
            }
        )
    }
}

/**
 * Consulta UNICA de posicion para la vista previa del mapa en reposo
 * (ultima conocida + un fix puntual de bajo consumo). NO es tracking:
 * el seguimiento continuo vive solo en LocationTrackingService.
 * El llamante comprueba ACCESS_FINE_LOCATION antes de invocarla.
 */
@SuppressLint("MissingPermission")
private fun fetchPreviewPoint(context: android.content.Context, onPoint: (GeoPoint) -> Unit) {
    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    fun toGeoPoint(location: android.location.Location) = GeoPoint(
        latitude = location.latitude,
        longitude = location.longitude,
        timeMillis = if (location.time > 0) location.time else System.currentTimeMillis()
    )
    runCatching {
        fusedClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) onPoint(toGeoPoint(location))
        }
    }
    runCatching {
        fusedClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location ->
            if (location != null) onPoint(toGeoPoint(location))
        }
    }
}

/** Texto localizado con lo que falta para que la actividad sea valida. */
private fun missingActivityText(context: android.content.Context, steps: Int, durationSec: Long): String {
    if (ActivityFilter.isValid(steps, durationSec)) {
        return context.getString(R.string.activity_valid_word)
    }
    val parts = mutableListOf<String>()
    if (steps < ActivityFilter.MIN_STEPS) {
        parts.add(context.getString(R.string.missing_steps, steps, ActivityFilter.MIN_STEPS))
    }
    if (durationSec < ActivityFilter.MIN_DURATION_SEC) {
        parts.add(
            context.getString(
                R.string.missing_time,
                StatsCalculator.formatDuration(durationSec),
                StatsCalculator.formatDuration(ActivityFilter.MIN_DURATION_SEC)
            )
        )
    }
    return context.getString(R.string.missing_wrap, parts.joinToString(" · "))
}

/** Motivos de rechazo GPS (el servicio guarda el nombre del enum). */
@Composable
private fun rejectionReasonText(reasonName: String): String = when (reasonName) {
    "MOCK_LOCATION" -> stringResource(R.string.reason_mock)
    "LOW_ACCURACY" -> stringResource(R.string.reason_low_accuracy)
    "REPORTED_SPEED_TOO_HIGH" -> stringResource(R.string.reason_reported_speed)
    "IMPLAUSIBLE_SPEED" -> stringResource(R.string.reason_implausible_speed)
    "JITTER_TOO_CLOSE" -> stringResource(R.string.reason_jitter)
    "INVALID_TIMESTAMP" -> stringResource(R.string.reason_invalid_time)
    else -> reasonName
}
