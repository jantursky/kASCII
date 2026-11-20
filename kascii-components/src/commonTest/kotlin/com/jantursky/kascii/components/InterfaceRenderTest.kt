package com.jantursky.kascii.components

import com.jantursky.kascii.core.BoxStyle
import com.jantursky.kascii.core.KasciiTheme
import kotlin.test.Test

class InterfaceRenderTest {
    private val theme = KasciiTheme.Dark
    private val idle = ControlState()
    private val focused = ControlState(focused = true)

    @Test
    fun button() {
        assertGrid(
            renderButton("OK", idle, theme),
            "| [ OK ] |",
        )
    }

    @Test
    fun buttonFocused() {
        assertGrid(
            renderButton("OK", focused, theme),
            "|>[ OK ]<|",
        )
    }

    @Test
    fun cardRounded() {
        assertGrid(
            renderCard("Note", "Hello brave new world of ascii art", 20, BoxStyle.Rounded, theme),
            "|╭─ Note ───────────╮|",
            "|│ Hello brave new  │|",
            "|│ world of ascii   │|",
            "|│ art              │|",
            "|╰──────────────────╯|",
        )
    }

    @Test
    fun dividerWithLabel() {
        assertGrid(
            renderDivider(20, AsciiDividerStyle.Double, "Part", theme),
            "|══ Part ════════════|",
        )
    }

    @Test
    fun progressBlocks() {
        assertGrid(
            renderProgressBar(24, 0.45f, AsciiBarStyle.Blocks, true, 0f, theme),
            "|[███████░░░░░░░░░░]  45%|",
        )
    }

    @Test
    fun progressSmooth() {
        assertGrid(
            renderProgressBar(24, 0.453f, AsciiBarStyle.Smooth, true, 0f, theme),
            "|[███████▋·········]  45%|",
        )
    }

    @Test
    fun progressIndeterminate() {
        assertGrid(
            renderProgressBar(24, null, AsciiBarStyle.Hash, false, 0.5f, theme),
            "|[-------------#####----]|",
        )
    }

    @Test
    fun spinnerLine() {
        assertGrid(
            renderSpinner(AsciiSpinnerStyle.Line, 0.2f, "x", theme),
            "|- x|",
        )
    }

    @Test
    fun spinnerDots() {
        assertGrid(
            renderSpinner(AsciiSpinnerStyle.Dots, 0.2f, "x", theme),
            "|..  x|",
        )
    }

    @Test
    fun skeleton() {
        assertGrid(
            renderSkeleton(20, 3, 0.5f, theme),
            "|░░░▒▒▒▒░░░░░░░░░░░░░|",
            "|░░░▒▒▒▒░░░░░░░░░░   |",
            "|░░░▒▒▒▒░░░░░        |",
        )
    }

    @Test
    fun checkboxFocused() {
        assertGrid(
            renderMarkerRow(CHECKBOX_MARKERS, true, "Accept", focused, theme),
            "|>[x] Accept|",
        )
    }

    @Test
    fun toggleOff() {
        assertGrid(
            renderMarkerRow(TOGGLE_MARKERS, false, "Dark", idle, theme),
            "| [■OFF] Dark|",
        )
    }

    @Test
    fun sliderHalf() {
        assertGrid(
            renderSlider(24, 0.5f, focused, theme),
            "|>[==========O----------]|",
        )
    }

    @Test
    fun tabSelected() {
        assertGrid(
            renderTab("Home", true, idle, theme),
            "|  Home  |",
            "|════════|",
        )
    }

    @Test
    fun tableSingle() {
        assertGrid(
            renderTable(
                listOf(AsciiTableColumn("Name"), AsciiTableColumn("Qty", AsciiAlign.End)),
                listOf(listOf("apple", "3"), listOf("kiwi", "12")),
                BoxStyle.Single,
            ),
            "|┌───────┬─────┐|",
            "|│ Name  │ Qty │|",
            "|├───────┼─────┤|",
            "|│ apple │   3 │|",
            "|│ kiwi  │  12 │|",
            "|└───────┴─────┘|",
        )
    }

    @Test
    fun tableDoubleCentered() {
        assertGrid(
            renderTable(listOf(AsciiTableColumn("A", AsciiAlign.Center)), listOf(listOf("xyz")), BoxStyle.Double),
            "|╔═════╗|",
            "|║  A  ║|",
            "|╠═════╣|",
            "|║ xyz ║|",
            "|╚═════╝|",
        )
    }

