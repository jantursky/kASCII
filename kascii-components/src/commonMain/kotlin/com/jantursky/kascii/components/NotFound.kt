package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme

/** A "404" page art: big digits with a [message] under them. */
@Composable
public fun AsciiNotFound(
    modifier: Modifier = Modifier,
    message: String = "Page not found",
) {
    GridSurface(renderNotFound(message, LocalKasciiTheme.current), modifier, "404, $message")
}

internal fun renderNotFound(message: String, theme: KasciiTheme): Grid {
    val digits = renderBigText("404", AsciiBigFont.Tall, theme.accent)
    val grid = Grid(maxOf(digits.cols, message.length), digits.rows + 2)
    grid.blit(digits, (grid.cols - digits.cols) / 2, 0)
    grid.write((grid.cols - message.length) / 2, digits.rows + 1, message)
    return grid
}
