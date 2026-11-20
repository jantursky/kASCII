package com.jantursky.kascii.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.AsciiPieceView
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.Noise
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.asciiPiece
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val COLS = 192
private const val ROWS = 48
private const val CELLS = COLS * ROWS
private const val LOOP = 20f
private const val WATER = 34
private const val TAU = AKit.TAU
private const val ASPECT = 2.2f
private const val PETALS = 110
private const val STARS = 110
private const val GLINTS = 45
private const val MOON_X = 128f
private const val MOON_Y = 11.5f
private const val MOON_R = 17.6f
private const val PAGODA_X = 166
private const val MIST_PERIOD = 5

private const val SKY = 0
private const val MOON = 1
private const val OTHER = 2
private const val CANOPY = 3

private const val GLYPHS = " ·•●"

private val BAYER =
    floatArrayOf(0f, 8f, 2f, 10f, 12f, 4f, 14f, 6f, 3f, 11f, 1f, 9f, 15f, 7f, 13f, 5f)
        .map { (it + 0.5f) / 16f - 0.5f }
        .toFloatArray()

/** The default blossom color: a mid pink. */
private val Blossom: Rgb = Rgb.fromHex("#f06aa8")

private class CbCol(
    val r: Float,
    val g: Float,
    val b: Float,
) {
    fun mix(
        o: CbCol,
        t: Float,
    ): CbCol = CbCol(r + (o.r - r) * t, g + (o.g - g) * t, b + (o.b - b) * t)
}

private fun col(hex: String): CbCol = col(Rgb.fromHex(hex))

private fun col(c: Rgb): CbCol = CbCol(c.red / 255f, c.green / 255f, c.blue / 255f)

private fun ramp(
    steps: Int,
    vararg stops: CbCol,
): List<CbCol> =
    (0 until steps).map { k ->
        val p = k.toFloat() / (steps - 1) * (stops.size - 1)
        val i = min(p.toInt(), stops.size - 2)
        stops[i].mix(stops[i + 1], p - i)
    }

private fun List<CbCol>.at(t: Float): CbCol = this[(clamp01(t) * (size - 0.01f)).toInt()]

private fun clamp01(v: Float): Float =
    if (v < 0f) {
        0f
    } else if (v > 1f) {
        1f
    } else {
        v
    }

private fun smooth(
    a: Float,
    b: Float,
    v: Float,
): Float {
    val k = clamp01((v - a) / (b - a))
    return k * k * (3f - 2f * k)
}

private fun gauss(
    x: Float,
    c: Float,
    w: Float,
): Float = exp(-((x - c) / w) * ((x - c) / w))

private fun fract(v: Float): Float = v - floor(v)

/** A tiling table of value noise, so a lookup is two lerps instead of eight hashes. */
private class CbLattice(
    seed: Int,
) {
    private val table = FloatArray(256 * 256) { Noise.hash(it and 255, it shr 8, 0, seed) }

    /** Noise at [x], [y]; with [period] above zero the x axis repeats every [period] lattice units. */
    fun at(
        x: Float,
        y: Float,
        period: Int = 0,
    ): Float {
        val xf = floor(x)
        val yf = floor(y)
        val fx = (x - xf).let { it * it * (3f - 2f * it) }
        val fy = (y - yf).let { it * it * (3f - 2f * it) }
        val xi = xf.toInt()
        val x0 = if (period > 0) ((xi % period) + period) % period else xi and 255
        val x1 = if (period > 0) (x0 + 1) % period else (xi + 1) and 255
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
        period: Int = 0,
    ): Float {
        var sum = 0f
        var amp = 0.5f
        var norm = 0f
        var f = 1f
        var p = period
        repeat(octaves) {
            sum += amp * at(x * f, y * f, p)
            norm += amp
            amp *= 0.5f
            f *= 2f
            p *= 2
        }
        return sum / norm
    }
}

/** The colours the scene can show, with a 5 bits per channel table that snaps any computed colour to the nearest. */
private class CbPalette(
    colors: List<CbCol>,
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
    ): Int = Rgb((r * 255f).roundToInt(), (g * 255f).roundToInt(), (b * 255f).roundToInt()).argb
}

