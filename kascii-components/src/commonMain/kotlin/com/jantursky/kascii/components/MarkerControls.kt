package com.jantursky.kascii.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme

/** A checkbox drawn as `[x]` or `[ ]` followed by its [label]. Toggles with a tap, Enter or Space. */
@Composable
public fun AsciiCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ToggleControl(checked, onCheckedChange, label, Role.Checkbox, CHECKBOX_MARKERS, modifier, enabled)
}

/** A switch drawn as `[ON ■]` or `[■OFF]` followed by its [label]. Toggles with a tap, Enter or Space. */
@Composable
public fun AsciiToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ToggleControl(checked, onCheckedChange, label, Role.Switch, TOGGLE_MARKERS, modifier, enabled)
}

/** One radio button drawn as `(*)` or `( )`. Group several with [AsciiRadioGroup] so they read as one choice. */
@Composable
public fun AsciiRadio(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val source = remember { MutableInteractionSource() }
    val state = rememberControlState(source, enabled)
    ControlGrid(
        grid = renderMarkerRow(RADIO_MARKERS, selected, label, state, LocalKasciiTheme.current),
        state = state,
        modifier =
            modifier
                .semantics { contentDescription = label }
                .selectable(selected, source, null, enabled, Role.RadioButton, onClick),
    )
}

/** A vertical group of radio buttons, one per entry of [options]. */
@Composable
public fun AsciiRadioGroup(
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSelect: (Int) -> Unit,
) {
    Column(modifier.selectableGroup()) {
        options.forEachIndexed { index, option ->
            AsciiRadio(index == selectedIndex, { onSelect(index) }, option, enabled = enabled)
        }
    }
}

@Composable
private fun ToggleControl(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    role: Role,
    markers: Markers,
    modifier: Modifier,
    enabled: Boolean,
) {
    val source = remember { MutableInteractionSource() }
    val state = rememberControlState(source, enabled)
    ControlGrid(
        grid = renderMarkerRow(markers, checked, label, state, LocalKasciiTheme.current),
        state = state,
        modifier =
            modifier
                .semantics { contentDescription = label }
                .toggleable(checked, source, null, enabled, role, onCheckedChange),
    )
}

internal class Markers(
    val on: String,
    val off: String,
)

internal val CHECKBOX_MARKERS = Markers("[x]", "[ ]")
internal val RADIO_MARKERS = Markers("(*)", "( )")
internal val TOGGLE_MARKERS = Markers("[ON ■]", "[■OFF]")

/** One row: the marker for [on], and the label. A focused marker swaps its outer characters for `>` and `<`. */
internal fun renderMarkerRow(
    markers: Markers,
    on: Boolean,
    label: String,
    state: ControlState,
    theme: KasciiTheme,
): Grid {
    val marker = if (on) markers.on else markers.off
    val grid = Grid(marker.length + if (label.isEmpty()) 0 else label.length + 1, 1)
    val markerColor =
        when {
            !state.enabled -> theme.dim()
            state.pressed -> null
            on -> theme.good()
            else -> null
        }
    grid.write(0, 0, marker, markerColor)
    if (label.isNotEmpty()) grid.write(marker.length + 1, 0, label, if (state.enabled) null else theme.dim())
    if (state.focused) {
        val accent = if (state.pressed) null else theme.accent
        grid.put(0, 0, '>', accent)
        grid.put(marker.length - 1, 0, '<', accent)
    }
    return grid
}
