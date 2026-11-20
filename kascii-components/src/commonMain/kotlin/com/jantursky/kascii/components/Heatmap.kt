package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import kotlin.math.roundToInt

/**
 * A heat map of a grid of [values], two cells per value. Intensity is shown by glyph (` ░▒▓█`) and by color, so
 * it reads without color. [rowLabels] name the rows. A screen reader gets the values row by row.
 */
@Composable
public fun AsciiHeatmap(
    values: List<List<Float>>,
    modifier: Modifier = Modifier,
    rowLabels: List<String> = emptyList(),
) {
    val description =
        "Heat map, ${values.size} rows: " +
            values
                .mapIndexed { i, row ->
                    "${rowLabels.getOrElse(i) { "row ${i + 1}" }} ${row.joinToString(" ") { it.pretty() }}"
                }.joinToString(". ")
    GridSurface(renderHeatmap(values, rowLabels, LocalKasciiTheme.current), modifier, description)
}

internal fun renderHeatmap(
    values: List<List<Float>>,
    rowLabels: List<String>,
    theme: KasciiTheme,
): Grid {
    val flat = values.flatten()
    val low = flat.minOrNull() ?: 0f
    val span = ((flat.maxOrNull() ?: 0f) - low).takeIf { it > 0f } ?: 1f
    val labelWidth = rowLabels.maxOfOrNull { it.length }?.plus(1) ?: 0
    val grid = Grid(labelWidth + 2 * (values.maxOfOrNull { it.size } ?: 0), values.size)
    values.forEachIndexed { y, row ->
        rowLabels.getOrNull(y)?.let { grid.write(0, y, it, theme.dim()) }
        row.forEachIndexed { x, v ->
            val tone = (v - low) / span
            val glyph = HEAT[(tone * (HEAT.length - 1)).roundToInt()]
            grid.write(labelWidth + 2 * x, y, "$glyph$glyph", theme.dim().lerp(theme.accent, tone))
        }
    }
    return grid
}

private const val HEAT = " ░▒▓█"