// Canopy clusters: centre x, centre row, half height in rows (the half width is 2.7 times that).
private val CLUSTERS =
    arrayOf(
        floatArrayOf(41f, 8f, 5.5f),
        floatArrayOf(24f, 11f, 4.5f),
        floatArrayOf(58f, 11f, 4.8f),
        floatArrayOf(72f, 13f, 4f),
        floatArrayOf(10f, 18f, 3.5f),
        floatArrayOf(30f, 16f, 4f),
        floatArrayOf(48f, 16f, 4.2f),
        floatArrayOf(66f, 19f, 3.4f),
        floatArrayOf(80f, 19f, 3f),
        floatArrayOf(20f, 5.5f, 3f),
        floatArrayOf(52f, 4f, 3f),
        floatArrayOf(36f, 3.5f, 2.6f),
        floatArrayOf(66f, 6f, 3f),
        floatArrayOf(14f, 12f, 3f),
        floatArrayOf(24f, 20f, 2.6f),
        floatArrayOf(40f, 20f, 3f),
        floatArrayOf(56f, 21f, 2.6f),
        floatArrayOf(78f, 10f, 2.8f),
        floatArrayOf(30f, 6f, 3.2f),
        floatArrayOf(46f, 12f, 3.6f),
    )

// Trunk and boughs: x, row pairs, then radius at the start and at the tip (in columns).
private class CbBough(
    val pts: FloatArray,
    val r0: Float,
    val r1: Float,
)

private val BOUGHS =
    listOf(
        CbBough(floatArrayOf(49f, 33f, 45f, 29f, 41f, 26.5f, 42f, 23f, 39f, 20.5f), 7f, 3.4f),
        CbBough(floatArrayOf(41f, 26.5f, 33f, 24f, 26f, 22.5f, 18f, 21f, 11f, 22f), 3.4f, 1.3f),
        CbBough(floatArrayOf(44f, 27f, 53f, 24.5f, 60f, 22f, 68f, 21.5f, 76f, 22.5f), 3.4f, 1.3f),
        CbBough(floatArrayOf(39f, 20.5f, 33f, 16f, 28f, 12f, 22f, 10f), 3f, 1.3f),
        CbBough(floatArrayOf(39f, 20.5f, 40f, 14f, 38f, 8f, 41f, 4.5f), 2.6f, 1.3f),
        CbBough(floatArrayOf(42f, 23f, 50f, 18f, 58f, 14f, 66f, 11f, 73f, 9f), 2.6f, 1.3f),
        CbBough(floatArrayOf(60f, 22f, 64f, 26f, 66f, 30f), 1.5f, 1.2f),
        CbBough(floatArrayOf(33f, 24f, 30f, 28f, 31f, 31f), 1.5f, 1.2f),
    )

