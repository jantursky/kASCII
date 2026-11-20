package com.jantursky.kascii.gallery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PiecesATest {
    private val seeded =
        setOf(
            "alpine-dawn",
            "hilltop-night",
            "starfield",
            "solar-system",
            "pond-ripples",
            "bouncing-balls",
            "rain",
            "campfire",
            "fractal-tree",
            "interference-field",
        )

    @Test
    fun coversTheFirstBatchOfCategories() {
        val categories = galleryPiecesA.map { it.category }.toSet()
        assertEquals(setOf("scenes", "shapes", "space", "physics", "nature", "shaders"), categories)
    }

    @Test
    fun idsAndNamesAreUnique() {
        assertEquals(galleryPiecesA.size, galleryPiecesA.map { it.id }.toSet().size)
        assertEquals(galleryPiecesA.size, galleryPiecesA.map { it.create(0).meta.name }.toSet().size)
    }

    @Test
    fun stillFrameIsDeterministicAndNotBlank() {
        for (entry in galleryPiecesA) {
            val first = entry.create(0).render().toText()
            assertEquals(first, entry.create(0).render().toText(), "${entry.id} is not deterministic")
            assertTrue(first.any { it != ' ' && it != '\n' }, "${entry.id} still frame is blank")
        }
    }

    @Test
    fun metaMatchesTheGrid() {
        for (entry in galleryPiecesA) {
            val piece = entry.create(0)
            val grid = piece.render()
            assertEquals(piece.meta.cols, grid.cols, "${entry.id} cols")
            assertEquals(piece.meta.rows, grid.rows, "${entry.id} rows")
            assertEquals(entry.category, piece.meta.category, "${entry.id} category")
        }
    }

    @Test
    fun seedsChangeSeededPieces() {
        for (entry in galleryPiecesA.filter { it.id in seeded }) {
            assertNotEquals(
                entry.create(0).render().toText(),
                entry.create(1).render().toText(),
                "${entry.id} ignores its seed",
            )
        }
    }

    @Test
    fun loopingPiecesRepeatAfterTheirLoop() {
        for (entry in galleryPiecesA) {
            val piece = entry.create(0)
            if (piece.meta.loop <= 0f) continue
            assertEquals(
                piece.render(LOOP_PROBE).toText(),
                piece.render(LOOP_PROBE + piece.meta.loop).toText(),
                "${entry.id} does not close its loop",
            )
        }
    }

    @Test
    fun framesChangeOverTime() {
        for (entry in galleryPiecesA) {
            val piece = entry.create(0)
            assertNotEquals(piece.render(0.3f).toText(), piece.render(1.9f).toText(), "${entry.id} is frozen")
        }
    }

    private companion object {
        const val LOOP_PROBE = 0.7f
    }
}
