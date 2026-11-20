package com.jantursky.kascii.compose

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import com.jantursky.kascii.compose.internal.CellMetrics
import com.jantursky.kascii.compose.internal.FieldSource
import com.jantursky.kascii.compose.internal.GridLayout
import com.jantursky.kascii.compose.internal.PieceSource
import com.jantursky.kascii.compose.internal.StaticSource
import com.jantursky.kascii.compose.internal.cellMetrics
import com.jantursky.kascii.compose.internal.fitCell
import com.jantursky.kascii.core.Charsets
import com.jantursky.kascii.core.ColorMode
import com.jantursky.kascii.core.Fields
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.asciiPiece
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CellMetricsTest {
    private val field = FieldSource(Fields.solid(1f), Charsets.Standard, ColorMode.Theme)

    @Test
    fun aCellIsWholePixelsWithTheFontSizeThatFillsItsWidth() {
        val m = cellMetrics(8.4f, 0.6f)
        assertEquals(8, m.width)
        assertEquals(8 / 0.6f, m.fontSize)
        // 13.33 px font times a 1.32 em line.
        assertEquals(18, m.height)
        assertEquals(18f / 8f, m.aspect)
    }

    @Test
    fun aCellIsNeverSmallerThanOnePixel() {
        val m = cellMetrics(0.1f, 0.6f)
        assertEquals(1, m.width)
        assertTrue(m.height >= 1)
    }

    @Test
    fun aFittedCellIsTheLargestThatKeepsTheWholeGridInside() {
        val m = fitCell(400f, 300f, 40, 10, 0.6f)
        assertTrue(m.width * 40 <= 400 && m.height * 10 <= 300, "$m")
        val bigger = cellMetrics(m.width + 1f, 0.6f)
        assertTrue(bigger.width * 40 > 400 || bigger.height * 10 > 300, "$bigger would fit too")
    }

    @Test
    fun aFittedCellIsLimitedByHeightWhenTheViewIsWide() {
        val m = fitCell(1000f, 44f, 10, 2, 0.6f)
        assertTrue(m.height * 2 <= 44, "$m")
        assertEquals(m.height, cellMetrics(m.width.toFloat(), 0.6f).height)
    }

    @Test
    fun anEmptyGridFitsAOnePixelCell() {
        assertEquals(1, fitCell(100f, 100f, 0, 5, 0.6f).width)
    }

    @Test
    fun aFieldCoversTheWholeViewWithCellsOfTheRequestedWidth() {
        val layout = field.layout(Size(100f, 50f), 0.6f, 8f)
        assertEquals(GridLayout(CellMetrics(8, 18, 8 / 0.6f), 13, 3), layout)
        assertEquals(39, layout.cells)
    }

    @Test
    fun aPieceIsCentredAndPrefersItsDefaultCell() {
        val source = PieceSource(asciiPiece("p", "test", 10, 2) { _, _ -> })
        val layout = source.layout(Size(100f, 100f), 0.6f, 8f)
        assertEquals(10, layout.cols)
        assertEquals((100 - 10 * layout.metrics.width) / 2, layout.x)
        assertEquals((100 - 2 * layout.metrics.height) / 2, layout.y)
        assertEquals(IntSize(80, 36), source.preferredSize(0.6f, 8f))
        assertEquals(IntSize.Zero, field.preferredSize(0.6f, 8f))
    }

    @Test
    fun sourcesAreEqualWhenTheyDrawTheSameThing() {
        val piece = asciiPiece("p", "test", 2, 2) { _, _ -> }
        val grid = Grid(2, 2)
        assertEquals(PieceSource(piece), PieceSource(piece))
        assertEquals(PieceSource(piece).hashCode(), PieceSource(piece).hashCode())
        assertNotEquals(PieceSource(piece), PieceSource(asciiPiece("p", "test", 2, 2) { _, _ -> }))
        assertEquals(StaticSource(grid), StaticSource(grid))
        assertEquals(StaticSource(grid).hashCode(), StaticSource(grid).hashCode())
        assertNotEquals(StaticSource(grid), StaticSource(Grid(2, 2)))
        assertNotEquals<Any>(StaticSource(grid), PieceSource(piece))
    }
}
