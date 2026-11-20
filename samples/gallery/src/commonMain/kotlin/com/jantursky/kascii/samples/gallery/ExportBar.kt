package com.jantursky.kascii.samples.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.samples.gallery.examples.ExportFormat
import com.jantursky.kascii.samples.gallery.examples.export
import com.jantursky.kascii.samples.gallery.examples.toPng
import kotlinx.coroutines.launch

/**
 * Buttons that copy the still frame of what is on the stage as text, ANSI and SVG, and hand it over as PNG where the
 * platform can. The frame is the one a piece shows under reduced motion, so Motion "Reduced" shows what is copied.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportBar(
    theme: KasciiTheme,
    name: String,
    frame: () -> Grid,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        ExportFormat.entries.forEach { format ->
            Pill("Copy ${format.label}", {
                scope.launch {
                    clipboard.setClipEntry(plainTextEntry(frame().export(format, theme)))
                    status = "Copied the frame as ${format.label}"
                }
            }, tint = Tint.Amber)
        }
        pngActionLabel?.let { label ->
            Pill(label, {
                scope.launch {
                    status = if (deliverPng(frame().toPng(theme), name)) "Done: PNG" else "The platform refused the PNG"
                }
            }, tint = Tint.Amber)
        }
        BasicText(
            status,
            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = Gallery.type.caption.copy(color = Gallery.colors.green),
        )
    }
}
