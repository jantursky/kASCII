package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.BrailleCanvas
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * One slice of an [AsciiPieChart].
 *
 * @property label Name shown in the legend.
 * @property value Any non-negative size. Slices are drawn in proportion to the total.
 */
public class AsciiPieSlice(
    public val label: String,
    public val value: Float,
)

/**
 * A pie chart drawn as a Braille disc, [cols] cells wide, with a legend of labels and percentages below. Slice
 * colors come from the theme palette. The legend carries the numbers, so the chart reads without color.
 */
@Composable
public fun AsciiPieChart(
    slices: List<AsciiPieSlice>,
    modifier: Modifier = Modifier,
    cols: Int = 20,
) {
    val description =
        "Pie chart: " +
            pieShares(slices).zip(slices).joinToString(", ") { (share, s) ->
                "${s.label} ${percentOf(share)}%"
            }
    GridSurface(renderPieChart(slices, cols, LocalKasciiTheme.current), modifier, description)
}

/** Each slice's share of the total, 0..1. All zeros when the slices add up to nothing. */
internal fun pieShares(slices: List<AsciiPieSlice>): List<Float> {
    val total = slices.sumOf { it.value.coerceAtLeast(0f).toDouble() }.toFloat()
    return slices.map { if (total <= 0f) 0f else it.value.coerceAtLeast(0f) / total }
}

internal fun renderPieChart(
    slices: List<AsciiPieSlice>,
    cols: Int,
    theme: KasciiTheme,
): Grid {
    val rows = cols / 2
    val legend = pieShares(slices).zip(slices).map { (share, s) -> " ${s.label} ${percentOf(share)}%" }
    val grid = Grid(maxOf(cols, (legend.maxOfOrNull { it.length } ?: 0) + 1), rows + slices.size)
    val canvases = slices.map { BrailleCanvas(cols, rows) }
    val ends = pieShares(slices).runningFold(0f) { sum, share -> sum + share }.drop(1)
    val radius = minOf(cols, rows * 2) - 0.5f
    for (y in 0 until rows * 4) {
        for (x in 0 until cols * 2) {
            val dx = x + 0.5f - cols
            val dy = y + 0.5f - rows * 2
            if (sqrt(dx * dx + dy * dy) > radius) continue
            val turn = (atan2(dx, -dy) / (2 * PI)).toFloat().let { if (it < 0f) it + 1f else it }
            val index = ends.indexOfFirst { turn < it }
            if (index >= 0) canvases[index].plot(x, y)
        }
    }
    canvases.forEachIndexed { i, canvas -> canvas.drawTo(grid, 0, 0, theme.pick(i)) }
    legend.forEachIndexed { i, line ->
        grid.put(0, rows + i, '■', theme.pick(i))
        grid.write(1, rows + i, line)
    }
    return grid
}
