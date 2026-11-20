package com.jantursky.kascii.components

import com.jantursky.kascii.core.BoxStyle
import com.jantursky.kascii.core.KasciiTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InterfaceBehaviorTest {
    private val theme = KasciiTheme.Dark
    private val idle = ControlState()

    @Test
    fun buttonColorsFollowTheState() {
        assertEquals(theme.good(), renderButton("OK", idle, theme).colorAt(2, 0))
        assertNull(renderButton("OK", ControlState(pressed = true), theme).colorAt(2, 0))
        assertEquals(theme.dim(), renderButton("OK", ControlState(enabled = false), theme).colorAt(2, 0))
    }

    @Test
    fun cardHasAMinimumWidthAndAnOptionalTitle() {
        val grid = renderCard(null, "hi", 2, BoxStyle.Heavy, theme)
        assertEquals(listOf("┏━━━━━━┓", "┃ hi   ┃", "┗━━━━━━┛"), grid.toText().lines())
        assertEquals("┌─ T ─", renderCard("T", "x", 40, BoxStyle.Single, theme).toText().take(6))
    }

    @Test
    fun longCardTitlesAreCut() {
        val top = renderCard("A very long title indeed", "x", 12, BoxStyle.Single, theme).toText().lines()[0]
        assertEquals(12, top.length)
        assertTrue(top.startsWith("┌─ A very "))
    }

    @Test
    fun everyDividerStyleDrawsItsGlyph() {
        for (style in AsciiDividerStyle.entries) {
            assertEquals(style.glyph.toString().repeat(5), renderDivider(5, style, null, theme).toText())
        }
    }

    @Test
    fun progressBarClampsAndKeepsItsWidth() {
        val empty = renderProgressBar(24, -1f, AsciiBarStyle.Blocks, true, 0f, theme)
        val full = renderProgressBar(24, 2f, AsciiBarStyle.Blocks, true, 0f, theme)
        assertEquals("[░░░░░░░░░░░░░░░░░]   0%", empty.toText())
        assertEquals("[█████████████████] 100%", full.toText())
        assertEquals(10, renderProgressBar(10, 0.5f, AsciiBarStyle.Hash, false, 0f, theme).cols)
    }

    @Test
    fun indeterminateChunkStaysInsideTheTrack() {
        for (step in 0..40) {
            val text = renderProgressBar(12, null, AsciiBarStyle.Hash, false, step / 10f, theme).toText()
            assertEquals(12, text.length)
            assertTrue(text.startsWith("[") && text.endsWith("]"))
            assertEquals(2, text.count { it == '#' })
        }
    }

    @Test
    fun progressColorsMarkTheFilledPart() {
        val grid = renderProgressBar(12, 0.5f, AsciiBarStyle.Blocks, false, 0f, theme)
        assertEquals(theme.good(), grid.colorAt(1, 0))
        assertEquals(theme.dim(), grid.colorAt(9, 0))
    }

    @Test
    fun hasAtLeastTenSpinnerStylesWithEvenFrames() {
        assertTrue(AsciiSpinnerStyle.entries.size >= 10)
        for (style in AsciiSpinnerStyle.entries) {
            assertTrue(style.frames.size > 1, style.name)
            assertEquals(
                1,
                style.frames
                    .map { it.length }
                    .toSet()
                    .size,
                style.name,
            )
            assertTrue(style.fps > 0)
        }
    }

    @Test
    fun spinnerWrapsAroundItsFrames() {
        val style = AsciiSpinnerStyle.Line
        val frames = (0 until 5).map { renderSpinner(style, it / style.fps.toFloat(), null, theme).toText() }
        assertEquals(listOf("|", "/", "-", "\\", "|"), frames)
        assertEquals(1, renderSpinner(style, 0f, null, theme).cols)
    }

    @Test
    fun skeletonLinesShrinkAndWrapAroundThePattern() {
        val grid = renderSkeleton(20, 6, 0f, theme)
        val widths = grid.toText().lines().map { it.trimEnd().length }
        assertEquals(listOf(20, 17, 12, 19, 14, 20), widths)
    }

    @Test
    fun skeletonSweepMoves() {
        assertNotEquals(renderSkeleton(20, 1, 0f, theme).toText(), renderSkeleton(20, 1, 0.5f, theme).toText())
    }

    @Test
    fun markerRowsHandleStateAndEmptyLabels() {
        val bare = renderMarkerRow(RADIO_MARKERS, true, "", idle, theme)
        assertEquals(" (*)", bare.toText())
        assertEquals(theme.good(), bare.colorAt(1, 0))
        val off = renderMarkerRow(RADIO_MARKERS, false, "A", idle, theme)
        assertEquals(" ( ) A", off.toText())
        assertNull(off.colorAt(1, 0))
        val disabled = renderMarkerRow(CHECKBOX_MARKERS, true, "A", ControlState(enabled = false), theme)
        assertEquals(theme.dim(), disabled.colorAt(1, 0))
        assertEquals(theme.dim(), disabled.colorAt(5, 0))
        val pressed = renderMarkerRow(CHECKBOX_MARKERS, true, "A", ControlState(focused = true, pressed = true), theme)
        assertNull(pressed.colorAt(1, 0))
        assertNull(pressed.colorAt(0, 0))
    }

    @Test
    fun sliderThumbSitsAtTheEnds() {
        assertEquals(" [O--------]", renderSlider(12, 0f, idle, theme).toText())
        assertEquals(" [========O]", renderSlider(12, 1f, idle, theme).toText())
        assertEquals(theme.dim(), renderSlider(12, 0.5f, ControlState(enabled = false), theme).colorAt(1, 0))
        assertEquals(theme.good(), renderSlider(12, 0.5f, idle, theme).colorAt(2, 0))
    }

    @Test
    fun tabsMarkFocusAndSelection() {
        val focusedTab = renderTab("Docs", false, ControlState(focused = true), theme)
        assertEquals(listOf(" >Docs< ", "────────"), focusedTab.toText().lines())
        assertEquals(theme.dim(), focusedTab.colorAt(2, 0))
        assertEquals(theme.accent, renderTab("Docs", true, idle, theme).colorAt(2, 0))
    }

    @Test
    fun treeDrawsNestedBranches() {
        val tree =
            AsciiTreeNode(
                "proj",
                listOf(
                    AsciiTreeNode("src", listOf(AsciiTreeNode("a.kt"), AsciiTreeNode("b.kt"))),
                    AsciiTreeNode("README"),
                ),
            )
        assertEquals(
            listOf("proj/", "├── src/", "│   ├── a.kt", "│   └── b.kt", "└── README"),
            renderTree(tree),
        )
        assertEquals("proj (src (a.kt, b.kt), README)", describeTree(tree))
        assertEquals(listOf("solo"), renderTree(AsciiTreeNode("solo")))
    }

    @Test
    fun tableDescriptionListsEveryRow() {
        val columns = listOf(AsciiTableColumn("Name"), AsciiTableColumn("Qty"))
        assertEquals(
            "Table, 2 columns: Name, Qty. Row 1: apple, 3. Row 2: kiwi, 12",
            describeTable(columns, listOf(listOf("apple", "3"), listOf("kiwi", "12"))),
        )
    }

    @Test
    fun tableUsesTheJunctionsOfItsBorder() {
        val columns = listOf(AsciiTableColumn("A"), AsciiTableColumn("B"))
        val rows = listOf(listOf("1", "2"))
        assertEquals("╦", renderTable(columns, rows, BoxStyle.Double).toText().lines()[0].substring(4, 5))
        assertEquals("┳", renderTable(columns, rows, BoxStyle.Heavy).toText().lines()[0].substring(4, 5))
        assertEquals("+", renderTable(columns, rows, BoxStyle.Ascii).toText().lines()[2].substring(4, 5))
        assertEquals("┬", renderTable(columns, rows, BoxStyle.Rounded).toText().lines()[0].substring(4, 5))
        assertEquals(4, renderTable(columns, emptyList(), BoxStyle.Single).rows)
    }

    @Test
    fun calendarMathKnowsLeapYearsAndWeekdays() {
        assertEquals(6, dayOfWeek(2000, 1, 1))
        assertEquals(0, dayOfWeek(2026, 2, 1))
        assertEquals(1, dayOfWeek(2027, 2, 1))
        assertEquals(29, daysInMonth(2024, 2))
        assertEquals(28, daysInMonth(1900, 2))
        assertEquals(29, daysInMonth(2000, 2))
        assertEquals(30, daysInMonth(2026, 4))
        assertEquals(31, daysInMonth(2026, 12))
    }

    @Test
    fun calendarHasTheRightNumberOfWeeks() {
        assertEquals(2 + 4, renderCalendar(2027, 2, null, null, theme).rows)
        assertEquals(2 + 6, renderCalendar(2026, 8, null, null, theme).rows)
    }

    @Test
    fun calendarMarksSelectionAndToday() {
        val grid = renderCalendar(2026, 2, 14, 3, theme)
        assertEquals(theme.accent, grid.colorAt(21, 4))
        assertEquals("[14]", grid.toText().lines()[4].substring(20, 24))
        assertEquals("( 3)", grid.toText().lines()[3].substring(4, 8))
    }

    @Test
    fun clockTextIsZeroPaddedAndValidated() {
        assertEquals("09:05", clockText(9, 5, null))
        assertEquals("23:59:07", clockText(23, 59, 7))
        assertFailsWith<IllegalArgumentException> { clockText(24, 0, null) }
        assertFailsWith<IllegalArgumentException> { clockText(0, 60, null) }
        assertFailsWith<IllegalArgumentException> { clockText(0, 0, 60) }
    }

    @Test
    fun everyBigGlyphIsRectangularAndTheSetIsComplete() {
        for (font in AsciiBigFont.entries) {
            val glyphs = font.glyphs()
            for ((char, rows) in glyphs) {
                assertEquals(font.rows, rows.size, "$char in ${font.name}")
                assertEquals(1, rows.map { it.length }.toSet().size, "$char in ${font.name}")
            }
            for (char in ('A'..'Z') + ('0'..'9') + listOf(' ', '.', ',', '!', '?', '-', ':')) {
                assertTrue(char in glyphs, "$char missing in ${font.name}")
            }
        }
    }

    @Test
    fun bigTextUppercasesAndFallsBackToQuestionMark() {
        for (font in AsciiBigFont.entries) {
            assertEquals(renderBigText("ab", font).toText(), renderBigText("AB", font).toText())
            assertEquals(renderBigText("?", font).toText(), renderBigText("~", font).toText())
            assertEquals(0, renderBigText("", font).cols)
            assertEquals(font.rows, renderBigText("", font).rows)
        }
    }

    @Test
    fun terminalScrollsAndKeepsThePromptLast() {
        val grid = renderTerminal(listOf("one", "two", "three", "four"), 12, 3, "> ", "ls", false, theme)
        assertEquals(listOf("three", "four", "> ls"), grid.toText().lines().map { it.trimEnd() })
        assertEquals(theme.good(), grid.colorAt(0, 2))
        assertNull(grid.colorAt(0, 1))
    }

    @Test
    fun terminalWrapsLongLinesAndEmptyLines() {
        val grid = renderTerminal(listOf("abcdefgh", ""), 5, 6, "$ ", "", true, theme)
        assertEquals(
            listOf("abcde", "fgh", "", "$ █"),
            grid
                .toText()
                .lines()
                .map { it.trimEnd() }
                .take(4),
        )
    }

    @Test
    fun terminalPromptSplitAcrossRowsStaysColored() {
        val grid = renderTerminal(emptyList(), 4, 2, "> ", "abcdef", false, theme)
        assertEquals(listOf("> ab", "cdef"), grid.toText().lines())
        assertEquals(theme.good(), grid.colorAt(0, 0))
    }

    @Test
    fun bootLogTruncatesAndColorsStatus() {
        val entries = listOf(AsciiBootEntry("x".repeat(40), AsciiBootStatus.Warn), AsciiBootEntry("later"))
        val grid = renderBootLog(entries, 5, 20, theme)
        assertEquals("[ WARN ] xxxxxxxxxxx", grid.toText().lines()[0])
        assertEquals(theme.warn(), grid.colorAt(2, 0))
        assertEquals("[  OK  ] later", grid.toText().lines()[1].trimEnd())
        assertTrue(renderBootLog(entries, 0, 20, theme).toText().isBlank())
    }

    @Test
    fun badgeAndToastCarryTheToneColorAndMarker() {
        assertEquals(theme.bad(), renderBadge("ERR", AsciiTone.Error, theme).colorAt(1, 0))
        assertNull(renderBadge("n", AsciiTone.Neutral, theme).colorAt(1, 0))
        val toast = renderToast("Oops", AsciiTone.Error, theme)
        assertEquals("│ (x) Oops │", toast.toText().lines()[1])
        assertEquals(theme.bad(), toast.colorAt(3, 1))
    }

    @Test
    fun avatarInitialsAndColors() {
        assertEquals("JT", initialsOf("Jan Tursky"))
        assertEquals("JT", initialsOf("  jan   middle  tursky "))
        assertEquals("AL", initialsOf("alice"))
        assertEquals("?", initialsOf("   "))
        assertEquals(theme.pick("A".sumOf { it.code }), renderAvatar("A", theme).colorAt(0, 0))
        assertNotEquals(renderAvatar("A", theme).colorAt(0, 0), renderAvatar("B", theme).colorAt(0, 0))
    }

    @Test
    fun notFoundIsAsWideAsItsWidestPart() {
        assertEquals(17, renderNotFound("x", theme).cols)
        assertEquals(30, renderNotFound("y".repeat(30), theme).cols)
    }
}
