package com.jantursky.kascii.samples.gallery

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import kotlin.io.encoding.Base64

internal actual val pngActionLabel: String? = "Save PNG"

internal actual suspend fun deliverPng(
    image: ImageBitmap,
    name: String,
): Boolean {
    val data = Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG) ?: return false
    save("$name.png", "data:image/png;base64," + Base64.encode(data.bytes))
    return true
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun save(
    name: String,
    href: String,
): Unit =
    js(
        "{ const a = document.createElement('a'); a.href = href; a.download = name; " +
            "document.body.appendChild(a); a.click(); a.remove(); }",
    )