private class CbScene(
    private val seed: Int,
    blossom: Rgb,
    private val wind: Float,
) {
    private val lat = CbLattice(seed)

    // Static layer, built once.
    private val br = FloatArray(CELLS)
    private val bg = FloatArray(CELLS)
    private val bb = FloatArray(CELLS)
    private val bc = FloatArray(CELLS)
    private val kind = ByteArray(CELLS)

    // Per frame layer.
    private val fr = FloatArray(CELLS)
    private val fg = FloatArray(CELLS)
    private val fb = FloatArray(CELLS)
    private val fc = FloatArray(CELLS)

    private val blossomCol = col(blossom)
    private val pinkR =
        ramp(
            12,
            col("#26103a"),
            blossomCol.mix(col("#40104a"), 0.55f),
            blossomCol,
            blossomCol.mix(col("#ffffff"), 0.55f),
            col("#fff4fa"),
        )
    private val mauveR = ramp(8, col("#3a1458"), col("#8a2a8a"), col("#d44a9c"))
    private val violetR = ramp(7, col("#160f3a"), col("#3a2780"), col("#8a6acf"))
    private val skyR = ramp(8, col("#100d30"), col("#2e2268"), col("#7a55b0"))
    private val hazeR = ramp(6, col("#7a55b0"), col("#c28ad8"), col("#f0c4e4"))
    private val moonR = ramp(9, col("#5e5c94"), col("#b4b4dc"), col("#fbf9ff"))
    private val trunkR = ramp(8, col("#0a0510"), col("#1c1024"), col("#7a3c68"), col("#f0a8c4"))
    private val ridgeR = ramp(7, col("#100e2c"), col("#2a2260"), col("#4a3c8c"), col("#7a68c0"))
    private val waterR = ramp(9, col("#050e1a"), col("#0e3040"), col("#23807f"), col("#8ae8d8"))
    private val stoneR = ramp(5, col("#131a2c"), col("#2d3a52"), col("#6a7a98"))
    private val warmR = ramp(6, col("#5a2a10"), col("#ff9a3a"), col("#ffe8b0"))
    private val roseR = ramp(6, col("#2a0c2e"), col("#8a2858"), col("#f06a98"))
    private val mistR = ramp(6, col("#2a2450"), col("#6a5aa0"), col("#c0b4e8"))
    private val mistCol = col("#a898dc")
    private val wDeep = col("#071626")
    private val wMid = col("#12404d")
    private val moonPale = col("#dcdcff")
    private val palette =
        CbPalette(
            pinkR + mauveR + violetR + skyR + hazeR + moonR + trunkR + ridgeR + waterR + stoneR + warmR + mistR +
                roseR,
        )

    private val canopyCells = ArrayList<Int>()
    private val litCells = ArrayList<Int>()
    private val windows = ArrayList<Int>()

    private val starIdx = IntArray(STARS)
    private val starPhase = FloatArray(STARS)
    private val starRate = IntArray(STARS)
    private val starPower = FloatArray(STARS)
    private val glintIdx = IntArray(GLINTS)
    private val glintPhase = FloatArray(GLINTS)
    private val glintRate = IntArray(GLINTS)

    // Per petal: start x, start row, end row, drift, sway, phase, laps, lightness.
    private val petal = Array(PETALS) { FloatArray(8) }

    init {
        buildSky()
        buildRidges()
        buildPagoda()
        buildTorii()
        buildBanks()
        for (b in BOUGHS) bough(b)
        buildCanopy()
        placeStars()
        placeGlints()
        placePetals()
    }

    private fun put(
        x: Int,
        y: Int,
        c: CbCol,
        cov: Float,
        k: Int,
    ) {
        if (x !in 0 until COLS || y !in 0 until ROWS) return
        val i = y * COLS + x
        br[i] = c.r
        bg[i] = c.g
        bb[i] = c.b
        bc[i] = cov
        kind[i] = k.toByte()
    }

    // ---- static layers ---------------------------------------------------------------------------------------

    private fun buildSky() {
        val indigo = skyR[0]
        val horizon = col("#e0a0d8")
        val lilac = col("#b8a8f0")
        for (y in 0 until WATER) {
            val t = (y + 0.5f) / WATER
            for (x in 0 until COLS) {
                val px = x + 0.5f
                val py = (y + 0.5f) * ASPECT
                val dx = px - MOON_X
                val dy = py - MOON_Y * ASPECT
                val dm = sqrt(dx * dx + dy * dy)
                if (dm < MOON_R) {
                    val n = lat.fbm(px * 0.05f + 7f, py * 0.05f + 3f, 3)
                    val dark = smooth(0.5f, 0.62f, n)
                    val limb = (dm / MOON_R).let { it * it * it }
                    val lit = 0.82f - 0.3f * dark - 0.22f * limb + 0.1f * (Noise.hash(x, y, 5, seed) - 0.5f)
                    put(x, y, moonR.at(lit), if (dm < MOON_R - 1.4f) 2f else 1.8f, MOON)
                    continue
                }
                val halo = exp(-(dm - MOON_R) / 15f)
                var c = indigo.mix(skyR[4], smooth(0.05f, 0.85f, t))
                c = c.mix(hazeR[1], smooth(0.75f, 1f, t) * 0.6f).mix(horizon, smooth(0.9f, 1f, t) * 0.3f)
                c = c.mix(lilac, halo * 0.55f)
                put(x, y, c, 0.5f * t * t + 0.5f * halo * halo, SKY)
            }
        }
    }

    private fun ridgeBack(x: Int): Float {
        val n = lat.fbm(x * 0.022f + 4f, 1.3f, 3)
        val m = lat.fbm(x * 0.09f + 2f, 8f, 2)
        return 27.5f - 5f * gauss(x.toFloat(), 78f, 12f) - 3f * gauss(x.toFloat(), 118f, 9f) -
            3f * gauss(x.toFloat(), 150f, 10f) + 7f * (n - 0.5f) + 5f * (m - 0.5f)
    }

    private fun ridgeFront(x: Int): Float {
        val n = lat.fbm(x * 0.03f + 9f, 5.1f, 3)
        return 30.2f - 2.5f * gauss(x.toFloat(), 118f, 26f) + 3f * (n - 0.5f)
    }

    private fun buildRidges() {
        for (x in 0 until COLS) {
            val hb = ridgeBack(x).roundToInt()
            for (y in hb until WATER) {
                val d = (y - hb) / 8f
                put(x, y, ridgeR.at(0.5f + 0.45f * d), 1.6f, OTHER)
            }
            val hf = ridgeFront(x).roundToInt()
            for (y in hf until WATER) {
                put(x, y, ridgeR.at(0.12f + 0.2f * (y - hf) / 4f), 1.85f, OTHER)
            }
        }
    }

    private fun buildBanks() {
        for (x in 0 until COLS) {
            val mound = (1.4f * gauss(x.toFloat(), 46f, 12f)).roundToInt()
            val left =
                if (x < 66) {
                    32 - mound
                } else if (x < 80) {
                    33
                } else {
                    WATER
                }
            val right =
                if (x >= 140) {
                    32
                } else if (x >= 134) {
                    33
                } else {
                    WATER
                }
            val top = min(left, right)
            for (y in top until WATER) {
                val h = Noise.hash(x, y, 11, seed)
                val rim = if (y == top) 0.34f else 0.12f
                val c = violetR.at(rim + 0.1f * h)
                put(x, y, c, if (h > 0.78f) 1.85f else 2f, OTHER)
            }
        }
    }

    private fun side(
        x: Int,
        cx: Int,
        hw: Float,
    ): Float = clamp01(0.5f + (cx - x) / (2f * hw))

    private fun span(
        y: Int,
        cx: Int,
        hw: Float,
        shade: (Int) -> CbCol,
        cov: Float,
    ) {
        val x0 = (cx - hw + 0.5f).toInt()
        val x1 = (cx + hw + 0.5f).toInt()
        for (x in x0..x1) put(x, y, shade(x), cov, OTHER)
    }

    private fun buildPagoda() {
        val cx = PAGODA_X
        span(30, cx, 15f, { stoneR.at(0.25f + 0.4f * side(it, cx, 15f)) }, 2f)
        span(31, cx, 17f, { stoneR.at(0.15f + 0.4f * side(it, cx, 17f)) }, 2f)
        for (tier in 0..2) {
            val hwBody = 8f - 1.5f * tier
            val yb = 29 - 5 * tier
            for (y in yb - 1..yb) {
                span(
                    y,
                    cx,
                    hwBody,
                    { x ->
                        val pillar = if ((x - cx) % 4 == 0) 0f else 0.06f
                        violetR.at(0.12f + pillar + 0.3f * side(x, cx, hwBody))
                    },
                    2f,
                )
            }
            val offs = if (hwBody >= 7f) intArrayOf(-5, -1, 3) else intArrayOf(-3, 1)
            for (o in offs) {
                for (y in yb - 1..yb) {
                    for (x in cx + o..cx + o + 1) {
                        put(x, y, warmR.at(0.7f), 2f, OTHER)
                        windows.add(y * COLS + x)
                    }
                }
            }
            val hwTop = hwBody + 1f
            val hwBot = hwBody + 7f
            val top = yb - 4
            for (j in 0..2) {
                val hw = hwTop + (hwBot - hwTop) * j / 2f
                span(
                    top + j,
                    cx,
                    hw,
                    { x ->
                        val s = side(x, cx, hwBot)
                        violetR.at(0.2f + 0.45f * s * s + 0.1f * (2 - j) / 2f)
                    },
                    if (j == 2) 1.85f else 2f,
                )
            }
            val tip = (hwBot + 0.5f).toInt()
            for (o in tip - 1..tip + 2) {
                put(cx - o, top + 1, violetR.at(0.55f), 2f, OTHER)
                put(cx + o, top + 1, violetR.at(0.2f), 2f, OTHER)
            }
            put(cx - tip - 2, top, violetR.at(0.6f), 2f, OTHER)
            put(cx + tip + 2, top, violetR.at(0.25f), 2f, OTHER)
        }
        for (y in 8..14) {
            put(cx - 1, y, violetR.at(0.5f), 2f, OTHER)
            put(cx, y, violetR.at(0.2f), 2f, OTHER)
        }
        for (y in intArrayOf(10, 12)) {
            put(cx - 2, y, violetR.at(0.55f), 2f, OTHER)
            put(cx + 1, y, violetR.at(0.25f), 2f, OTHER)
        }
        put(cx - 1, 7, violetR.at(0.6f), 1.85f, OTHER)
        put(cx, 7, violetR.at(0.3f), 1.85f, OTHER)
    }

    private fun buildTorii() {
        val cx = 100
        for (y in 27..33) {
            for (o in intArrayOf(-7, 6)) {
                put(cx + o, y, roseR.at(0.55f - 0.1f * (y - 27) / 6f), 2f, OTHER)
                put(cx + o + 1, y, roseR.at(0.3f), 2f, OTHER)
            }
        }
        for (x in cx - 8..cx + 8) {
            put(x, 29, roseR.at(0.4f), 2f, OTHER)
            put(x, 30, roseR.at(0.3f), 1.85f, OTHER)
        }
        for (x in cx - 11..cx + 11) {
            val edge = abs(x - cx) > 9
            put(x, 26, roseR.at(if (edge) 0.75f else 0.5f), 2f, OTHER)
            if (!edge) put(x, 27, roseR.at(0.3f), 1.85f, OTHER)
        }
        for (o in intArrayOf(-12, -13, 12, 13)) put(cx + o, 25, roseR.at(0.7f), 2f, OTHER)
    }

    private fun bough(b: CbBough) {
        val n = b.pts.size / 2 - 1
        for (s in 0 until n) {
            val ra = b.r0 + (b.r1 - b.r0) * s / n
            val rb = b.r0 + (b.r1 - b.r0) * (s + 1) / n
            capsule(b.pts[2 * s], b.pts[2 * s + 1], b.pts[2 * s + 2], b.pts[2 * s + 3], ra, rb)
        }
    }

    private fun capsule(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        ra: Float,
        rb: Float,
    ) {
        val ay = y0 * ASPECT
        val by = y1 * ASPECT
        val rm = max(ra, rb) + 1f
        val minX = floor(min(x0, x1) - rm).toInt()
        val maxX = floor(max(x0, x1) + rm).toInt() + 1
        val minY = floor((min(ay, by) - rm) / ASPECT).toInt()
        val maxY = floor((max(ay, by) + rm) / ASPECT).toInt() + 1
        val abx = x1 - x0
        val aby = by - ay
        val l2 = abx * abx + aby * aby
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val px = x + 0.5f
                val py = (y + 0.5f) * ASPECT
                val h = clamp01(((px - x0) * abx + (py - ay) * aby) / l2)
                val dx = px - (x0 + abx * h)
                val dy = py - (ay + aby * h)
                val r = ra + (rb - ra) * h
                if (dx * dx + dy * dy > r * r) continue
                val rim = ((dx * 0.75f - dy * 0.55f) / max(r, 1f)).coerceIn(-1f, 1f)
                val bark = lat.at(px * 0.5f, py * 0.35f)
                val lit = 0.27f + 0.7f * max(0f, rim) * max(0f, rim) + 0.1f * (bark - 0.5f)
                put(x, y, trunkR.at(lit), if (bark > 0.62f) 1.85f else 2f, OTHER)
            }
        }
    }

    private fun field(
        px: Float,
        py: Float,
    ): Float {
        var best = -1f
        for (c in CLUSTERS) {
            val dx = (px - c[0]) / (c[2] * 2.7f)
            val dy = (py - c[1]) / c[2]
            val v = 1f - sqrt(dx * dx + dy * dy)
            if (v > best) best = v
        }
        return best + (lat.fbm(px * 0.22f, py * 0.5f, 3) - 0.5f) * 0.7f
    }

    private fun buildCanopy() {
        for (y in 0 until 30) {
            for (x in 0 until 100) {
                val px = x + 0.5f
                val py = y + 0.5f
                val f = field(px, py)
                if (f < 0.05f) continue
                val detail = lat.fbm(px * 0.4f + 9f, py * 0.9f + 2f, 2)
                if (f < 0.55f && detail < 0.27f) continue
                val rim = clamp01((field(px - 2.5f, py) - field(px + 2.5f, py)) * 2.2f)
                val lit =
                    0.28f + 0.5f * clamp01(f) + 0.3f * rim + (Noise.hash(x, y, 13, seed) - 0.5f) * 0.25f +
                        0.3f * (lat.at(px * 0.5f + 3f, py * 1.1f) - 0.5f) +
                        0.1f * (0.5f - y / 24f)
                var c = pinkR.at(lit)
                if (lit < 0.5f) c = c.mix(mauveR.at(lit * 1.6f), clamp01((0.5f - lit) * 1.6f))
                val cov =
                    when {
                        f < 0.3f -> 0.45f + (f - 0.05f) * 2f
                        f < 0.45f -> 1.6f
                        f < 0.65f -> 1.85f
                        else -> 2f
                    }
                put(x, y, c, cov, CANOPY)
                val i = y * COLS + x
                canopyCells.add(i)
                if (cov >= 1.6f && lit > 0.6f) litCells.add(i)
            }
        }
    }

    private fun placeStars() {
        val rng = AKit.rng(seed, 41)
        for (s in 0 until STARS) {
            var i: Int
            var tries = 0
            do {
                i = rng.int(COLS) + rng.int(WATER - 8) * COLS
                val dx = i % COLS - MOON_X
                val dy = (i / COLS - MOON_Y) * ASPECT
                tries++
            } while ((kind[i].toInt() != SKY || dx * dx + dy * dy < (MOON_R + 8f) * (MOON_R + 8f)) && tries < 60)
            starIdx[s] = i
            starPhase[s] = rng.float()
            starRate[s] = 1 + rng.int(3)
            starPower[s] = 0.35f + 0.65f * rng.float()
        }
    }

    private fun placeGlints() {
        val rng = AKit.rng(seed, 43)
        for (g in 0 until GLINTS) {
            glintIdx[g] = litCells[rng.int(litCells.size)]
            glintPhase[g] = rng.float()
            glintRate[g] = 1 + rng.int(3)
        }
    }

    private fun placePetals() {
        val rng = AKit.rng(seed, 47)
        for (p in petal) {
            val cell = canopyCells[rng.int(canopyCells.size)]
            p[0] = (cell % COLS).toFloat()
            p[1] = (cell / COLS).toFloat()
            p[2] = rng.float(35f, 46f)
            p[3] = rng.float(18f, 46f) * wind
            p[4] = rng.float(0.8f, 2.4f)
            p[5] = rng.float()
            p[6] = if (rng.chance(0.2f)) 2f else 1f
            p[7] = rng.float(0.78f, 1f)
        }
    }

    // ---- per frame ---------------------------------------------------------------------------------------------

    private fun water(t: Float) {
        val ph = TAU * 2f * t / LOOP
        for (y in WATER until ROWS) {
            val k = (y - WATER) / (ROWS - 1 - WATER).toFloat()
            val sy = (WATER - 0.5f - (y - WATER + 0.5f) * 1.5f).toInt().coerceIn(0, ROWS - 1)
            val amp = 0.6f + 2.4f * k
            for (x in 0 until COLS) {
                val i = y * COLS + x
                val sw = sin(ph + y * 2.3f + x * 0.11f) + 0.5f * sin(2f * ph + y * 1.1f - x * 0.07f)
                val sx = (x + amp * sw).toInt().coerceIn(0, COLS - 1)
                val s = sy * COLS + sx
                val dash = smooth(0.2f, 0.8f, 0.5f + 0.5f * sin(x * 0.42f + y * 3.1f + ph + 2f * sin(x * 0.05f + ph)))
                var r = wDeep.r + (wMid.r - wDeep.r) * k * 0.7f
                var g = wDeep.g + (wMid.g - wDeep.g) * k * 0.7f
                var b = wDeep.b + (wMid.b - wDeep.b) * k * 0.7f
                var cov = 0.04f + 0.16f * k * dash
                val strength = if (kind[s].toInt() == SKY) 0.2f else 1f
                val w = min(bc[s], 1f) * strength * (0.6f + 0.4f * dash) * (1f - 0.35f * k)
                val a = clamp01(w * 1.7f)
                r += (br[s] * 0.8f - r) * a
                g += (bg[s] * 0.85f - g) * a
                b += (bb[s] * 0.9f - b) * a
                cov = max(cov, w)
                val gl = gauss(x.toFloat(), MOON_X + 2f * sin(ph + y), 5f + 14f * k)
                val d2 = smooth(0.35f, 0.8f, 0.5f + 0.5f * sin(y * 3.7f + x * 0.9f + ph * 2f + 3f * sin(x * 0.13f)))
                val m = gl * d2
                r += (moonPale.r - r) * m * 0.9f
                g += (moonPale.g - g) * m * 0.9f
                b += (moonPale.b - b) * m * 0.9f
                cov = min(0.98f, cov + 0.9f * m)
                fr[i] = r
                fg[i] = g
                fb[i] = b
                fc[i] = cov
            }
        }
    }

    private fun mist(t: Float) {
        val shift = MIST_PERIOD * t / LOOP
        for (y in 20..43) {
            val band = gauss(y.toFloat(), 31f, 4.5f)
            if (band < 0.05f) continue
            for (x in 0 until COLS) {
                val n = lat.fbm(x * 0.03f + shift, y * 0.28f, 3, MIST_PERIOD)
                val m = smooth(0.3f, 0.66f, n) * band * 1.1f
                if (m < 0.02f) continue
                val i = y * COLS + x
                val solid = fc[i] > 1.5f
                val a = if (solid) m * 0.4f else m * 0.8f
                if (!solid) fc[i] = min(1f, fc[i] + m * 0.65f)
                fr[i] += (mistCol.r - fr[i]) * a
                fg[i] += (mistCol.g - fg[i]) * a
                fb[i] += (mistCol.b - fb[i]) * a
            }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        br.copyInto(fr, 0, 0, WATER * COLS)
        bg.copyInto(fg, 0, 0, WATER * COLS)
        bb.copyInto(fb, 0, 0, WATER * COLS)
        bc.copyInto(fc, 0, 0, WATER * COLS)
        val phi = TAU * t / LOOP
        for (k in windows.indices) {
            val i = windows[k]
            val e = 0.62f + 0.12f * sin(phi * 3f + k * 1.9f) + 0.08f * sin(phi * 7f + k * 0.7f)
            val c = warmR.at(e)
            fr[i] = c.r
            fg[i] = c.g
            fb[i] = c.b
        }
        water(t)
        mist(t)
        output(grid)
        stars(grid, t)
        glints(grid, t)
        petals(grid, t)
    }

    private fun output(grid: Grid) {
        for (y in 0 until ROWS) {
            for (x in 0 until COLS) {
                val i = y * COLS + x
                val c = fc[i]
                if (c > 1.5f) {
                    grid.glyphs[i] =
                        if (c > 1.95f) {
                            '█'
                        } else if (c > 1.75f) {
                            '▓'
                        } else {
                            '▒'
                        }
                    grid.colors[i] = palette.pick(fr[i], fg[i], fb[i])
                    continue
                }
                val q = c * 3f + BAYER[(y and 3) * 4 + (x and 3)]
                val idx = min(3, (q + 0.5f).toInt())
                if (idx <= 0) {
                    grid.glyphs[i] = ' '
                    grid.colors[i] = 0
                } else {
                    grid.glyphs[i] = GLYPHS[idx]
                    grid.colors[i] = palette.pick(fr[i], fg[i], fb[i])
                }
            }
        }
    }

    private fun stars(
        grid: Grid,
        t: Float,
    ) {
        for (s in 0 until STARS) {
            val i = starIdx[s]
            if (fc[i] > 0.5f || kind[i].toInt() != SKY) continue
            val tw = (0.55f + 0.45f * sin(TAU * (starRate[s] * t / LOOP + starPhase[s]))) * starPower[s]
            if (tw < 0.22f) continue
            grid.glyphs[i] =
                if (tw > 0.7f) {
                    '*'
                } else if (tw > 0.42f) {
                    '+'
                } else {
                    '.'
                }
            grid.colors[i] = palette.pick(0.6f + 0.4f * tw, 0.6f + 0.35f * tw, 0.9f + 0.1f * tw)
        }
    }

    private fun glints(
        grid: Grid,
        t: Float,
    ) {
        for (g in 0 until GLINTS) {
            val i = glintIdx[g]
            if (fc[i] < 1.5f) continue
            val tw = sin(TAU * (glintRate[g] * t / LOOP + glintPhase[g]))
            if (tw < 0.9f) continue
            grid.glyphs[i] = if (tw > 0.97f) '*' else '+'
            grid.colors[i] = palette.pick(1f, 0.9f, 0.95f)
        }
    }

    private fun petals(
        grid: Grid,
        t: Float,
    ) {
        for (p in petal) {
            val age = fract(p[5] + p[6] * t / LOOP)
            val alpha = smooth(0f, 0.07f, age) * (1f - smooth(0.86f, 1f, age))
            if (alpha < 0.3f) continue
            val y = (p[1] + (p[2] - p[1]) * age).toInt()
            val x =
                floor(p[0] + p[3] * (age + 0.15f * sin(TAU * age + p[5] * 6f)) + p[4] * sin(TAU * (3f * age + p[5])))
                    .toInt()
            if (x !in 0 until COLS || y !in 0 until ROWS) continue
            val i = y * COLS + x
            val tumble = ((age * p[6] * 14f + p[5] * 4f).toInt()) and 3
            val onWater = y >= WATER
            grid.glyphs[i] = if (onWater) "·•··"[tumble] else "•,·`"[tumble]
            val dim = if (onWater) 0.7f else 1f
            val c = pinkR.at(p[7] * dim)
            grid.colors[i] = palette.pick(c.r, c.g, c.b)
        }
    }
}

