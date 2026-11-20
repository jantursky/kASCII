package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.BoxStyle
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.rect

/**
 * A key figure: a [label], the [value] in big digits, a [delta] in percent with an arrow and an optional [trend]
 * sparkline. [cols] is the card width.
 */
@Composable
public fun AsciiStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    delta: Float? = null,
    trend: List<Float> = emptyList(),
    cols: Int = 28,
) {
    val change = delta?.let { ", ${if (it >= 0f) "up" else "down"} ${kotlin.math.abs(it).pretty()} percent" }.orEmpty()
    val grid = renderStatCard(label, value, delta, trend, cols, LocalKasciiTheme.current)
    GridSurface(grid, modifier, "$label: $value$change")
}

internal fun renderStatCard(
    label: String,
    value: String,
    delta: Float?,
    trend: List<Float>,
    cols: Int,
    theme: KasciiTheme,
): Grid {
    val grid = Grid(cols, STAT_ROWS)
    grid.rect(0, 0, cols, STAT_ROWS, BoxStyle.Rounded, color = theme.dim())
    grid.write(2, 1, label.take(cols - 4), theme.dim())
    grid.blit(renderBigText(value, AsciiBigFont.Compact), 2, 2)
    delta?.let {
        val color = if (it >= 0f) theme.good() else theme.bad()
        grid.write(2, 5, (if (it >= 0f) "▲ " else "▼ ") + "${kotlin.math.abs(it).pretty()}%", color)
    }
    val spark = renderSparkline(trend.takeLast(cols / 2), theme)
    grid.blit(spark, cols - 2 - spark.cols, 5)
    return grid
}

private const val STAT_ROWS = 7
