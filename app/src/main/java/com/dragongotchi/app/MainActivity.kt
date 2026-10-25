package com.dragongotchi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.dragongotchi.app.data.preferences.localizedActivityContext
import androidx.activity.compose.LocalActivityResultRegistryOwner
import com.dragongotchi.app.ui.navigation.DragonGotchiNavGraph
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.dragongotchi.app.ui.theme.DragonGotchiTheme
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as DragonGotchiApp).container
        // GDPR/UMP: primero el consentimiento, despues los anuncios.
        gatherConsentAndInitAds()
        // Idioma de la app (ES/EN) guardado en DataStore (el mismo de
        // recompensas y preferencias). Primera lectura directa; despues
        // se observa como estado. Al cambiar, recomponemos TODO con un
        // contexto localizado: la UI cambia de idioma al instante, sin
        // recrear la actividad, sin cortes y sin crashes.
        val initialLanguage = runBlocking { container.userPreferences.appLanguage.first() }
        setContent {
            val language by container.userPreferences.appLanguage
                .collectAsState(initial = initialLanguage)
            val localizedCtx = remember(language) { localizedActivityContext(language) }
            val localizedConfig = remember(language) {
                android.content.res.Configuration(resources.configuration).apply {
                    setLocale(java.util.Locale.forLanguageTag(language.code))
                }
            }
            // LocalActivityResultRegistryOwner se provee de forma
            // explicita: los rememberLauncherForActivityResult de las
            // pantallas (Entrenar, permisos) lo necesitan siempre a
            // mano, viva o no la Activity dentro del LocalContext.
            CompositionLocalProvider(
                LocalContext provides localizedCtx,
                LocalConfiguration provides localizedConfig,
                LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                DragonGotchiTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        DragonGotchiNavGraph(container = container)
                    }
                }
            }
        }
    }

    /**
     * Flujo de consentimiento GDPR con UMP (Europa):
     * 1. Se pide el estado de consentimiento a Google.
     * 2. Si hace falta, se muestra el formulario de privacidad
     *    (solo la primera vez o cuando Google lo requiera).
     * 3. SOLO cuando ya se pueden pedir anuncios (consentimiento dado
     *    ahora o guardado de sesiones anteriores) se inicializa el
     *    SDK de AdMob. El banner muestra anuncios cuando el SDK esta
     *    listo; mientras tanto la app funciona igual.
     * El usuario puede cambiar su eleccion cuando quiera desde
     * Ajustes > "Opciones de privacidad".
     */
    private fun gatherConsentAndInitAds() {
        val consentInformation = UserMessagingPlatform.getConsentInformation(this)
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { _ ->
                    if (consentInformation.canRequestAds()) {
                        MobileAds.initialize(this) {}
                    }
                }
            },
            { _ ->
                // Sin conexion o error al actualizar: se respeta el
                // estado guardado y solo se inicializa si ya se podia.
                if (consentInformation.canRequestAds()) {
                    MobileAds.initialize(this) {}
                }
            }
        )
        // Consentimiento ya guardado de sesiones anteriores: no hay
        // que esperar al formulario para arrancar los anuncios.
        if (consentInformation.canRequestAds()) {
            MobileAds.initialize(this) {}
        }
    }
}
