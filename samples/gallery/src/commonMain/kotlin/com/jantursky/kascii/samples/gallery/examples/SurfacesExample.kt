package com.jantursky.kascii.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.components.AsciiAvatar
import com.jantursky.kascii.components.AsciiBadge
import com.jantursky.kascii.components.AsciiCard
import com.jantursky.kascii.components.AsciiDivider
import com.jantursky.kascii.components.AsciiDividerStyle
import com.jantursky.kascii.components.AsciiTone
import com.jantursky.kascii.core.BoxStyle

// Surfaces are drawn once from the theme, so they cost nothing at rest. A card and a divider take their width in
// columns, one column being one glyph cell wide.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SurfacesExample(modifier: Modifier = Modifier) {
    FlowRow(modifier, Arrangement.spacedBy(32.dp), Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AsciiCard("A card frames a few lines of text.", title = "Single", cols = 32)
            AsciiCard("Double and rounded frames work too.", title = "Double", cols = 32, border = BoxStyle.Double)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AsciiDividerStyle.entries.forEach { AsciiDivider(cols = 32, style = it, label = it.name) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AsciiTone.entries.forEach { AsciiBadge(it.name.lowercase(), tone = it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Ada Lovelace", "Grace Hopper", "Linus").forEach { AsciiAvatar(it) }
            }
        }
    }
}
