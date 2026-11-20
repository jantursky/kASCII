package com.jantursky.kascii.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.AsciiPieceView
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.asciiPiece
import com.jantursky.kascii.gallery.b.Palette
import com.jantursky.kascii.gallery.b.frameIndex

private const val COLS = 64
private const val ROWS = 24
private const val FPS = 10
private const val PERIOD = 30
private const val PAD = 4
private const val WIDTH = COLS + 2 * PAD + 8
private const val HEIGHT = ROWS * 2 + 2 * PAD + 8

// How far down the glider lane the eater sits, past the visible field, so every glider is swallowed.
private const val LANE = 30
private const val GUN_X = 2
private const val GUN_Y = 2
private val GLYPHS = charArrayOf(' ', '▀', '▄', '█')

private val GUN =
    listOf(
        "........................o",
        "......................o.o",
        "............oo......oo............oo",
        "...........o...o....oo............oo",
        "oo........o.....o...oo",
        "oo........o...o.oo....o.o",
        "..........o.....o.......o",
        "...........o...o",
        "............oo",
    )
private val EATER = listOf("oo", "o", ".ooo", "...o")

private fun ByteArray.place(
    shape: List<String>,
    x0: Int,
    y0: Int,
) {
    shape.forEachIndexed { y, row ->
        row.forEachIndexed { x, c -> if (c == 'o') this[(PAD + y0 + y) * WIDTH + PAD + x0 + x] = 1 }
    }
}

/** One Conway generation. The outer ring of the field is never written, so it stays dead. */
internal fun ByteArray.nextGeneration(): ByteArray {
    val out = ByteArray(size)
    for (y in 1 until HEIGHT - 1) {
        for (x in 1 until WIDTH - 1) {
            val i = y * WIDTH + x
            val n =
                this[i - WIDTH - 1] + this[i - WIDTH] + this[i - WIDTH + 1] + this[i - 1] + this[i + 1] +
                    this[i + WIDTH - 1] + this[i + WIDTH] + this[i + WIDTH + 1]
            out[i] = if (n == 3 || (n == 2 && this[i].toInt() == 1)) 1 else 0
        }
    }
    return out
}

/** The thirty boards of one period of the gun, taken after it has filled the glider lane up to the eater. */
internal fun gliderGunCycle(): List<ByteArray> {
    var board = ByteArray(WIDTH * HEIGHT)
    board.place(GUN, GUN_X, GUN_Y)
    board.place(EATER, GUN_X + 28 + LANE, GUN_Y + 15 + LANE)
    repeat(4 * (LANE + 40) + 60) { board = board.nextGeneration() }
    return List(PERIOD) { board.also { board = board.nextGeneration() } }
}

/**
 * Gosper's glider gun in Conway's life, two cells to a character so every cell is square. It fires a glider every
 * 30 generations and an eater in the corner swallows each one, so the field repeats exactly every 3 seconds.
 * The 30 boards of one period are computed once and the frame is a lookup by time, so the output depends on the
 * time alone. No seed: the pattern is fixed.
 *
 * Ported from ascii.rest `glider gun` (MIT, Copyright (c) 2026 bas3line): the same gun and eater layout,
 * re-implemented with a precomputed period and colored cells.
 */
public fun gliderGunPiece(color: Rgb = Palette.Green): AsciiPiece {
    val cycle by lazy { gliderGunCycle() }
    return asciiPiece(
        name = "glider gun",
        category = "generative",
        cols = COLS,
        rows = ROWS,
        fps = FPS,
        still = 1.5f,
        loop = PERIOD / FPS.toFloat(),
        note = "gosper's gun firing a glider every 30 generations",
    ) { t, grid ->
        val board = cycle[frameIndex(t, FPS) % PERIOD]
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                val top = board[(PAD + 2 * r) * WIDTH + PAD + c].toInt()
                val bottom = board[(PAD + 2 * r + 1) * WIDTH + PAD + c].toInt()
                if (top + bottom > 0) grid.put(c, r, GLYPHS[top + 2 * bottom], color)
            }
        }
    }
}

/** Gosper's glider gun firing a stream of gliders into an eater, in [color]. Ported from ascii.rest `glider gun`. */
@Composable
public fun AsciiGliderGun(
    modifier: Modifier = Modifier,
    color: Rgb = Palette.Green,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Conway's life: a glider gun firing gliders into an eater",
) {
    val piece = remember(color) { gliderGunPiece(color) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
