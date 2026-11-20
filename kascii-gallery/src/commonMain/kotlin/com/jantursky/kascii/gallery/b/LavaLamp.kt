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
import com.jantursky.kascii.gallery.b.Palette
import com.jantursky.kascii.gallery.b.TAU
import com.jantursky.kascii.gallery.b.rngFor
import kotlin.math.cos
import kotlin.math.sin

private const val COLS = 30
private const val ROWS = 26
private const val MID = COLS / 2f
private const val LOOP = 48f
private const val GLASS_TOP = 3
private const val GLASS_BOTTOM = 20
private const val WAX_GLYPHS = ".:-=+*#%@"

// A cell is 0.6 em wide and 1.2 em tall, and the wax is measured in em so blobs stay round.
private const val CELL_W = 0.6f
private const val CELL_H = 1.2f
private const val TOP_EM = 6.5f
private const val FLOOR_EM = 24f

/** One blob: how many laps it makes per loop, where in its lap it starts, its side, sway and radius in em. */
private class Blob(
    val laps: Int,
    val phase: Float,
    val side: Float,
    val sway: Float,
    val radius: Float,
)

/** Half the lamp's width in a row: cap, tapered glass, then a flared base. */
private fun halfWidth(row: Int): Int =
    when {
        row < 1 -> 2
        row < GLASS_TOP -> row + 1
        row < GLASS_BOTTOM -> 5 + (row - GLASS_TOP) / 4
        row == GLASS_BOTTOM -> 9
        else -> 8 + (row - GLASS_BOTTOM)
    }

private fun Grid.shell() {
    for (r in 0 until ROWS) {
        val h = halfWidth(r)
        val glass = r in GLASS_TOP until GLASS_BOTTOM
        val grow = halfWidth(r + 1) - h
        put(
            (MID - h).toInt(),
            r,
            if (glass) {
                '|'
            } else if (grow > 0) {
                '/'
            } else {
                '\\'
            },
        )
        put(
            (MID + h).toInt() - 1,
            r,
            if (glass) {
                '|'
            } else if (grow > 0) {
                '\\'
            } else {
                '/'
            },
        )
    }
    for (x in (MID - 2).toInt() until (MID + 2).toInt()) put(x, 0, '_')
    for (x in (MID - 9).toInt() until (MID + 9).toInt()) put(x, GLASS_BOTTOM, '_')
    for (x in (MID - 12).toInt() until (MID + 12).toInt()) put(x, ROWS - 1, '_')
}

/** Sum of blob influences at a point in em, where 1 is the wax surface. The pool and the cap are fixed blobs. */
private fun field(
    blobs: List<Pair<Float, Float>>,
    radii: List<Float>,
    x: Float,
    y: Float,
): Float {
    var sum = 0f
    for (i in blobs.indices) {
        val dx = x - blobs[i].first
        val dy = y - blobs[i].second
        sum += radii[i] * radii[i] / (dx * dx + dy * dy + 0.05f)
    }
    val pool = (x * x / 4f + (y - FLOOR_EM - 2f) * (y - FLOOR_EM - 2f))
    return sum + 22f / (pool + 0.05f) + 3f / (x * x / 4f + (y - 3f) * (y - 3f) + 0.05f)
}

/**
 * A lava lamp. Wax pools on the floor of the glass, blobs swell off it, climb, meet and part and sink again.
 * Each blob makes a whole number of laps per 48 seconds so the loop closes. [seed] shifts where in its lap each
 * blob starts.
 *
 * Ported from ascii.rest `lava lamp` (MIT, Copyright (c) 2026 bas3line): the same metaball wax in a glass with a
 * base, with a new field function and a colored wax.
 */
public fun lavaLampPiece(
    seed: Int = 0,
    wax: Rgb = Palette.Red,
): AsciiPiece {
    val rng = rngFor(seed, 3)
    val blobs =
        listOf(
            Blob(2, rng.float(), -0.5f, 0.7f, 3.0f),
            Blob(3, rng.float(), 0.6f, 0.6f, 2.7f),
            Blob(1, rng.float(), 0.1f, 0.8f, 3.3f),
        )
    return asciiPiece(
        name = "lava lamp",
        category = "objects",
        cols = COLS,
        rows = ROWS,
        fps = 15,
        still = 8f,
        loop = LOOP,
        note = "wax blobs rising, merging and sinking in a lit glass",
    ) { t, grid ->
        val centers =
            blobs.map {
                val a = TAU * (it.laps * t / LOOP + it.phase)
                val y = TOP_EM + (FLOOR_EM - TOP_EM) * (1f + cos(a)) / 2f
                (it.side * 1.5f + it.sway * sin(a * 2f)) to y
            }
        val radii = blobs.map { it.radius }
        for (r in GLASS_TOP until GLASS_BOTTOM) {
            val room = halfWidth(r) - 1
            for (x in (MID - room).toInt() + 1 until (MID + room).toInt() - 1) {
                val v = field(centers, radii, (x + 0.5f - MID) * CELL_W, (r + 0.5f) * CELL_H)
                if (v < 1f) continue
                val level = ((v - 1f) * 3f).toInt().coerceAtMost(WAX_GLYPHS.length - 1)
                grid.put(x, r, WAX_GLYPHS[level], wax)
            }
        }
        grid.shell()
    }
}

/** A lava lamp with wax blobs rising, merging and sinking. [seed] shifts the blobs, [wax] colors them. */
@Composable
public fun AsciiLavaLamp(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    wax: Rgb = Palette.Red,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A lava lamp with wax blobs rising and sinking",
) {
    val piece = remember(seed, wax) { lavaLampPiece(seed, wax) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
