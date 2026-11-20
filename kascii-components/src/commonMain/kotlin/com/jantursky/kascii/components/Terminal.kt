package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.compose.rememberReducedMotion
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme

/**
 * A terminal window of [rows] lines, [cols] cells wide. [lines] is the scrolling log: only the newest ones fit.
 * [typing] is a command typed after the log at [charsPerSecond], followed by a blinking cursor. Under reduced
 * motion the command is complete and the cursor stays solid.
 */
@Composable
public fun AsciiTerminal(
    lines: List<String>,
    modifier: Modifier = Modifier,
    cols: Int = 60,
    rows: Int = 12,
    prompt: String = "$ ",
    typing: String = "",
    charsPerSecond: Float = 12f,
) {
    val reduced = rememberReducedMotion()
    val time = rememberClock(TERMINAL_FPS, reduced).floatValue
    val typed = if (reduced) typing.length else (time * charsPerSecond).toInt()
    val cursorOn = reduced || (time * CURSOR_HZ).toInt() % 2 == 0
    GridSurface(
        grid = renderTerminal(lines, cols, rows, prompt, typing.take(typed), cursorOn, LocalKasciiTheme.current),
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        description = (lines + (prompt + typing)).takeLast(rows).joinToString("\n"),
    )
}

internal fun renderTerminal(
    lines: List<String>,
    cols: Int,
    rows: Int,
    prompt: String,
    input: String,
    cursorOn: Boolean,
    theme: KasciiTheme,
): Grid {
    val log = lines.flatMap { it.chunked(cols).ifEmpty { listOf("") } }
    val current = (prompt + input + if (cursorOn) "█" else "").chunked(cols)
    val visible = (log + current).takeLast(rows)
    val grid = Grid(cols, rows)
    visible.forEachIndexed { y, line -> grid.write(0, y, line) }
    val promptRow = visible.size - current.size
    if (promptRow >= 0) grid.write(0, promptRow, prompt.take(cols), theme.good())
    return grid
}

private const val TERMINAL_FPS = 20
private const val CURSOR_HZ = 2
