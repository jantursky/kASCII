package com.jantursky.kascii.gallery.h

import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.Noise
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.Rng
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal const val HCOLS = 192
internal const val HROWS = 48
internal const val HCELLS = HCOLS * HROWS

/** One cell is 2.2 times taller than wide, so a row is this many width units. */
internal const val HROW_U = 2.2f
internal const val HTAU = 6.2831855f

/** Halftone glyphs of a gradient, from empty to dense. */
internal const val HHALFTONE = " ·•●"

private const val NONE = 0.toChar()

/** A color as floats in 0..1, used for blending before the final snap to a palette. */
internal class HCol(
    val r: Float,
    val g: Float,
    val b: Float,
) {
    fun mix(
        o: HCol,
        t: Float,
    ): HCol = HCol(r + (o.r - r) * t, g + (o.g - g) * t, b + (o.b - b) * t)

    fun scale(k: Float): HCol = HCol(r * k, g * k, b * k)
}

internal fun hcol(hex: String): HCol = Rgb.fromHex(hex).let { HCol(it.red / 255f, it.green / 255f, it.blue / 255f) }

internal fun hramp(
    steps: Int,
    vararg stops: HCol,
): List<HCol> =
    (0 until steps).map { k ->
        val p = k.toFloat() / (steps - 1) * (stops.size - 1)
        val i = min(p.toInt(), stops.size - 2)
        stops[i].mix(stops[i + 1], p - i)
    }

/** The color at [t] (0..1) along evenly spaced [stops]. */
internal fun hgrad(
    t: Float,
    stops: List<HCol>,
): HCol {
    val p = hclamp(t) * (stops.size - 1)
    val i = min(p.toInt(), stops.size - 2)
    return stops[i].mix(stops[i + 1], p - i)
}

internal fun hclamp(v: Float): Float =
    if (v < 0f) {
        0f
    } else if (v > 1f) {
        1f
    } else {
        v
    }

internal fun hsmooth(
    a: Float,
    b: Float,
    v: Float,
): Float {
    val k = hclamp((v - a) / (b - a))
    return k * k * (3f - 2f * k)
}

internal fun hgauss(
    x: Float,
    c: Float,
    w: Float,
): Float = exp(-((x - c) / w) * ((x - c) / w))

/** An int in [lo], [hi) from [this]. */
internal fun Rng.hrange(
    lo: Int,
    hi: Int,
): Int = lo + int(hi - lo)

internal fun hfract(v: Float): Float = v - floor(v)

/** A generator for one purpose ([salt]) of a piece, so adding a draw call never shifts another one. */
internal fun hrng(
    seed: Int,
    salt: Int,
): Rng = Rng(seed * 1_000_003L + salt * 7919L + 17L)

internal val HBAYER: FloatArray =
    floatArrayOf(0f, 8f, 2f, 10f, 12f, 4f, 14f, 6f, 3f, 11f, 1f, 9f, 15f, 7f, 13f, 5f)
        .map { (it + 0.5f) / 16f - 0.5f }
        .toFloatArray()

/** The ordered-dither threshold of cell [x], [y], in -0.5..0.5. */
internal fun hbayer(
    x: Int,
    y: Int,
): Float = HBAYER[(y and 3) * 4 + (x and 3)]

