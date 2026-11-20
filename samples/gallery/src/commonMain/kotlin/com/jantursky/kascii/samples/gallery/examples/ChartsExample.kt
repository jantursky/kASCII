package com.jantursky.kascii.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.components.AsciiBarChart
import com.jantursky.kascii.components.AsciiCandlesticks
import com.jantursky.kascii.components.AsciiDivider
import com.jantursky.kascii.components.AsciiHeatmap
import com.jantursky.kascii.components.AsciiLineChart
import com.jantursky.kascii.components.AsciiPieChart
import com.jantursky.kascii.components.AsciiPieSlice
import com.jantursky.kascii.components.AsciiRadar
import com.jantursky.kascii.components.AsciiSparkline
import com.jantursky.kascii.components.Candle

private val Weekly = listOf(3f, 5f, 4f, 8f, 6f, 9f, 7f)
private val Days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val Series = listOf(2f, 4f, 3f, 6f, 5f, 8f, 7f, 9f, 6f, 8f, 10f, 9f)

// Charts take plain lists of numbers and draw them once. Every one describes its data to a screen reader.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartsExample(modifier: Modifier = Modifier) {
    FlowRow(modifier, Arrangement.spacedBy(32.dp), Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AsciiDivider(cols = 30, label = "Bars")
            AsciiBarChart(Weekly, labels = Days, rows = 6)
            AsciiDivider(cols = 30, label = "Sparkline")
            AsciiSparkline(Series)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AsciiDivider(cols = 30, label = "Line")
            AsciiLineChart(Series, cols = 30, rows = 6)
            AsciiDivider(cols = 30, label = "Candlesticks")
            AsciiCandlesticks(
                listOf(Candle(4f, 6f, 3f, 5f), Candle(5f, 8f, 5f, 7f), Candle(7f, 9f, 4f, 5f), Candle(5f, 7f, 4f, 6f)),
                rows = 8,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AsciiDivider(cols = 30, label = "Pie")
            AsciiPieChart(listOf(AsciiPieSlice("Kotlin", 6f), AsciiPieSlice("Swift", 3f), AsciiPieSlice("JS", 1f)))
            AsciiDivider(cols = 30, label = "Heatmap")
            AsciiHeatmap(
                listOf(listOf(0f, 0.3f, 0.6f, 1f), listOf(1f, 0.6f, 0.3f, 0f), listOf(0.2f, 0.9f, 0.4f, 0.7f)),
                rowLabels = listOf("a", "b", "c"),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AsciiDivider(cols = 30, label = "Radar")
            AsciiRadar(listOf(0.9f, 0.6f, 0.8f, 0.4f, 0.7f), listOf("Speed", "Size", "Reach", "Cost", "Fun"))
        }
    }
}
