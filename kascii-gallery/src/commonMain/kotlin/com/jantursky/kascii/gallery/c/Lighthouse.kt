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
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

private const val COLS = 64
private const val ROWS = 24
private const val HORIZON = 15
private const val LOOP = 24f
private const val FPS = 15
private const val SWEEPS = 6 // beam turns per loop
private const val TOWER_X = 42 // left column of the tower art
private const val TOWER_Y = 2
private const val LAMP_X = TOWER_X + 5
private const val LAMP_Y = TOWER_Y + 2
private const val SHIP_WIDTH = 10
private const val ROCK_TOP = TOWER_Y + 11 // the row of the tower base
private const val ROCK_FOOT = 20 // the first row of open water in front of the rock

private val TOWER =
    listOf(
        "     _     ",
        "    /_\\    ",
        "   |[*]|   ",
        "  =======  ",
        "   |&&&|   ",
        "   |===|   ",
        "   |&&&|   ",
        "  /=====\\  ",
        "  |&&&&&|  ",
        "  |=====|  ",
        "  |&&_&&|  ",
        " /__|_|__\\ ",
    )

private val SHIP =
    listOf(
        "    _     ",
        "  _|_|__  ",
        "  \\o_o_/  ",
    )

/**
 * A lighthouse on a rock at night. Its beam turns six times per loop, sweeping across the sky and lighting the
 * waves below; when it faces the viewer the lamp flares. Stars twinkle, a small ship crosses the horizon once per
 * loop and the wave rows roll, the near ones faster.
 */
public fun lighthousePiece(
    seed: Int = 0,
    beam: Rgb = AKit.Amber,
    sea: Rgb = AKit.Blue,
): AsciiPiece =
    asciiPiece(
        name = "lighthouse",
        category = "scenes",
        cols = COLS,
        rows = ROWS,
        fps = FPS,
        still = 10f,
        loop = LOOP,
        note = "a lighthouse beam sweeping over a night sea as a ship crosses the horizon",
    ) { time, grid ->
        val t = SceneKit.loopTime(time, FPS, LOOP)
        val turn = AKit.TAU * SWEEPS * t / LOOP
        paintNightSky(grid, seed, t)
        val reach = paintBeam(grid, turn, beam)
        paintShip(grid, t, beam)
        paintRock(grid, seed)
        paintSea(grid, seed, t, reach, sea, beam)
        SceneKit.stamp(grid, TOWER_X, TOWER_Y, TOWER, tint = mapOf('*' to beam, '=' to AKit.Red))
        if (sin(turn) > 0.85f) paintFlare(grid, beam)
    }

private fun paintNightSky(
    grid: Grid,
    seed: Int,
    t: Float,
) {
    for (y in 0 until HORIZON - 1) {
        for (x in 0 until COLS) {
            val h = Noise.hash(x, y, 11, seed)
            if (h < 0.96f) continue
            val rate = 1 + (h * 1000f).toInt() % 3
            val glint = sin(AKit.TAU * (rate * t / LOOP + h * 50f))
            val glyph =
                when {
                    glint > 0.75f -> '+'
                    glint > -0.4f -> '.'
                    else -> ' '
                }
            grid.put(x, y, glyph, if (h > 0.992f) AKit.Blue else null)
        }
    }
}

/** Draws the beam for the lamp angle [turn] and returns the columns it spans, which light the waves below. */
private fun paintBeam(
    grid: Grid,
    turn: Float,
    color: Rgb,
): IntRange? {
    val side = cos(turn)
    if (abs(side) < 0.08f) return null
    val dir = if (side < 0f) -1 else 1
    val length = (4f + 58f * abs(side)).toInt()
    var last = LAMP_X
    for (step in 2..length) {
        val x = LAMP_X + dir * step
        if (x !in 0 until COLS) break
        last = x
        val centre = LAMP_Y + 0.5f + step * 0.17f
        val half = 0.4f + step * 0.06f
        val fade = 1f - step / (length + 1f)
        for (y in (centre - half).toInt()..minOf((centre + half).toInt(), HORIZON - 1)) {
            val off = abs(y + 0.5f - centre) / half
            if (off > 1f) continue
            val tone = (1f - off) * (0.35f + 0.65f * fade)
            grid.put(x, y, AKit.glyph(" .-=", tone + 0.15f), color)
        }
    }
    return minOf(LAMP_X, last)..maxOf(LAMP_X, last)
}

