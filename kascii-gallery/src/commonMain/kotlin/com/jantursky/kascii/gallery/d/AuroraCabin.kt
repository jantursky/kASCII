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
private const val LOOP = 16f
private const val WL = 35
private const val TAU = AKit.TAU
private const val FLAKES = 70
private const val STARS = 170
private const val PUFFS = 13
private const val BANDS = 4

private const val AIR = 0
private const val FAR = 1
private const val NEAR = 2
private const val TREE = 3
private const val WALL = 4
private const val ROOF = 5
private const val PANE = 6
private const val STONE = 7
private const val ICE = 8
private const val SOLID = 9

private const val GLYPHS = " ·•●"
private const val CHIMNEY_X = 145.5f
private const val CHIMNEY_TOP = 15f
private const val APEX_X = 138f
private const val APEX_Y = 17f
private const val ROOF_SLOPE = 0.44f
private const val EAVES_Y = 24.7f
private const val BASE_Y = 34f

private val BAYER =
    floatArrayOf(0f, 8f, 2f, 10f, 12f, 4f, 14f, 6f, 3f, 11f, 1f, 9f, 15f, 7f, 13f, 5f)
        .map { (it + 0.5f) / 16f - 0.5f }
        .toFloatArray()

private class Col(
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

private fun col(hex: String): Col = col(Rgb.fromHex(hex))

private fun col(c: Rgb): Col = Col(c.red / 255f, c.green / 255f, c.blue / 255f)

private fun ramp(
    steps: Int,
    vararg stops: Col,
): List<Col> =
    (0 until steps).map { k ->
        val p = k.toFloat() / (steps - 1) * (stops.size - 1)
        val i = min(p.toInt(), stops.size - 2)
        stops[i].mix(stops[i + 1], p - i)
    }

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
private class Lattice(
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

/** The colours the scene can show, with a 5 bits per channel table that snaps any computed colour to the nearest. */
private class Palette(
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
    ): Int = Rgb((r * 255f).roundToInt(), (g * 255f).roundToInt(), (b * 255f).roundToInt()).argb
}

/** One shaded layer: material, base colour, glyph coverage and how much it takes from the aurora and window light. */
private class Layer {
    val mat = IntArray(CELLS)
    val r = FloatArray(CELLS)
    val g = FloatArray(CELLS)
    val b = FloatArray(CELLS)
    val cov = FloatArray(CELLS)
    val rec = FloatArray(CELLS)
    val warmL = FloatArray(CELLS)
    val warmR = FloatArray(CELLS)
    val refl = FloatArray(CELLS)

    fun set(
        x: Int,
        y: Int,
        m: Int,
        c: Col,
        coverage: Float,
        receive: Float,
    ) {
        if (x !in 0 until COLS || y !in 0 until ROWS) return
        val i = y * COLS + x
        mat[i] = m
        r[i] = c.r
        g[i] = c.g
        b[i] = c.b
        cov[i] = coverage
        rec[i] = receive
        warmL[i] = 0f
        warmR[i] = 0f
    }
}

private class Window(
    val x0: Int,
    val y0: Int,
    val w: Int,
    val h: Int,
    val right: Boolean,
)

private class Scene(
    private val seed: Int,
    aurora: Rgb,
    window: Rgb,
) {
    private val lat = Lattice(seed)
    private val far = Layer()
    private val fg = Layer()
    private val fgCells: IntArray
    private val windowCol = col(window)
    private val auroraCol = col(aurora)
    private val pane = windowCol.mix(col("#fff2c4"), 0.45f)
    private val palette: Palette
    private val starX = IntArray(STARS)
    private val starY = IntArray(STARS)
    private val starPhase = FloatArray(STARS)
    private val starRate = IntArray(STARS)
    private val starPower = FloatArray(STARS)
    private val flake = Array(FLAKES) { FloatArray(5) }

    // Per band: bottom edge colour, middle and top.
    private val bandCol: Array<Array<Col>>

    private val windows =
        listOf(
            Window(126, 28, 5, 3, false),
            Window(144, 28, 5, 3, true),
            Window(136, 22, 4, 2, false),
            Window(161, 31, 5, 2, true),
        )

    init {
        val violet = col("#9a6ae8")
        bandCol =
            arrayOf(
                arrayOf(auroraCol.mix(col("#ffffff"), 0.35f), auroraCol, violet),
                arrayOf(col("#4fe8cf"), col("#2cb5a6").mix(auroraCol, 0.3f), col("#7a7ae8")),
                arrayOf(col("#f06ab8"), col("#b05ad0"), col("#6a5ad8")),
                arrayOf(col("#7af0a8"), col("#3acf9a").mix(auroraCol, 0.3f), col("#6a8ae8")),
            )
        buildFar()
        buildForeground()
        fgCells = (0 until CELLS).filter { fg.mat[it] != AIR }.toIntArray()
        palette = Palette(buildPalette(window))
        val rng = AKit.rng(seed, 41)
        for (s in 0 until STARS) {
            var x: Int
            var y: Int
            do {
                x = rng.int(COLS)
                y = rng.int(WL - 7)
            } while (far.mat[y * COLS + x] != AIR || fg.mat[y * COLS + x] != AIR)
            starX[s] = x
            starY[s] = y
            starPhase[s] = rng.float()
            starRate[s] = 2 + rng.int(3)
            starPower[s] = 0.35f + 0.65f * rng.float()
        }
        for (f in flake) {
            f[0] = rng.float(0f, COLS.toFloat())
            f[1] = rng.float()
            f[2] = if (rng.chance(0.35f)) 2f else 1f
            f[3] = rng.float()
            f[4] = rng.float(0.6f, 2.0f)
        }
    }

    private fun buildPalette(window: Rgb): List<Col> {
        val black = col("#000000")
        val white = col("#ffffff")
        val aur = auroraCol
        return ramp(6, col("#1a2250"), col("#2c3b7a"), col("#4a5fa8")) +
            ramp(7, col("#2d2060"), col("#6a3c9f"), col("#c48ae4")) +
            ramp(9, aur.mix(black, 0.7f), aur, aur.mix(white, 0.7f)) +
            ramp(6, col("#0f4f58"), col("#2cb5a6"), col("#a8f0e0")) +
            ramp(9, col("#3a4a7e"), col("#8ea6d6"), col("#f4f8ff")) +
            ramp(8, col("#4a1f10"), col(window), col("#fff2c4")) +
            ramp(7, col("#2e1d14"), col("#6b4a32"), col("#b0814c")) +
            ramp(6, col("#0e2230"), col("#1d4a4a"), col("#3a7060")) +
            ramp(5, col("#2f3650"), col("#5b6478"), col("#a0a8bd")) +
            ramp(7, col("#16304c"), col("#2d6a88"), col("#9fd8e8")) +
            ramp(4, col("#6a2a6a"), col("#d65aa8"), col("#ffc0e0"))
    }

    // ---- the static layers, built once -----------------------------------------------------------------------

    private fun farHeight(x: Float): Float =
        4.2f + 4f * lat.fbm(x * 0.013f + 3f, 1.7f, 3) + 9f * gauss(x, 30f, 22f) + 8f * gauss(x, 92f, 16f) +
            3.5f * gauss(x, 60f, 9f) + 9f * gauss(x, 186f, 14f) + 1.5f * (1f - abs(2f * lat.at(x * 0.1f, 9f) - 1f))

    private fun nearHeight(x: Float): Float {
        val hill = 2.4f + 2.4f * lat.fbm(x * 0.03f + 20f, 5f, 2)
        val plateau = smooth(104f, 120f, x) * smooth(190f, 178f, x)
        return hill + (1.8f - hill) * plateau
    }

    private fun buildFar() {
        val haze = col("#6a7cb8")
        val shadow = col("#34448a")
        val lit = col("#93a8de")
        val snowShade = col("#8aa0d4")
        val snowDark = col("#3a4a80")
        for (x in 0 until COLS) {
            val farTop = WL - farHeight(x + 0.5f)
            val nearTop = WL - nearHeight(x + 0.5f)
            for (y in 0 until WL) {
                val yc = y + 0.5f
                if (yc >= nearTop) {
                    val depth = yc - nearTop
                    val drift = lat.fbm(x * 0.12f, yc * 0.45f, 2)
                    val shade = clamp01(0.25f + 0.85f * drift - depth * 0.05f)
                    far.set(
                        x,
                        y,
                        NEAR,
                        snowDark.mix(snowShade, shade),
                        0.78f + 0.2f * shade,
                        0.85f * exp(-depth / 3f) + 0.15f,
                    )
                } else if (yc >= farTop) {
                    val depth = yc - farTop
                    val rib = lat.at((x + depth * 0.55f) * 0.2f, 4f)
                    val shade = clamp01(0.15f + 1.1f * rib - depth * 0.008f)
                    val c = shadow.mix(lit, shade).mix(haze, smooth(8f, 22f, depth) * 0.5f)
                    far.set(x, y, FAR, c, 0.7f + 0.3f * shade, 0.9f * exp(-depth / 5f) + 0.1f)
                }
            }
        }
        val rng = AKit.rng(seed, 7)
        repeat(104) {
            val px = rng.float(0f, COLS.toFloat())
            if (px in 114f..178f) return@repeat
            val h = rng.float(4f, 8f)
            paintPine(far, px, WL - nearHeight(px) + 1.6f, h, 0.45f, false)
        }
        var tx = 4f
        while (tx < COLS) {
            val px = tx + rng.float(-2f, 2f)
            if (px !in 112f..178f) {
                val h = rng.float(8f, 13.5f)
                paintPine(far, px, WL - nearHeight(px) + 2f, h, 0.18f, false)
            }
            tx += rng.float(6f, 10f)
        }
        for (px in floatArrayOf(113f, 116f, 177f, 180f)) {
            paintPine(far, px, WL - nearHeight(px) + 1.8f, rng.float(8f, 12f), 0.18f, false)
        }
        paintCabin()
        paintIce()
    }

    private fun paintCabin() {
        val snow = col("#e4eeff")
        val shingle = col("#5a4e8c")
        val wood = col("#9a6a40")
        val woodDark = col("#6e4a2c")
        val planks = col("#a87a4a")
        val stone = col("#5b6478")
        val trim = col("#d8b98a")
        for (y in 12 until WL) {
            for (x in 112 until 182) {
                val xc = x + 0.5f
                val yc = y + 0.5f
                val roofY = APEX_Y + abs(xc - APEX_X) * ROOF_SLOPE
                val inRoofSpan = abs(xc - APEX_X) <= 17.6f
                val thick = 2.6f + 0.5f * lat.at(xc * 0.4f, 3f)
                val annexTop = 27.2f + (xc - 152f) * 0.1f
                when {
                    xc in 144f..147f && yc in CHIMNEY_TOP..(roofY + 1f) && !(inRoofSpan && yc >= roofY) -> {
                        val k = lat.at(xc * 1.3f, yc * 0.9f)
                        val cap = yc < CHIMNEY_TOP + 1.2f
                        far.set(
                            x,
                            y,
                            STONE,
                            if (cap) snow else stone.scale(0.8f + 0.5f * k),
                            if (cap) 1.98f else 1.6f,
                            if (cap) 1f else 0.3f,
                        )
                    }

                    inRoofSpan && yc >= roofY && yc < roofY + thick -> {
                        val snowy = yc < roofY + 1.7f + 0.4f * lat.at(xc * 0.6f, 8f)
                        if (snowy) {
                            far.set(x, y, ROOF, snow, 1.98f, 1f)
                        } else {
                            far.set(x, y, ROOF, shingle, 1.6f, 0.2f)
                        }
                    }

                    inRoofSpan && yc >= roofY + thick && yc < EAVES_Y -> {
                        val plank = if (x % 2 == 0) planks else planks.scale(0.82f)
                        far.set(x, y, WALL, plank, 0.95f, 0.1f)
                    }

                    xc in 123.5f..152.5f && yc in EAVES_Y..BASE_Y -> {
                        val log = if (y % 2 == 0) wood else woodDark
                        val end = xc < 125f || xc > 151f
                        far.set(x, y, WALL, if (end) wood.mix(trim, 0.35f) else log, 0.95f, 0.1f)
                    }

                    xc in 152f..175f && yc >= annexTop && yc < annexTop + 2.4f -> {
                        val edge = yc > annexTop + 1.3f
                        if (edge) {
                            far.set(x, y, ROOF, shingle, 1.6f, 0.2f)
                        } else {
                            far.set(x, y, ROOF, snow, 1.98f, 1f)
                        }
                    }

                    xc in 153f..174f && yc >= annexTop + 2.4f && yc < BASE_Y -> {
                        far.set(x, y, WALL, if (y % 2 == 0) wood else woodDark, 0.95f, 0.1f)
                    }
                }
            }
        }
        for (w in windows) paintWindow(w, trim)
        for (y in 29 until 34) {
            for (x in 136 until 140) {
                far.set(x, y, WALL, if (x == 139 && y == 31) trim else col("#4a2f20"), 1f, 0.05f)
            }
        }
        spillWarm()
    }

    private fun paintWindow(
        w: Window,
        trim: Col,
    ) {
        for (y in w.y0 - 1..w.y0 + w.h) {
            for (x in w.x0 - 1..w.x0 + w.w) {
                val inside = x in w.x0 until w.x0 + w.w && y in w.y0 until w.y0 + w.h
                if (!inside) {
                    far.set(x, y, WALL, trim.scale(0.75f), 1f, 0.1f)
                } else {
                    val bar = x == w.x0 + w.w / 2
                    far.set(x, y, if (bar) WALL else PANE, if (bar) col("#5a3a22") else windowCol, 1f, 0f)
                    val i = y * COLS + x
                    if (w.right) far.warmR[i] = 1f else far.warmL[i] = 1f
                }
            }
        }
    }

    /** Window light spilling onto what stands near the windows, each window with its own flicker. */
    private fun spillWarm() {
        for (w in windows) {
            val cx = w.x0 + w.w / 2f
            val cy = w.y0 + w.h / 2f
            val radius = if (w.h == 2) 12f else 19f
            for (y in 10 until WL) {
                for (x in 100 until COLS) {
                    val i = y * COLS + x
                    val m = far.mat[i]
                    if (m == AIR || m == PANE || m == FAR) continue
                    val dx = x + 0.5f - cx
                    val dy = (y + 0.5f - cy) * 2.2f
                    val d = sqrt(dx * dx + dy * dy)
                    if (d >= radius) continue
                    val k = (1f - d / radius).let { it * it }
                    val gain =
                        if (m == NEAR) {
                            1.0f
                        } else if (m == WALL) {
                            0.45f
                        } else if (m == TREE) {
                            0.5f
                        } else {
                            0.25f
                        }
                    if (w.right) far.warmR[i] += k * gain else far.warmL[i] += k * gain
                }
            }
        }
    }

    private fun paintIce() {
        val top = col("#245c7c")
        val bottom = col("#0f2440")
        val crack = col("#9fd8e8")
        val patch = col("#a8bbe0")
        for (y in WL until ROWS) {
            val k = (y - WL) / (ROWS - WL - 1f)
            for (x in 0 until COLS) {
                val i = y * COLS + x
                val n = lat.fbm(x * 0.05f, y * 0.3f + 40f, 3)
                val c = top.mix(bottom, k * 0.85f).mix(top, 0.3f * n)
                val line = abs(lat.fbm(x * 0.035f + 70f, y * 0.16f, 2) - 0.5f)
                val snowy = lat.fbm(x * 0.03f + 11f, y * 0.2f + 5f, 3)
                far.set(x, y, ICE, c, 0.2f + 0.1f * n, 0.3f)
                far.refl[i] = 1f
                if (line < 0.018f) {
                    far.set(x, y, ICE, crack, 0.55f, 0.5f)
                    far.refl[i] = 0.5f
                }
                if (snowy > 0.6f + 0.1f * (1f - k)) {
                    val s = smooth(0.6f, 0.68f, snowy)
                    far.set(x, y, SOLID, c.mix(patch, s), 0.45f + 0.4f * s, 0.9f * s + 0.3f)
                    far.refl[i] = 0.15f * (1f - s)
                }
            }
        }
    }

    private fun buildForeground() {
        val snowDark = col("#3a4a7e")
        val snowLit = col("#a9bddd")
        for (x in 0 until COLS) {
            val left = if (x < 66) 41f + 4.5f * smooth(0f, 66f, x.toFloat()) else 99f
            val right = if (x > 150) 42f + 3f * smooth(192f, 150f, x.toFloat()) else 99f
            val top = min(left, right) + 1f * (lat.at(x * 0.09f, 2f) - 0.5f)
            for (y in 0 until ROWS) {
                if (y + 0.5f < top) continue
                val n = lat.fbm(x * 0.14f, y * 0.6f, 2)
                val c = snowDark.mix(snowLit, clamp01(0.35f + 0.9f * n - (y + 0.5f - top) * 0.05f))
                fg.set(x, y, SOLID, c, 0.88f, 0.95f)
            }
        }
        paintPine(fg, 12f, 43.5f, 32f, 0f, true)
        paintPine(fg, 33f, 46f, 20f, 0f, true)
        paintPine(fg, 58f, 47f, 12f, 0f, true)
        paintPine(fg, 185f, 45f, 29f, 0f, true)
    }

    private fun paintPine(
        l: Layer,
        px: Float,
        baseY: Float,
        h: Float,
        haze: Float,
        foreground: Boolean,
    ) {
        val halfW = h * (if (foreground) 0.4f else 0.5f)
        val dark = if (foreground) col("#0e2230") else col("#12303c")
        val mid = if (foreground) col("#1b4046") else col("#245358")
        val tip = col("#a9bddd")
        val hazeCol = col("#34407a")
        val tier = max(2.4f, h * 0.11f)
        val topY = baseY - h
        for (y in max(0, floor(topY).toInt())..min(ROWS - 1, baseY.toInt())) {
            val yc = y + 0.5f
            val d = yc - topY
            if (d < 0f) continue
            val fr = fract(d / tier)
            val w = max(0.65f, halfW * (d / h) * (0.62f + 0.38f * fr) + 0.8f * (lat.at(yc * 0.9f, px) - 0.5f))
            for (x in floor(px - w).toInt()..floor(px + w).toInt() + 1) {
                val xc = x + 0.5f
                if (abs(xc - px) > w) continue
                val side = ((xc - px) / w).coerceIn(-1f, 1f)
                val n = lat.at(xc * 0.5f, yc * 0.8f)
                if (fr < 0.38f && n > 0.42f && d > 1.5f) {
                    l.set(x, y, TREE, tip.scale(0.6f + 0.4f * n).mix(hazeCol, haze), 0.95f, 0.9f)
                } else {
                    val c = mid.mix(dark, clamp01(0.5f + 0.5f * side + 0.2f * n)).mix(hazeCol, haze)
                    l.set(x, y, TREE, c, if (foreground || haze < 0.3f) 1.6f else 0.9f, 0.25f)
                }
            }
        }
    }

    // ---- one frame --------------------------------------------------------------------------------------------

    private class Frame {
        val r = FloatArray(CELLS)
        val g = FloatArray(CELLS)
        val b = FloatArray(CELLS)
        val cov = FloatArray(CELLS)
        val lr = FloatArray(COLS)
        val lg = FloatArray(COLS)
        val lb = FloatArray(COLS)
        val bottom = FloatArray(BANDS * COLS)
        val height = FloatArray(BANDS * COLS)
        val gain = FloatArray(BANDS * COLS)
    }

    private val base = floatArrayOf(24f, 19f, 27f, 15.5f)
    private val amp1 = floatArrayOf(4.5f, 3.8f, 3f, 2.6f)
    private val freq1 = floatArrayOf(0.052f, 0.07f, 0.043f, 0.06f)
    private val turns1 = intArrayOf(1, 2, 1, 1)
    private val phase1 = floatArrayOf(0f, 1.9f, 4.1f, 2.7f)
    private val amp2 = floatArrayOf(2f, 1.6f, 1.3f, 1.3f)
    private val freq2 = floatArrayOf(0.13f, 0.16f, 0.11f, 0.14f)
    private val turns2 = intArrayOf(2, 1, 3, 2)
    private val phase2 = floatArrayOf(2f, 0.5f, 3.3f, 5.1f)
    private val tall = floatArrayOf(24f, 17f, 10f, 13f)
    private val strength = floatArrayOf(1f, 0.8f, 0.65f, 0.45f)
    private val tilt = floatArrayOf(0.03f, -0.025f, 0.02f, -0.015f)

    private fun curtains(
        f: Frame,
        phi: Float,
    ) {
        val cs = kotlin.math.cos(phi)
        val sn = sin(phi)
        for (b in 0 until BANDS) {
            for (x in 0 until COLS) {
                val k = b * COLS + x
                val xf = x.toFloat()
                val pres = smooth(0.22f, 0.45f, lat.at(xf * 0.03f + b * 17.3f + 2.2f * cs, b * 5.1f + 2.2f * sn))
                val n2 = lat.at(xf * 0.17f + b * 31f + 1.6f * sin(phi + b), 7.3f * b + 1.6f * kotlin.math.cos(phi * 2f))
                val n3 = lat.at(xf * 0.45f + b * 5f + 1.2f * cs, 3f + 1.2f * sn)
                val ray = 0.1f + 1.5f * (0.65f * n2 + 0.35f * n3).let { it * it }
                f.bottom[k] =
                    base[b] + (xf - 96f) * tilt[b] +
                    amp1[b] * sin(freq1[b] * xf + turns1[b] * phi + phase1[b]) +
                    amp2[b] * sin(freq2[b] * xf - turns2[b] * phi + phase2[b])
                val env = lat.at(xf * 0.024f + b * 9f + 1.6f * sn, b * 3f + 1.6f * cs + 11f)
                f.height[k] = tall[b] * (0.55f + 0.45f * n2 + 0.55f * env)
                f.gain[k] = pres * ray * strength[b]
            }
        }
        for (x in 0 until COLS) {
            var r = 0f
            var g = 0f
            var bl = 0f
            for (b in 0 until BANDS) {
                val a = f.gain[b * COLS + x] * 0.3f
                val m = bandCol[b][1]
                r += m.r * a
                g += m.g * a
                bl += m.b * a
            }
            f.lr[x] = r
            f.lg[x] = g
            f.lb[x] = bl
        }
        repeat(2) {
            for (x in 1 until COLS - 1) {
                f.lr[x] = (f.lr[x - 1] + 2f * f.lr[x] + f.lr[x + 1]) * 0.25f
                f.lg[x] = (f.lg[x - 1] + 2f * f.lg[x] + f.lg[x + 1]) * 0.25f
                f.lb[x] = (f.lb[x - 1] + 2f * f.lb[x] + f.lb[x + 1]) * 0.25f
            }
        }
    }

    private fun sky(
        f: Frame,
        x: Int,
        y: Int,
        phi: Float,
    ) {
        val i = y * COLS + x
        val yc = y + 0.5f
        val h = yc / WL
        var r = 0.10f + 0.07f * h + f.lr[x] * 0.18f
        var g = 0.13f + 0.08f * h + f.lg[x] * 0.18f
        var bl = 0.31f + 0.14f * h + f.lb[x] * 0.18f
        var amount = 0f
        for (b in 0 until BANDS) {
            val k = b * COLS + x
            val gain = f.gain[k]
            if (gain < 0.01f) continue
            val d = (f.bottom[k] - yc) / f.height[k]
            if (d < -0.25f || d > 1f) continue
            val prof = if (d < 0f) 1f + 4f * d else (1f - d) * (1f - d)
            val a = prof * gain * 2.4f
            val c = bandCol[b]
            val u = clamp01(d)
            val from = if (u < 0.5f) c[0] else c[1]
            val to = if (u < 0.5f) c[1] else c[2]
            val m = if (u < 0.5f) u * 2f else (u - 0.5f) * 2f
            r += (from.r + (to.r - from.r) * m) * a
            g += (from.g + (to.g - from.g) * m) * a
            bl += (from.b + (to.b - from.b) * m) * a
            amount += a
        }
        if (amount > 0.02f) {
            val patch = 0.8f + 0.4f * lat.at(x * 0.08f + 4f * kotlin.math.cos(phi), y * 0.2f + 4f * sin(phi))
            r = 0.1f + (r - 0.1f) * patch
            g = 0.13f + (g - 0.13f) * patch
        }
        f.r[i] = r
        f.g[i] = g
        f.b[i] = bl
        f.cov[i] = 0.1f + 0.95f * clamp01(amount * 1.1f)
    }

    private fun smoke(
        f: Frame,
        t: Float,
    ) {
        for (p in 0 until PUFFS) {
            val age = fract(3f * t / LOOP + p / PUFFS.toFloat())
            val cx = CHIMNEY_X + age * age * 15f + 1.8f * sin(TAU * age * 1.5f + p)
            val cy = CHIMNEY_TOP - 0.5f - age * 17f
            val rad = 2f + age * 7f
            val dens = 2f * smooth(0f, 0.1f, age) * (1f - age) * (1f - age)
            for (y in floor(cy - rad / 2.2f).toInt()..floor(cy + rad / 2.2f).toInt() + 1) {
                for (x in floor(cx - rad).toInt()..floor(cx + rad).toInt() + 1) {
                    if (x !in 0 until COLS || y !in 0 until WL) continue
                    val dx = x + 0.5f - cx
                    val dy = (y + 0.5f - cy) * 2.2f
                    val q = 1f - (dx * dx + dy * dy) / (rad * rad)
                    if (q <= 0f) continue
                    val i = y * COLS + x
                    val a = min(0.95f, q * dens * 1.8f)
                    f.r[i] += (0.74f + f.lr[x] * 0.6f - f.r[i]) * a
                    f.g[i] += (0.78f + f.lg[x] * 0.6f - f.g[i]) * a
                    f.b[i] += (0.9f + f.lb[x] * 0.4f - f.b[i]) * a
                    f.cov[i] += (1.7f - f.cov[i]).coerceAtLeast(0f) * a
                }
            }
        }
    }

    private fun land(
        f: Frame,
        x: Int,
        y: Int,
        fl: Float,
        fr: Float,
    ) {
        val i = y * COLS + x
        val m = far.mat[i]
        if (m == PANE) {
            val k = if (far.warmR[i] > 0f) fr else fl
            val sc = 0.7f + 0.4f * k
            f.r[i] = pane.r * sc
            f.g[i] = pane.g * sc
            f.b[i] = pane.b * sc
            f.cov[i] = if (k > 0.8f) 2f else 1.9f
            return
        }
        val rec = far.rec[i]
        val warm = far.warmL[i] * fl + far.warmR[i] * fr
        val ar = f.lr[x] * rec
        val ag = f.lg[x] * rec
        val ab = f.lb[x] * rec
        f.r[i] = far.r[i] + ar + windowCol.r * warm * 0.7f
        f.g[i] = far.g[i] + ag + windowCol.g * warm * 0.55f
        f.b[i] = far.b[i] + ab + windowCol.b * warm * 0.35f
        f.cov[i] =
            if (far.cov[i] > 1.5f) {
                far.cov[i]
            } else {
                min(1f, far.cov[i] + 0.7f * (0.3f * ar + 0.59f * ag + 0.11f * ab) + 0.35f * warm)
            }
    }

    private fun lake(
        f: Frame,
        phi: Float,
    ) {
        for (y in WL until ROWS) {
            val d = (y - WL).toFloat()
            val wobble = 0.35f + 0.9f * d / (ROWS - WL - 1f)
            val ys = max(0, (WL - 1 - d * 1.4f).toInt())
            for (x in 0 until COLS) {
                val i = y * COLS + x
                val sx = (x + wobble * sin(0.85f * y + 0.21f * x + 2f * phi)).roundToInt().coerceIn(0, COLS - 1)
                val j = ys * COLS + sx
                val shimmer = 0.85f + 0.15f * sin(1.9f * y + 3f * phi + 0.15f * x)
                val w = far.refl[i] * (0.85f - 0.015f * d) * shimmer * (if (far.mat[j] == AIR) 0.45f else 1f)
                val rec = far.rec[i] * 0.5f
                f.r[i] = far.r[i] * (1f - w) + f.r[j] * w * 0.8f + f.lr[x] * rec
                f.g[i] = far.g[i] * (1f - w) + f.g[j] * w * 0.8f + f.lg[x] * rec
                f.b[i] = far.b[i] * (1f - w) + f.b[j] * w * 0.8f + f.lb[x] * rec
                val lum = 0.3f * f.lr[x] + 0.59f * f.lg[x] + 0.11f * f.lb[x]
                f.cov[i] = min(1f, far.cov[i] * (1f - w) + min(1f, f.cov[j] * 0.9f + 0.1f) * w + lum * rec * 0.6f)
            }
        }
    }

    private fun foreground(f: Frame) {
        for (i in fgCells) {
            val x = i % COLS
            val rec = fg.rec[i]
            val ar = f.lr[x] * rec
            val ag = f.lg[x] * rec
            val ab = f.lb[x] * rec
            f.r[i] = fg.r[i] + ar
            f.g[i] = fg.g[i] + ag
            f.b[i] = fg.b[i] + ab
            f.cov[i] =
                if (fg.cov[i] > 1.5f) {
                    fg.cov[i]
                } else {
                    min(1f, fg.cov[i] + 0.5f * (0.3f * ar + 0.59f * ag + 0.11f * ab))
                }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = TAU * t / LOOP
        val f = Frame()
        curtains(f, phi)
        val fl = clamp01(0.8f + 0.1f * sin(phi * 5f + 0.6f) + 0.08f * sin(phi * 11f + 2f))
        val fr = clamp01(0.78f + 0.12f * sin(phi * 7f + 1.9f) + 0.08f * sin(phi * 13f + 0.3f))
        for (y in 0 until WL) {
            for (x in 0 until COLS) {
                if (far.mat[y * COLS + x] == AIR) sky(f, x, y, phi) else land(f, x, y, fl, fr)
            }
        }
        smoke(f, t)
        lake(f, phi)
        foreground(f)
        output(f, grid)
        stars(f, grid, t)
        flakes(grid, t)
    }

    private fun output(
        f: Frame,
        grid: Grid,
    ) {
        for (y in 0 until ROWS) {
            for (x in 0 until COLS) {
                val i = y * COLS + x
                val c = f.cov[i]
                if (c > 1.5f) {
                    grid.glyphs[i] = if (c > 1.95f) '█' else '▓'
                    grid.colors[i] = palette.pick(f.r[i], f.g[i], f.b[i])
                    continue
                }
                val q = c * 3f + BAYER[(y and 3) * 4 + (x and 3)]
                val idx = min(3, (q + 0.5f).toInt())
                if (idx <= 0) {
                    grid.glyphs[i] = ' '
                    grid.colors[i] = 0
                } else {
                    grid.glyphs[i] = GLYPHS[idx]
                    grid.colors[i] = palette.pick(f.r[i], f.g[i], f.b[i])
                }
            }
        }
    }

    private fun stars(
        f: Frame,
        grid: Grid,
        t: Float,
    ) {
        for (s in 0 until STARS) {
            val i = starY[s] * COLS + starX[s]
            if (f.cov[i] > 0.6f) continue
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
            grid.colors[i] = palette.pick(0.55f + 0.45f * tw, 0.6f + 0.4f * tw, 0.85f + 0.15f * tw)
        }
    }

    private fun flakes(
        grid: Grid,
        t: Float,
    ) {
        for (fk in flake) {
            val y = (fract(fk[1] + fk[2] * t / LOOP) * (ROWS + 2)).toInt() - 1
            val x = (fk[0] + fk[4] * sin(TAU * (2f * t / LOOP + fk[3]))).toInt()
            if (x !in 0 until COLS || y !in 0 until ROWS) continue
            val i = y * COLS + x
            grid.glyphs[i] = if (fk[2] > 1.5f) '*' else '.'
            grid.colors[i] = palette.pick(0.85f, 0.9f, 1f)
        }
    }
}

/**
 * A snowy night by a frozen lake: aurora curtains with rays sweep over a far shore of ridges and pines, a log cabin
 * glows from its windows and sends smoke up the chimney, and the dark ice mirrors the lot with a slow shimmer. Every
 * cell is computed from its position and the time, shaded in its own colour and drawn as a four level dithered
 * halftone. The seed shapes the aurora, the ridges, the pines, the stars and the snow. Every motion completes a
 * whole number of cycles per loop.
 */
public fun auroraCabinPiece(
    seed: Int = 0,
    aurora: Rgb = AKit.Green,
    window: Rgb = AKit.Amber,
): AsciiPiece {
    val scene = Scene(seed, aurora, window)
    return asciiPiece(
        name = "aurora cabin",
        ground = Rgb.fromHex("#05080f"),
        category = "scenes",
        cols = COLS,
        rows = ROWS,
        fps = 15,
        still = 5f,
        loop = LOOP,
        note = "aurora curtains over a log cabin by a frozen lake, with smoke, lit windows and a reflection",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A snowy night: aurora curtains over a log cabin by a frozen lake, with lit windows, chimney smoke, pines, twinkling
 * stars and falling snow.
 *
 * @param seed shapes the aurora, the ridges, the pines, the stars and the snow.
 * @param aurora the main color of the aurora curtains.
 * @param window the color of the lit windows.
 */
@Composable
public fun AsciiAuroraCabin(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    aurora: Rgb = AKit.Green,
    window: Rgb = AKit.Amber,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A log cabin by a frozen lake at night, with aurora curtains waving in the sky",
) {
    val piece = remember(seed, aurora, window) { auroraCabinPiece(seed, aurora, window) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
