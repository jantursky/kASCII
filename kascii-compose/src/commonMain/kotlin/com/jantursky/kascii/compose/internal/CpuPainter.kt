package com.jantursky.kascii.compose.internal

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.jantursky.kascii.core.Grid

/** Exact colors cached before new ones snap to a 4-bit grid, so a picture of thousands of colors stays bounded. */
private const val MAX_EXACT_FILTERS = 256

/** Keeps the top four bits of each channel and centers the color in that step. */
private fun snap(argb: Int): Int = (argb and 0xFF000000.toInt()) or (argb and 0x00F0F0F0) or 0x00080808

/** Copies atlas slots to cells, tinted per cell. The tint filters are cached by color across frames. */
internal class CpuPainter {
    private val filters = HashMap<Int, ColorFilter>()

    fun draw(
        scope: DrawScope,
        grid: Grid,
        atlas: GlyphAtlas,
        ink: Int,
    ) {
        val w = atlas.metrics.width
        val h = atlas.metrics.height
        val size = IntSize(w, h)
        for (row in 0 until grid.rows) {
            for (col in 0 until grid.cols) {
                val i = row * grid.cols + col
                val slot = atlas.slotOf(grid.glyphs[i])
                if (slot < 0) continue
                scope.drawImage(
                    image = atlas.image,
                    srcOffset = atlas.layout.offset(slot),
                    srcSize = size,
                    dstOffset = IntOffset(col * w, row * h),
                    dstSize = size,
                    colorFilter = filterFor(cellColor(grid.colors[i], ink)),
                    filterQuality = FilterQuality.None,
                )
            }
        }
    }

    /**
     * One filter per color, kept for good. Every filter is a native object the collector cannot see, so building
     * thousands of them each frame, as a picture with a color per cell does, can exhaust memory on a phone. Past
     * [MAX_EXACT_FILTERS] colors, new ones share a filter with the nearest color on a coarse grid, which bounds the
     * cache at a few thousand entries.
     */
    private fun filterFor(argb: Int): ColorFilter {
        filters[argb]?.let { return it }
        val key = if (filters.size < MAX_EXACT_FILTERS) argb else snap(argb)
        return filters.getOrPut(key) { ColorFilter.tint(Color(key), BlendMode.SrcIn) }
    }
}

/** A cell's color: its own, or [ink] when the grid stores 0 for "theme ink". */
internal fun cellColor(
    stored: Int,
    ink: Int,
): Int = if (stored == 0) ink else stored
