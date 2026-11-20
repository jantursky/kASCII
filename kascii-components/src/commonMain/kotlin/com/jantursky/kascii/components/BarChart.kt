package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme

/**
 * A vertical bar chart. Bars scale to the largest of [values] and grow in eighths of a cell. [labels] go under
 * the bars. A screen reader gets every value as text.
 */
@Composable
public fun AsciiBarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    labels: List<String> = emptyList(),
    rows: Int = 8,
    barWidth: Int = 3,
    gap: Int = 1,
) {
    val description = "Bar chart: " + values.indices.joinToString(", ") { i ->
        "${labels.getOrElse(i) { (i + 1).toString() }} ${values[i].pretty()}"
    }
    GridSurface(renderBarChart(values, labels, rows, barWidth, gap, LocalKasciiTheme.current), modifier, description)
}

internal fun renderBarChart(
    values: List<Float>,
    labels: List<String>,
    rows: Int,
    barWidth: Int,
    gap: Int,
    theme: KasciiTheme,
): Grid {
    val peak = values.maxOrNull()?.takeIf { it > 0f } ?: 1f
    val bars = renderColumns(values.map { it / peak }, rows, barWidth, gap, theme.accent)
    val grid = Grid(bars.cols, rows + 1)
    grid.blit(bars, 0, 0)
    val pitch = barWidth + gap
    labels.take(values.size).forEachIndexed { i, label ->
        grid.write(i * pitch, rows, label.take(maxOf(barWidth, pitch - 1)), theme.dim())
    }
    return grid
}