/** A tiling table of value noise, so a lookup is two lerps instead of eight hashes. */
internal class HLattice(
    seed: Int,
    salt: Int = 0,
) {
    private val table = FloatArray(256 * 256) { Noise.hash(it and 255, it shr 8, salt, seed) }

    fun at(
        x: Float,
        y: Float,
    ): Float = wrapAt(x, y, 256)

    /** Value noise that repeats every [period] lattice units along x, for fields that drift a whole period per loop. */
    fun wrapAt(
        x: Float,
        y: Float,
        period: Int,
    ): Float {
        val xf = floor(x)
        val yf = floor(y)
        val fx = (x - xf).let { it * it * (3f - 2f * it) }
        val fy = (y - yf).let { it * it * (3f - 2f * it) }
        val xi = xf.toInt()
        val x0 = ((xi % period) + period) % period
        val x1 = (x0 + 1) % period
        val y0 = (yf.toInt() and 255) shl 8
        val y1 = ((yf.toInt() + 1) and 255) shl 8
        val a = table[y0 + x0] + (table[y0 + x1] - table[y0 + x0]) * fx
        val b = table[y1 + x0] + (table[y1 + x1] - table[y1 + x0]) * fx
        return a + (b - a) * fy
    }

    fun fbm(
        x: Float,
        y: Float,
        octaves: Int,
    ): Float = wrapFbm(x, y, octaves, 256)

    /** Fractal noise whose first octave repeats every [period] units along x; finer octaves repeat with it. */
    fun wrapFbm(
        x: Float,
        y: Float,
        octaves: Int,
        period: Int,
    ): Float {
        var sum = 0f
        var amp = 0.5f
        var norm = 0f
        var f = 1f
        var p = period
        repeat(octaves) {
            sum += amp * wrapAt(x * f, y * f + it * 37f, min(256, p))
            norm += amp
            amp *= 0.5f
            f *= 2f
            p *= 2
        }
        return sum / norm
    }
}

/** The colors a scene can show, with a 5 bits per channel table that snaps any computed color to the nearest. */
internal class HPalette(
    colors: List<HCol>,
) {
    private val lut = IntArray(32768)

    init {
        val argb = IntArray(colors.size) { packed(colors[it].r, colors[it].g, colors[it].b) }
        for (k in lut.indices) {
            val r = (((k shr 10) and 31) + 0.5f) / 32f
            val g = (((k shr 5) and 31) + 0.5f) / 32f
            val b = ((k and 31) + 0.5f) / 32f
            var best = 0
            var bd = Float.MAX_VALUE
            for (i in colors.indices) {
                val dr = colors[i].r - r
                val dg = colors[i].g - g
                val db = colors[i].b - b
                val d = 0.35f * dr * dr + 0.45f * dg * dg + 0.2f * db * db
                if (d < bd) {
                    bd = d
                    best = i
                }
            }
            lut[k] = argb[best]
        }
    }

    fun pick(
        r: Float,
        g: Float,
        b: Float,
    ): Int =
        lut[
            ((hclamp(r) * 31.99f).toInt() shl 10) or
                ((hclamp(g) * 31.99f).toInt() shl 5) or
                (hclamp(b) * 31.99f).toInt(),
        ]

    private fun packed(
        r: Float,
        g: Float,
        b: Float,
    ): Int = Rgb((r * 255f).roundToInt(), (g * 255f).roundToInt(), (b * 255f).roundToInt()).argb
}

/**
 * A full-grid canvas of float colors. [cov] is the coverage of a cell: 0 to 1 is a halftone of dots, above 1.4 is a
 * solid block. [ch] overrides the glyph of a cell when it is not zero.
 */
internal class HFrame {
    val r = FloatArray(HCELLS)
    val g = FloatArray(HCELLS)
    val b = FloatArray(HCELLS)
    val cov = FloatArray(HCELLS)
    val ch = CharArray(HCELLS)

    fun copyFrom(o: HFrame) {
        o.r.copyInto(r)
        o.g.copyInto(g)
        o.b.copyInto(b)
        o.cov.copyInto(cov)
        o.ch.copyInto(ch)
    }

    /** Lays every cell of [o] that has coverage over this canvas. */
    fun over(o: HFrame) {
        for (i in 0 until HCELLS) {
            if (o.cov[i] <= 0f) continue
            r[i] = o.r[i]
            g[i] = o.g[i]
            b[i] = o.b[i]
            cov[i] = o.cov[i]
            ch[i] = o.ch[i]
        }
    }

