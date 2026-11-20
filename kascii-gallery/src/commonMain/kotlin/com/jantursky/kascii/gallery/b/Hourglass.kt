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
import com.jantursky.kascii.gallery.b.Palette
import com.jantursky.kascii.gallery.b.frac
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

private const val COLS = 40
private const val ROWS = 24
private const val CX = COLS / 2
private const val CHAMBER = 11
private const val LOOP = 20f
private const val FPS = 15

/** Half the glass width in a row: wide at the caps, a pinch at the neck. */
private fun halfWidth(row: Int): Int {
    val fromNeck = abs(row + 0.5f - ROWS / 2f) / (ROWS / 2f - 0.5f)
    return 1 + (15f * fromNeck.pow(0.75f)).roundToInt()
}

private fun sandWidth(row: Int): Int = 2 * halfWidth(row) - 1

private fun Grid.glass() {
    for (r in 1 until ROWS - 1) {
        val h = halfWidth(r)
        val upper = r < ROWS / 2
        put(CX - h, r, if (upper) '\\' else '/')
        put(CX + h, r, if (upper) '/' else '\\')
    }
    for (x in CX - 17..CX + 17) {
        put(x, 0, '=')
        put(x, ROWS - 1, '=')
    }
}

/** The rows of a chamber from the neck outward, each with the number of sand cells it holds. */
private fun chamber(top: Boolean): List<Pair<Int, Int>> {
    val rows = if (top) CHAMBER downTo 1 else CHAMBER + 1..ROWS - 2
    return rows.map { it to sandWidth(it) }
}

/** Piles [amount] cells of sand into [rows], neck outward or, with [fromBottom], from the far end back. */
private fun Grid.sand(
    rows: List<Pair<Int, Int>>,
    amount: Float,
    fromBottom: Boolean,
    seed: Int,
    color: Rgb,
) {
    var left = amount
    for ((row, width) in if (fromBottom) rows.reversed() else rows) {
        val take = left.coerceAtMost(width.toFloat())
        if (take < 0.5f) break
        left -= take
        for (i in 0 until take.roundToInt()) {
            val x = CX + if (i % 2 == 0) i / 2 else -(i + 1) / 2
            put(x, row, if (Noise.hash(x, row, 0, seed) > 0.8f) ';' else ':', color)
        }
    }
}

private fun Grid.stream(
    fromRow: Int,
    toRow: Int,
    frame: Int,
    color: Rgb,
) {
    for (r in fromRow..toRow) put(CX, r, if ((r + frame) % 2 == 0) '|' else ':', color)
}

/**
 * An hourglass that drains in 18 seconds and flips. The glass is symmetric, so the flip is the mirror of the
 * last frame and the loop of 20 seconds closes without a cut. [seed] only changes the grain texture of the sand.
 * Ported from ascii.rest `hourglass` (MIT, Copyright (c) 2026 bas3line): the same drain and flip idea with a new
 * glass and a falling stream.
 */
public fun hourglassPiece(
    seed: Int = 0,
    sand: Rgb = Palette.Amber,
): AsciiPiece {
    val upper = chamber(top = true)
    val lower = chamber(top = false)
    val total = upper.sumOf { it.second }.toFloat()
    return asciiPiece(
        name = "hourglass",
        category = "objects",
        cols = COLS,
        rows = ROWS,
        fps = FPS,
        still = 6f,
        loop = LOOP,
        note = "sand draining through a glass that flips when it is empty",
    ) { time, grid ->
        val t = time % LOOP
        val drained = (t / (LOOP - 2f)).coerceAtMost(1f)
        val flipped = t >= LOOP - 2f
        val moved = total * drained
        if (!flipped) grid.stream(CHAMBER, ROWS - 2, (t * FPS).toInt(), sand)
        grid.sand(upper, total - moved, fromBottom = false, seed = seed, color = sand)
        grid.sand(lower, moved, fromBottom = true, seed = seed, color = sand)
        if (flipped) grid.mirrorRows()
        grid.glass()
    }
}

private fun Grid.mirrorRows() {
    for (y in 0 until ROWS / 2) {
        for (x in 0 until COLS) {
            val a = glyphAt(x, y)
            val ac = colorAt(x, y)
            put(x, y, glyphAt(x, ROWS - 1 - y), colorAt(x, ROWS - 1 - y))
            put(x, ROWS - 1 - y, a, ac)
        }
    }
}

/**
 * An hourglass whose sand drains and then flips, looping every 20 seconds. [seed] varies the sand texture and
 * [sand] colors it.
 */
@Composable
public fun AsciiHourglass(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    sand: Rgb = Palette.Amber,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "An hourglass with sand draining from the top to the bottom",
) {
    val piece = remember(seed, sand) { hourglassPiece(seed, sand) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
