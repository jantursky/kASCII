package com.jantursky.kascii.compose.internal

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Marks a finished bitmap as read-only. A GPU backed canvas uploads a changeable bitmap as a new texture on every
 * draw call, so an atlas drawn once per cell, as the atlas painter does, copied its pixels hundreds of times per
 * frame and took gigabytes on a phone. A read-only bitmap is uploaded once and reused.
 */
internal expect fun ImageBitmap.freezePixels()
