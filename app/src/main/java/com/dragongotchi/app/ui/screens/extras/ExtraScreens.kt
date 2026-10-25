package com.dragongotchi.app.ui.screens.extras

import androidx.compose.ui.platform.LocalContext
import com.google.android.ump.ConsentInformation
import com.google.android.ump.UserMessagingPlatform

import com.dragongotchi.app.ui.theme.dragonKingdomBackground

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.res.stringResource
import com.dragongotchi.app.R
import com.dragongotchi.app.data.preferences.AppLanguage
import com.dragongotchi.app.data.preferences.findActivity
import com.dragongotchi.app.ui.theme.AncientGold
import com.dragongotchi.app.ui.theme.ArcanePurple
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dragongotchi.app.domain.DragonStage
import com.dragongotchi.app.domain.LevelingSystem
import com.dragongotchi.app.ui.components.BackTopBar
import com.dragongotchi.app.ui.components.dragonSpriteDrawableRes
import kotlinx.coroutines.launch

/** Galeria de las 15 evoluciones: las desbloqueadas a color, las futuras bloqueadas. */
@Composable
fun EvolutionScreen(
    currentLevel: Int,
    onBack: () -> Unit,
    selectedPetId: Int = 1,
    shinyEquipped: Boolean = false
) {
    Column(Modifier.fillMaxSize().dragonKingdomBackground().padding(16.dp)) {
        BackTopBar(title = stringResource(R.string.evo_title), onBack = onBack)
        Text(
            stringResource(R.string.evo_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(DragonStage.entries) { stage ->
                val unlocked = stage.level <= currentLevel
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (stage.level == currentLevel) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(
                                id = dragonSpriteDrawableRes(stage.level, selectedPetId, shinyEquipped)
                            ),
                            contentDescription = stringResource(stage.nameRes),
                            modifier = Modifier.size(64.dp),
                            alpha = if (unlocked) 1f else 0.3f
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.evo_level, stage.level, if (unlocked) stringResource(stage.nameRes) else stringResource(R.string.evo_locked_name)),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (unlocked) stringResource(stage.descRes) else stringResource(R.string.evo_locked_desc),
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (stage.level < LevelingSystem.MAX_LEVEL) {
                                Text(
                                    stringResource(R.string.evo_next_level, LevelingSystem.expNeededForLevel(stage.level), LevelingSystem.stepsNeededForLevel(stage.level)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(stringResource(R.string.evo_final_form), style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF8F00))
                            }
                        }
                        Text(if (stage.level == currentLevel) "📍" else if (unlocked) "✅" else "🔒")
                    }
                }
            }
        }
    }
}

/** Ajustes simples: peso (para kcal) y nombre de la mascota. */
@Composable
fun SettingsScreen(
    initialWeightKg: Float,
    initialPetName: String,
    onSave: suspend (weightKg: Float, petName: String) -> Unit,
    selectedLanguage: AppLanguage = AppLanguage.ES,
    onLanguageSelected: (AppLanguage) -> Unit = {}
) {
    var weight by remember(initialWeightKg) { mutableStateOf(initialWeightKg.toInt().toString()) }
    var petName by remember(initialPetName) { mutableStateOf(initialPetName) }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    val savedText = stringResource(R.string.settings_saved)
    val weightErrorText = stringResource(R.string.settings_weight_error)
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().dragonKingdomBackground().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.settings_weight_explain),
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it.filter { c -> c.isDigit() } },
            label = { Text(stringResource(R.string.settings_weight_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = petName,
            onValueChange = { petName = it.take(20) },
            label = { Text(stringResource(R.string.settings_pet_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                val w = weight.toFloatOrNull()
                if (w == null || w !in 30f..250f) {
                    savedMessage = weightErrorText
                } else {
                    scope.launch {
                        onSave(w, petName)
                        savedMessage = savedText
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.settings_save)) }
        savedMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.settings_battery_tip),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Opciones de privacidad GDPR (solo si Google las requiere en tu region):
        // permite revisar o cambiar el consentimiento de anuncios cuando quieras.
        val settingsContext = LocalContext.current
        val consentInformation = UserMessagingPlatform.getConsentInformation(settingsContext)
        if (consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        ) {
            OutlinedButton(
                onClick = {
                    settingsContext.findActivity()?.let { activity ->
                        UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.settings_privacy)) }
        }
        // Version instalada (util para comprobar que APK llevas puesta)
        Text(
            text = stringResource(
                R.string.settings_version,
                runCatching {
                settingsContext.packageManager
                    .getPackageInfo(settingsContext.packageName, 0).versionName
                }.getOrNull().orEmpty()
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ============ IDIOMA (bandera España / Inglaterra, estilo
        // heraldico medieval): la eleccion se guarda en el mismo
        // DataStore que las recompensas y preferencias, y la UI cambia
        // al instante sin recrear la actividad ni cortes.
        Text(
            text = stringResource(R.string.settings_language),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AncientGold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LanguageWarButton(
                flag = "🇪🇸",
                label = stringResource(R.string.lang_es),
                selected = selectedLanguage == AppLanguage.ES,
                modifier = Modifier.weight(1f),
                onClick = { onLanguageSelected(AppLanguage.ES) }
            )
            LanguageWarButton(
                flag = "🇬🇧",
                label = stringResource(R.string.lang_en),
                selected = selectedLanguage == AppLanguage.EN,
                modifier = Modifier.weight(1f),
                onClick = { onLanguageSelected(AppLanguage.EN) }
            )
        }
    }
}

/**
 * Boton de idioma estilo estandarte de guerra medieval: seleccionado =
 * blasón morado arcano con borde de oro antiguo; sin seleccionar =
 * contorno dorado sobre fondo transparente.
 */
@Composable
private fun LanguageWarButton(
    flag: String,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.border(2.dp, AncientGold, shape),
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = ArcanePurple,
                contentColor = Color(0xFFFFC107)
            )
        ) { Text("$flag $label", fontWeight = FontWeight.Bold, maxLines = 1) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AncientGold)
        ) { Text("$flag $label", maxLines = 1) }
    }
}
