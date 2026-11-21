package com.jantursky.kascii.gallery.o

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PiecesOTest {
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
            assertTrue(piece.meta.still > 0f && piece.meta.still < piece.meta.loop, case.name)
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
            assertTrue(piece.meta.loop > 0f && piece.meta.still >= 0f, case.name)
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
        val times = listOf(2.5f, 0.5f, 7f, 0f, 11.3f)
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
            for (t in listOf(0f, 1.25f, piece.meta.loop / 2f)) {
                assertEquals(piece.render(t).toText(), piece.render(t + piece.meta.loop).toText(), "${case.name} at $t")
            }
        }
    }

    @Test
    fun theSceneMovesAcrossTheLoop() {
        for (case in pieceCases) {
            val piece = case.create(0)
            val quarters = (0 until 4).map { piece.render(it * piece.meta.loop / 4f).toText() }
            assertEquals(4, quarters.toSet().size, case.name)
        }
    }

    @Test
    fun everyGlyphIsInTheBundledFontSubsetTheGalleryUses() {
        for (case in pieceCases) {
            val piece = case.create(2)
            val steps = (piece.meta.loop * 4f).toInt()
            for (i in 0 until steps) {
                val text = piece.render(i / 4f).toText()
                val stray = text.filter { it != '\n' && !allowed(it) }.toSet()
                assertTrue(stray.isEmpty(), "${case.name} at ${i / 4f} draws $stray")
            }
        }
    }

    @Test
    fun theCatalogListsEveryPieceOnceWithMatchingMeta() {
        assertEquals(pieceCases.map { it.name }, galleryPiecesO.map { it.id })
        assertEquals(galleryPiecesO.size, galleryPiecesO.map { it.id }.toSet().size)
        for (entry in galleryPiecesO) {
            assertEquals(entry.category, entry.create(0).meta.category, entry.id)
            assertEquals(null, entry.credit, entry.id)
        }
    }

    /** Printable ASCII, box drawing and block elements: the glyphs every renderer and the bundled font draw. */
    private fun allowed(c: Char): Boolean = c in ' '..'~' || c in '─'..'▟' || c in "·•●"
}
