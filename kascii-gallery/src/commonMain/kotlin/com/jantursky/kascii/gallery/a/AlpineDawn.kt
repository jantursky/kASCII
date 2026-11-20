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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val COLS = 72
private const val ROWS = 26
private const val HORIZON = 17
private const val SUN_X = 46
private const val LOOP = 24f

private class DawnRidge(
    val base: Float,
    val amp: Float,
    val scale: Float,
    val body: Char,
    val shadow: Rgb?,
    val lit: Rgb?,
)

/**
 * A painted alpine dawn: three ridges of snow peaks, a sun that climbs behind them and back, mist along the shore
 * and a lake that mirrors the lot. The sun rises and sets once per loop.
 *
 * Re-imagined from ascii.rest `alpine-dawn` (MIT, bas3line): the same scene, drawn here from layered noise
 * ridges instead of a ray-marched heightfield.
 */
public fun alpineDawnPiece(
    seed: Int = 0,
    sun: Rgb = AKit.Amber,
    snowLine: Float = 0.62f,
): AsciiPiece {
    val ridges =
        listOf(
            DawnRidge(5f, 9f, 0.09f, '.', AKit.Violet, AKit.Blue),
            DawnRidge(3f, 7f, 0.13f, ':', AKit.Violet, sun),
            DawnRidge(1f, 5f, 0.21f, '%', null, null),
        )
    return asciiPiece(
        name = "alpine dawn",
        category = "scenes",
        cols = COLS,
        rows = ROWS,
        fps = 15,
        still = LOOP / 2,
        loop = LOOP,
        note = "snow peaks catching first light above a still, misty lake",
    ) { t, grid ->
        val angle = AKit.TAU * t / LOOP
        val rise = 0.5f - 0.5f * cos(angle)
        paintSky(grid, seed, rise, sun)
        ridges.forEachIndexed { i, ridge ->
            paintRidge(grid, ridge, seed, i, snowLine)
            if (i == 1) paintMist(grid, seed, angle)
        }
        paintLake(grid, seed, angle)
    }
}

private fun paintSky(
    grid: Grid,
    seed: Int,
    rise: Float,
    sun: Rgb,
) {
    val sunY = HORIZON + 3f - rise * 12f
    for (y in 0 until HORIZON) {
        for (x in 0 until COLS) {
            val dx = (x - SUN_X) * 0.5f
            val dy = y - sunY
            val d = sqrt(dx * dx + dy * dy)
            val glow = (0.35f + 0.65f * rise) / (1f + d * d * 0.08f)
            when {
                d < 3.2f -> grid.put(x, y, '@', sun)
                glow > 0.1f -> grid.put(x, y, AKit.glyph(" .:-=", glow * 2f), AKit.Blue.lerp(sun, glow * 1.6f))
                glow < 0.06f && Noise.hash(x, y, 0, seed) > 0.985f -> grid.put(x, y, '.')
            }
        }
    }
}

private fun ridgeHeight(
    ridge: DawnRidge,
    x: Int,
    layer: Int,
    seed: Int,
): Int = (ridge.base + ridge.amp * Noise.fbm(x * ridge.scale + layer * 37f, layer * 5f, 0f, 4, seed)).toInt()

private fun paintRidge(
    grid: Grid,
    ridge: DawnRidge,
    seed: Int,
    layer: Int,
    snowLine: Float,
) {
    val peak = ridge.base + ridge.amp
    for (x in 0 until COLS) {
        val h = ridgeHeight(ridge, x, layer, seed)
        val next = ridgeHeight(ridge, x + 1, layer, seed)
        val facesSun = next <= h
        val color = if (facesSun) ridge.lit else ridge.shadow
        val top = HORIZON - h
        val snowy = h > snowLine * peak
        for (y in top until HORIZON) {
            val edge = y == top
            val glyph =
                when {
                    edge && next > h -> '/'
                    edge && next < h -> '\\'
                    edge -> '_'
                    snowy && y - top < 2 -> '*'
                    else -> ridge.body
                }
            grid.put(x, y, glyph, if (snowy && y - top < 2) null else color)
        }
    }
}

private fun paintMist(
    grid: Grid,
    seed: Int,
    angle: Float,
) {
    for (x in 0 until COLS) {
        val n = Noise.value(x * 0.15f + 2f * cos(angle), 5f + 2f * sin(angle), 0f, seed)
        if (n > 0.55f) grid.put(x, HORIZON - 1, if (n > 0.75f) '~' else '-', AKit.Blue)
    }
}

private fun paintLake(
    grid: Grid,
    seed: Int,
    angle: Float,
) {
    for (y in HORIZON until ROWS) {
        val mirror = 2 * HORIZON - 1 - y
        val shift = (sin(y * 1.7f + 3f * angle) * 1.2f).toInt()
        for (x in 0 until COLS) {
            val glyph = grid.glyphAt(x + shift, mirror)
            if (glyph == ' ') {
                if (Noise.hash(x, y, 1, seed) > 0.93f) grid.put(x, y, '~', AKit.Blue)
                continue
            }
            val tint = grid.colorAt(x + shift, mirror)?.lerp(AKit.Blue, 0.4f) ?: AKit.Blue
            grid.put(
                x,
                y,
                if (glyph == '@') {
                    '='
                } else if (glyph == '.') {
                    '.'
                } else {
                    '-'
                },
                tint,
            )
        }
    }
}

/**
 * A painted alpine dawn: layered snow peaks, a sun that rises behind them, mist and a mirroring lake.
 * Re-imagined from ascii.rest `alpine-dawn`.
 *
 * @param seed picks the shape of the peaks.
 * @param sun the color of the sun and of the light on the mid ridge.
 * @param snowLine how high on a ridge the snow starts, 0 (everywhere) to 1 (nowhere).
 */
@Composable
public fun AlpineDawn(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    sun: Rgb = AKit.Amber,
    snowLine: Float = 0.62f,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Snow peaks at dawn above a misty lake, the sun rising behind them",
) {
    val piece = remember(seed, sun, snowLine) { alpineDawnPiece(seed, sun, snowLine) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
