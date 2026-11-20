package com.jantursky.kascii.components

import com.jantursky.kascii.core.KasciiTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DataBehaviorTest {
    private val theme = KasciiTheme.Dark

    @Test
    fun columnsGrowInEighths() {
        val grid = renderColumns(listOf(0f, 0.5f, 1f, 0.0625f, 2f, -1f), rows = 2, barWidth = 1, gap = 0, color = null)
        assertEquals(6, grid.cols)
        assertEquals(' ', grid.glyphAt(0, 1))
        assertEquals('█', grid.glyphAt(1, 1))
        assertEquals(' ', grid.glyphAt(1, 0))
        assertEquals('█', grid.glyphAt(2, 0))
        assertEquals('▁', grid.glyphAt(3, 1))
        assertEquals('█', grid.glyphAt(4, 0))
        assertEquals(' ', grid.glyphAt(5, 1))
    }

    @Test
    fun barChartHandlesEmptyAndZeroData() {
        assertEquals(0, renderBarChart(emptyList(), emptyList(), 4, 3, 1, theme).cols)
        assertEquals(
            listOf("   ", "   ", "   ", "   ", "a  "),
            renderBarChart(listOf(0f), listOf("a"), 4, 3, 1, theme).toText().lines(),
        )
    }

    @Test
    fun sparklineHandlesFlatAndEmptyData() {
        assertEquals("▅▅▅", renderSparkline(listOf(2f, 2f, 2f), theme).toText())
        assertEquals(0, renderSparkline(emptyList(), theme).cols)
        assertEquals("▁█", renderSparkline(listOf(-5f, 5f), theme).toText())
    }

    @Test
    fun seriesDescriptionNamesTheRange() {
        assertEquals("Line chart, no data", describeSeries("Line chart", "points", emptyList()))
        assertEquals(
            "Sparkline, 3 values from 1 to 2, low 1, high 3",
            describeSeries("Sparkline", "values", listOf(1f, 3f, 2f)),
        )
    }

    @Test
    fun lineChartHandlesEmptySingleAndFlatSeries() {
        assertEquals(0, renderLineChart(emptyList(), 10, 3, theme).toText().count { it in '⠁'..'⣿' })
        assertTrue(renderLineChart(listOf(3f), 10, 3, theme).toText().any { it in '⠁'..'⣿' })
        val flat = renderLineChart(listOf(2f, 2f, 2f), 10, 3, theme).toText().lines()
        assertTrue(flat[1].any { it in '⠁'..'⣿' })
        assertEquals("2", flat[0].trim().take(1))
    }

    @Test
    fun candlesticksColorRisingAndFallingApart() {
        val grid = renderCandlesticks(listOf(Candle(1f, 4f, 0f, 3f), Candle(3f, 4f, 0f, 1f)), 5, theme)
        assertEquals(theme.good(), grid.colorAt(0, 2))
        assertEquals(theme.bad(), grid.colorAt(2, 2))
        assertTrue(grid.toText().contains('▒') && grid.toText().contains('█'))
        assertEquals(0, renderCandlesticks(emptyList(), 5, theme).cols)
        assertEquals(1, renderCandlesticks(listOf(Candle(2f, 2f, 2f, 2f)), 4, theme).cols)
    }

    @Test
    fun gaugeClampsItsValueAndHasNoLabelRowWithoutLabel() {
        val plain = renderGauge(1.7f, 16, null, theme)
        assertEquals(4, plain.rows)
        assertTrue(plain.toText().contains("100%"))
        assertTrue(renderGauge(-1f, 16, null, theme).toText().contains("0%"))
        assertEquals(5, renderGauge(0.5f, 16, "CPU", theme).rows)
    }

    @Test
    fun gaugeColorsTheFilledArc() {
        val grid = renderGauge(0.5f, 24, null, theme)
        val cells = (0 until grid.cols).flatMap { x -> (0 until grid.rows).map { y -> x to y } }
        val dots = cells.filter { (x, y) -> grid.glyphAt(x, y) in '⠁'..'⣿' }
        val colors = dots.mapNotNull { (x, y) -> grid.colorAt(x, y) }
        assertTrue(theme.accent in colors && theme.dim() in colors)
    }

    @Test
    fun heatmapHandlesEmptyAndFlatData() {
        assertEquals(0, renderHeatmap(emptyList(), emptyList(), theme).rows)
        assertEquals("  ", renderHeatmap(listOf(listOf(5f)), emptyList(), theme).toText())
    }

    @Test
    fun heatmapColorsFollowIntensity() {
        val grid = renderHeatmap(listOf(listOf(0f, 1f)), emptyList(), theme)
        assertEquals(theme.dim(), grid.colorAt(0, 0))
        assertEquals(theme.accent, grid.colorAt(2, 0))
    }

    @Test
    fun radarNeedsThreeSpokes() {
        assertFailsWith<IllegalArgumentException> { renderRadar(listOf(1f, 1f), listOf("a", "b"), 20, 10, theme) }
    }

    @Test
    fun radarLabelsLandInsideTheGrid() {
        val grid = renderRadar(listOf(1f, 1f, 1f), listOf("TOP", "RIGHT", "LEFT"), 40, 14, theme)
        assertTrue(grid.toText().contains("TOP"))
        assertTrue(grid.toText().contains("RIGHT"))
        assertTrue(grid.toText().contains("LEFT"))
    }

    @Test
    fun equalizerIsDeterministicAndBounded() {
        assertEquals(equalizerLevels(8, 1.5f, 3L), equalizerLevels(8, 1.5f, 3L))
        assertNotEquals(equalizerLevels(8, 1.5f, 3L), equalizerLevels(8, 1.5f, 4L))
        assertNotEquals(equalizerLevels(8, 1.5f, 3L), equalizerLevels(8, 1.6f, 3L))
        for (step in 0..100) assertTrue(equalizerLevels(8, step / 10f, 9L).all { it in 0f..1f })
    }

    @Test
    fun cpuMetersChangeColorWithLoad() {
        val grid = renderCpuMeters(listOf(0.2f, 0.6f, 0.9f), 20, theme)
        assertEquals(theme.good(), grid.colorAt(4, 0))
        assertEquals(theme.warn(), grid.colorAt(4, 1))
        assertEquals(theme.bad(), grid.colorAt(4, 2))
    }

    @Test
    fun ecgBeatPeaksAtTheRWaveAndRestsBetweenBeats() {
        assertTrue(ecgSample(0.44f) > 0.9f)
        assertTrue(ecgSample(0.95f) < 0.01f)
        assertTrue(ecgSample(0.48f) < 0f)
    }

    @Test
    fun heartbeatScrolls() {
        assertNotEquals(
            renderHeartbeat(30, 3, 0f, 72, theme).toText(),
            renderHeartbeat(30, 3, 0.4f, 72, theme).toText(),
        )
    }

    @Test
    fun uptimeCountsOnlyKnownSlots() {
        assertNull(uptimePercent(emptyList()))
        assertNull(uptimePercent(listOf(AsciiUptimeState.Unknown)))
        assertEquals(
            50f,
            uptimePercent(listOf(AsciiUptimeState.Up, AsciiUptimeState.Down, AsciiUptimeState.Unknown))!!,
        )
        assertTrue(renderUptimeBar(emptyList(), theme).toText().contains("n/a"))
    }

    @Test
    fun uptimeSlotsHaveTheirOwnColor() {
        val grid = renderUptimeBar(AsciiUptimeState.entries, theme)
        assertEquals(listOf(theme.good(), theme.warn(), theme.bad(), theme.dim()), (0..3).map { grid.colorAt(it, 0) })
    }

    @Test
    fun pieSharesHandleZeroAndNegativeValues() {
        assertEquals(listOf(0f, 0f), pieShares(listOf(AsciiPieSlice("a", 0f), AsciiPieSlice("b", 0f))))
        assertEquals(listOf(0f, 1f), pieShares(listOf(AsciiPieSlice("a", -3f), AsciiPieSlice("b", 2f))))
        assertEquals(listOf(0.25f, 0.75f), pieShares(listOf(AsciiPieSlice("a", 1f), AsciiPieSlice("b", 3f))))
    }

    @Test
    fun pieLegendColorsMatchTheSlices() {
        val grid = renderPieChart(listOf(AsciiPieSlice("a", 1f), AsciiPieSlice("b", 1f)), 12, theme)
        assertEquals(theme.pick(0), grid.colorAt(0, 6))
        assertEquals(theme.pick(1), grid.colorAt(0, 7))
        assertTrue(renderPieChart(listOf(AsciiPieSlice("z", 0f)), 12, theme).toText().contains("z 0%"))
    }

    @Test
    fun statCardShowsDirectionAndSkipsMissingParts() {
        val down = renderStatCard("Users", "7", -3.5f, emptyList(), 24, theme)
        assertEquals(theme.bad(), down.colorAt(2, 5))
        assertTrue(down.toText().contains("▼ 3.5%"))
        val plain = renderStatCard("Users", "7", null, listOf(1f, 2f), 24, theme)
        assertTrue(!plain.toText().contains("%"))
    }
}
