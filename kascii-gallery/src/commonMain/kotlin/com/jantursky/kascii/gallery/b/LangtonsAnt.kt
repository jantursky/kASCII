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
import com.jantursky.kascii.gallery.b.Stepper
import com.jantursky.kascii.gallery.b.frameIndex
import com.jantursky.kascii.gallery.b.rngFor

private const val COLS = 64
private const val ROWS = 24
private const val FPS = 20
private const val WIDTH = COLS
private const val HEIGHT = ROWS * 2
private val GLYPHS = charArrayOf(' ', '▀', '▄', '█')
private val DX = intArrayOf(0, 1, 0, -1)
private val DY = intArrayOf(-1, 0, 1, 0)

/** The torus the ant walks on: black or white cells, and the ant's cell and heading (0 up, then clockwise). */
private class Field(
    startX: Int,
    startY: Int,
    var heading: Int,
) {
    val cells = BooleanArray(WIDTH * HEIGHT)
    var x = startX
    var y = startY

    /** Turns right on a white cell and left on a black one, flips the cell and steps forward. */
    fun step() {
        val i = y * WIDTH + x
        heading = (heading + if (cells[i]) 1 else 3) % 4
        cells[i] = !cells[i]
        x = (x + DX[heading] + WIDTH) % WIDTH
        y = (y + DY[heading] + HEIGHT) % HEIGHT
    }
}

/**
 * Langton's ant on a wrapping field, two cells to a character. The ant turns right on a white cell and left on
 * a black one and flips the cell, which draws a tangle that turns into a diagonal highway after about ten
 * thousand steps. The field after frame N is the field after N * [stepsPerFrame] steps from the seed's starting
 * cell: a [Stepper] keeps the live field and only replays from the start when asked for an earlier frame, so the
 * output depends on the time alone. [seed] 0 starts in the middle, any other seed somewhere else.
 *
 * Ported from ascii.rest `langtons ant` (MIT, Copyright (c) 2026 bas3line): same rule, re-implemented on a
 * wrapping field with half-block cells and a colored ant.
 */
public fun langtonsAntPiece(
    seed: Int = 0,
    stepsPerFrame: Int = 12,
    ant: Rgb = Palette.Red,
): AsciiPiece {
    val rng = rngFor(seed, 4)
    val startX = if (seed == 0) WIDTH / 2 else rng.int(WIDTH)
    val startY = if (seed == 0) HEIGHT / 2 else rng.int(HEIGHT)
    val stepper =
        Stepper(
            create = { Field(startX, startY, 0) },
            advance = { f -> repeat(stepsPerFrame) { f.step() } },
        )
    return asciiPiece(
        name = "langton's ant",
        category = "generative",
        cols = COLS,
        rows = ROWS,
        fps = FPS,
        still = 40f,
        note = "an ant flipping cells until it builds a diagonal highway",
    ) { t, grid ->
        val field = stepper.at(frameIndex(t, FPS))
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                val top = field.cells[2 * r * WIDTH + c]
                val bottom = field.cells[(2 * r + 1) * WIDTH + c]
                val glyph = GLYPHS[(if (top) 1 else 0) + (if (bottom) 2 else 0)]
                if (glyph != ' ') grid.put(c, r, glyph)
            }
        }
        grid.put(field.x, field.y / 2, if (field.y % 2 == 0) '▀' else '▄', ant)
    }
}

/**
 * Langton's ant drawing its highway on a wrapping field. [seed] picks the starting cell, [stepsPerFrame] how fast
 * the ant walks and [ant] colors it. Ported from ascii.rest `langtons ant`.
 */
@Composable
public fun AsciiLangtonsAnt(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    stepsPerFrame: Int = 12,
    ant: Rgb = Palette.Red,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Langton's ant flipping cells and building a diagonal highway",
) {
    val piece = remember(seed, stepsPerFrame, ant) { langtonsAntPiece(seed, stepsPerFrame, ant) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
