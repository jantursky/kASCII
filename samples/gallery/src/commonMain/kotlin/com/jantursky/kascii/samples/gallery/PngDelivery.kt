package com.jantursky.kascii.samples.gallery

import androidx.compose.ui.graphics.ImageBitmap

/** The label of the button that hands a rendered frame over as PNG, or null where the platform has no way to. */
internal expect val pngActionLabel: String?

/** Copies or saves [image] as a PNG called [name]. Returns false when the platform refused. */
internal expect suspend fun deliverPng(
    image: ImageBitmap,
    name: String,
): Boolean
