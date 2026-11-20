package com.jantursky.kascii.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.AsciiPieceView
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.Noise
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.asciiPiece
import com.jantursky.kascii.gallery.b.Palette
import kotlin.math.floor

private const val COLS = 64
private const val ROWS = 24
internal const val DEFAULT_GLYPHS = "0123456789abcdefghijklmnopqrstuvwxyz:;<>=+*#$%&"

/**
 * Falling code. Every column has its own speed, trail length and starting point, all hashed from the seed, so
 * the picture is a pure function of the time and needs no state. The head of a trail uses the theme ink and the
 * body fades to dots; [density] is the share of columns that carry a trail. Glyphs come from [glyphs].
 *
 * Ported from ascii.rest `matrix rain` (MIT, Copyright (c) 2026 bas3line): same falling trails, with trails
 * derived from a hash instead of a mutable drop list.
 */
public fun matrixRainPiece(
    seed: Int = 0,
    density: Float = 0.7f,
    glyphs: String = DEFAULT_GLYPHS,
    color: Rgb = Palette.Green,
): AsciiPiece {
    require(glyphs.isNotEmpty()) { "glyphs must not be empty" }
    return asciiPiece(
        name = "matrix rain",
        category = "effects",
        cols = COLS,
        rows = ROWS,
        fps = 20,
        still = 6f,
        note = "columns of falling code with bright heads",
    ) { t, grid ->
        for (x in 0 until COLS) {
            if (Noise.hash(x, 1, 0, seed) > density) continue
            val speed = 5f + 9f * Noise.hash(x, 2, 0, seed)
            val length = 6 + (10f * Noise.hash(x, 3, 0, seed)).toInt()
            val cycle = ROWS + length
            val head = (t * speed + cycle * Noise.hash(x, 4, 0, seed)) % cycle - length / 2
            val flicker = floor(t * 6f).toInt()
            for (y in 0 until ROWS) {
                val d = head - y
                if (d < 0f || d >= length) continue
                val g = glyphs[(Noise.hash(x, y, flicker / 3, seed) * glyphs.length).toInt() % glyphs.length]
                when {
                    d < 1f -> grid.put(x, y, g)
                    d > length * 0.75f -> grid.put(x, y, '.', color)
                    else -> grid.put(x, y, g, color)
                }
            }
        }
    }
}

/**
 * Columns of falling code. [seed] arranges the columns, [density] is the share of columns with a trail, [glyphs]
 * the characters shown and [color] the body of the trails. Ported from ascii.rest `matrix rain`.
 */
@Composable
public fun AsciiMatrixRain(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    density: Float = 0.7f,
    glyphs: String = DEFAULT_GLYPHS,
    color: Rgb = Palette.Green,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Columns of green code falling down the screen",
) {
    val piece = remember(seed, density, glyphs, color) { matrixRainPiece(seed, density, glyphs, color) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
