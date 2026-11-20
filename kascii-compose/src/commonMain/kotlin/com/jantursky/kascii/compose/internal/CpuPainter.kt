package com.jantursky.kascii.compose.internal

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.jantursky.kascii.core.Grid

private const val MAX_FILTERS = 512

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

    private fun filterFor(argb: Int): ColorFilter {
        if (filters.size >= MAX_FILTERS) filters.clear()
        return filters.getOrPut(argb) { ColorFilter.tint(Color(argb), BlendMode.SrcIn) }
    }
}

/** A cell's color: its own, or [ink] when the grid stores 0 for "theme ink". */
internal fun cellColor(
    stored: Int,
    ink: Int,
): Int = if (stored == 0) ink else stored
