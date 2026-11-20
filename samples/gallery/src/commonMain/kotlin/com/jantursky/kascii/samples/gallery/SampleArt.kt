package com.jantursky.kascii.samples.gallery

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

private const val WIDTH = 320
private const val HEIGHT = 160

/** A small picture drawn in code, so the gallery needs no image files: three discs and a diagonal band. */
fun sampleArt(): ImageBitmap {
    val image = ImageBitmap(WIDTH, HEIGHT)
    val size = Size(WIDTH.toFloat(), HEIGHT.toFloat())
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), size) {
        drawRect(Color(0xFF06051D))
        drawCircle(Color(0xFF7F52FF), radius = 70f, center = Offset(110f, 80f))
        drawCircle(Color(0xFFE44857), radius = 55f, center = Offset(190f, 80f))
        drawCircle(Color(0xFFF0B100), radius = 32f, center = Offset(150f, 80f))
        drawLine(Color(0xFFCAD5E2), Offset(0f, 150f), Offset(WIDTH.toFloat(), 10f), strokeWidth = 10f)
    }
    return image
}
