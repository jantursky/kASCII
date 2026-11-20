package com.jantursky.kascii.core

/** Text, ANSI and SVG output of a [Grid]. PNG lives in the Compose module because it needs a bitmap. */
public object Export {
    /** Plain text, one line per row. [trimEnd] drops trailing spaces from each line. */
    public fun toText(
        grid: Grid,
        trimEnd: Boolean = false,
    ): String {
        val text = grid.toText()
        return if (trimEnd) text.split('\n').joinToString("\n") { it.trimEnd() } else text
    }

    /**
     * ANSI escape text. With [truecolor] every colored run uses a 24-bit foreground. Otherwise it snaps to the
     * nearest of the 16 basic colors. Cells without a color use the default foreground.
     */
    public fun toAnsi(
        grid: Grid,
        truecolor: Boolean = true,
    ): String =
        buildString {
            for (y in 0 until grid.rows) {
                var current = 0
                for (x in 0 until grid.cols) {
                    val color = grid.colors[y * grid.cols + x]
                    if (color != current) {
                        append(
                            if (color == 0) {
                                "\u001b[39m"
                            } else if (truecolor) {
                                ansi24(Rgb(color))
                            } else {
                                ansi16(Rgb(color))
                            },
                        )
                        current = color
                    }
                    append(grid.glyphs[y * grid.cols + x])
                }
                if (current != 0) append("\u001b[39m")
                if (y < grid.rows - 1) append('\n')
            }
        }

    /**
     * An SVG with one `<text>` element per row, drawn on [theme]'s paper. Characters are placed on a fixed
     * [cellWidth] by [cellHeight] grid so the layout does not depend on font metrics.
     */
    public fun toSvg(
        grid: Grid,
        theme: KasciiTheme = KasciiTheme.Dark,
        cellWidth: Float = 8f,
        cellHeight: Float = 16f,
        fontFamily: String = "'JetBrains Mono', ui-monospace, Menlo, Consolas, monospace",
    ): String =
        buildString {
            val w = grid.cols * cellWidth
            val h = grid.rows * cellHeight
            append(
                """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${num(
                    w,
                )} ${num(h)}" width="${num(w)}" height="${num(h)}">""",
            )
            append("""<rect width="100%" height="100%" fill="${theme.paper.toHex()}"/>""")
            append(
                """<g font-family="${escape(
                    fontFamily,
                )}" font-size="${num(cellHeight * 0.8f)}" fill="${theme.ink.toHex()}" xml:space="preserve">""",
            )
            for (y in 0 until grid.rows) {
                var x = 0
                while (x < grid.cols) {
                    val color = grid.colors[y * grid.cols + x]
                    var end = x
                    while (end < grid.cols && grid.colors[y * grid.cols + end] == color) end++
                    val run = grid.glyphs.concatToString(y * grid.cols + x, y * grid.cols + end).trimEnd()
                    if (run.isNotBlank()) {
                        val fill = if (color == 0) "" else """ fill="${Rgb(color).toHex()}""""
                        append(
                            """<text x="${num(
                                x * cellWidth,
                            )}" y="${num(
                                (y + 0.8f) * cellHeight,
                            )}" textLength="${num(run.length * cellWidth)}"$fill>${escape(run)}</text>""",
                        )
                    }
                    x = end
                }
            }
            append("</g></svg>")
        }

    private fun num(v: Float): String = ((v * 100).toInt() / 100f).toString().removeSuffix(".0")

    private fun escape(s: String): String =
        s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

    private fun ansi24(c: Rgb) = "\u001b[38;2;${c.red};${c.green};${c.blue}m"

    private val basic =
        listOf(
            0 to Rgb(0, 0, 0),
            1 to Rgb(205, 0, 0),
            2 to Rgb(0, 205, 0),
            3 to Rgb(205, 205, 0),
            4 to Rgb(0, 0, 238),
            5 to Rgb(205, 0, 205),
            6 to Rgb(0, 205, 205),
            7 to Rgb(229, 229, 229),
            8 to Rgb(127, 127, 127),
            9 to Rgb(255, 0, 0),
            10 to Rgb(0, 255, 0),
            11 to Rgb(255, 255, 0),
            12 to Rgb(92, 92, 255),
            13 to Rgb(255, 0, 255),
            14 to Rgb(0, 255, 255),
            15 to Rgb(255, 255, 255),
        )

    private fun ansi16(c: Rgb): String {
        val best =
            basic
                .minBy { (_, p) ->
                    val dr = p.red - c.red
                    val dg = p.green - c.green
                    val db = p.blue - c.blue
                    dr * dr + dg * dg + db * db
                }.first
        return "\u001b[${if (best < 8) 30 + best else 90 + best - 8}m"
    }
}
