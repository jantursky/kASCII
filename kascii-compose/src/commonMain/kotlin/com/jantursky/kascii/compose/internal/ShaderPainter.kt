package com.jantursky.kascii.compose.internal

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.Grid

/** Where [AsciiRenderer.Auto] switches to the shader: more cells than a frame of image copies can reasonably cost. */
internal const val AUTO_SHADER_CELLS = 2_000

/** Draws a glyph grid with one fragment shader. One painter per node, so compiled shaders are never shared. */
internal interface ShaderPainter {
    /** Draws [frame] from the scope's origin, and throws if the platform rejects the program. */
    fun draw(
        scope: DrawScope,
        frame: ShaderFrame,
    )
}

/**
 * One frame as the shader reads it. [cells] is a `2 * cols` by `rows` image: per cell a high and a low byte of the
 * atlas slot plus one, stored as grays so color management leaves them alone, 0 for an empty cell. [tints] is a
 * `cols` by `rows` image of unpremultiplied ARGB colors.
 */
internal class ShaderFrame(
    val cols: Int,
    val rows: Int,
    val atlas: GlyphAtlas,
    val cells: IntArray,
    val tints: IntArray,
)

/** A painter for this platform, or null when it has no usable runtime shaders. */
internal expect fun createShaderPainter(): ShaderPainter?

/** Whether [renderer] asks for the shader for a grid of [cells] cells. */
internal fun wantsShader(
    renderer: AsciiRenderer,
    cells: Int,
): Boolean =
    renderer == AsciiRenderer.Gpu ||
        (renderer == AsciiRenderer.Auto && cells > AUTO_SHADER_CELLS)

/** Encodes [grid] for the shader against [atlas], with [ink] for cells that use the theme ink. */
internal fun shaderFrame(
    grid: Grid,
    atlas: GlyphAtlas,
    ink: Int,
): ShaderFrame {
    val n = grid.cols * grid.rows
    val cells = IntArray(n * 2)
    val tints = IntArray(n)
    for (i in 0 until n) {
        val slot = atlas.slotOf(grid.glyphs[i]) + 1
        cells[i * 2] = gray(slot shr 8)
        cells[i * 2 + 1] = gray(slot and 0xFF)
        tints[i] = cellColor(grid.colors[i], ink)
    }
    return ShaderFrame(grid.cols, grid.rows, atlas, cells, tints)
}

private fun gray(v: Int): Int = (0xFF shl 24) or (v shl 16) or (v shl 8) or v

/**
 * The program, the same in AGSL and SkSL. Each pixel finds its cell, decodes the atlas slot, reads the glyph's
 * coverage from the atlas at the same spot inside the slot and tints it.
 */
internal const val GLYPH_SHADER: String = """
uniform shader atlas;
uniform shader cells;
uniform shader tints;
uniform float2 cell;
uniform float2 grid;
uniform float atlasColumns;

half4 main(float2 p) {
    float2 c = floor(p / cell);
    if (c.x < 0.0 || c.y < 0.0 || c.x >= grid.x || c.y >= grid.y) return half4(0.0);
    float hi = floor(float(cells.eval(float2(c.x * 2.0 + 0.5, c.y + 0.5)).r) * 255.0 + 0.5);
    float lo = floor(float(cells.eval(float2(c.x * 2.0 + 1.5, c.y + 0.5)).r) * 255.0 + 0.5);
    float slot = hi * 256.0 + lo - 1.0;
    if (slot < 0.0) return half4(0.0);
    float row = floor((slot + 0.5) / atlasColumns);
    float2 origin = float2(slot - row * atlasColumns, row) * cell;
    half coverage = atlas.eval(origin + p - c * cell).a;
    return tints.eval(c + 0.5) * coverage;
}
"""
