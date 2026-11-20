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
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sqrt

private const val COLS = 64
private const val ROWS = 24
private const val LOOP = 9f
private const val MAX_RADIUS = 30f
private const val RAMP = " .:-~=*"

private class RippleDrop(
    val x: Float,
    val y: Float,
    val phase: Float,
)

/**
 * Raindrops landing on a still pond. Each drop sends out a ring that spreads, fades and interferes with its
 * neighbours; the summed height of the water is drawn as a glyph ramp. Every drop lands once per loop.
 */
public fun pondRipplesPiece(
    seed: Int = 0,
    drops: Int = 5,
    color: Rgb = AKit.Blue,
): AsciiPiece {
    require(drops > 0) { "drops must be positive" }
    val rng = AKit.rng(seed, 4)
    val list = List(drops) { RippleDrop(rng.float(8f, 56f), rng.float(4f, 20f), (it + rng.float(0f, 0.6f)) / drops) }
    return asciiPiece(
        name = "pond ripples",
        category = "physics",
        cols = COLS,
        rows = ROWS,
        fps = 20,
        still = 4f,
        loop = LOOP,
        note = "raindrop rings interfering on a pond",
    ) { t, grid ->
        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                var height = 0f
                for (d in list) {
                    val age = AKit.frac(t / LOOP + d.phase)
                    val dx = col + 0.5f - d.x
                    val dy = (row + 0.5f - d.y) * 2f
                    val off = sqrt(dx * dx + dy * dy) - age * MAX_RADIUS
                    height += (1f - age) * (1f - age) * exp(-off * off / 5f) * (0.5f + 0.5f * cos(off * 1.6f))
                }
                val glyph = AKit.glyph(RAMP, height)
                if (glyph != ' ') grid.put(col, row, glyph, color)
            }
        }
    }
}

/**
 * Raindrops landing on a still pond, their rings spreading and interfering.
 *
 * @param seed places the drops.
 * @param drops how many drops land per loop.
 * @param color the water.
 */
@Composable
public fun PondRipples(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    drops: Int = 5,
    color: Rgb = AKit.Blue,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Raindrops landing on a still pond, rings spreading across the water",
) {
    val piece = remember(seed, drops, color) { pondRipplesPiece(seed, drops, color) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
