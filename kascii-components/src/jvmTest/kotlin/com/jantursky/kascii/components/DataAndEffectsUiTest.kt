package com.jantursky.kascii.components

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithContentDescription
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class DataAndEffectsUiTest {
    @Test
    fun dataComponentsDescribeTheirData() = runUiTest {
        setComponent {
            Column {
                AsciiBarChart(listOf(3f, 5f), labels = listOf("a", "b"))
                AsciiLineChart(listOf(1f, 2f, 3f))
                AsciiSparkline(listOf(1f, 3f, 2f))
                AsciiCandlesticks(listOf(Candle(1f, 3f, 0f, 2f)))
                AsciiGauge(0.6f, label = "CPU")
                AsciiHeatmap(listOf(listOf(0f, 1f)), rowLabels = listOf("Mon"))
                AsciiRadar(listOf(1f, 0.5f, 0.2f), listOf("A", "B", "C"))
                AsciiCpuMeters(listOf(0.5f))
                AsciiUptimeBar(listOf(AsciiUptimeState.Up, AsciiUptimeState.Down))
                AsciiPieChart(listOf(AsciiPieSlice("x", 1f), AsciiPieSlice("y", 1f)))
                AsciiStatCard("Users", "7", delta = 2f, trend = listOf(1f, 2f))
            }
        }
        listOf(
            "Bar chart: a 3, b 5",
            "Line chart, 3 points from 1 to 3, low 1, high 3",
            "Sparkline, 3 values from 1 to 2, low 1, high 3",
            "Candlestick chart, 1 candles, 1 rising, 0 falling",
            "CPU 60%",
            "Heat map, 1 rows: Mon 0 1",
            "Radar chart: A 100%, B 50%, C 20%",
            "CPU load: core 0 50%",
            "Uptime 50%: 1 slots down",
            "Pie chart: x 50%, y 50%",
            "Users: 7, up 2 percent",
        ).forEach { onNodeWithContentDescription(it).assertExists() }
    }

    @Test
    fun textEffectsAnnounceTheFinalText() = runUiTest {
        mainClock.autoAdvance = false
        setComponent {
            Column {
                AsciiTypewriter("typed")
                AsciiScrambleText("scrambled")
                AsciiGlitchText("glitched")
                AsciiDissolveText("dissolved")
                AsciiMarquee("marquee text")
                AsciiWaveText("waving")
                AsciiSplitFlap("flap")
                AsciiMorse("sos")
                AsciiRainbowText("rainbow")
                AsciiShimmerText("shimmer")
            }
        }
        listOf(
            "typed", "scrambled", "glitched", "dissolved", "marquee text", "waving", "flap", "Morse code: sos",
            "rainbow", "shimmer",
        ).forEach { onNodeWithContentDescription(it).assertExists() }
    }

    @Test
    fun animatedDecorationHasNoDescription() = runUiTest {
        mainClock.autoAdvance = false
        setComponent {
            Column {
                AsciiEqualizer()
                AsciiHeartbeat()
                AsciiSkeleton()
                AsciiTerminal(listOf("boot"), typing = "ls")
                AsciiBootLog(listOf(AsciiBootEntry("net")))
                AsciiDivider(label = "Part")
            }
        }
        onNodeWithContentDescription("Loading").assertExists()
        onNodeWithContentDescription("boot\n$ ls").assertExists()
        onNodeWithContentDescription("Part").assertExists()
        onNodeWithContentDescription("net: ok").assertExists()
    }

    @Test
    fun aGridIsExactlyAsLargeAsItsCells() = runUiTest {
        setComponent { AsciiBadge("ab") }
        onNodeWithContentDescription("ab").assertWidthIsEqualTo(CellWidth * 4).assertHeightIsEqualTo(CellHeight)
    }
}
