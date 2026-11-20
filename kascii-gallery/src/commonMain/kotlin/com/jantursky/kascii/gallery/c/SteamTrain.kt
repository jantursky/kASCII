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

private const val COLS = 64
private const val ROWS = 24
private const val LOOP = 8f
private const val FPS = 15
private const val PERIOD = 64 // the width after which every scrolling layer repeats
private const val RAIL = 19
private const val TRAIN_TOP = RAIL - 6
private const val LOCO_X = 41
private const val POLE_GAP = 32
private const val POLE_TOP = 7
private const val PUFFS = 7
private const val TURNS = 16 // wheel turns per loop
private const val SMOKE_RAMP = " .:oO@"

private val LOCO =
    listOf(
        " _____       __      ",
        "|[_]_|      |  |     ",
        "|&&&&|______|__|___  ",
        "|&&&&|==============\\",
        "|____|______________|>",
        "  (|)   (|)  (|) (|)  ",
    )

private val TENDER =
    listOf(
        "         ",
        "         ",
        " _______ ",
        "|#######|",
        "|_______|",
        " (|) (|) ",
    )

private val COACH =
    listOf(
        "              ",
        " ____________ ",
        "|&[]&[]&[]&[]|",
        "|&&&&&&&&&&&&|",
        "|____________|",
        "  (|)    (|)  ",
    )

private val CLOUD =
    listOf(
        "   .--.     ",
        " .(    ).-. ",
        "(___.__)__) ",
    )

/**
 * A steam train crossing hilly country, seen from a camera that keeps pace with it. Far hills, near hills,
 * telegraph poles and the grass scroll past at their own speeds, the wheels turn, and puffs of smoke rise from the
 * chimney and drift back over the cars. [cars] coaches follow the tender. Every layer repeats a whole number of
 * times per loop.
 */
public fun steamTrainPiece(
    seed: Int = 0,
    cars: Int = 2,
    engine: Rgb = AKit.Red,
): AsciiPiece {
    require(cars in 0..3) { "cars must be in 0..3" }
    return asciiPiece(
        name = "steam train",
        category = "scenes",
        cols = COLS,
        rows = ROWS,
        fps = FPS,
        still = 1.2f,
        loop = LOOP,
        note = "a steam train puffing through rolling hills past telegraph poles",
    ) { time, grid ->
        val t = SceneKit.loopTime(time, FPS, LOOP)
        val run = t / LOOP
        paintClouds(grid, seed, run)
        paintHills(grid, seed, run)
        paintPoles(grid, run)
        paintSmoke(grid, run)
        paintTrain(grid, cars, run, engine)
        paintTrack(grid, seed, run)
    }
}

/** The height of a layer at world column [u]: a sum of sines that repeats every [PERIOD] columns. */
private fun hillHeight(
    u: Float,
    seed: Int,
    layer: Int,
    amp: Float,
): Float {
    var h = 0f
    for (k in 1..3) {
        val phase = Noise.hash(k, layer, 21, seed) * AKit.TAU
        h += sin(AKit.TAU * k * (layer + 1) * u / PERIOD + phase) / k
    }
    return amp * (0.5f + 0.35f * h)
}

private fun paintClouds(
    grid: Grid,
    seed: Int,
    run: Float,
) {
    val span = COLS + CLOUD[0].length
    for (i in 0..1) {
        val start = Noise.hash(i, 0, 22, seed) * span
        val x = SceneKit.wrap(start - run * span, span.toFloat()).toInt() - CLOUD[0].length
        SceneKit.stamp(grid, x, 1 + i * 3, CLOUD, AKit.Blue)
    }
}

private fun paintHills(
    grid: Grid,
    seed: Int,
    run: Float,
) {
    paintHillLayer(grid, seed, 0, run * PERIOD, 12, 6f, '.', AKit.Violet)
    paintHillLayer(grid, seed, 1, run * PERIOD * 2, 16, 5f, ':', AKit.Green)
}

private fun paintHillLayer(
    grid: Grid,
    seed: Int,
    layer: Int,
    scroll: Float,
    base: Int,
    amp: Float,
    texture: Char,
    color: Rgb,
) {
    val top = { x: Int -> base - hillHeight(x + scroll, seed, layer, amp).toInt() }
    for (x in 0 until COLS) {
        val y0 = top(x)
        val next = top(x + 1)
        val prev = top(x - 1)
        val ridge =
            when {
                next < y0 -> '/'
                prev < y0 -> '\\'
                else -> '_'
            }
        grid.put(x, y0, ridge, color)
        for (y in y0 + 1 until RAIL) {
            val u = SceneKit.wrap(x + scroll, PERIOD.toFloat()).toInt()
            val dense = Noise.hash(u, y, 24 + layer, seed) > 0.55f
            grid.put(x, y, if (dense) texture else ' ', color)
        }
    }
}

