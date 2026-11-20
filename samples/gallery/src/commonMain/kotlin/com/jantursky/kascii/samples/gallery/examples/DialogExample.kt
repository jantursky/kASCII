package com.jantursky.kascii.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.components.AsciiButton
import com.jantursky.kascii.components.AsciiDialog
import com.jantursky.kascii.components.AsciiToast
import com.jantursky.kascii.components.AsciiTone

// A dialog is a real window layer: Escape and a tap outside dismiss it, and the buttons are reached with Tab.
// A toast is only text, so the caller decides where it sits and when it goes.
@Composable
fun DialogExample(modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    Column(modifier, Arrangement.spacedBy(16.dp)) {
        AsciiButton("Delete file") { open = true }
        if (done) AsciiToast("File deleted", tone = AsciiTone.Success, onDismiss = { done = false })
        AsciiToast("Disk almost full", tone = AsciiTone.Warning)
        AsciiToast("Build failed", tone = AsciiTone.Error)
    }
    if (open) {
        AsciiDialog(
            title = "Delete file",
            message = "This cannot be undone.",
            onDismissRequest = { open = false },
            confirmLabel = "Delete",
            onConfirm = {
                open = false
                done = true
            },
            dismissLabel = "Cancel",
        )
    }
}
