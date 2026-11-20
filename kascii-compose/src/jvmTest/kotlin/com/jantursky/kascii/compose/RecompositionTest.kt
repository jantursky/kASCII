package com.jantursky.kascii.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.core.Charsets
import com.jantursky.kascii.core.ColorMode
import com.jantursky.kascii.core.Fields
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.Motion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Changing an argument redraws with it; recomposing with the same arguments changes nothing. */
@OptIn(ExperimentalTestApi::class)
class RecompositionTest {
    private val amberInk = KasciiTheme.Amber.ink.toColor()
    private val block = Grid(1, 1).apply { put(0, 0, '█') }

    @Test
    fun everyViewFollowsANewTheme() =
        runAsciiTest {
            var theme by mutableStateOf(KasciiTheme.Dark)
            var unrelated by mutableIntStateOf(0)
            val bitmap = whiteBitmap(4)
            val cell = Modifier.size(10.dp, 22.dp)
            setAsciiContent {
                // The tag reads the counter, so bumping it recomposes the views with the same arguments.
                Column(Modifier.testTag("views$unrelated")) {
                    AsciiGridView(block, cell, theme = theme)
                    AsciiPieceView(stopwatch(), Modifier.size(100.dp, 22.dp), motion = Motion.Reduced, theme = theme)
                    AsciiCanvas(Fields.solid(1f), cell, Charsets.Blocks, 10.dp, motion = Motion.Reduced, theme = theme)
                    AsciiImage(bitmap, cell, Charsets.Blocks, 10.dp, renderer = AsciiRenderer.Cpu, theme = theme)
                    AsciiGridView(block, cell, AsciiRenderer.Text, theme)
                }
            }
            waitUntil(timeoutMillis = 5_000) {
                mainClock.advanceTimeByFrame()
                capture().countOf(Ink, 0, 0, 10, 110) >= 4 * 220
            }
            unrelated++
            mainClock.advanceTimeByFrame()
            theme = KasciiTheme.Amber
            mainClock.advanceTimeByFrame()
            val image = capture()
            for (row in 0 until 4) {
                assertEquals(220, image.countOf(amberInk, 0, row * 22, 10, row * 22 + 22), "row $row")
            }
            assertTrue(image.countOf(amberInk, 0, 88, 10, 110) > 0, "the text view")
        }

    @Test
    fun aCanvasFollowsNewDrawingArguments() =
        runAsciiTest {
            var charset by mutableStateOf(Charsets.Blocks)
            var colorMode: ColorMode by mutableStateOf(ColorMode.Theme)
            var renderer by mutableStateOf(AsciiRenderer.Cpu)
            setAsciiContent {
                AsciiCanvas(
                    Fields.solid(1f),
                    Modifier.size(20.dp, 22.dp),
                    charset = charset,
                    cell = 10.dp,
                    colorMode = colorMode,
                    speed = 2f,
                    stillTime = 1f,
                    motion = Motion.Full,
                    renderer = renderer,
                    contentDescription = "Solid",
                )
            }
            assertEquals(440, capture().countOf(Ink, 0, 0, 20, 22))
            charset = Charsets.custom(" .")
            mainClock.advanceTimeByFrame()
            assertTrue(capture().countOf(Ink, 0, 0, 20, 22) in 1 until 440)
            charset = Charsets.Blocks
            colorMode = ColorMode.Mono(KasciiTheme.Amber.ink)
            renderer = AsciiRenderer.Gpu
            mainClock.advanceTimeByFrame()
            assertEquals(440, capture().countOf(amberInk, 0, 0, 20, 22))
        }

    @Test
    fun aPieceViewFollowsNewPlaybackArguments() =
        runAsciiTest {
            var speed by mutableStateOf(1f)
            var renderer by mutableStateOf(AsciiRenderer.Cpu)
            setAsciiContent {
                val size = Modifier.size(100.dp, 22.dp)
                AsciiPieceView(stopwatch(), size, speed, Motion.Full, renderer, contentDescription = "Clock")
            }
            mainClock.advanceTimeBy(1_100)
            speed = 4f
            renderer = AsciiRenderer.Gpu
            mainClock.advanceTimeBy(1_000)
            assertEquals(5, capture().firstInkColumn(10))
        }

    @Test
    fun anImageIsSampledAgainWhenItChanges() =
        runAsciiTest {
            var bitmap by mutableStateOf(whiteBitmap(2))
            setAsciiContent {
                val size = Modifier.size(10.dp, 22.dp)
                AsciiImage(bitmap, size, Charsets.Blocks, 10.dp, SampledColors, AsciiRenderer.Cpu)
            }
            awaitCount(Color.White, 220)
            bitmap = ImageBitmap(2, 2)
            awaitCount(Paper, 220)
        }

    private fun ComposeUiTest.awaitCount(
        color: Color,
        count: Int,
    ) = waitUntil(timeoutMillis = 5_000) {
        mainClock.advanceTimeByFrame()
        capture().countOf(color, 0, 0, 10, 22) == count
    }
}
