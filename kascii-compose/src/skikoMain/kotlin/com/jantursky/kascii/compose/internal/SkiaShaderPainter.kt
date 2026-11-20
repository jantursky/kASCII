package com.jantursky.kascii.compose.internal

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.skiaCanvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Shader

/** Draws with a Skia runtime effect, which desktop, iOS and web all have. */
internal class SkiaShaderPainter : ShaderPainter {
    private val effect by lazy { RuntimeEffect.makeForShader(GLYPH_SHADER) }
    private var atlasSource: GlyphAtlas? = null
    private var atlasShader: Shader? = null

    /** The atlas as a nearest-sampled image shader, rebuilt only when the atlas changes. */
    private fun atlasShaderFor(atlas: GlyphAtlas): Shader {
        atlasShader?.let { if (atlasSource === atlas) return it }
        atlasShader?.close()
        val shader = Image.makeFromBitmap(atlas.image.asSkiaBitmap()).toNearestShader()
        atlasSource = atlas
        atlasShader = shader
        return shader
    }

    override fun draw(
        scope: DrawScope,
        frame: ShaderFrame,
    ) {
        val metrics = frame.atlas.metrics
        val builder = RuntimeShaderBuilder(effect)
        val cells = rasterShader(frame.cells, frame.cols * 2, frame.rows)
        val tints = rasterShader(frame.tints, frame.cols, frame.rows)
        val paint = Paint()
        try {
            builder.child("atlas", atlasShaderFor(frame.atlas))
            builder.child("cells", cells)
            builder.child("tints", tints)
            builder.uniform("cell", metrics.width.toFloat(), metrics.height.toFloat())
            builder.uniform("grid", frame.cols.toFloat(), frame.rows.toFloat())
            builder.uniform(
                "atlasColumns",
                frame.atlas.layout.columns
                    .toFloat(),
            )
            paint.shader = builder.makeShader()
            val rect = Rect.makeWH(frame.cols * metrics.width.toFloat(), frame.rows * metrics.height.toFloat())
            scope.drawIntoCanvas { it.skiaCanvas.drawRect(rect, paint) }
        } finally {
            paint.close()
            builder.close()
            cells.close()
            tints.close()
        }
    }
}

/** A nearest-sampled shader of the image, which is closed once the shader holds it. */
private fun Image.toNearestShader(): Shader =
    try {
        makeShader(FilterTileMode.CLAMP, FilterTileMode.CLAMP, SamplingMode.DEFAULT)
    } finally {
        close()
    }

/** An unpremultiplied ARGB array as a nearest-sampled image shader. */
private fun rasterShader(
    argb: IntArray,
    width: Int,
    height: Int,
): Shader {
    val bytes = ByteArray(argb.size * 4)
    argb.forEachIndexed { i, c ->
        bytes[i * 4] = (c shr 16).toByte()
        bytes[i * 4 + 1] = (c shr 8).toByte()
        bytes[i * 4 + 2] = c.toByte()
        bytes[i * 4 + 3] = (c ushr 24).toByte()
    }
    val info = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL)
    return Image.makeRaster(info, bytes, width * 4).toNearestShader()
}
