package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.Rgb

/**
 * [text] in a large block font. Letters are upper cased. Characters the font lacks are drawn as `?`. A screen
 * reader gets the plain text.
 */
@Composable
public fun AsciiBigText(
    text: String,
    modifier: Modifier = Modifier,
    font: AsciiBigFont = AsciiBigFont.Tall,
) {
    GridSurface(renderBigText(text, font), modifier, text)
}

/** Draws [text] glyph by glyph with one blank column between glyphs. */
internal fun renderBigText(text: String, font: AsciiBigFont, color: Rgb? = null): Grid {
    val table = font.glyphs()
    val glyphs = text.map { table[it.uppercaseChar()] ?: table.getValue('?') }
    val grid = Grid((glyphs.sumOf { it[0].length } + glyphs.size - 1).coerceAtLeast(0), font.rows)
    var x = 0
    for (glyph in glyphs) {
        glyph.forEachIndexed { y, row -> grid.write(x, y, row, color) }
        x += glyph[0].length + 1
    }
    return grid
}
