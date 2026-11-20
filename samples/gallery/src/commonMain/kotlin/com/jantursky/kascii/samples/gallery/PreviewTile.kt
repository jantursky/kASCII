package com.jantursky.kascii.samples.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** A picker whose options show what they do: a labeled preview, one of which is selected. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> PreviewPicker(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    paper: Color,
    preview: @Composable (T, Modifier) -> Unit,
) {
    val colors = Gallery.colors
    val shape = RoundedCornerShape(8.dp)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val on = option == selected
            Column(
                Modifier
                    .width(112.dp)
                    .pressable(Role.RadioButton, shape) { onSelect(option) }
                    .semantics { this.selected = on }
                    .background(if (on) colors.surface3 else colors.surface2)
                    .border(if (on) 2.dp else 1.dp, if (on) colors.green else colors.border, shape)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                preview(option, Modifier.width(96.dp).height(56.dp).background(paper))
                BasicText(
                    label(option),
                    style = Gallery.type.caption.copy(color = if (on) colors.strong else colors.text),
                    maxLines = 1,
                )
            }
        }
    }
}
