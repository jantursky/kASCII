package com.jantursky.kascii.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GridExportTest {
    private fun sample(): Grid =
        Grid(4, 2).also {
            it.write(0, 0, "ab")
            it.put(2, 0, 'c', Rgb.fromHex("#ff0000"))
            it.write(0, 1, "<&>")
        }

    @Test
    fun writeAndPutClipAtTheEdge() {
        val g = Grid(3, 1)
        g.write(1, 0, "xyz")
        g.put(-1, 0, 'q')
        g.put(0, 5, 'q')
        assertEquals(" xy", g.toText())
    }

    @Test
    fun textGolden() {
        assertEquals("abc \n<&> ", Export.toText(sample()))
        assertEquals("abc\n<&>", Export.toText(sample(), trimEnd = true))
    }

    @Test
    fun ansiGoldenTruecolor() {
        val expected = "ab\u001b[38;2;255;0;0mc\u001b[39m \n<&> "
        assertEquals(expected, Export.toAnsi(sample()))
    }

    @Test
    fun ansiSixteenSnapsToBasicRed() {
        assertTrue(Export.toAnsi(sample(), truecolor = false).contains("\u001b[91mc"))
    }

    @Test
    fun svgGoldenAndEscaped() {
        val svg = Export.toSvg(sample(), KasciiTheme.Dark, cellWidth = 10f, cellHeight = 20f, fontFamily = "mono")
        val expected =
            """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40" width="40" height="40">""" +
                """<rect width="100%" height="100%" fill="#06051d"/>""" +
                """<g font-family="mono" font-size="16" fill="#cad5e2" xml:space="preserve">""" +
                """<text x="0" y="16" textLength="20">ab</text>""" +
                """<text x="20" y="16" textLength="10" fill="#ff0000">c</text>""" +
                """<text x="0" y="36" textLength="30">&lt;&amp;&gt;</text>""" +
                "</g></svg>"
        assertEquals(expected, svg)
    }

    @Test
    fun blitCopiesColorsAndGlyphs() {
        val target = Grid(5, 3)
        target.blit(sample(), 1, 1)
        assertEquals('a', target.glyphAt(1, 1))
        assertEquals(Rgb.fromHex("#ff0000"), target.colorAt(3, 1))
        assertEquals('&', target.glyphAt(2, 2))
    }

    @Test
    fun clearResetsEverything() {
        val g = sample()
        g.clear()
        assertEquals("    \n    ", g.toText())
        assertEquals(null, g.colorAt(2, 0))
    }

    @Test
    fun controlCharactersNeverReachAnsiOrSvgOutput() {
        val g = Grid(4, 1)
        g.write(0, 0, "\u001b]0\u0007")
        assertEquals(false, Export.toAnsi(g).contains('\u001b'))
        assertEquals(false, Export.toSvg(g).contains('\u0007'))
    }

    @Test
    fun negativeSpeedWrapsIntoTheLoop() {
        val t = PieceClock.next(0f, 1f, -1f, reduced = false, still = 0f, loop = 10f)
        assertEquals(9f, t)
    }

    @Test
    fun badInputsAreRejected() {
        assertFailsWith<IllegalArgumentException> { Noise.fbm(0f, 0f, octaves = 0) }
        assertFailsWith<IllegalArgumentException> { Fields.fromTones(FloatArray(0), 0, 0) }
        assertFailsWith<IllegalArgumentException> { Rgb.fromHex("#-00001") }
    }
}
