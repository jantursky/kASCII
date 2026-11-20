package com.jantursky.kascii.gallery

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class GliderGunPeriodTest {
    @Test
    fun theBoardRepeatsAfterOneThirtyGenerationPeriod() {
        val cycle = gliderGunCycle()
        var board = cycle[0]
        repeat(cycle.size) { board = board.nextGeneration() }
        assertEquals(30, cycle.size)
        assertContentEquals(cycle[0], board)
    }
}
