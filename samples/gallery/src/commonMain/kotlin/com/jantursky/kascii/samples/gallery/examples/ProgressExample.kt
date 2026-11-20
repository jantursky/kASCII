package com.jantursky.kascii.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.components.AsciiBarStyle
import com.jantursky.kascii.components.AsciiProgressBar
import com.jantursky.kascii.components.AsciiSkeleton
import com.jantursky.kascii.components.AsciiSpinner
import com.jantursky.kascii.components.AsciiSpinnerStyle

// A null value makes the bar indeterminate. Spinners and skeletons are read as "Loading", or as their label.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressExample(modifier: Modifier = Modifier) {
    FlowRow(modifier, Arrangement.spacedBy(32.dp), Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AsciiBarStyle.entries.forEach { AsciiProgressBar(0.65f, style = it, label = "${it.name} progress") }
            AsciiProgressBar(null, label = "Working")
            AsciiSkeleton(cols = 32, rows = 3)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AsciiSpinnerStyle.entries.forEach { AsciiSpinner(style = it, label = it.name) }
        }
    }
}
