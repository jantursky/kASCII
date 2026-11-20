package com.jantursky.kascii.samples.gallery

import com.jantursky.kascii.samples.gallery.terminal.TerminalAppPage

/** The pages of the gallery that are not one piece, in nav order. */
private val Showcase: List<Page> =
    listOf(
        Page(
            "overview",
            Group.Start,
            "Overview",
            "ASCII art for Compose Multiplatform.",
            emptyList(),
            wide = true,
        ) { Overview(it) },
        Page(
            "pieces",
            Group.Showcase,
            "Pieces",
            "Every piece of the gallery, live. A piece is a function of time that draws a grid of glyphs; open one " +
                "for its controls and exports.",
            emptyList(),
            wide = true,
        ) { PiecesGrid(it) },
        Page(
            "fields",
            Group.Showcase,
            "Fields",
            "A field is a function of position and time that returns a tone from 0 to 1. A charset turns tones into " +
                "glyphs and a color mode turns them into colors. Pick any combination.",
            listOf("FieldsExample"),
        ) { FieldsPage(it) },
        Page(
            "renderers",
            Group.Showcase,
            "Renderers",
            "The same field drawn by each renderer. Auto picks Gpu where the platform has a shader and the grid is " +
                "large, otherwise Cpu.",
            listOf("RenderersExample"),
        ) { RenderersPage(it) },
        Page(
            "modifier",
            Group.Showcase,
            "Modifier and image",
            "Modifier.ascii turns any composable into ASCII, and AsciiImage does the same for a bitmap. Sampled " +
                "colors give each glyph the color of the pixels under it.",
            listOf("ModifierExample", "ImageExample"),
        ) { ModifierPage(it) },
        Page(
            "theme",
            Group.Showcase,
            "Themes",
            "A theme is ink, paper, an accent and a palette. Light and dark follow the design system; phosphor and " +
                "amber are there for the terminal look.",
            listOf("ThemeExample"),
        ) { ThemePage(it) },
        Page(
            "terminal-app",
            Group.Showcase,
            "Terminal app",
            "A master and detail app in the terminal style, made only of the components and layout: a server list, " +
                "a detail screen and settings, with a dialog and a toast. It is also what the Android and iOS " +
                "apps open. Its source is in the terminal package of this module.",
            emptyList(),
            fills = true,
        ) { TerminalAppPage(it) },
    )

private val PiecePages: List<Page> =
    Pieces.map { entry ->
        Page(
            entry.id,
            Group.Pieces,
            entry.title,
            listOfNotNull(entry.category, entry.credit).joinToString(" · "),
            listOf("PieceExample", "ExportExample"),
            fills = true,
        ) { PiecePage(entry, it) }
    }

/** Every page of the gallery. A piece's page has the id of the piece. */
val Catalog: List<Page> = Showcase + ComponentPages + PiecePages

/** A link of the nav bar: it is current on the page [id] and, when it has a [group], on every page of that group. */
class NavLink(
    val title: String,
    val id: String,
    private val group: Group? = null,
) {
    fun isCurrent(page: Page): Boolean = id == page.id || (group != null && page.group == group)
}

/** The nav bar: the showcase pages, then one link for the components and one for the pieces, which open from it. */
val NavLinks: List<NavLink> =
    Showcase.map { NavLink(it.title, it.id, if (it.id == "pieces") Group.Pieces else null) } +
        NavLink("Components", ComponentPages.first().id, Group.Components)

/** The page with [id], or the overview for an unknown id. */
fun pageFor(id: String?): Page = Catalog.firstOrNull { it.id == id } ?: Catalog.first()
