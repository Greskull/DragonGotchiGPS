package com.dragongotchi.app.ui.screens.extras

import androidx.compose.ui.res.stringResource
import com.dragongotchi.app.R

import com.dragongotchi.app.ui.theme.dragonKingdomBackground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dragongotchi.app.domain.ActivityFilter
import com.dragongotchi.app.domain.PetChoice
import com.dragongotchi.app.domain.RewardId
import com.dragongotchi.app.domain.RewardState
import com.dragongotchi.app.domain.RewardsSystem
import com.dragongotchi.app.ui.components.BackTopBar

/**
 * Menu de RECOMPENSAS (dice roll).
 *
 * - Cada actividad valida (>=500 pasos y >=250 s) hace una tirada 1..100
 *   al finalizar, con intervalos fijos que nunca se solapan:
 *   Aura 1-15 (15%), Shiny 16-22 (7%), Dragon 2 23-25 (3%),
 *   Dragon 3 26-28 (3%), sin recompensa 29-100 (72%).
 * - Lo desbloqueado es para siempre: su intervalo queda anulado y
 *   nunca puede volver a tocar.
 * - Desde aqui se equipa/desequipa el aura y el shiny, y se elige
 *   que dragon usar (1, 2 o 3). Todo se aplica en «Mi dragon» y en
 *   el marcador del mapa de la siguiente sesion.
 */
@Composable
fun RewardsScreen(
    rewardState: RewardState,
    onBack: () -> Unit,
    onRewardsSeen: () -> Unit,
    onToggleAura: (Boolean) -> Unit,
    onToggleShiny: (Boolean) -> Unit,
    onSelectPet: (Int) -> Unit
) {
    // Al entrar en el menu, el aviso «!» rojo de Inicio se apaga
    LaunchedEffect(Unit) { onRewardsSeen() }

    Column(Modifier.fillMaxSize().dragonKingdomBackground().padding(16.dp)) {
        BackTopBar(title = stringResource(R.string.rewards_title), onBack = onBack)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.rewards_how_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.rewards_how_body, ActivityFilter.MIN_STEPS, ActivityFilter.MIN_DURATION_SEC.toInt()),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    buildString {
                        append(stringResource(R.string.rewards_rolls_done, rewardState.totalRolls))
                        rewardState.lastRollValue?.let { append(stringResource(R.string.rewards_last_roll, it)) }
                        rewardState.lastUnlockedReward?.let { append(stringResource(R.string.rewards_last_prize, it.emoji, stringResource(it.nameRes))) }
                        append(stringResource(R.string.rewards_current_chance, RewardsSystem.effectiveAnyRewardPercent(rewardState.unlockedRewards)))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // ---------- AURA ----------
            item {
                RewardCard(
                    emoji = RewardId.AURA.emoji,
                    title = stringResource(RewardId.AURA.nameRes),
                    description = stringResource(RewardId.AURA.descRes),
                    chanceText = stringResource(R.string.chance_percent_range, 15, "1-15"),
                    unlocked = rewardState.isUnlocked(RewardId.AURA),
                    equippedText = if (rewardState.auraEquipped) stringResource(R.string.aura_on) else stringResource(R.string.aura_off),
                    actionText = if (rewardState.auraEquipped) stringResource(R.string.deactivate_aura) else stringResource(R.string.activate_aura),
                    onAction = { onToggleAura(!rewardState.auraEquipped) }
                )
            }
            // ---------- SHINY ----------
            item {
                RewardCard(
                    emoji = RewardId.SHINY_SKIN.emoji,
                    title = stringResource(RewardId.SHINY_SKIN.nameRes),
                    description = stringResource(RewardId.SHINY_SKIN.descRes),
                    chanceText = stringResource(R.string.chance_percent_range, 7, "16-22"),
                    unlocked = rewardState.isUnlocked(RewardId.SHINY_SKIN),
                    equippedText = if (rewardState.shinyEquipped) stringResource(R.string.shiny_on) else stringResource(R.string.shiny_off),
                    actionText = if (rewardState.shinyEquipped) stringResource(R.string.deactivate_shiny) else stringResource(R.string.activate_shiny),
                    onAction = { onToggleShiny(!rewardState.shinyEquipped) }
                )
            }
            // ---------- DRAGONES 1, 2 y 3 ----------
            items(PetChoice.entries) { pet ->
                val unlocked = pet.isUnlocked(rewardState.unlockedRewards)
                val inUse = rewardState.selectedPetId == pet.petId
                val chance = when (pet) {
                    PetChoice.DRAGON_1 -> stringResource(R.string.pet_always_available)
                    PetChoice.DRAGON_2 -> stringResource(R.string.chance_percent_range, 3, "23-25")
                    PetChoice.DRAGON_3 -> stringResource(R.string.chance_percent_range, 3, "26-28")
                }
                PetCard(
                    pet = pet,
                    chanceText = chance,
                    unlocked = unlocked,
                    inUse = inUse,
                    onUse = { onSelectPet(pet.petId) }
                )
            }
            item {
                Text(
                    stringResource(R.string.rewards_no_reward_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun RewardCard(
    emoji: String,
    title: String,
    description: String,
    chanceText: String,
    unlocked: Boolean,
    equippedText: String,
    actionText: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, style = MaterialTheme.typography.headlineMedium)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(description, style = MaterialTheme.typography.bodySmall)
                    Text(chanceText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = if (unlocked) stringResource(R.string.reward_unlocked) else stringResource(R.string.reward_locked),
                    color = if (unlocked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(equippedText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Button(onClick = onAction, enabled = unlocked) { Text(actionText) }
            }
            if (!unlocked) {
                Text(
                    stringResource(R.string.reward_locked_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PetCard(
    pet: PetChoice,
    chanceText: String,
    unlocked: Boolean,
    inUse: Boolean,
    onUse: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (inUse) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when (pet) {
                        PetChoice.DRAGON_1 -> "🟢"
                        PetChoice.DRAGON_2 -> "🔴"
                        PetChoice.DRAGON_3 -> "🔵"
                    },
                    style = MaterialTheme.typography.headlineMedium
                )
                // Los textos ocupan TODO el ancho restante hasta el borde
                // derecho: en esta fila NO hay ningun boton que les robe
                // espacio (el boton va en su propia linea, debajo).
                Column(Modifier.weight(1f).fillMaxWidth()) {
                    Text(stringResource(pet.nameRes), fontWeight = FontWeight.Bold)
                    Text(chanceText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        when {
                            inUse -> stringResource(R.string.pet_in_use)
                            unlocked -> stringResource(R.string.pet_unlocked_hint)
                            else -> stringResource(R.string.pet_locked_hint)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // El boton va SIEMPRE debajo de los textos, al final de la
            // tarjeta y alineado a la derecha (misma zona de pulsacion
            // que los botones de Aura y Shiny): a la derecha de los
            // textos no cabia y deformaba la tarjeta hacia abajo.
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (inUse) {
                    OutlinedButton(onClick = {}, enabled = false) { Text(stringResource(R.string.pet_in_use_btn)) }
                } else {
                    Button(onClick = onUse, enabled = unlocked) { Text(stringResource(R.string.pet_use_btn, stringResource(pet.nameRes))) }
                }
            }
        }
    }
}
