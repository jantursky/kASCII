package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.BrailleCanvas
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.label
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** A half-circle gauge in Braille dots showing [value] (0..1) as a percentage, with an optional [label] below. */
@Composable
public fun AsciiGauge(
    value: Float,
    modifier: Modifier = Modifier,
    cols: Int = 24,
    label: String? = null,
) {
    val text = listOfNotNull(label, "${percentOf(value)}%").joinToString(" ")
    GridSurface(
        grid = renderGauge(value, cols, label, LocalKasciiTheme.current),
        modifier =
            modifier.semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(0f, 1f), 0f..1f)
                stateDescription = text
            },
        description = text,
    )
}

internal fun renderGauge(
    value: Float,
    cols: Int,
    label: String?,
    theme: KasciiTheme,
): Grid {
    val radius = cols - 1
    val arcRows = (radius + 4) / 4
    val grid = Grid(cols, arcRows + if (label == null) 0 else 1)
    val rest = BrailleCanvas(cols, arcRows)
    val filled = BrailleCanvas(cols, arcRows)
    val steps = (PI * radius * 2).toInt().coerceAtLeast(1)
    val centerX = radius
    val centerY = arcRows * 4 - 1
    for (i in 0..steps) {
        val angle = PI * (1.0 - i / steps.toDouble())
        val target = if (i / steps.toFloat() <= value.coerceIn(0f, 1f)) filled else rest
        for (r in radius - 1..radius) {
            target.plot((centerX + r * cos(angle)).roundToInt(), (centerY - r * sin(angle)).roundToInt())
        }
    }
    rest.drawTo(grid, 0, 0, theme.dim())
    filled.drawTo(grid, 0, 0, theme.accent)
    grid.label(cols / 2, arcRows - 1, "${percentOf(value)}%")
    label?.let { grid.label(cols / 2, arcRows, it, theme.dim()) }
    return grid
}
