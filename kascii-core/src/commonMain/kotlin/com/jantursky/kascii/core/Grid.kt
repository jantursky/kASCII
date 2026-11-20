package com.jantursky.kascii.core

/**
 * A grid of glyph cells. Every renderer and exporter reads one. [colors] holds an ARGB value per cell, or 0
 * for "use the theme ink".
 */
public class Grid(
    /** Number of columns. */
    public val cols: Int,
    /** Number of rows. */
    public val rows: Int,
) {
    init {
        require(cols >= 0 && rows >= 0) { "Grid size must not be negative" }
    }

    /** One UTF-16 glyph per cell, row major. */
    public val glyphs: CharArray = CharArray(cols * rows) { ' ' }

    /** One ARGB color per cell, row major. 0 means the theme ink. */
    public val colors: IntArray = IntArray(cols * rows)

    /** True when [x], [y] is inside the grid. */
    public fun contains(
        x: Int,
        y: Int,
    ): Boolean = x in 0 until cols && y in 0 until rows

    /** Writes one glyph, ignoring positions outside the grid. */
    public fun put(
        x: Int,
        y: Int,
        glyph: Char,
        color: Rgb? = null,
    ) {
        if (!contains(x, y)) return
        val i = y * cols + x
        glyphs[i] = glyph
        colors[i] = color?.argb ?: 0
    }

    /** Writes [text] from [x], [y] to the right, clipped at the grid edge. */
    public fun write(
        x: Int,
        y: Int,
        text: String,
        color: Rgb? = null,
    ) {
        for (k in text.indices) put(x + k, y, text[k], color)
    }

    /** Fills a rectangle with [glyph]. */
    public fun fill(
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        glyph: Char = ' ',
        color: Rgb? = null,
    ) {
        for (j in y until y + h) for (i in x until x + w) put(i, j, glyph, color)
    }

    /** Resets every cell to a space with the theme ink. */
    public fun clear() {
        glyphs.fill(' ')
        colors.fill(0)
    }

    /** The glyph at [x], [y], or a space outside the grid. */
    public fun glyphAt(
        x: Int,
        y: Int,
    ): Char = if (contains(x, y)) glyphs[y * cols + x] else ' '

    /** The explicit color of a cell, or null when it uses the theme ink. */
    public fun colorAt(
        x: Int,
        y: Int,
    ): Rgb? = if (contains(x, y) && colors[y * cols + x] != 0) Rgb(colors[y * cols + x]) else null

    /** Plain text, one line per row, no trailing newline. Trailing spaces are kept. */
    public fun toText(): String =
        buildString {
            for (y in 0 until rows) {
                if (y > 0) append('\n')
                appendRange(glyphs, y * cols, (y + 1) * cols)
            }
        }

    /** Copies [other] into this grid with its top-left corner at [x], [y]. Spaces are copied too. */
    public fun blit(
        other: Grid,
        x: Int,
        y: Int,
    ) {
        for (j in 0 until other.rows) {
            for (i in 0 until other.cols) {
                val k = j * other.cols + i
                put(x + i, y + j, other.glyphs[k], if (other.colors[k] != 0) Rgb(other.colors[k]) else null)
            }
        }
    }
}
