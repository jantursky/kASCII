package com.jantursky.kascii.gallery.j

import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.Noise
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.Rng
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal const val JCOLS = 192
internal const val JROWS = 48
internal const val JCELLS = JCOLS * JROWS

/** One cell is 2.2 times taller than wide, so a row is this many width units. */
internal const val JROW_U = 2.2f
internal const val JTAU = 6.2831855f

/** Halftone glyphs of a gradient, from empty to dense. */
internal const val JHALFTONE = " ·•●"

private const val NONE = 0.toChar()

/** A color as floats in 0..1, used for blending before the final snap to a palette. */
internal class JCol(
    val r: Float,
    val g: Float,
    val b: Float,
) {
    fun mix(
        o: JCol,
        t: Float,
    ): JCol = JCol(r + (o.r - r) * t, g + (o.g - g) * t, b + (o.b - b) * t)

    fun scale(k: Float): JCol = JCol(r * k, g * k, b * k)
}

internal fun jcol(hex: String): JCol = Rgb.fromHex(hex).let { JCol(it.red / 255f, it.green / 255f, it.blue / 255f) }

internal fun jramp(
    steps: Int,
    vararg stops: JCol,
): List<JCol> =
    (0 until steps).map { k ->
        val p = k.toFloat() / (steps - 1) * (stops.size - 1)
        val i = min(p.toInt(), stops.size - 2)
        stops[i].mix(stops[i + 1], p - i)
    }

internal fun jclamp(v: Float): Float =
    if (v < 0f) {
        0f
    } else if (v > 1f) {
        1f
    } else {
        v
    }

internal fun jsmooth(
    a: Float,
    b: Float,
    v: Float,
): Float {
    val k = jclamp((v - a) / (b - a))
    return k * k * (3f - 2f * k)
}

internal fun jgauss(
    x: Float,
    c: Float,
    w: Float,
): Float = exp(-((x - c) / w) * ((x - c) / w))

internal fun jfract(v: Float): Float = v - floor(v)

/** An int in [lo], [hi) from [this]. */
internal fun Rng.jint(
    lo: Int,
    hi: Int,
): Int = lo + int(hi - lo)

internal val JBAYER: FloatArray =
    floatArrayOf(0f, 8f, 2f, 10f, 12f, 4f, 14f, 6f, 3f, 11f, 1f, 9f, 15f, 7f, 13f, 5f)
        .map { (it + 0.5f) / 16f - 0.5f }
        .toFloatArray()

/** A tiling table of value noise, so a lookup is two lerps instead of eight hashes. */
internal class JLattice(
    seed: Int,
    salt: Int = 0,
) {
    private val table = FloatArray(256 * 256) { Noise.hash(it and 255, it shr 8, salt, seed) }

    fun at(
        x: Float,
        y: Float,
    ): Float {
        val xf = floor(x)
        val yf = floor(y)
        val fx = (x - xf).let { it * it * (3f - 2f * it) }
        val fy = (y - yf).let { it * it * (3f - 2f * it) }
        val x0 = xf.toInt() and 255
        val x1 = (x0 + 1) and 255
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
    ): Float {
        var sum = 0f
        var amp = 0.5f
        var norm = 0f
        var f = 1f
        repeat(octaves) {
            sum += amp * at(x * f, y * f)
            norm += amp
            amp *= 0.5f
            f *= 2f
        }
        return sum / norm
    }
}

/** The colors a scene can show, with a 5 bits per channel table that snaps any computed color to the nearest. */
internal class JPalette(
    colors: List<JCol>,
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
            ((jclamp(r) * 31.99f).toInt() shl 10) or
                ((jclamp(g) * 31.99f).toInt() shl 5) or
                (jclamp(b) * 31.99f).toInt(),
        ]

    private fun packed(
        r: Float,
        g: Float,
        b: Float,
    ): Int = Rgb((r * 255f).roundToInt(), (g * 255f).roundToInt(), (b * 255f).roundToInt()).argb
}

