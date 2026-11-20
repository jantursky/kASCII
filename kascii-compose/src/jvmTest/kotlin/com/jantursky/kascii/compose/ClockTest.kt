package com.jantursky.kascii.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.core.Motion
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ClockTest {
    // Ten 10 x 22 px cells in a row.
    private val row = Modifier.size(100.dp, 22.dp)

    @Test
    fun aPieceAdvancesWithTheFrameClock() =
        runAsciiTest {
            setAsciiContent { AsciiPieceView(stopwatch(), row, motion = Motion.Full) }
            assertEquals(0, capture().firstInkColumn(10))
            mainClock.advanceTimeBy(2_500)
            assertEquals(2, capture().firstInkColumn(10))
        }

    @Test
    fun speedScalesTime() =
        runAsciiTest {
            setAsciiContent { AsciiPieceView(stopwatch(), row, speed = 2f, motion = Motion.Full) }
            mainClock.advanceTimeBy(2_600)
            assertEquals(5, capture().firstInkColumn(10))
        }

    @Test
    fun changingSpeedKeepsTheClockRunning() =
        runAsciiTest {
            var speed by mutableStateOf(1f)
            setAsciiContent { AsciiPieceView(stopwatch(), row, speed = speed, motion = Motion.Full) }
            mainClock.advanceTimeBy(2_500)
            assertEquals(2, capture().firstInkColumn(10))
            // A new speed every 100 ms must not restart the clock, which would freeze time.
            for (i in 1..30) {
                speed = 1f + i % 2
                mainClock.advanceTimeBy(100)
            }
            assertEquals(true, capture().firstInkColumn(10) > 2)
        }

    @Test
    fun reducedMotionHoldsTheStillFrame() =
        runAsciiTest {
            setAsciiContent { AsciiPieceView(stopwatch(still = 7f), row, motion = Motion.Reduced) }
            mainClock.advanceTimeBy(2_500)
            assertEquals(7, capture().firstInkColumn(10))
        }

    @Test
    fun theDesktopReducedMotionPropertyIsHonouredUnderSystem() =
        runAsciiTest {
            System.setProperty("kascii.reducedMotion", "true")
            try {
                setAsciiContent { AsciiPieceView(stopwatch(still = 4f), row) }
                mainClock.advanceTimeBy(2_500)
                assertEquals(4, capture().firstInkColumn(10))
            } finally {
                System.clearProperty("kascii.reducedMotion")
            }
        }

    @Test
    fun aStillPieceDoesNotRun() =
        runAsciiTest {
            setAsciiContent { AsciiPieceView(stopwatch(fps = 0, still = 3f), row, motion = Motion.Full) }
            mainClock.advanceTimeBy(2_500)
            assertEquals(3, capture().firstInkColumn(10))
        }

    @Test
    fun theClockPausesWhileTheViewIsClippedAway() =
        runAsciiTest {
            var shift by mutableStateOf(300)
            setAsciiContent {
                Box(Modifier.size(100.dp, 22.dp).clipToBounds()) {
                    AsciiPieceView(stopwatch(), Modifier.offset(shift.dp, 0.dp).then(row), motion = Motion.Full)
                }
            }
            mainClock.advanceTimeBy(3_000)
            shift = 0
            mainClock.advanceTimeBy(1_500)
            // Three seconds offscreen did not count; only the 1.5 s on screen did.
            assertEquals(1, capture().firstInkColumn(10))
        }

    @Test
    fun framesRedrawWithoutRecomposing() =
        runAsciiTest {
            var compositions = 0
            setAsciiContent {
                Counted { compositions++ }
                AsciiPieceView(stopwatch(), row, motion = Motion.Full)
            }
            mainClock.advanceTimeByFrame()
            val before = compositions
            mainClock.advanceTimeBy(2_000)
            assertEquals(before, compositions)
            assertEquals(2, capture().firstInkColumn(10))
        }

    @Test
    fun leavingTheCompositionStopsTheClock() =
        runAsciiTest {
            var shown by mutableStateOf(true)
            setAsciiContent { if (shown) AsciiPieceView(stopwatch(), row, motion = Motion.Full) }
            mainClock.advanceTimeBy(200)
            shown = false
            mainClock.advanceTimeBy(200)
            // With no frame loop left asking for frames, the window goes idle.
            waitForIdle()
        }

    @Test
    fun theTextRendererAnimatesToo() =
        runAsciiTest {
            setAsciiContent {
                AsciiPieceView(stopwatch(), row, motion = Motion.Full, renderer = AsciiRenderer.Text)
            }
            mainClock.advanceTimeBy(2_500)
            onNode(hasText("  █       ")).assertExists()
        }
}

/** Runs [onCompose] every time the caller composes. */
@NonSkippableComposable
@Composable
private fun Counted(onCompose: () -> Unit) {
    SideEffect(onCompose)
}
