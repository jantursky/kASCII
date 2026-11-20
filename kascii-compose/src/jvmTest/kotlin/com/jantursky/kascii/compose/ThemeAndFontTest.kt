package com.jantursky.kascii.compose

import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.text.font.FontFamily
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.Rgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ThemeAndFontTest {
    @Test
    fun theThemeIsProvidedAndTheSystemThemeIsABuiltIn() =
        runAsciiTest {
            lateinit var provided: KasciiTheme
            lateinit var system: KasciiTheme
            setContent {
                system = systemKasciiTheme()
                ProvideKasciiTheme(KasciiTheme.Amber) { provided = LocalKasciiTheme.current }
            }
            assertSame(KasciiTheme.Amber, provided)
            assertTrue(system === KasciiTheme.Dark || system === KasciiTheme.Light)
        }

    @Test
    fun theBundledFontLoadsOnDesktop() =
        runAsciiTest {
            lateinit var family: FontFamily
            setContent { family = kasciiFontFamily() }
            assertNotEquals(FontFamily.Monospace, family)
        }

    @Test
    fun aGridBecomesABitmapOfWholeCells() {
        runAsciiTest { setContent { kasciiFontFamily() } }
        val green = Rgb(0, 255, 0)
        val grid = Grid(3, 2).apply { put(0, 0, '█', green) }
        val image = grid.toImageBitmap(KasciiTheme.Dark, cellHeightPx = 33)
        // A 33 px line is a 25 px font, which advances 15 px.
        assertEquals(45, image.width)
        assertEquals(66, image.height)
        val pixels = image.toPixelMap()
        assertEquals(green.toColor(), pixels[7, 16])
        assertEquals(KasciiTheme.Dark.paper.toColor(), pixels[22, 16])
        assertEquals(KasciiTheme.Dark.paper.toColor(), pixels[7, 50])
    }

    @Test
    fun anEmptyGridHasNoBitmap() {
        assertFailsWith<IllegalArgumentException> { Grid(0, 3).toImageBitmap() }
        assertFailsWith<IllegalArgumentException> { Grid(1, 1).toImageBitmap(cellHeightPx = 0) }
    }
}
