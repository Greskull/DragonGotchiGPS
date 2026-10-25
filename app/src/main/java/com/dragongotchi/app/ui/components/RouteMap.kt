package com.dragongotchi.app.ui.components

import androidx.compose.ui.res.stringResource
import com.dragongotchi.app.R

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.content.Context
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.dragongotchi.app.domain.GeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import java.io.File

/**
 * ============================================================
 *  MAPA GRATUITO — OpenStreetMap + osmdroid
 * ============================================================
 * - Sin API key, sin cuenta y sin coste: tiles publicos de
 *   OpenStreetMap (TileSourceFactory.MAPNIK). Solo necesitas Internet.
 * - Politica de uso de OSM: hay que identificarse con un user-agent
 *   propio (el package de la app) y mostrar la atribucion
 *   "© OpenStreetMap contributors" (texto en la esquina inferior).
 * - La cache de tiles se guarda en el cacheDir de la app: las zonas
 *   ya visitadas se pueden volver a ver sin conexion.
 *
 * Modos:
 * - En vivo (isLiveTracking = true): sigue a tu dragon. Si arrastras
 *   el mapa con el dedo deja de seguirte, y aparece el boton
 *   "Centrar en mi dragon" para volver a centrarlo.
 * - Historial (fitEntireRoute = true): encuadra la ruta completa.
 *
 * Marcador de posicion: si [dragonLevel] no es null, tu posicion es el
 * sprite del Tamagotchi en ese nivel, rasterizado SIEMPRE a
 * [DRAGON_MARKER_SIZE_DP] dp (ver DragonMarker.kt), sea cual sea el
 * tamano del PNG que pongas en drawable. Si es null (historial) se
 * usa un punto clasico de fin de ruta.
 *
 * ⚠️ POR QUE NO USAMOS Marker/Polyline DE OSMDROID (crash real):
 * en osmdroid 6.1.20, `MapView.onDetach()` anula el MapView interno
 * de su MapViewRepository. Si despues se crea un `Marker` (su
 * constructor pide el InfoWindow por defecto al repository), se
 * produce NullPointerException en InfoWindow.<init> y la app se
 * cierra al empezar una actividad. Por eso la ruta y las posiciones
 * se dibujan con overlays propios (Canvas/Paint/Bitmap), que nunca
 * tocan el repository, y NO llamamos a onDetach/onResume/onPause a
 * mano: el MapView gestiona su propio ciclo de vida al entrar y
 * salir de la ventana.
 */

/** Convierte cualquier Drawable a Bitmap (los marcadores ya son BitmapDrawable). */
private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap
    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(canvas)
    return bitmap
}

/**
 * Ruta con efecto de TERRENO QUEMADO (scorched ground): el dragon ha
 * pasado ardiendo por ahi. Se dibuja con 5 trazos, de fuera a dentro,
 * para simular el rastro calcinado:
 *  1. Halo de ceniza oscura (el suelo chamuscado, ancho y translucido)
 *  2. Costra de carbon casi negra
 *  3. Brasa incandescente naranja-rojiza
 *  4. Lava viva naranja
 *  5. Filo caliente amarillo, discontinuo como chispas/grietas de lava
 *
 * Es un Overlay propio que pinta con Canvas: NO crea Polyline de
 * osmdroid (ver la nota de cabecera sobre el crash del repository).
 */
private class ScorchedRouteOverlay(private val points: List<OsmGeoPoint>) : Overlay() {

    private val layers: List<Paint> = listOf(
        layer(AndroidColor.rgb(46, 30, 20), width = 24f, alpha = 120),
        layer(AndroidColor.rgb(38, 24, 18), width = 17f, alpha = 215),
        layer(AndroidColor.rgb(216, 67, 21), width = 11f, alpha = 235),
        layer(AndroidColor.rgb(255, 124, 0), width = 6.5f, alpha = 245),
        layer(
            AndroidColor.rgb(255, 213, 79), width = 3f, alpha = 255,
            dash = DashPathEffect(floatArrayOf(14f, 10f), 0f)
        )
    )

