package com.jantursky.kascii.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.AsciiPieceView
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.Noise
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.asciiPiece
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

private const val COLS = 72
private const val ROWS = 26
private const val GROUND = 22
private const val LOOP = 12f
private const val CHIMNEY_ROW = 18 // smoke starts above the roof peak

private val HOUSE =
    listOf(
        "   ^   ",
        "  / \\  ",
        " /___\\ ",
        " | # | ",
        " |___| ",
    )

/**
 * A quiet village at night: twinkling stars, a crescent moon, rolling hills, cottages with flickering windows and a
 * thread of chimney smoke. Houses and pines stand where the seed puts them.
 */
public fun hilltopNightPiece(
    seed: Int = 0,
    window: Rgb = AKit.Amber,
    moon: Rgb = AKit.Amber,
): AsciiPiece {
    val rng = AKit.rng(seed, 2)
    val houses = List(4) { 4 + it * 17 + rng.int(6) }
    val pines = List(6) { 2 + it * 12 + rng.int(8) }
    return asciiPiece(
        name = "hilltop night",
        category = "scenes",
        cols = COLS,
        rows = ROWS,
        fps = 12,
        still = 3f,
        loop = LOOP,
        note = "a village under a crescent moon, windows flickering",
    ) { t, grid ->
        paintStars(grid, seed, t)
        paintMoon(grid, moon)
        paintHills(grid, seed)
        pines.forEach { paintPine(grid, it) }
        houses.forEachIndexed { i, x -> paintHouse(grid, x, i, t, window) }
        paintSmoke(grid, houses[0] + 3, t)
        for (x in 0 until COLS) {
            grid.put(x, GROUND, '=')
            grid.put(x, GROUND + 1, ':')
            grid.put(x, GROUND + 2, '.')
        }
    }
}

private fun paintStars(
    grid: Grid,
    seed: Int,
    t: Float,
) {
    for (y in 0 until 13) {
        for (x in 0 until COLS) {
            val h = Noise.hash(x, y, 0, seed)
            if (h < 0.972f) continue
            val glint = sin(AKit.TAU * (3f * t / LOOP + h * 40f))
            val glyph =
                if (glint > 0.8f) {
                    '+'
                } else if (glint > -0.5f) {
                    '*'
                } else {
                    '.'
                }
            grid.put(x, y, glyph, if (h > 0.99f) AKit.Blue else null)
        }
    }
}

private fun paintMoon(
    grid: Grid,
    color: Rgb,
) {
    for (y in 1..9) {
        for (x in 50..64) {
            val inside = sqrt((x - 57f) * (x - 57f) * 0.25f + (y - 5f) * (y - 5f)) <= 3.6f
            val bite = sqrt((x - 60.5f) * (x - 60.5f) * 0.25f + (y - 4.6f) * (y - 4.6f)) <= 3.4f
            if (inside && !bite) grid.put(x, y, '@', color)
        }
    }
}

private fun paintHills(
    grid: Grid,
    seed: Int,
) {
    for (x in 0 until COLS) {
        val far = 15 - (Noise.fbm(x * 0.05f, 1f, 0f, 3, seed) * 6f).toInt()
        val near = 19 - (Noise.fbm(x * 0.08f, 9f, 0f, 3, seed + 1) * 4f).toInt()
        for (y in far until GROUND) grid.put(x, y, '.', AKit.Violet)
        for (y in near until GROUND) grid.put(x, y, ':', AKit.Blue)
    }
}

private fun paintPine(
    grid: Grid,
    x: Int,
) {
    for (k in 0..3) for (dx in -k..k) grid.put(x + dx, GROUND - 5 + k, '^', AKit.Green)
    grid.put(x, GROUND - 1, '|')
}

private fun paintHouse(
    grid: Grid,
    x: Int,
    index: Int,
    t: Float,
    window: Rgb,
) {
    val lit = Noise.hash(index, floor(t * 2f).toInt(), 3, 0) > 0.15f
    HOUSE.forEachIndexed { row, line ->
        line.forEachIndexed { col, ch ->
            val y = GROUND - HOUSE.size + row
            when {
                ch == '#' -> grid.put(x + col, y, if (lit) '#' else ' ', window)
                else -> grid.put(x + col, y, ch)
            }
        }
    }
}

private fun paintSmoke(
    grid: Grid,
    x: Int,
    t: Float,
) {
    for (k in 0..5) {
        val sway = sin(AKit.TAU * (2f * t / LOOP) + k * 0.9f) * 1.5f
        grid.put(
            x + (sway + k * 0.3f).toInt(),
            CHIMNEY_ROW - 2 - k,
            if (k < 2) {
                'o'
            } else if (k < 4) {
                ':'
            } else {
                '.'
            },
        )
    }
}

/**
 * A village at night: stars, a crescent moon, hills, cottages with flickering windows and chimney smoke.
 *
 * @param seed places the houses and pines and the stars.
 * @param window the color of the lit windows.
 * @param moon the color of the moon.
 */
@Composable
public fun HilltopNight(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    window: Rgb = AKit.Amber,
    moon: Rgb = AKit.Amber,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A village at night under a crescent moon, with lit windows and chimney smoke",
) {
    val piece = remember(seed, window, moon) { hilltopNightPiece(seed, window, moon) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
