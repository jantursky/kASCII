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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val COLS = 64
private const val ROWS = 24
private const val LOOP = 12f
private const val TREE_X = 25
private const val HILL_TOP = 16
private const val BASE = 21
private const val PETALS = 36
private const val FALLING = 0.8f

/** The default blossom color: the palette red pulled a little towards violet, a mid-tone pink. */
private val Blossom: Rgb = AKit.Red.lerp(AKit.Violet, 0.3f)

// Trunk and main boughs, centred on the "||" in column TRUNK_MID; the canopy covers most of the upper part.
private val TRUNK =
    listOf(
        "  \\_    \\  |  /    _/  ",
        "    \\_   \\ | /   _/    ",
        "      \\__ \\|/ __/      ",
        "         \\_||_/        ",
        "           ||          ",
        "           ||          ",
        "           ||          ",
        "         _/||\\_        ",
    )
private const val TRUNK_MID = 11

private class CanopyBlob(
    val cx: Float,
    val cy: Float,
    val rx: Float,
    val ry: Float,
)

private val CANOPY =
    listOf(
        CanopyBlob(TREE_X + 1f, 6f, 13f, 4f),
        CanopyBlob(TREE_X - 10f, 8.5f, 7f, 2.6f),
        CanopyBlob(TREE_X + 12f, 8f, 7.5f, 2.8f),
        CanopyBlob(TREE_X + 1f, 3.5f, 8f, 2.4f),
    )

private val CLOUDS =
    listOf(
        listOf("    .--.     ", " .-(    ).   ", "(___.___)__) "),
        listOf("  .-~-.  ", "(_______)"),
        listOf("   _.--._    ", "(_________)_)"),
    )

/**
 * A cherry tree in full bloom on a grassy hill. Petals let go of the canopy, drift down and sideways on the wind
 * and settle on the slope, where a little drift of them gathers and thins again; clouds sail past behind. The seed
 * shapes the canopy and the petals' paths. Every petal and cloud completes a whole number of trips per loop.
 */
public fun cherryBlossomPiece(
    seed: Int = 0,
    blossom: Rgb = Blossom,
    wind: Float = 1f,
): AsciiPiece {
    val rng = AKit.rng(seed, 61)
    val petals =
        List(PETALS) {
            val blob = CANOPY[rng.int(CANOPY.size)]
            floatArrayOf(
                blob.cx + rng.float(-blob.rx, blob.rx) * 0.8f,
                blob.cy + rng.float(-blob.ry, blob.ry) * 0.6f,
                (1 + rng.int(2)).toFloat(),
                rng.float(),
                wind * rng.float(8f, 26f),
                rng.float(),
            )
        }
    val cloudStart = List(CLOUDS.size) { rng.float() }
    return asciiPiece(
        name = "cherry blossom",
        category = "scenes",
        cols = COLS,
        rows = ROWS,
        fps = 15,
        still = 4f,
        loop = LOOP,
        note = "a blossoming cherry tree on a hill, petals drifting on the wind",
    ) { time, grid ->
        val t = loopTime(time, LOOP)
        val angle = AKit.TAU * t / LOOP
        paintClouds(grid, cloudStart, t)
        paintHill(grid, seed)
        paintTrunk(grid)
        paintCanopy(grid, seed, angle, blossom)
        paintPile(grid, seed, angle, blossom)
        petals.forEach { paintPetal(grid, it, t, blossom) }
    }
}

/** The row of the grass line in column [x]. */
private fun hill(x: Int): Int {
    val d = (x - TREE_X) / 19f
    return (HILL_TOP + d * d * 6f).toInt().coerceAtMost(BASE)
}

private fun paintClouds(
    grid: Grid,
    start: List<Float>,
    t: Float,
) {
    CLOUDS.forEachIndexed { i, cloud ->
        val width = cloud[0].length
        val x = (AKit.frac(start[i] + t / LOOP) * (COLS + width)).toInt() - width
        val y = 1 + i * 3
        cloud.forEachIndexed { row, line ->
            line.forEachIndexed { col, ch -> if (ch != ' ') grid.put(x + col, y + row, ch) }
        }
    }
}

