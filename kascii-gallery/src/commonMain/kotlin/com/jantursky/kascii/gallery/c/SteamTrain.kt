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
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val COLS = 192
private const val ROWS = 48
private const val FPS = 15
private const val LOOP = 16f
private const val CELL = 2.2f // a glyph cell is this much taller than wide
private const val RAIL = 34 // the row of the rail
private const val DECK = 35 // the first row of the viaduct
private const val PIER = 32 // columns from one arch to the next
private const val DRIVER_CIRC = 48f // circumference of a driving wheel, in columns
private const val SMALL_CIRC = 25.6f // circumference of a bogie wheel
private const val DRIVER_TURNS = 8 // driving wheel turns per loop
private const val GROUND = DRIVER_TURNS * 48 // columns the track slides past per loop
private const val SMALL_TURNS = 15 // bogie wheel turns per loop
private const val POLE_GAP = 96
private const val POLE_RUN = 6 * POLE_GAP // columns the poles slide past per loop
private const val CLOUD_P = 144
private const val FX = 162f // the front of the smokebox
private const val CHX = FX - 10f // the chimney
private const val CHY = 8f
private const val LAMP_X = FX + 0.6f
private const val LAMP_Y = 16.6f
private const val PUFFS = 8
private const val PUFF_CYCLES = 3
private const val NONE = '\u0000'
private const val RAMP = " ·•●▒"
private const val MOON_X = 30f
private const val MOON_Y = 9f
private const val MOON_R = 12f // in columns

private val BAYER = intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)

private fun smooth(
    a: Float,
    b: Float,
    v: Float,
): Float {
    val k = ((v - a) / (b - a)).coerceIn(0f, 1f)
    return k * k * (3f - 2f * k)
}

private fun lerp(
    a: Float,
    b: Float,
    k: Float,
): Float = a + (b - a) * k

/** Value noise on a lattice that wraps after [period] cells in x, so a texture built from it tiles seamlessly. */
private fun tiledNoise(
    x: Float,
    y: Float,
    period: Int,
    seed: Int,
): Float {
    val xi = floor(x).toInt()
    val yi = floor(y).toInt()
    val fx = x - xi
    val fy = y - yi
    val u = fx * fx * (3f - 2f * fx)
    val v = fy * fy * (3f - 2f * fy)
    val x0 = xi.mod(period)
    val x1 = (xi + 1).mod(period)
    val a = Noise.hash(x0, yi, 5, seed)
    val b = Noise.hash(x1, yi, 5, seed)
    val c = Noise.hash(x0, yi + 1, 5, seed)
    val d = Noise.hash(x1, yi + 1, 5, seed)
    return (a + (b - a) * u) * (1f - v) + (c + (d - c) * u) * v
}

/** A layer of cells: a glyph and a color per cell, [NONE] where it is transparent. */
private class TrainLayer {
    val glyph = CharArray(COLS * ROWS)
    val r = FloatArray(COLS * ROWS)
    val g = FloatArray(COLS * ROWS)
    val b = FloatArray(COLS * ROWS)
    val kind = ByteArray(COLS * ROWS) // 1 for the headlamp glass

    fun copyTo(o: TrainLayer) {
        glyph.copyInto(o.glyph)
        r.copyInto(o.r)
        g.copyInto(o.g)
        b.copyInto(o.b)
        kind.copyInto(o.kind)
    }

    fun put(
        x: Int,
        y: Int,
        ch: Char,
        cr: Float,
        cg: Float,
        cb: Float,
        k: Int = 0,
    ) {
        if (x < 0 || x >= COLS || y < 0 || y >= ROWS) return
        val i = y * COLS + x
        glyph[i] = ch
        r[i] = cr
        g[i] = cg
        b[i] = cb
        kind[i] = k.toByte()
    }

    /**
     * Fills the cells whose centre lies in [x0], [x1] by [y0], [y1]. A row that the box covers only partly gets a half
     * block, so edges move in half rows. [shade] 1 lights the top and darkens the bottom by [hi], 2 shades a cylinder.
     */
    fun box(
        x0: Float,
        x1: Float,
        y0: Float,
        y1: Float,
        cr: Float,
        cg: Float,
        cb: Float,
        hi: Float = 0f,
        shade: Int = 1,
        k: Int = 0,
    ) {
        val xs = ceil(x0 - 0.5f).toInt()
        val xe = ceil(x1 - 0.5f).toInt() - 1
        for (y in floor(y0).toInt()..ceil(y1).toInt() - 1) {
            val c = min(y1, y + 1f) - max(y0, y.toFloat())
            val ch =
                when {
                    c >= 0.75f -> '█'
                    c < 0.25f -> continue
                    y0 > y && y1 < y + 1f -> if ((y0 + y1) / 2f > y + 0.5f) '▄' else '▀'
                    y0 > y -> '▄'
                    else -> '▀'
                }
            val v = ((y + 0.5f - y0) / (y1 - y0)).coerceIn(0f, 1f)
            val f =
                if (shade == 2) {
                    0.3f + 0.95f * exp(-((v - 0.22f) / 0.24f).pow(2)) + 0.3f * (1f - v)
                } else {
                    1f + hi * (1f - 2f * v)
                }
            for (x in xs..xe) put(x, y, ch, cr * f, cg * f, cb * f, k)
        }
    }

    /** A thin steel bar from one point to another, a column at a time. */
    fun bar(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        cr: Float,
        cg: Float,
        cb: Float,
    ) {
        val n = ceil(abs(x1 - x0)).toInt().coerceAtLeast(1)
        for (s in 0..n) {
            val x = lerp(x0, x1, s / n.toFloat())
            val y = lerp(y0, y1, s / n.toFloat())
            box(x - 0.5f, x + 0.5f, y - 0.5f, y + 0.5f, cr, cg, cb)
        }
    }
}

private class TrainWheel(
    val cx: Float,
    val cy: Float,
    val rx: Float,
    val turns: Int,
    val spokes: Int,
)