    @Test
    fun calendar() {
        assertGrid(
            renderCalendar(2026, 2, 14, 3, theme),
            "|        February 2026       |",
            "| Mo  Tu  We  Th  Fr  Sa  Su |",
            "|                          1 |",
            "|  2 ( 3)  4   5   6   7   8 |",
            "|  9  10  11  12  13 [14] 15 |",
            "| 16  17  18  19  20  21  22 |",
            "| 23  24  25  26  27  28     |",
        )
    }

    @Test
    fun bigTextTall() {
        assertGrid(
            renderBigText("Hi 42!", AsciiBigFont.Tall),
            "|█   █ █████     █  █   ███  █|",
            "|█   █   █       █  █  █   █ █|",
            "|█████   █       █████    █  █|",
            "|█   █   █          █    █    |",
            "|█   █ █████        █  █████ █|",
        )
    }

    @Test
    fun bigTextCompactAtoM() {
        assertGrid(
            renderBigText("ABCDEFGHIJKLM", AsciiBigFont.Compact),
            "|┌─┐ ┬─┐ ┌─┐ ┬─╮ ┌─╴ ┌─╴ ┌─┐ ╷ ╷ ─┬─  ─┐ ╷ ╱ ╷   ┌┬┐|",
            "|├─┤ ├─┤ │   │ │ ├─  ├─  │ ┬ ├─┤  │    │ ├<  │   │││|",
            "|╵ ╵ ┴─┘ └─┘ ┴─╯ └─╴ ╵   └─┘ ╵ ╵ ─┴─ └─┘ ╵ ╲ └─╴ ╵ ╵|",
        )
    }

    @Test
    fun bigTextCompactNtoZ() {
        assertGrid(
            renderBigText("NOPQRSTUVWXYZ", AsciiBigFont.Compact),
            "|┌─╮ ┌─┐ ┌─┐ ┌─┐ ┌─┐ ┌─╴ ─┬─ ╷ ╷ ╷ ╷ ╷ ╷ ╲ ╱ ╲ ╱ ╶─┐|",
            "|│ │ │ │ ├─┘ │ │ ├┬┘ └─┐  │  │ │ ╲ ╱ │││  ╳   │   ╱ |",
            "|╵ ╵ └─┘ ╵   └─╲ ╵╲  ╶─┘  ╵  └─┘  ╵  └┴┘ ╱ ╲  ╵  └─╴|",
        )
    }

    @Test
    fun bigTextCompactDigits() {
        assertGrid(
            renderBigText("0123456789.,!?", AsciiBigFont.Compact),
            "|┌─┐ ╶┐  ╶─┐ ╶─┐ ╷ ╷ ┌─╴ ┌─╴ ╶─┐ ┌─┐ ┌─┐     │ ╶─┐|",
            "|│╱│  │  ┌─┘  ─┤ └─┤ └─┐ ├─┐   │ ├─┤ └─┤        ╭╯|",
            "|└─┘ ╶┴╴ └─╴ ╶─┘   ╵ ╶─┘ └─┘   ╵ └─┘ ╶─┘ · ╯ ·  · |",
        )
    }

    @Test
    fun clock() {
        assertGrid(
            renderBigText("09:41", AsciiBigFont.Compact),
            "|┌─┐ ┌─┐ · ╷ ╷ ╶┐ |",
            "|│╱│ └─┤   └─┤  │ |",
            "|└─┘ ╶─┘ ·   ╵ ╶┴╴|",
        )
    }

    @Test
    fun bootLogShowsOnlyVisibleEntries() {
        assertGrid(
            renderBootLog(
                listOf(AsciiBootEntry("Started net"), AsciiBootEntry("Disk", AsciiBootStatus.Fail)),
                1,
                30,
                theme,
            ),
            "|[  OK  ] Started net          |",
            "|                              |",
        )
    }

    @Test
    fun notFound() {
        assertGrid(
            renderNotFound("Page not found", theme),
            "|█  █   ███  █  █ |",
            "|█  █  █  ██ █  █ |",
            "|█████ █ █ █ █████|",
            "|   █  ██  █    █ |",
            "|   █   ███     █ |",
            "|                 |",
            "| Page not found  |",
        )
    }

    @Test
    fun badge() {
        assertGrid(
            renderBadge("NEW", AsciiTone.Success, theme),
            "|(NEW)|",
        )
    }

    @Test
    fun avatar() {
        assertGrid(
            renderAvatar("Jan Tursky", theme),
            "|╭────╮|",
            "|│ JT │|",
            "|╰────╯|",
        )
    }

    @Test
    fun toast() {
        assertGrid(
            renderToast("Saved", AsciiTone.Success, theme),
            "|╭───────────╮|",
            "|│ (+) Saved │|",
            "|╰───────────╯|",
        )
    }
}
