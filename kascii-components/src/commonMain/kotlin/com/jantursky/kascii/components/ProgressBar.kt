package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.compose.rememberReducedMotion
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import kotlin.math.abs

/** How the filled part of a progress bar is drawn. */
public enum class AsciiBarStyle(
    internal val full: Char,
    internal val empty: Char,
    internal val partials: String,
) {
    /** Solid blocks on a shaded track. */
    Blocks('█', '░', ""),

    /** Plain `#` on `-`, safe on any terminal font. */
    Hash('#', '-', ""),

    /** Blocks with eighth-cell edges, for fine steps. */
    Smooth('█', '·', "▏▎▍▌▋▊▉"),
}

/**
 * A progress bar [cols] cells wide. [value] is 0..1, or null for an indeterminate bar whose chunk bounces back
 * and forth (it rests at the start under reduced motion).
 */
@Composable
public fun AsciiProgressBar(
    value: Float?,
    modifier: Modifier = Modifier,
    cols: Int = 24,
    style: AsciiBarStyle = AsciiBarStyle.Blocks,
    showPercent: Boolean = true,
    label: String = "Progress",
) {
    val theme = LocalKasciiTheme.current
    val time = if (value == null) rememberClock(BAR_FPS, rememberReducedMotion()).floatValue else 0f
    GridSurface(
        grid = renderProgressBar(cols, value, style, showPercent, time, theme),
        modifier =
            modifier.semantics(mergeDescendants = true) {
                contentDescription = label
                progressBarRangeInfo = value?.let { ProgressBarRangeInfo(it.coerceIn(0f, 1f), 0f..1f) }
                    ?: ProgressBarRangeInfo.Indeterminate
                value?.let { stateDescription = "${percentOf(it)}%" }
            },
    )
}

internal fun renderProgressBar(
    cols: Int,
    value: Float?,
    style: AsciiBarStyle,
    showPercent: Boolean,
    t: Float,
    theme: KasciiTheme,
): Grid {
    val inner = (cols - 2 - if (showPercent) PERCENT_WIDTH else 0).coerceAtLeast(1)
    val grid = Grid(cols, 1)
    grid.put(0, 0, '[', theme.dim())
    grid.put(inner + 1, 0, ']', theme.dim())
    grid.fill(1, 0, inner, 1, style.empty, theme.dim())
    if (value == null) {
        val chunk = maxOf(2, inner / 4)
        val start = (bounce(t * BOUNCE_SPEED) * (inner - chunk)).toInt().coerceAtLeast(0)
        grid.fill(1 + start, 0, minOf(chunk, inner), 1, style.full, theme.good())
    } else {
        val eighths = (value.coerceIn(0f, 1f) * inner * 8).toInt()
        grid.fill(1, 0, eighths / 8, 1, style.full, theme.good())
        val rest = eighths % 8
        if (rest > 0 && style.partials.isNotEmpty()) {
            grid.put(1 + eighths / 8, 0, style.partials[rest - 1], theme.good())
        }
        if (showPercent) grid.write(inner + 2, 0, "${percentOf(value)}%".padStart(PERCENT_WIDTH))
    }
    return grid
}

/** 0 to 1 and back, once per second at speed 1. */
private fun bounce(x: Float): Float = 1f - abs(2f * (x - kotlin.math.floor(x)) - 1f)

private const val BAR_FPS = 15
private const val BOUNCE_SPEED = 0.8f
private const val PERCENT_WIDTH = 5
