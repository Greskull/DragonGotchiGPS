package com.dragongotchi.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.dragongotchi.app.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * ============================================================
 *  BANNER PUBLICITARIO (AdMob) — pantalla Entrenar / mapa
 * ============================================================
 * Banner adaptable anclado (anchored adaptive): Google calcula la
 * altura optima para el ancho disponible; aqui solo se carga y se
 * destruye correctamente al salir de la pantalla.
 *
 * El ID del anuncio sale de res/values/strings.xml
 * (admob_banner_unit_id). Ahora mismo contiene el ID DE PRUEBA
 * oficial de Google, que sirve anuncios de ejemplo sin cuenta ni
 * ingresos: la app compila y funciona tal cual.
 * Para anuncios reales basta sustituir ese string por tu unidad de
 * anuncio Banner (ca-app-pub-XXXX/ZZZZ) y el admob_app_id por tu
 * App ID (ca-app-pub-XXXX~YYYY). No hay que tocar este fichero.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    // Ancho util = ancho de pantalla menos el padding lateral (16+16 dp)
    val adWidthDp = (LocalConfiguration.current.screenWidthDp - 32).coerceAtLeast(280)
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            AdView(ctx).apply {
                setAdUnitId(ctx.getString(R.string.admob_banner_unit_id))
                setAdSize(
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidthDp)
                )
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { adView -> adView.destroy() }
    )
}
