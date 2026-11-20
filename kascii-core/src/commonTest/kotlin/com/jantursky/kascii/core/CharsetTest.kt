package com.jantursky.kascii.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CharsetTest {
    @Test
    fun rampEndsPickFirstAndLastGlyph() {
        assertEquals(' ', Charsets.Standard.glyphFor(0f))
        assertEquals('@', Charsets.Standard.glyphFor(1f))
    }

    @Test
    fun rampClampsOutOfRangeTones() {
        assertEquals(' ', Charsets.Blocks.glyphFor(-3f))
        assertEquals('█', Charsets.Blocks.glyphFor(9f))
    }

    @Test
    fun rampSplitsTonesEvenly() {
        assertEquals("░", Charsets.Blocks.glyphFor(0.25f).toString())
        assertEquals("▒", Charsets.Blocks.glyphFor(0.45f).toString())
        assertEquals("▓", Charsets.Blocks.glyphFor(0.65f).toString())
    }

    @Test
    fun emptyRampIsRejected() {
        assertFailsWith<IllegalArgumentException> { Charsets.custom("") }
    }

    @Test
    fun byCoverageSortsLightToDark() {
        val order = mapOf('a' to 0.9f, 'b' to 0.1f, 'c' to 0.5f)
        val ramp = Charset.Ramp.byCoverage("t", "abc") { order.getValue(it) }
        assertEquals("bca", ramp.glyphs)
    }

    @Test
    fun brailleSubCellMapping() {
        val b = Charsets.Braille
        assertEquals(8, b.subX * b.subY)
        assertEquals('⠀', b.glyphFor(FloatArray(8)))
        assertEquals('⣿', b.glyphFor(FloatArray(8) { 1f }))
        // Samples are row major, 2 wide: top-left dot is bit 0x01, bottom-right is 0x80.
        assertEquals('⠁', b.glyphFor(FloatArray(8).also { it[0] = 1f }))
        assertEquals('⢀', b.glyphFor(FloatArray(8).also { it[7] = 1f }))
        assertEquals('⠈', b.glyphFor(FloatArray(8).also { it[1] = 1f }))
        assertEquals('⡀', b.glyphFor(FloatArray(8).also { it[6] = 1f }))
    }

    @Test
    fun quadrantMapping() {
        val q = Charsets.Quadrants
        assertEquals(' ', q.glyphFor(FloatArray(4)))
        assertEquals('█', q.glyphFor(FloatArray(4) { 1f }))
        assertEquals('▀', q.glyphFor(floatArrayOf(1f, 1f, 0f, 0f)))
        assertEquals('▄', q.glyphFor(floatArrayOf(0f, 0f, 1f, 1f)))
        assertEquals('▌', q.glyphFor(floatArrayOf(1f, 0f, 1f, 0f)))
        assertEquals('▚', q.glyphFor(floatArrayOf(1f, 0f, 0f, 1f)))
    }

    @Test
    fun thresholdMovesTheCutoff() {
        val samples = FloatArray(8) { 0.4f }
        assertEquals('⠀', Charsets.Braille.glyphFor(samples))
        assertEquals('⣿', Charsets.Braille.withThreshold(0.3f).glyphFor(samples))
    }

    @Test
    fun everyBuiltInHasAUniqueName() {
        assertEquals(
            Charsets.All.size,
            Charsets.All
                .map { it.name }
                .toSet()
                .size,
        )
    }
}
