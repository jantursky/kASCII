package com.jantursky.kascii.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme

/** A tab row with an underline: the selected tab is accented over a double rule. Each tab is its own stop. */
@Composable
public fun AsciiTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.selectableGroup()) {
        tabs.forEachIndexed { index, tab -> AsciiTab(tab, index == selectedIndex) { onSelect(index) } }
    }
}

@Composable
private fun AsciiTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val state = rememberControlState(source, enabled = true)
    ControlGrid(
        grid = renderTab(label, selected, state, LocalKasciiTheme.current),
        state = state,
        modifier = Modifier
            .semantics { contentDescription = label }
            .selectable(selected, source, null, true, Role.Tab, onClick),
    )
}

internal fun renderTab(label: String, selected: Boolean, state: ControlState, theme: KasciiTheme): Grid {
    val grid = Grid(label.length + 4, 2)
    val color = if (selected) theme.accent else theme.dim()
    grid.write(2, 0, label, color)
    grid.fill(0, 1, grid.cols, 1, if (selected) '═' else '─', color)
    if (state.focused) {
        grid.put(1, 0, '>', color)
        grid.put(grid.cols - 2, 0, '<', color)
    }
    return grid
}