/** All state of one steam train piece. The static layers are built once, a frame only reads them. */
private class TrainScene(
    private val seed: Int,
    private val cars: Int,
    engine: Rgb,
) {
    private val n = COLS * ROWS
    private val train = TrainLayer()
    private val work = TrainLayer()
    private val wheels = ArrayList<TrainWheel>()
    private val spill = FloatArray(COLS)

    // The sky is built once: gradient, moon, stars and the clouds on a texture that tiles.
    private val skyGlyph = CharArray(n)
    private val skyR = FloatArray(n)
    private val skyG = FloatArray(n)
    private val skyB = FloatArray(n)
    private val star = FloatArray(n)
    private val starPhase = FloatArray(n)
    private val starRate = IntArray(n)
    private val cloud = FloatArray(CLOUD_ROWS * CLOUD_P)
    private val cloudLit = FloatArray(CLOUD_ROWS * CLOUD_P)
    private val smoke = FloatArray(n)
    private val warm = FloatArray(n)
    private val threshold =
        FloatArray(n) {
            (0.4f * (BAYER[((it / COLS) and 3) * 4 + (it % COLS and 3)] + 0.5f) / 16f) +
                0.6f * Noise.hash(it % COLS, it / COLS, 41, seed)
        }

    // The ridges, as a profile that wraps and the top row of each column in the current frame.
    private val farProfile = hillProfile(FAR_P, 1, 15f, 4.2f, 2, 8)
    private val midProfile = hillProfile(MID_P, 2, 20.5f, 3.4f, 3, 11)
    private val nearProfile = hillProfile(NEAR_P, 3, 26f, 2.4f, 3, 12)
    private val treeProfile = treeProfile(TREE_P)
    private val houseMask = BooleanArray(TREE_P)
    private val farTop = FloatArray(COLS)
    private val midTop = FloatArray(COLS)
    private val nearTop = FloatArray(COLS)
    private val treeTop = FloatArray(COLS)

    private val palette: IntArray
    private val lut = IntArray(32768) { -1 }

    private val engR = engine.red / 255f
    private val engG = engine.green / 255f
    private val engB = engine.blue / 255f

    // Per-frame state.
    private var r = 0f
    private var g = 0f
    private var b = 0f
    private var fixed = NONE
    private var run = 0f
    private var flare = 1f
    private var cloudShift = 0
    private var cloudFrac = 0f
    private var viaductShift = 0
    private var houseShift = 0
    private var cloudDens = 0f

    private val poleColor: Int
    private val poleRim: Int
    private val wireColor: Int
    private val glassColor: Int

    init {
        palette = buildPalette(engine)
        poleColor = quantize(0.012f, 0.014f, 0.03f)
        poleRim = quantize(0.07f, 0.08f, 0.17f)
        wireColor = quantize(0.2f, 0.23f, 0.4f)
        glassColor = quantize(0.55f, 0.6f, 0.85f)
        buildSky()
        buildTrain()
        for (u in 0 until TREE_P) {
            houseMask[u] = Noise.hash(u, 5, 67, seed) > 0.975f
        }
    }

    private fun buildPalette(engine: Rgb): IntArray {
        val out = ArrayList<Rgb>()

        fun ramp(
            steps: Int,
            vararg anchors: String,
        ) {
            val a = anchors.map { Rgb.fromHex(it) }
            for (s in 0 until steps) {
                val p = s / (steps - 1f) * (a.size - 1)
                val i = min(p.toInt(), a.size - 2)
                out += a[i].lerp(a[i + 1], p - i)
            }
        }
        ramp(12, "#04061a", "#0b1136", "#1b1f58", "#3f2f78", "#7b4a8a", "#bc6478", "#f09a66", "#ffd9a0")
        ramp(5, "#0c0f2a", "#1c1e4a", "#32305f")
        ramp(3, "#080a1e", "#10153a")
        ramp(4, "#02030a", "#070a1a")
        ramp(7, "#14172c", "#2c3150", "#56608a", "#8a92b8")
        ramp(4, "#2c1c20", "#8a5a3c", "#d09a5a")
        ramp(7, engine.lerp(Rgb.Black, 0.8f).toHex(), engine.lerp(Rgb.Black, 0.4f).toHex(), engine.toHex(), "#ffb0a8")
        ramp(6, "#08201a", "#1f5a46", "#52a888", "#a8e0c8")
        ramp(3, "#4a3c2e", "#c8b48a", "#f0e4c0")
        ramp(6, "#6a3a10", "#ff9a30", "#fff0b8")
        ramp(6, "#14162a", "#585e82", "#a0a6c8", "#e6eaff")
        ramp(3, "#5a3c0c", "#c89a30", "#fff0a0")
        ramp(8, "#1c1e36", "#4a4a6c", "#8a88a8", "#d0ccdc")
        ramp(3, "#4a2a24", "#c87850")
        ramp(3, "#b0a688", "#fffae0")
        ramp(2, "#46508a", "#ffffff")
        ramp(4, "#5a3a12", "#ffd88a", "#fff6d0")
        ramp(2, "#1c2048", "#5a5890")
        ramp(2, "#010208", "#06080f")
        return IntArray(out.size) { out[it].argb }
    }

    /** The palette colour nearest to a color with channels in 0..1, through a lookup table filled on demand. */
    private fun quantize(
        cr: Float,
        cg: Float,
        cb: Float,
    ): Int {
        val ir = (cr * 31.99f).toInt().coerceIn(0, 31)
        val ig = (cg * 31.99f).toInt().coerceIn(0, 31)
        val ib = (cb * 31.99f).toInt().coerceIn(0, 31)
        val key = (ir shl 10) or (ig shl 5) or ib
        val hit = lut[key]
        if (hit >= 0) return hit
        var best = 0
        var bestD = Float.MAX_VALUE
        for (i in palette.indices) {
            val c = palette[i]
            val dr = ((c shr 16) and 0xFF) / 255f - (ir + 0.5f) / 32f
            val dg = ((c shr 8) and 0xFF) / 255f - (ig + 0.5f) / 32f
            val db = (c and 0xFF) / 255f - (ib + 0.5f) / 32f
            val d = 0.3f * dr * dr + 0.5f * dg * dg + 0.2f * db * db
            if (d < bestD) {
                bestD = d
                best = i
            }
        }
        lut[key] = palette[best]
        return palette[best]
    }

    private fun hillProfile(
        p: Int,
        salt: Int,
        base: Float,
        amp: Float,
        kMin: Int,
        kMax: Int,
    ): FloatArray {
        val out = FloatArray(p)
        var norm = 0f
        for (k in kMin..kMax) norm += 1f / k
        for (k in kMin..kMax) {
            val phase = Noise.hash(k, salt, 61, seed) * AKit.TAU
            val a = 1f / k / norm
            for (u in 0 until p) out[u] += a * sin(AKit.TAU * k * u / p + phase)
        }
        for (u in 0 until p) out[u] = base - amp * 1.5f * out[u]
        return out
    }

    private fun treeProfile(p: Int): FloatArray {
        val out = hillProfile(p, 4, 43f, 1.3f, 3, 9)
        val raw = FloatArray(p) { Noise.hash(it, 9, 66, seed) }
        for (u in 0 until p) {
            val a = (raw[u] + raw[(u + 1) % p] + raw[(u + p - 1) % p]) / 3f
            out[u] -= 2.6f * a
        }
        return out
    }

    private fun buildSky() {
        val anchors =
            arrayOf(
                floatArrayOf(0f, 0.01f, 0.014f, 0.04f),
                floatArrayOf(7f, 0.025f, 0.03f, 0.09f),
                floatArrayOf(12f, 0.07f, 0.065f, 0.2f),
                floatArrayOf(16f, 0.22f, 0.13f, 0.3f),
                floatArrayOf(19f, 0.55f, 0.27f, 0.36f),
                floatArrayOf(22f, 0.85f, 0.46f, 0.36f),
                floatArrayOf(30f, 0.9f, 0.55f, 0.4f),
            )
        for (y in 0 until ROWS) {
            var a = 0
            while (a < anchors.size - 2 && y + 0.5f > anchors[a + 1][0]) a++
            val k = ((y + 0.5f - anchors[a][0]) / (anchors[a + 1][0] - anchors[a][0])).coerceIn(0f, 1f)
            for (x in 0 until COLS) {
                val i = y * COLS + x
                skyR[i] = lerp(anchors[a][1], anchors[a + 1][1], k)
                skyG[i] = lerp(anchors[a][2], anchors[a + 1][2], k)
                skyB[i] = lerp(anchors[a][3], anchors[a + 1][3], k)
            }
        }
        buildMoon()
        for (y in 0 until 16) {
            for (x in 0 until COLS) {
                val i = y * COLS + x
                val dx = x + 0.5f - MOON_X
                val dy = (y + 0.5f - MOON_Y) * CELL
                if (Noise.hash(x, y, 21, seed) > 0.982f && dx * dx + dy * dy > 700f && skyGlyph[i] == NONE) {
                    star[i] = 0.4f + 0.6f * Noise.hash(x, y, 22, seed)
                    starPhase[i] = Noise.hash(x, y, 23, seed)
                    starRate[i] = 2 + (Noise.hash(x, y, 24, seed) * 5f).toInt()
                }
            }
        }
        for (y in 0 until CLOUD_ROWS) {
            val band = smooth(3f, 8f, y.toFloat()) * smooth(20f, 14f, y.toFloat())
            for (x in 0 until CLOUD_P) {
                var sum = 0f
                var amp = 0.6f
                var total = 0f
                var fx = x / 36f
                var fy = y / 3.4f
                var period = 4
                for (o in 0 until 3) {
                    sum += amp * tiledNoise(fx, fy, period, seed + o * 37)
                    total += amp
                    amp *= 0.45f
                    fx *= 2f
                    fy *= 2f
                    period *= 2
                }
                cloud[y * CLOUD_P + x] = smooth(0.46f, 0.64f, sum / total) * band
            }
        }
        for (y in 0 until CLOUD_ROWS) {
            for (x in 0 until CLOUD_P) {
                val above = if (y > 0) cloud[(y - 1) * CLOUD_P + x] else 0f
                val below = if (y < CLOUD_ROWS - 1) cloud[(y + 1) * CLOUD_P + x] else 0f
                cloudLit[y * CLOUD_P + x] = (0.45f + 1.6f * (above - below)).coerceIn(0f, 1f)
            }
        }
    }

    /** A thin crescent: a disc with a second, shifted disc cut out of it. */
    private fun buildMoon() {
        for (y in 0 until 20) {
            for (x in 0 until COLS) {
                val i = y * COLS + x
                val dx = x + 0.5f - MOON_X
                val dy = (y + 0.5f - MOON_Y) * CELL
                val d2 = dx * dx + dy * dy
                skyR[i] += 0.1f * exp(-d2 / 340f)
                skyG[i] += 0.11f * exp(-d2 / 340f)
                skyB[i] += 0.17f * exp(-d2 / 340f)
                if (d2 > MOON_R * MOON_R) continue
                val cx = dx - 5.4f
                val cy = dy + 2.2f
                val cut = cx * cx + cy * cy
                val cutR = MOON_R * 0.97f
                if (cut < cutR * cutR) continue
                val edge = min(MOON_R - sqrt(d2), sqrt(cut) - cutR)
                skyGlyph[i] = if (edge < 1.1f) '▓' else '█'
                skyR[i] = 0.97f
                skyG[i] = 0.94f
                skyB[i] = 0.8f
            }
        }
    }

    private fun cyl(
        x0: Float,
        x1: Float,
        y0: Float,
        y1: Float,
        c: FloatArray,
        k: Int = 0,
    ) = train.box(x0, x1, y0, y1, c[0], c[1], c[2], shade = 2, k = k)

    private fun flat(
        x0: Float,
        x1: Float,
        y0: Float,
        y1: Float,
        c: FloatArray,
        hi: Float = 0f,
    ) = train.box(x0, x1, y0, y1, c[0], c[1], c[2], hi)

    private fun buildTrain() {
        val iron = floatArrayOf(0.15f, 0.16f, 0.25f)
        val dark = floatArrayOf(0.06f, 0.065f, 0.12f)
        val brass = floatArrayOf(0.78f, 0.56f, 0.16f)
        val steel = floatArrayOf(0.5f, 0.54f, 0.72f)
        val boiler = floatArrayOf(engR * 0.62f, engG * 0.62f, engB * 0.62f)
        val cab = floatArrayOf(engR * 0.42f, engG * 0.42f, engB * 0.42f)
        val cream = floatArrayOf(0.72f, 0.62f, 0.44f)
        val amber = floatArrayOf(1f, 0.74f, 0.3f)

        // The cowcatcher, a grill sloping down to the front.
        for (x in (FX - 1).toInt() until (FX + 10).toInt()) {
            val top = 30.2f + (x - (FX - 1f)) * 0.3f
            val bar = (x - FX.toInt()).mod(3) == 0
            train.box(
                x.toFloat(),
                x + 1f,
                top,
                34f,
                if (bar) 0.6f else 0.22f,
                if (bar) 0.64f else 0.24f,
                if (bar) 0.86f else 0.36f,
            )
        }
        // The frame and the cylinder block.
        flat(FX - 52f, FX + 1f, 27.4f, 31.6f, dark)
        flat(FX - 16f, FX - 3f, 26.6f, 31.6f, floatArrayOf(0.15f, 0.16f, 0.24f), 0.3f)
        // The boiler, the smokebox and its bands.
        cyl(FX - 32f, FX - 7f, 18.2f, 26f, boiler)
        cyl(FX - 7f, FX - 0.5f, 18.2f, 26f, floatArrayOf(0.34f, 0.37f, 0.55f))
        for (bx in intArrayOf(-27, -17, -8)) cyl(FX + bx, FX + bx + 1.2f, 18.2f, 26f, brass)
        // The chimney, a stem with a flared cap.
        val halfs = floatArrayOf(6.2f, 4.8f, 3.4f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f, 2.5f)
        for (k in halfs.indices) {
            val y = 9 + k
            for (x in floor(CHX - halfs[k]).toInt()..ceil(CHX + halfs[k]).toInt()) {
                val u = (x + 0.5f - CHX) / halfs[k]
                if (abs(u) > 1f) continue
                val f = 0.55f + 0.9f * exp(-((u + 0.35f) / 0.4f).pow(2))
                train.put(x, y, '█', 0.2f * f, 0.21f * f, 0.32f * f)
            }
        }
        for (x in floor(CHX - 6.2f).toInt()..ceil(CHX + 6.2f).toInt()) {
            if (abs(x + 0.5f - CHX) < 6.2f) train.put(x, 9, '▄', 0.62f, 0.66f, 0.9f)
        }
        // The brass domes.
        for (dome in arrayOf(floatArrayOf(FX - 20f, 3.9f, 18.3f), floatArrayOf(FX - 28f, 2.7f, 18.3f))) {
            for (y in 14..18) {
                for (x in floor(dome[0] - dome[1]).toInt()..ceil(dome[0] + dome[1]).toInt()) {
                    val dx = x + 0.5f - dome[0]
                    val dy = (y + 0.5f - dome[2]) * CELL
                    if (dy > 0f || dx * dx + dy * dy > dome[1] * dome[1]) continue
                    val f = 0.45f + 0.8f * exp(-((dx + 1.2f) / 2.2f).pow(2))
                    train.put(x, y, '█', brass[0] * f, brass[1] * f, brass[2] * f)
                }
            }
        }
        // The headlamp, the glass bright.
        flat(FX - 6.5f, FX - 0.5f, 14.8f, 18.4f, iron, 0.2f)
        flat(FX - 5.5f, FX - 1.5f, 13.9f, 14.8f, iron, 0.5f)
        train.box(FX - 3.5f, FX - 0.5f, 15.2f, 18f, 1f, 0.97f, 0.75f, 0f, 1, 1)
        flat(FX - 5.5f, FX - 3.5f, 15.5f, 17.7f, floatArrayOf(0.4f, 0.3f, 0.12f))
        // The running board and the cab.
        flat(FX - 52f, FX + 3f, 26f, 27.1f, floatArrayOf(0.055f, 0.06f, 0.1f), 0.5f)
        flat(FX - 52f, FX - 30.5f, 13.7f, 30.6f, cab, 0.2f)
        flat(FX - 52f, FX - 30.5f, 14.8f, 15.6f, cream)
        flat(FX - 52f, FX - 30.5f, 24.6f, 25.4f, floatArrayOf(brass[0] * 0.7f, brass[1] * 0.7f, brass[2] * 0.7f))
        flat(FX - 54.5f, FX - 28f, 12.2f, 13.8f, floatArrayOf(0.22f, 0.24f, 0.38f), 0.7f)
        flat(FX - 48.5f, FX - 37.5f, 15.6f, 23.6f, floatArrayOf(brass[0] * 0.6f, brass[1] * 0.6f, brass[2] * 0.6f))
        flat(FX - 47.5f, FX - 38.5f, 16.6f, 22.6f, amber)
        train.put((FX - 43f).toInt(), 18, '●', 0.3f, 0.14f, 0.05f)
        flat(FX - 45f, FX - 41f, 19.3f, 22.6f, floatArrayOf(0.3f, 0.14f, 0.05f))
        window(FX - 47.5f, FX - 38.5f)
        // The tender with its heap of coal.
        val tx0 = FX - 77f
        val tx1 = FX - 55f
        flat(tx0, tx1, 22f, 31f, floatArrayOf(0.15f, 0.17f, 0.28f), 0.3f)
        flat(tx0 - 0.5f, tx1 + 0.5f, 21.3f, 22.3f, floatArrayOf(0.42f, 0.45f, 0.64f))
        flat(tx0 + 5f, tx1 - 5f, 25.4f, 27.6f, cream, -0.2f)
        for (x in tx0.toInt() + 1 until tx1.toInt() - 1) {
            val h = Noise.hash(x, 3, 68, seed)
            val top = 21.4f - 2.8f * sin(AKit.TAU * 0.5f * (x - tx0) / (tx1 - tx0)) * (0.75f + 0.35f * h)
            val glint = Noise.hash(x, 4, 68, seed) > 0.8f
            flat(x.toFloat(), x + 1f, top, 21.5f, floatArrayOf(0.05f, 0.05f, 0.09f))
            if (glint) train.put(x, top.toInt() + 1, '▓', 0.34f, 0.34f, 0.5f)
        }
        flat(tx1, FX - 52f, 29.5f, 30.4f, iron, 0.4f)
        // The coaches, lit.
        val green = floatArrayOf(0.1f, 0.33f, 0.26f)
        for (c in 0 until cars) {
            val x0 = FX - 111f - 34f * c
            val x1 = x0 + 32f
            flat(x0, x1, 22.6f, 30.4f, green, 0.28f)
            flat(x0 + 1f, x1 - 1f, 21.7f, 22.7f, floatArrayOf(0.1f, 0.11f, 0.18f), 0.5f)
            flat(x0 + 2.5f, x1 - 2.5f, 21f, 21.8f, floatArrayOf(0.18f, 0.19f, 0.3f), 0.5f)
            flat(x0, x1, 23.3f, 24.1f, cream)
            flat(x0, x1, 28.7f, 29.4f, cream)
            flat(x0, x1, 30.4f, 31.6f, iron, 0.3f)
            for (j in 0 until 6) {
                val ix = x0.toInt() + 2 + 5 * j
                val f = 0.88f + 0.12f * Noise.hash(c, j, 69, seed)
                train.box(ix.toFloat(), ix + 3f, 25f, 28f, amber[0] * f, amber[1] * f, amber[2] * f)
                if (Noise.hash(c, j, 70, seed) > 0.5f) {
                    train.put(ix + 1, 26, '●', 0.3f, 0.14f, 0.05f)
                    train.put(ix, 27, '▄', 0.3f, 0.14f, 0.05f)
                    train.put(ix + 1, 27, '█', 0.3f, 0.14f, 0.05f)
                    train.put(ix + 2, 27, '▄', 0.3f, 0.14f, 0.05f)
                }
                window(ix.toFloat(), ix + 3f)
            }
            flat(x1, x1 + 2f, 29.6f, 30.5f, iron, 0.4f)
        }
        flat(tx0 - 2f, tx0, 29.6f, 30.5f, iron, 0.4f)
        // Buffers at the front of the pilot and the rod-free steel parts need no more, the wheels do.
        flat(FX + 7f, FX + 9f, 29.2f, 30.2f, steel, 0.3f)
        buildWheels(tx0)
        for (x in 0 until COLS) spill[x] = min(1f, spill[x])
    }

    private fun window(
        x0: Float,
        x1: Float,
    ) {
        for (x in max(0, (x0 - 4f).toInt())..min(COLS - 1, (x1 + 4f).toInt())) {
            val d = max(0f, max(x0 - (x + 0.5f), x + 0.5f - x1))
            spill[x] += 0.3f * exp(-d * d / 7f)
        }
    }

    private fun buildWheels(tenderX: Float) {
        val small = SMALL_CIRC / AKit.TAU
        val smallY = RAIL - small / CELL
        val big = DRIVER_CIRC / AKit.TAU
        val bigY = RAIL - big / CELL
        wheels += TrainWheel(FX - 38f, bigY, big, DRIVER_TURNS, 8)
        wheels += TrainWheel(FX - 19f, bigY, big, DRIVER_TURNS, 8)
        wheels += TrainWheel(FX - 9f, smallY, small, SMALL_TURNS, 4)
        wheels += TrainWheel(FX - 1f, smallY, small, SMALL_TURNS, 4)
        for (dx in floatArrayOf(4f, 11f, 18f)) wheels += TrainWheel(tenderX + dx, smallY, small, SMALL_TURNS, 4)
        for (c in 0 until cars) {
            for (dx in floatArrayOf(4f, 11f, 21f, 28f)) {
                wheels += TrainWheel(FX - 111f - 34f * c + dx, smallY, small, SMALL_TURNS, 4)
            }
        }
    }

    // ---------------------------------------------------------------- frame

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        run = t / LOOP
        flare = 0.93f + 0.07f * sin(AKit.TAU * 8f * run)
        train.copyTo(work)
        for (w in wheels) drawWheel(w)
        drawRods()
        val cs = run * CLOUD_P
        cloudShift = floor(cs).toInt()
        cloudFrac = cs - cloudShift
        viaductShift = (run * GROUND).roundToInt()
        houseShift = floor(run * TREE_P).toInt()
        ridge(farTop, farProfile, run * FAR_P)
        ridge(midTop, midProfile, run * MID_P)
        ridge(nearTop, nearProfile, run * NEAR_P)
        ridge(treeTop, treeProfile, run * TREE_P)
        buildSmoke()
        val glyphs = grid.glyphs
        val colors = grid.colors
        for (y in 0 until ROWS) {
            for (x in 0 until COLS) cell(x, y, y * COLS + x, glyphs, colors)
        }
        wires(grid)
        poles(grid)
    }

    private fun ridge(
        out: FloatArray,
        profile: FloatArray,
        shift: Float,
    ) {
        val p = profile.size
        val base = floor(shift).toInt()
        val fr = shift - base
        for (x in 0 until COLS) {
            val a = (x + base).mod(p)
            val c = if (a + 1 == p) 0 else a + 1
            out[x] = profile[a] * (1f - fr) + profile[c] * fr
        }
    }

    private fun drawWheel(w: TrainWheel) {
        val theta = AKit.TAU * w.turns * run
        val ry = w.rx / CELL
        val big = w.spokes > 4
        val sector = AKit.TAU / w.spokes
        for (y in floor(w.cy - ry - 1f).toInt()..ceil(w.cy + ry + 1f).toInt()) {
            for (x in floor(w.cx - w.rx - 1f).toInt()..ceil(w.cx + w.rx + 1f).toInt()) {
                if (x < 0 || x >= COLS || y < 0 || y >= ROWS) continue
                val dx = x + 0.5f - w.cx
                val dy = (y + 0.5f - w.cy) * CELL
                val d = sqrt(dx * dx + dy * dy)
                val limit = w.rx * w.rx
                val top = dx * dx + (dy - 0.25f * CELL).let { it * it } <= limit
                val bottom = dx * dx + (dy + 0.25f * CELL).let { it * it } <= limit
                if (!top && !bottom) continue
                val edge =
                    if (top && bottom) {
                        '█'
                    } else if (top) {
                        '▀'
                    } else {
                        '▄'
                    }
                val light = 1f - 0.22f * dy / w.rx
                val i = y * COLS + x
                if (edge != '█' || d > w.rx - (if (big) 1.7f else 1.5f)) {
                    if (big) {
                        put(i, edge, 0.55f * light, 0.6f * light, 0.88f * light)
                    } else {
                        put(
                            i,
                            edge,
                            0.3f * light,
                            0.34f * light,
                            0.52f * light,
                        )
                    }
                } else if (d < (if (big) 2.6f else 1.7f)) {
                    put(i, '█', 0.9f * light, 0.68f * light, 0.22f * light)
                } else {
                    var a = (atan2(dy, dx) - theta).mod(sector) - sector / 2f
                    a = abs(a)
                    if (d * sin(a) < (if (big) 0.8f else 0.7f)) {
                        put(i, '█', 0.62f * light, 0.66f * light, 0.9f * light)
                    } else {
                        put(i, '█', 0.13f * light, 0.14f * light, 0.23f * light)
                    }
                }
            }
        }
    }

    private fun put(
        i: Int,
        ch: Char,
        cr: Float,
        cg: Float,
        cb: Float,
    ) {
        work.glyph[i] = ch
        work.r[i] = cr
        work.g[i] = cg
        work.b[i] = cb
        work.kind[i] = 0
    }

    /** The main rod and the coupling rod follow the crank pins of the two driving wheels. */
    private fun drawRods() {
        val rc = 4.6f
        val theta = AKit.TAU * DRIVER_TURNS * run
        val big = DRIVER_CIRC / AKit.TAU
        val cy = RAIL - big / CELL
        val px = rc * cos(theta)
        val py = rc * sin(theta) / CELL
        val x1 = FX - 38f + px
        val x2 = FX - 19f + px
        val y = cy + py
        work.box(x1 - 1f, x2 + 1f, y - 0.45f, y + 0.45f, 0.4f, 0.44f, 0.66f)
        work.bar(x2, y, FX - 11f, 30.4f, 0.4f, 0.44f, 0.66f)
        work.box(FX - 13f, FX - 9f, 29.9f, 30.9f, 0.9f, 0.7f, 0.25f)
    }

    private fun buildSmoke() {
        smoke.fill(0f)
        warm.fill(0f)
        for (p in 0 until PUFFS) {
            val age = AKit.frac(PUFF_CYCLES * run + p / PUFFS.toFloat())
            val px = CHX - 62f * age.pow(1.2f)
            val py = CHY - 4f * sqrt(age)
            val rad = 3.4f + 12f * sqrt(age)
            val amp = (1f - age).pow(1.1f) * min(1f, age * 7f) * 1.9f
            val heat = max(0f, 1f - age * 3f)
            for (l in 0 until 3) {
                val ox = (Noise.hash(p, l, 62, seed) - 0.5f) * 1.2f * rad
                val oy = (Noise.hash(p, l, 63, seed) - 0.5f) * 0.9f * rad
                val lr = rad * (0.55f + 0.4f * Noise.hash(p, l, 64, seed))
                val cx = px + ox
                val cy = py + oy / CELL
                val ry = lr / CELL
                for (y in max(0, floor(cy - ry).toInt())..min(ROWS - 1, ceil(cy + ry).toInt())) {
                    for (x in max(0, floor(cx - lr).toInt())..min(COLS - 1, ceil(cx + lr).toInt())) {
                        val dx = x + 0.5f - cx
                        val dy = (y + 0.5f - cy) * CELL
                        val d2 = (dx * dx + dy * dy) / (lr * lr)
                        if (d2 >= 1f) continue
                        val v = (1f - d2).pow(0.7f) * amp
                        val i = y * COLS + x
                        if (v > smoke[i]) smoke[i] = v
                        if (v * heat > warm[i]) warm[i] = v * heat
                    }
                }
            }
        }
    }

    private fun cell(
        x: Int,
        y: Int,
        i: Int,
        glyphs: CharArray,
        colors: IntArray,
    ) {
        fixed = NONE
        val tg = work.glyph[i]
        if (tg != NONE) {
            fixed = tg
            r = work.r[i]
            g = work.g[i]
            b = work.b[i]
            if (work.kind[i].toInt() == 1) {
                r *= flare
                g *= flare
                b *= flare
            }
            if (x > FX.toInt()) beam(x, y, 0.5f)
            solidOut(i, glyphs, colors)
            return
        }
        when {
            y >= DECK -> viaduct(x, y, i)
            y == RAIL -> rail(x)
            else -> landscape(x, y, i)
        }
        smokeOver(i, y)
        if (x > FX.toInt()) beam(x, y, if (fixed == NONE) 1f else 0.5f)
        if (fixed != NONE) solidOut(i, glyphs, colors) else softOut(i, glyphs, colors)
    }

    private fun solidOut(
        i: Int,
        glyphs: CharArray,
        colors: IntArray,
    ) {
        glyphs[i] = fixed
        colors[i] = quantize(min(1f, r), min(1f, g), min(1f, b))
    }

    private fun softOut(
        i: Int,
        glyphs: CharArray,
        colors: IntArray,
    ) {
        val top = max(r, max(g, b))
        if (top < 0.03f) {
            glyphs[i] = ' '
            colors[i] = 0
            return
        }
        val f = min(1f, top * 1.25f) * (RAMP.length - 1)
        val base = f.toInt()
        val idx = min(RAMP.length - 1, base + if (f - base > threshold[i]) 1 else 0)
        if (idx == 0) {
            glyphs[i] = ' '
            colors[i] = 0
            return
        }
        glyphs[i] = RAMP[idx]
        val inv = 1f / top
        val tone = 0.5f + 0.5f * min(1f, top * 1.5f)
        colors[i] = quantize(min(1f, r * inv) * tone, min(1f, g * inv) * tone, min(1f, b * inv) * tone)
    }

    private fun cover(
        top: Float,
        y: Int,
    ): Char {
        val c = y + 1f - top
        return if (c >= 0.75f) {
            '█'
        } else if (c >= 0.25f) {
            '▄'
        } else {
            NONE
        }
    }

    private fun landscape(
        x: Int,
        y: Int,
        i: Int,
    ) {
        val yc = y + 0.5f
        var ch = cover(nearTop[x], y)
        if (ch != NONE) {
            val rim = smooth(1.5f, 0f, yc - nearTop[x])
            fixed = ch
            r = 0.03f + 0.07f * rim
            g = 0.034f + 0.075f * rim
            b = 0.1f + 0.12f * rim
            return
        }
        ch = cover(midTop[x], y)
        if (ch != NONE) {
            val rim = smooth(1.6f, 0f, yc - midTop[x])
            fixed = ch
            r = 0.075f + 0.12f * rim
            g = 0.075f + 0.1f * rim
            b = 0.2f + 0.16f * rim
            return
        }
        ch = cover(farTop[x], y)
        if (ch != NONE) {
            val d = yc - farTop[x]
            val rim = smooth(2f, 0f, d)
            val haze = smooth(0f, 9f, d)
            fixed = ch
            r = 0.13f + 0.14f * haze + 0.16f * rim
            g = 0.115f + 0.07f * haze + 0.1f * rim
            b = 0.3f + 0.06f * haze + 0.16f * rim
            return
        }
        sky(x, y, i)
    }

    private fun sky(
        x: Int,
        y: Int,
        i: Int,
    ) {
        cloudDens = 0f
        if (skyGlyph[i] != NONE) {
            fixed = skyGlyph[i]
            r = skyR[i]
            g = skyG[i]
            b = skyB[i]
            return
        }
        r = skyR[i]
        g = skyG[i]
        b = skyB[i]
        if (y < CLOUD_ROWS) {
            val a0 = (x + cloudShift).mod(CLOUD_P)
            val a1 = if (a0 + 1 == CLOUD_P) 0 else a0 + 1
            val row = y * CLOUD_P
            val dens = cloud[row + a0] * (1f - cloudFrac) + cloud[row + a1] * cloudFrac
            cloudDens = dens
            if (dens > 0.01f) {
                val lit = cloudLit[row + a0] * (1f - cloudFrac) + cloudLit[row + a1] * cloudFrac
                val glow = smooth(5f, 17f, y.toFloat())
                val k = dens * 0.88f
                r += (0.07f + glow * 0.62f * lit - r) * k
                g += (0.07f + glow * 0.3f * lit - g) * k
                b += (0.17f + glow * 0.22f * lit - b) * k
            }
        }
        val amp = star[i]
        if (amp != 0f) {
            val tw = 0.5f + 0.5f * sin(AKit.TAU * (starRate[i] * run + starPhase[i]))
            val s = amp * (0.35f + 0.65f * tw) * (1f - cloudDens) * (1f - min(1f, 2.2f * max(r, max(g, b))))
            if (s > 0.12f) {
                fixed =
                    if (s > 0.62f && amp > 0.8f) {
                        '*'
                    } else if (s > 0.38f) {
                        '+'
                    } else {
                        '·'
                    }
                r = 0.75f * s
                g = 0.85f * s
                b = s
            }
        }
    }

    private fun rail(x: Int) {
        fixed = '▀'
        val shine = 0.5f + 0.5f * sin(AKit.TAU * 0.07f * (x + viaductShift))
        r = 0.45f + 0.15f * shine
        g = 0.5f + 0.15f * shine
        b = 0.7f + 0.15f * shine
    }

    private fun viaduct(
        x: Int,
        y: Int,
        i: Int,
    ) {
        val yc = y + 0.5f
        val sx = x + viaductShift
        val sp =
            spill[x] * (
                if (y == DECK) {
                    1f
                } else if (y == DECK + 1) {
                    0.7f
                } else if (y == DECK + 2) {
                    0.35f
                } else {
                    0f
                }
            )
        if (y == DECK) {
            val tie = sx.mod(8) < 2
            fixed = if (tie) '█' else '▒'
            if (tie) {
                r = 0.05f
                g = 0.035f
                b = 0.04f
            } else {
                r = 0.16f
                g = 0.16f
                b = 0.23f
            }
        } else if (y <= DECK + 2) {
            fixed = if (y == DECK + 1) '█' else '▓'
            val k = if (y == DECK + 1) 1f else 0.5f
            r = 0.3f * k
            g = 0.32f * k
            b = 0.48f * k
        } else {
            val u = sx.mod(PIER)
            val dxu = u + 0.5f - 20f
            val spring = 38f + 12f / CELL
            val dist: Float
            if (yc < spring) {
                val dyu = (yc - spring) * CELL
                dist = 12f - sqrt(dxu * dxu + dyu * dyu)
            } else {
                dist = 12f - abs(dxu)
            }
            if (dist > 0f) {
                behind(x, y, i)
                return
            }
            val ring = dist > -2.3f
            val block = (sx + (if (y and 1 == 0) 0 else 4)).mod(8)
            val brick = Noise.hash(sx / 8 + (y and 1), y, 71, seed)
            val depthShade = 1f - 0.5f * smooth(38f, 47f, yc)
            val moonSide = if (u < 2) 1.35f else 1f
            val lightness = (if (ring) 1.25f else 0.82f + 0.2f * brick) * depthShade * moonSide
            fixed = if (block == 0 && !ring) '▓' else '█'
            val m = if (block == 0 && !ring) 0.7f else 1f
            r = 0.2f * lightness * m
            g = 0.22f * lightness * m
            b = 0.35f * lightness * m
        }
        r += 0.55f * sp
        g += 0.32f * sp
        b += 0.1f * sp
    }

    /** What shows through an arch: mist, dark trees and a few lit windows. */
    private fun behind(
        x: Int,
        y: Int,
        i: Int,
    ) {
        val yc = y + 0.5f
        val tt = treeTop[x]
        if (yc >= tt) {
            fixed = '█'
            val rim = smooth(1.3f, 0f, yc - tt)
            r = 0.012f + 0.02f * rim
            g = 0.016f + 0.025f * rim
            b = 0.04f + 0.06f * rim
            val col = (x + houseShift).mod(TREE_P)
            if (houseMask[col] && y == tt.toInt() + 1) {
                fixed = '●'
                r = 0.95f
                g = 0.65f
                b = 0.25f
            }
            return
        }
        val k = smooth(37f, 47f, yc)
        val s = k * 4.2f + threshold[i] * 0.9f - 0.3f
        fixed =
            when {
                s > 2.4f -> '▒'
                s > 1.3f -> '░'
                s > 0.6f -> '·'
                else -> ' '
            }
        if (fixed == ' ') {
            fixed = NONE
            r = 0f
            g = 0f
            b = 0f
            return
        }
        r = 0.19f + 0.1f * k
        g = 0.19f + 0.09f * k
        b = 0.4f + 0.12f * k
    }

    private fun smokeOver(
        i: Int,
        y: Int,
    ) {
        val dens = smoke[i]
        if (dens < 0.07f) return
        val up = if (y > 0) smoke[i - COLS] else 0f
        val down = if (y < ROWS - 1) smoke[i + COLS] else 0f
        val lit = (0.6f + 1.5f * (down - up)).coerceIn(0.2f, 1f)
        val tone = dens + (threshold[i] - 0.45f) * 0.3f
        val ch =
            when {
                tone > 0.8f -> '█'
                tone > 0.55f -> '▓'
                tone > 0.36f -> '▒'
                tone > 0.2f -> '░'
                tone > 0.1f -> '·'
                else -> return
            }
        val w = warm[i] * (0.4f + 0.6f * (1f - lit))
        val sr = 0.26f + 0.6f * lit + 0.55f * w
        val sg = 0.26f + 0.58f * lit + 0.25f * w
        val sb = 0.36f + 0.6f * lit - 0.1f * w
        val k = min(1f, dens * 1.6f)
        if (fixed == NONE) {
            r += (sr - r) * k
            g += (sg - g) * k
            b += (sb - b) * k
        } else {
            r += (sr - r) * k
            g += (sg - g) * k
            b += (sb - b) * k
        }
        fixed = ch
    }

    /** The headlamp's beam and its bloom, added to whatever is lit. */
    private fun beam(
        x: Int,
        y: Int,
        gain: Float,
    ) {
        val dx = x + 0.5f - LAMP_X
        val dy = (y + 0.5f - LAMP_Y) * CELL
        val d2 = dx * dx + dy * dy
        var k = 0.9f * exp(-d2 / 14f)
        val along = dx * 0.995f + dy * 0.1f
        if (along > 1f) {
            val off = abs(dy * 0.995f - dx * 0.1f)
            val half = 2.5f + 0.26f * along
            if (off < half * 2.2f) {
                val prof = max(0f, 1f - off / half)
                val wide = 1f - off / (half * 2.2f)
                k += (0.7f * prof * sqrt(prof) + 0.22f * wide * wide) * (1f - along / 64f).coerceIn(0f, 1f)
            }
        }
        k *= flare * gain
        if (k < 0.01f) return
        r += 1f * k
        g += 0.8f * k
        b += 0.4f * k
    }

    private fun wires(grid: Grid) {
        val off = (run * POLE_RUN).roundToInt()
        val first = (40 - off).mod(POLE_GAP) - POLE_GAP
        var xa = first
        while (xa < COLS + POLE_GAP) {
            for (w in WIRES) {
                val x0 = xa + w[0].toInt()
                val x1 = x0 + POLE_GAP
                for (x in max(0, x0)..min(COLS - 1, x1)) {
                    val k = (x - x0) / POLE_GAP.toFloat()
                    val yw = w[1] + 2.4f * 4f * k * (1f - k)
                    val row = floor(yw).toInt()
                    val f = yw - row
                    set(
                        grid,
                        x,
                        row,
                        if (f < 0.34f) {
                            '▔'
                        } else if (f < 0.67f) {
                            '─'
                        } else {
                            '▁'
                        },
                        wireColor,
                    )
                }
            }
            xa += POLE_GAP
        }
    }

    private fun poles(grid: Grid) {
        val off = (run * POLE_RUN).roundToInt()
        var xa = (40 - off).mod(POLE_GAP) - POLE_GAP
        while (xa < COLS + POLE_GAP) {
            for (y in 2 until ROWS) {
                for (dx in -1..1) {
                    set(grid, xa + dx, y, '█', if (dx == -1) poleRim else poleColor)
                }
            }
            for (y in 3..3) for (dx in -8..8) set(grid, xa + dx, y, '█', poleColor)
            for (y in 7..7) for (dx in -6..6) set(grid, xa + dx, y, '█', poleColor)
            for (dx in intArrayOf(-7, -3, 3, 7)) set(grid, xa + dx, 2, '●', glassColor)
            for (dx in intArrayOf(-5, -2, 2, 5)) set(grid, xa + dx, 6, '●', glassColor)
            xa += POLE_GAP
        }
    }

    private fun set(
        grid: Grid,
        x: Int,
        y: Int,
        ch: Char,
        color: Int,
    ) {
        if (x < 0 || x >= COLS || y < 0 || y >= ROWS) return
        grid.glyphs[y * COLS + x] = ch
        grid.colors[y * COLS + x] = color
    }

    private companion object {
        const val CLOUD_ROWS = 22
        const val FAR_P = 192
        const val MID_P = 288
        const val NEAR_P = 320
        const val TREE_P = 352
        val WIRES =
            arrayOf(
                floatArrayOf(-7f, 2.3f),
                floatArrayOf(7f, 2.3f),
                floatArrayOf(-5f, 6.3f),
                floatArrayOf(5f, 6.3f),
            )
    }
}

