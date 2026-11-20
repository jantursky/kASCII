package com.jantursky.kascii.components

import com.jantursky.kascii.core.KasciiTheme
import kotlin.test.Test

class DataRenderTest {
    private val theme = KasciiTheme.Dark
    private val idle = ControlState()
    private val focused = ControlState(focused = true)

    @Test
    fun barChart() {
        assertGrid(
            renderBarChart(listOf(3f, 5f, 1f, 8f), listOf("a", "b", "c", "d"), 4, 3, 1, theme),
            "|            ███|",
            "|    ▄▄▄     ███|",
            "|▄▄▄ ███     ███|",
            "|███ ███ ▄▄▄ ███|",
            "|a   b   c   d  |",
        )
    }

    @Test
    fun sparkline() {
        assertGrid(
            renderSparkline(listOf(1f, 3f, 5f, 7f, 4f, 2f), theme),
            "|▁▃▆█▅▂|",
        )
    }

    @Test
    fun lineChart() {
        assertGrid(
            renderLineChart(listOf(1f, 4f, 2f, 8f, 5f), 16, 4, theme),
            "|8          ⢠⠓⢄⡀ |",
            "|          ⢠⠃  ⠈⠢|",
            "|    ⢀⠔⠤⣀ ⢠⠃     |",
            "|1 ⡠⠊⠁   ⠉⠃      |",
        )
    }

    @Test
    fun candlesticks() {
        assertGrid(
            renderCandlesticks(
                listOf(Candle(5f, 8f, 4f, 7f), Candle(7f, 9f, 3f, 4f), Candle(4f, 6f, 2f, 5f)),
                8,
                theme,
            ),
            "|  │  |",
            "|│ │  |",
            "|▒ █  |",
            "|▒ █ │|",
            "|▒ █ ▒|",
            "|│ █ ▒|",
            "|  │ │|",
            "|    │|",
        )
    }

    @Test
    fun gauge() {
        assertGrid(
            renderGauge(0.6f, 16, "CPU", theme),
            "|   ⣠⣴⠶⠛⠛⠛⠳⢶⣤⡀   |",
            "| ⣠⡾⠋       ⠈⠻⣦⡀ |",
            "|⢰⡟           ⠘⣷ |",
            "|⣿      60%    ⢸⡇|",
            "|       CPU      |",
        )
    }

    @Test
    fun heatmap() {
        assertGrid(
            renderHeatmap(listOf(listOf(0f, 0.5f, 1f), listOf(1f, 0.25f, 0f)), listOf("Mon", "Tue"), theme),
            "|Mon   ▒▒██|",
            "|Tue ██░░  |",
        )
    }

    @Test
    fun radar() {
        assertGrid(
            renderRadar(listOf(1f, 0.5f, 0.8f, 0.3f), listOf("ATK", "DEF", "SPD", "INT"), 30, 12, theme),
            "|              ATK             |",
            "|             ⢀⢠⠳⡀             |",
            "|           ⢀⠔⠁⡜⡇⠱⡀⢄           |",
            "|         ⢀⠔⠁ ⢰⠁⡇ ⠱⡀⠑⢄         |",
            "|       ⢀⠔⠁   ⡇ ⡇  ⠱⡀ ⠑⢄       |",
            "|     ⢀⠔⠁    ⡸  ⡇   ⠱⡀  ⠑⢄     |",
            "| INT ⠙⢍⠉⠉⠉⠉⠉⢣⠉⠉⡏⠉⠉⠉⡰⠁⠉⠉⢉⠝⠁ DEF|",
            "|       ⠑⢄    ⢇ ⡇  ⡔⠁ ⢀⠔⠁      |",
            "|         ⠑⢄  ⠘⡄⡇⢀⠎ ⢀⠔⠁        |",
            "|           ⠑⢄ ⠸⣠⠊⢀⠔⠁          |",
            "|             ⠑⢄⠁⠔⠁            |",
            "|              SPD             |",
        )
    }

    @Test
    fun equalizer() {
        assertGrid(
            renderEqualizer(6, 4, 0.3f, 7L, theme),
            "|      ▇▇         |",
            "|      ██ ██      |",
            "|▆▆    ██ ██ ▅▅   |",
            "|██ ▄▄ ██ ██ ██ ▆▆|",
        )
    }

    @Test
    fun cpuMeters() {
        assertGrid(
            renderCpuMeters(listOf(0.1f, 0.6f, 0.95f), 28, theme),
            "| 0 [|                 ]  10%|",
            "| 1 [||||||||||        ]  60%|",
            "| 2 [||||||||||||||||| ]  95%|",
        )
    }

    @Test
    fun heartbeat() {
        assertGrid(
            renderHeartbeat(30, 3, 0.2f, 72, theme),
            "|  ⢰            ⡆           ⢰  |",
            "|  ⡜⡄ ⢀⡀       ⢠⢣  ⣀        ⡜⡄ |",
            "|⠉⠉⠇⡟⠉⠁⠈⠉⠉⠉⠉⠉⠉⠉⠹⢸⠋⠉ ⠉⠉⠉⠉⠉⠉⠉⠉⠇⡟⠉|",
        )
    }

    @Test
    fun uptimeBar() {
        assertGrid(
            renderUptimeBar(
                listOf(
                    AsciiUptimeState.Up,
                    AsciiUptimeState.Up,
                    AsciiUptimeState.Degraded,
                    AsciiUptimeState.Down,
                    AsciiUptimeState.Unknown,
                ),
                theme,
            ),
            "|██▄▁· 50% uptime|",
        )
    }

    @Test
    fun pieChart() {
        assertGrid(
            renderPieChart(
                listOf(AsciiPieSlice("Kotlin", 60f), AsciiPieSlice("Swift", 30f), AsciiPieSlice("C", 10f)),
                16,
                theme,
            ),
            "|   ⢀⣤⣴⣶⣶⣶⣶⣦⣤⡀   |",
            "| ⢀⣴⣿⠻⣿⣿⣿⣿⣿⣿⣿⣿⣦⡀ |",
            "|⢀⣿⣿⣿⣿⠘⢿⣿⣿⣿⣿⣿⣿⣿⣿⡀|",
            "|⢸⣿⣿⣿⣿⣿⠈⠻⣿⣿⣿⣿⣿⣿⣿⡇|",
            "|⢸⣿⣿⣿⣿⣿⡿⠋⣿⣿⣿⣿⣿⣿⣿⡇|",
            "|⠈⣿⣿⣿⣿⡟⠁⣿⣿⣿⣿⣿⣿⣿⣿⠁|",
            "| ⠈⠻⣿⠋⣿⣿⣿⣿⣿⣿⣿⣿⠟⠁ |",
            "|   ⠈⠛⠻⠿⠿⠿⠿⠟⠛⠁   |",
            "|■ Kotlin 60%    |",
            "|■ Swift 30%     |",
            "|■ C 10%         |",
        )
    }

    @Test
    fun statCard() {
        assertGrid(
            renderStatCard("Revenue", "1,204", 12.5f, listOf(1f, 3f, 2f, 5f), 28, theme),
            "|╭──────────────────────────╮|",
            "|│ Revenue                  │|",
            "|│ ╶┐    ╶─┐ ┌─┐ ╷ ╷        │|",
            "|│  │    ┌─┘ │╱│ └─┤        │|",
            "|│ ╶┴╴ ╯ └─╴ └─┘   ╵        │|",
            "|│ ▲ 12.5%             ▁▅▃█ │|",
            "|╰──────────────────────────╯|",
        )
    }
}
