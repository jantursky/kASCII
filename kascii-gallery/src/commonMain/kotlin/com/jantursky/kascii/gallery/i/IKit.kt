package com.jantursky.kascii.gallery.i

import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.Noise
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.Rng
import com.jantursky.kascii.gallery.AKit
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal const val ICOLS = 192
internal const val IROWS = 48
internal const val ICELLS = ICOLS * IROWS

/** One cell is 2.2 times taller than wide, so a row is this many width units. */
internal const val ROW_U = 2.2f
internal const val ITAU = 6.2831855f

/** Halftone glyphs of a gradient, from empty to dense. */
internal const val HALFTONE = " ·•●"

internal const val NONE = 0.toChar()

/** A color as floats in 0..1, used for blending before the final snap to a palette. */
internal class Col(
    val r: Float,
    val g: Float,
    val b: Float,
) {
    fun mix(
        o: Col,
        t: Float,
    ): Col = Col(r + (o.r - r) * t, g + (o.g - g) * t, b + (o.b - b) * t)

    fun scale(k: Float): Col = Col(r * k, g * k, b * k)
}

internal fun col(hex: String): Col = Rgb.fromHex(hex).let { Col(it.red / 255f, it.green / 255f, it.blue / 255f) }

internal fun ramp(
    steps: Int,
    vararg stops: Col,
): List<Col> =
    (0 until steps).map { k ->
        val p = k.toFloat() / (steps - 1) * (stops.size - 1)
        val i = min(p.toInt(), stops.size - 2)
        stops[i].mix(stops[i + 1], p - i)
    }

/** A ramp of [steps] shades of [c], from a dark tint through [c] to a pale tint. */
internal fun tones(
    c: Col,
    steps: Int = 10,
): List<Col> = ramp(steps, c.scale(0.12f), c.scale(0.5f), c, c.mix(Col(1f, 1f, 1f), 0.6f))

internal fun clamp01(v: Float): Float =
    if (v < 0f) {
        0f
    } else if (v > 1f) {
        1f
    } else {
        v
    }

internal fun smooth(
    a: Float,
    b: Float,
    v: Float,
): Float {
    val k = clamp01((v - a) / (b - a))
    return k * k * (3f - 2f * k)
}

internal fun gauss(
    x: Float,
    c: Float,
    w: Float,
): Float = exp(-((x - c) / w) * ((x - c) / w))

/** An int in [lo], [hi) from [this]. */
internal fun Rng.int(
    lo: Int,
    hi: Int,
): Int = lo + int(hi - lo)

internal fun fract(v: Float): Float = v - floor(v)

/** A random generator for one purpose ([salt]) of a piece. */
internal fun irng(
    seed: Int,
    salt: Int,
): Rng = AKit.rng(seed, salt)

internal val BAYER: FloatArray =
    floatArrayOf(0f, 8f, 2f, 10f, 12f, 4f, 14f, 6f, 3f, 11f, 1f, 9f, 15f, 7f, 13f, 5f)
        .map { (it + 0.5f) / 16f - 0.5f }
        .toFloatArray()

/**
 * A tiling table of value noise, so a lookup is two lerps instead of eight hashes. It repeats every [period] units
 * along x (a power of two up to 256), which lets a layer scroll by whole periods and close its loop.
 */
internal class ILattice(
    seed: Int,
    salt: Int = 0,
    period: Int = 256,
) {
    private val mask = period - 1
    private val table = FloatArray(256 * 256) { Noise.hash((it and 255) and mask, it shr 8, salt, seed) }

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
internal class IPalette(
    colors: List<Col>,
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
            ((clamp01(r) * 31.99f).toInt() shl 10) or
                ((clamp01(g) * 31.99f).toInt() shl 5) or
                (clamp01(b) * 31.99f).toInt(),
        ]

    private fun packed(
        r: Float,
        g: Float,
        b: Float,
    ): Int =
        Rgb(
            (clamp01(r) * 255f).roundToInt(),
            (clamp01(g) * 255f).roundToInt(),
            (clamp01(b) * 255f).roundToInt(),
        ).argb
}

/**
 * A full-grid canvas of float colors. [cov] is the coverage of a cell: 0 to 1 is a halftone of dots, above 1.4 is a
 * solid block. [ch] overrides the glyph of a cell when it is not zero.
 */
