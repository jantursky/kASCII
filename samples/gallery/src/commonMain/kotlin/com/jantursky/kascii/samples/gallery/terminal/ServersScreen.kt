package com.jantursky.kascii.samples.gallery.terminal

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.components.AsciiBadge
import com.jantursky.kascii.components.AsciiSparkline
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.samples.gallery.Gallery
import com.jantursky.kascii.samples.gallery.pressable

private const val SPARK_READINGS = 12

/** The master list: one row per server, each opening its detail. */
@Composable
internal fun ServersScreen(
    state: TerminalState,
    modifier: Modifier = Modifier,
) {
    val servers = state.visible
    if (servers.isEmpty()) {
        AppText("No server matches the filters in Settings.", modifier, muted = true)
        return
    }
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(servers, key = { it.id }) { server ->
            ServerRow(server, selected = server.id == state.selectedId) { state.open(server.id) }
        }
    }
}

@Composable
private fun ServerRow(
    server: Server,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val frame = if (selected) Color(LocalKasciiTheme.current.accent.argb) else Gallery.colors.border
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(Role.Button, shape, onClick)
            .border(1.dp, frame, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            AppText(server.id)
            AppText(server.region, muted = true)
        }
        AsciiBadge(server.health.label, tone = server.health.tone)
        AsciiSparkline(server.load.takeLast(SPARK_READINGS))
    }
}