    /**
     * A small bright particle of [c] at strength [a] (0..1): over a solid cell it tints the block, so no dark gap opens
     * around it; over open sky it draws [glyph].
     */
    fun speck(
        x: Int,
        y: Int,
        c: HCol,
        a: Float,
        glyph: Char = '•',
    ) {
        if (x < 0 || x >= HCOLS || y < 0 || y >= HROWS) return
        val i = y * HCOLS + x
        if (cov[i] > 1.4f) {
            r[i] += (c.r - r[i]) * a
            g[i] += (c.g - g[i]) * a
            b[i] += (c.b - b[i]) * a
            cov[i] = 2f
            ch[i] = NONE
        } else {
            set(i, c, 1f, glyph)
        }
    }

    /** Paints a cell outright, replacing whatever was there. */
    fun put(
        x: Int,
        y: Int,
        c: HCol,
        coverage: Float = SOLID,
        glyph: Char = NONE,
    ) {
        if (x < 0 || x >= HCOLS || y < 0 || y >= HROWS) return
        set(y * HCOLS + x, c, coverage, glyph)
    }

    /** Paints cell index [i] outright. */
    fun set(
        i: Int,
        c: HCol,
        coverage: Float = SOLID,
        glyph: Char = NONE,
    ) {
        r[i] = c.r
        g[i] = c.g
        b[i] = c.b
        cov[i] = coverage
        ch[i] = glyph
    }

    /** Blends a haze of color [c] and thickness [d] (0..1) over cell [i]; solids are tinted, gaps fill with dots. */
    fun haze(
        i: Int,
        c: HCol,
        d: Float,
    ) {
        r[i] += (c.r - r[i]) * d
        g[i] += (c.g - g[i]) * d
        b[i] += (c.b - b[i]) * d
        if (cov[i] < 1.4f) cov[i] += (1f - cov[i]) * d * 0.9f
    }

    /** Adds light of color [c] with strength [k] to cell [i]. */
    fun light(
        i: Int,
        c: HCol,
        k: Float,
    ) {
        r[i] += c.r * k
        g[i] += c.g * k
        b[i] += c.b * k
        if (cov[i] < 1.4f && k > cov[i]) cov[i] = min(1f, k)
    }

    /**
     * Adds a soft round glow of [c] centred on column [cx], row [cy], falling off over [radius] width units, at most
     * [k] strong. Dots appear in empty cells where the glow is strong enough.
     */
    fun glow(
        cx: Float,
        cy: Float,
        radius: Float,
        c: HCol,
        k: Float,
    ) {
        val reach = radius * 2.2f
        val y0 = max(0, (cy - reach / HROW_U).toInt())
        val y1 = min(HROWS - 1, (cy + reach / HROW_U).toInt() + 1)
        val x0 = max(0, (cx - reach).toInt())
        val x1 = min(HCOLS - 1, (cx + reach).toInt() + 1)
        val inv = 1f / (radius * radius)
        for (y in y0..y1) {
            val dy = (y + 0.5f - cy) * HROW_U
            for (x in x0..x1) {
                val dx = x + 0.5f - cx
                val d2 = (dx * dx + dy * dy) * inv
                if (d2 > 4.84f) continue
                light(y * HCOLS + x, c, k * exp(-d2))
            }
        }
    }

    /** Turns the canvas into glyphs and palette colors. */
    fun output(
        grid: Grid,
        palette: HPalette,
    ) {
        for (y in 0 until HROWS) {
            for (x in 0 until HCOLS) {
                val i = y * HCOLS + x
                val c = cov[i]
                if (c > 1.4f) {
                    val o = ch[i]
                    grid.glyphs[i] =
                        if (o != NONE) {
                            o
                        } else if (c > 1.95f) {
                            '█'
                        } else if (c > 1.7f) {
                            '▓'
                        } else {
                            '▒'
                        }
                    grid.colors[i] = palette.pick(r[i], g[i], b[i])
                    continue
                }
                if (ch[i] != NONE && c > 0.05f) {
                    grid.glyphs[i] = ch[i]
                    grid.colors[i] = palette.pick(r[i], g[i], b[i])
                    continue
                }
                val q = c * 3f + HBAYER[(y and 3) * 4 + (x and 3)]
                val idx = min(3, (q + 0.5f).toInt())
                if (idx <= 0) {
                    grid.glyphs[i] = ' '
                    grid.colors[i] = 0
                } else {
                    grid.glyphs[i] = HHALFTONE[idx]
                    grid.colors[i] = palette.pick(r[i], g[i], b[i])
                }
            }
        }
    }

