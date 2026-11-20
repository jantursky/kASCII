package com.jantursky.kascii.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme

/**
 * A text button drawn as `[ label ]`. Keyboard focus adds `>` and `<` marks, a press inverts the colors. Activates
 * with a tap, click, Enter or Space. [contentDescription] is what a screen reader announces.
 */
@Composable
public fun AsciiButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = label,
) {
    val source = remember { MutableInteractionSource() }
    val state = rememberControlState(source, enabled)
    val theme = LocalKasciiTheme.current
    ControlGrid(
        grid = renderButton(label, state, theme),
        state = state,
        modifier = modifier
            .semantics { this.contentDescription = contentDescription }
            .clickable(source, null, enabled, role = Role.Button, onClick = onClick),
    )
}

internal fun renderButton(label: String, state: ControlState, theme: KasciiTheme): Grid {
    val color = when {
        !state.enabled -> theme.dim()
        state.pressed -> null
        else -> theme.good()
    }
    val grid = Grid(label.length + 6, 1)
    grid.write(1, 0, "[ $label ]", color)
    if (state.focused) {
        grid.put(0, 0, '>', color)
        grid.put(grid.cols - 1, 0, '<', color)
    }
    return grid
}
