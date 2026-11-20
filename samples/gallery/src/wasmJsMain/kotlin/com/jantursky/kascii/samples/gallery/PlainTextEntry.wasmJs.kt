package com.jantursky.kascii.samples.gallery

import androidx.compose.ui.platform.ClipEntry

internal actual fun plainTextEntry(text: String): ClipEntry = ClipEntry.withPlainText(text)
