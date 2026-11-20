package com.jantursky.kascii.samples.gallery

import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.samples.gallery.examples.ExportFormat
import com.jantursky.kascii.samples.gallery.examples.export
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CatalogTest {
    @Test
    fun everyPageHasItsOwnId() {
        assertEquals(Catalog.size, Catalog.map { it.id }.toSet().size)
    }

    @Test
    fun everyPieceHasAPage() {
        assertEquals(34, Pieces.size)
        Pieces.forEach { assertEquals(Group.Pieces, pageFor(it.id).group) }
    }

    @Test
    fun theComponentPagesAreRegisteredAndReachableFromTheNav() {
        assertEquals(8, ComponentPages.size)
        ComponentPages.forEach { assertEquals(Group.Components, pageFor(it.id).group) }
        val link = NavLinks.single { it.title == "Components" }
        assertTrue(ComponentPages.all { link.isCurrent(it) })
        assertTrue(NavLinks.single { it.title == "Pieces" }.isCurrent(pageFor("donut")))
        assertEquals("terminal-app", pageFor("terminal-app").id)
    }

    @Test
    fun anUnknownIdOpensTheOverview() {
        assertEquals("overview", pageFor("no-such-page").id)
        assertEquals("overview", pageFor(null).id)
    }

    @Test
    fun everySourceNamedByAPageIsGenerated() {
        Catalog.flatMap { it.sources }.forEach { assertNotNull(ExampleSources[it], it) }
    }

    @Test
    fun theGeneratedSourceHasNoImports() {
        ExampleSources.values.forEach { assertTrue(!it.contains("import ")) }
    }

    @Test
    fun aFrameExportsToEveryFormat() {
        val grid = Grid(3, 2).apply { write(0, 0, "abc") }
        assertEquals("abc\n", grid.export(ExportFormat.Text, KasciiTheme.Dark))
        assertTrue(grid.export(ExportFormat.Ansi, KasciiTheme.Dark).startsWith("abc"))
        assertTrue(grid.export(ExportFormat.Svg, KasciiTheme.Dark).startsWith("<svg"))
    }

    @Test
    fun everyColorChoiceIsADistinctMode() {
        assertEquals(
            6,
            ColorChoice.entries
                .map { it.mode::class }
                .toSet()
                .size,
        )
    }
}