    private val reusePoint = Point()

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || points.size < 2) return
        val projection = mapView.projection
        val path = android.graphics.Path()
        points.forEachIndexed { index, geoPoint ->
            projection.toPixels(geoPoint, reusePoint)
            if (index == 0) {
                path.moveTo(reusePoint.x.toFloat(), reusePoint.y.toFloat())
            } else {
                path.lineTo(reusePoint.x.toFloat(), reusePoint.y.toFloat())
            }
        }
        layers.forEach { paint -> canvas.drawPath(path, paint) }
    }

    private companion object {
        fun layer(color: Int, width: Float, alpha: Int, dash: DashPathEffect? = null): Paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                this.alpha = alpha
                strokeWidth = width
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                if (dash != null) pathEffect = dash
            }
    }
}

/**
 * Posicion dibujada como Bitmap centrado en el punto (el dragon de
 * 48 dp o el circulo de inicio/fin). Sustituye al Marker de osmdroid
 * por el mismo motivo: no toca el MapViewRepository.
 */
private class BitmapPositionOverlay(
    private val position: OsmGeoPoint,
    private val bitmap: Bitmap
) : Overlay() {

    private val reusePoint = Point()

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        mapView.projection.toPixels(position, reusePoint)
        canvas.drawBitmap(
            bitmap,
            reusePoint.x - bitmap.width / 2f,
            reusePoint.y - bitmap.height / 2f,
            null
        )
    }
}

/** Quita SOLO nuestros overlays (ruta quemada y posiciones). */
private fun removeCustomOverlays(map: MapView) {
    map.overlays.removeAll { overlay: Overlay ->
        overlay is ScorchedRouteOverlay || overlay is BitmapPositionOverlay
    }
}