/**
 * A full-grid canvas of float colors. [cov] is the coverage of a cell: 0 to 1 is a halftone of dots, above 1.4 is a
 * solid block. [ch] overrides the glyph of a cell when it is not zero. Shape helpers take x in cells (width units),
 * y in rows, horizontal radii in width units and vertical radii in rows.
 */
internal class JFrame {
    val r = FloatArray(JCELLS)
    val g = FloatArray(JCELLS)
    val b = FloatArray(JCELLS)
    val cov = FloatArray(JCELLS)
    val ch = CharArray(JCELLS)

    fun clear() {
        r.fill(0f)
        g.fill(0f)
        b.fill(0f)
        cov.fill(0f)
        ch.fill(NONE)
    }

    fun copyFrom(o: JFrame) {
        o.r.copyInto(r)
        o.g.copyInto(g)
        o.b.copyInto(b)
        o.cov.copyInto(cov)
        o.ch.copyInto(ch)
    }

    /** Paints a cell outright, replacing whatever was there. */
    fun put(
        x: Int,
        y: Int,
        c: JCol,
        coverage: Float = SOLID,
        glyph: Char = NONE,
    ) {
        if (x < 0 || x >= JCOLS || y < 0 || y >= JROWS) return
        val i = y * JCOLS + x
        r[i] = c.r
        g[i] = c.g
        b[i] = c.b
        cov[i] = coverage
        ch[i] = glyph
    }

    /** Lays every cell of [o] that has coverage over this canvas. */
    fun over(o: JFrame) {
        for (i in 0 until JCELLS) {
            if (o.cov[i] <= 0f) continue
            r[i] = o.r[i]
            g[i] = o.g[i]
            b[i] = o.b[i]
            cov[i] = o.cov[i]
            ch[i] = o.ch[i]
        }
    }

    /** Blends a haze of color [c] and thickness [d] (0..1) over cell [i]; solids are tinted, gaps fill with dots. */
    fun haze(
        i: Int,
        c: JCol,
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
        c: JCol,
        k: Float,
    ) {
        r[i] += c.r * k
        g[i] += c.g * k
        b[i] += c.b * k
        if (cov[i] < 1.4f && k > cov[i]) cov[i] = min(1f, k)
    }

    /** Adds a round glow of color [c] around ([cx], [cy]) that falls off over [radius] width units. */
    fun glow(
        cx: Float,
        cy: Float,
        radius: Float,
        c: JCol,
        k: Float,
    ) {
        val reach = radius * 2.6f
        for (y in max(0, (cy - reach / JROW_U).toInt())..min(JROWS - 1, (cy + reach / JROW_U).toInt() + 1)) {
            for (x in max(0, (cx - reach).toInt())..min(JCOLS - 1, (cx + reach).toInt() + 1)) {
                val dx = x + 0.5f - cx
                val dy = (y + 0.5f - cy) * JROW_U
                val d2 = (dx * dx + dy * dy) / (radius * radius)
                if (d2 > 6.8f) continue
                light(y * JCOLS + x, c, k * exp(-d2))
            }
        }
    }

    /** Fills an ellipse; [shade] gets the normalized position (-1..1) across and down the ellipse. */
    fun ellipse(
        cx: Float,
        cy: Float,
        rx: Float,
        ry: Float,
        coverage: Float = SOLID,
        glyph: Char = NONE,
        shade: (Float, Float) -> JCol,
    ) {
        for (y in max(0, (cy - ry).toInt() - 1)..min(JROWS - 1, (cy + ry).toInt() + 1)) {
            for (x in max(0, (cx - rx).toInt() - 1)..min(JCOLS - 1, (cx + rx).toInt() + 1)) {
                val a = (x + 0.5f - cx) / rx
                val b = (y + 0.5f - cy) / ry
                if (a * a + b * b <= 1f) put(x, y, shade(a, b), coverage, glyph)
            }
        }
    }

