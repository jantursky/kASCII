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
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val COLS = 64
private const val ROWS = 24
private const val LOOP = 48f
private const val FPS = 12
private const val PERIOD = 64 // the width after which every dune layer repeats
private const val SUN_X = 47f
private const val SUN_Y = 6f
private const val SUN_R = 9f // radius in columns; a row is two columns tall
private const val SHIMMER_TOP = 7
private const val SHIMMER_BOTTOM = 13
private const val CAMEL_GAP = 18
private const val STRIDES = 72 // leg cycles per loop
private const val GRAINS = 18
private const val PI_F = 3.1415927f

private val CAMEL =
    listOf(
        "     _          ",
        "   _/#\\_     _  ",
        " _/&&&&&\\___/ o>",
        "/&&&&&&&&&&&&_/ ",
        "\\_&&&&&&&&&_/   ",
    )

private val LEGS =
    listOf(
        listOf("  | |    | |    ", "  | |    | |    ", "  | |    | |    "),
        listOf("  | |    | |    ", "  / \\    / \\    ", " /   \\  /   \\   "),
    )

private class Dunes(
    val layer: Int,
    val base: Int,
    val amp: Float,
    val waves: Int,
    val laps: Int,
    val texture: Char,
)

/**
 * A camel caravan crossing the desert in the heat of the day. A big sun hangs over three layers of dunes that slide
 * past at their own speeds, the far ones wavering in the heat, while the camels plod across, rising and dipping
 * with the sand, and grains of sand blow along the ground. [camels] camels walk in the line. Every layer repeats a
 * whole number of times per loop and the caravan crosses once.
 */
public fun desertCaravanPiece(
    seed: Int = 0,
    camels: Int = 4,
    sun: Rgb = AKit.Amber,
): AsciiPiece {
    require(camels in 1..6) { "camels must be in 1..6" }
    val far = Dunes(0, 13, 2f, 3, 1, '.')
    val mid = Dunes(1, 16, 3f, 2, 2, ':')
    val near = Dunes(2, 21, 2f, 1, 3, '.')
    val rng = AKit.rng(seed, 41)
    val grains = List(GRAINS) { floatArrayOf(rng.float(), 19f + rng.int(5), (6 + rng.int(5)).toFloat()) }
    val length = camels * CAMEL_GAP
    return asciiPiece(
        name = "desert caravan",
        category = "scenes",
        cols = COLS,
        rows = ROWS,
        fps = FPS,
        still = 30f,
        loop = LOOP,
        note = "a camel caravan crossing rolling dunes under a blazing sun",
    ) { time, grid ->
        val t = SceneKit.loopTime(time, FPS, LOOP)
        val run = t / LOOP
        paintDesertSun(grid, sun)
        paintDunes(grid, seed, far, run, AKit.Violet, AKit.Violet)
        paintHeat(grid, t)
        paintDunes(grid, seed, mid, run, AKit.Red, AKit.Red)
        paintDunes(grid, seed, near, run, sun, AKit.Red)
        val lead = -CAMEL[0].length + (COLS + length) * run
        for (i in 0 until camels) paintCamel(grid, seed, near, run, floor(lead - i * CAMEL_GAP).toInt(), i)
        for (g in grains) paintGrain(grid, g, t, sun)
    }
}

private fun paintDesertSun(
    grid: Grid,
    sun: Rgb,
) {
    for (y in 0 until SHIMMER_BOTTOM) {
        for (x in 0 until COLS) {
            val dx = (x + 0.5f - SUN_X) / SUN_R
            val dy = (y + 0.5f - SUN_Y) / (SUN_R / 2f)
            val d = sqrt(dx * dx + dy * dy)
            when {
                d <= 0.85f -> grid.put(x, y, '@', sun)
                d <= 1f -> grid.put(x, y, '#', sun)
                d <= 1.25f -> grid.put(x, y, ':', sun.lerp(AKit.Red, 0.5f))
                d <= 1.5f && (x + y) % 2 == 0 -> grid.put(x, y, '.', AKit.Red)
            }
        }
    }
}

