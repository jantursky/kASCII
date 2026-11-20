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
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val COLS = 192
private const val ROWS = 48
private const val FPS = 15
private const val LOOP = 40f
private const val SWEEPS = 2 // beam sweeps per loop
private const val HORIZON = 33 // first row of open sea
private const val ROCK_END = 42 // last row of the headland, below it the swell washes the foot
private const val CELL = 2.2f // a glyph cell is this much taller than wide
private const val TX = 150f // centre column of the tower
private const val LAMP_Y = 11.5f
private const val MOON_X = 38f
private const val MOON_Y = 9f
private const val MOON_R = 4.3f // in rows
private const val RAMP = " ·•●▓█"
private const val NONE = '\u0000'

private val BAYER = intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)

/**
 * A lighthouse on a rocky headland at night, wide like a postcard: a red and white tower on the right, a long beam
 * sweeping across a cloudy sky over a moon and twinkling stars, and a sea that mirrors the moon and the lamp. The
 * tower, the rock and the moon are solid blocks, the sky, the beam and the water are shaded as a halftone.
 */
public fun lighthousePiece(
    seed: Int = 0,
    beam: Rgb = AKit.Amber,
    sea: Rgb = AKit.Blue,
): AsciiPiece {
    val scene = LighthouseScene(seed, beam, sea)
    return asciiPiece(
        name = "lighthouse",
        category = "scenes",
        cols = COLS,
        rows = ROWS,
        fps = FPS,
        still = 12f,
        loop = LOOP,
        ground = Rgb.fromHex("#070b18"),
        note = "a lighthouse beam sweeping over clouds and a night sea, the moon and stars above",
    ) { time, grid -> scene.draw(SceneKit.loopTime(time, FPS, LOOP), grid) }
}

private fun smooth(
    a: Float,
    b: Float,
    v: Float,
): Float {
    val k = ((v - a) / (b - a)).coerceIn(0f, 1f)
    return k * k * (3f - 2f * k)
}

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

