package com.jantursky.kascii.compose.internal

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.compose.toColor
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme

/**
 * Everything one view needs to draw grids: a pixel measurer, the atlas, the atlas painter and, when asked for, the
 * shader painter. A program the platform rejects is remembered, so a view falls back to the atlas painter once
 * instead of failing every frame.
 */
internal class GlyphRenderer {
    private var resolver: FontFamily.Resolver? = null
    private var measurer: TextMeasurer? = null
    private var measuredFont: FontFamily? = null
    private var advance = DEFAULT_ADVANCE_EM
    private val atlases = AtlasCache()
    private val cpu = CpuPainter()
    private var shader: ShaderPainter? = null
    private var shaderCreated = false
    private var shaderBroken = false

    private fun measurerFor(resolver: FontFamily.Resolver): TextMeasurer {
        measurer?.let { if (this.resolver === resolver) return it }
        val created = pixelMeasurer(resolver)
        this.resolver = resolver
        measurer = created
        measuredFont = null
        return created
    }

    /** The glyph advance of [font] in ems, measured once per font and resolver. */
    fun advanceEm(
        font: FontFamily,
        resolver: FontFamily.Resolver,
    ): Float {
        val m = measurerFor(resolver)
        if (measuredFont != font) {
            advance = m.advanceEm(font)
            measuredFont = font
        }
        return advance
    }

    /** Paints [theme]'s paper over the whole scope, then [grid] at [layout], clipped to the scope. */
    fun draw(
        scope: DrawScope,
        grid: Grid,
        layout: GridLayout,
        font: FontFamily,
        resolver: FontFamily.Resolver,
        theme: KasciiTheme,
        renderer: AsciiRenderer,
        glyphs: String = "",
    ) {
        scope.drawRect(theme.paper.toColor())
        // An empty grid would hand the shader empty images, which it rejects for good.
        if (grid.cols == 0 || grid.rows == 0) return
        val atlas = atlases.atlasFor(grid, glyphs, layout.metrics, font, measurerFor(resolver))
        val ink = theme.ink.argb
        scope.clipRect {
            translate(layout.x.toFloat(), layout.y.toFloat()) {
                if (!drewWithShader(this, grid, atlas, ink, renderer)) cpu.draw(this, grid, atlas, ink)
            }
        }
    }

    private fun drewWithShader(
        scope: DrawScope,
        grid: Grid,
        atlas: GlyphAtlas,
        ink: Int,
        renderer: AsciiRenderer,
    ): Boolean {
        if (shaderBroken || !wantsShader(renderer, grid.cols * grid.rows)) return false
        if (!shaderCreated) {
            shader = createShaderPainter()
            shaderCreated = true
        }
        val painter = shader ?: return false
        return try {
            painter.draw(scope, shaderFrame(grid, atlas, ink))
            true
        } catch (_: Exception) {
            shaderBroken = true
            false
        }
    }
}
