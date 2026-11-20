package com.jantursky.kascii.samples.gallery.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.components.AsciiBadge
import com.jantursky.kascii.components.AsciiButton
import com.jantursky.kascii.components.AsciiLineChart
import com.jantursky.kascii.components.AsciiStatCard
import com.jantursky.kascii.components.AsciiTerminal
import kotlin.math.roundToInt

private const val MAX_COLS = 60
private const val CARD_COLS = 28
private const val LOG_ROWS = 8

/** The detail of the selected server: key figures, the load, its log and the restart. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailScreen(
    state: TerminalState,
    cols: Int,
    modifier: Modifier = Modifier,
) {
    val server = state.selected
    if (server == null) {
        Column(modifier, Arrangement.spacedBy(8.dp)) {
            AppText("No server is open.", muted = true)
            AsciiButton("Open the list") { state.show(Screen.Servers) }
        }
        return
    }
    Column(modifier.verticalScroll(rememberScrollState()), Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppText("${server.id} in ${server.region}", Modifier.semantics { heading() })
            AsciiBadge(server.health.label, tone = server.health.tone)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val width = minOf(CARD_COLS, cols)
            AsciiStatCard(
                "Load, percent",
                "${(server.currentLoad * 100).roundToInt()}",
                delta = server.loadChange,
                trend = server.load,
                cols = width,
            )
            AsciiStatCard("Requests per minute", "${server.requests}", trend = server.load, cols = width)
        }
        AsciiLineChart(server.load, cols = minOf(cols, MAX_COLS), rows = 6)
        AsciiTerminal(server.log, cols = minOf(cols, MAX_COLS), rows = LOG_ROWS)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AsciiButton("Restart", onClick = state::askRestart)
            AsciiButton("Back") { state.show(Screen.Servers) }
        }
    }
}
