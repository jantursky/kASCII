package com.jantursky.kascii.compose.internal

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap

internal actual fun ImageBitmap.freezePixels() {
    asSkiaBitmap().setImmutable()
}
