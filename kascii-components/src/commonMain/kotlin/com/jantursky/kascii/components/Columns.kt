package com.jantursky.kascii.components

import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.Rgb
import kotlin.math.roundToInt

/** Partial block glyphs from one eighth of a cell to a full one. */
internal const val EIGHTHS = "▁▂▃▄▅▆▇█"

/**
 * Vertical bars from the bottom of a [rows] tall grid, one per entry of [levels] (0..1). Bars grow in eighths of
 * a cell, so they are [barWidth] cells wide with [gap] blank cells between them.
 */
internal fun renderColumns(
    levels: List<Float>,
    rows: Int,
    barWidth: Int,
    gap: Int,
    color: Rgb?,
): Grid {
    val grid = Grid((levels.size * (barWidth + gap) - gap).coerceAtLeast(0), rows)
    levels.forEachIndexed { i, level ->
        val eighths = (level.coerceIn(0f, 1f) * rows * 8).roundToInt()
        val x = i * (barWidth + gap)
        for (r in 0 until eighths / 8) grid.fill(x, rows - 1 - r, barWidth, 1, '█', color)
        val rest = eighths % 8
        if (rest > 0) grid.fill(x, rows - 1 - eighths / 8, barWidth, 1, EIGHTHS[rest - 1], color)
    }
    return grid
}
