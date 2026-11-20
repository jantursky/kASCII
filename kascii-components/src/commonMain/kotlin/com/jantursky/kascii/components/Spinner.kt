package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.compose.rememberReducedMotion
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme

/** Classic spinner frame sets. Every frame of a set has the same width. */
public enum class AsciiSpinnerStyle(
    internal val frames: List<String>,
    internal val fps: Int,
) {
    /** Braille dots running around a cell. */
    BrailleDots("⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏".toFrames(), 12),

    /** The `|/-\` line, safe on any font. */
    Line("|/-\\".toFrames(), 10),

    /** A quarter block turning around the cell. */
    Arc("▘▝▗▖".toFrames(), 10),

    /** A dot bouncing between the cell edges. */
    Bounce("⠁⠂⠄⡀⢀⠠⠐⠈".toFrames(), 10),

    /** A half block turning. */
    Clock("▀▐▄▌".toFrames(), 6),

    /** A circle filling and emptying. */
    Moon("○◔◕●◕◔".toFrames(), 6),

    /** Arrows turning clockwise. */
    Arrows("←↖↑↗→↘↓↙".toFrames(), 10),

    /** A bar growing and shrinking. */
    Grow("▁▃▄▅▆▇▆▅▄▃".toFrames(), 12),

    /** A square switching between filled and hollow. */
    Toggle("■□".toFrames(), 3),

    /** A block running around the quadrants of a cell. */
    Box("▖▘▝▗".toFrames(), 8),

    /** Three dots filling up and emptying, plain ASCII. */
    Dots(listOf(".  ", ".. ", "...", " ..", "  .", "   "), 6),

    /** A dot swelling and fading. */
    Pulse("·•●•".toFrames(), 6),
}

private fun String.toFrames(): List<String> = map { it.toString() }

/**
 * An indeterminate spinner, optionally followed by a [label]. Under reduced motion it shows its first frame. The
 * accessible name is [label], or "Loading".
 */
@Composable
public fun AsciiSpinner(
    modifier: Modifier = Modifier,
    style: AsciiSpinnerStyle = AsciiSpinnerStyle.BrailleDots,
    label: String? = null,
) {
    val loading = LocalAsciiStrings.current.loading
    val theme = LocalKasciiTheme.current
    val time = rememberClock(style.fps, rememberReducedMotion()).floatValue
    GridSurface(
        grid = renderSpinner(style, time, label, theme),
        modifier =
            modifier.semantics(mergeDescendants = true) {
                contentDescription = label ?: loading
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            },
    )
}

internal fun renderSpinner(
    style: AsciiSpinnerStyle,
    t: Float,
    label: String?,
    theme: KasciiTheme,
): Grid {
    val frame = style.frames[(t * style.fps).toInt() % style.frames.size]
    val grid = Grid(frame.length + (label?.let { it.length + 1 } ?: 0), 1)
    grid.write(0, 0, frame, theme.accent)
    label?.let { grid.write(frame.length + 1, 0, it) }
    return grid
}
