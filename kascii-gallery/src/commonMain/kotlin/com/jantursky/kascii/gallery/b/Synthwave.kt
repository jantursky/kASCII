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
import com.jantursky.kascii.gallery.b.Star
import com.jantursky.kascii.gallery.b.TAU
import com.jantursky.kascii.gallery.b.frac
import com.jantursky.kascii.gallery.b.rngFor
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private const val COLS = 64
private const val ROWS = 24
private const val HORIZON = 13
private const val LOOP = 5f
private const val CYCLES = 4
private const val SUN_RADIUS = 15f

private fun Grid.sun(t: Float, top: Rgb, bottom: Rgb) {
    val cx = COLS / 2f
    for (y in 0 until HORIZON) for (x in 0 until COLS) {
        val dx = (x + 0.5f - cx) / SUN_RADIUS
        val dy = (y + 0.5f - (HORIZON - 1f)) * 2f / SUN_RADIUS
        if (dx * dx + dy * dy > 1f) continue
        val below = (y - (HORIZON - 6f)) / 6f
        val stripe = frac((y - t * CYCLES / LOOP * 2f) / 2f)
        if (below > 0f && stripe < below * 0.7f) continue
        put(x, y, '@', top.lerp(bottom, y / (HORIZON - 1f)))
    }
}

private fun Grid.ridge(color: Rgb) {
    var previous = HORIZON - 3
    for (x in 0 until COLS) {
        val h = HORIZON - 2 - (2.2f + 1.6f * sin(x * 0.31f) + 1.1f * sin(x * 0.83f + 1f)).roundToInt()
        val glyph = if (h < previous) '/' else if (h > previous) '\\' else '_'
        if (abs(x - COLS / 2) > SUN_RADIUS + 1f) put(x, h, glyph, color)
        previous = h
    }
}

private fun Grid.road(t: Float, color: Rgb) {
    val depthShift = frac(t * CYCLES / LOOP)
    for (k in 0 until 9) {
        val d = 1f + k + depthShift
        val row = HORIZON + ((ROWS - HORIZON - 0.5f) / d * 1.9f).toInt()
        if (row in HORIZON + 1 until ROWS) for (x in 0 until COLS) put(x, row, '-', color)
    }
    for (j in -9..9) for (row in HORIZON + 1 until ROWS) {
        val x = (COLS / 2f + j * (row - HORIZON) * 2.6f).roundToInt()
        put(x, row, if (j == 0) '|' else if (j < 0) '/' else '\\', color)
    }
}

/**
 * A retro sunset: a striped sun sinking behind a ridge and a perspective grid rushing towards the viewer. The
 * grid and the sun's stripes advance four steps per loop, so it closes every 5 seconds. [seed] places the
 * stars. The sun runs from [sunTop] to [sunBottom], the grid and the ridge use [grid].
 *
 * Ported from ascii.rest `synthwave` (MIT, Copyright (c) 2026 bas3line): the same sun, horizon and grid road,
 * re-implemented with a per-row color and a closed loop.
 */
public fun synthwavePiece(
    seed: Int = 0,
    sunTop: Rgb = Palette.Amber,
    sunBottom: Rgb = Palette.Red,
    grid: Rgb = Palette.Violet,
): AsciiPiece {
    val rng = rngFor(seed, 6)
    val stars = List(26) { Star(rng.int(COLS), rng.int(HORIZON - 3), 1 + rng.int(3), rng.float() * TAU) }
    return asciiPiece(
        name = "synthwave",
        category = "effects",
        cols = COLS,
        rows = ROWS,
        fps = 20,
        still = 1f,
        loop = LOOP,
        note = "a striped sun over a ridge and a grid rushing towards you",
    ) { t, canvas ->
        for (s in stars) {
            val v = sin(TAU * s.rate * t / LOOP + s.phase)
            canvas.put(s.x, s.y, if (v > 0.4f) '*' else if (v > -0.4f) '+' else '.')
        }
        canvas.sun(t, sunTop, sunBottom)
        canvas.ridge(grid)
        canvas.road(t, grid)
    }
}

/**
 * A retro sunset over a rushing perspective grid. [seed] places the stars, [sunTop] and [sunBottom] color the
 * sun and [grid] the road. Ported from ascii.rest `synthwave`.
 */
@Composable
public fun AsciiSynthwave(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    sunTop: Rgb = Palette.Amber,
    sunBottom: Rgb = Palette.Red,
    grid: Rgb = Palette.Violet,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A striped retro sun setting over a glowing perspective grid",
) {
    val piece = remember(seed, sunTop, sunBottom, grid) { synthwavePiece(seed, sunTop, sunBottom, grid) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
