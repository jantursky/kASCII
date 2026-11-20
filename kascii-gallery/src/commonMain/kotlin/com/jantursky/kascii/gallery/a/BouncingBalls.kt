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
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

private const val COLS = 64
private const val ROWS = 22
private const val LOOP = 12f
private const val RAMP = " .:-=+*#%@"

private val RADII = floatArrayOf(3f, 2.3f, 1.7f, 2.6f, 2f)
private val HOPS = intArrayOf(4, 6, 8, 5, 7)
private val DefaultColors = listOf(AKit.Red, AKit.Amber, AKit.Green, AKit.Blue, AKit.Violet)

/**
 * Shaded rubber balls hopping on a floor. Each ball follows a parabola, flattens as it touches down and leaves a
 * shadow that shrinks with its height. A ball hops a whole number of times per loop.
 *
 * Ported from ascii.rest `bouncing-balls` (MIT, bas3line): same squash-on-landing look and floor shadow, but the
 * motion is a closed-form hop instead of a stepped physics simulation, so any frame can be drawn on its own.
 */
public fun bouncingBallsPiece(
    seed: Int = 0,
    count: Int = 3,
    colors: List<Rgb> = DefaultColors,
): AsciiPiece {
    require(count in 1..RADII.size) { "count must be 1..${RADII.size}" }
    require(colors.isNotEmpty()) { "colors must not be empty" }
    val rng = AKit.rng(seed, 5)
    val phases = List(count) { rng.float() }
    return asciiPiece(
        name = "bouncing balls",
        category = "physics",
        cols = COLS,
        rows = ROWS,
        still = 0.9f,
        loop = LOOP,
        note = "shaded rubber balls hopping and squashing on a floor",
    ) { t, grid ->
        val floor = ROWS - 1f
        for (x in 0 until COLS) grid.put(x, ROWS - 1, '=')
        for (i in 0 until count) {
            val r = RADII[i]
            val u = AKit.frac(t * HOPS[i] / LOOP + phases[i])
            val peak = ROWS - 4f - 2f * r
            val h = peak * 4f * u * (1f - u)
            val squash = 1f - 0.3f * (1f - h).coerceIn(0f, 1f)
            val ry = r * squash
            val rx = r * 2f / squash
            val cx = 10f + i * (COLS - 20f) / maxOf(1, count - 1) + 2f * sin(AKit.TAU * t / LOOP + i)
            val cy = floor - ry - h
            val shadow = rx * (1f - 0.6f * h / peak)
            for (x in (cx - shadow).toInt()..(cx + shadow).toInt()) grid.put(x, ROWS - 1, '#')
            shade(grid, cx, cy, rx, ry, colors[i % colors.size])
        }
    }
}

private fun shade(
    grid: Grid,
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    color: Rgb,
) {
    for (y in (cy - ry).toInt() - 1..(cy + ry).toInt() + 1) {
        for (x in (cx - rx).toInt() - 1..(cx + rx).toInt() + 1) {
            val nx = (x + 0.5f - cx) / rx
            val ny = (y + 0.5f - cy) / ry
            val flat = nx * nx + ny * ny
            if (flat > 1f) continue
            val lum = nx * -0.5f + ny * -0.6f + sqrt(1f - flat) * 0.62f
            grid.put(x, y, AKit.glyph(RAMP, 0.15f + 0.85f * abs(lum.coerceIn(0f, 1f))), color)
        }
    }
}

/**
 * Shaded rubber balls hopping on a floor, squashing as they land.
 * Ported from ascii.rest `bouncing-balls`.
 *
 * @param seed offsets the balls' hops from each other.
 * @param count how many balls, 1 to 5.
 * @param colors the ball colors, used in order and repeated if there are fewer than balls.
 */
@Composable
public fun BouncingBalls(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    count: Int = 3,
    colors: List<Rgb> = DefaultColors,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Colored rubber balls hopping on a floor and squashing as they land",
) {
    val piece = remember(seed, count, colors) { bouncingBallsPiece(seed, count, colors) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
