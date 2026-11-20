package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme

/** A small status badge drawn as `(text)` in the color of its [tone]. */
@Composable
public fun AsciiBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: AsciiTone = AsciiTone.Neutral,
) {
    GridSurface(renderBadge(text, tone, LocalKasciiTheme.current), modifier, text)
}

internal fun renderBadge(
    text: String,
    tone: AsciiTone,
    theme: KasciiTheme,
): Grid {
    val grid = Grid(text.length + 2, 1)
    grid.write(0, 0, "($text)", tone.color(theme))
    return grid
}
