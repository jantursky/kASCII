package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.BrailleCanvas
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import kotlin.math.roundToInt

/** A line chart drawn in Braille dots, [cols] by [rows] cells, with the highest and lowest value on the left. */
@Composable
public fun AsciiLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    cols: Int = 40,
    rows: Int = 8,
) {
    val description = describeSeries("Line chart", "points", values)
    GridSurface(renderLineChart(values, cols, rows, LocalKasciiTheme.current), modifier, description)
}

internal fun renderLineChart(values: List<Float>, cols: Int, rows: Int, theme: KasciiTheme): Grid {
    val grid = Grid(cols, rows)
    if (values.isEmpty()) return grid
    val low = values.min()
    val high = values.max()
    val axis = maxOf(low.pretty().length, high.pretty().length) + 1
    grid.write(0, 0, high.pretty().padStart(axis - 1), theme.dim())
    grid.write(0, rows - 1, low.pretty().padStart(axis - 1), theme.dim())
    val canvas = BrailleCanvas((cols - axis).coerceAtLeast(1), rows)
    val span = high - low
    fun dotY(v: Float) =
        if (span <= 0f) canvas.dotHeight / 2 else ((1f - (v - low) / span) * (canvas.dotHeight - 1)).roundToInt()
    fun dotX(i: Int) = if (values.size == 1) 0 else (i * (canvas.dotWidth - 1) / (values.size - 1f)).roundToInt()
    if (values.size == 1) canvas.plot(0, dotY(values[0]))
    for (i in 1 until values.size) canvas.line(dotX(i - 1), dotY(values[i - 1]), dotX(i), dotY(values[i]))
    canvas.drawTo(grid, axis, 0, theme.accent)
    return grid
}