internal class IFrame {
    val r = FloatArray(ICELLS)
    val g = FloatArray(ICELLS)
    val b = FloatArray(ICELLS)
    val cov = FloatArray(ICELLS)
    val ch = CharArray(ICELLS)

    fun copyFrom(o: IFrame) {
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
        c: Col,
        coverage: Float = SOLID,
        glyph: Char = NONE,
    ) {
        if (x < 0 || x >= ICOLS || y < 0 || y >= IROWS) return
        val i = y * ICOLS + x
        r[i] = c.r
        g[i] = c.g
        b[i] = c.b
        cov[i] = coverage
        ch[i] = glyph
    }

    /** Lays every cell of [o] that has coverage over this canvas. */
    fun over(o: IFrame) {
        for (i in 0 until ICELLS) {
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
        c: Col,
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
        c: Col,
        k: Float,
    ) {
        if (cov[i] < 0.05f) {
            r[i] = 0f
            g[i] = 0f
            b[i] = 0f
        }
        r[i] += c.r * k
        g[i] += c.g * k
        b[i] += c.b * k
        if (cov[i] < 1.4f && k > cov[i]) cov[i] = min(1f, k)
    }

    /** Adds a round glow of color [c], strength [k] and radius [rad] (width units) around a point in cells. */
    fun glow(
        cx: Float,
        cy: Float,
        rad: Float,
        c: Col,
        k: Float,
    ) {
        val reach = rad * 2.2f
        for (y in max(0, (cy - reach / ROW_U).toInt())..min(IROWS - 1, (cy + reach / ROW_U).toInt() + 1)) {
            for (x in max(0, (cx - reach).toInt())..min(ICOLS - 1, (cx + reach).toInt() + 1)) {
                val dx = x + 0.5f - cx
                val dy = (y + 0.5f - cy) * ROW_U
                val d2 = (dx * dx + dy * dy) / (rad * rad)
                if (d2 > 4.9f) continue
                light(y * ICOLS + x, c, k * exp(-d2))
            }
        }
    }

    /** Fills an ellipse of radii [rx] (width units) and [ry] (rows), shading each cell from its local -1..1 coords. */
    fun ellipse(
        cx: Float,
        cy: Float,
        rx: Float,
        ry: Float,
        cov: Float = SOLID,
        shade: (Float, Float) -> Col,
    ) {
        for (y in max(0, (cy - ry).toInt() - 1)..min(IROWS - 1, (cy + ry).toInt() + 1)) {
            for (x in max(0, (cx - rx).toInt() - 1)..min(ICOLS - 1, (cx + rx).toInt() + 1)) {
                val a = (x + 0.5f - cx) / rx
                val b = (y + 0.5f - cy) / ry
                if (a * a + b * b <= 1f) put(x, y, shade(a, b), cov)
            }
        }
    }

    /** Fills the cells of a rectangle, shading each from its local 0..1 coords. */
    fun rect(
        x0: Int,
        y0: Int,
        x1: Int,
        y1: Int,
        cov: Float = SOLID,
        shade: (Float, Float) -> Col,
    ) {
        val w = max(1, x1 - x0)
        val h = max(1, y1 - y0)
        for (y in y0..y1) for (x in x0..x1) put(x, y, shade((x - x0).toFloat() / w, (y - y0).toFloat() / h), cov)
    }

    /** A thick segment of half width [half] (width units), shaded from the signed distance across it. */
    fun segment(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        half: Float,
        shade: (Float) -> Col,
    ) {
        val ax = x1 - x0
        val ay = (y1 - y0) * ROW_U
        val len2 = max(1e-4f, ax * ax + ay * ay)
        val pad = (half + 1f).toInt()
        for (y in max(
            0,
            (min(y0, y1) - half / ROW_U).toInt() - 1,
        )..min(IROWS - 1, (max(y0, y1) + half / ROW_U).toInt() + 1)) {
            for (x in max(0, min(x0, x1).toInt() - pad)..min(ICOLS - 1, max(x0, x1).toInt() + pad)) {
                val px = x + 0.5f - x0
                val py = (y + 0.5f - y0) * ROW_U
                val s = clamp01((px * ax + py * ay) / len2)
                val ex = px - ax * s
                val ey = py - ay * s
                if (ex * ex + ey * ey <= half * half) put(x, y, shade(ey / half), SOLID)
            }
        }
    }

    /** A thin line of box or ASCII glyphs chosen by its slope. */
    fun line(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        c: Col,
        coverage: Float = 1f,
    ) {
        val dx = x1 - x0
        val dy = y1 - y0
        val slope = if (abs(dx) < 1e-4f) 99f else dy * ROW_U / dx
        val glyph =
            when {
                abs(slope) < 0.35f -> '─'
                abs(slope) > 3.5f -> '│'
                slope > 0f -> '╲'
                else -> '╱'
            }
        val n = max(abs(dx), abs(dy)).toInt() + 1
        for (k in 0..n) {
            val p = k.toFloat() / n
            put((x0 + dx * p).toInt(), (y0 + dy * p).toInt(), c, coverage, glyph)
        }
    }

    /** Writes [s] starting at cell [x], [y]. */
    fun text(
        x: Int,
        y: Int,
        s: String,
        c: Col,
    ) {
        for (k in s.indices) if (s[k] != ' ') put(x + k, y, c, 1f, s[k])
    }

    /** Turns the canvas into glyphs and palette colors. */
    fun output(
        grid: Grid,
        palette: IPalette,
    ) {
        for (y in 0 until IROWS) {
            for (x in 0 until ICOLS) {
                val i = y * ICOLS + x
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
                val q = c * 3f + BAYER[(y and 3) * 4 + (x and 3)]
                val idx = min(3, (q + 0.5f).toInt())
                if (idx <= 0) {
                    grid.glyphs[i] = ' '
                    grid.colors[i] = 0
                } else {
                    grid.glyphs[i] = HALFTONE[idx]
                    grid.colors[i] = palette.pick(r[i], g[i], b[i])
                }
            }
        }
    }

    companion object {
        const val SOLID = 2f
    }
}

/** Fixed stars of a night sky that twinkle a whole number of times per loop. */
internal class IStars(
    seed: Int,
    salt: Int,
    count: Int,
    maxRow: Int,
    skip: (Int, Int) -> Boolean = { _, _ -> false },
) {
    private val xs = IntArray(count)
    private val ys = IntArray(count)
    private val phase = FloatArray(count)
    private val rate = IntArray(count)
    private val mag = FloatArray(count)

    init {
        val rng = irng(seed, salt)
        for (s in 0 until count) {
            var x: Int
            var y: Int
            var tries = 0
            do {
                x = rng.int(ICOLS)
                y = rng.int(maxRow)
                tries++
            } while (skip(x, y) && tries < 40)
            xs[s] = x
            ys[s] = y
            phase[s] = rng.float()
            rate[s] = 1 + rng.int(3)
            mag[s] = rng.float()
        }
    }

    /** Draws the stars into empty cells of [f] at loop phase [u] (0..1), scaled by [k] and fading with [fade] per row. */
    fun draw(
        f: IFrame,
        u: Float,
        k: Float = 1f,
        fade: (Int) -> Float = { 1f },
    ) {
        for (s in xs.indices) {
            val tw = 0.55f + 0.45f * sin(ITAU * (rate[s] * u + phase[s]))
            val v = tw * (0.4f + 0.6f * mag[s]) * k * fade(ys[s])
            if (v < 0.22f) continue
            val i = ys[s] * ICOLS + xs[s]
            if (f.cov[i] > 0.6f) continue
            val c = Col(0.55f + 0.45f * v, 0.6f + 0.4f * v, 0.75f + 0.25f * v)
            f.put(
                xs[s],
                ys[s],
                c,
                1f,
                if (v > 0.8f) {
                    '*'
                } else if (v > 0.5f) {
                    '+'
                } else if (v > 0.33f) {
                    '·'
                } else {
                    '.'
                },
            )
        }
    }
}

/** Distance in width units between two points given in cells. */
internal fun dist(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
): Float {
    val dx = x1 - x0
    val dy = (y1 - y0) * ROW_U
    return sqrt(dx * dx + dy * dy)
}
