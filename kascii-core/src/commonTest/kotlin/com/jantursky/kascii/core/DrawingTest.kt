package com.jantursky.kascii.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DrawingTest {
    @Test
    fun containsChecksBothAxes() {
        val g = Grid(3, 2)
        assertTrue(g.contains(2, 1))
        assertTrue(!g.contains(3, 0))
        assertTrue(!g.contains(0, -1))
    }

    @Test
    fun negativeGridSizeIsRejected() {
        assertFailsWith<IllegalArgumentException> { Grid(-1, 2) }
    }

    @Test
    fun fillWritesARectangleAndClips() {
        val g = Grid(4, 3)
        g.fill(2, 1, 5, 5, '#')
        assertEquals("    \n  ##\n  ##", g.toText())
    }

    @Test
    fun glyphAtOutsideIsASpace() {
        assertEquals(' ', Grid(2, 2).glyphAt(9, 9))
        assertNull(Grid(2, 2).colorAt(9, 9))
    }

    @Test
    fun rectWithFillAndNoBorder() {
        val g = Grid(3, 2)
        g.rect(0, 0, 3, 2, border = null, fillGlyph = '.')
        assertEquals("...\n...", g.toText())
    }

    @Test
    fun rectIgnoresEmptySizes() {
        val g = Grid(2, 2)
        g.rect(0, 0, 0, 2)
        g.rect(0, 0, 2, 0)
        assertEquals("  \n  ", g.toText())
    }

    @Test
    fun boxStylesAreDistinct() {
        val styles = listOf(BoxStyle.Single, BoxStyle.Double, BoxStyle.Rounded, BoxStyle.Heavy, BoxStyle.Ascii)
        assertEquals(styles.size, styles.map { it.topLeft }.toSet().size)
        assertEquals('+', BoxStyle.Ascii.topLeft)
    }

    @Test
    fun discDrawsInsideItsRadiusOnly() {
        val g = Grid(21, 11)
        g.disc(10.5f, 5.5f, 5f, '@')
        assertEquals('@', g.glyphAt(10, 5))
        assertEquals(' ', g.glyphAt(0, 0))
        assertEquals(' ', g.glyphAt(20, 10))
    }

    @Test
    fun labelCentresText() {
        val g = Grid(9, 1)
        g.label(4, 0, "abc")
        assertEquals("   abc   ", g.toText())
    }

    @Test
    fun brailleLineLightsBothEndsAndClipsOutside() {
        val c = BrailleCanvas(3, 1)
        c.line(0, 0, 5, 3)
        c.plot(-1, 0)
        c.plot(0, 99)
        val g = Grid(3, 1)
        c.drawTo(g, 0, 0)
        assertEquals('⠑', g.glyphAt(0, 0))
        assertTrue(g.glyphAt(2, 0).code > 0x2800)
        assertEquals(6, c.dotWidth)
        assertEquals(4, c.dotHeight)
    }

    @Test
    fun rngRejectsBadBoundAndChanceFollowsProbability() {
        val r = Rng(5)
        assertFailsWith<IllegalArgumentException> { r.int(0) }
        assertTrue((0 until 200).none { r.chance(0f) })
        assertTrue((0 until 200).all { r.chance(1.1f) })
    }

    @Test
    fun blendingColors() {
        val mid = Rgb.Black.lerp(Rgb.White, 0.5f)
        assertEquals(127, mid.red)
        assertEquals(Rgb.White, Rgb.Black.lerp(Rgb.White, 2f))
        assertEquals(Rgb.Black, Rgb.Black.lerp(Rgb.White, -1f))
        assertEquals("#ffffff", Rgb.White.toHex())
    }

    @Test
    fun badHexIsRejected() {
        assertFailsWith<IllegalArgumentException> { Rgb.fromHex("#12") }
    }

    @Test
    fun gradientColorModeBlendsBetweenStops() {
        val mode = ColorMode.Gradient(listOf(Rgb.Black, Rgb.White))
        val g = Rasterizer.rasterize(Fields.gradient(), Charsets.Standard, 3, 1, colorMode = mode)
        assertNotNull(g.colorAt(2, 0))
        assertTrue(g.colorAt(2, 0)!!.red > g.colorAt(0, 0)!!.red)
    }

    @Test
    fun sourceColorModeReadsTheField() {
        val red = Rgb.fromHex("#ff0000")
        val mode = ColorMode.Source(ColorField { _, _, _ -> red })
        val g = Rasterizer.rasterize(Fields.solid(1f), Charsets.Standard, 2, 1, colorMode = mode)
        assertEquals(red, g.colorAt(1, 0))
    }

    @Test
    fun builtInThemesHaveReadableInk() {
        for (theme in listOf(KasciiTheme.Light, KasciiTheme.Dark, KasciiTheme.Phosphor, KasciiTheme.Amber)) {
            assertTrue(theme.contrastRatio() >= 4.5f)
        }
        assertFailsWith<IllegalArgumentException> { KasciiTheme(Rgb.Black, Rgb.White, palette = emptyList()) }
    }
}
