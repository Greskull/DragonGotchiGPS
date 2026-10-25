package com.dragongotchi.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * ============================================================
 *  MARCADOR DRAGON DEL MAPA — SOLO EL SPRITE, FONDO TRANSPARENTE
 * ============================================================
 * El sprite del Tamagotchi (sea el PNG de 1056 px o cualquier otro
 * que pongas en drawable) se rasteriza UNA VEZ a un bitmap de
 * exactamente [DRAGON_MARKER_SIZE_DP] x [DRAGON_MARKER_SIZE_DP] dp
 * con fondo totalmente transparente: sin circulo blanco, sin borde,
 * sin sombra y sin aura. Solo el dragon, como un icono "limpio"
 * flotando sobre tu posicion GPS.
 *
 * El aspecto (dragon elegido, shiny o no y nivel) ya viene resuelto
 * en el propio PNG que se pasa como [spriteRes] (ver
 * dragonSpriteDrawableRes en TamagotchiView.kt): aqui solo se escala.
 */
const val DRAGON_MARKER_SIZE_DP = 48

fun createDragonMarkerDrawable(
    context: Context,
    @DrawableRes spriteRes: Int,
    sizeDp: Int = DRAGON_MARKER_SIZE_DP
): Drawable {
    val sizePx = (sizeDp * context.resources.displayMetrics.density).roundToInt()
        .coerceAtLeast(24)
    // Bitmap transparente: no se pinta ningun fondo, solo el sprite.
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Sprite escalado a la caja fija conservando su proporcion
    // (fit-center). El dibujo original puede ser enorme: se dibuja
    // dentro de esta caja y el bitmap resultante siempre mide
    // sizePx x sizePx, con las esquinas transparentes.
    val sprite = ContextCompat.getDrawable(context, spriteRes)
    if (sprite != null) {
        val iw = sprite.intrinsicWidth.takeIf { it > 0 } ?: sizePx
        val ih = sprite.intrinsicHeight.takeIf { it > 0 } ?: sizePx
        val scale = min(sizePx / iw.toFloat(), sizePx / ih.toFloat())
        val drawW = iw * scale
        val drawH = ih * scale
        val left = (sizePx - drawW) / 2f
        val top = (sizePx - drawH) / 2f
        sprite.setBounds(
            left.roundToInt(),
            top.roundToInt(),
            (left + drawW).roundToInt(),
            (top + drawH).roundToInt()
        )
        sprite.draw(canvas)
    }

    return BitmapDrawable(context.resources, bitmap)
}

/** Marcador circular sencillo de color (inicio de ruta / fin en el historial). */
fun createCircleMarkerDrawable(
    context: Context,
    fillColor: Int,
    sizeDp: Int = 18
): Drawable {
    val sizePx = (sizeDp * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(12)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 2f, paint)
    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = sizePx * 0.12f
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - border.strokeWidth, border)
    return BitmapDrawable(context.resources, bitmap)
}
