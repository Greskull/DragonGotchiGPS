package com.dragongotchi.app.data.preferences

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Resources
import android.app.Activity
import android.content.res.Configuration
import java.util.Locale
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dragongotchi.app.domain.RewardId
import com.dragongotchi.app.domain.RewardState
import com.dragongotchi.app.domain.RollResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    companion object {
        private val KEY_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_WEIGHT = floatPreferencesKey("user_weight_kg")
        private val KEY_ONBOARDING = booleanPreferencesKey("onboarding_completed")
        private val KEY_PET_NAME = stringPreferencesKey("pet_name")

        // ---- Recompensas (dice roll) ----
        private val KEY_UNLOCKED_REWARDS = stringSetPreferencesKey("rewards_unlocked")
        private val KEY_AURA_EQUIPPED = booleanPreferencesKey("reward_aura_equipped")
        private val KEY_SHINY_EQUIPPED = booleanPreferencesKey("reward_shiny_equipped")
        private val KEY_SELECTED_PET = intPreferencesKey("reward_selected_pet")
        private val KEY_HAS_UNSEEN = booleanPreferencesKey("reward_has_unseen")
        private val KEY_TOTAL_ROLLS = intPreferencesKey("reward_total_rolls")
        private val KEY_LAST_ROLL = intPreferencesKey("reward_last_roll")
        private val KEY_LAST_UNLOCKED = stringPreferencesKey("reward_last_unlocked")

        const val DEFAULT_WEIGHT_KG = 70f
    }

    /**
     * Idioma de la app elegido en Ajustes (mismo DataStore que las
     * recompensas y demas preferencias). Por defecto, español.
     */
    val appLanguage: Flow<AppLanguage> = context.dataStore.data
        .map { AppLanguage.fromCode(it[KEY_LANGUAGE]) }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.dataStore.edit { it[KEY_LANGUAGE] = language.code }
    }

    /** Peso para el calculo de kcal. Editable en Ajustes (por defecto 70 kg). */
    val weightKg: Flow<Float> = context.dataStore.data.map { it[KEY_WEIGHT] ?: DEFAULT_WEIGHT_KG }
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING] ?: false }
    val petName: Flow<String> = context.dataStore.data.map { it[KEY_PET_NAME] ?: "Draco" }

    suspend fun setWeightKg(value: Float) {
        context.dataStore.edit { it[KEY_WEIGHT] = value.coerceIn(30f, 250f) }
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { it[KEY_ONBOARDING] = true }
    }

    suspend fun setPetName(name: String) {
        context.dataStore.edit { if (name.isNotBlank()) it[KEY_PET_NAME] = name.trim().take(20) }
    }

    // ---------------------------------------------------------------
    // Recompensas: estado, tiradas y equipamiento
    // ---------------------------------------------------------------
    val rewardState: Flow<RewardState> = context.dataStore.data.map { prefs ->
        RewardState(
            unlockedRewards = (prefs[KEY_UNLOCKED_REWARDS] ?: emptySet())
                .mapNotNull { RewardId.fromStorageName(it) }
                .toSet(),
            auraEquipped = prefs[KEY_AURA_EQUIPPED] ?: false,
            shinyEquipped = prefs[KEY_SHINY_EQUIPPED] ?: false,
            selectedPetId = (prefs[KEY_SELECTED_PET] ?: 1).coerceIn(1, 3),
            hasUnseenReward = prefs[KEY_HAS_UNSEEN] ?: false,
            totalRolls = prefs[KEY_TOTAL_ROLLS] ?: 0,
            lastRollValue = prefs[KEY_LAST_ROLL],
            lastUnlockedReward = RewardId.fromStorageName(prefs[KEY_LAST_UNLOCKED])
        )
    }

    /**
     * Registra el resultado de una tirada. Si desbloquea una recompensa,
     * la guarda (para siempre: nunca puede volver a tocar) y levanta el
     * aviso «!» hasta que el usuario entre en el menu de Recompensas.
     */
    suspend fun recordRoll(result: RollResult) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOTAL_ROLLS] = (prefs[KEY_TOTAL_ROLLS] ?: 0) + 1
            prefs[KEY_LAST_ROLL] = result.value
            val reward = result.unlockedReward
            if (reward != null) {
                val current = prefs[KEY_UNLOCKED_REWARDS] ?: emptySet()
                prefs[KEY_UNLOCKED_REWARDS] = current + reward.name
                prefs[KEY_LAST_UNLOCKED] = reward.name
                prefs[KEY_HAS_UNSEEN] = true
            }
        }
    }

    suspend fun setAuraEquipped(equipped: Boolean) {
        context.dataStore.edit { prefs ->
            val unlocked = (prefs[KEY_UNLOCKED_REWARDS] ?: emptySet())
                .mapNotNull { RewardId.fromStorageName(it) }.toSet()
            // No se puede equipar lo que no se ha desbloqueado
            if (!equipped || RewardId.AURA in unlocked) prefs[KEY_AURA_EQUIPPED] = equipped
        }
    }

    suspend fun setShinyEquipped(equipped: Boolean) {
        context.dataStore.edit { prefs ->
            val unlocked = (prefs[KEY_UNLOCKED_REWARDS] ?: emptySet())
                .mapNotNull { RewardId.fromStorageName(it) }.toSet()
            if (!equipped || RewardId.SHINY_SKIN in unlocked) prefs[KEY_SHINY_EQUIPPED] = equipped
        }
    }

    suspend fun setSelectedPet(petId: Int) {
        context.dataStore.edit { prefs ->
            val unlocked = (prefs[KEY_UNLOCKED_REWARDS] ?: emptySet())
                .mapNotNull { RewardId.fromStorageName(it) }.toSet()
            val allowed = when (petId) {
                1 -> true
                2 -> RewardId.PET_DRAGON_2 in unlocked
                3 -> RewardId.PET_DRAGON_3 in unlocked
                else -> false
            }
            if (allowed) prefs[KEY_SELECTED_PET] = petId
        }
    }

    /** El usuario ya ha visto la novedad: se apaga el «!» rojo. */
    suspend fun markRewardsSeen() {
        context.dataStore.edit { it[KEY_HAS_UNSEEN] = false }
    }
}

