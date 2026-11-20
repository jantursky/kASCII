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
import com.jantursky.kascii.gallery.b.gradient
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val COLS = 64
private const val ROWS = 24
private const val LOOP = 16f
private const val GROUND = 18
private const val CABIN_X = 36
private const val FLAKES = 70
private const val SMOKE_PUFFS = 7

// The cabin, drawn from its top-left corner. '#' is the lit window, the rest is theme ink.
private val CABIN =
    listOf(
        "            ||    ",
        "   _________||___ ",
        "  /.:.:.:.:.:.:.:\\",
        " /_______________\\",
        "  |==+--+==|  |==|",
        "  |==|##|==|  |==|",
        "  |==+--+==|  |==|",
        "  |========|__|==|",
    )
private const val CHIMNEY_DX = 12
private const val WINDOW_DX = 6

/**
 * A snowy night in the hills: aurora curtains wave across the sky above twinkling stars, a log cabin glows from its
 * window, chimney smoke drifts away and snow falls between the pines. The seed shapes the aurora, the drifts and
 * the stars and plants the trees. Every motion completes a whole number of cycles per loop.
 */
public fun auroraCabinPiece(
    seed: Int = 0,
    aurora: Rgb = AKit.Green,
    window: Rgb = AKit.Amber,
): AsciiPiece {
    val rng = AKit.rng(seed, 41)
    val pines = listOf(3, 12, 22, 29, 57).map { intArrayOf(it + rng.int(4), 4 + rng.int(3)) }
    val flakes =
        List(FLAKES) {
            floatArrayOf(rng.float(0f, COLS.toFloat()), if (rng.chance(0.3f)) 2f else 1f, rng.float(), rng.float())
        }
    return asciiPiece(
        name = "aurora cabin",
        category = "scenes",
        cols = COLS,
        rows = ROWS,
        fps = 15,
        still = 5f,
        loop = LOOP,
        note = "aurora curtains over a snowy cabin with a glowing window and falling snow",
    ) { time, grid ->
        val t = loopTime(time, LOOP)
        val angle = AKit.TAU * t / LOOP
        paintStars(grid, seed, t)
        paintAurora(grid, seed, angle, aurora)
        paintFlakes(grid, flakes, t, behind = true)
        paintSnowField(grid, seed)
        pines.forEach { paintPine(grid, it[0], surface(it[0], seed), it[1]) }
        paintCabin(grid, angle, window)
        paintSmoke(grid, t)
        paintFlakes(grid, flakes, t, behind = false)
    }
}

private fun rawSurface(
    x: Int,
    seed: Int,
): Int {
    if (x in CABIN_X - 2..CABIN_X + CABIN[0].length + 1) return GROUND
    return GROUND + (Noise.fbm(x * 0.07f, 3f, 0f, 3, seed) * 3f - 1.5f).toInt()
}

/** The first snow row of column [x]; one-column dips are filled so the drift line stays smooth. */
private fun surface(
    x: Int,
    seed: Int,
): Int {
    val h = rawSurface(x, seed)
    return if (rawSurface(x - 1, seed) < h && rawSurface(x + 1, seed) < h) h - 1 else h
}

private fun paintStars(
    grid: Grid,
    seed: Int,
    t: Float,
) {
    for (y in 0 until GROUND - 4) {
        for (x in 0 until COLS) {
            val h = Noise.hash(x, y, 11, seed)
            if (h < 0.975f) continue
            val glint = sin(AKit.TAU * (2f * t / LOOP + h * 50f))
            grid.put(x, y, if (glint > 0.6f) '+' else '.')
        }
    }
}

private fun paintAurora(
    grid: Grid,
    seed: Int,
    angle: Float,
    color: Rgb,
) {
    val colors = listOf(AKit.Violet, color, color, AKit.Blue)
    for (band in 0..1) {
        val base = if (band == 0) 2.5f else 6.5f
        val depth = if (band == 0) 6f else 4f
        for (x in 0 until COLS) {
            // Where along the sky this curtain hangs, and how bright it is there.
            val presence =
                Noise.value(x * 0.06f + band * 13f + 0.7f * cos(angle), 0.7f * sin(angle) + band * 4f, 0f, seed)
            val envelope = ((presence - 0.2f) * 2.5f).coerceIn(0f, 1f) * (if (band == 0) 1f else 0.8f)
            if (envelope <= 0f) continue
            val top = base + 2f * sin(x * 0.09f + angle * (band + 1) + band * 2f)
            val ray = Noise.value(x * 0.5f + 2f * cos(angle + band), 2f * sin(angle + band), band * 5f, seed + 7)
            val topRow = top.toInt()
            for (y in topRow..(top + depth).toInt()) {
                if (y !in 0 until GROUND - 4) continue
                val d = (y - top).coerceAtLeast(0f)
                val tone = envelope * (0.3f + 0.7f * ray) * (1f - d / depth)
                val glyph =
                    when {
                        y == topRow && tone > 0.3f -> '~'
                        tone > 0.62f -> '|'
                        tone > 0.45f -> '!'
                        tone > 0.3f -> ':'
                        tone > 0.18f -> '.'
                        else -> continue
                    }
                grid.put(x, y, glyph, gradient(colors, (d / depth).coerceIn(0f, 1f)))
            }
        }
    }
}

