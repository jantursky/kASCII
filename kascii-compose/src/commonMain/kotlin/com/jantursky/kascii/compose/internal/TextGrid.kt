package com.jantursky.kascii.compose.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.jantursky.kascii.compose.toColor
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme

/**
 * The grid as text, one line per row, with a color span over every run of cells that has its own color. Cells
 * without one take the text style's color, the theme ink.
 */
internal fun gridAnnotatedString(grid: Grid): AnnotatedString =
    buildAnnotatedString {
        append(grid.toText())
        for (row in 0 until grid.rows) {
            var col = 0
            while (col < grid.cols) {
                val color = grid.colors[row * grid.cols + col]
                var end = col + 1
                while (end < grid.cols && grid.colors[row * grid.cols + end] == color) end++
                // Each row is cols glyphs plus a newline, so a cell's text offset is row * (cols + 1) + col.
                val start = row * (grid.cols + 1)
                if (color != 0) addStyle(SpanStyle(color = Color(color)), start + col, start + end)
                col = end
            }
        }
    }

/**
 * Draws a [GridSource] as selectable text. Time is read during composition here, so every frame recomposes; that is
 * the cost of real text, and why this renderer suits small or still grids.
 */
@Composable
internal fun TextAsciiView(
    source: GridSource,
    cell: Dp,
    font: FontFamily,
    theme: KasciiTheme,
    clockSpec: ClockSpec,
    modifier: Modifier,
) {
    val clock = remember { AsciiClock(clockSpec.still) }
    var onScreen by remember { mutableStateOf(false) }
    LaunchedEffect(clockSpec, onScreen) {
        clock.settle(clockSpec)
        if (clockSpec.running && onScreen) clock.run(clockSpec)
    }
    val measurer = rememberTextMeasurer()
    val advance = remember(font, measurer) { measurer.advanceEm(font) }
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier
            .background(theme.paper.toColor())
            .clipToBounds()
            .onGloballyPositioned { onScreen = it.isOnScreen() },
    ) {
        val cellPx = with(density) { cell.toPx() }
        val size =
            if (constraints.hasBoundedWidth && constraints.hasBoundedHeight) {
                Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
            } else {
                val preferred = source.preferredSize(advance, cellPx)
                Size(preferred.width.toFloat(), preferred.height.toFloat())
            }
        val layout = source.layout(size, advance, cellPx)
        val grid = source.frame(clock.time, layout, theme, null)
        SelectionContainer(Modifier.offset { IntOffset(layout.x, layout.y) }) {
            BasicText(
                text = gridAnnotatedString(grid),
                style = textGridStyle(font, layout.metrics, theme.ink.toColor(), density),
                overflow = TextOverflow.Clip,
                softWrap = false,
            )
        }
    }
}

private fun textGridStyle(
    font: FontFamily,
    metrics: CellMetrics,
    ink: Color,
    density: Density,
): TextStyle =
    with(density) {
        TextStyle(
            color = ink,
            fontFamily = font,
            fontSize = metrics.fontSize.toSp(),
            lineHeight = metrics.height.toSp(),
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
            letterSpacing = 0.sp,
        )
    }
