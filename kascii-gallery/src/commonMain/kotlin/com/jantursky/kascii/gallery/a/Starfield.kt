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

private const val COLS = 64
private const val ROWS = 24
private const val LOOP = 8f
private const val NEAR = 0.05f

/**
 * A flight through a field of stars. Every star sits at a fixed spot in space and approaches the camera once or
 * twice per loop, growing from a dot to a bright blob and leaving a short streak as it speeds up.
 */
public fun starfieldPiece(seed: Int = 0, density: Int = 140, tint: Rgb = AKit.Blue): AsciiPiece {
    require(density > 0) { "density must be positive" }
    return asciiPiece(
        name = "starfield",
        category = "space",
        cols = COLS,
        rows = ROWS,
        still = 2f,
        loop = LOOP,
        note = "flying through a field of stars",
    ) { t, grid ->
        val rng = AKit.rng(seed, 1)
        repeat(density) { i ->
            val x = rng.float(-1f, 1f)
            val y = rng.float(-1f, 1f)
            val phase = rng.float()
            val progress = AKit.frac(phase + (1 + i % 2) * t / LOOP)
            val glyph = AKit.glyph(".+*@", progress)
            val color = when (i % 5) {
                0 -> tint
                1 -> AKit.Amber
                else -> null
            }
            if (progress > 0.5f) plot(grid, x, y, 1f - (progress - 0.04f) * (1f - NEAR), '.', color)
            plot(grid, x, y, 1f - progress * (1f - NEAR), glyph, color)
        }
    }
}

private fun plot(grid: Grid, x: Float, y: Float, z: Float, glyph: Char, color: Rgb?) {
    grid.put((COLS / 2f + x * 0.5f * COLS / z).toInt(), (ROWS / 2f + y * 0.5f * ROWS / z).toInt(), glyph, color)
}

/**
 * A flight through a field of stars.
 *
 * @param seed places the stars.
 * @param density how many stars are in flight.
 * @param tint the color of the blue stars.
 */
@Composable
public fun Starfield(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    density: Int = 140,
    tint: Rgb = AKit.Blue,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Stars streaming past as if flying through space",
) {
    val piece = remember(seed, density, tint) { starfieldPiece(seed, density, tint) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