    /** A thick capsule from ([x0], [y0]) to ([x1], [y1]) with half width [half] (width units, tapering to [half1]). */
    fun segment(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        half: Float,
        half1: Float = half,
        coverage: Float = SOLID,
        shade: (Float, Float) -> JCol,
    ) {
        val ax = x1 - x0
        val ay = (y1 - y0) * JROW_U
        val len2 = max(1e-4f, ax * ax + ay * ay)
        val hm = max(half, half1)
        val pad = (hm / JROW_U).toInt() + 2
        for (y in max(0, min(y0, y1).toInt() - pad)..min(JROWS - 1, max(y0, y1).toInt() + pad)) {
            for (x in max(0, (min(x0, x1) - hm).toInt() - 1)..min(JCOLS - 1, (max(x0, x1) + hm).toInt() + 1)) {
                val px = x + 0.5f - x0
                val py = (y + 0.5f - y0) * JROW_U
                val s = jclamp((px * ax + py * ay) / len2)
                val ex = px - ax * s
                val ey = py - ay * s
                val h = half + (half1 - half) * s
                val e2 = ex * ex + ey * ey
                if (e2 <= h * h) put(x, y, shade(s, sqrt(e2) / max(h, 0.01f) * (if (ey < 0f) -1f else 1f)), coverage)
            }
        }
    }

    /** Fills a triangle given by three points (x in cells, y in rows). */
    fun tri(
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        cx: Float,
        cy: Float,
        c: JCol,
        coverage: Float = SOLID,
    ) {
        val y0 = max(0, min(ay, min(by, cy)).toInt())
        val y1 = min(JROWS - 1, max(ay, max(by, cy)).toInt())
        val x0 = max(0, min(ax, min(bx, cx)).toInt())
        val x1 = min(JCOLS - 1, max(ax, max(bx, cx)).toInt())
        for (y in y0..y1) {
            for (x in x0..x1) {
                val px = x + 0.5f
                val py = y + 0.5f
                val d1 = (px - bx) * (ay - by) - (ax - bx) * (py - by)
                val d2 = (px - cx) * (by - cy) - (bx - cx) * (py - cy)
                val d3 = (px - ax) * (cy - ay) - (cx - ax) * (py - ay)
                val neg = d1 < 0f || d2 < 0f || d3 < 0f
                val pos = d1 > 0f || d2 > 0f || d3 > 0f
                if (!(neg && pos)) put(x, y, c, coverage)
            }
        }
    }

    /** Draws a one-cell line of glyphs from ([x0], [y0]) to ([x1], [y1]) that picks `| / \ -` by its slope. */
    fun line(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        c: JCol,
        coverage: Float = 1f,
    ) {
        val dx = x1 - x0
        val dy = y1 - y0
        val n = max(1, max(kotlin.math.abs(dx), kotlin.math.abs(dy) * 2f).toInt())
        val glyph = slopeGlyph(dx, dy)
        for (k in 0..n) {
            val p = k.toFloat() / n
            val x = x0 + dx * p
            val y = y0 + dy * p
            put(floor(x).toInt(), floor(y).toInt(), c, coverage, glyph)
        }
    }

    /** Turns the canvas into glyphs and palette colors. */
    fun output(
        grid: Grid,
        palette: JPalette,
    ) {
        for (y in 0 until JROWS) {
            for (x in 0 until JCOLS) {
                val i = y * JCOLS + x
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
                val q = c * 3f + JBAYER[(y and 3) * 4 + (x and 3)]
                val idx = min(3, (q + 0.5f).toInt())
                if (idx <= 0) {
                    grid.glyphs[i] = ' '
                    grid.colors[i] = 0
                } else {
                    grid.glyphs[i] = JHALFTONE[idx]
                    grid.colors[i] = palette.pick(r[i], g[i], b[i])
                }
            }
        }
    }

    companion object {
        const val SOLID = 2f
    }
}

/** The line glyph for a direction ([dx] cells, [dy] rows). */
internal fun slopeGlyph(
    dx: Float,
    dy: Float,
): Char {
    val ax = kotlin.math.abs(dx)
    val ay = kotlin.math.abs(dy) * JROW_U
    return if (ay < ax * 0.4f) {
        '-'
    } else if (ax < ay * 0.4f) {
        '|'
    } else if ((dx > 0f) == (dy > 0f)) {
        '\\'
    } else {
        '/'
    }
}