private fun paintHill(
    grid: Grid,
    seed: Int,
) {
    for (x in 0 until COLS) {
        val h = hill(x)
        val edge =
            when {
                hill(x + 1) < h -> '/'
                hill(x - 1) < h -> '\\'
                else -> '_'
            }
        grid.put(x, h - 1, edge, AKit.Green)
        for (y in h until ROWS) {
            grid.put(x, y, ' ')
            val n = Noise.hash(x, y, 17, seed)
            if (n > 0.8f) grid.put(x, y, if (n > 0.93f) '"' else ',', AKit.Green)
        }
    }
}

private fun paintTrunk(grid: Grid) {
    val left = TREE_X - TRUNK_MID
    val top = HILL_TOP - TRUNK.size
    TRUNK.forEachIndexed { row, line ->
        line.forEachIndexed { col, ch -> if (ch != ' ') grid.put(left + col, top + row, ch) }
    }
}

private fun paintCanopy(
    grid: Grid,
    seed: Int,
    angle: Float,
    color: Rgb,
) {
    val deep = color.lerp(AKit.Red, 0.6f)
    for (y in 0 until HILL_TOP - 3) {
        for (x in 0 until COLS) {
            var inside = -1f
            for (b in CANOPY) {
                val dx = (x + 0.5f - b.cx) / b.rx
                val dy = (y + 0.5f - b.cy) / b.ry
                inside = maxOf(inside, 1f - sqrt(dx * dx + dy * dy))
            }
            if (inside <= -0.15f) continue
            val n = Noise.fbm(x * 0.3f + 0.5f * cos(angle), y * 0.5f + 0.5f * sin(angle), 0f, 3, seed)
            val v = inside * 1.1f + (n - 0.5f) * 0.8f + 0.3f
            val glyph =
                when {
                    v > 0.88f -> '@'
                    v > 0.62f -> '*'
                    v > 0.4f -> 'o'
                    v > 0.2f -> '.'
                    else -> continue
                }
            grid.put(x, y, glyph, if (Noise.hash(x, y, 19, seed) > 0.75f) deep else color)
        }
    }
}

/** Fallen petals on the slope below the tree; the drift thickens and thins once per loop. */
private fun paintPile(
    grid: Grid,
    seed: Int,
    angle: Float,
    color: Rgb,
) {
    val level = 0.35f + 0.2f * sin(angle)
    for (x in TREE_X - 14..TREE_X + 18) {
        val near = 1f - abs(x - TREE_X - 2f) / 17f
        val h = Noise.hash(x, 0, 23, seed)
        if (h < level * near) grid.put(x, hill(x) - 1, if (h < 0.12f) ',' else '.', color)
    }
}

private fun paintPetal(
    grid: Grid,
    p: FloatArray,
    t: Float,
    color: Rgb,
) {
    val age = AKit.frac(p[3] + p[2] * t / LOOP)
    val landX = (p[0] + p[4]).toInt()
    val landY = hill(landX) - 1
    if (age >= FALLING) {
        grid.put(landX, landY, '.', color)
        return
    }
    val k = age / FALLING
    val sway = sin(AKit.TAU * (k * 2f + p[5])) * 1.6f * sin(PI.toFloat() * k)
    val x = (p[0] + p[4] * k + sway).toInt()
    val y = (p[1] + (landY - p[1]) * k).toInt()
    val glyph = "'`,."[((k * 14f + p[5] * 4f).toInt()) % 4]
    grid.put(x, y, glyph, color)
}

/**
 * A blossoming cherry tree on a hill, its petals drifting down and sideways on the wind, with clouds sailing by.
 *
 * @param seed shapes the canopy and the petals' paths.
 * @param blossom the color of the blossom and the petals.
 * @param wind how far the petals drift sideways; 0 lets them fall straight down.
 */
@Composable
public fun AsciiCherryBlossom(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    blossom: Rgb = Blossom,
    wind: Float = 1f,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A cherry tree in bloom on a hill, petals drifting down on the wind",
) {
    val piece = remember(seed, blossom, wind) { cherryBlossomPiece(seed, blossom, wind) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
