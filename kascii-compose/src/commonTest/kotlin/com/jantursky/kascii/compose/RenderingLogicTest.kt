package com.jantursky.kascii.compose

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.IntSize
import com.jantursky.kascii.compose.internal.AUTO_SHADER_CELLS
import com.jantursky.kascii.compose.internal.cellColor
import com.jantursky.kascii.compose.internal.gridAnnotatedString
import com.jantursky.kascii.compose.internal.visibleIn
import com.jantursky.kascii.compose.internal.wantsShader
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.Rgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RenderingLogicTest {
    @Test
    fun theTextOfAGridIsTheCoreText() {
        val grid =
            Grid(3, 2).apply {
                write(0, 0, "ab")
                write(0, 1, "cde")
            }
        assertEquals(grid.toText(), gridAnnotatedString(grid).text)
    }

    @Test
    fun coloredRunsBecomeSpansAndInkCellsDoNot() {
        val red = Rgb(255, 0, 0)
        val grid =
            Grid(4, 2).apply {
                write(0, 0, "aaab")
                write(1, 0, "aa", red)
                write(0, 1, "cc", red)
            }
        val spans = gridAnnotatedString(grid).spanStyles
        assertEquals(2, spans.size)
        assertEquals(SpanStyle(color = Color(red.argb)), spans[0].item)
        assertEquals(1 to 3, spans[0].start to spans[0].end)
        // The second row starts after four glyphs and a newline.
        assertEquals(5 to 7, spans[1].start to spans[1].end)
    }

    @Test
    fun cellsWithoutAColorTakeTheInk() {
        assertEquals(7, cellColor(0, 7))
        assertEquals(5, cellColor(5, 7))
    }

    @Test
    fun autoUsesTheShaderOnlyForLargeGrids() {
        assertFalse(wantsShader(AsciiRenderer.Auto, AUTO_SHADER_CELLS))
        assertTrue(wantsShader(AsciiRenderer.Auto, AUTO_SHADER_CELLS + 1))
        assertTrue(wantsShader(AsciiRenderer.Gpu, 1))
        assertFalse(wantsShader(AsciiRenderer.Cpu, 1_000_000))
        assertFalse(wantsShader(AsciiRenderer.Text, 1_000_000))
    }

    @Test
    fun anElementIsOnScreenWhenAnyOfItOverlapsTheWindow() {
        val window = IntSize(100, 100)
        assertTrue(visibleIn(Rect(90f, 90f, 120f, 120f), window))
        assertFalse(visibleIn(Rect(100f, 0f, 150f, 50f), window))
        assertFalse(visibleIn(Rect(-50f, 0f, 0f, 50f), window))
        assertFalse(visibleIn(Rect(10f, 10f, 10f, 50f), window))
    }

    @Test
    fun reducedMotionFollowsThePlatformOnlyUnderSystem() {
        assertTrue(resolveReducedMotion(Motion.System, 1f, true))
        assertTrue(resolveReducedMotion(Motion.System, 0f, false))
        assertFalse(resolveReducedMotion(Motion.System, 1f, false))
        assertFalse(resolveReducedMotion(Motion.Full, 0f, true))
        assertTrue(resolveReducedMotion(Motion.Reduced, 1f, false))
    }

    @Test
    fun composeColorsRoundTripThroughCoreColors() {
        val theme = kasciiThemeOf(Color(0xFF112233), Color(0x80FFFFFF.toInt()))
        assertEquals(Rgb.fromHex("#112233"), theme.ink)
        assertEquals(0x80FFFFFF.toInt(), theme.paper.argb)
        assertEquals(theme.ink, theme.accent)
        assertEquals(listOf(theme.ink), theme.palette)
        assertEquals(Color(0xFF112233), theme.ink.toColor())
        assertEquals(
            KasciiTheme.Dark.ink,
            KasciiTheme.Dark.ink
                .toColor()
                .toRgb(),
        )
    }
}
