package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.BrailleCanvas
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.label
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A radar chart: one spoke per entry of [values] (0..1, 1 on the outer ring), named by [labels]. The shape is
 * drawn in Braille dots. At least three spokes are needed.
 */
@Composable
public fun AsciiRadar(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    cols: Int = 40,
    rows: Int = 14,
) {
    val description = "Radar chart: " + values.indices.joinToString(", ") {
        "${labels.getOrElse(it) { "" }} ${percentOf(values[it])}%"
    }
    GridSurface(renderRadar(values, labels, cols, rows, LocalKasciiTheme.current), modifier, description)
}

internal fun renderRadar(values: List<Float>, labels: List<String>, cols: Int, rows: Int, theme: KasciiTheme): Grid {
    require(values.size >= 3) { "A radar chart needs at least three spokes" }
    val grid = Grid(cols, rows)
    val margin = (labels.maxOfOrNull { it.length } ?: 0) * 2 + 2
    val radius = minOf(cols * 2 / 2 - margin, rows * 4 / 2 - 4)
    val centerX = cols.toFloat()
    val centerY = rows * 2f
    fun point(i: Int, length: Float): Pair<Int, Int> {
        val angle = 2 * PI * i / values.size - PI / 2
        return (centerX + length * cos(angle)).roundToInt() to (centerY + length * sin(angle)).roundToInt()
    }
    val ring = BrailleCanvas(cols, rows)
    val shape = BrailleCanvas(cols, rows)
    for (i in values.indices) {
        val next = (i + 1) % values.size
        val (rx, ry) = point(i, radius.toFloat())
        val (nx, ny) = point(next, radius.toFloat())
        ring.line(rx, ry, nx, ny)
        ring.line(centerX.roundToInt(), centerY.roundToInt(), rx, ry)
        val (sx, sy) = point(i, radius * values[i].coerceIn(0f, 1f))
        val (tx, ty) = point(next, radius * values[next].coerceIn(0f, 1f))
        shape.line(sx, sy, tx, ty)
    }
    ring.drawTo(grid, 0, 0, theme.dim())
    shape.drawTo(grid, 0, 0, theme.accent)
    labels.take(values.size).forEachIndexed { i, text ->
        val (dx, dy) = point(i, radius + 6f)
        grid.label(dx / 2, (dy / 4).coerceIn(0, rows - 1), text)
    }
    return grid
}
