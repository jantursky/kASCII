package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.BoxStyle
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.rect

/**
 * A framed block of wrapped [text], [cols] cells wide. [border] is any core [BoxStyle]: single, double, rounded
 * or heavy. The optional [title] sits in the top edge.
 */
@Composable
public fun AsciiCard(
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    cols: Int = 40,
    border: BoxStyle = BoxStyle.Single,
) {
    val theme = LocalKasciiTheme.current
    GridSurface(
        grid = renderCard(title, text, cols, border, theme),
        modifier = modifier,
        description = listOfNotNull(title, text).joinToString(". "),
    )
}

internal fun renderCard(title: String?, text: String, cols: Int, border: BoxStyle, theme: KasciiTheme): Grid {
    val width = cols.coerceAtLeast(MIN_CARD_COLS)
    val lines = wrapText(text, width - 4)
    val grid = Grid(width, lines.size + 2)
    grid.rect(0, 0, width, grid.rows, border, color = theme.dim())
    if (!title.isNullOrEmpty()) grid.write(2, 0, " ${title.take(width - 6)} ")
    lines.forEachIndexed { y, line -> grid.write(2, y + 1, line) }
    return grid
}

private const val MIN_CARD_COLS = 8
