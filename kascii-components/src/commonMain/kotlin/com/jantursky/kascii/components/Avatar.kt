package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.BoxStyle
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.rect

/** An avatar showing the initials of [name] in a rounded box. Its color is picked from the theme by name. */
@Composable
public fun AsciiAvatar(name: String, modifier: Modifier = Modifier) {
    GridSurface(renderAvatar(name, LocalKasciiTheme.current), modifier, name)
}

internal fun renderAvatar(name: String, theme: KasciiTheme): Grid {
    val color = theme.pick(name.sumOf { it.code })
    val grid = Grid(AVATAR_COLS, 3)
    grid.rect(0, 0, AVATAR_COLS, 3, BoxStyle.Rounded, color = color)
    grid.write(2, 1, initialsOf(name), color)
    return grid
}

/** First letters of the first and last word, or the first two letters of a single word. `?` for a blank name. */
internal fun initialsOf(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(2)
        else -> "${words.first()[0]}${words.last()[0]}"
    }.uppercase()
}

private const val AVATAR_COLS = 6
