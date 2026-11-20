package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme

/** The state of one slot of an uptime bar. */
public enum class AsciiUptimeState(
    internal val glyph: Char,
) {
    /** The service was up. */
    Up('█'),

    /** The service answered but degraded. */
    Degraded('▄'),

    /** The service was down. */
    Down('▁'),

    /** There is no data for this slot. */
    Unknown('·'),
}

/** An uptime history, one glyph per slot, followed by the share of slots that were up. */
@Composable
public fun AsciiUptimeBar(
    states: List<AsciiUptimeState>,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAsciiStrings.current
    val down = states.count { it == AsciiUptimeState.Down }
    val description = "${strings.uptime} ${uptimeLabel(states)}: $down ${strings.slotsDown}"
    GridSurface(renderUptimeBar(states, LocalKasciiTheme.current), modifier, description)
}

/** The percentage of known slots that were up, or null when no slot has data. */
internal fun uptimePercent(states: List<AsciiUptimeState>): Float? {
    val known = states.filter { it != AsciiUptimeState.Unknown }
    return if (known.isEmpty()) null else known.count { it == AsciiUptimeState.Up } * 100f / known.size
}

private fun uptimeLabel(states: List<AsciiUptimeState>): String =
    uptimePercent(states)?.let { "${it.pretty()}%" } ?: "n/a"

internal fun renderUptimeBar(
    states: List<AsciiUptimeState>,
    theme: KasciiTheme,
): Grid {
    val label = " ${uptimeLabel(states)} uptime"
    val grid = Grid(states.size + label.length, 1)
    states.forEachIndexed { x, state ->
        val color =
            when (state) {
                AsciiUptimeState.Up -> theme.good()
                AsciiUptimeState.Degraded -> theme.warn()
                AsciiUptimeState.Down -> theme.bad()
                AsciiUptimeState.Unknown -> theme.dim()
            }
        grid.put(x, 0, state.glyph, color)
    }
    grid.write(states.size, 0, label)
    return grid
}
