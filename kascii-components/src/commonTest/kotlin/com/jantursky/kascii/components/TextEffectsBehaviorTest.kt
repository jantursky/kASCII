package com.jantursky.kascii.components

import com.jantursky.kascii.core.KasciiTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TextEffectsBehaviorTest {
    private val theme = KasciiTheme.Dark

    @Test
    fun reducedMotionShowsTheFinalText() {
        assertEquals("Hello world", renderTypewriter("Hello world", 0f, 10f, true).toText().trim())
        assertEquals("Hello world", renderScramble("Hello world", 0f, 1.5f, 1L, true, theme).toText())
        assertEquals("Hello world", renderDissolve("Hello world", 0f, 1.5f, 1L, true).toText())
        assertEquals("GATE 7", renderSplitFlap("Gate 7", 0f, 1L, true).toText())
        assertEquals(" Hello world ", renderGlitch("Hello world", 0.3f, 1f, 1L, true, theme).toText())
        assertEquals("... --- ... / --. ---", renderMorse("SOS go", 0f, 6f, true, theme).toText())
        assertEquals("Hello", renderMarquee("Hello", 5, 0f, 8f).toText())
    }

    @Test
    fun typewriterCursorBlinksOnceTheTextIsDone() {
        assertEquals("Hello▌", renderTypewriter("Hello", 3f, 10f, false).toText())
        assertEquals("Hello ", renderTypewriter("Hello", 3.5f, 10f, false).toText())
        assertEquals("▌", renderTypewriter("Hello", 0f, 10f, false).toText().trimEnd())
    }

    @Test
    fun scrambleDecodesLeftToRightAndKeepsSpaces() {
        val mid = renderScramble("ab cd", 0.75f, 1.5f, 1L, false, theme)
        assertEquals("ab", mid.toText().take(2))
        assertEquals(' ', mid.glyphAt(2, 0))
        assertNull(mid.colorAt(0, 0))
        assertEquals(theme.dim(), mid.colorAt(3, 0))
        assertEquals("ab cd", renderScramble("ab cd", 1.5f, 1.5f, 1L, false, theme).toText())
    }

    @Test
    fun scrambleFramesDependOnTheSeedAndTheFrame() {
        fun frame(
            seed: Long,
            t: Float,
        ) = renderScramble("abcdefghijkl", t, 100f, seed, false, theme).toText()
        assertEquals(frame(1L, 0.5f), frame(1L, 0.5f))
        assertNotEquals(frame(1L, 0.5f), frame(2L, 0.5f))
        assertNotEquals(frame(1L, 0.5f), frame(1L, 0.6f))
    }

    @Test
    fun dissolveLeavesSpacesAlone() {
        assertEquals(' ', renderDissolve("a b", 3f, 6f, 1L, false).glyphAt(1, 0))
        assertEquals("a b", renderDissolve("a b", 6f, 6f, 1L, false).toText())
    }

    @Test
    fun splitFlapKeepsCharactersTheBoardLacks() {
        assertEquals("a~", renderSplitFlap("a~", 0f, 1L, true).toText().lowercase())
        assertEquals('~', renderSplitFlap("~", 0f, 1L, false).glyphAt(0, 0))
        assertEquals("GATE 7", renderSplitFlap("gate 7", 9f, 5L, false).toText())
    }

    @Test
    fun splitFlapChangesWhileFlipping() {
        val early = renderSplitFlap("Gate 7", 0f, 1L, false).toText()
        assertNotEquals(early, renderSplitFlap("Gate 7", 0.5f, 1L, false).toText())
    }

    @Test
    fun glitchBurstsAreRepeatableAndKeepTheLength() {
        fun frames() = (0..60).map { renderGlitch("Hello world", it / 24f, 0.3f, 4L, false, theme) }
        val first = frames()
        assertEquals(first.map { it.toText() }, frames().map { it.toText() })
        assertTrue(first.any { it.toText().trim() != "Hello world" }, "some frame glitches")
        assertTrue(first.any { it.toText() == " Hello world " }, "some frame is clean")
        assertTrue(first.all { it.cols == 13 })
        assertTrue(first.any { grid -> (0 until grid.cols).any { grid.colorAt(it, 0) == theme.bad() } })
    }

    @Test
    fun marqueeLoopsWithAGap() {
        assertEquals("Hello    H", renderMarquee("Hello", 10, 0f, 1f).toText())
        assertEquals("ello    He", renderMarquee("Hello", 10, 1f, 1f).toText())
        assertEquals("Hello    H", renderMarquee("Hello", 10, 9f, 1f).toText())
        assertEquals("ab", renderMarquee("abcdefghij", 2, 0f, 1f).toText())
    }

    @Test
    fun waveIsFlatWithoutTime() {
        assertEquals(listOf("", "Hello", "").map { it.padEnd(5) }, renderWave("Hello", null, 1).toText().lines())
        assertEquals(5, renderWave("Hi", 0.4f, 2).rows)
    }

    @Test
    fun rainbowDriftsAndVariesAlongTheLine() {
        val a = renderRainbow("abcdef", 0f, theme)
        assertNotEquals(a.colorAt(0, 0), a.colorAt(3, 0))
        assertNotEquals(a.colorAt(0, 0), renderRainbow("abcdef", 1f, theme).colorAt(0, 0))
        assertEquals("abcdef", a.toText())
        assertEquals(a.colorAt(2, 0), renderRainbow("abcdef", 0f, theme).colorAt(2, 0))
    }

    @Test
    fun shimmerBrightensNearTheSweepOnly() {
        val grid = renderShimmer("abcdefghij", 0.5f, false, theme)
        val colors = (0 until 10).map { grid.colorAt(it, 0) }
        assertEquals(theme.ink, colors.maxByOrNull { it!!.red })
        assertEquals(theme.dim(), colors.last())
        assertTrue(renderShimmer("abc", 0.5f, true, theme).let { g -> (0..2).all { g.colorAt(it, 0) == null } })
    }

    @Test
    fun morseTableCoversTextAndSkipsWhatItLacks() {
        assertEquals("... --- ... / --. ---", morseOf("SOS go"))
        assertEquals(".- -...", morseOf("a~b"))
        assertEquals("", morseOf("~~"))
        assertEquals(".---- ----- ..---", morseOf("102"))
        assertEquals(".-.-.- --..-- ..--.. -.-.-- -....- -..-.", morseOf(".,?!-/"))
    }

    @Test
    fun morseShowsPendingSymbolsAsDots() {
        val grid = renderMorse("SO", 0.5f, 2f, false, theme)
        assertEquals(".·· ···", grid.toText())
        assertEquals(theme.accent, grid.colorAt(0, 0))
        assertEquals(theme.dim(), grid.colorAt(1, 0))
    }
}
