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
import com.jantursky.kascii.core.Rng
import com.jantursky.kascii.core.asciiPiece
import com.jantursky.kascii.core.line
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val COLS = 60
private const val ROWS = 26
private const val LOOP = 12f
private const val TRUNK = 11f
private const val ROW_UNITS = 2f

/** Everything one frame of the tree shares, so the recursion only passes what changes per branch. */
private class TreeFrame(
    val grid: Grid,
    val rng: Rng,
    val phase: Float,
    val wind: Float,
    val depth: Int,
    val leaf: Rgb,
    val blossom: Rgb,
)

/**
 * A tree that forks and forks again, each limb a little shorter than its parent, with leaves at the tips and a
 * wind that bends every limb, the outer ones most. A seeded generator draws the same tree every frame (it is
 * re-run per frame in a fixed order), so only the wind moves.
 *
 * Ported from ascii.rest `fractal-tree` (MIT, bas3line): same recursive fork-and-sway idea, on a Bresenham line
 * per limb with the glyph chosen from its direction.
 */
public fun fractalTreePiece(
    seed: Int = 0,
    depth: Int = 8,
    wind: Float = 1f,
    leaf: Rgb = AKit.Green,
    blossom: Rgb = AKit.Amber,
): AsciiPiece {
    require(depth in 1..10) { "depth must be 1..10" }
    return asciiPiece(
        name = "fractal tree",
        category = "nature",
        cols = COLS,
        rows = ROWS,
        fps = 15,
        still = 3f,
        loop = LOOP,
        note = "a recursive branching tree swaying in the wind",
    ) { t, grid ->
        for (x in 0 until COLS) grid.put(x, ROWS - 1, if (x % 3 == 0) '^' else '_')
        val frame = TreeFrame(grid, AKit.rng(seed, 8), AKit.TAU * t / LOOP, wind, depth, leaf, blossom)
        branch(frame, COLS / 2f, (ROWS - 1) * ROW_UNITS, 0f, TRUNK, depth)
    }
}

private fun branch(
    f: TreeFrame,
    x: Float,
    y: Float,
    angle: Float,
    length: Float,
    level: Int,
) {
    val lean = f.depth - level
    val sway = f.wind * sin(f.phase + lean * 0.5f) * 0.03f * (lean + 1)
    val a = angle + sway
    val x1 = x + sin(a) * length
    val y1 = y - cos(a) * length
    val spread = 0.35f + f.rng.float() * 0.3f
    val shrink = 0.72f + f.rng.float() * 0.08f
    val dx = x1 - x
    val dy = y1 - y
    val glyph =
        when {
            abs(dx) < abs(dy) * 0.45f -> '|'
            abs(dy) < abs(dx) * 0.45f -> '-'
            dx * dy < 0f -> '/'
            else -> '\\'
        }
    f.grid.line(x.toInt(), (y / ROW_UNITS).toInt(), x1.toInt(), (y1 / ROW_UNITS).toInt(), glyph)
    if (level == 1) {
        val cx = x1.toInt()
        val cy = (y1 / ROW_UNITS).toInt()
        val bloom = f.rng.chance(0.2f)
        f.grid.put(cx, cy, if (bloom) '*' else 'o', if (bloom) f.blossom else f.leaf)
        f.grid.put(cx - 1, cy, 'o', f.leaf)
        f.grid.put(cx + 1, cy, 'o', f.leaf)
        return
    }
    branch(f, x1, y1, a - spread, length * shrink, level - 1)
    branch(f, x1, y1, a + spread, length * shrink, level - 1)
}

/**
 * A recursive tree swaying in the wind.
 * Ported from ascii.rest `fractal-tree`.
 *
 * @param seed shapes the forks.
 * @param depth how many times the limbs fork, 1 to 10.
 * @param wind how strongly the wind bends the limbs; 0 is a still tree.
 * @param leaf the color of the leaves.
 * @param blossom the color of the occasional blossom.
 */
@Composable
public fun FractalTree(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    depth: Int = 8,
    wind: Float = 1f,
    leaf: Rgb = AKit.Green,
    blossom: Rgb = AKit.Amber,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A tree whose branches fork again and again, swaying in the wind",
) {
    val piece = remember(seed, depth, wind, leaf, blossom) { fractalTreePiece(seed, depth, wind, leaf, blossom) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
