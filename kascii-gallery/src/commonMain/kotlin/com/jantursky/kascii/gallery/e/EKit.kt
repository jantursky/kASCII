package com.jantursky.kascii.gallery

import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.Noise
import com.jantursky.kascii.core.Rgb
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

internal const val E_COLS = 192
internal const val E_ROWS = 48
internal const val E_CELLS = E_COLS * E_ROWS
internal const val E_TAU = 6.2831855f

/** A glyph cell is 2.2 times taller than wide, so one row is this many columns. */
internal const val E_ASPECT = 2.2f

internal val E_BAYER =
    floatArrayOf(0f, 8f, 2f, 10f, 12f, 4f, 14f, 6f, 3f, 11f, 1f, 9f, 15f, 7f, 13f, 5f)
        .map { (it + 0.5f) / 16f - 0.5f }
        .toFloatArray()

/** A colour with float channels from 0 to 1. */
internal class ECol(
    val r: Float,
    val g: Float,
    val b: Float,
) {
    fun mix(
        o: ECol,
        t: Float,
    ): ECol = ECol(r + (o.r - r) * t, g + (o.g - g) * t, b + (o.b - b) * t)
}

internal fun eCol(hex: String): ECol = Rgb.fromHex(hex).let { ECol(it.red / 255f, it.green / 255f, it.blue / 255f) }

/** [steps] colours spread evenly along the piecewise linear path through [stops]. */
internal fun eRamp(
    steps: Int,
    vararg stops: ECol,
): List<ECol> =
    (0 until steps).map { k ->
        val p = k.toFloat() / (steps - 1) * (stops.size - 1)
        val i = min(p.toInt(), stops.size - 2)
        stops[i].mix(stops[i + 1], p - i)
    }

internal fun eClamp(v: Float): Float =
    if (v < 0f) {
        0f
    } else if (v > 1f) {
        1f
    } else {
        v
    }

internal fun eSmooth(
    a: Float,
    b: Float,
    v: Float,
): Float {
    val k = eClamp((v - a) / (b - a))
    return k * k * (3f - 2f * k)
}

internal fun eFract(v: Float): Float = v - floor(v)

/** A tiling table of value noise, so a lookup is two lerps instead of eight hashes. */
internal class ELattice(
    seed: Int,
) {
    private val table = FloatArray(256 * 256) { Noise.hash(it and 255, it shr 8, 0, seed) }

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

/** The colours a scene can show, with a 5 bits per channel table that snaps any computed colour to the nearest. */
internal class EPalette(
    colors: List<ECol>,
) {
    private val lut = IntArray(32768)

    init {
        val argb =
            IntArray(colors.size) {
                Rgb(
                    (colors[it].r * 255f).roundToInt(),
                    (colors[it].g * 255f).roundToInt(),
                    (colors[it].b * 255f).roundToInt(),
                ).argb
            }
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
            ((eClamp(r) * 31.99f).toInt() shl 10) or
                ((eClamp(g) * 31.99f).toInt() shl 5) or
                (eClamp(b) * 31.99f).toInt(),
        ]
}

/** Per cell colour and coverage of one frame. Coverage up to 1 is a dithered halftone, above 1.5 a solid block. */
internal class EFrame {
    val r = FloatArray(E_CELLS)
    val g = FloatArray(E_CELLS)
    val b = FloatArray(E_CELLS)
    val cov = FloatArray(E_CELLS)

    fun clear() {
        r.fill(0f)
        g.fill(0f)
        b.fill(0f)
        cov.fill(0f)
    }

    /** Adds light to a cell and raises its coverage by [cover]. */
    fun add(
        i: Int,
        red: Float,
        green: Float,
        blue: Float,
        cover: Float,
    ) {
        r[i] += red
        g[i] += green
        b[i] += blue
        cov[i] += cover
    }

    fun set(
        i: Int,
        red: Float,
        green: Float,
        blue: Float,
        cover: Float,
    ) {
        r[i] = red
        g[i] = green
        b[i] = blue
        cov[i] = cover
    }
}

private const val HALFTONE = " ·•●"

/** Turns [f] into glyphs: a four level dithered halftone below coverage 1.5, '▓' and '█' blocks above. */
internal fun eOutput(
    f: EFrame,
    grid: Grid,
    palette: EPalette,
) {
    for (y in 0 until E_ROWS) {
        for (x in 0 until E_COLS) {
            val i = y * E_COLS + x
            val c = f.cov[i]
            if (c > 1.5f) {
                grid.glyphs[i] = if (c > 1.95f) '█' else '▓'
                grid.colors[i] = palette.pick(f.r[i], f.g[i], f.b[i])
                continue
            }
            val q = c * 3f + E_BAYER[(y and 3) * 4 + (x and 3)]
            val idx = min(3, (q + 0.5f).toInt())
            if (idx <= 0) {
                grid.glyphs[i] = ' '
                grid.colors[i] = 0
            } else {
                grid.glyphs[i] = HALFTONE[idx]
                grid.colors[i] = palette.pick(f.r[i], f.g[i], f.b[i])
            }
        }
    }
}

/** Writes one glyph straight into the grid, after [eOutput], for small sharp details on top. */
internal fun ePut(
    grid: Grid,
    palette: EPalette,
    x: Int,
    y: Int,
    glyph: Char,
    r: Float,
    g: Float,
    b: Float,
) {
    if (x !in 0 until E_COLS || y !in 0 until E_ROWS) return
    val i = y * E_COLS + x
    grid.glyphs[i] = glyph
    grid.colors[i] = palette.pick(r, g, b)
}
