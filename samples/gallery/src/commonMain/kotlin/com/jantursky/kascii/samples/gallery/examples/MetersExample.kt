package com.jantursky.kascii.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.components.AsciiCpuMeters
import com.jantursky.kascii.components.AsciiDivider
import com.jantursky.kascii.components.AsciiEqualizer
import com.jantursky.kascii.components.AsciiGauge
import com.jantursky.kascii.components.AsciiHeartbeat
import com.jantursky.kascii.components.AsciiStatCard
import com.jantursky.kascii.components.AsciiUptimeBar
import com.jantursky.kascii.components.AsciiUptimeState

private val Uptime =
    List(12) { AsciiUptimeState.Up } + AsciiUptimeState.Degraded + List(6) { AsciiUptimeState.Up } +
        AsciiUptimeState.Down + List(4) { AsciiUptimeState.Up }

// Meters show a live reading. The equalizer and the heartbeat are decoration that move on their own and hold a
// still frame under reduced motion.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetersExample(modifier: Modifier = Modifier) {
    FlowRow(modifier, Arrangement.spacedBy(32.dp), Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AsciiDivider(cols = 32, label = "Gauge")
            AsciiGauge(0.72f, cols = 24, label = "CPU")
            AsciiDivider(cols = 32, label = "Cores")
            AsciiCpuMeters(listOf(0.12f, 0.45f, 0.83f, 0.97f), cols = 32)
            AsciiDivider(cols = 32, label = "Uptime")
            AsciiUptimeBar(Uptime)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AsciiDivider(cols = 32, label = "Stat card")
            AsciiStatCard("Requests", "1284", delta = 8.2f, trend = listOf(3f, 4f, 3f, 6f, 7f, 9f), cols = 32)
            AsciiDivider(cols = 32, label = "Equalizer")
            AsciiEqualizer(bands = 16, rows = 5)
            AsciiDivider(cols = 32, label = "Heartbeat")
            AsciiHeartbeat(bpm = 72, cols = 32)
        }
    }
}
