package com.jantursky.kascii.samples.gallery

import androidx.compose.ui.graphics.ImageBitmap

// Android has no plain way to put an image on the clipboard without a FileProvider, so the button is hidden.
internal actual val pngActionLabel: String? = null

internal actual suspend fun deliverPng(
    image: ImageBitmap,
    name: String,
): Boolean = false
