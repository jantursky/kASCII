package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.compose.rememberReducedMotion
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme

/** How a boot step ended. */
public enum class AsciiBootStatus(
    internal val tag: String,
    internal val tone: AsciiTone,
) {
    /** The step succeeded. */
    Ok("  OK  ", AsciiTone.Success),

    /** The step finished with a warning. */
    Warn(" WARN ", AsciiTone.Warning),

    /** The step failed. */
    Fail(" FAIL ", AsciiTone.Error),
}

/**
 * One line of a boot log.
 *
 * @property message What was started or checked.
 * @property status How it ended.
 */
public class AsciiBootEntry(
    public val message: String,
    public val status: AsciiBootStatus = AsciiBootStatus.Ok,
)

/**
 * A boot log in the style of a Linux init: `[  OK  ] message`. Lines appear one after the other at
 * [linesPerSecond]; the box keeps its full height so the layout does not jump. Under reduced motion every line is
 * shown at once.
 */
@Composable
public fun AsciiBootLog(
    entries: List<AsciiBootEntry>,
    modifier: Modifier = Modifier,
    cols: Int = 48,
    linesPerSecond: Float = 6f,
) {
    val reduced = rememberReducedMotion()
    val time = rememberClock(BOOT_FPS, reduced, endTime = entries.size / linesPerSecond).floatValue
    val visible = if (reduced) entries.size else (time * linesPerSecond).toInt()
    GridSurface(
        grid = renderBootLog(entries, visible, cols, LocalKasciiTheme.current),
        modifier = modifier,
        description = entries.joinToString(". ") { "${it.message}: ${it.status.name.lowercase()}" },
    )
}

internal fun renderBootLog(
    entries: List<AsciiBootEntry>,
    visible: Int,
    cols: Int,
    theme: KasciiTheme,
): Grid {
    val grid = Grid(cols, entries.size)
    entries.take(visible).forEachIndexed { y, entry ->
        grid.write(0, y, "[${entry.status.tag}]", entry.status.tone.color(theme))
        grid.write(TAG_WIDTH + 1, y, entry.message.take(cols - TAG_WIDTH - 1))
    }
    return grid
}

private const val BOOT_FPS = 20
private const val TAG_WIDTH = 8
