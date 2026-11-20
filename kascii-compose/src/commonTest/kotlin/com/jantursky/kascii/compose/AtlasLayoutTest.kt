package com.jantursky.kascii.compose

import androidx.compose.ui.unit.IntOffset
import com.jantursky.kascii.compose.internal.AtlasLayout
import com.jantursky.kascii.compose.internal.glyphSet
import com.jantursky.kascii.compose.internal.glyphsOf
import com.jantursky.kascii.core.Charsets
import com.jantursky.kascii.core.Grid
import kotlin.test.Test
import kotlin.test.assertEquals

class AtlasLayoutTest {
    @Test
    fun slotsFillANearSquareAtlasRowByRow() {
        val layout = AtlasLayout(10, 8, 18)
        assertEquals(4, layout.columns)
        assertEquals(3, layout.rows)
        assertEquals(32, layout.width)
        assertEquals(54, layout.height)
        assertEquals(IntOffset(0, 0), layout.offset(0))
        assertEquals(IntOffset(24, 0), layout.offset(3))
        assertEquals(IntOffset(8, 18), layout.offset(5))
        assertEquals(IntOffset(8, 36), layout.offset(9))
    }

    @Test
    fun anEmptyAtlasStillHasOneSlot() {
        val layout = AtlasLayout(0, 8, 18)
        assertEquals(8, layout.width)
        assertEquals(18, layout.height)
    }

    @Test
    fun aGridsGlyphsAreSortedDistinctAndWithoutSpaces() {
        val grid = Grid(4, 1).apply { write(0, 0, "b a") }
        assertEquals("#ab", glyphsOf(grid, "#a"))
    }

    @Test
    fun charsetsListEveryGlyphTheyCanProduce() {
        assertEquals(Charsets.Standard.glyphs, Charsets.Standard.glyphSet())
        assertEquals(
            256,
            Charsets.Braille
                .glyphSet()
                .toSet()
                .size,
        )
        assertEquals((0x2800..0x28FF).map { it.toChar() }.toSet(), Charsets.Braille.glyphSet().toSet())
        assertEquals(" ▘▝▀▖▌▞▛▗▚▐▜▄▙▟█", Charsets.Quadrants.glyphSet())
        assertEquals(Charsets.Standard.glyphs + "-/\\|", Charsets.Edge.glyphSet())
    }
}
