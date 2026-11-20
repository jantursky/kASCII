package com.jantursky.kascii.core

/** Turns a [Field] into a [Grid] of glyphs. Pure and deterministic. */
public object Rasterizer {
    /**
     * Samples [field] at time [t] over a [cols] by [rows] grid and maps the tones through [charset].
     * [cellAspect] is cell height over cell width in pixels (about 2 for monospace fonts) and keeps fields round.
     */
    public fun rasterize(
        field: Field,
        charset: Charset,
        cols: Int,
        rows: Int,
        t: Float = 0f,
        cellAspect: Float = 2f,
        colorMode: ColorMode = ColorMode.Theme,
        theme: KasciiTheme = KasciiTheme.Dark,
        into: Grid = Grid(cols, rows),
    ): Grid {
        require(into.cols == cols && into.rows == rows) { "Target grid has the wrong size" }
        val aspect = if (rows == 0) 1f else cols / (rows * cellAspect)
        val sw = cols * charset.subX
        val sh = rows * charset.subY
        val tones = FloatArray(sw * sh)
        for (sy in 0 until sh) {
            for (sx in 0 until sw) {
                tones[sy * sw + sx] = field.tone((sx + 0.5f) / sw, (sy + 0.5f) / sh, t, aspect).coerceIn(0f, 1f)
            }
        }
        return fromTones(tones, charset, cols, rows, t, colorMode, theme, into)
    }

    /** Maps an already sampled tone grid (`cols*subX` by `rows*subY`, row major) into glyphs. */
    public fun fromTones(
        tones: FloatArray,
        charset: Charset,
        cols: Int,
        rows: Int,
        t: Float = 0f,
        colorMode: ColorMode = ColorMode.Theme,
        theme: KasciiTheme = KasciiTheme.Dark,
        into: Grid = Grid(cols, rows),
    ): Grid {
        val sw = cols * charset.subX
        require(tones.size == sw * rows * charset.subY) { "Tone count does not match the grid" }
        val cell = FloatArray(charset.subX * charset.subY)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                var mean = 0f
                for (y in 0 until charset.subY) {
                    for (x in 0 until charset.subX) {
                        val v = tones[(r * charset.subY + y) * sw + c * charset.subX + x]
                        cell[y * charset.subX + x] = v
                        mean += v
                    }
                }
                mean /= cell.size
                val glyph =
                    when (charset) {
                        is Charset.Ramp -> charset.glyphFor(mean)
                        is Charset.Mask -> charset.glyphFor(cell)
                        is Charset.Edge -> edgeGlyph(charset, tones, sw, cols, rows, c, r)
                    }
                val color = colorMode.resolve(theme, mean, (c + 0.5f) / cols, (r + 0.5f) / rows, t)
                into.put(c, r, glyph, color)
            }
        }
        return into
    }

    private fun edgeGlyph(
        e: Charset.Edge,
        tones: FloatArray,
        w: Int,
        cols: Int,
        rows: Int,
        c: Int,
        r: Int,
    ): Char {
        fun at(
            x: Int,
            y: Int,
        ) = tones[y.coerceIn(0, rows - 1) * w + x.coerceIn(0, cols - 1)]
        val gx =
            -at(c - 1, r - 1) - 2 * at(c - 1, r) - at(c - 1, r + 1) + at(c + 1, r - 1) + 2 * at(c + 1, r) +
                at(c + 1, r + 1)
        val gy =
            -at(c - 1, r - 1) - 2 * at(c, r - 1) - at(c + 1, r - 1) + at(c - 1, r + 1) + 2 * at(c, r + 1) +
                at(c + 1, r + 1)
        val mag = kotlin.math.sqrt(gx * gx + gy * gy) / 4f
        if (mag <= e.threshold) return e.base.glyphFor(at(c, r))
        // The edge runs perpendicular to the gradient. Terminal cells are about twice as tall as wide.
        val deg = kotlin.math.atan2(gy.toDouble(), gx.toDouble()) * 180.0 / kotlin.math.PI
        val a = ((deg % 180.0) + 180.0) % 180.0
        return when {
            a < 22.5 || a >= 157.5 -> '|'
            a < 67.5 -> '/'
            a < 112.5 -> '-'
            else -> '\\'
        }
    }
}
