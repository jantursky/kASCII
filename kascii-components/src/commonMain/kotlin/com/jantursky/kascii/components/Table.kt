package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.jantursky.kascii.core.BoxStyle
import com.jantursky.kascii.core.Grid

/** How a table cell sits in its column. */
public enum class AsciiAlign {
    /** Flush left. */
    Start,

    /** Centered. */
    Center,

    /** Flush right, for numbers. */
    End,
}

/**
 * One table column.
 *
 * @property header Text of the header cell.
 * @property align How the cells of this column line up.
 */
public class AsciiTableColumn(
    public val header: String,
    public val align: AsciiAlign = AsciiAlign.Start,
)

/**
 * A table with a header row, drawn with box characters. Every row of [rows] needs one cell per column. A screen
 * reader gets the whole table as text, plus its row and column count.
 */
@Composable
public fun AsciiTable(
    columns: List<AsciiTableColumn>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier,
    border: BoxStyle = BoxStyle.Single,
) {
    val description = describeTable(columns, rows, LocalAsciiStrings.current)
    GridSurface(
        grid = renderTable(columns, rows, border),
        modifier =
            modifier.semantics {
                collectionInfo = CollectionInfo(rowCount = rows.size + 1, columnCount = columns.size)
                contentDescription = description
            },
        description = description,
    )
}

internal fun renderTable(
    columns: List<AsciiTableColumn>,
    rows: List<List<String>>,
    border: BoxStyle,
): Grid {
    val widths = columns.mapIndexed { i, c -> maxOf(c.header.length, rows.maxOfOrNull { it[i].length } ?: 0) }
    val j = junctions(border)
    val bar = border.vertical.toString()

    fun rule(
        left: Char,
        mid: Char,
        right: Char,
    ) = widths.joinToString(mid.toString(), left.toString(), right.toString()) {
        border.horizontal.toString().repeat(it + 2)
    }

    fun row(cells: List<String>) =
        cells.indices.joinToString(bar, bar, bar) { " " + align(cells[it], widths[it], columns[it].align) + " " }
    val lines =
        buildList {
            add(rule(border.topLeft, j.top, border.topRight))
            add(row(columns.map { it.header }))
            add(rule(j.left, j.cross, j.right))
            rows.forEach { add(row(it)) }
            add(rule(border.bottomLeft, j.bottom, border.bottomRight))
        }
    return gridOfLines(lines)
}

internal fun describeTable(
    columns: List<AsciiTableColumn>,
    rows: List<List<String>>,
    strings: AsciiStrings = AsciiStrings(),
): String =
    "${strings.table}, ${columns.size} ${strings.columns}: ${columns.joinToString { it.header }}. " +
        rows.mapIndexed { i, cells -> "${strings.row} ${i + 1}: ${cells.joinToString()}" }.joinToString(". ")

private fun align(
    text: String,
    width: Int,
    align: AsciiAlign,
): String =
    when (align) {
        AsciiAlign.Start -> text.padEnd(width)
        AsciiAlign.End -> text.padStart(width)
        AsciiAlign.Center -> text.centered(width)
    }

private class Junctions(
    val top: Char,
    val bottom: Char,
    val left: Char,
    val right: Char,
    val cross: Char,
)

private fun junctions(border: BoxStyle): Junctions =
    when (border.horizontal) {
        '═' -> Junctions('╦', '╩', '╠', '╣', '╬')
        '━' -> Junctions('┳', '┻', '┣', '┫', '╋')
        '-' -> Junctions('+', '+', '+', '+', '+')
        else -> Junctions('┬', '┴', '├', '┤', '┼')
    }
