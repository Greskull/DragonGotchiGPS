package com.dragongotchi.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ============================================================
 *  IDENTIDAD VISUAL: fantasia medieval / mundo de dragones 8-bit
 * ============================================================
 * TIPOGRAFIA: sin anadir ningun paquete de fuentes, la familia del
 * sistema que mejor evoca lo gotico/medieval es FontFamily.Serif
 * (la serif clasica de Android: trazo grueso, remates marcados).
 * Se aplica a TODOS los estilos de texto (titulos y cuerpo) con
 * pesos fuertes y un ligero tracking en los titulares, a modo de
 * rotulo de pergamino / letrero de taberna.
 * (FontFamily.Cursive seria la otra candidata "caligrafica", pero
 * varia mucho entre fabricantes y se lee peor en pantallas densas.)
 *
 * PALETA: adios al blanco y verde plano. Pergamino envejecido,
 * morado arcano, oro antiguo, sangre de dragon y brasa, con un
 * modo oscuro de castillo a medianoche. Formas casi rectas
 * (2-8 dp) para el toque pixel/8-bit en tarjetas y botones.
 */

// ---- Paleta base ----
val Parchment = Color(0xFFF3E5C0)
val ParchmentSurface = Color(0xFFFAF0D7)
val ParchmentVariant = Color(0xFFE7D3A1)
val InkBrown = Color(0xFF2B2118)
val ArcanePurple = Color(0xFF5E2B97)
val ArcanePurpleDark = Color(0xFF311B92)
val AncientGold = Color(0xFFC99612)
val BrightGold = Color(0xFFFFC107)
val DragonBlood = Color(0xFF8E2434)
val EmberOrange = Color(0xFFE65100)
val MidnightCastle = Color(0xFF16121F)
val CastleSurface = Color(0xFF231A33)
val CastleVariant = Color(0xFF372A52)
val ParchmentOnDark = Color(0xFFF5E9C9)

// Compatibilidad con nombres usados en otras pantallas
val DragonGold = BrightGold
val DragonFire = EmberOrange

/**
 * Fondo "reino de dragones" de la pantalla principal, compartido por
 * TODAS las pantallas (Entrenar, Historial, Recompensas, Evoluciones,
 * Ajustes...) para que la app entera tenga la misma identidad visual:
 * pergamino que cae a morado arcano (o castillo a medianoche en modo
 * oscuro, via el tema). Uso: Modifier.fillMaxSize().dragonKingdomBackground()
 */
@Composable
fun Modifier.dragonKingdomBackground(): Modifier = this.background(
    Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            MaterialTheme.colorScheme.background
        )
    )
)

private val LightColors = lightColorScheme(
    primary = ArcanePurple,
    onPrimary = ParchmentSurface,
    primaryContainer = Color(0xFFDCCBF5),
    onPrimaryContainer = ArcanePurpleDark,
    secondary = AncientGold,
    onSecondary = InkBrown,
    secondaryContainer = Color(0xFFF5DE9C),
    onSecondaryContainer = InkBrown,
    tertiary = EmberOrange,
    onTertiary = ParchmentSurface,
    background = Parchment,
    onBackground = InkBrown,
    surface = ParchmentSurface,
    onSurface = InkBrown,
    surfaceVariant = ParchmentVariant,
    onSurfaceVariant = Color(0xFF5D4037),
    error = DragonBlood,
    onError = ParchmentSurface
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB39DDB),
    onPrimary = MidnightCastle,
    primaryContainer = CastleVariant,
    onPrimaryContainer = ParchmentOnDark,
    secondary = BrightGold,
    onSecondary = InkBrown,
    secondaryContainer = Color(0xFF4A3A12),
    onSecondaryContainer = ParchmentOnDark,
    tertiary = Color(0xFFFF8A65),
    onTertiary = MidnightCastle,
    background = MidnightCastle,
    onBackground = ParchmentOnDark,
    surface = CastleSurface,
    onSurface = ParchmentOnDark,
    surfaceVariant = CastleVariant,
    onSurfaceVariant = Color(0xFFD7C6F0),
    error = Color(0xFFEF9A9A),
    onError = MidnightCastle
)

/** Serif medieval para todo el texto (sin paquetes de fuentes nuevos). */
private val MedievalFont = FontFamily.Serif

private val MedievalTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = MedievalFont, fontWeight = FontWeight.Bold,
        fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = 0.5.sp
    ),
    displayMedium = TextStyle(
        fontFamily = MedievalFont, fontWeight = FontWeight.Bold,
        fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.5.sp
    ),
    displaySmall = TextStyle(
        fontFamily = MedievalFont, fontWeight = FontWeight.Bold,
        fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.5.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = MedievalFont, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.5.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = MedievalFont, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.5.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = MedievalFont, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = MedievalFont, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.3.sp
    ),
    titleMedium = TextStyle(
        fontFamily = MedievalFont, fontWeight = FontWeight.Bold,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.3.sp
    ),
    titleSmall = TextStyle(
        fontFamily = MedievalFont, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.3.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = MedievalFont, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = MedievalFont, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = MedievalFont, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = MedievalFont, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.3.sp
    ),
    labelMedium = TextStyle(
        fontFamily = MedievalFont, fontWeight = FontWeight.Bold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = MedievalFont, fontWeight = FontWeight.Bold,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp
    )
)

/** Esquinas casi rectas: el toque 8-bit / pixel en botones y tarjetas. */
private val PixelShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp)
)

@Composable
fun DragonGotchiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MedievalTypography,
        shapes = PixelShapes,
        content = content
    )
}