/** Idiomas disponibles de la app. El codigo se guarda en DataStore. */
enum class AppLanguage(val code: String) {
    ES("es"),
    EN("en");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ES
    }
}

/**
 * Devuelve un Context cuyos recursos (strings.xml / values-en) se
 * resuelven en el idioma elegido, sin cambiar el idioma del sistema
 * ni recrear la actividad: en Compose basta recomponer con este
 * contexto para que toda la UI cambie de idioma al instante.
 */
/**
 * Contexto para la UI de Compose: un ContextWrapper SOBRE la Activity
 * con los recursos (getResources/getString) del idioma elegido.
 *
 * Es importante que envuelva a la Activity (y no usar el contexto de
 * createConfigurationContext «pelado», que ya no la contiene): asi la
 * cadena de contextos sigue llegando a la Activity, y cosas como
 * rememberLauncherForActivityResult (LocalActivityResultRegistryOwner)
 * o la busqueda de la Activity siguen funcionando igual que siempre.
 */
fun Context.localizedActivityContext(language: AppLanguage): Context {
    val localized = localizedContext(language)
    return object : ContextWrapper(this) {
        // getString/getText de Context son final y delegan en
        // getResources(): con esto basta para que TODA lectura de
        // recursos (stringResource, getString, painterResource...)
        // salga en el idioma elegido.
        override fun getResources(): Resources = localized.resources
        override fun getAssets(): AssetManager = localized.assets
    }
}

/** Desenvuelve ContextWrapper hasta encontrar la Activity, si la hay. */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx != null) {
        if (ctx is Activity) return ctx
        ctx = (ctx as? ContextWrapper)?.baseContext
    }
    return null
}

fun Context.localizedContext(language: AppLanguage): Context {
    val config = Configuration(resources.configuration).apply {
        setLocale(Locale.forLanguageTag(language.code))
    }
    return createConfigurationContext(config)
}