/**
 * A steam train at dusk, wide like a postcard: a bold locomotive with a flared chimney, a lit cab, big spoked
 * wheels and a lamp throwing its beam ahead pulls a tender and [cars] lit coaches along a stone viaduct. Smoke
 * billows from the chimney and drifts back over the coaches while the telegraph poles, the viaduct and three
 * layers of hills slide past at their own speeds under a thin moon and a field of stars. The wheels turn exactly
 * as fast as the track passes, and every layer repeats a whole number of times per loop.
 */
public fun steamTrainPiece(
    seed: Int = 0,
    cars: Int = 3,
    engine: Rgb = AKit.Red,
): AsciiPiece {
    require(cars in 0..3) { "cars must be in 0..3" }
    val scene = TrainScene(seed, cars, engine)
    return asciiPiece(
        name = "steam train",
        category = "scenes",
        cols = COLS,
        rows = ROWS,
        fps = FPS,
        still = 3.2f,
        loop = LOOP,
        ground = Rgb.fromHex("#0a0d1c"),
        note = "a steam train with lit coaches crossing a viaduct at dusk, smoke drifting back past telegraph poles",
    ) { time, grid -> scene.draw(SceneKit.loopTime(time, FPS, LOOP), grid) }
}

/**
 * A steam train at dusk crossing a stone viaduct, its wheels turning and smoke drifting back over the lit coaches.
 *
 * @param seed shapes the hills, the stars, the clouds and the paint of the stone.
 * @param cars the number of coaches behind the tender, 0 to 3.
 * @param engine the color of the boiler and the cab.
 */
@Composable
public fun AsciiSteamTrain(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    cars: Int = 3,
    engine: Rgb = AKit.Red,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A steam train at dusk crossing a viaduct, smoke drifting back over its lit coaches",
) {
    val piece = remember(seed, cars, engine) { steamTrainPiece(seed, cars, engine) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
