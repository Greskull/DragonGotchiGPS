package com.dragongotchi.app.ui.screens.home

import androidx.compose.ui.res.stringResource
import com.dragongotchi.app.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragongotchi.app.data.local.PetStateEntity
import com.dragongotchi.app.domain.RewardState
import com.dragongotchi.app.domain.LevelingSystem
import com.dragongotchi.app.domain.StatsCalculator
import com.dragongotchi.app.ui.components.StatCard
import com.dragongotchi.app.ui.components.TamagotchiView
import com.dragongotchi.app.ui.theme.AncientGold
import com.dragongotchi.app.ui.theme.ArcanePurple
import com.dragongotchi.app.ui.theme.ArcanePurpleDark
import com.dragongotchi.app.ui.theme.BrightGold
import com.dragongotchi.app.ui.theme.DragonBlood
import com.dragongotchi.app.ui.theme.dragonKingdomBackground

@Composable
fun HomeScreen(
    pet: PetStateEntity,
    rewardState: RewardState = RewardState(),
    isTracking: Boolean,
    onStartClicked: () -> Unit,
    onRewardsClicked: () -> Unit,
    onEvolutionClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Fondo de reino compartido por todas las pantallas
            // (pergamino -> morado arcano; ver Theme.kt)
            .dragonKingdomBackground()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Nombre de la app como placa heraldica: blasón morado arcano
        // con borde de oro antiguo y letras en degradado dorado.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(ArcanePurpleDark, ArcanePurple, DragonBlood)
                    )
                )
                .border(2.dp, AncientGold, RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚔️ DragonGotchiGPS ⚔️",
                style = MaterialTheme.typography.headlineMedium.copy(
                    brush = Brush.linearGradient(
                        listOf(BrightGold, Color(0xFFFFECB3), BrightGold)
                    )
                ),
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TamagotchiView(
            pet = pet,
            modifier = Modifier.fillMaxWidth(),
            auraEquipped = rewardState.auraEquipped,
            shinyEquipped = rewardState.shinyEquipped,
            selectedPetId = rewardState.selectedPetId
        )

        // Totales de por vida
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                label = stringResource(R.string.home_lifetime_distance),
                value = StatsCalculator.formatDistanceKm(pet.lifetimeDistanceM),
                unit = "km",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.home_lifetime_steps),
                value = "%,d".format(pet.lifetimeSteps),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                label = stringResource(R.string.home_sessions),
                value = pet.totalSessions.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.home_total_exp),
                value = pet.totalExp.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onStartClicked,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = if (isTracking) stringResource(R.string.home_view_ongoing) else stringResource(R.string.home_start),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // "Historial de rutas" ya vive en su pestana inferior: aqui va Recompensas.
            // Si hay una recompensa recien desbloqueada sin ver, boton con «!» rojo.
            // Ambos botones en UNA sola linea: padding horizontal reducido
            // (6 dp), fuente 13 sp y maxLines = 1 en los dos textos.
            OutlinedButton(
                onClick = onRewardsClicked,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Text(stringResource(R.string.home_rewards), fontSize = 13.sp, maxLines = 1)
                if (rewardState.hasUnseenReward) {
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD32F2F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = onEvolutionClicked,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Text(stringResource(R.string.home_evolutions), fontSize = 13.sp, maxLines = 1)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.home_rule, LevelingSystem.STEPS_PER_EXP, LevelingSystem.expNeededForLevel(1)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
