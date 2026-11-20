package com.jantursky.kascii.compose.internal

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope

internal actual fun createShaderPainter(): ShaderPainter? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) AgslShaderPainter() else null

/** Draws with an AGSL runtime shader, which Android has from 13 (API 33). */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class AgslShaderPainter : ShaderPainter {
    private val shader by lazy { RuntimeShader(GLYPH_SHADER) }
    private var atlasSource: GlyphAtlas? = null
    private val cells = FrameBitmap()
    private val tints = FrameBitmap()

    override fun draw(
        scope: DrawScope,
        frame: ShaderFrame,
    ) {
        val metrics = frame.atlas.metrics
        if (atlasSource !== frame.atlas) {
            shader.setInputShader("atlas", nearest(frame.atlas.image.asAndroidBitmap()))
            atlasSource = frame.atlas
        }
        shader.setInputShader("cells", nearest(cells.update(frame.cells, frame.cols * 2, frame.rows)))
        shader.setInputShader("tints", nearest(tints.update(frame.tints, frame.cols, frame.rows)))
        shader.setFloatUniform("cell", metrics.width.toFloat(), metrics.height.toFloat())
        shader.setFloatUniform("grid", frame.cols.toFloat(), frame.rows.toFloat())
        shader.setFloatUniform(
            "atlasColumns",
            frame.atlas.layout.columns
                .toFloat(),
        )
        scope.drawRect(
            ShaderBrush(shader),
            size = Size(frame.cols * metrics.width.toFloat(), frame.rows * metrics.height.toFloat()),
        )
    }

    private fun nearest(bitmap: Bitmap): BitmapShader =
        BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            filterMode = BitmapShader.FILTER_MODE_NEAREST
        }

    /** A bitmap reused across frames while the grid keeps its size. */
    private class FrameBitmap {
        private var bitmap: Bitmap? = null

        fun update(
            argb: IntArray,
            width: Int,
            height: Int,
        ): Bitmap {
            val current =
                bitmap?.takeIf { it.width == width && it.height == height }
                    ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap = it }
            current.setPixels(argb, 0, width, 0, 0, width, height)
            return current
        }
    }
}
