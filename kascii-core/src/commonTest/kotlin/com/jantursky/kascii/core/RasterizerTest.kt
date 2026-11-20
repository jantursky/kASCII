package com.jantursky.kascii.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RasterizerTest {
    @Test
    fun gridSizeMatchesRequest() {
        val g = Rasterizer.rasterize(Fields.plasma(), Charsets.Standard, 17, 9)
        assertEquals(17, g.cols)
        assertEquals(9, g.rows)
        assertEquals(17 * 9, g.glyphs.size)
    }

    @Test
    fun solidFieldFillsWithOneGlyph() {
        val g = Rasterizer.rasterize(Fields.solid(1f), Charsets.Blocks, 4, 2)
        assertEquals("████\n████", g.toText())
        assertEquals("    \n    ", Rasterizer.rasterize(Fields.solid(0f), Charsets.Blocks, 4, 2).toText())
    }

    @Test
    fun gradientRunsLightToDark() {
        val g = Rasterizer.rasterize(Fields.gradient(), Charsets.Blocks, 5, 1)
        assertEquals(" ░▒▓█", g.toText())
    }

    @Test
    fun brailleUsesTwoByFourSamplesPerCell() {
        // A field that is lit only in the top half of each cell.
        val topHalf = Field { _, y, _, _ -> if ((y * 4 * 2).toInt() % 4 < 2) 1f else 0f }
        val g = Rasterizer.rasterize(topHalf, Charsets.Braille, 2, 2)
        assertEquals("⠛⠛\n⠛⠛", g.toText())
    }

    @Test
    fun sameInputsGiveSameGrid() {
        val a = Rasterizer.rasterize(Fields.noise(seed = 7), Charsets.Dense, 20, 10, t = 1.5f).toText()
        val b = Rasterizer.rasterize(Fields.noise(seed = 7), Charsets.Dense, 20, 10, t = 1.5f).toText()
        assertEquals(a, b)
    }

    @Test
    fun differentSeedChangesNoise() {
        val a = Rasterizer.rasterize(Fields.noise(seed = 1), Charsets.Dense, 20, 10).toText()
        val b = Rasterizer.rasterize(Fields.noise(seed = 2), Charsets.Dense, 20, 10).toText()
        assertTrue(a != b)
    }

    @Test
    fun edgeCharsetFindsAVerticalEdge() {
        val step = Field { x, _, _, _ -> if (x < 0.5f) 0f else 1f }
        val g = Rasterizer.rasterize(step, Charsets.Edge, 8, 4)
        assertEquals('|', g.glyphAt(3, 1))
        assertEquals('|', g.glyphAt(4, 1))
        assertEquals(' ', g.glyphAt(0, 1))
        assertEquals('@', g.glyphAt(7, 1))
    }

    @Test
    fun edgeCharsetFindsAHorizontalEdge() {
        val step = Field { _, y, _, _ -> if (y < 0.5f) 0f else 1f }
        val g = Rasterizer.rasterize(step, Charsets.Edge, 4, 8)
        assertEquals('-', g.glyphAt(1, 3))
    }

    @Test
    fun wrongTargetSizeIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            Rasterizer.rasterize(Fields.plasma(), Charsets.Standard, 4, 4, into = Grid(3, 3))
        }
    }

    @Test
    fun themeColorModeLeavesCellsUncolored() {
        val g = Rasterizer.rasterize(Fields.solid(1f), Charsets.Standard, 2, 2, colorMode = ColorMode.Theme)
        assertNull(g.colorAt(0, 0))
    }

    @Test
    fun monoAndDuotoneColorCells() {
        val red = Rgb.fromHex("#ff0000")
        val mono = Rasterizer.rasterize(Fields.solid(1f), Charsets.Standard, 2, 1, colorMode = ColorMode.Mono(red))
        assertEquals(red, mono.colorAt(1, 0))
        val duo = Rasterizer.rasterize(
            Fields.gradient(), Charsets.Standard, 3, 1,
            colorMode = ColorMode.Duotone(Rgb.Black, Rgb.White),
        )
        assertNotNull(duo.colorAt(0, 0))
        assertTrue(duo.colorAt(2, 0)!!.red > duo.colorAt(0, 0)!!.red)
    }

    @Test
    fun gradientModeNeedsTwoStops() {
        assertFailsWith<IllegalArgumentException> { ColorMode.Gradient(listOf(Rgb.Black)) }
    }

    @Test
    fun paletteModePicksThemeColors() {
        val g = Rasterizer.rasterize(Fields.solid(0f), Charsets.Standard, 1, 1, colorMode = ColorMode.Palette)
        assertEquals(KasciiTheme.Dark.palette.first(), g.colorAt(0, 0))
    }

    @Test
    fun fromTonesMustMatchGridSize() {
        assertFailsWith<IllegalArgumentException> {
            Rasterizer.fromTones(FloatArray(3), Charsets.Standard, 2, 2)
        }
    }
}
