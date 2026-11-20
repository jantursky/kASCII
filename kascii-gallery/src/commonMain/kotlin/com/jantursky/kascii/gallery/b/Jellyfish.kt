package com.jantursky.kascii.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.AsciiPieceView
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.asciiPiece
import com.jantursky.kascii.gallery.b.Palette
import com.jantursky.kascii.gallery.b.TAU
import com.jantursky.kascii.gallery.b.ease
import com.jantursky.kascii.gallery.b.frac
import com.jantursky.kascii.gallery.b.rngFor
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val COLS = 48
private const val ROWS = 26
private const val LOOP = 9.6f
private const val STROKES = 4
private const val SQUEEZE_SHARE = 0.28f
private const val BELL_GLYPHS = " .:=+*#%@"

private class Rim(
    val x: Float,
    val y: Float,
    val half: Float,
    val squeeze: Float,
)

private class Speck(
    val x: Float,
    val y: Float,
    val lanes: Int,
    val wobble: Float,
)

/** Where the bell's rim sits at [t]: it squeezes four times per loop and lurches up with each stroke. */
private fun rimAt(t: Float): Rim {
    val p = frac(t / LOOP * STROKES)
    val squeeze =
        if (p < SQUEEZE_SHARE) ease(p / SQUEEZE_SHARE) else 1f - ease((p - SQUEEZE_SHARE) / (1f - SQUEEZE_SHARE))
    val lift = if (p < 0.4f) ease(p / 0.4f) else 1f - ease((p - 0.4f) / 0.6f)
    return Rim(
        x = COLS / 2f + 1.5f * sin(TAU * t / LOOP),
        y = 11f - 0.8f * squeeze - 1.4f * lift,
        half = (13f - 2.6f * squeeze) * (1f - 0.3f * squeeze),
        squeeze = squeeze,
    )
}

private fun Grid.bell(
    rim: Rim,
    color: Rgb,
) {
    val height = 6.5f + rim.squeeze
    val rimRow = rim.y.toInt()
    for (r in 0 until rimRow) {
        val s = (rim.y - (r + 0.5f)) / height
        if (s < 0f || s >= 1f) continue
        val width = rim.half * sqrt(1f - s * s)
        for (x in 0 until COLS) {
            val u = (x + 0.5f - rim.x) / width
            if (abs(u) >= 1f) continue
            val depth = sqrt(1f - u * u) * sqrt(1f - s * s)
            val glyph =
                when {
                    depth >= 0.2f -> BELL_GLYPHS[(1 + (0.15f + 0.65f * (1f - depth) * (1f - depth)) * 7f).toInt()]
                    abs(u) > 0.8f -> if (u < 0f) '(' else ')'
                    else -> '-'
                }
            put(x, r, glyph, color)
        }
    }
    for (x in 0 until COLS) {
        val u = (x + 0.5f - rim.x) / rim.half
        if (abs(u) < 1f) put(x, rimRow, if (x % 2 == 0) '~' else '\'', color)
    }
}

/** One tentacle: every row below the rim hangs from where the rim was a moment earlier, so it trails late. */
private fun Grid.strand(
    t: Float,
    rim: Rim,
    u: Float,
    length: Float,
    index: Int,
    color: Rgb,
) {
    val top = rim.y.toInt() + 1
    var previous = Float.NaN
    for (r in top until ROWS) {
        val d = (r - top + 1).toFloat()
        if (d >= length) break
        val was = rimAt(t - d * 0.06f)
        val sway = (0.05f + d * 0.013f) * d * sin(0.5f * d - TAU * 4f * t / LOOP + index * 1.9f)
        val x = was.x + u * was.half * (1f - 0.01f * d) + sway
        val lean = if (previous.isNaN()) 0f else x - previous
        val glyph =
            when {
                d > length * 0.85f -> '.'
                lean > 0.6f -> '\\'
                lean < -0.6f -> '/'
                else -> '|'
            }
        put(x.roundToInt(), r, glyph, color)
        previous = x
    }
}

/**
 * A moon jelly swimming up through dark water. The bell squeezes and relaxes, the tentacles follow the rim a
 * beat late and [specks] of plankton drift down past it. The seed places the plankton.
 *
 * Ported from ascii.rest `jellyfish` (MIT, Copyright (c) 2026 bas3line): same pulse, lagging tentacles and
 * shaded dome, re-implemented with a per-cell color.
 */
public fun jellyfishPiece(
    seed: Int = 0,
    color: Rgb = Palette.Violet,
    specks: Int = 30,
): AsciiPiece {
    val rng = rngFor(seed, 1)
    val plankton = List(specks) { Speck(rng.float() * COLS, rng.float() * ROWS, 1 + rng.int(2), rng.float() * TAU) }
    return asciiPiece(
        name = "jellyfish",
        category = "creatures",
        cols = COLS,
        rows = ROWS,
        fps = 24,
        still = 1.1f,
        loop = LOOP,
        note = "a jellyfish pulsing upward, its tentacles trailing late",
    ) { t, grid ->
        for (s in plankton) {
            val y = floor((s.y + s.lanes * ROWS * t / LOOP) % ROWS).toInt()
            val x = (s.x + 0.6f * sin(TAU * 3f * t / LOOP + s.wobble)).roundToInt()
            grid.put(x, y, if (s.lanes > 1) '\'' else '.')
        }
        val rim = rimAt(t)
        for (i in 0 until 8) {
            val u = -0.92f + 1.84f * i / 7f
            grid.strand(t, rim, u, 8.5f + 3.2f * (1f - abs(u)) + (i * 3 % 5) * 0.8f, i, color)
        }
        for (i in 0..1) {
            val u = if (i == 0) -0.16f else 0.16f
            grid.strand(t, rim, u, 10f, 8 + i, color)
        }
        grid.bell(rim, color)
    }
}

/**
 * A jellyfish pulsing through dark water, its tentacles trailing a beat behind. Ported from ascii.rest
 * `jellyfish`. [seed] places the drifting plankton, [color] tints the animal.
 */
@Composable
public fun AsciiJellyfish(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    color: Rgb = Palette.Violet,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A jellyfish pulsing through dark water",
) {
    val piece = remember(seed, color) { jellyfishPiece(seed, color) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
