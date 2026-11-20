package com.jantursky.kascii.components

import com.jantursky.kascii.core.Grid
import kotlin.test.assertEquals

/** Compares [grid] with [rows], each row written between `|` marks so trailing spaces stay visible. */
internal fun assertGrid(grid: Grid, vararg rows: String) {
    assertEquals(rows.toList(), grid.toText().lines().map { "|$it|" })
}
