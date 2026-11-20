package com.jantursky.kascii.components

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.compose.AsciiGridView
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme

/** Width of one glyph cell. The renderer draws its default cell this wide. */
internal val CellWidth: Dp = 8.dp

/** Height of one glyph cell: the bundled monospace font is twice as tall as wide. */
internal val CellHeight: Dp = 16.dp

internal val MinTouchTarget: Dp = 48.dp

/** What a control looks like right now. Renderers turn it into glyphs. */
internal data class ControlState(
    val enabled: Boolean = true,
    val focused: Boolean = false,
    val pressed: Boolean = false,
)

@Composable
internal fun rememberControlState(source: InteractionSource, enabled: Boolean): ControlState {
    val focused by source.collectIsFocusedAsState()
    val pressed by source.collectIsPressedAsState()
    return ControlState(enabled, focused, pressed)
}

/**
 * Draws [grid] at its natural size. The inner node carries no semantics of its own: [description] is the text a
 * screen reader gets for decorative art, and null hides it. Interactive callers describe the whole control
 * instead and leave this null.
 */
@Composable
internal fun GridSurface(
    grid: Grid,
    modifier: Modifier = Modifier,
    description: String? = null,
    theme: KasciiTheme = LocalKasciiTheme.current,
) {
    AsciiGridView(
        grid = grid,
        modifier = modifier
            .size(CellWidth * grid.cols, CellHeight * grid.rows)
            .clearAndSetSemantics { description?.let { contentDescription = it } },
        theme = theme,
    )
}

/** A [GridSurface] for a control: at least 48dp square to touch, inverted while pressed. */
@Composable
internal fun ControlGrid(grid: Grid, state: ControlState, modifier: Modifier = Modifier) {
    val theme = LocalKasciiTheme.current
    Box(modifier.minimumTouchTarget(), contentAlignment = Alignment.Center) {
        GridSurface(grid, theme = if (state.pressed) theme.inverse() else theme)
    }
}

/** Expands the layout to [MinTouchTarget] on both axes, centering the content, like Material's equivalent. */
internal fun Modifier.minimumTouchTarget(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val min = MinTouchTarget.roundToPx()
    val width = maxOf(placeable.width, min).coerceIn(constraints.minWidth, constraints.maxWidth)
    val height = maxOf(placeable.height, min).coerceIn(constraints.minHeight, constraints.maxHeight)
    layout(width, height) { placeable.place((width - placeable.width) / 2, (height - placeable.height) / 2) }
}
