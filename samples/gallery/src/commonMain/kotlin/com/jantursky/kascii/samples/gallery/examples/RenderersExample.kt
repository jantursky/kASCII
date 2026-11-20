package com.jantursky.kascii.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.compose.AsciiCanvas
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.Charsets
import com.jantursky.kascii.core.Fields
import com.jantursky.kascii.core.Motion

// The same field through each renderer. Cpu draws from a glyph atlas everywhere, Gpu runs a shader where the platform
// has one (and falls back to Cpu where it has not), Text lays out real, selectable text.
@Composable
fun RenderersExample(
    motion: Motion,
    modifier: Modifier = Modifier,
) {
    val field = Fields.ripples()
    Row(modifier.fillMaxWidth().height(220.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(AsciiRenderer.Cpu, AsciiRenderer.Gpu, AsciiRenderer.Text).forEach { renderer ->
            AsciiCanvas(
                field = field,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                charset = Charsets.Standard,
                cell = 10.dp,
                stillTime = 1.5f,
                motion = motion,
                renderer = renderer,
                contentDescription = "Ripples drawn by the ${renderer.name} renderer",
            )
        }
    }
}
