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
import kotlin.math.ceil
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
private const val CELLS = COLS * ROWS
private const val FPS = 15
private const val LOOP = 22f
private const val TAU = AKit.TAU
private const val ASPECT = 2.2f
private const val STARS = 150

// The scene, in cells: the rocket's axis, the pad surface and the horizon.
private const val CX = 104f
private const val BASE = 41f
private const val HORIZON = 38f
private const val ROCKET_HEIGHT = 33f
private const val BOOSTER_X = 8.2f

// The countdown, in seconds of the loop.
private const val T_IGNITION = 5f
private const val T_LIFTOFF = 7.6f
private const val T_GONE = 14.8f
private const val T_REVEAL = 19.5f
private const val REVEAL_TIME = 2f

private const val GLYPHS = " ·•●"

private val BAYER =
    floatArrayOf(0f, 8f, 2f, 10f, 12f, 4f, 14f, 6f, 3f, 11f, 1f, 9f, 15f, 7f, 13f, 5f)
        .map { (it + 0.5f) / 16f - 0.5f }
        .toFloatArray()

private class RkCol(
    val r: Float,
    val g: Float,
    val b: Float,
) {
    fun mix(
        o: RkCol,
        t: Float,
    ): RkCol = RkCol(r + (o.r - r) * t, g + (o.g - g) * t, b + (o.b - b) * t)

    fun scale(k: Float): RkCol = RkCol(r * k, g * k, b * k)
}

private fun rk(hex: String): RkCol = rk(Rgb.fromHex(hex))

private fun rk(c: Rgb): RkCol = RkCol(c.red / 255f, c.green / 255f, c.blue / 255f)

