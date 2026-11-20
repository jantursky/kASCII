package com.jantursky.kascii.components

import androidx.compose.ui.input.key.Key
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.Rgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HelpersTest {
    @Test
    fun wrapsAtWordBoundariesAndHonorsNewlines() {
        assertEquals(listOf("the quick", "brown fox", "", "end"), wrapText("the quick brown fox\n\nend", 9))
    }

    @Test
    fun breaksWordsLongerThanTheWidth() {
        assertEquals(listOf("abcd", "efgh", "ij"), wrapText("abcdefghij", 4))
        assertEquals(listOf("ab", "abcd", "ef"), wrapText("ab abcdef", 4))
    }

    @Test
    fun wrapNeedsAPositiveWidth() {
        assertFailsWith<IllegalArgumentException> { wrapText("x", 0) }
    }

    @Test
    fun centersWithTheExtraCellOnTheRight() {
        assertEquals(" ab  ", "ab".centered(5))
        assertEquals("abcdef", "abcdef".centered(3))
    }

    @Test
    fun prettyDropsAWholeFraction() {
        assertEquals("3", 3f.pretty())
        assertEquals("3.5", 3.46f.pretty())
        assertEquals("-2.5", (-2.5f).pretty())
        assertEquals("0.1", 0.1f.pretty())
    }

    @Test
    fun percentIsClamped() {
        assertEquals(0, percentOf(-1f))
        assertEquals(46, percentOf(0.456f))
        assertEquals(100, percentOf(3f))
    }

    @Test
    fun gridOfLinesIsAsWideAsTheLongestLine() {
        val grid = gridOfLines(listOf("ab", "abcd"))
        assertEquals("ab  \nabcd", grid.toText())
        assertEquals(0, gridOfLines(emptyList()).cols)
    }

    @Test
    fun clockQuantizesToFrames() {
        assertEquals(0f, quantizedSeconds(0, 10))
        assertEquals(0.1f, quantizedSeconds(190_000_000, 10))
        assertEquals(2f, quantizedSeconds(2_050_000_000, 10))
    }

    @Test
    fun paletteColorLoops() {
        val a = Rgb(0, 0, 0)
        val b = Rgb(200, 100, 50)
        assertEquals(a, paletteColor(listOf(a, b), 0f))
        assertEquals(a, paletteColor(listOf(a, b), 1f))
        assertEquals(b, paletteColor(listOf(a, b), 0.5f))
        assertEquals(Rgb(100, 50, 25), paletteColor(listOf(a, b), 0.25f))
        assertEquals(a, paletteColor(listOf(a), 0.7f))
    }

    @Test
    fun toneColorsComeFromThePalette() {
        val dark = KasciiTheme.Dark
        assertNull(AsciiTone.Neutral.color(dark))
        assertEquals(dark.accent, AsciiTone.Info.color(dark))
        assertEquals(dark.palette[2], AsciiTone.Success.color(dark))
        assertEquals(dark.palette[3], AsciiTone.Warning.color(dark))
        assertEquals(dark.palette[1], AsciiTone.Error.color(dark))
        assertEquals(setOf('*', 'i', '+', '!', 'x'), AsciiTone.entries.map { it.marker() }.toSet())
    }

    @Test
    fun singleColorThemesWrapAround() {
        val mono = KasciiTheme.Phosphor
        assertEquals(mono.ink, mono.good())
        assertEquals(mono.ink, mono.pick(5))
        assertEquals(mono.ink, mono.pick(-1))
    }

    @Test
    fun dimTextKeepsReadableContrastOnBothDarkAndLight() {
        for (theme in listOf(KasciiTheme.Dark, KasciiTheme.Light)) {
            val dim = theme.dim()
            val check = KasciiTheme(dim, theme.paper)
            assertTrue(check.contrastRatio() >= 4.5f, "dim text contrast ${check.contrastRatio()}")
        }
    }

    @Test
    fun inverseSwapsInkAndPaper() {
        val inverse = KasciiTheme.Dark.inverse()
        assertEquals(KasciiTheme.Dark.paper, inverse.ink)
        assertEquals(KasciiTheme.Dark.ink, inverse.paper)
    }

    @Test
    fun sliderMath() {
        val range = 10f..20f
        assertEquals(0.5f, fractionOf(15f, range))
        assertEquals(0f, fractionOf(5f, range))
        assertEquals(0f, fractionOf(1f, 3f..3f))
        assertEquals(17.5f, valueAt(0.75f, range))
        assertEquals(0.25f, snapFraction(0.3f, 3))
        assertEquals(0.3f, snapFraction(0.3f, 0))
        assertEquals(1f, snapFraction(4f, 0))
        assertEquals(2, sliderTrack(1))
        assertEquals(21, sliderTrack(24))
        assertEquals(0f, fractionAtCell(0f, 10))
        assertEquals(1f, fractionAtCell(99f, 10))
        assertEquals(0.5f, fractionAtCell(2.5f + 4.5f, 10))
    }

    @Test
    fun sliderKeys() {
        assertEquals(0.55f, keyTarget(Key.DirectionRight, 0.5f, 0)!!, 1e-6f)
        assertEquals(0.75f, keyTarget(Key.DirectionUp, 0.5f, 3)!!, 1e-6f)
        assertEquals(0.45f, keyTarget(Key.DirectionLeft, 0.5f, 0)!!, 1e-6f)
        assertEquals(0.25f, keyTarget(Key.DirectionDown, 0.5f, 3)!!, 1e-6f)
        assertEquals(0f, keyTarget(Key.MoveHome, 0.5f, 0))
        assertEquals(1f, keyTarget(Key.MoveEnd, 0.5f, 0))
        assertNull(keyTarget(Key.A, 0.5f, 0))
    }

    @Test
    fun fieldBracketsChangeWithFocus() {
        assertEquals('[' to ']', fieldBrackets(false))
        assertEquals('▌' to '▐', fieldBrackets(true))
    }
}
