package com.jantursky.kascii.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FieldsTest {
    private val fields: Map<String, Field> =
        mapOf(
            "plasma" to Fields.plasma(),
            "noise" to Fields.noise(seed = 3),
            "waves" to Fields.waves(),
            "ripples" to Fields.ripples(),
            "gradient" to Fields.gradient(0.7f),
            "radial" to Fields.radial(),
            "solid" to Fields.solid(0.4f),
        )

    @Test
    fun everyFieldStaysInRangeAndIsDeterministic() {
        for ((name, field) in fields) {
            for (i in 0 until 60) {
                val x = (i % 10) / 10f
                val y = (i / 10) / 6f
                val v = field.tone(x, y, i * 0.37f, 2f)
                assertTrue(v in 0f..1f, "$name gave $v")
                assertEquals(v, field.tone(x, y, i * 0.37f, 2f), name)
            }
        }
    }

    @Test
    fun timeChangesAnimatedFields() {
        for (name in listOf("plasma", "noise", "waves", "ripples")) {
            val field = fields.getValue(name)
            val a = field.tone(0.3f, 0.6f, 0f, 2f)
            val b = field.tone(0.3f, 0.6f, 1.7f, 2f)
            assertTrue(a != b, "$name did not change over time")
        }
    }

    @Test
    fun gradientRunsFromZeroToOne() {
        val g = Fields.gradient()
        assertEquals(0f, g.tone(0f, 0.5f, 0f, 1f))
        assertEquals(1f, g.tone(1f, 0.5f, 0f, 1f))
    }

    @Test
    fun radialIsBrightestAtTheCentre() {
        val r = Fields.radial(radius = 0.4f)
        assertEquals(1f, r.tone(0.5f, 0.5f, 0f, 1f))
        assertEquals(0f, r.tone(0f, 0f, 0f, 1f))
    }

    @Test
    fun solidIsConstant() {
        assertEquals(0.4f, Fields.solid(0.4f).tone(0.9f, 0.1f, 5f, 3f))
    }

    @Test
    fun fromTonesReadsTheGrid() {
        val f = Fields.fromTones(floatArrayOf(0f, 1f, 0.5f, 0.25f), 2, 2)
        assertEquals(0f, f.tone(0.1f, 0.1f, 0f, 1f))
        assertEquals(1f, f.tone(0.9f, 0.1f, 0f, 1f))
        assertEquals(0.5f, f.tone(0.1f, 0.9f, 0f, 1f))
        assertEquals(0.25f, f.tone(1f, 1f, 0f, 1f))
    }

    @Test
    fun fromTonesRejectsWrongSize() {
        assertFailsWith<IllegalArgumentException> { Fields.fromTones(FloatArray(3), 2, 2) }
    }

    @Test
    fun noiseSeedChangesTheField() {
        val a = Fields.noise(seed = 1).tone(0.4f, 0.4f, 0f, 1f)
        val b = Fields.noise(seed = 2).tone(0.4f, 0.4f, 0f, 1f)
        assertTrue(a != b)
    }
}
