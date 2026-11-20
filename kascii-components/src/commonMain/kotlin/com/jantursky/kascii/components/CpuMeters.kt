package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme

/** Per-core load meters in the style of htop: `0 [|||||    ] 45%`. [loads] are 0..1, one per core. */
@Composable
public fun AsciiCpuMeters(
    loads: List<Float>,
    modifier: Modifier = Modifier,
    cols: Int = 32,
) {
    val description = "CPU load: " + loads.mapIndexed { i, l -> "core $i ${percentOf(l)}%" }.joinToString(", ")
    GridSurface(renderCpuMeters(loads, cols, LocalKasciiTheme.current), modifier, description)
}

internal fun renderCpuMeters(
    loads: List<Float>,
    cols: Int,
    theme: KasciiTheme,
): Grid {
    val grid = Grid(cols, loads.size)
    val inner = (cols - METER_CHROME).coerceAtLeast(1)
    loads.forEachIndexed { y, load ->
        val color =
            when {
                load < MEDIUM_LOAD -> theme.good()
                load < HIGH_LOAD -> theme.warn()
                else -> theme.bad()
            }
        grid.write(0, y, y.toString().padStart(2) + " [", theme.dim())
        grid.fill(4, y, (load.coerceIn(0f, 1f) * inner).toInt(), 1, '|', color)
        grid.write(4 + inner, y, "] " + "${percentOf(load)}%".padStart(4))
    }
    return grid
}

/** Core label, brackets and the percentage. */
private const val METER_CHROME = 10
private const val MEDIUM_LOAD = 0.5f
private const val HIGH_LOAD = 0.8f