private fun paintShip(
    grid: Grid,
    t: Float,
    lights: Rgb,
) {
    val x = floor(-SHIP_WIDTH + (COLS + SHIP_WIDTH) * AKit.frac(t / LOOP)).toInt()
    SceneKit.stamp(grid, x, HORIZON - SHIP.size + 1, SHIP, tint = mapOf('o' to lights))
}

private fun rockEdge(
    y: Int,
    seed: Int,
): Int = TOWER_X + 1 - (y - ROCK_TOP) * 3 + (Noise.hash(y, 1, 13, seed) * 2f).toInt()

private fun paintRock(
    grid: Grid,
    seed: Int,
) {
    for (y in ROCK_TOP until ROCK_FOOT) {
        val edge = rockEdge(y, seed)
        for (x in edge until COLS) {
            val h = Noise.hash(x, y, 14, seed)
            val glyph =
                when {
                    x == edge -> '/'
                    y == ROCK_TOP -> '_'
                    h > 0.75f -> '%'
                    h > 0.4f -> ':'
                    else -> '.'
                }
            grid.put(x, y, glyph)
        }
    }
}

private fun paintSea(
    grid: Grid,
    seed: Int,
    t: Float,
    lit: IntRange?,
    sea: Rgb,
    beam: Rgb,
) {
    for (y in HORIZON until ROWS) {
        val depth = (y - HORIZON) / (ROWS - 1f - HORIZON)
        val rolls = 2 + (depth * 4f).toInt()
        val phase = AKit.TAU * rolls * t / LOOP
        val shift = Noise.hash(y, 0, 12, seed) * AKit.TAU
        val k = 0.5f - depth * 0.28f
        val end = if (y < ROCK_FOOT) rockEdge(y, seed) else COLS
        for (x in 0 until end) {
            if (y == HORIZON && grid.glyphAt(x, y) != ' ') continue
            val v = sin(x * k + shift - phase) + 0.6f * sin(x * k * 2.3f - shift + phase * 2f)
            val glyph =
                when {
                    v > 1.0f -> if (depth > 0.4f) '~' else '-'
                    v > 0.45f -> if (depth > 0.4f) '-' else '.'
                    else -> ' '
                }
            if (glyph == ' ') continue
            grid.put(x, y, glyph, if (lit != null && x in lit) beam else sea)
        }
        if (y >= ROCK_FOOT) continue
        // Spray where the waves break on the rock, each row on its own beat.
        val burst = sin(AKit.TAU * 4f * t / LOOP + y * 1.3f)
        if (burst > 0.5f) grid.put(end - 1, y, if (burst > 0.85f) '*' else '.', sea)
        if (burst > 0.85f && y > HORIZON) grid.put(end - 2, y - 1, '\'', sea)
    }
    // Foam along the foot of the rock.
    for (x in rockEdge(ROCK_FOOT - 1, seed) until COLS) {
        val wash = sin(x * 0.7f - AKit.TAU * 6f * t / LOOP)
        grid.put(x, ROCK_FOOT, if (wash > 0.3f) '~' else '-', sea)
    }
}

private fun paintFlare(
    grid: Grid,
    color: Rgb,
) {
    grid.put(LAMP_X - 2, LAMP_Y - 1, '\\', color)
    grid.put(LAMP_X, LAMP_Y - 2, '|', color)
    grid.put(LAMP_X + 2, LAMP_Y - 1, '/', color)
    grid.put(LAMP_X - 3, LAMP_Y, '-', color)
    grid.put(LAMP_X + 3, LAMP_Y, '-', color)
    grid.put(LAMP_X, LAMP_Y, '@', color)
}

/**
 * A lighthouse on a rock at night, its beam sweeping over the sky and the sea while a ship crosses the horizon.
 *
 * @param seed places the stars, the wave pattern and the shape of the rock.
 * @param beam the color of the lamp, the beam, the lit waves and the ship's portholes.
 * @param sea the color of the waves and the spray.
 */
@Composable
public fun AsciiLighthouse(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    beam: Rgb = AKit.Amber,
    sea: Rgb = AKit.Blue,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A lighthouse on a rock at night, its beam sweeping across the sea",
) {
    val piece = remember(seed, beam, sea) { lighthousePiece(seed, beam, sea) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
