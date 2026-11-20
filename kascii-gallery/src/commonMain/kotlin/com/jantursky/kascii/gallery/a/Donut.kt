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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val COLS = 44
private const val ROWS = 22
private const val TUBE = 1f
private const val RING = 2f
private const val EYE = 6f
private const val STEP_THETA = 0.07f
private const val STEP_PHI = 0.03f
private const val LOOP = 10f

private val DonutRamp = Charset.Ramp("donut", ".,-~:;=!*#$@")

/**
 * A torus turning on two axes, lit from the upper left, drawn with a depth buffer. The tube is sampled in two
 * angles and each sample is shaded by its surface normal. One loop is two turns around one axis and one around
 * the other.
 *
 * Ported from ascii.rest `donut` (MIT, bas3line), itself after Andy Sloane's donut.c: same projection and surface
 * normal, re-written with a typed ramp and per-cell color.
 */
public fun donutPiece(
    from: Rgb = AKit.Violet,
    to: Rgb = AKit.Red,
    charset: Charset.Ramp = DonutRamp,
): AsciiPiece {
    val scale = (COLS - 2) / 2f / ((TUBE + RING) / sqrt(EYE * EYE - (TUBE + RING) * (TUBE + RING)))
    val norm = sqrt(0.4f * 0.4f + 2f)
    val light = floatArrayOf(-0.4f / norm, 1f / norm, -1f / norm)
    return asciiPiece(
        name = "donut",
        category = "shapes",
        cols = COLS,
        rows = ROWS,
        still = 1.5f,
        loop = LOOP,
        note = "a lit torus turning on two axes",
    ) { t, grid ->
        val a = 1f + AKit.TAU * t / (LOOP / 2)
        val b = 1f + AKit.TAU * t / LOOP
        val sa = sin(a)
        val ca = cos(a)
        val sb = sin(b)
        val cb = cos(b)
        val depth = FloatArray(COLS * ROWS)
        var th = 0f
        while (th < AKit.TAU) {
            val ct = cos(th)
            val st = sin(th)
            var ph = 0f
            while (ph < AKit.TAU) {
                val cp = cos(ph)
                val sp = sin(ph)
                val h = RING + TUBE * ct
                val x = h * (cb * cp + sa * sb * sp) - TUBE * st * ca * sb
                val y = h * (sb * cp - sa * cb * sp) + TUBE * st * ca * cb
                val ooz = 1f / (EYE + ca * h * sp + TUBE * st * sa)
                val col = (COLS / 2f + scale * ooz * x).toInt()
                val row = (ROWS / 2f - scale * 0.5f * ooz * y).toInt()
                if (grid.contains(col, row) && ooz > depth[row * COLS + col]) {
                    depth[row * COLS + col] = ooz
                    val nx = ct * (cb * cp + sa * sb * sp) - st * ca * sb
                    val ny = ct * (sb * cp - sa * cb * sp) + st * ca * cb
                    val nz = ca * ct * sp + st * sa
                    val tone = (nx * light[0] + ny * light[1] + nz * light[2]).coerceAtLeast(0f)
                    grid.put(col, row, charset.glyphFor(tone), from.lerp(to, tone))
                }
                ph += STEP_PHI
            }
            th += STEP_THETA
        }
    }
}

/**
 * A lit torus turning on two axes.
 * Ported from ascii.rest `donut`.
 *
 * @param from the color of the shadowed side.
 * @param to the color of the lit side.
 * @param charset the glyphs from the shadow to the highlight.
 */
@Composable
public fun Donut(
    modifier: Modifier = Modifier,
    from: Rgb = AKit.Violet,
    to: Rgb = AKit.Red,
    charset: Charset.Ramp = DonutRamp,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A shaded torus, a ring, turning in space",
) {
    val piece = remember(from, to, charset) { donutPiece(from, to, charset) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