private fun rkRamp(
    steps: Int,
    vararg stops: RkCol,
): List<RkCol> =
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
private class RkLattice(
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
private class RkPalette(
    colors: List<RkCol>,
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

/** The working buffers of one frame: colour, glyph coverage, flame heat, smoke density and the flame's light. */
private class RkFrame {
    val r = FloatArray(CELLS)
    val g = FloatArray(CELLS)
    val b = FloatArray(CELLS)
    val cov = FloatArray(CELLS)
    val heat = FloatArray(CELLS)
    val dens = FloatArray(CELLS)
    val light = FloatArray(CELLS)
}

private class RkScene(
    private val seed: Int,
    flame: Rgb,
    trim: Rgb,
) {
    private val lat = RkLattice(seed)
    private val flameCol = rk(flame)
    private val trimCol = rk(trim)
    private val palette: RkPalette

    // The static scene: everything except the rocket, the arms, the flame and the smoke.
    private val bgR = FloatArray(CELLS)
    private val bgG = FloatArray(CELLS)
    private val bgB = FloatArray(CELLS)
    private val bgCov = FloatArray(CELLS)
    private val bgRec = FloatArray(CELLS)
    private val starX = IntArray(STARS)
    private val starY = IntArray(STARS)
    private val starPhase = FloatArray(STARS)
    private val starRate = IntArray(STARS)
    private val starPower = FloatArray(STARS)
    private val heatOut = FloatArray(3)

    init {
        buildSky()
        buildBeams()
        buildHills()
        buildGround()
        buildStructures()
        buildTower()
        palette = RkPalette(buildPalette())
        val rng = AKit.rng(seed, 41)
        for (s in 0 until STARS) {
            var x: Int
            var y: Int
            do {
                x = rng.int(COLS)
                y = rng.int(26)
            } while (bgCov[y * COLS + x] > 0.3f)
            starX[s] = x
            starY[s] = y
            starPhase[s] = rng.float()
            starRate[s] = 3 + rng.int(6)
            starPower[s] = 0.4f + 0.6f * rng.float()
        }
    }

    private fun buildPalette(): List<RkCol> {
        val white = rk("#ffffff")
        return rkRamp(7, rk("#05071a"), rk("#0f1838"), rk("#1c2a5c")) +
            rkRamp(7, rk("#2a2058"), rk("#5a3a78"), rk("#b0587a")) +
            rkRamp(9, rk("#8a3a4a"), rk("#e0703a"), rk("#ffb865"), rk("#fff0c0")) +
            rkRamp(8, rk("#0a0e1c"), rk("#1e2840"), rk("#3a4868"), rk("#6a7898"), rk("#aab4cc")) +
            rkRamp(9, rk("#20243a"), rk("#50587a"), rk("#8a92b0"), rk("#c4cade"), rk("#f4f6ff")) +
            rkRamp(7, rk("#2a0a10"), trimCol.scale(0.5f), trimCol, trimCol.mix(white, 0.35f)) +
            rkRamp(
                10,
                rk("#3a0a05"),
                rk("#b02a0a"),
                flameCol.mix(rk("#ff4a10"), 0.4f),
                flameCol.mix(white, 0.3f),
                rk("#fff4cc"),
            ) +
            rkRamp(9, rk("#1c2030"), rk("#3a3e56"), rk("#626682"), rk("#9a9cb4"), rk("#d0d0e0")) +
            rkRamp(8, rk("#3a2418"), rk("#80482a"), rk("#d08a50"), rk("#ffc888")) +
            rkRamp(6, rk("#14182a"), rk("#2c3450"), rk("#586480"), rk("#8c98b8")) +
            rkRamp(5, rk("#101830"), rk("#2a3a6a"), rk("#6a80b0"), rk("#c8d8f0")) +
            rkRamp(5, rk("#1a1830"), rk("#2e2444"), rk("#5a3a58"), rk("#8a4a50")) +
            rkRamp(4, rk("#5a3a14"), rk("#d89a3a"), rk("#ffe090")) +
            rkRamp(3, rk("#0b1a2a"), rk("#2a6aa0"), rk("#80d0ff")) +
            rkRamp(3, rk("#1a4a2a"), rk("#3aff7a"), rk("#c0ffd0"))
    }

    // ---- the static layers, built once -----------------------------------------------------------------------

    private fun put(
        x: Int,
        y: Int,
        c: RkCol,
        coverage: Float,
        rec: Float,
    ) {
        if (x !in 0 until COLS || y !in 0 until ROWS) return
        val i = y * COLS + x
        bgR[i] = c.r
        bgG[i] = c.g
        bgB[i] = c.b
        bgCov[i] = coverage
        bgRec[i] = rec
    }

    private fun glowX(x: Float): Float = 0.4f + 0.6f * gauss(x, 150f, 75f)

    private fun buildSky() {
        val top = rk("#05071a")
        val mid = rk("#16224e")
        val dusk = rk("#6a3a78")
        val dawn = rk("#e0703a")
        val bright = rk("#ffc070")
        for (y in 0 until ROWS) {
            for (x in 0 until COLS) {
                val v = clamp01((y + 0.5f) / HORIZON)
                val dh = 35f - (y + 0.5f)
                val glow = exp(-(dh / 9f) * (dh / 9f)) * glowX(x + 0.5f)
                var c = top.mix(mid, v.pow(1.6f))
                c = c.mix(dusk, clamp01(glow * 1.5f))
                c = c.mix(dawn, smooth(0.35f, 0.8f, glow))
                c = c.mix(bright, smooth(0.75f, 1f, glow))
                val streak = smooth(0.55f, 0.85f, lat.fbm(x * 0.02f + 9f, y * 0.55f, 3)) * smooth(18f, 32f, y.toFloat())
                c = c.mix(rk("#c05a50"), streak * 0.35f * glowX(x + 0.5f))
                val cov = 0.03f + 0.3f * v * v + 0.8f * glow + 0.22f * streak
                put(x, y, c, min(1f, cov), 0.12f)
            }
        }
    }

    private fun beam(
        lx: Float,
        ly: Float,
        tx: Float,
        ty: Float,
    ) {
        val ax = tx - lx
        val ay = (ty - ly) * ASPECT
        val len = sqrt(ax * ax + ay * ay)
        val ux = ax / len
        val uy = ay / len
        for (y in 0 until ROWS) {
            for (x in 0 until COLS) {
                val vx = x + 0.5f - lx
                val vy = (y + 0.5f - ly) * ASPECT
                val along = vx * ux + vy * uy
                if (along < 3f) continue
                val perp = abs(vx * -uy + vy * ux)
                val hw = 1.2f + along * 0.17f
                if (perp >= hw) continue
                val q = 1f - perp / hw
                val k = q * q * (1f - smooth(len * 0.9f, len * 1.3f, along)) * (1f / (1f + along / 90f)) * 1.1f
                val i = y * COLS + x
                bgCov[i] = min(1f, bgCov[i] + 0.55f * k)
                bgR[i] += 0.5f * k * 0.5f
                bgG[i] += 0.62f * k * 0.5f
                bgB[i] += 0.9f * k * 0.5f
            }
        }
    }

    private fun buildBeams() {
        beam(37f, 18f, CX - 2f, 27f)
        beam(177f, 21f, CX + 3f, 28f)
    }

    private fun buildHills() {
        val rim = rk("#e08a50")
        val farTop = FloatArray(COLS)
        val nearTop = FloatArray(COLS)
        for (x in 0 until COLS) {
            farTop[x] = 40.5f - (4.2f + 3.6f * lat.fbm(x * 0.022f + 4f, 2f, 3) + 3f * gauss(x.toFloat(), 20f, 14f))
            nearTop[x] = 40.5f - (1.8f + 2.4f * lat.fbm(x * 0.05f + 11f, 6f, 3))
        }
        val rng = AKit.rng(seed, 9)
        for (x in 0 until COLS) {
            for (y in 0..40) {
                val yc = y + 0.5f
                if (yc >= farTop[x]) {
                    val first = yc - farTop[x] < 1f
                    val haze = rk("#1c1634").mix(rk("#3a2848"), glowX(x + 0.5f) * 0.8f * smooth(30f, 40f, yc))
                    val c = if (first) haze.mix(rim, 0.25f * glowX(x + 0.5f)) else haze
                    put(x, y, c, 1.95f, 0.3f)
                }
                if (yc >= nearTop[x]) {
                    val first = yc - nearTop[x] < 1f
                    val c = rk("#080a16").mix(rk("#16162c"), lat.at(x * 0.2f, y * 0.7f))
                    put(x, y, if (first) c.mix(rim, 0.18f * glowX(x + 0.5f)) else c, 2f, 0.4f)
                }
            }
        }
        repeat(34) {
            val x = rng.int(COLS)
            if (x in 66..86 || x in 40..66) return@repeat
            val y = (nearTop[x] - 1f - rng.float(0f, 2f)).toInt().coerceIn(30, 39)
            if (y >= nearTop[x]) return@repeat
            put(x, y, rk("#ffd890"), 1f, 0.2f)
        }
    }

    private fun buildGround() {
        val slab = rk("#586480")
        val slabDark = rk("#2c3450")
        for (y in 41 until ROWS) {
            for (x in 0 until COLS) {
                val pool =
                    0.6f * gauss(x.toFloat(), 37f, 24f) * gauss(y.toFloat(), 44f, 5f) +
                        0.6f * gauss(x.toFloat(), 177f, 24f) * gauss(y.toFloat(), 44f, 5f)
                val noise = lat.at(x * 0.31f, y * 0.9f + 3f)
                val d = (y - 41f) / 6f
                val c = rk("#0b1020").mix(rk("#2a3a6a"), clamp01(pool * 1.4f))
                put(x, y, c, min(1f, 0.1f * (1f - d) + 0.12f * noise + pool * 0.8f), 1f)
            }
        }
        for (y in 41..43) {
            for (x in 56..150) {
                val top = y == 41
                val edge = x < 59 || x > 147
                val base = if (top) slab else slabDark
                val c = base.mix(rk("#8c98b8"), 0.35f * gauss(x.toFloat(), 37f, 40f) * (if (top) 1f else 0.3f))
                put(x, y, if (edge) c.scale(0.75f) else c, if (top) 2f else 1.9f, 1f)
            }
        }
        for (y in 41..43) {
            for (x in (CX - 5.5f).toInt()..(CX + 5.5f).toInt()) {
                put(x, y, rk("#05070e"), 2f, 1f)
            }
        }
        var x = 60
        while (x < 150) {
            if (x < 98 || x > 110) put(x, 43, if ((x / 6) % 2 == 0) rk("#3aff7a") else rk("#ff4040"), 2f, 0.3f)
            x += 6
        }
    }

    private fun mast(
        x: Int,
        top: Int,
    ) {
        val steel = rk("#3a4868")
        for (y in top + 1..44) {
            put(x, y, steel.mix(rk("#8a92b0"), 0.35f), 2f, 0.8f)
            put(x + 1, y, steel.scale(0.7f), 2f, 0.8f)
        }
        val lamp = rk("#fff4cc")
        for (dy in 0..1) {
            for (dx in -4..5) put(x + dx, top - 1 + dy, if (dy == 0) lamp else lamp.mix(steel, 0.35f), 2f, 0.3f)
        }
        for (y in top - 5..top + 3) {
            for (xx in x - 9..x + 10) {
                val dx = (xx + 0.5f - (x + 1f)) / 9f
                val dy = (y + 0.5f - top) * ASPECT / 9f
                val h = exp(-(dx * dx + dy * dy) * 2.2f)
                if (xx !in 0 until COLS || y !in 0 until ROWS) continue
                val i = y * COLS + xx
                if (bgCov[i] > 1.5f) continue
                bgCov[i] = min(1f, bgCov[i] + 0.9f * h)
                bgR[i] += 0.5f * h
                bgG[i] += 0.5f * h
                bgB[i] += 0.4f * h
            }
        }
    }

    private fun buildStructures() {
        mast(36, 17)
        mast(176, 21)
        // A low control bunker on the left.
        for (y in 36..40) {
            for (x in 44..63) {
                val c = if (y == 36) rk("#3a4868") else rk("#12182a").mix(rk("#2a3454"), lat.at(x * 0.3f, y * 0.8f))
                put(x, y, c, 2f, 0.9f)
            }
        }
        for (x in 46..61 step 3) {
            put(x, 38, rk("#ffd070"), 2f, 0.2f)
            put(x + 1, 38, rk("#d89a3a"), 2f, 0.2f)
        }
        // A spherical fuel tank on the right, on short legs.
        val cx = 156f
        val cy = 36.4f
        val rad = 8.6f
        for (y in 31..41) {
            for (x in 146..166) {
                val dx = (x + 0.5f - cx) / rad
                val dy = (y + 0.5f - cy) * ASPECT / rad
                val d2 = dx * dx + dy * dy
                if (d2 >= 1f) continue
                val nz = sqrt(1f - d2)
                val lam = 0.18f + 0.85f * max(0f, -0.55f * dx - 0.35f * dy + 0.75f * nz)
                val rimR = 0.5f * max(0f, dx - 0.4f) * glowX(cx)
                val c =
                    rk(
                        "#8a92b0",
                    ).scale(lam).let { RkCol(it.r + rimR * 0.8f, it.g + rimR * 0.4f, it.b + rimR * 0.2f) }
                put(x, y, c, 2f, 0.8f)
            }
        }
        for (y in 39..41) {
            put(149, y, rk("#3a4868"), 2f, 0.8f)
            put(163, y, rk("#3a4868"), 2f, 0.8f)
        }
    }

    private fun buildTower() {
        val steel = rk("#3a4a6c")
        val lit = rk("#7a8cb4")
        val dark = rk("#1c2540")
        for (y in 12..40) {
            val h = 41 - y
            for (x in 69..82) {
                val leg = x in 69..71 || x in 80..82
                val girder = h % 5 == 0
                var on = leg || girder
                if (!on && x in 72..79) {
                    val bay = h / 5
                    val frac = ((h % 5) + 0.5f) / 5f
                    val u = (x - 72f) / 7f
                    val target = if (bay % 2 == 0) frac else 1f - frac
                    on = abs(u - target) < 0.11f
                }
                if (!on) continue
                val c =
                    when {
                        x == 69 || x == 72 -> lit
                        x == 70 || x == 80 -> steel.mix(lit, 0.45f)
                        x == 82 -> dark
                        else -> steel
                    }
                put(x, y, c, 2f, 1f)
            }
        }
        for (x in 67..84) {
            put(x, 11, steel.mix(lit, 0.5f), 2f, 1f)
            put(x, 12, steel, 2f, 1f)
        }
        for (y in 6..10) {
            put(75, y, lit, 2f, 1f)
            put(76, y, steel, 2f, 1f)
        }
    }

    // ---- the timeline ----------------------------------------------------------------------------------------

    private fun altitude(t: Float): Float {
        if (t < T_LIFTOFF) return 0f
        val d = t - T_LIFTOFF
        return 0.25f * d + 0.85f * d * d
    }

    private fun engineEnv(t: Float): Float = smooth(T_IGNITION, T_IGNITION + 1.7f, t) * (1f - smooth(14.2f, T_GONE, t))

    private fun reveal(t: Float): Float =
        if (t >= T_REVEAL) {
            clamp01((t - T_REVEAL) / REVEAL_TIME)
        } else if (t < T_GONE) {
            1f
        } else {
            0f
        }

    private fun armReach(t: Float): Float =
        when {
            t < 6.9f -> 1f
            t < 7.7f -> 1f - smooth(6.9f, 7.7f, t)
            t < 21f -> 0f
            else -> smooth(21f, 21.8f, t)
        }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val f = RkFrame()
        bgR.copyInto(f.r)
        bgG.copyInto(f.g)
        bgB.copyInto(f.b)
        bgCov.copyInto(f.cov)
        val alt = altitude(t)
        val env = engineEnv(t)
        val ny = BASE - alt
        lighting(f, t, env, ny)
        arms(f, t)
        rocket(f, t, alt)
        flame(f, t, env, ny)
        smoke(f, t, ny)
        output(f, grid)
        stars(f, grid, t)
        sparks(grid, t)
        beacons(grid, t)
    }

    /** The flame lights everything around it; the light fades with the distance to the nozzle. */
    private fun lighting(
        f: RkFrame,
        t: Float,
        env: Float,
        ny: Float,
    ) {
        if (env <= 0.01f) return
        val flick = env * (0.9f + 0.1f * sin(TAU * 151f * t / LOOP))
        for (y in 0 until ROWS) {
            val dy = (y + 0.5f - ny) * ASPECT
            for (x in 0 until COLS) {
                val dx = x + 0.5f - CX
                val d2 = dx * dx + dy * dy
                val l = flick * (0.75f * exp(-d2 / 1800f) + 0.22f * exp(-d2 / 9800f))
                val i = y * COLS + x
                f.light[i] = l
                val k = l * bgRec[i]
                if (k < 0.004f) continue
                f.r[i] = f.r[i] * (1f + 0.9f * k) + 0.2f * k
                f.g[i] = f.g[i] * (1f + 0.5f * k) + 0.1f * k
                f.b[i] = f.b[i] * (1f + 0.15f * k) + 0.03f * k
                if (f.cov[i] < 1.5f) f.cov[i] = min(1.4f, f.cov[i] + 0.7f * k)
            }
        }
    }

    private fun arms(
        f: RkFrame,
        t: Float,
    ) {
        val reach = armReach(t)
        if (reach <= 0.01f) return
        val steel = rk("#5a6a90")
        val specs =
            arrayOf(
                floatArrayOf(29f, CX - 6.3f),
                floatArrayOf(13f, CX - BOOSTER_X - 2.5f),
                floatArrayOf(
                    6f,
                    CX - BOOSTER_X - 2.5f,
                ),
            )
        for (s in specs) {
            val y = (BASE - s[0]).toInt()
            val len = ((s[1] - 83f) * reach).roundToInt()
            for (k in 0 until len) {
                val x = 83 + k
                val c = if (k % 2 == 0) steel else steel.scale(0.7f)
                val i = y * COLS + x
                f.r[i] = c.r * (1f + 1.5f * f.light[i]) + 0.3f * f.light[i]
                f.g[i] = c.g * (1f + f.light[i])
                f.b[i] = c.b
                f.cov[i] = 2f
                if (k % 3 == 1) {
                    val j = i + COLS
                    f.r[j] = c.r * 0.6f
                    f.g[j] = c.g * 0.6f
                    f.b[j] = c.b * 0.6f
                    f.cov[j] = 1.7f
                }
            }
        }
    }

    // ---- the rocket ------------------------------------------------------------------------------------------

    private fun coreHalf(h: Float): Float =
        when {
            h < 0f || h > ROCKET_HEIGHT -> {
                0f
            }

            h < 3f -> {
                3f + (3f - h) * 0.45f
            }

            h < 4.2f -> {
                4.8f + (h - 3f) * 1f
            }

            h < 23f -> {
                6f
            }

            else -> {
                val u = (h - 23f) / (ROCKET_HEIGHT - 23f)
                6f * (1f - u * u).pow(0.55f)
            }
        }

    private fun boosterHalf(h: Float): Float =
        when {
            h < 0f || h > 22f -> {
                0f
            }

            h < 3f -> {
                1.5f + (3f - h) * 0.2f
            }

            h < 16f -> {
                2.3f
            }

            else -> {
                val u = (h - 16f) / 6f
                2.3f * (1f - u * u).pow(0.6f)
            }
        }

    private fun rocket(
        f: RkFrame,
        t: Float,
        alt: Float,
    ) {
        val rv = reveal(t)
        if (rv <= 0f) return
        val amp =
            smooth(T_IGNITION + 0.8f, T_IGNITION + 2f, t) * (1f - smooth(T_LIFTOFF + 0.5f, T_LIFTOFF + 2.5f, t)) * 0.7f
        val cx = CX + amp * (0.6f * sin(TAU * 163f * t / LOOP) + 0.4f * sin(TAU * 241f * t / LOOP + 1f))
        val white = RkCol(0.93f, 0.94f, 0.98f)
        val steel = RkCol(0.2f, 0.21f, 0.27f)
        val checker = RkCol(0.06f, 0.07f, 0.1f)
        val seam = RkCol(0.45f, 0.48f, 0.58f)
        val window = RkCol(0.2f, 0.6f, 1f)
        val rimWarm = glowX(150f)
        val topRow = BASE - alt - ROCKET_HEIGHT
        val y0 = max(0, floor(topRow).toInt() - 1)
        val y1 = min(ROWS - 1, floor(BASE - alt).toInt())
        val x0 = max(0, (cx - 16f).toInt())
        val x1 = min(COLS - 1, (cx + 16f).toInt())
        for (y in y0..y1) {
            val h = BASE - alt - (y + 0.5f)
            if (h < 0f) continue
            for (x in x0..x1) {
                val px = x + 0.5f
                val dxc = px - cx
                var hw = coreHalf(h)
                var n = 0f
                var u = 0f
                var kind = -1
                if (abs(dxc) < hw) {
                    kind = 0
                    n = dxc / hw
                    u = dxc + hw
                } else {
                    for (s in -1..1 step 2) {
                        val db = px - (cx + s * BOOSTER_X)
                        val bw = boosterHalf(h)
                        if (abs(db) < bw) {
                            kind = 1
                            hw = bw
                            n = db / bw
                            u = db + bw
                        }
                    }
                }
                if (kind < 0) continue
                val diff = rv * (ROCKET_HEIGHT + 6f) - h
                if (diff <= BAYER[(y and 3) * 4 + (x and 3)] * 5f) continue
                val alb: RkCol =
                    if (kind == 0) {
                        when {
                            h < 3f -> {
                                steel
                            }

                            h < 4.2f -> {
                                seam.scale(0.55f)
                            }

                            h >= 23f -> {
                                trimCol
                            }

                            h >= 21.5f -> {
                                trimCol.scale(0.85f)
                            }

                            h in 11.5f..15.5f -> {
                                val a = floor(u / 3f).toInt() + floor((h - 11.5f) / 2f).toInt()
                                if (a % 2 == 0) checker else white
                            }

                            abs(h - 18.3f) < 0.5f || abs(h - 10.4f) < 0.5f -> {
                                seam
                            }

                            ((dxc * dxc) / 5.8f + ((h - 7.8f) * ASPECT) * ((h - 7.8f) * ASPECT) / 5.8f) < 1f -> {
                                window
                            }

                            else -> {
                                white
                            }
                        }
                    } else {
                        when {
                            h < 3f -> steel
                            h >= 16f -> trimCol
                            abs(h - 9f) < 0.5f -> seam
                            else -> white
                        }
                    }
                val nz = sqrt(max(0f, 1f - n * n))
                val lam = 0.2f + 0.78f * max(0f, -0.55f * n + 0.6f * nz)
                val fill = max(0f, n) * rimWarm
                val i = y * COLS + x
                val l = f.light[i]
                val emissive = if (alb === window) 0.65f else 0f
                f.r[i] = alb.r * (lam * 0.92f + 0.5f * fill) + alb.r * l * 0.45f + 0.12f * l + emissive * 0.3f
                f.g[i] = alb.g * (lam * 0.96f + 0.3f * fill) + alb.g * l * 0.3f + 0.07f * l + emissive * 0.8f
                f.b[i] = alb.b * (lam * 1.04f + 0.2f * fill) + alb.b * l * 0.2f + 0.03f * l + emissive
                f.cov[i] = 2f
                if (diff < 2.2f && rv < 1f) {
                    f.r[i] += 0.5f
                    f.g[i] += 0.7f
                    f.b[i] += 0.9f
                }
            }
        }
    }

    // ---- the flame -------------------------------------------------------------------------------------------

    private fun heatColor(
        heat: Float,
        out: FloatArray,
    ) {
        val c: RkCol =
            when {
                heat < 0.15f -> {
                    RkCol(0.45f, 0.06f, 0.03f).mix(RkCol(0.75f, 0.14f, 0.04f), heat / 0.15f)
                }

                heat < 0.35f -> {
                    RkCol(0.75f, 0.14f, 0.04f).mix(
                        flameCol.mix(RkCol(1f, 0.3f, 0.06f), 0.45f),
                        (heat - 0.15f) / 0.2f,
                    )
                }

                heat < 0.6f -> {
                    flameCol.mix(RkCol(1f, 0.3f, 0.06f), 0.45f).mix(
                        flameCol.mix(RkCol(1f, 1f, 1f), 0.25f),
                        (heat - 0.35f) / 0.25f,
                    )
                }

                else -> {
                    flameCol.mix(RkCol(1f, 1f, 1f), 0.25f).mix(
                        RkCol(1f, 0.97f, 0.85f),
                        clamp01((heat - 0.6f) / 0.35f),
                    )
                }
            }
        out[0] = c.r
        out[1] = c.g
        out[2] = c.b
    }

    private fun flame(
        f: RkFrame,
        t: Float,
        env: Float,
        ny: Float,
    ) {
        if (env <= 0.01f) return
        val thin = smooth(T_LIFTOFF, T_LIFTOFF + 3f, t)
        val ground = env * (1f - smooth(T_LIFTOFF + 0.8f, T_LIFTOFF + 3.5f, t))
        val x0 = max(0, (CX - 34f).toInt())
        val x1 = min(COLS - 1, (CX + 34f).toInt())
        val yStart = max(0, floor(ny - 1f).toInt())
        for (p in 0 until 3) {
            val xc = CX + (p - 1) * BOOSTER_X
            val w0 = if (p == 1) 6.2f else 3.6f
            val l0 = if (p == 1) 26f else 18f
            val len = l0 * env * (0.8f + 0.55f * thin) * (0.93f + 0.07f * sin(TAU * 137f * t / LOOP + p))
            for (y in yStart..min(ROWS - 1, 44)) {
                val s = y + 0.5f - ny
                if (s < -0.4f || s > len) continue
                val tt = max(0f, s) / len
                val bell = 0.8f + 0.5f * sin(3.1416f * min(1f, max(0f, s) / 8f))
                val hw = w0 * bell * (1f - tt).pow(0.75f) * (1f + 0.1f * sin(TAU * 149f * t / LOOP - 1.4f * s + p))
                if (hw < 0.3f) continue
                for (x in max(x0, floor(xc - hw - 1f).toInt())..min(x1, ceil(xc + hw + 1f).toInt())) {
                    val q = 1f - abs(x + 0.5f - xc) / hw
                    if (q <= 0f) continue
                    val fk = 0.5f + 0.5f * sin(TAU * 157f * t / LOOP - 1.1f * s + 2.1f * p + 0.3f * x)
                    val heat = min(1f, 1.15f * q.pow(0.4f) * (1f - tt).pow(0.6f) * (0.88f + 0.12f * fk))
                    val i = y * COLS + x
                    if (heat > f.heat[i]) f.heat[i] = heat
                }
            }
        }
        if (ground > 0.01f) {
            for (y in 38..43) {
                val dy = (y + 0.5f - 41.2f) * ASPECT / 6.5f
                for (x in x0..x1) {
                    val dx = (x + 0.5f - CX) / 20f
                    val q = 1f - dx * dx - dy * dy
                    if (q <= 0f) continue
                    val fk = 0.5f + 0.5f * sin(TAU * 127f * t / LOOP + 0.7f * x + 2f * y)
                    val heat = q * ground * (0.7f + 0.3f * fk) * 0.95f
                    val i = y * COLS + x
                    if (heat > f.heat[i]) f.heat[i] = heat
                }
            }
        }
        for (y in yStart until ROWS) {
            for (x in x0..x1) {
                val i = y * COLS + x
                val h = f.heat[i]
                if (h < 0.04f) continue
                heatColor(h, heatOut)
                val w = smooth(0.04f, 0.3f, h)
                f.r[i] += (heatOut[0] - f.r[i]) * w
                f.g[i] += (heatOut[1] - f.g[i]) * w
                f.b[i] += (heatOut[2] - f.b[i]) * w
                val c = 0.8f + 2f * h
                f.cov[i] += (min(2f, c) - f.cov[i]) * w
            }
        }
    }

    // ---- smoke -----------------------------------------------------------------------------------------------

    private fun puff(
        f: RkFrame,
        cx: Float,
        cy: Float,
        rad: Float,
        dens: Float,
        ox: Float,
        oy: Float,
    ) {
        if (dens <= 0.002f) return
        val ry = rad / ASPECT * 0.85f
        val x0 = max(0, floor(cx - rad).toInt())
        val x1 = min(COLS - 1, ceil(cx + rad).toInt())
        val y0 = max(0, floor(cy - ry).toInt())
        val y1 = min(ROWS - 1, ceil(cy + ry).toInt())
        for (y in y0..y1) {
            val dy = (y + 0.5f - cy) / ry
            val dy2 = dy * dy
            if (dy2 >= 1f) continue
            for (x in x0..x1) {
                val dx = (x + 0.5f - cx) / rad
                val d2 = dx * dx + dy2
                if (d2 >= 1f) continue
                val n = 0.3f + 1.4f * lat.at(x * 0.42f + ox, y * 0.9f + oy)
                f.dens[y * COLS + x] += (1f - d2) * n * dens
            }
        }
    }

    private fun emitSmoke(
        f: RkFrame,
        t: Float,
    ) {
        // The cloud that rolls sideways over the pad.
        val life = 10f
        for (dir in -1..1 step 2) {
            for (p in 0 until 66) {
                val tb = T_IGNITION + 0.05f + p * 0.1f + (if (dir > 0) 0.05f else 0f)
                val a = t - tb
                if (a < 0f || a > life) continue
                val rad = 3f + 5.4f * a.pow(0.6f)
                val px = CX + dir * (4f + 42f * (1f - exp(-a / 2.4f))) + 1.3f * sin(a * 1.1f + p)
                val py = 40.2f - 5.5f * (1f - exp(-a / 3.5f)) - rad / ASPECT * 0.3f
                val env = smooth(0f, 0.5f, a) * (1f - a / life).pow(1.1f) * (1f - 0.45f * smooth(8f, 11.5f, tb))
                puff(f, px, py, rad, 0.95f * env, p * 7.3f + dir * 3.1f, p * 3.7f + a * 0.5f)
            }
        }
        // The column the engines leave behind as the rocket climbs.
        val trail = 7f
        for (p in 0 until 76) {
            val tb = T_LIFTOFF - 0.6f + p * 0.1f
            val a = t - tb
            if (a < 0f || a > trail) continue
            val ty = BASE - altitude(tb) + 1.5f
            val rad = 3f + 3.8f * a.pow(0.65f)
            val px = CX + 1.1f * sin(tb * 3f) + 1.4f * a
            val py = ty - 0.3f * a
            val env = smooth(0.3f, 1f, a) * (1f - a / trail).pow(1.4f)
            puff(f, px, py, rad, 0.8f * env, p * 5.1f + 40f, p * 2.3f + a * 0.4f)
        }
        // Vapor venting off the waiting rocket.
        val vent = if (t >= 20.5f) smooth(20.5f, 21.5f, t) else 1f - smooth(5.2f, 6.2f, t)
        if (vent > 0.01f) {
            for (v in 0 until 4) {
                val vx = if (v == 1) -1f else 1f
                val sx = CX + vx * (if (v == 3) 10.8f else 6.3f)
                val sy = BASE - floatArrayOf(25f, 19f, 10f, 4f)[v]
                for (k in 0 until 3) {
                    val ph = fract(t / 2.2f + k / 3f + v * 0.27f)
                    val rad = 3f + 7f * ph
                    val px = sx + vx * (2f + 9f * ph)
                    val py = sy - 3f * ph
                    puff(f, px, py, rad, 0.5f * vent * sin(3.1416f * ph), v * 9f + k * 3.3f, ph * 1.5f)
                }
            }
        }
    }

    private fun smoke(
        f: RkFrame,
        t: Float,
        ny: Float,
    ) {
        emitSmoke(f, t)
        for (i in 0 until CELLS) {
            val d = f.dens[i]
            if (d < 0.03f) continue
            val x = i % COLS
            val grain = 0.8f + 0.4f * lat.at(x * 0.8f, (i / COLS) * 1.7f + 5f)
            val a = smooth(0.12f, 0.9f, d * grain) * (1f - 0.95f * clamp01(f.heat[i] * 3f))
            if (a <= 0.003f) continue
            val dn = if (i + COLS + 1 < CELLS) f.dens[i + COLS + 1] else 0f
            val sh = (1.12f - 0.3f * min(1f, d) + 1.3f * (dn - d)).coerceIn(0.5f, 1.3f)
            val ff = 0.38f + 0.34f * gauss(x + 0.5f, CX, 55f)
            val lw = f.light[i] * sh
            val sr = 0.46f * sh * ff + lw * 0.4f + 0.04f
            val sg = 0.49f * sh * ff + lw * 0.2f + 0.045f
            val sb = 0.6f * sh * ff + lw * 0.07f + 0.06f
            val cw = min(1f, a * 2f + 0.15f)
            f.r[i] += (sr - f.r[i]) * cw
            f.g[i] += (sg - f.g[i]) * cw
            f.b[i] += (sb - f.b[i]) * cw
            f.cov[i] = f.cov[i] * (1f - a) + (0.3f + 1.9f * a) * a
        }
    }

    // ---- output ----------------------------------------------------------------------------------------------

    private fun output(
        f: RkFrame,
        grid: Grid,
    ) {
        for (y in 0 until ROWS) {
            for (x in 0 until COLS) {
                val i = y * COLS + x
                val c = f.cov[i]
                if (c > 1.5f) {
                    grid.glyphs[i] =
                        if (c > 1.95f) {
                            '█'
                        } else if (c > 1.7f) {
                            '▓'
                        } else {
                            '▒'
                        }
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
        f: RkFrame,
        grid: Grid,
        t: Float,
    ) {
        for (s in 0 until STARS) {
            val i = starY[s] * COLS + starX[s]
            if (f.cov[i] > 0.5f) continue
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

    private fun sparks(
        grid: Grid,
        t: Float,
    ) {
        for (k in 0 until 26) {
            val a = t - (T_IGNITION + 1f + k * 0.12f)
            if (a < 0f || a > 1.7f) continue
            val h1 = Noise.hash(k, 1, 5, seed)
            val h2 = Noise.hash(k, 2, 5, seed)
            val h3 = Noise.hash(k, 3, 5, seed)
            val x = (CX + (h1 - 0.5f) * 14f + (h2 - 0.5f) * 40f * a).toInt()
            val y = (40.5f - (10f + 16f * h3) * a + 13f * a * a).toInt()
            if (x !in 0 until COLS || y !in 0 until 41) continue
            val i = y * COLS + x
            grid.glyphs[i] = if (h3 > 0.5f) '*' else '·'
            grid.colors[i] = palette.pick(1f, 0.62f + 0.3f * h2, 0.2f)
        }
    }

    private fun beacons(
        grid: Grid,
        t: Float,
    ) {
        if (fract(t * 20f / LOOP) < 0.35f) {
            for (x in 75..76) {
                grid.glyphs[5 * COLS + x] = '█'
                grid.colors[5 * COLS + x] = palette.pick(1f, 0.2f, 0.2f)
            }
            grid.glyphs[4 * COLS + 75] = '•'
            grid.colors[4 * COLS + 75] = palette.pick(0.8f, 0.15f, 0.15f)
        }
    }
}

/**
 * A rocket launch at dawn: a tall rocket with two boosters waits on its pad beside a steel gantry under floodlights
 * and venting vapor, the engines ignite and a billowing cloud of exhaust rolls out along the ground, the arms swing
 * away, and the rocket climbs out of the frame on a long flame while the camera stays. The smoke thins out, a new
 * rocket appears on the pad, and the loop starts over. Every cell is computed from its position and the time, shaded
 * in its own colour and drawn as a four level dithered halftone. The seed shapes the ridges, the stars and the
 * smoke. Every motion completes a whole number of cycles per loop.
 */
public fun rocketLaunchPiece(
    seed: Int = 0,
    flame: Rgb = AKit.Amber,
    trim: Rgb = AKit.Red,
): AsciiPiece {
    val scene = RkScene(seed, flame, trim)
    return asciiPiece(
        name = "rocket launch",
        ground = Rgb.fromHex("#080a14"),
        category = "scenes",
        cols = COLS,
        rows = ROWS,
        fps = FPS,
        still = 5.8f,
        loop = LOOP,
        note = "a rocket on a gantry pad lights its engines in a rolling cloud and climbs out of frame at dawn",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A rocket launch at dawn: the engines light in a rolling cloud of exhaust, the rocket climbs out of the frame, and
 * a new one stands on the pad.
 *
 * @param seed shapes the ridges, the stars and the smoke.
 * @param flame the color of the engine flame.
 * @param trim the color of the nose and the bands on the rocket.
 */
@Composable
public fun AsciiRocketLaunch(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    flame: Rgb = AKit.Amber,
    trim: Rgb = AKit.Red,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A rocket lifting off its launch pad at dawn in a cloud of smoke and flame",
) {
    val piece = remember(seed, flame, trim) { rocketLaunchPiece(seed, flame, trim) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