/** All state of one lighthouse piece. The static layers are built once, a frame only reads them. */
private class LighthouseScene(
    private val seed: Int,
    beam: Rgb,
    sea: Rgb,
) {
    private val n = COLS * ROWS
    private val solid = CharArray(n)
    private val solidR = FloatArray(n)
    private val solidG = FloatArray(n)
    private val solidB = FloatArray(n)
    private val glass = BooleanArray(n)
    private val skyR = FloatArray(HORIZON * COLS)
    private val skyG = FloatArray(HORIZON * COLS)
    private val skyB = FloatArray(HORIZON * COLS)
    private val cloud = FloatArray(HORIZON * COLS)
    private val cloudLit = FloatArray(HORIZON * COLS)
    private val star = FloatArray(HORIZON * COLS)
    private val starPhase = FloatArray(HORIZON * COLS)
    private val starRate = IntArray(HORIZON * COLS)
    private val rowPhase = FloatArray(ROWS)
    private val threshold =
        FloatArray(n) {
            (0.55f * (BAYER[((it / COLS) and 3) * 4 + (it % COLS and 3)] + 0.5f) / 16f) +
                0.45f * Noise.hash(it % COLS, it / COLS, 41, seed)
        }
    private val palette: IntArray
    private val lut = IntArray(32768) { -1 }

    private val lampR = beam.red / 255f
    private val lampG = beam.green / 255f
    private val lampB = beam.blue / 255f
    private val seaR = sea.red / 255f
    private val seaG = sea.green / 255f
    private val seaB = sea.blue / 255f

    // Per-frame state.
    private var r = 0f
    private var g = 0f
    private var b = 0f
    private var fixed = NONE
    private var u = 0f
    private var shift = 0
    private var shiftFrac = 0f
    private var dirX = -1f
    private var dirY = 0f
    private var flare = 1f
    private var cloudDens = 0f

    init {
        palette = buildPalette(beam, sea)
        buildSky()
        buildLand()
        for (y in 0 until ROWS) rowPhase[y] = Noise.hash(y, 3, 9, seed) * AKit.TAU
    }

    private fun buildPalette(
        beam: Rgb,
        sea: Rgb,
    ): IntArray {
        val out = ArrayList<Rgb>()

        fun ramp(
            steps: Int,
            anchors: List<Rgb>,
        ) {
            for (s in 0 until steps) {
                val p = s / (steps - 1f) * (anchors.size - 1)
                val i = min(p.toInt(), anchors.size - 2)
                out += anchors[i].lerp(anchors[i + 1], p - i)
            }
        }
        ramp(12, listOf(Rgb.fromHex("#2a3470"), Rgb.fromHex("#5a70c0"), Rgb.fromHex("#b4cdf6")))
        ramp(6, listOf(Rgb.fromHex("#5a4a8c"), Rgb.fromHex("#b0709c"), Rgb.fromHex("#f0b090")))
        ramp(9, listOf(sea.lerp(Rgb.Black, 0.6f), sea, sea.lerp(Rgb.White, 0.7f)))
        ramp(9, listOf(Rgb.fromHex("#2c2a3a"), Rgb.fromHex("#5c5260"), Rgb.fromHex("#a89c98")))
        ramp(6, listOf(Rgb.fromHex("#0c1228"), Rgb.fromHex("#1a2448"), Rgb.fromHex("#2c3a6c")))
        ramp(9, listOf(Rgb.fromHex("#4a1218"), Rgb.fromHex("#d03a3a"), Rgb.fromHex("#ff9482")))
        ramp(9, listOf(Rgb.fromHex("#343a58"), Rgb.fromHex("#b0b4cc"), Rgb.fromHex("#fffaf0")))
        ramp(9, listOf(beam.lerp(Rgb.Black, 0.45f), beam, Rgb.fromHex("#fff4c8")))
        ramp(6, listOf(Rgb.fromHex("#8a8470"), Rgb.fromHex("#e4dcc0"), Rgb.fromHex("#fffbe8")))
        ramp(3, listOf(Rgb.fromHex("#704018"), Rgb.fromHex("#e8a040"), Rgb.fromHex("#ffe8a0")))
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
            val dr = ((c shr 16) and 0xFF) / 255f - cr
            val dg = ((c shr 8) and 0xFF) / 255f - cg
            val db = (c and 0xFF) / 255f - cb
            val d = 0.3f * dr * dr + 0.5f * dg * dg + 0.2f * db * db
            if (d < bestD) {
                bestD = d
                best = i
            }
        }
        lut[key] = palette[best]
        return palette[best]
    }

    private fun putSolid(
        i: Int,
        glyph: Char,
        cr: Float,
        cg: Float,
        cb: Float,
    ) {
        solid[i] = glyph
        solidR[i] = cr
        solidG[i] = cg
        solidB[i] = cb
    }

    private fun buildSky() {
        for (y in 0 until HORIZON) {
            val v = y / (HORIZON - 1f)
            val glow = smooth(0.4f, 1f, v)
            for (x in 0 until COLS) {
                val i = y * COLS + x
                var sr = 0.01f + 0.03f * v + 0.22f * glow * glow
                var sg = 0.016f + 0.025f * v + 0.14f * glow * glow
                var sb = 0.04f + 0.03f * v + 0.2f * glow
                val dx = x + 0.5f - MOON_X
                val dy = (y + 0.5f - MOON_Y) * CELL
                val d2 = dx * dx + dy * dy
                val halo = 0.2f * exp(-d2 / 260f)
                sr += halo * 0.7f
                sg += halo * 0.8f
                sb += halo
                skyR[i] = sr
                skyG[i] = sg
                skyB[i] = sb
                val limit = 0.983f + 0.006f * v
                if (Noise.hash(x, y, 21, seed) > limit && d2 > 400f && y < HORIZON - 5) {
                    val k = Noise.hash(x, y, 22, seed)
                    star[i] = 0.4f + 0.6f * k
                    starPhase[i] = Noise.hash(x, y, 23, seed)
                    starRate[i] = 2 + (Noise.hash(x, y, 24, seed) * 5f).toInt()
                }
            }
        }
        // Clouds: broad streaks on a texture that tiles across the width, so scrolling it once closes the loop.
        for (y in 0 until HORIZON) {
            val band = smooth(1f, 5f, y.toFloat()) * smooth(31f, 22f, y.toFloat())
            for (x in 0 until COLS) {
                var sum = 0f
                var amp = 0.6f
                var total = 0f
                var fx = x / 48f
                var fy = y / 4.5f
                var period = 4
                for (o in 0 until 3) {
                    sum += amp * tiledNoise(fx, fy, period, seed + o * 37)
                    total += amp
                    amp *= 0.45f
                    fx *= 2f
                    fy *= 2f
                    period *= 2
                }
                cloud[y * COLS + x] = smooth(0.42f, 0.6f, sum / total) * band
            }
        }
        for (y in 0 until HORIZON) {
            for (x in 0 until COLS) {
                val above = if (y > 0) cloud[(y - 1) * COLS + x] else 0f
                val below = if (y < HORIZON - 1) cloud[(y + 1) * COLS + x] else 0f
                cloudLit[y * COLS + x] = (0.45f + 1.5f * (above - below)).coerceIn(0f, 1f)
            }
        }
    }

    private fun rockTop(x: Int): Float {
        val xf = x + 0.5f
        val bump = 2.2f * (Noise.fbm(xf * 0.07f, 4f, 0f, 3, seed) - 0.5f)
        val left = smooth(100f, 142f, xf) // the cliff climbs from the sea to the plateau
        val right = smooth(168f, 191f, xf)
        return 46f - 12.5f * left + 2.5f * right + bump * (0.4f + left)
    }

    private fun towerHalfWidth(y: Int): Float =
        when (y) {
            6 -> 0.7f
            7 -> 2f
            8 -> 4.2f
            9 -> 6.2f
            in 10..12 -> 5.2f
            13 -> 9.6f
            14 -> 10.8f
            in 15..33 -> 6.4f + (y - 15) / 18f * 3.8f
            else -> -1f
        }

    private fun buildLand() {
        // The moon, a solid disc with a few darker seas.
        val moonR = MOON_R * CELL
        for (y in 0 until HORIZON) {
            for (x in 0 until COLS) {
                val dx = x + 0.5f - MOON_X
                val dy = (y + 0.5f - MOON_Y) * CELL
                val d = sqrt(dx * dx + dy * dy)
                if (d > moonR) continue
                val maria = smooth(0.5f, 0.62f, Noise.fbm(x * 0.13f + 3f, y * 0.28f, 0f, 3, seed + 11))
                val shade = 1f - 0.22f * maria - 0.1f * smooth(0.55f, 1f, d / moonR)
                putSolid(y * COLS + x, if (d > moonR - 1.1f) '▓' else '█', 0.97f * shade, 0.94f * shade, 0.8f * shade)
            }
        }
        // A far ridge on the horizon.
        for (x in 36 until 118) {
            val xf = x + 0.5f
            val hill = exp(-((xf - 84f) * (xf - 84f)) / 700f)
            val top = HORIZON - 0.6f - 4.5f * hill - 2.5f * (Noise.fbm(xf * 0.08f, 6f, 0f, 3, seed) - 0.3f) * hill
            for (y in 0 until HORIZON) {
                if (y + 0.5f < top) continue
                val rim = smooth(1.4f, 0f, y + 0.5f - top)
                putSolid(y * COLS + x, '█', 0.045f + 0.1f * rim, 0.065f + 0.1f * rim, 0.15f + 0.14f * rim)
            }
        }
        // The headland.
        for (x in 0 until COLS) {
            val top = rockTop(x)
            for (y in 0..ROCK_END) {
                if (y + 0.5f < top) continue
                val i = y * COLS + x
                val tex = Noise.fbm(x * 0.1f, y * 0.4f, 2f, 3, seed + 3)
                val slope =
                    Noise.fbm((x - 2) * 0.1f, y * 0.4f, 2f, 3, seed + 3) -
                        Noise.fbm((x + 2) * 0.1f, y * 0.4f, 2f, 3, seed + 3)
                val depth = y + 0.5f - top
                val lit = (0.5f + slope * 4f + 0.5f * (tex - 0.5f)).coerceIn(0.25f, 1f)
                val rim = smooth(2.4f, 0f, depth)
                val wet = smooth(ROCK_END - 3f, ROCK_END.toFloat(), y.toFloat())
                val k = (0.3f + 0.4f * lit + 0.55f * rim) * (1f - 0.35f * wet)
                val glyph =
                    when {
                        depth < 1.5f -> '█'
                        tex > 0.5f -> '█'
                        tex > 0.4f -> '▓'
                        else -> '▒'
                    }
                putSolid(i, glyph, 0.62f * k, 0.52f * k, 0.52f * k + 0.04f * wet)
            }
        }
        // The tower.
        for (y in 6..33) {
            val hw = towerHalfWidth(y)
            if (hw < 0f) continue
            for (x in 0 until COLS) {
                val dx = x + 0.5f - TX
                if (abs(dx) <= hw) paintTowerCell(x, y, dx, hw)
            }
        }
    }

    private fun paintTowerCell(
        x: Int,
        y: Int,
        dx: Float,
        hw: Float,
    ) {
        val i = y * COLS + x
        val nx = (dx / hw).coerceIn(-1f, 1f)
        val nz = sqrt(1f - nx * nx)
        val diffuse = (-0.55f * nx + 0.6f * nz + 0.18f).coerceIn(0.12f, 1f)
        when {
            y in 10..12 -> {
                // Lantern room: mullions at the edges and in the middle, glass between them.
                if (abs(dx) > 4f || abs(dx) < 1f) {
                    putSolid(i, '█', 0.22f, 0.22f, 0.3f)
                } else {
                    putSolid(i, '█', 1f, 0.9f, 0.55f)
                    glass[i] = true
                }
            }

            y == 13 -> {
                // Railing: posts and a rail.
                putSolid(i, if ((x and 1) == 0) '┬' else '─', 0.62f, 0.64f, 0.76f)
            }

            y == 14 -> {
                putSolid(i, '█', 0.22f * diffuse + 0.1f, 0.22f * diffuse + 0.1f, 0.3f * diffuse + 0.12f)
            }

            y <= 9 -> {
                putSolid(
                    i,
                    if (y ==
                        6
                    ) {
                        '│'
                    } else {
                        '█'
                    },
                    0.28f + 0.6f * diffuse,
                    0.08f + 0.14f * diffuse,
                    0.1f + 0.16f * diffuse,
                )
            }

            else -> {
                val red = ((y - 15) / 5) % 2 == 1 || y >= 30
                val shade = 0.2f + 0.8f * diffuse
                var cr: Float
                var cg: Float
                var cb: Float
                if (red) {
                    cr = 0.92f * shade
                    cg = 0.2f * shade
                    cb = 0.2f * shade
                } else {
                    cr = 0.95f * shade
                    cg = 0.94f * shade
                    cb = 0.98f * shade
                }
                var glyph = if (nx > 0.78f) '▓' else '█'
                if (y >= 30 && abs(dx) < 1.9f) {
                    cr = 0.07f
                    cg = 0.05f
                    cb = 0.07f
                    glyph = '█'
                } else if ((y == 20 || y == 21 || y == 26 || y == 27) && abs(dx) < 1.6f) {
                    cr = 1f
                    cg = 0.72f
                    cb = 0.28f
                    glyph = '█'
                }
                putSolid(i, glyph, cr, cg, cb)
            }
        }
    }

    // ---- per frame -------------------------------------------------------------------------------------------

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        u = t / LOOP
        val angle = -0.01f + 0.13f * sin(AKit.TAU * SWEEPS * u)
        dirX = -cos(angle)
        dirY = -sin(angle)
        flare = 0.9f + 0.1f * sin(AKit.TAU * 4f * u)
        val sh = u * COLS
        shift = floor(sh).toInt()
        shiftFrac = sh - shift
        val glyphs = grid.glyphs
        val colors = grid.colors
        for (y in 0 until ROWS) {
            for (x in 0 until COLS) {
                val i = y * COLS + x
                if (solid[i] != NONE) solidCell(x, y, i, glyphs, colors) else softCell(x, y, i, glyphs, colors)
            }
        }
    }

    private fun solidCell(
        x: Int,
        y: Int,
        i: Int,
        glyphs: CharArray,
        colors: IntArray,
    ) {
        r = solidR[i]
        g = solidG[i]
        b = solidB[i]
        if (glass[i]) {
            val f = 0.85f + 0.15f * flare
            r *= f
            g *= f
            b = b * f + 0.1f
        } else if (y < ROCK_END) {
            lampLight(x, y, 0.5f)
        }
        glyphs[i] = solid[i]
        colors[i] = quantize(min(1f, r), min(1f, g), min(1f, b))
    }

    private fun softCell(
        x: Int,
        y: Int,
        i: Int,
        glyphs: CharArray,
        colors: IntArray,
    ) {
        fixed = NONE
        if (y < HORIZON) {
            sky(x, y)
            lampLight(x, y, 1f)
            beamLight(x, y, dirX, dirY, 1f)
            beamLight(x, y, -dirX, -dirY, 0.45f)
            // Stars sit behind the beam and the clouds.
            val amp = star[i]
            if (amp != 0f) {
                val tw = 0.5f + 0.5f * sin(AKit.TAU * (starRate[i] * u + starPhase[i]))
                val s = amp * (0.35f + 0.65f * tw) * (1f - cloudDens) * (1f - min(1f, 3f * max(r, max(g, b))))
                if (s > 0.1f) {
                    fixed =
                        if (s > 0.62f && amp > 0.8f) {
                            '*'
                        } else if (s > 0.38f) {
                            '+'
                        } else {
                            '·'
                        }
                    r = 0.8f * s
                    g = 0.88f * s
                    b = s
                }
            }
        } else {
            water(x, y)
            lampLight(x, y, 0.5f)
        }
        emit(x, y, i, glyphs, colors)
    }

    /** Sky colour of a cell: gradient and moon glow, plus the drifting clouds. */
    private fun sky(
        x: Int,
        y: Int,
    ) {
        val i = y * COLS + x
        r = skyR[i]
        g = skyG[i]
        b = skyB[i]
        var a0 = x - shift
        if (a0 < 0) a0 += COLS
        val a1 = if (a0 == 0) COLS - 1 else a0 - 1
        val row = y * COLS
        val dens = cloud[row + a0] * (1f - shiftFrac) + cloud[row + a1] * shiftFrac
        cloudDens = dens
        if (dens > 0.01f) {
            val lit = cloudLit[row + a0] * (1f - shiftFrac) + cloudLit[row + a1] * shiftFrac
            val k = dens * 0.9f
            r += (0.1f + 0.26f * lit - r) * k
            g += (0.12f + 0.26f * lit - g) * k
            b += (0.26f + 0.26f * lit - b) * k
        }
    }

    private fun water(
        x: Int,
        y: Int,
    ) {
        val j = y - HORIZON
        val d = (j + 0.5f) / (ROWS - HORIZON)
        val z = 1f / (d + 0.12f)
        val h =
            0.6f * sin(z * 1.6f + x * 0.03f - AKit.TAU * 4f * u + rowPhase[y]) +
                0.4f * sin(z * 2.7f - x * 0.07f + AKit.TAU * 3f * u)
        val far = (1f - d) * (1f - d)
        val body = 0.2f + 0.18f * far
        val swell = 0.55f + 0.6f * smooth(-0.6f, 0.9f, h)
        r = seaR * body * 0.5f * swell + 0.1f * far * far
        g = seaG * body * 0.7f * swell + 0.06f * far * far
        b = seaB * body * swell + 0.1f * far * far
        // Crests are drawn as short dashes.
        val crest = smooth(0.35f, 0.8f, h) * (0.35f + 0.65f * d)
        if (crest > 0.28f) {
            fixed = '─'
            r += 0.08f * crest
            g += 0.2f * crest
            b += 0.3f * crest
        }
        glitter(x, MOON_X, d, h, 1f, 0.9f, 0.92f, 0.8f, 1.3f)
        if (y > ROCK_END) {
            glitter(x, TX, d, h, flare, lampR, lampG, lampB, 0.5f)
            val w = sin(AKit.TAU * 3f * u + x * 0.7f + rowPhase[y]) + sin(AKit.TAU * 2f * u - x * 0.31f)
            val reach = smooth(ROCK_END + 3f, ROCK_END + 1f, y.toFloat()) * smooth(112f, 128f, x.toFloat())
            val foam = smooth(0.7f, 1.7f, w) * reach
            if (foam > 0.2f) {
                r += 0.35f * foam
                g += 0.4f * foam
                b += 0.45f * foam
            }
        }
    }

    private fun glitter(
        x: Int,
        cx: Float,
        d: Float,
        h: Float,
        strength: Float,
        cr: Float,
        cg: Float,
        cb: Float,
        spread: Float,
    ) {
        val w = (2.5f + 13f * d) * spread
        val dx = (x + 0.5f - cx) / w
        if (dx > 2.5f || dx < -2.5f) return
        val k = strength * exp(-dx * dx) * smooth(-0.9f, 0.5f, h) * (1f - 0.3f * d)
        if (k < 0.12f) return
        fixed = if (k > 0.5f) '━' else '─'
        r += cr * k
        g += cg * k
        b += cb * k
    }

    private fun lampLight(
        x: Int,
        y: Int,
        scale: Float,
    ) {
        val dx = x + 0.5f - TX
        val dy = (y + 0.5f - LAMP_Y) * CELL
        val d2 = dx * dx + dy * dy
        if (d2 > 1500f) return
        val k = (0.65f * exp(-d2 / 45f) + 0.14f * exp(-d2 / 420f)) * flare * scale
        r += lampR * k
        g += lampG * k
        b += lampB * k
    }

    private fun beamLight(
        x: Int,
        y: Int,
        ax: Float,
        ay: Float,
        gain: Float,
    ) {
        val dx = x + 0.5f - TX
        val dy = (y + 0.5f - LAMP_Y) * CELL
        val along = dx * ax + dy * ay
        if (along < 4f) return
        val half = 3f + 0.1f * along
        val off = abs(dx * ay - dy * ax)
        val fade = 1f - 0.45f * along / 190f
        val wide = 1f - off / (half * 2.4f)
        if (wide <= 0f) return
        var k = 0.16f * wide * wide
        if (off < half) {
            val prof = 1f - off / half
            k += prof * sqrt(prof) * 0.78f
        }
        k *= fade * gain * flare * (1f + 0.9f * cloudDens)
        r += lampR * k
        g += lampG * k
        b += lampB * k
    }

    private fun emit(
        x: Int,
        y: Int,
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
        val inv = 1f / top
        val tone = 0.45f + 0.55f * min(1f, top * 1.5f)
        if (fixed != NONE) {
            glyphs[i] = fixed
        } else {
            val f = min(1f, top * 1.1f) * (RAMP.length - 1)
            val base = f.toInt()
            val idx = min(RAMP.length - 1, base + if (f - base > threshold[i]) 1 else 0)
            if (idx == 0) {
                glyphs[i] = ' '
                colors[i] = 0
                return
            }
            glyphs[i] = RAMP[idx]
        }
        colors[i] = quantize(min(1f, r * inv) * tone, min(1f, g * inv) * tone, min(1f, b * inv) * tone)
    }
}

/**
 * A lighthouse on a rocky coast at night, its beam sweeping over drifting clouds and a rolling sea under a moon and
 * twinkling stars. Every cell is shaded as a halftone in its own color.
 *
 * @param seed places the stars, the clouds, the swell and the shape of the rock.
 * @param beam the color of the lamp, the beam and the light it throws on the water.
 * @param sea the tint of the water.
 */
@Composable
public fun AsciiLighthouse(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    beam: Rgb = AKit.Amber,
    sea: Rgb = AKit.Blue,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A lighthouse on a rocky coast at night, its beam sweeping across the sea",
) {
    val piece = remember(seed, beam, sea) { lighthousePiece(seed, beam, sea) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
