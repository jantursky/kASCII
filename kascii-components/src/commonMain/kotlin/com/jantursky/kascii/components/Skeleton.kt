package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.compose.rememberReducedMotion
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme

/** A loading placeholder of [rows] text lines, [cols] cells wide, with a highlight sweeping across it. */
@Composable
public fun AsciiSkeleton(
    modifier: Modifier = Modifier,
    cols: Int = 32,
    rows: Int = 3,
) {
    val theme = LocalKasciiTheme.current
    val time = rememberClock(SKELETON_FPS, rememberReducedMotion()).floatValue
    GridSurface(
        grid = renderSkeleton(cols, rows, time, theme),
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "Loading" },
    )
}

internal fun renderSkeleton(
    cols: Int,
    rows: Int,
    t: Float,
    theme: KasciiTheme,
): Grid {
    val grid = Grid(cols, rows)
    val sweep = (t * SWEEP_SPEED).toInt() % (cols + 2 * SWEEP_WIDTH) - SWEEP_WIDTH
    for (y in 0 until rows) {
        val width = (cols * LINE_SHARE[y % LINE_SHARE.size]).toInt().coerceAtLeast(1)
        for (x in 0 until width) {
            val lit = x - sweep in 0 until SWEEP_WIDTH
            grid.put(x, y, if (lit) '▒' else '░', theme.dim())
        }
    }
    return grid
}

private val LINE_SHARE = floatArrayOf(1f, 0.85f, 0.6f, 0.95f, 0.7f)
private const val SKELETON_FPS = 20
private const val SWEEP_SPEED = 14
private const val SWEEP_WIDTH = 4