/**
 * Moonlit cherry blossoms: a gnarled sakura with dense pink blossom clouds leans over a still pond, a small pagoda
 * with lit windows stands on the far shore, and the big pale moon, the tree and the pagoda are mirrored in the
 * rippling water under a low drift of mist. Petals let go of the canopy and fall on the wind across the pond. Every
 * cell is computed from its position and the time, shaded in its own colour and drawn as a four level dithered
 * halftone or a solid block. The seed shapes the canopy texture, the ridges, the stars, the glints and the petals'
 * paths. Every motion completes a whole number of cycles per loop, petals included.
 */
public fun cherryBlossomPiece(
    seed: Int = 0,
    blossom: Rgb = Blossom,
    wind: Float = 1f,
): AsciiPiece {
    val scene = CbScene(seed, blossom, wind)
    return asciiPiece(
        name = "cherry blossom",
        ground = Rgb.fromHex("#0b0a1c"),
        category = "scenes",
        cols = COLS,
        rows = ROWS,
        fps = 15,
        still = 5f,
        loop = LOOP,
        note = "a blossoming sakura under a big moon beside a pond and pagoda, petals drifting, mist and reflections",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A moonlit cherry tree in full bloom beside a still pond with a pagoda, petals drifting down on the wind and mist
 * low over the water.
 *
 * @param seed shapes the canopy texture, the ridges, the stars, the glints and the petals.
 * @param blossom the color of the blossom and the petals.
 * @param wind how far the petals drift sideways; 0 lets them fall straight down.
 */
@Composable
public fun AsciiCherryBlossom(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    blossom: Rgb = Blossom,
    wind: Float = 1f,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A moonlit cherry tree in bloom by a pond and a pagoda, petals drifting on the wind",
) {
    val piece = remember(seed, blossom, wind) { cherryBlossomPiece(seed, blossom, wind) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
