package com.jantursky.kascii.compose.internal

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.Charset
import com.jantursky.kascii.core.ColorMode
import com.jantursky.kascii.core.Field
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.Rasterizer
import kotlin.math.ceil

/** Where the grid sits in the view: its cell, its size in cells and the pixel offset of its top-left corner. */
internal data class GridLayout(
    val metrics: CellMetrics,
    val cols: Int,
    val rows: Int,
    val x: Int = 0,
    val y: Int = 0,
) {
    val cells: Int get() = cols * rows
}

/** What a view draws and how its grid fits the view. */
internal sealed interface GridSource {
    /** Glyphs worth having in the atlas up front, so it is not rebuilt while frames reveal them. */
    val glyphs: String

    /** Lays the grid out in a view of [size] pixels. [cellPx] is the requested cell width where the source has one. */
    fun layout(
        size: Size,
        advanceEm: Float,
        cellPx: Float,
    ): GridLayout

    /** The view's size when nothing constrains it. */
    fun preferredSize(
        advanceEm: Float,
        cellPx: Float,
    ): IntSize

    /** The grid at time [t], drawn into [reuse] when it has the right size. */
    fun frame(
        t: Float,
        layout: GridLayout,
        theme: KasciiTheme,
        reuse: Grid?,
    ): Grid
}

/** A field sampled over as many cells of the given width as cover the view. */
internal data class FieldSource(
    val field: Field,
    val charset: Charset,
    val colorMode: ColorMode,
) : GridSource {
    override val glyphs: String by lazy { charset.glyphSet() }

    override fun layout(
        size: Size,
        advanceEm: Float,
        cellPx: Float,
    ): GridLayout {
        val metrics = cellMetrics(cellPx, advanceEm)
        return GridLayout(metrics, ceil(size.width / metrics.width).toInt(), ceil(size.height / metrics.height).toInt())
    }

    override fun preferredSize(
        advanceEm: Float,
        cellPx: Float,
    ): IntSize = IntSize.Zero

    override fun frame(
        t: Float,
        layout: GridLayout,
        theme: KasciiTheme,
        reuse: Grid?,
    ): Grid {
        val into = reuse?.takeIf { it.cols == layout.cols && it.rows == layout.rows } ?: Grid(layout.cols, layout.rows)
        return Rasterizer.rasterize(
            field,
            charset,
            layout.cols,
            layout.rows,
            t,
            layout.metrics.aspect,
            colorMode,
            theme,
            into,
        )
    }
}

/** A grid of a fixed size, scaled to the largest whole-pixel cell that fits and centred. */
internal sealed class FittedSource(
    private val cols: Int,
    private val rows: Int,
) : GridSource {
    override fun layout(
        size: Size,
        advanceEm: Float,
        cellPx: Float,
    ): GridLayout {
        val metrics = fitCell(size.width, size.height, cols, rows, advanceEm)
        val x = ((size.width - cols * metrics.width) / 2).toInt().coerceAtLeast(0)
        val y = ((size.height - rows * metrics.height) / 2).toInt().coerceAtLeast(0)
        return GridLayout(metrics, cols, rows, x, y)
    }

    override fun preferredSize(
        advanceEm: Float,
        cellPx: Float,
    ): IntSize {
        val metrics = cellMetrics(cellPx, advanceEm)
        return IntSize(cols * metrics.width, rows * metrics.height)
    }
}

/** A piece, drawn into a cleared grid of its own size every frame. */
internal class PieceSource(
    val piece: AsciiPiece,
) : FittedSource(piece.meta.cols, piece.meta.rows) {
    override val glyphs: String get() = ""

    override fun frame(
        t: Float,
        layout: GridLayout,
        theme: KasciiTheme,
        reuse: Grid?,
    ): Grid {
        val grid = reuse ?: Grid(piece.meta.cols, piece.meta.rows)
        grid.clear()
        piece.draw(t, grid)
        return grid
    }

    override fun equals(other: Any?): Boolean = other is PieceSource && other.piece === piece

    override fun hashCode(): Int = piece.hashCode()
}

/** A grid that is drawn as it is. */
internal class StaticSource(
    val grid: Grid,
) : FittedSource(grid.cols, grid.rows) {
    override val glyphs: String by lazy { glyphsOf(grid) }

    override fun frame(
        t: Float,
        layout: GridLayout,
        theme: KasciiTheme,
        reuse: Grid?,
    ): Grid = grid

    override fun equals(other: Any?): Boolean = other is StaticSource && other.grid === grid

    override fun hashCode(): Int = grid.hashCode()
}
