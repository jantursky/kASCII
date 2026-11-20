package com.jantursky.kascii.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.BoxStyle

/**
 * A modal dialog: a double-framed card with [title] and [message], and a confirm button, plus a dismiss button
 * when [dismissLabel] is set. Escape or a tap outside calls [onDismissRequest].
 */
@Composable
public fun AsciiDialog(
    title: String,
    message: String,
    onDismissRequest: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String? = null,
    cols: Int = 40,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier.background(Color(LocalKasciiTheme.current.paper.argb)).semantics { paneTitle = title },
            horizontalAlignment = Alignment.End,
        ) {
            AsciiCard(message, title = title, cols = cols, border = BoxStyle.Double)
            Row {
                dismissLabel?.let { AsciiButton(it, onDismissRequest) }
                AsciiButton(confirmLabel, onConfirm)
            }
        }
    }
}