private fun paintPoles(
    grid: Grid,
    run: Float,
) {
    val scroll = run * PERIOD * 4
    for (x in 0 until COLS) {
        val k = SceneKit.wrap(x + scroll, POLE_GAP.toFloat()) / POLE_GAP
        val sag = POLE_TOP + 0.5f + 2.6f * 4f * k * (1f - k)
        grid.put(x, sag.toInt(), if (sag - floor(sag) < 0.5f) '-' else '_')
    }
    for (x in 0 until COLS) {
        if (SceneKit.wrap(x + scroll, POLE_GAP.toFloat()) >= 1f) continue
        grid.put(x - 1, POLE_TOP, '-')
        grid.put(x, POLE_TOP, '+')
        grid.put(x + 1, POLE_TOP, '-')
        for (y in POLE_TOP + 1 until RAIL) grid.put(x, y, '|')
    }
}

private fun paintSmoke(
    grid: Grid,
    run: Float,
) {
    val chimney = LOCO_X + 13
    // Oldest first, so the young dense puffs are drawn over the old faint ones.
    val ages = List(PUFFS) { AKit.frac(2f * run + it.toFloat() / PUFFS) }
    for ((i, age) in ages.withIndex().sortedByDescending { it.value }) {
        val cx = chimney - age * 38f + sin(AKit.TAU * (age * 1.5f + i * 0.3f)) * 1.5f
        val cy = TRAIN_TOP - 1.5f - sqrt(age) * 10f
        val radius = 0.8f + age * 3.4f
        val ry = radius / 2f
        for (y in (cy - ry).toInt() - 1..(cy + ry).toInt() + 1) {
            for (x in (cx - radius).toInt() - 1..(cx + radius).toInt() + 1) {
                val dx = (x + 0.5f - cx) / radius
                val dy = (y + 0.5f - cy) / ry
                val d = sqrt(dx * dx + dy * dy)
                if (d > 1f) continue
                val tone = (1f - d * d) * (1.05f - age * 0.7f)
                val glyph = AKit.glyph(SMOKE_RAMP, tone)
                if (glyph != ' ') grid.put(x, y, glyph)
            }
        }
    }
}

private fun paintTrain(
    grid: Grid,
    cars: Int,
    run: Float,
    engine: Rgb,
) {
    val spoke = "|/-\\"[floor(4f * TURNS * run).toInt().mod(4)]
    val wheels = { lines: List<String> -> lines.map { it.replace("(|)", "($spoke)") } }
    SceneKit.stamp(grid, LOCO_X, TRAIN_TOP, wheels(LOCO), tint = mapOf('=' to engine, '[' to engine, ']' to engine))
    var x = LOCO_X - TENDER[0].length - 1
    grid.put(x + TENDER[0].length, TRAIN_TOP + 4, '-')
    SceneKit.stamp(grid, x, TRAIN_TOP, wheels(TENDER))
    repeat(cars) {
        x -= COACH[0].length + 1
        grid.put(x + COACH[0].length, TRAIN_TOP + 4, '-')
        SceneKit.stamp(grid, x, TRAIN_TOP, wheels(COACH), tint = mapOf('[' to AKit.Amber, ']' to AKit.Amber))
    }
}

private fun paintTrack(
    grid: Grid,
    seed: Int,
    run: Float,
) {
    val near = run * PERIOD * 4
    for (x in 0 until COLS) {
        grid.put(x, RAIL, '=')
        val tie = SceneKit.wrap(x + near, 4f) < 1f
        grid.put(x, RAIL + 1, if (tie) '#' else '-')
        for (y in RAIL + 2 until ROWS) {
            val u = SceneKit.wrap(x + near * (1f + (y - RAIL - 2) * 0.5f), PERIOD.toFloat()).toInt()
            val h = Noise.hash(u, y, 23, seed)
            val glyph =
                when {
                    h > 0.9f -> '"'
                    h > 0.7f -> ','
                    h > 0.5f -> '.'
                    else -> ' '
                }
            if (glyph != ' ') grid.put(x, y, glyph, AKit.Green)
        }
    }
}

/**
 * A steam train crossing hilly country, its wheels turning and smoke drifting back over the cars.
 *
 * @param seed shapes the hills and the grass and places the clouds.
 * @param cars the number of coaches behind the tender, 0 to 3.
 * @param engine the color of the boiler band and the cab window.
 */
@Composable
public fun AsciiSteamTrain(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    cars: Int = 2,
    engine: Rgb = AKit.Red,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A steam train puffing smoke as it crosses rolling hills past telegraph poles",
) {
    val piece = remember(seed, cars, engine) { steamTrainPiece(seed, cars, engine) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
