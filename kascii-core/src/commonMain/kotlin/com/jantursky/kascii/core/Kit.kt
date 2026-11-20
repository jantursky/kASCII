package com.jantursky.kascii.core

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * What a piece says about itself. [fps] 0 marks a still. [still] is the time shown under reduced motion, [loop]
 * the seconds after which the animation repeats (0 for never).
 */
public class PieceMeta(
    public val name: String,
    public val category: String,
    public val cols: Int,
    public val rows: Int,
    public val fps: Int = 30,
    public val still: Float = 0f,
    public val loop: Float = 0f,
    public val note: String = "",
)

/** A piece draws the frame for time `t` seconds into a cleared grid. Build one with [asciiPiece]. */
public class AsciiPiece(public val meta: PieceMeta, public val draw: (t: Float, grid: Grid) -> Unit) {
    /** Renders one frame at [t] into a fresh grid of the piece's own size. */
    public fun render(t: Float = meta.still): Grid = Grid(meta.cols, meta.rows).also { draw(t, it) }
}

/** Defines a piece: size, timing and a draw function. */
public fun asciiPiece(
    name: String,
    category: String,
    cols: Int,
    rows: Int,
    fps: Int = 30,
    still: Float = 0f,
    loop: Float = 0f,
    note: String = "",
    draw: (t: Float, grid: Grid) -> Unit,
): AsciiPiece = AsciiPiece(PieceMeta(name, category, cols, rows, fps, still, loop, note), draw)

/** Draws a straight line of [glyph] between two cells (Bresenham). */
public fun Grid.line(x0: Int, y0: Int, x1: Int, y1: Int, glyph: Char = '#', color: Rgb? = null) {
    var x = x0
    var y = y0
    val dx = abs(x1 - x0)
    val dy = -abs(y1 - y0)
    val sx = if (x0 < x1) 1 else -1
    val sy = if (y0 < y1) 1 else -1
    var err = dx + dy
    while (true) {
        put(x, y, glyph, color)
        if (x == x1 && y == y1) break
        val e2 = 2 * err
        if (e2 >= dy) { err += dy; x += sx }
        if (e2 <= dx) { err += dx; y += sy }
    }
}

/** Draws a rectangle outline with the given [border] characters, or a filled one when [fillGlyph] is set. */
public fun Grid.rect(x: Int, y: Int, w: Int, h: Int, border: BoxStyle? = BoxStyle.Single, fillGlyph: Char? = null, color: Rgb? = null) {
    if (w <= 0 || h <= 0) return
    if (fillGlyph != null) fill(x, y, w, h, fillGlyph, color)
    if (border == null) return
    for (i in 1 until w - 1) { put(x + i, y, border.horizontal, color); put(x + i, y + h - 1, border.horizontal, color) }
    for (j in 1 until h - 1) { put(x, y + j, border.vertical, color); put(x + w - 1, y + j, border.vertical, color) }
    put(x, y, border.topLeft, color)
    put(x + w - 1, y, border.topRight, color)
    put(x, y + h - 1, border.bottomLeft, color)
    put(x + w - 1, y + h - 1, border.bottomRight, color)
}

/** A filled disc of [glyph] centred on a cell. [cellAspect] is cell height over width so it looks round. */
public fun Grid.disc(cx: Float, cy: Float, radius: Float, glyph: Char = '@', color: Rgb? = null, cellAspect: Float = 2f) {
    val ry = radius / cellAspect
    for (y in (cy - ry).toInt() - 1..(cy + ry).toInt() + 1) for (x in (cx - radius).toInt() - 1..(cx + radius).toInt() + 1) {
        val dx = (x + 0.5f - cx) / radius
        val dy = (y + 0.5f - cy) / ry
        if (dx * dx + dy * dy <= 1f) put(x, y, glyph, color)
    }
}

/** Writes [text] centred on column [cx] of row [y]. */
public fun Grid.label(cx: Int, y: Int, text: String, color: Rgb? = null) {
    write(cx - text.length / 2, y, text, color)
}

/** Border characters for frames. */
public class BoxStyle(
    public val topLeft: Char,
    public val topRight: Char,
    public val bottomLeft: Char,
    public val bottomRight: Char,
    public val horizontal: Char,
    public val vertical: Char,
) {
    public companion object {
        public val Single: BoxStyle = BoxStyle('┌', '┐', '└', '┘', '─', '│')
        public val Double: BoxStyle = BoxStyle('╔', '╗', '╚', '╝', '═', '║')
        public val Rounded: BoxStyle = BoxStyle('╭', '╮', '╰', '╯', '─', '│')
        public val Heavy: BoxStyle = BoxStyle('┏', '┓', '┗', '┛', '━', '┃')
        public val Ascii: BoxStyle = BoxStyle('+', '+', '+', '+', '-', '|')
    }
}

/**
 * A sub-cell canvas: each cell holds 2x4 dots, drawn as Braille. [dotWidth] is `cols * 2`, [dotHeight] `rows * 4`.
 */
public class BrailleCanvas(public val cols: Int, public val rows: Int) {
    public val dotWidth: Int = cols * 2
    public val dotHeight: Int = rows * 4
    private val masks = IntArray(cols * rows)
    private val bits = arrayOf(intArrayOf(0x01, 0x08), intArrayOf(0x02, 0x10), intArrayOf(0x04, 0x20), intArrayOf(0x40, 0x80))

    /** Lights one dot. Positions outside the canvas are ignored. */
    public fun plot(x: Int, y: Int) {
        if (x !in 0 until dotWidth || y !in 0 until dotHeight) return
        masks[(y / 4) * cols + x / 2] = masks[(y / 4) * cols + x / 2] or bits[y % 4][x % 2]
    }

    /** Lights the dots of a line between two dot positions. */
    public fun line(x0: Int, y0: Int, x1: Int, y1: Int) {
        val steps = maxOf(abs(x1 - x0), abs(y1 - y0), 1)
        for (s in 0..steps) plot(x0 + ((x1 - x0) * s / steps.toFloat()).roundToInt(), y0 + ((y1 - y0) * s / steps.toFloat()).roundToInt())
    }

    /** Writes the lit cells into [grid] at [x],[y]. Empty cells are left alone. */
    public fun drawTo(grid: Grid, x: Int, y: Int, color: Rgb? = null) {
        for (r in 0 until rows) for (c in 0 until cols) {
            val m = masks[r * cols + c]
            if (m != 0) grid.put(x + c, y + r, (0x2800 + m).toChar(), color)
        }
    }
}
