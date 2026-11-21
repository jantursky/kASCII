package com.jantursky.kascii.gallery.l

import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.Noise
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.Rng
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

internal const val LCOLS = 192
internal const val LROWS = 48
internal const val LCELLS = LCOLS * LROWS

/** One cell is 2.2 times taller than wide, so a row is this many width units. */
internal const val L_ROW_U = 2.2f
internal const val LTAU = 6.2831855f

/** Halftone glyphs of a gradient, from empty to dense. */
internal const val L_HALFTONE = " ·•●"

private const val L_NONE = 0.toChar()

/** A color as floats in 0..1, used for blending before the final snap to a palette. */
internal class LCol(
    val r: Float,
    val g: Float,
    val b: Float,
) {
    fun mix(
        o: LCol,
        t: Float,
    ): LCol = LCol(r + (o.r - r) * t, g + (o.g - g) * t, b + (o.b - b) * t)

    fun scale(k: Float): LCol = LCol(r * k, g * k, b * k)
}

internal fun lcol(hex: String): LCol = Rgb.fromHex(hex).let { LCol(it.red / 255f, it.green / 255f, it.blue / 255f) }

internal fun lramp(
    steps: Int,
    vararg stops: LCol,
): List<LCol> =
    (0 until steps).map { k ->
        val p = k.toFloat() / (steps - 1) * (stops.size - 1)
        val i = min(p.toInt(), stops.size - 2)
        stops[i].mix(stops[i + 1], p - i)
    }

internal fun lclamp01(v: Float): Float =
    if (v < 0f) {
        0f
    } else if (v > 1f) {
        1f
    } else {
        v
    }

internal fun lsmooth(
    a: Float,
    b: Float,
    v: Float,
): Float {
    val k = lclamp01((v - a) / (b - a))
    return k * k * (3f - 2f * k)
}

internal fun lgauss(
    x: Float,
    c: Float,
    w: Float,
): Float = exp(-((x - c) / w) * ((x - c) / w))

/** An int in [lo], [hi) from [this]. */
internal fun Rng.intIn(
    lo: Int,
    hi: Int,
): Int = lo + int(hi - lo)

internal fun lfract(v: Float): Float = v - floor(v)

internal val L_BAYER: FloatArray =
    floatArrayOf(0f, 8f, 2f, 10f, 12f, 4f, 14f, 6f, 3f, 11f, 1f, 9f, 15f, 7f, 13f, 5f)
        .map { (it + 0.5f) / 16f - 0.5f }
        .toFloatArray()

/** A tiling table of value noise, so a lookup is two lerps instead of eight hashes. */
internal class LLattice(
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
internal class LPalette(
    colors: List<LCol>,
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
            ((lclamp01(r) * 31.99f).toInt() shl 10) or
                ((lclamp01(g) * 31.99f).toInt() shl 5) or
                (lclamp01(b) * 31.99f).toInt(),
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
internal class LFrame {
    val r = FloatArray(LCELLS)
    val g = FloatArray(LCELLS)
    val b = FloatArray(LCELLS)
    val cov = FloatArray(LCELLS)
    val ch = CharArray(LCELLS)

    fun clear() {
        r.fill(0f)
        g.fill(0f)
        b.fill(0f)
        cov.fill(0f)
        ch.fill(L_NONE)
    }

    fun copyFrom(o: LFrame) {
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
        c: LCol,
        coverage: Float = SOLID,
        glyph: Char = L_NONE,
    ) {
        if (x < 0 || x >= LCOLS || y < 0 || y >= LROWS) return
        val i = y * LCOLS + x
        r[i] = c.r
        g[i] = c.g
        b[i] = c.b
        cov[i] = coverage
        ch[i] = glyph
    }

    /** Lays every cell of [o] that has coverage over this canvas. */
    fun over(o: LFrame) {
        for (i in 0 until LCELLS) {
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
        c: LCol,
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
        c: LCol,
        k: Float,
    ) {
        r[i] += c.r * k
        g[i] += c.g * k
        b[i] += c.b * k
        if (cov[i] < 1.4f && k > cov[i]) cov[i] = min(1f, k)
    }

    /** Turns the canvas into glyphs and palette colors, jittering each color by [dither] of the Bayer matrix. */
    fun output(
        grid: Grid,
        palette: LPalette,
        dither: Float = 0f,
    ) {
        for (y in 0 until LROWS) {
            for (x in 0 until LCOLS) {
                val i = y * LCOLS + x
                val c = cov[i]
                val d = L_BAYER[(y and 3) * 4 + (x and 3)] * dither
                r[i] += d
                g[i] += d
                b[i] += d
                if (c > 1.4f) {
                    val o = ch[i]
                    grid.glyphs[i] =
                        if (o != L_NONE) {
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
                if (ch[i] != L_NONE && c > 0.05f) {
                    grid.glyphs[i] = ch[i]
                    grid.colors[i] = palette.pick(r[i], g[i], b[i])
                    continue
                }
                val q = c * 3f + L_BAYER[(y and 3) * 4 + (x and 3)]
                val idx = min(3, (q + 0.5f).toInt())
                if (idx <= 0) {
                    grid.glyphs[i] = ' '
                    grid.colors[i] = 0
                } else {
                    grid.glyphs[i] = L_HALFTONE[idx]
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
internal fun lLoopTime(
    t: Float,
    loop: Float,
): Float = t - floor(t / loop) * loop

/** A random stream for [seed], kept apart from other streams of the same seed by [salt]. */
internal fun lRng(
    seed: Int,
    salt: Int,
): Rng = Rng(seed * 1_000_003L + salt * 7919L + 17L)

/** Sets cell [i] to the color [r], [g], [b] with [coverage] and [glyph] (zero for no glyph override). */
internal fun LFrame.set(
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
internal fun LFrame.text(
    x: Int,
    y: Int,
    text: String,
    c: LCol,
    back: LCol? = null,
) {
    for (k in text.indices) {
        val cx = x + k
        if (cx < 0 || cx >= LCOLS || y < 0 || y >= LROWS) continue
        val ch = text[k]
        if (ch == ' ') {
            if (back != null) put(cx, y, back, 2f, ' ')
            continue
        }
        put(cx, y, c, 2f, ch)
    }
}

/** A depth buffer over the grid: smaller is nearer. */
internal class LDepth {
    val z = FloatArray(LCELLS)

    fun clear() {
        z.fill(Float.MAX_VALUE)
    }

    /** True and records [depth] when [depth] is nearer than what cell [i] holds. */
    fun test(
        i: Int,
        depth: Float,
    ): Boolean {
        if (depth >= z[i]) return false
        z[i] = depth
        return true
    }
}

/** The cell index of the point [x], [y] in width units, or -1 when it falls off the grid. */
internal fun lcell(
    x: Float,
    y: Float,
): Int {
    val cx = floor(x).toInt()
    val cy = floor(y / L_ROW_U).toInt()
    if (cx < 0 || cx >= LCOLS || cy < 0 || cy >= LROWS) return -1
    return cy * LCOLS + cx
}

/** A line glyph for a stroke of direction [dx], [dy] in width units. */
internal fun lstroke(
    dx: Float,
    dy: Float,
): Char {
    val ax = kotlin.math.abs(dx)
    val ay = kotlin.math.abs(dy)
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
