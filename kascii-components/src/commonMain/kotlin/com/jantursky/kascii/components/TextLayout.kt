package com.jantursky.kascii.components

import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.Rgb
import kotlin.math.roundToInt

/** Builds a grid as wide as the longest of [lines]. */
internal fun gridOfLines(lines: List<String>, color: Rgb? = null): Grid {
    val grid = Grid(lines.maxOfOrNull { it.length } ?: 0, lines.size)
    lines.forEachIndexed { y, line -> grid.write(0, y, line, color) }
    return grid
}

/** Greedy word wrap. Honors `\n`, and breaks words longer than [width] hard. */
internal fun wrapText(text: String, width: Int): List<String> {
    require(width > 0) { "width must be positive" }
    val out = mutableListOf<String>()
    for (paragraph in text.split('\n')) {
        var line = ""
        for (word in paragraph.split(' ').filter { it.isNotEmpty() }) {
            var rest = word
            while (rest.length > width) {
                if (line.isNotEmpty()) {
                    out += line
                    line = ""
                }
                out += rest.take(width)
                rest = rest.drop(width)
            }
            line = when {
                line.isEmpty() -> rest
                line.length + 1 + rest.length <= width -> "$line $rest"
                else -> {
                    out += line
                    rest
                }
            }
        }
        out += line
    }
    return out
}

internal fun String.centered(width: Int): String {
    val gap = (width - length).coerceAtLeast(0)
    return " ".repeat(gap / 2) + this + " ".repeat(gap - gap / 2)
}

/** `3` for whole numbers, `3.5` otherwise. */
internal fun Float.pretty(): String {
    val tenths = (this * 10).roundToInt()
    return if (tenths % 10 == 0) (tenths / 10).toString() else "${tenths / 10}.${kotlin.math.abs(tenths % 10)}"
}

internal fun percentOf(fraction: Float): Int = (fraction.coerceIn(0f, 1f) * 100).roundToInt()
