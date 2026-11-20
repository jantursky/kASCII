package com.jantursky.kascii.gallery.c

import com.jantursky.kascii.gallery.desertCaravanPiece
import com.jantursky.kascii.gallery.galleryPiecesC
import com.jantursky.kascii.gallery.steamTrainPiece
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PiecesCTest {
    @Test
    fun renderingTheStillFrameTwiceGivesTheSameText() {
        for (case in pieceCases) {
            val piece = case.create(3)
            assertEquals(piece.render().toText(), piece.render().toText(), case.name)
            assertEquals(piece.render().toText(), case.create(3).render().toText(), "${case.name} rebuilt")
        }
    }

    @Test
    fun theStillFrameIsTheFrameAtTheStillTime() {
        for (case in pieceCases) {
            val piece = case.create(3)
            assertEquals(piece.render(piece.meta.still).toText(), piece.render().toText(), case.name)
        }
    }

    @Test
    fun theStillFrameIsNotBlank() {
        for (case in pieceCases) {
            assertTrue(
                case
                    .create(3)
                    .render()
                    .toText()
                    .any { !it.isWhitespace() },
                case.name,
            )
        }
    }

    @Test
    fun metaSizeMatchesTheGridAndNamesAreFilled() {
        for (case in pieceCases) {
            val piece = case.create(0)
            val grid = piece.render()
            assertEquals(piece.meta.cols, grid.cols, case.name)
            assertEquals(piece.meta.rows, grid.rows, case.name)
            assertTrue(piece.meta.note.isNotBlank() && piece.meta.fps > 0, case.name)
            assertTrue(piece.meta.loop > 0f && piece.meta.still in 0f..piece.meta.loop, case.name)
        }
    }

    @Test
    fun differentSeedsDrawDifferentFramesWhereASeedApplies() {
        for (case in pieceCases.filter { it.seeded }) {
            val a = case.create(1).render(4f).toText()
            val b = case.create(2).render(4f).toText()
            assertNotEquals(a, b, case.name)
        }
    }

    @Test
    fun aFrameDoesNotDependOnTheOrderFramesAreRequestedIn() {
        val times = listOf(2.5f, 0.5f, 7f, 0f, 3.2f, 7f)
        for (case in pieceCases) {
            val fresh = times.associateWith { case.create(5).render(it).toText() }
            val reused = case.create(5)
            for (t in times.reversed() + times) assertEquals(fresh[t], reused.render(t).toText(), "${case.name} at $t")
        }
    }

    @Test
    fun loopingPiecesCloseTheirLoop() {
        for (case in pieceCases) {
            val piece = case.create(5)
            for (t in listOf(0f, 1.25f, piece.meta.loop / 3f)) {
                assertEquals(piece.render(t).toText(), piece.render(t + piece.meta.loop).toText(), "${case.name} at $t")
            }
        }
    }

    @Test
    fun framesChangeOverTime() {
        for (case in pieceCases) {
            val piece = case.create(0)
            assertNotEquals(piece.render(0.3f).toText(), piece.render(1.9f).toText(), case.name)
        }
    }

    @Test
    fun everyGlyphIsInTheBundledFont() {
        for (case in pieceCases) {
            val piece = case.create(2)
            for (step in 0 until FRAMES) {
                val t = piece.meta.loop * step / FRAMES
                for (ch in piece.render(t).toText()) {
                    assertTrue(
                        ch == '\n' || ch.code in ASCII || ch.code in BOXES_AND_BLOCKS,
                        "${case.name} draws '$ch'",
                    )
                }
            }
        }
    }

    @Test
    fun argumentsOutsideTheirRangeAreRejected() {
        assertFailsWith<IllegalArgumentException> { steamTrainPiece(cars = -1) }
        assertFailsWith<IllegalArgumentException> { steamTrainPiece(cars = 4) }
        assertFailsWith<IllegalArgumentException> { desertCaravanPiece(camels = 0) }
        assertFailsWith<IllegalArgumentException> { desertCaravanPiece(camels = 7) }
    }

    @Test
    fun theTrainAndTheCaravanDrawEveryAllowedLength() {
        val trains = (0..3).map { steamTrainPiece(cars = it).render().toText() }
        assertEquals(trains.size, trains.toSet().size)
        val caravans = (1..6).map { desertCaravanPiece(camels = it).render().toText() }
        assertEquals(caravans.size, caravans.toSet().size)
    }

    @Test
    fun theCatalogListsEveryPieceOnceWithMatchingMeta() {
        assertEquals(pieceCases.map { it.name }, galleryPiecesC.map { it.id })
        assertEquals(galleryPiecesC.size, galleryPiecesC.map { it.id }.toSet().size)
        for (entry in galleryPiecesC) {
            assertEquals(entry.category, entry.create(0).meta.category, entry.id)
            assertEquals("scenes", entry.category, entry.id)
            assertEquals(null, entry.credit, entry.id)
        }
    }

    private companion object {
        const val FRAMES = 48
        val ASCII = 0x20..0x7e
        val BOXES_AND_BLOCKS = 0x2500..0x259f
    }
}
