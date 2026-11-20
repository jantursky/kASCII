package com.jantursky.kascii.samples.gallery

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry

internal actual fun plainTextEntry(text: String): ClipEntry = ClipEntry(ClipData.newPlainText("Kascii frame", text))
