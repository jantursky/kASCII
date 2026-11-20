package com.jantursky.kascii.gallery.b

import com.jantursky.kascii.gallery.galleryPiecesB
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PiecesBTest {
    @Test
    fun renderingTheStillFrameTwiceGivesTheSameText() {
        for (case in pieceCases) {
            val piece = case.create(3)
            assertEquals(piece.render().toText(), piece.render().toText(), case.name)
            assertEquals(piece.render().toText(), case.create(3).render().toText(), "${case.name} rebuilt")
        }
    }

    @Test
    fun theStillFrameIsNotBlank() {
        for (case in pieceCases) {
            assertTrue(case.create(3).render().toText().any { !it.isWhitespace() }, case.name)
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
            assertTrue(piece.meta.loop >= 0f && piece.meta.still >= 0f, case.name)
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
            if (piece.meta.loop <= 0f) continue
            assertEquals(piece.render(1.25f).toText(), piece.render(1.25f + piece.meta.loop).toText(), case.name)
        }
    }

    @Test
    fun theCatalogListsEveryPieceOnceWithMatchingMeta() {
        assertEquals(pieceCases.map { it.name }, galleryPiecesB.map { it.id })
        assertEquals(galleryPiecesB.size, galleryPiecesB.map { it.id }.toSet().size)
        for (entry in galleryPiecesB) {
            assertEquals(entry.category, entry.create(0).meta.category, entry.id)
        }
        val ported = galleryPiecesB.filter { it.credit != null }
        assertTrue(ported.all { "ascii.rest" in it.credit.orEmpty() })
    }
}
