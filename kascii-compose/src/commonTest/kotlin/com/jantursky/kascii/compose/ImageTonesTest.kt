package com.jantursky.kascii.compose

import com.jantursky.kascii.core.ColorMode
import com.jantursky.kascii.core.Rgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ImageTonesTest {
    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()

    @Test
    fun tonesMeasureContrastAgainstThePaper() {
        val pixels = intArrayOf(black, white)
        val onDark = sampleTones(pixels, 2, 1, 2, 1, black)
        assertEquals(listOf(0f, 1f), onDark.tones.toList())
        val onLight = sampleTones(pixels, 2, 1, 2, 1, white)
        assertEquals(listOf(1f, 0f), onLight.tones.toList())
    }

    @Test
    fun aSampleAveragesThePixelsItCovers() {
        val pixels = intArrayOf(black, white, black, white)
        val tones = sampleTones(pixels, 2, 2, 1, 1, black)
        assertEquals(0.5f, tones.tones[0], 0.01f)
        assertEquals(Rgb(128, 128, 128).argb, tones.colors[0])
    }

    @Test
    fun transparentPixelsCountAsPaper() {
        val tones = sampleTones(intArrayOf(0x00FFFFFF), 1, 1, 1, 1, black)
        assertEquals(0f, tones.tones[0])
        assertEquals(black, tones.colors[0])
    }

    @Test
    fun aSmallImageIsSampledByNearestPixel() {
        val tones = sampleTones(intArrayOf(black, white), 2, 1, 4, 2, black)
        assertEquals(listOf(0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f), tones.tones.toList())
    }

    @Test
    fun tonesBecomeFieldsAndColorFields() {
        val red = Rgb(255, 0, 0).argb
        val tones = sampleTones(intArrayOf(black, red), 2, 1, 2, 1, black)
        assertEquals(0f, tones.toField().tone(0.1f, 0.5f, 0f, 1f))
        assertEquals(Rgb(red), tones.toColorField().color(0.9f, 0.5f, 0f))
        val mode = SampledColors.withSamples(tones)
        assertTrue(mode is ColorMode.Source && mode.field.color(0.9f, 0f, 0f) == Rgb(red))
        assertSame(ColorMode.Theme, ColorMode.Theme.withSamples(tones))
        // Outside a sampler the marker leaves cells at the theme ink.
        assertEquals(0, (SampledColors as ColorMode.Source).field.color(0f, 0f, 0f).argb)
    }

    @Test
    fun largeImagesAreSampledAtAtMost512OnTheLongerSide() {
        assertEquals(512 to 256, sampleSize(2048, 1024))
        assertEquals(30 to 20, sampleSize(30, 20))
        assertEquals(512 to 1, sampleSize(4000, 2))
    }
}
