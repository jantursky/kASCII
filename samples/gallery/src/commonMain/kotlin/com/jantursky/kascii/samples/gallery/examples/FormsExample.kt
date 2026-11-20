package com.jantursky.kascii.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.components.AsciiBadge
import com.jantursky.kascii.components.AsciiButton
import com.jantursky.kascii.components.AsciiCheckbox
import com.jantursky.kascii.components.AsciiDivider
import com.jantursky.kascii.components.AsciiRadioGroup
import com.jantursky.kascii.components.AsciiSlider
import com.jantursky.kascii.components.AsciiTextField
import com.jantursky.kascii.components.AsciiToggle

// Every control is a plain composable: it takes its state and reports changes, so the state lives in the caller.
// Tab moves focus between them, Space and Enter press, the arrow keys move the slider.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormsExample(modifier: Modifier = Modifier) {
    var saved by remember { mutableIntStateOf(0) }
    var checked by remember { mutableStateOf(true) }
    var toggled by remember { mutableStateOf(false) }
    var size by remember { mutableIntStateOf(1) }
    var volume by remember { mutableFloatStateOf(0.4f) }
    var name by remember { mutableStateOf("") }
    FlowRow(modifier, Arrangement.spacedBy(32.dp), Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AsciiDivider(cols = 24, label = "Buttons")
            AsciiButton("Save") { saved++ }
            AsciiButton("Disabled", onClick = {}, enabled = false)
            AsciiBadge("saved $saved times")
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AsciiDivider(cols = 24, label = "Choices")
            AsciiCheckbox(checked, { checked = it }, "Remember me")
            AsciiToggle(toggled, { toggled = it }, "Notifications")
            AsciiRadioGroup(listOf("Small", "Medium", "Large"), size) { size = it }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AsciiDivider(cols = 24, label = "Values")
            AsciiSlider(volume, { volume = it }, cols = 24, label = "Volume")
            AsciiTextField(name, { name = it }, label = "Name", cols = 24, placeholder = "Your name")
        }
    }
}
