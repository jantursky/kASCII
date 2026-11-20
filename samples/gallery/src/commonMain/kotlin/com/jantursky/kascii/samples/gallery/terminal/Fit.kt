package com.jantursky.kascii.samples.gallery.terminal

/** The width of one glyph cell of the components, in dp. */
private const val CELL_DP = 8f
private const val MIN_COLS = 20

/** How many columns fit in [widthDp], at most [max]: components are drawn in whole columns. */
fun fitCols(
    widthDp: Float,
    max: Int,
): Int = (widthDp / CELL_DP).toInt().coerceIn(MIN_COLS, max)
