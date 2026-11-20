package com.jantursky.kascii.compose.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class ClockAdvanceTest {
    private val spec = ClockSpec(running = true, speed = 2f)

    @Test
    fun timeAdvancesByTheDeltaTimesSpeed() {
        assertEquals(1.0, advance(0.0, 0.5, spec))
    }

    @Test
    fun aNegativeDeltaIsIgnored() {
        assertEquals(3.0, advance(3.0, -1.0, spec))
    }

    @Test
    fun aLoopWrapsTime() {
        assertEquals(1.0, advance(9.0, 2.0, spec.copy(loop = 10f, speed = 1f)))
    }

    @Test
    fun aNegativeSpeedWrapsIntoTheLoop() {
        assertEquals(9.0, advance(0.0, 1.0, spec.copy(speed = -1f, loop = 10f)))
    }

    @Test
    fun smallStepsStillAddUpAfterDays() {
        val week = 7.0 * 24 * 3600
        assertEquals(week + 1.0 / 60, advance(week, 1.0 / 60, ClockSpec(running = true)), 1e-9)
    }
}
