package com.jantursky.kascii.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.core.Charsets
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AsciiModifierTest {
    // Two 10 x 22 px cells.
    private val box = Modifier.size(20.dp, 22.dp)

    private fun ComposeUiTest.awaitPixels(
        color: Color,
        count: Int,
    ) = waitUntil(timeoutMillis = 5_000) {
        mainClock.advanceTimeByFrame()
        capture().countOf(color, 0, 0, 20, 22) == count
    }

    @Test
    fun brightContentBecomesFullBlocksInInk() =
        runAsciiTest {
            setAsciiContent {
                Box(box.ascii(Charsets.Blocks, cell = 10.dp, renderer = AsciiRenderer.Cpu)) {
                    Box(Modifier.fillMaxSize().background(Color.White))
                }
            }
            awaitPixels(Ink, 440)
            assertEquals(0, capture().countOf(Color.White))
        }

    @Test
    fun sampledColorsTintGlyphsLikeTheContent() =
        runAsciiTest {
            setAsciiContent {
                Box(box.ascii(Charsets.Blocks, cell = 10.dp, colorMode = SampledColors)) {
                    // Bright enough to be a full block on the dark paper.
                    Box(Modifier.fillMaxSize().background(Color.Yellow))
                }
            }
            awaitPixels(Color.Yellow, 440)
        }

    @Test
    fun aChangeInTheContentIsSampledAgain() =
        runAsciiTest {
            var color by mutableStateOf(Color.White)
            setAsciiContent {
                Box(box.ascii(Charsets.Blocks, cell = 10.dp)) {
                    Box(Modifier.fillMaxSize().background(color))
                }
            }
            awaitPixels(Ink, 440)
            color = Color.Transparent
            awaitPixels(Paper, 440)
        }

    @Test
    fun liveSamplingFollowsContentThatChangesInsideItsOwnLayer() =
        runAsciiTest {
            var level by mutableFloatStateOf(1f)
            var live by mutableStateOf(true)
            var cell by mutableStateOf(10.dp)
            setAsciiContent {
                Box(box.ascii(Charsets.Blocks, cell = cell, live = live)) {
                    Box(Modifier.fillMaxSize().graphicsLayer { alpha = level }.background(Color.White))
                }
            }
            awaitPixels(Ink, 440)
            level = 0f
            awaitPixels(Paper, 440)
            cell = 11.dp
            waitForIdle()
            live = false
            level = 1f
            waitForIdle()
        }

    @Test
    fun theTextRendererDrawsLikeTheAtlasPainterOnAModifier() =
        runAsciiTest {
            setAsciiContent {
                Box(box.ascii(Charsets.Blocks, cell = 10.dp, renderer = AsciiRenderer.Text)) {
                    Box(Modifier.fillMaxSize().background(Color.White))
                }
            }
            awaitPixels(Ink, 440)
        }

    @Test
    fun theShaderDrawsTheModifierAndLeavingReleasesTheLayer() =
        runAsciiTest {
            var shown by mutableStateOf(true)
            setAsciiContent {
                val modifier = if (shown) box.ascii(Charsets.Blocks, 10.dp, renderer = AsciiRenderer.Gpu) else box
                Box(modifier) { Box(Modifier.fillMaxSize().background(Color.White)) }
            }
            awaitPixels(Ink, 440)
            shown = false
            awaitPixels(Color.White, 440)
        }
}
