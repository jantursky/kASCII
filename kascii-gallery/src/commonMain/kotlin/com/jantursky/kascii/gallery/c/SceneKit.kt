package com.jantursky.kascii.gallery

import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.Rgb
import kotlin.math.floor
import kotlin.math.roundToInt

/** Small drawing helpers shared by the scenes in this directory. */
internal object SceneKit {
    /** In sprite art, this glyph is an opaque blank: it clears what lies behind, where a space leaves it. */
    const val SOLID: Char = '&'

    /**
     * Draws [lines] with their top-left corner at [x], [y]. Spaces are transparent and [SOLID] clears the cell, so
     * a sprite can hide what is behind its body. [tint] colors single glyphs; the rest take [color].
     */
    fun stamp(
        grid: Grid,
        x: Int,
        y: Int,
        lines: List<String>,
        color: Rgb? = null,
        tint: Map<Char, Rgb> = emptyMap(),
    ) {
        lines.forEachIndexed { row, line ->
            line.forEachIndexed { col, ch ->
                when (ch) {
                    ' ' -> {}

                    SOLID -> {
                        grid.put(x + col, y + row, ' ')
                    }

                    else -> {
                        grid.put(x + col, y + row, ch, tint[ch] ?: color)
                    }
                }
            }
        }
    }

    /**
     * The time [t] snapped to its frame at [fps] and wrapped into the loop of [loop] seconds. It is computed from the
     * whole frame number, so a time and the same time one loop later give the very same value and the loop closes
     * exactly, with no rounding drift between them.
     */
    fun loopTime(
        t: Float,
        fps: Int,
        loop: Float,
    ): Float {
        val frames = (loop * fps).roundToInt()
        return floor(t * fps + 1e-3f).toInt().mod(frames).toFloat() / fps
    }

    /** [v] wrapped into 0 until [period]. */
    fun wrap(
        v: Float,
        period: Float,
    ): Float = v - floor(v / period) * period
}
