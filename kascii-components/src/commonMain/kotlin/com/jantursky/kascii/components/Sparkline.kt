package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import kotlin.math.roundToInt

/** A one-line trend of [values], one block glyph per value, scaled between their minimum and maximum. */
@Composable
public fun AsciiSparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    val clean = values.finite()
    val description = describeSeries("Sparkline", "values", clean)
    GridSurface(renderSparkline(clean, LocalKasciiTheme.current), modifier, description)
}

internal fun renderSparkline(
    values: List<Float>,
    theme: KasciiTheme,
): Grid {
    val values = values.finite()
    val grid = Grid(values.size, 1)
    val low = values.minOrNull() ?: return grid
    val span = values.max() - low
    values.forEachIndexed { x, v ->
        val level = if (span <= 0f) EIGHTHS.length / 2 else ((v - low) / span * (EIGHTHS.length - 1)).roundToInt()
        grid.put(x, 0, EIGHTHS[level], theme.accent)
    }
    return grid
}

/** Spoken summary of a data series: its size, where it starts and ends, and its range. */
internal fun describeSeries(
    name: String,
    unit: String,
    values: List<Float>,
): String =
    if (values.isEmpty()) {
        "$name, no data"
    } else {
        "$name, ${values.size} $unit from ${values.first().pretty()} to ${values.last().pretty()}, " +
            "low ${values.min().pretty()}, high ${values.max().pretty()}"
    }
