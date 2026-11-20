package com.jantursky.kascii.samples.gallery

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun plainTextEntry(text: String): ClipEntry = ClipEntry.withPlainText(text)
