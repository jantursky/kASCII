package com.jantursky.kascii.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.AsciiPieceView
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.Charset
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.asciiPiece
import kotlin.math.exp
import kotlin.math.sqrt

private const val COLS = 48
private const val ROWS = 22
private const val LOOP = 1.2f
private const val COLS_PER_UNIT = 16f
private const val ROWS_PER_UNIT = 8f

private val HeartRamp = Charset.Ramp("heart", " .:-=+*#%@")

/**
 * A heart filled from the implicit curve `(x² + y² - 1)³ - x²y³ = 0`, shaded by how deep a point sits inside it
 * with a highlight on the upper left. It beats like a pulse: a strong thump, a short one, then rest.
 */
public fun heartPiece(
    color: Rgb = AKit.Red,
    charset: Charset.Ramp = HeartRamp,
): AsciiPiece =
    asciiPiece(
        name = "heart",
        category = "shapes",
        cols = COLS,
        rows = ROWS,
        fps = 30,
        still = 0.05f,
        loop = LOOP,
        note = "a heart beating lub-dub",
    ) { t, grid ->
        val p = AKit.frac(t / LOOP)
        val scale = 1f + 0.08f * (exp(-14f * p) + 0.6f * exp(-14f * AKit.frac(p - 0.2f)))
        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                val x = (col + 0.5f - COLS / 2f) / COLS_PER_UNIT / scale
                val y = (ROWS / 2f - row - 0.5f) / ROWS_PER_UNIT / scale + 0.1f
                val q = x * x + y * y - 1f
                val f = q * q * q - x * x * y * y * y
                if (f > 0f) continue
                val shine = 0.35f * exp(-((x + 0.5f) * (x + 0.5f) + (y - 0.6f) * (y - 0.6f)) * 8f)
                val tone = 0.2f + 0.8f * sqrt(-f).coerceAtMost(1f) + shine
                grid.put(col, row, charset.glyphFor(tone), color)
            }
        }
    }

/**
 * A heart that beats like a pulse.
 *
 * @param color the heart.
 * @param charset the glyphs from the rim to the highlight.
 */
@Composable
public fun BeatingHeart(
    modifier: Modifier = Modifier,
    color: Rgb = AKit.Red,
    charset: Charset.Ramp = HeartRamp,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A heart beating",
) {
    val piece = remember(color, charset) { heartPiece(color, charset) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
