package com.dragongotchi.app.ui.components

import androidx.compose.ui.res.stringResource
import com.dragongotchi.app.R

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dragongotchi.app.data.local.PetStateEntity
import com.dragongotchi.app.domain.DragonStage
import com.dragongotchi.app.domain.LevelingSystem

/** Resuelve el drawable del aura (reward_aura.xml hoy, reward_aura.png manana). */
@Composable
fun auraDrawableRes(): Int {
    val context = LocalContext.current
    return context.resources.getIdentifier("reward_aura", "drawable", context.packageName)
}

/**
 * Nombre del asset de sprite para (nivel, dragon, shiny), siguiendo el
 * contrato de tools/generate_sprites.py y los PNG de drawable-nodpi:
 *  - Dragon 1 normal : stage_03_cria_dragon.png
 *  - Dragon 1 shiny  : shiny_stage_03_cria_dragon.png
 *  - Dragon 2/3      : pet2_stage_03.png / pet3_stage_03.png
 *  - Shiny 2/3       : shiny_pet2_stage_03.png / shiny_pet3_stage_03.png
 * Son PNG de 1056x1056 (264 dp x 4) con fondo transparente.
 */
fun dragonSpriteAssetName(level: Int, selectedPetId: Int, shinyEquipped: Boolean): String {
    val stage = DragonStage.forLevel(level)
    val base = if (selectedPetId == 1) stage.drawableName else "pet${selectedPetId}_stage_%02d".format(stage.level)
    return if (shinyEquipped) "shiny_$base" else base
}

/**
 * Resuelve el drawable del sprite REAL (ya recolorado en el propio PNG:
 * sin filtros de color en tiempo de ejecucion). Con caidas en cascada:
 * shiny -> normal del dragon -> Dragon 1 -> huevo inicial.
 */
@Composable
@DrawableRes
fun dragonSpriteDrawableRes(level: Int, selectedPetId: Int = 1, shinyEquipped: Boolean = false): Int {
    val context = LocalContext.current
    fun find(name: String): Int = context.resources.getIdentifier(name, "drawable", context.packageName)
    val stage = DragonStage.forLevel(level)
    val candidates = listOf(
        dragonSpriteAssetName(level, selectedPetId, shinyEquipped),
        dragonSpriteAssetName(level, selectedPetId, shinyEquipped = false),
        stage.drawableName,
        "stage_01_huevo_mistico"
    )
    return candidates.firstNotNullOfOrNull { name -> find(name).takeIf { it != 0 } } ?: 0
}

/** Resuelve R.drawable.stage_XX_* a partir del nombre declarado en DragonStage. */
@Composable
@DrawableRes
fun dragonDrawableRes(level: Int): Int {
    val context = LocalContext.current
    val stage = DragonStage.forLevel(level)
    val resId = context.resources.getIdentifier(stage.drawableName, "drawable", context.packageName)
    // Fallback al huevo si aun no has sustituido los placeholders por tus sprites finales
    return if (resId != 0) resId else
        context.resources.getIdentifier("stage_01_huevo_mistico", "drawable", context.packageName)
}

/**
 * Vista principal del Tamagotchi: sprite segun nivel (carpeta drawable),
 * animacion de "respiracion/salto", nombre, nivel y barra de EXP.
 * Para usar tus sprites finales basta con sustituir los XML de
 * res/drawable/stage_XX_*.xml por tus PNG/WebP con el MISMO nombre.
 */
/** Marco unico donde conviven aura (detras) y dragon (delante). */
val PetStageSize = 264.dp

/** El aura se ve con un 40% de transparencia -> alpha 0.60. */
const val AURA_ALPHA = 0.60f

@Composable
fun TamagotchiView(
    pet: PetStateEntity,
    modifier: Modifier = Modifier,
    auraEquipped: Boolean = false,
    shinyEquipped: Boolean = false,
    selectedPetId: Int = 1
) {
    val stage = DragonStage.forLevel(pet.level)
    val transition = rememberInfiniteTransition(label = "pet_bounce")
    val bounce by transition.animateFloat(
        initialValue = 0f, targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "bounce"
    )
    val needed = LevelingSystem.expNeededForLevel(pet.level)
    val progress = LevelingSystem.progressInLevel(pet.level, pet.currentExp)
    val auraRes = auraDrawableRes()

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ================================================================
        // ESCENARIO DEL DRAGON (un unico marco, dos imagenes compartidas)
        //  - Hueco fijo de PetStageSize: dentro conviven aura y dragon.
        //  - CAPA TRASERA: el PNG del aura (reward_aura), con un 40% de
        //    transparencia (alpha = 0.60), ocupando TODO el marco.
        //  - CAPA DELANTERA: el PNG del dragon, siempre ENCIMA del aura.
        // Al declarar primero el aura y despues el dragon, Compose los
        // apila en ese orden (z-order por orden de declaracion).
        // ================================================================
        Box(
            modifier = Modifier
                .size(PetStageSize)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF3B2A5D),
                            Color(0xFF241536),
                            Color(0xFF120B1E)
                        )
                    )
                )
                .border(3.dp, Color(0xFFC99612), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            // CAPA 1 (detras): aura con 40% de transparencia
            if (auraEquipped && auraRes != 0) {
                Image(
                    painter = painterResource(id = auraRes),
                    contentDescription = "Aura equipada",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = AURA_ALPHA }
                )
            }
            // CAPA 2 (delante): el dragon, siempre por encima del aura.
            // El PNG ya es el del dragon elegido y, si el shiny esta
            // equipado, su version dorada: sin filtros de color.
            Image(
                painter = painterResource(
                    id = dragonSpriteDrawableRes(pet.level, selectedPetId, shinyEquipped)
                ),
                contentDescription = stringResource(stage.nameRes),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = bounce }
                    .padding(18.dp)
            )
            if (shinyEquipped) {
                Text(
                    text = "✨",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                )
                Text(
                    text = "✨",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                )
            }
        }
        if (shinyEquipped || selectedPetId != 1 || auraEquipped) {
            Text(
                text = buildString {
                    if (auraEquipped) append(stringResource(R.string.pet_aura_active) + "  ")
                    if (shinyEquipped) append(stringResource(R.string.pet_shiny_active) + "  ")
                    if (selectedPetId == 2) append("🔴 " + stringResource(R.string.pet_dragon2_name))
                    if (selectedPetId == 3) append("🔵 " + stringResource(R.string.pet_dragon3_name))
                }.trim(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = pet.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        // Insignia de nivel: placa oscura con borde dorado (estilo 8-bit)
        Surface(
            color = Color(0xFF241536),
            contentColor = Color(0xFFFFC107),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.border(2.dp, Color(0xFFC99612), RoundedCornerShape(4.dp))
        ) {
            Text(
                text = stringResource(R.string.pet_level_badge, pet.level, stringResource(stage.nameRes).uppercase()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFC107),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
        Text(
            text = stringResource(stage.descRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        if (pet.level >= LevelingSystem.MAX_LEVEL) {
            Text(
                text = stringResource(R.string.pet_max_level),
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF8F00)
            )
        } else {
            // Barra de EXP 8-bit: marco oscuro, relleno dorado->brasa
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF241536))
                    .border(2.dp, Color(0xFF120B1E), RoundedCornerShape(3.dp))
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFFC107), Color(0xFFFF8F00), Color(0xFFE65100))
                            )
                        )
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.pet_exp_progress, pet.currentExp, needed, (needed - pet.currentExp) * LevelingSystem.STEPS_PER_EXP),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
