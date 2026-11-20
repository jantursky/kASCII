package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme

/** The line a divider is drawn with. */
public enum class AsciiDividerStyle(
    internal val glyph: Char,
) {
    /** A thin line. */
    Line('─'),

    /** A double line. */
    Double('═'),

    /** A heavy line. */
    Heavy('━'),

    /** A dashed line. */
    Dashed('╌'),

    /** A dotted line. */
    Dotted('·'),
}

/** A horizontal rule [cols] cells wide, with an optional [label] near its left end. Decorative unless labelled. */
@Composable
public fun AsciiDivider(
    modifier: Modifier = Modifier,
    cols: Int = 40,
    style: AsciiDividerStyle = AsciiDividerStyle.Line,
    label: String? = null,
) {
    GridSurface(renderDivider(cols, style, label, LocalKasciiTheme.current), modifier, label)
}

internal fun renderDivider(
    cols: Int,
    style: AsciiDividerStyle,
    label: String?,
    theme: KasciiTheme,
): Grid {
    val grid = Grid(cols, 1)
    grid.fill(0, 0, cols, 1, style.glyph, theme.dim())
    if (!label.isNullOrEmpty()) grid.write(DIVIDER_LABEL_X, 0, " ${label.take(cols - 2 * DIVIDER_LABEL_X)} ")
    return grid
}

private const val DIVIDER_LABEL_X = 2
