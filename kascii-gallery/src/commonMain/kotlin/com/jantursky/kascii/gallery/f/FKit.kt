package com.jantursky.kascii.gallery

import com.jantursky.kascii.core.Grid
import kotlin.math.max
import kotlin.math.min

internal const val F_COLS = 192
internal const val F_ROWS = 48
internal const val F_CELLS = F_COLS * F_ROWS

/** How much taller than wide a cell is drawn; a round shape is [F_ASPECT] times wider than tall in cells. */
internal const val F_ASPECT = 2.2f

private val BAYER =
    FloatArray(16) {
        intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)[it].let { v -> (v + 0.5f) / 16f - 0.5f }
    }

private val LEVELS = floatArrayOf(0.30f, 0.42f, 0.54f, 0.68f, 0.84f, 1.0f)

/** Cells that are glow, haze or sky: a sparse halftone of dots. */
internal const val F_HALF: Byte = 0

/** Cells that are a solid shape: dense blocks. */
internal const val F_DENSE: Byte = 1

/**
 * A palette of hue families, each offered at six brightness levels, with a 5 bits per channel lookup table to the
 * nearest entry. Built once per scene.
 */
internal class FPalette(
    private val hues: Array<FloatArray>,
) {
    val argb: IntArray =
        IntArray(hues.size * LEVELS.size) {
            val h = hues[it / LEVELS.size]
            val l = LEVELS[it % LEVELS.size]
            (0xFF shl 24) or ((h[0] * l * 255f).toInt() shl 16) or ((h[1] * l * 255f).toInt() shl 8) or
                (h[2] * l * 255f).toInt()
        }

    val lut: ByteArray =
        ByteArray(32768) { key ->
            val r = ((key shr 10) and 31) / 31f
            val g = ((key shr 5) and 31) / 31f
            val b = (key and 31) / 31f
            var best = 0
            var bestD = Float.MAX_VALUE
            for (p in argb.indices) {
                val h = hues[p / LEVELS.size]
                val l = LEVELS[p % LEVELS.size]
                val dr = h[0] * l - r
                val dg = h[1] * l - g
                val db = h[2] * l - b
                val d = 0.3f * dr * dr + 0.5f * dg * dg + 0.2f * db * db
                if (d < bestD) {
                    bestD = d
                    best = p
                }
            }
            best.toByte()
        }
}

/**
 * Per-cell float colour with a kind (halftone or dense) and an optional fixed glyph. A scene paints into it each
 * frame and [write] turns it into glyphs and palette colours with Bayer dithering.
 */
internal class FCanvas(
    private val palette: FPalette,
) {
    val r = FloatArray(F_CELLS)
    val g = FloatArray(F_CELLS)
    val b = FloatArray(F_CELLS)
    val kind = ByteArray(F_CELLS)
    val glyph = CharArray(F_CELLS)

    fun load(
        baseR: FloatArray,
        baseG: FloatArray,
        baseB: FloatArray,
        baseKind: ByteArray,
    ) {
        baseR.copyInto(r)
        baseG.copyInto(g)
        baseB.copyInto(b)
        baseKind.copyInto(kind)
        glyph.fill(0.toChar())
    }

    fun add(
        i: Int,
        cr: Float,
        cg: Float,
        cb: Float,
    ) {
        r[i] += cr
        g[i] += cg
        b[i] += cb
    }

    /** Paints a solid cell, replacing whatever was there. */
    fun solid(
        i: Int,
        cr: Float,
        cg: Float,
        cb: Float,
        ch: Char = 0.toChar(),
    ) {
        r[i] = cr
        g[i] = cg
        b[i] = cb
        kind[i] = F_DENSE
        glyph[i] = ch
    }

    /** Blanks a cell to the ground, a silhouette. */
    fun cut(i: Int) {
        r[i] = 0f
        g[i] = 0f
        b[i] = 0f
        kind[i] = F_DENSE
        glyph[i] = ' '
    }

    fun write(out: Grid) {
        val glyphs = out.glyphs
        val colors = out.colors
        val lut = palette.lut
        for (y in 0 until F_ROWS) {
            for (x in 0 until F_COLS) {
                val i = y * F_COLS + x
                val cr = r[i]
                val cg = g[i]
                val cb = b[i]
                val m = max(cr, max(cg, cb))
                var ch = glyph[i]
                if (ch == 0.toChar()) {
                    val bay = BAYER[(y and 3) * 4 + (x and 3)]
                    ch =
                        if (kind[i] == F_DENSE) {
                            val v = m + bay * 0.12f
                            if (v < 0.05f) {
                                ' '
                            } else if (v < 0.2f) {
                                '░'
                            } else if (v < 0.4f) {
                                '▒'
                            } else if (v < 0.68f) {
                                '▓'
                            } else {
                                '█'
                            }
                        } else {
                            val v = m + bay * 0.16f
                            if (v < 0.06f) {
                                ' '
                            } else if (v < 0.2f) {
                                '·'
                            } else if (v < 0.36f) {
                                '•'
                            } else if (v < 0.62f) {
                                '●'
                            } else if (v < 0.9f) {
                                '▒'
                            } else {
                                '▓'
                            }
                        }
                }
                if (ch == ' ') {
                    glyphs[i] = ' '
                    colors[i] = 0
                    continue
                }
                val mm = if (m < 0.001f) 0.001f else m
                val inv = min(1f, 0.30f + 1.1f * mm) / mm
                val key =
                    (min(31, (cr * inv * 31.99f).toInt()) shl 10) or
                        (min(31, (cg * inv * 31.99f).toInt()) shl 5) or
                        min(31, (cb * inv * 31.99f).toInt())
                glyphs[i] = ch
                colors[i] = palette.argb[lut[key].toInt()]
            }
        }
    }
}

/** A soft bump, 1 at the centre of a source and 0 at [radius], smooth in between and cheap (no exp). */
internal fun fGlow(
    d2: Float,
    radius: Float,
): Float {
    val q = d2 / (radius * radius)
    if (q >= 1f) return 0f
    val k = 1f - q
    return k * k
}
