package com.jantursky.kascii.gallery.m

import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.Noise
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.Rng
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

internal const val MCOLS = 192
internal const val MROWS = 48
internal const val MCELLS = MCOLS * MROWS

/** One cell is 2.2 times taller than wide, so a row is this many width units. */
internal const val M_ROW_U = 2.2f
internal const val MTAU = 6.2831855f

/** Halftone glyphs of a gradient, from empty to dense. */
internal const val M_HALFTONE = " ·•●"

private const val M_NONE = 0.toChar()

/** A color as floats in 0..1, used for blending before the final snap to a palette. */
internal class MCol(
    val r: Float,
    val g: Float,
    val b: Float,
) {
    fun mix(
        o: MCol,
        t: Float,
    ): MCol = MCol(r + (o.r - r) * t, g + (o.g - g) * t, b + (o.b - b) * t)

    fun scale(k: Float): MCol = MCol(r * k, g * k, b * k)
}

internal fun mcol(hex: String): MCol = Rgb.fromHex(hex).let { MCol(it.red / 255f, it.green / 255f, it.blue / 255f) }

internal fun mramp(
    steps: Int,
    vararg stops: MCol,
): List<MCol> =
    (0 until steps).map { k ->
        val p = k.toFloat() / (steps - 1) * (stops.size - 1)
        val i = min(p.toInt(), stops.size - 2)
        stops[i].mix(stops[i + 1], p - i)
    }

internal fun mclamp01(v: Float): Float =
    if (v < 0f) {
        0f
    } else if (v > 1f) {
        1f
    } else {
        v
    }

internal fun msmooth(
    a: Float,
    b: Float,
    v: Float,
): Float {
    val k = mclamp01((v - a) / (b - a))
    return k * k * (3f - 2f * k)
}

internal fun mgauss(
    x: Float,
    c: Float,
    w: Float,
): Float = exp(-((x - c) / w) * ((x - c) / w))

/** An int in [lo], [hi) from [this]. */
internal fun Rng.intIn(
    lo: Int,
    hi: Int,
): Int = lo + int(hi - lo)

internal fun mfract(v: Float): Float = v - floor(v)

internal val M_BAYER: FloatArray =
    floatArrayOf(0f, 8f, 2f, 10f, 12f, 4f, 14f, 6f, 3f, 11f, 1f, 9f, 15f, 7f, 13f, 5f)
        .map { (it + 0.5f) / 16f - 0.5f }
        .toFloatArray()

/** A tiling table of value noise, so a lookup is two lerps instead of eight hashes. */
internal class MLattice(
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
internal class MPalette(
    colors: List<MCol>,
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
            ((mclamp01(r) * 31.99f).toInt() shl 10) or
                ((mclamp01(g) * 31.99f).toInt() shl 5) or
                (mclamp01(b) * 31.99f).toInt(),
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
internal class MFrame {
    val r = FloatArray(MCELLS)
    val g = FloatArray(MCELLS)
    val b = FloatArray(MCELLS)
    val cov = FloatArray(MCELLS)
    val ch = CharArray(MCELLS)

    fun clear() {
        r.fill(0f)
        g.fill(0f)
        b.fill(0f)
        cov.fill(0f)
        ch.fill(M_NONE)
    }

    fun copyFrom(o: MFrame) {
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
        c: MCol,
        coverage: Float = SOLID,
        glyph: Char = M_NONE,
    ) {
        if (x < 0 || x >= MCOLS || y < 0 || y >= MROWS) return
        val i = y * MCOLS + x
        r[i] = c.r
        g[i] = c.g
        b[i] = c.b
        cov[i] = coverage
        ch[i] = glyph
    }

    /** Lays every cell of [o] that has coverage over this canvas. */
    fun over(o: MFrame) {
        for (i in 0 until MCELLS) {
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
        c: MCol,
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
        c: MCol,
        k: Float,
    ) {
        r[i] += c.r * k
        g[i] += c.g * k
        b[i] += c.b * k
        if (cov[i] < 1.4f && k > cov[i]) cov[i] = min(1f, k)
    }

    /** Turns the canvas into glyphs and palette colors. */
    fun output(
        grid: Grid,
        palette: MPalette,
    ) {
        for (y in 0 until MROWS) {
            for (x in 0 until MCOLS) {
                val i = y * MCOLS + x
                val c = cov[i]
                if (c > 1.4f) {
                    val o = ch[i]
                    grid.glyphs[i] =
                        if (o != M_NONE) {
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
                if (ch[i] != M_NONE && c > 0.05f) {
                    grid.glyphs[i] = ch[i]
                    grid.colors[i] = palette.pick(r[i], g[i], b[i])
                    continue
                }
                val q = c * 3f + M_BAYER[(y and 3) * 4 + (x and 3)]
                val idx = min(3, (q + 0.5f).toInt())
                if (idx <= 0) {
                    grid.glyphs[i] = ' '
                    grid.colors[i] = 0
                } else {
                    grid.glyphs[i] = M_HALFTONE[idx]
                    grid.colors[i] = palette.pick(r[i], g[i], b[i])
                }
            }
        }
    }

    companion object {
        const val SOLID = 2f
    }
}

/** [t] wrapped into 0 until [loop], so a piece repeats exactly every [loop] seconds. */
internal fun mLoopTime(
    t: Float,
    loop: Float,
): Float = t - floor(t / loop) * loop

/** A random stream for [seed], kept apart from other streams of the same seed by [salt]. */
internal fun mRng(
    seed: Int,
    salt: Int,
): Rng = Rng(seed * 1_000_003L + salt * 7919L + 17L)

/** Sets cell [i] to the color [r], [g], [b] with [coverage] and [glyph] (zero for no glyph override). */
internal fun MFrame.set(
    i: Int,
    r: Float,
    g: Float,
    b: Float,
    coverage: Float,
    glyph: Char = 0.toChar(),
) {
    this.r[i] = r
    this.g[i] = g
    this.b[i] = b
    cov[i] = coverage
    ch[i] = glyph
}

/** Writes [text] left to right from cell [x], [y] in color [c] over a solid [back] so it reads on any field. */
internal fun MFrame.text(
    x: Int,
    y: Int,
    text: String,
    c: MCol,
    back: MCol? = null,
) {
    for (k in text.indices) {
        val cx = x + k
        if (cx < 0 || cx >= MCOLS || y < 0 || y >= MROWS) continue
        val ch = text[k]
        if (ch == ' ') {
            if (back != null) put(cx, y, back, 2f, ' ')
            continue
        }
        put(cx, y, c, 2f, ch)
    }
}
