package com.jantursky.kascii.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.AsciiPieceView
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.Rng
import com.jantursky.kascii.core.asciiPiece
import com.jantursky.kascii.gallery.b.Palette
import com.jantursky.kascii.gallery.b.Stepper
import com.jantursky.kascii.gallery.b.frameIndex
import com.jantursky.kascii.gallery.b.rngFor

private const val COLS = 64
private const val ROWS = 24
private const val FPS = 24
private const val HEAT_ROWS = ROWS * 2
private const val MAX_HEAT = 36
private const val WARM_UP = 80
private const val GLYPHS = " .:;+*#%@"

/** The heat of every cell, with the bottom row held at the maximum. */
private class Flames(
    val rng: Rng,
) {
    val heat = IntArray(COLS * HEAT_ROWS)

    init {
        for (x in 0 until COLS) heat[(HEAT_ROWS - 1) * COLS + x] = MAX_HEAT
        repeat(WARM_UP) { spread() }
    }

    /** Every cell passes its heat to the cell above, one column to either side or straight up, a little cooler. */
    fun spread() {
        for (y in 1 until HEAT_ROWS) {
            for (x in 0 until COLS) {
                val r = rng.int(3)
                val to = (x - r + 1).coerceIn(0, COLS - 1)
                heat[(y - 1) * COLS + to] = (heat[y * COLS + x] - r).coerceAtLeast(0)
            }
        }
    }
}

/**
 * The classic demoscene fire: a hot bed along the bottom, every cell handing its heat to the cell above, a bit
 * cooler and a step sideways. Each frame is one spread step of a seeded generator, so a [Stepper] replays the
 * fire from the start when asked for an earlier frame and the output depends on the seed and the time alone.
 * Cells are two heat rows to a character. Colors run from [cool] to [hot]; the hottest cells use the theme ink.
 *
 * Ported from ascii.rest `doom fire` (MIT, Copyright (c) 2026 bas3line): the same fire-spread rule, simplified to
 * the original rule and extended with seeds and per-cell colors.
 */
public fun flamesPiece(
    seed: Int = 0,
    cool: Rgb = Palette.Red,
    hot: Rgb = Palette.Amber,
): AsciiPiece {
    val stepper = Stepper(create = { Flames(rngFor(seed, 5)) }, advance = { it.spread() })
    return asciiPiece(
        name = "flames",
        category = "effects",
        cols = COLS,
        rows = ROWS,
        fps = FPS,
        still = 2f,
        note = "the psx fire spread climbing off a white-hot bed",
    ) { t, grid ->
        val flames = stepper.at(frameIndex(t, FPS))
        for (y in 0 until ROWS) {
            for (x in 0 until COLS) {
                val level = (flames.heat[2 * y * COLS + x] + flames.heat[(2 * y + 1) * COLS + x]) / 2f / MAX_HEAT
                val glyph = GLYPHS[(level * (GLYPHS.length - 1) + 0.5f).toInt().coerceIn(0, GLYPHS.length - 1)]
                if (glyph != ' ') grid.put(x, y, glyph, if (level > 0.9f) null else cool.lerp(hot, level))
            }
        }
    }
}

/**
 * A fire spreading up from a hot bed. [seed] picks the flicker, [cool] and [hot] color the flame from its edge to
 * its core. Ported from ascii.rest `doom fire`.
 */
@Composable
public fun AsciiFlames(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    cool: Rgb = Palette.Red,
    hot: Rgb = Palette.Amber,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A fire burning along the bottom edge",
) {
    val piece = remember(seed, cool, hot) { flamesPiece(seed, cool, hot) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