@Composable
fun RouteMap(
    points: List<GeoPoint>,
    modifier: Modifier = Modifier,
    currentPoint: GeoPoint? = null,
    dragonLevel: Int? = null,
    dragonPetId: Int = 1,
    dragonShiny: Boolean = false,
    isLiveTracking: Boolean = false,
    fitEntireRoute: Boolean = false,
    /** Cambia en cada sesion nueva (p. ej. su hora de inicio): al cambiar,
     *  el mapa olvida la camara y el seguimiento de la sesion anterior. */
    sessionKey: Long = 0L,
    gpsError: String? = null,
    routeColor: Color = Color(0xFF2E7D32)
) {
    val context = LocalContext.current

    // Al cambiar sessionKey (nueva actividad), seguimiento y ultimo
    // centrado se reinician: nada de la sesion anterior sobrevive.
    var followDragon by remember(isLiveTracking, sessionKey) { mutableStateOf(isLiveTracking) }
    var lastCenteredKey by remember(sessionKey) { mutableStateOf<String?>(null) }
    var recenterTick by remember(sessionKey) { mutableIntStateOf(0) }
    var hasFittedRoute by remember(sessionKey, points.firstOrNull()?.timeMillis, points.size) {
        mutableStateOf(false)
    }

    val dragonRes = dragonLevel?.let {
        dragonSpriteDrawableRes(it, dragonPetId, dragonShiny)
    }
    // Marcador = SOLO el sprite del dragon, fondo transparente (sin
    // circulo blanco ni aura). El sprite ya es el del nivel/mascota/
    // shiny vigente: dragonSpriteDrawableRes lo resuelve por nombre.
    val dragonMarker = remember(context, dragonRes) {
        dragonRes?.let { createDragonMarkerDrawable(context = context, spriteRes = it) }
    }
    val dragonBitmap = remember(dragonMarker) { dragonMarker?.let { drawableToBitmap(it) } }
    val startBitmap = remember(context) {
        // Inicio en morado arcano (fantasia), distinto de la ruta quemada
        drawableToBitmap(createCircleMarkerDrawable(context, AndroidColor.rgb(94, 53, 177)))
    }
    val endBitmap = remember(context) {
        drawableToBitmap(createCircleMarkerDrawable(context, AndroidColor.rgb(198, 40, 40)))
    }

    val visiblePosition = points.lastOrNull() ?: currentPoint
    val positionKey = visiblePosition?.let {
        "${it.latitude},${it.longitude},${it.timeMillis},$recenterTick"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE8F5E9))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                // Configuracion global de osmdroid ANTES de crear el MapView
                // (user-agent descriptivo obligatorio segun la politica de
                // tiles de OpenStreetMap, y cache en disco para las teselas).
                val osmPrefs = ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
                Configuration.getInstance().apply {
                    load(ctx, osmPrefs)
                    userAgentValue = "DragonGotchiGPS/${ctx.packageManager
                        .getPackageInfo(ctx.packageName, 0).versionName} (${ctx.packageName})"
                    osmdroidBasePath = ctx.cacheDir
                    osmdroidTileCache = File(ctx.cacheDir, "osmdroid_tiles").apply { mkdirs() }
                    tileFileSystemCacheMaxBytes = 80L * 1024L * 1024L // 80 MB de teselas
                    tileDownloadThreads = 6
                    tileFileSystemThreads = 4
                    save(ctx, osmPrefs)
                }
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    isTilesScaledToDpi = true
                    setUseDataConnection(true) // permite descargar teselas
                    controller.setZoom(16.0)
                    // Centro por defecto (España) hasta que llega el primer GPS
                    controller.setCenter(OsmGeoPoint(40.4168, -3.7038))
                    setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_MOVE) {
                            // El usuario esta explorando: dejamos de seguir al dragon
                            followDragon = false
                        }
                        false
                    }
                    // SIN onResume()/onPause()/onDetach() manuales: el MapView
                    // gestiona su ciclo de vida al adjuntarse a la ventana, y
                    // llamar a onDetach() a mano dejaba el MapViewRepository
                    // anulado (crash al crear marcadores despues).
                }
            },
            update = { map ->
                // Solo retiramos NUESTROS overlays (nunca clear(): la lista
                // visible no incluye las teselas, que osmdroid gestiona en
                // su TilesOverlay interno, pero rehacerla entera rompe el
                // orden de dibujo y no aporta nada).
                removeCustomOverlays(map)

                if (points.isNotEmpty()) {
                    val osmPoints = points.map { OsmGeoPoint(it.latitude, it.longitude) }

                    if (osmPoints.size >= 2) {
                        // Camino quemado por el dragon (5 capas, ver arriba)
                        map.overlays.add(ScorchedRouteOverlay(osmPoints))
                    }

                    // Inicio de la ruta
                    map.overlays.add(BitmapPositionOverlay(osmPoints.first(), startBitmap))
                }

                // Posicion actual / final: EL DRAGON (o punto rojo en historial)
                val position = visiblePosition
                if (position != null) {
                    map.overlays.add(
                        BitmapPositionOverlay(
                            position = OsmGeoPoint(position.latitude, position.longitude),
                            bitmap = dragonBitmap ?: endBitmap
                        )
                    )
                }

                when {
                    // Historial: encuadrar toda la ruta una vez
                    fitEntireRoute && points.size >= 2 && !hasFittedRoute -> {
                        val box = BoundingBox.fromGeoPoints(
                            points.map { OsmGeoPoint(it.latitude, it.longitude) }
                        )
                        map.post {
                            map.zoomToBoundingBox(box, true, 72)
                            hasFittedRoute = true
                        }
                    }
                    fitEntireRoute && points.size == 1 && !hasFittedRoute -> {
                        map.controller.setZoom(17.0)
                        map.controller.setCenter(OsmGeoPoint(points[0].latitude, points[0].longitude))
                        hasFittedRoute = true
                    }
                    // En vivo: seguir al dragon solo si el usuario no esta explorando
                    isLiveTracking && position != null && followDragon && positionKey != lastCenteredKey -> {
                        lastCenteredKey = positionKey
                        map.controller.setZoom(maxOf(map.zoomLevelDouble, 16.5))
                        map.controller.animateTo(OsmGeoPoint(position.latitude, position.longitude))
                    }
                }
                map.invalidate()
            }
        )

        // Chip de estado GPS mientras aun no hay posicion
        if (visiblePosition == null) {
            Surface(
                color = if (gpsError != null) Color(0xFFC62828).copy(alpha = 0.94f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.TopCenter).padding(10.dp)
            ) {
                Text(
                    text = if (gpsError != null) "⚠️ $gpsError"
                           else stringResource(R.string.map_waiting_gps),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (gpsError != null) Color.White
                            else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // Boton para volver a seguir al dragon tras explorar el mapa
        if (isLiveTracking && visiblePosition != null && !followDragon) {
            TextButton(
                onClick = {
                    followDragon = true
                    recenterTick++
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Text(stringResource(R.string.map_center_dragon))
            }
        }

        // Atribucion obligatoria de los datos del mapa
        Text(
            text = "© OpenStreetMap contributors",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF37474F),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
