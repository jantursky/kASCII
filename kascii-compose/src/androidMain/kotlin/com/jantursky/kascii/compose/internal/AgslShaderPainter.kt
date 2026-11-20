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

    /**
     * Two bitmaps used in turn. The render thread may still read the previous frame's bitmap while the next frame
     * is written, so a frame never overwrites the one before it.
     */
    private class FrameBitmap {
        private val bitmaps = arrayOfNulls<Bitmap>(2)
        private var next = 0

        fun update(
            argb: IntArray,
            width: Int,
            height: Int,
        ): Bitmap {
            val current =
                bitmaps[next]?.takeIf { it.width == width && it.height == height }
                    ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmaps[next] = it }
            next = 1 - next
            current.setPixels(argb, 0, width, 0, 0, width, height)
            return current
        }
    }
}