private fun paintSnowField(
    grid: Grid,
    seed: Int,
) {
    for (x in 0 until COLS) {
        val h = surface(x, seed)
        val left = surface(x - 1, seed)
        val right = surface(x + 1, seed)
        val edge =
            when {
                right < h -> '/'
                left < h -> '\\'
                else -> '_'
            }
        grid.put(x, h - 1, edge)
        for (y in h until ROWS) {
            val glow = abs(x - (CABIN_X + WINDOW_DX + 1.5f)) < 4f - (y - GROUND) && y in GROUND until GROUND + 3
            when {
                glow -> grid.put(x, y, if (y == GROUND) ':' else '.', AKit.Amber)
                Noise.hash(x, y, 13, seed) > 0.9f -> grid.put(x, y, '.', AKit.Blue)
                else -> grid.put(x, y, ' ')
            }
        }
    }
}

private fun paintPine(
    grid: Grid,
    x: Int,
    ground: Int,
    height: Int,
) {
    val top = ground - height - 1
    grid.put(x, top, '^', AKit.Green)
    for (k in 1 until height) {
        grid.put(x - k, top + k, '/')
        grid.put(x + k, top + k, '\\')
        for (dx in -k + 1 until k) grid.put(x + dx, top + k, '^', AKit.Green)
    }
    grid.put(x, ground - 1, '|')
}

private fun paintCabin(
    grid: Grid,
    angle: Float,
    window: Rgb,
) {
    val top = GROUND - CABIN.size
    // Firelight inside: the window brightens and dims, a whole number of times per loop.
    val flicker = 0.5f + 0.25f * sin(angle * 5f) + 0.25f * sin(angle * 9f + 1f)
    val glow = window.lerp(AKit.Red, 1f - flicker)
    CABIN.forEachIndexed { row, line ->
        line.forEachIndexed { col, ch ->
            when (ch) {
                ' ' -> {}

                '#' -> {
                    grid.put(CABIN_X + col, top + row, if (flicker > 0.35f) '█' else '▓', glow)
                }

                else -> {
                    grid.put(CABIN_X + col, top + row, ch)
                }
            }
        }
    }
}

private fun paintSmoke(
    grid: Grid,
    t: Float,
) {
    val x0 = CABIN_X + CHIMNEY_DX
    val y0 = GROUND - CABIN.size - 1
    for (k in 0 until SMOKE_PUFFS) {
        val age = AKit.frac(4f * t / LOOP + k.toFloat() / SMOKE_PUFFS)
        val rise = age * 9f
        val drift = age * age * 7f + sin(AKit.TAU * age * 2f + k) * 0.8f
        val glyph =
            when {
                age < 0.25f -> 'o'
                age < 0.55f -> 'O'
                age < 0.8f -> '~'
                else -> '.'
            }
        grid.put(x0 + drift.toInt(), y0 - rise.toInt(), glyph)
    }
}

/** Snowflakes; slow ones (far) fall [behind] the cabin and trees, fast ones (near) in front of them. */
private fun paintFlakes(
    grid: Grid,
    flakes: List<FloatArray>,
    t: Float,
    behind: Boolean,
) {
    for (f in flakes) {
        if ((f[1] == 1f) != behind) continue
        val y = (AKit.frac(f[2] + f[1] * t / LOOP) * (ROWS + 1)).toInt() - 1
        val x = (f[0] + sin(AKit.TAU * (2f * t / LOOP + f[3])) * 1.2f).toInt()
        if (behind) {
            if (grid.glyphAt(x, y) == ' ') grid.put(x, y, '.')
        } else {
            grid.put(x, y, '*')
        }
    }
}

/**
 * A snowy night: aurora curtains waving over a log cabin with a glowing window, chimney smoke, pines and falling
 * snow.
 *
 * @param seed shapes the aurora, the drifts and the stars and plants the trees.
 * @param aurora the main color of the aurora curtains.
 * @param window the color of the lit window.
 */
@Composable
public fun AsciiAuroraCabin(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    aurora: Rgb = AKit.Green,
    window: Rgb = AKit.Amber,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A log cabin in the snow at night, with aurora curtains waving in the sky",
) {
    val piece = remember(seed, aurora, window) { auroraCabinPiece(seed, aurora, window) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