/** The top row of a dune layer at screen column [x]: crests are sharp, troughs round, as wind-blown dunes are. */
private fun duneTop(
    dunes: Dunes,
    x: Int,
    seed: Int,
    run: Float,
): Int {
    val u = x + run * PERIOD * dunes.laps
    var h = 0f
    for (k in 1..2) {
        val phase = Noise.hash(k, dunes.layer, 42, seed) * AKit.TAU
        h += (1f - abs(sin(PI_F * k * dunes.waves * u / PERIOD + phase))) / k
    }
    return dunes.base - (dunes.amp * h).toInt()
}

private fun paintDunes(
    grid: Grid,
    seed: Int,
    dunes: Dunes,
    run: Float,
    lit: Rgb,
    shade: Rgb,
) {
    val scroll = run * PERIOD * dunes.laps
    for (x in 0 until COLS) {
        val top = duneTop(dunes, x, seed, run)
        val next = duneTop(dunes, x + 1, seed, run)
        val prev = duneTop(dunes, x - 1, seed, run)
        // The sun is to the right, so the faces rising to the right are lit.
        val sunny = next <= top
        val ridge =
            when {
                next < top -> '/'
                prev < top -> '\\'
                else -> '_'
            }
        grid.put(x, top, ridge, if (sunny) lit else shade)
        val u = SceneKit.wrap(x + scroll, PERIOD.toFloat()).toInt()
        for (y in top + 1 until ROWS) {
            val grain = Noise.hash(u, y, 43 + dunes.layer, seed) > (if (sunny) 0.85f else 0.6f)
            grid.put(x, y, if (grain) dunes.texture else ' ', if (sunny) lit else shade)
        }
    }
}

/** Rows near the horizon waver sideways in the heat. */
private fun paintHeat(
    grid: Grid,
    t: Float,
) {
    for (y in SHIMMER_TOP..SHIMMER_BOTTOM) {
        val shift = (sin(AKit.TAU * 16f * t / LOOP + y * 1.9f) * 1.2f).roundToInt()
        if (shift == 0) continue
        val glyphs = CharArray(COLS) { grid.glyphAt(it, y) }
        val colors = List(COLS) { grid.colorAt(it, y) }
        for (x in 0 until COLS) {
            val from = (x - shift).coerceIn(0, COLS - 1)
            grid.put(x, y, glyphs[from], colors[from])
        }
    }
}

private fun paintCamel(
    grid: Grid,
    seed: Int,
    near: Dunes,
    run: Float,
    x: Int,
    index: Int,
) {
    if (x + CAMEL[0].length < 0 || x >= COLS) return
    val ground = duneTop(near, (x + CAMEL[0].length / 2).coerceIn(0, COLS - 1), seed, run)
    val top = ground - CAMEL.size - LEGS[0].size + 1
    val step = floor(2f * STRIDES * run + index).toInt().mod(2)
    SceneKit.stamp(grid, x, top, CAMEL, tint = mapOf('#' to AKit.Red))
    SceneKit.stamp(grid, x, top + CAMEL.size, LEGS[step])
}

private fun paintGrain(
    grid: Grid,
    grain: FloatArray,
    t: Float,
    color: Rgb,
) {
    val x = SceneKit.wrap((grain[0] + grain[2] * t / LOOP) * COLS, COLS.toFloat())
    val y = grain[1] + sin(AKit.TAU * (grain[2] * t / LOOP + grain[0]) * 3f) * 0.6f
    // A short streak trails each grain, pointing back into the wind.
    grid.put(x.toInt(), y.roundToInt(), if (grain[0] > 0.5f) '~' else '-', color)
    grid.put(x.toInt() - 1, y.roundToInt(), '-', color)
}

/**
 * A camel caravan crossing the dunes under a big sun, with heat shimmer and blowing sand.
 *
 * @param seed shapes the dunes and places the blowing grains of sand.
 * @param camels the number of camels in the line, 1 to 6.
 * @param sun the color of the sun and the lit faces of the near dunes.
 */
@Composable
public fun AsciiDesertCaravan(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    camels: Int = 4,
    sun: Rgb = AKit.Amber,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A line of camels crossing desert dunes under a blazing sun",
) {
    val piece = remember(seed, camels, sun) { desertCaravanPiece(seed, camels, sun) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