    companion object {
        const val SOLID = 2f
    }
}

/** A scattered, non repeating jitter in -0.5..0.5 for cell [x], [y]. */
internal fun hjitter(
    x: Int,
    y: Int,
): Float {
    var h = x * 374761393 + y * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    return ((h ushr 8) and 1023) / 1023f - 0.5f
}

/** The solid coverage for texture [k] in 0..1 with scattered jitter at cell [x], [y]: full, dense or medium shade. */
internal fun hsolid(
    k: Float,
    x: Int,
    y: Int,
): Float {
    val q = k + hjitter(x, y) * 0.45f
    return if (q > 0.62f) {
        2f
    } else if (q > 0.3f) {
        1.8f
    } else {
        1.6f
    }
}

/** Fills an ellipse centred on [cx], [cy] with radii [rx] columns and [ry] rows; [shade] gets local -1..1 coords. */
internal fun HFrame.ellipse(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    cov: Float = 2f,
    glyph: Char = 0.toChar(),
    shade: (Float, Float) -> HCol,
) {
    for (y in max(0, (cy - ry).toInt() - 1)..min(HROWS - 1, (cy + ry).toInt() + 1)) {
        for (x in max(0, (cx - rx).toInt() - 1)..min(HCOLS - 1, (cx + rx).toInt() + 1)) {
            val a = (x + 0.5f - cx) / rx
            val b = (y + 0.5f - cy) / ry
            if (a * a + b * b <= 1f) put(x, y, shade(a, b), cov, glyph)
        }
    }
}

/** A thick line from ([x0], [y0]) to ([x1], [y1]) in columns and rows, [half] width units either side. */
internal fun HFrame.segment(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    half: Float,
    c: HCol,
    cov: Float = 2f,
    glyph: Char = 0.toChar(),
) {
    val ax = x1 - x0
    val ay = (y1 - y0) * HROW_U
    val len2 = max(1e-4f, ax * ax + ay * ay)
    val reachX = half.toInt() + 2
    val reachY = (half / HROW_U).toInt() + 2
    for (y in max(0, min(y0, y1).toInt() - reachY)..min(HROWS - 1, max(y0, y1).toInt() + reachY)) {
        for (x in max(0, min(x0, x1).toInt() - reachX)..min(HCOLS - 1, max(x0, x1).toInt() + reachX)) {
            val px = x + 0.5f - x0
            val py = (y + 0.5f - y0) * HROW_U
            val s = hclamp((px * ax + py * ay) / len2)
            val ex = px - ax * s
            val ey = py - ay * s
            if (ex * ex + ey * ey <= half * half) put(x, y, c, cov, glyph)
        }
    }
}

/** A one-cell line of glyphs from ([x0], [y0]) to ([x1], [y1]), picking `─│╱╲` by its slope. */
internal fun HFrame.stroke(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    c: HCol,
    cov: Float = 1f,
) {
    val dx = x1 - x0
    val dy = y1 - y0
    val slope = dy * HROW_U / (if (dx == 0f) 1e-4f else dx)
    val glyph =
        if (kotlin.math.abs(slope) < 0.45f) {
            '─'
        } else if (kotlin.math.abs(slope) > 2.6f) {
            '│'
        } else if (slope > 0f) {
            '╲'
        } else {
            '╱'
        }
    val n = max(kotlin.math.abs(dx), kotlin.math.abs(dy)).toInt() + 1
    for (k in 0..n) {
        val p = k.toFloat() / n
        put((x0 + dx * p).toInt(), (y0 + dy * p).toInt(), c, cov, glyph)
    }
}
