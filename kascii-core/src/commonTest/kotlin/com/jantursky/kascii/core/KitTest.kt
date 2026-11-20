package com.jantursky.kascii.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KitTest {
    @Test
    fun rngIsDeterministicPerSeed() {
        val a = Rng(42)
        val b = Rng(42)
        repeat(20) { assertEquals(a.float(), b.float()) }
        assertTrue(Rng(1).float() != Rng(2).float())
    }

    @Test
    fun rngStaysInRange() {
        val r = Rng(3)
        repeat(500) {
            assertTrue(r.float() in 0f..1f)
            assertTrue(r.int(7) in 0..6)
            assertTrue(r.float(2f, 3f) in 2f..3f)
        }
    }

    @Test
    fun noiseStaysInRangeAndIsDeterministic() {
        for (i in 0 until 200) {
            val v = Noise.fbm(i * 0.37f, i * 0.11f, i * 0.05f, 4, 9)
            assertTrue(v in 0f..1f)
            assertEquals(v, Noise.fbm(i * 0.37f, i * 0.11f, i * 0.05f, 4, 9))
        }
    }

    @Test
    fun rgbHexRoundTrip() {
        assertEquals("#1d293d", Rgb.fromHex("#1d293d").toHex())
        assertEquals(Rgb.fromHex("#aabbcc"), Rgb.fromHex("#abc"))
        assertEquals(0x80, Rgb.fromHex("#80ff0000").alpha)
    }

    @Test
    fun themeContrastIsAboveAA() {
        assertTrue(KasciiTheme.Dark.contrastRatio() >= 4.5f)
        assertTrue(KasciiTheme.Light.contrastRatio() >= 4.5f)
    }

    @Test
    fun lineDrawsBothEndpoints() {
        val g = Grid(6, 4)
        g.line(0, 0, 5, 3)
        assertEquals('#', g.glyphAt(0, 0))
        assertEquals('#', g.glyphAt(5, 3))
    }

    @Test
    fun rectDrawsFrame() {
        val g = Grid(4, 3)
        g.rect(0, 0, 4, 3, BoxStyle.Single)
        assertEquals("┌──┐\n│  │\n└──┘", g.toText())
    }

    @Test
    fun brailleCanvasPlotsDots() {
        val c = BrailleCanvas(2, 1)
        c.plot(0, 0)
        c.plot(3, 3)
        val g = Grid(2, 1)
        c.drawTo(g, 0, 0)
        assertEquals("⠁⢀", g.toText())
    }

    @Test
    fun pieceRendersAtItsStillTime() {
        val piece = asciiPiece("t", "test", 3, 1, still = 2f) { t, grid -> grid.write(0, 0, t.toInt().toString()) }
        assertEquals("2  ", piece.render().toText())
    }

    @Test
    fun clockHoldsStillWhenReduced() {
        assertEquals(1.5f, PieceClock.next(9f, 1f, 1f, reduced = true, still = 1.5f))
        assertEquals(3.0f, PieceClock.next(1f, 1f, 2f, reduced = false, still = 0f))
        assertEquals(0.5f, PieceClock.next(1.75f, 0.75f, 1f, reduced = false, still = 0f, loop = 2f))
    }

    @Test
    fun motionResolvesAgainstPlatform() {
        assertTrue(PieceClock.isReduced(Motion.System, true))
        assertTrue(!PieceClock.isReduced(Motion.System, false))
        assertTrue(!PieceClock.isReduced(Motion.Full, true))
        assertTrue(PieceClock.isReduced(Motion.Reduced, false))
    }
}
