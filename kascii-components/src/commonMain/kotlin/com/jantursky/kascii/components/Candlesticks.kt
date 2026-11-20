package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import kotlin.math.roundToInt

/**
 * One price interval of a candlestick chart.
 *
 * @property open Price at the start of the interval.
 * @property high Highest price.
 * @property low Lowest price.
 * @property close Price at the end of the interval.
 */
public class Candle(
    public val open: Float,
    public val high: Float,
    public val low: Float,
    public val close: Float,
)

/**
 * A candlestick chart [rows] tall. Wicks are `│`. A rising candle (close at or above open) has a light `▒` body
 * in the success color, a falling one a solid `█` body in the error color, so the direction never relies on color.
 */
@Composable
public fun AsciiCandlesticks(
    candles: List<Candle>,
    modifier: Modifier = Modifier,
    rows: Int = 10,
) {
    val rising = candles.count { it.close >= it.open }
    val description = "Candlestick chart, ${candles.size} candles, $rising rising, ${candles.size - rising} falling"
    GridSurface(renderCandlesticks(candles, rows, LocalKasciiTheme.current), modifier, description)
}

internal fun renderCandlesticks(
    candles: List<Candle>,
    rows: Int,
    theme: KasciiTheme,
): Grid {
    val grid = Grid((candles.size * 2 - 1).coerceAtLeast(0), rows)
    if (candles.isEmpty()) return grid
    val top = candles.maxOf { it.high }
    val span = (top - candles.minOf { it.low }).takeIf { it > 0f } ?: 1f

    fun rowOf(price: Float) = ((top - price) / span * (rows - 1)).roundToInt()
    candles.forEachIndexed { i, c ->
        val color = if (c.close >= c.open) theme.good() else theme.bad()
        val body = if (c.close >= c.open) '▒' else '█'
        for (y in rowOf(c.high)..rowOf(c.low)) grid.put(i * 2, y, '│', color)
        for (y in rowOf(maxOf(c.open, c.close))..rowOf(minOf(c.open, c.close))) grid.put(i * 2, y, body, color)
    }
    return grid
}
