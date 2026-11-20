package com.jantursky.kascii.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.components.AsciiBootEntry
import com.jantursky.kascii.components.AsciiBootLog
import com.jantursky.kascii.components.AsciiBootStatus
import com.jantursky.kascii.components.AsciiNotFound
import com.jantursky.kascii.components.AsciiTerminal

private val Boot =
    listOf(
        AsciiBootEntry("Mount filesystems"),
        AsciiBootEntry("Start network", AsciiBootStatus.Warn),
        AsciiBootEntry("Load gallery"),
        AsciiBootEntry("Start renderer", AsciiBootStatus.Fail),
    )

// The terminal shows the lines it is given and types the line in progress; the boot log reveals its entries one by
// one. Under reduced motion both show their final state at once.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TerminalExample(modifier: Modifier = Modifier) {
    FlowRow(modifier, Arrangement.spacedBy(32.dp), Arrangement.spacedBy(24.dp)) {
        AsciiTerminal(
            lines = listOf("$ kascii --version", "kascii 1.0.0", "$ ls", "core  compose  components"),
            cols = 36,
            rows = 8,
            typing = "kascii run gallery",
        )
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AsciiBootLog(Boot, cols = 36)
            AsciiNotFound(message = "Nothing here")
        }
    }
}
