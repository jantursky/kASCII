package com.jantursky.kascii.samples.gallery

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIPasteboard

internal actual val pngActionLabel: String? = "Copy PNG"

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal actual suspend fun deliverPng(
    image: ImageBitmap,
    name: String,
): Boolean {
    val bytes = Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)?.bytes ?: return false
    val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    UIPasteboard.generalPasteboard.image = UIImage.imageWithData(data) ?: return false
    return true
}
