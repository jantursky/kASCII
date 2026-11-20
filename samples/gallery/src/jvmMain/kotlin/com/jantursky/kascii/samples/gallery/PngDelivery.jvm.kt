package com.jantursky.kascii.samples.gallery

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.HeadlessException
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage

internal actual val pngActionLabel: String? = "Copy PNG"

internal actual suspend fun deliverPng(
    image: ImageBitmap,
    name: String,
): Boolean =
    withContext(Dispatchers.IO) {
        try {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(ImageTransferable(image.toAwtImage()), null)
            true
        } catch (_: HeadlessException) {
            false
        } catch (_: IllegalStateException) {
            false
        }
    }

private class ImageTransferable(
    private val image: BufferedImage,
) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.imageFlavor

    override fun getTransferData(flavor: DataFlavor): Any =
        if (isDataFlavorSupported(flavor)) image else throw UnsupportedFlavorException(flavor)
}
