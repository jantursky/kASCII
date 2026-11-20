package com.jantursky.kascii.compose.internal

import androidx.compose.ui.graphics.ImageBitmap

// Android's hardware renderer already caches the texture of a bitmap between draws.
internal actual fun ImageBitmap.freezePixels() {}
