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
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val COLS = 192
private const val ROWS = 48
private const val CELL = 2.2f // a cell is this much taller than wide; all shapes live in square units
private const val LOOP = 40f
private const val FPS = 12
private const val SUN_X = 70f
private const val SUN_V = 49f
private const val SUN_R = 21f
private const val SKY_END = 62f // where the sky gradient reaches the horizon, in units
private const val LAYERS = 4
private const val TRACK = 2 // the dune layer the caravan walks along
private const val FRONT = 3
private const val GAP = 42f // between two camels
private const val MARGIN = 24f
private const val LEAD_X = 62f
private const val STEP_LENGTH = 17.6f // how far a camel walks in one leg cycle
private const val SCALE = 1.12f
private const val CLOUDS = 3
private const val PAD = 12
private const val STARS = 70
private const val GRAINS = 110
private const val SKY_GLYPHS = " ·•●"
private const val LAND_GLYPHS = " ░▒▓█"
private const val PI_F = 3.1415927f

private const val SKY_SHADES = 18
private const val SUN_SHADES = 10
private const val DUNE_SHADES = 5
private const val CLOUD_SHADES = 8

private val BAYER = floatArrayOf(0f, 8f, 2f, 10f, 12f, 4f, 14f, 6f, 3f, 11f, 1f, 9f, 15f, 7f, 13f, 5f)
private val CLOUD_ROWS = floatArrayOf(4.5f, 9f, 13.5f)
private val CLOUD_THICK = floatArrayOf(3.4f, 4.4f, 4.8f)
private val CLOUD_SWAY = floatArrayOf(5f, 7f, 4f)
private const val CLOUD_BLOCKS = "░▒▓"

// Per dune layer: the wavelength, the height, the base line of the crests, how deep the colour ramp runs.
private val DUNE_PERIOD = floatArrayOf(100f, 84f, 112f, 100f)
private val DUNE_AMP = floatArrayOf(15f, 20f, 9f, 11f)
private val DUNE_BASE = floatArrayOf(64f, 74f, 80f, 106f)
private val DUNE_DEPTH = floatArrayOf(26f, 20f, 20f, 12f)

/**
 * A camel caravan crossing the desert at dusk, shaded cell by cell on a wide frame. A huge low sun sits behind the
 * far dunes, four layers of dunes with sharp lit crests roll below it, and a line of camels with riders and loads
 * walks along the third layer as bold dark silhouettes, their legs stepping and their long shadows stretched across
 * the sand. The far dunes and the sun waver in the heat, thin clouds drift, stars glimmer and the wind carries sand.
 * The caravan walks one lap of a ring per loop, so it leaves on the left and comes back in on the right without a
 * jump. The piece paints its own dark ground, so it reads on light and dark pages alike. Every motion repeats a
 * whole number of times per loop.
 */
public fun desertCaravanPiece(
    seed: Int = 0,
    camels: Int = 4,
    sun: Rgb = AKit.Amber,
): AsciiPiece {
    require(camels in 1..6) { "camels must be in 1..6" }
    val scene = CaravanScene(seed, camels, sun)
    return asciiPiece(
        name = "desert caravan",
        ground = Rgb.fromHex("#120a14"),
        category = "scenes",
        cols = COLS,
        rows = ROWS,
        fps = FPS,
        still = 3f,
        loop = LOOP,
        note = "a camel caravan crossing rolling dunes under a huge low sun",
    ) { time, grid ->
        scene.draw(SceneKit.loopTime(time, FPS, LOOP), grid)
    }
}

/** The static layers and the palette of one scene, built once, and the per cell shading of a frame. */
private class CaravanScene(
    private val seed: Int,
    private val camels: Int,
    sun: Rgb,
) {
    private val paletteArgb: IntArray
    private val lut = ByteArray(1 shl 15)

    // The caravan walks one lap of this ring per loop; its length is a whole number of leg cycles.
    private val strides = maxOf(1, ((maxOf(COLS + 64f, camels * GAP + 40f)) / STEP_LENGTH).roundToInt())
    private val span = strides * STEP_LENGTH

    private val sunC = floats(sun)
    private val horizonC = mix3(sunC, hex(0xe0452b), 0.6f)
    private val core = hex(0xfff0b4)
    private val violet = hex(0x2a1356)
    private val plum = hex(0x7a2a62)
    private val cloudDark = hex(0x3a1a52)
    private val cloudLit = mix3(horizonC, sunC, 0.6f)
    private val starC = hex(0xd9c8f0)
    private val silC = hex(0x150a18)
    private val shadowC = hex(0x1a0a22)
    private val sandC = hex(0xffe2b4)
    private val sandDim = hex(0x4a2252)

    private val litCrest = arrayOf(hex(0xffb070), hex(0xf08060), hex(0xff9462), hex(0xd8684c))
    private val litDeep = arrayOf(hex(0xc8603e), hex(0x8a3058), hex(0x5a2248), hex(0x2a1030))
    private val rim = Array(LAYERS) { mix3(litCrest[it], core, 0.5f) }
    private val shadeTop = arrayOf(hex(0x7a3a70), hex(0x4a2260), hex(0x3a1a50), hex(0x1c0e2c))
    private val shadeDeep = arrayOf(hex(0x4a2060), hex(0x301448), hex(0x241038), hex(0x140a1c))

    private val skyR = FloatArray(ROWS)
    private val skyG = FloatArray(ROWS)
    private val skyB = FloatArray(ROWS)
    private val skyBase = FloatArray(ROWS)
    private val clouds = Array(CLOUDS) { FloatArray(COLS + 2 * PAD) }
    private val stars = ByteArray(COLS * ROWS)

    // The crest of each dune layer in units, per column, and whether the face there is turned to the sun.
    private val ridge = Array(LAYERS) { FloatArray(COLS) }
    private val lit = Array(LAYERS) { BooleanArray(COLS) }
    private val grains = Array(GRAINS) { FloatArray(6) }

    // Per frame state and the result of the last shaded cell.
    private var r = 0f
    private var g = 0f
    private var b = 0f
    private var glyph = ' '
    private var landMode = false
    private var density = 0f
    private var ang = 0f
    private var step = 0f
    private var sunDx = 0f
    private val sway = FloatArray(CLOUDS)
    private val camelX = FloatArray(6)
    private val camelBase = FloatArray(6)
    private val camelSlope = FloatArray(6)

    init {
        val colors = ArrayList<FloatArray>()
        for (i in 0 until SKY_SHADES) colors += skyRamp(i / (SKY_SHADES - 1f))
        for (i in 0 until SUN_SHADES) colors += sunRamp(i / (SUN_SHADES - 1f))
        for (l in 0 until LAYERS) {
            for (i in 0 until DUNE_SHADES) {
                colors += mix3(litCrest[l], litDeep[l], i / (DUNE_SHADES - 1f))
                colors += mix3(shadeTop[l], shadeDeep[l], i / (DUNE_SHADES - 1f))
            }
        }
        for (i in 0 until CLOUD_SHADES) colors += mix3(cloudDark, cloudLit, i / (CLOUD_SHADES - 1f))
        for (c in rim) colors += c
        colors += silC
        colors += shadowC
        colors += starC
        colors += sandC
        colors += sandDim
        colors += core
        paletteArgb = IntArray(colors.size) { pack(colors[it]) }
        buildLut(colors)

        for (row in 0 until ROWS) {
            val t = ((row + 0.5f) * CELL / SKY_END).coerceIn(0f, 1f)
            val c = skyRamp(t.pow(1.1f))
            skyR[row] = c[0]
            skyG[row] = c[1]
            skyB[row] = c[2]
            skyBase[row] = 0.1f + 0.55f * t.pow(1.7f)
        }
        for (l in 0 until LAYERS) {
            for (x in 0 until COLS) {
                ridge[l][x] = duneV(l, x + 0.5f)
                lit[l][x] = duneV(l, x + 1.5f) < duneV(l, x - 0.5f)
            }
        }
        for (i in 0 until CLOUDS) {
            val bandSeed = seed * 17 + i * 5
            for (x in 0 until COLS + 2 * PAD) {
                val n = Noise.fbm(x * 0.02f, i * 3.7f, 0f, 3, bandSeed)
                clouds[i][x] = ((n - 0.42f) * 3f).coerceIn(0f, 1f) * CLOUD_THICK[i]
            }
        }
        val rng = AKit.rng(seed, 51)
        for (i in 0 until STARS) {
            val x = rng.int(COLS)
            val y = rng.int(13)
            stars[y * COLS + x] = (1 + rng.int(3)).toByte()
        }
        for (gr in grains) {
            gr[0] = rng.float() * COLS
            gr[1] = if (rng.float() < 0.7f) FRONT.toFloat() else TRACK.toFloat() // the crest the grain follows
            gr[2] = 0.6f + rng.float() * 8f // height above the crest
            gr[3] = (3 + rng.int(7)).toFloat() // whole laps of the frame per loop
            gr[4] = rng.float() * AKit.TAU
            gr[5] = (1 + rng.int(3)).toFloat()
        }
    }

    private fun floats(c: Rgb) = floatArrayOf(c.red / 255f, c.green / 255f, c.blue / 255f)

    private fun hex(v: Int) = floatArrayOf(((v shr 16) and 255) / 255f, ((v shr 8) and 255) / 255f, (v and 255) / 255f)

    private fun mix3(
        a: FloatArray,
        b: FloatArray,
        t: Float,
    ) = floatArrayOf(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t)

    private fun pack(c: FloatArray): Int =
        (255 shl 24) or
            ((c[0] * 255f + 0.5f).toInt().coerceIn(0, 255) shl 16) or
            ((c[1] * 255f + 0.5f).toInt().coerceIn(0, 255) shl 8) or
            (c[2] * 255f + 0.5f).toInt().coerceIn(0, 255)

    /** Top of the sky to the horizon: night violet, plum, the horizon colour, then the sun colour. */
    private fun skyRamp(t: Float): FloatArray =
        when {
            t < 0.4f -> mix3(hex(0x0b0618), violet, t / 0.4f)
            t < 0.68f -> mix3(violet, plum, (t - 0.4f) / 0.28f)
            t < 0.88f -> mix3(plum, horizonC, (t - 0.68f) / 0.2f)
            else -> mix3(horizonC, sunC, (t - 0.88f) / 0.12f)
        }

    private fun sunRamp(t: Float): FloatArray =
        if (t < 0.5f) mix3(core, sunC, t * 2f) else mix3(sunC, horizonC, (t - 0.5f) * 2f)

    private fun buildLut(colors: List<FloatArray>) {
        for (key in lut.indices) {
            val cr = ((key shr 10) + 0.5f) / 32f
            val cg = (((key shr 5) and 31) + 0.5f) / 32f
            val cb = ((key and 31) + 0.5f) / 32f
            var best = 0
            var bestD = Float.MAX_VALUE
            for (i in colors.indices) {
                val dr = colors[i][0] - cr
                val dg = colors[i][1] - cg
                val db = colors[i][2] - cb
                val d = 0.3f * dr * dr + 0.5f * dg * dg + 0.2f * db * db
                if (d < bestD) {
                    bestD = d
                    best = i
                }
            }
            lut[key] = best.toByte()
        }
    }

    /**
     * The crest line of dune [layer] at [x], in units: a long gentle face rising to the right, towards the lit side,
     * then a sharp crest and a short steep drop.
     */
    private fun duneV(
        layer: Int,
        x: Float,
    ): Float {
        val u = x / DUNE_PERIOD[layer] + 0.08f * sin(x * 0.021f + layer * 2f + seed) + Noise.hash(layer, 5, 77, seed)
        val cell = floor(u)
        val f = u - cell
        val a = 0.55f + 0.45f * Noise.hash(cell.toInt(), layer, 61, seed)
        val crest = 0.74f
        val h =
            if (f < crest) {
                val k = f / crest
                k * k * (3f - 2f * k)
            } else {
                val k = (f - crest) / (1f - crest)
                (1f - k) * (1f - k)
            }
        val swell = 0.35f * sin(x * 0.012f + layer * 1.7f)
        return DUNE_BASE[layer] - DUNE_AMP[layer] * (a * h + swell * 0.4f)
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        ang = AKit.TAU * t / LOOP
        step = AKit.TAU * strides * t / LOOP
        for (i in 0 until CLOUDS) sway[i] = CLOUD_SWAY[i] * sin(ang + i * 1.3f)
        for (i in 0 until camels) {
            val p = SceneKit.wrap(LEAD_X + i * GAP - span * t / LOOP + MARGIN, span) - MARGIN
            camelX[i] = p
            camelBase[i] = duneV(TRACK, p)
            camelSlope[i] = (duneV(TRACK, p + 7f) - duneV(TRACK, p - 7f)) / 14f
        }
        for (y in 0 until ROWS) {
            val pv = (y + 0.5f) * CELL
            val heat = ((pv - 28f) / 30f).coerceIn(0f, 1f)
            val wob = sin(AKit.TAU * 12f * t / LOOP + pv * 0.55f) * (0.4f + 1.4f * heat) * heat
            sunDx = wob * 1.4f
            val farShift = wob.roundToInt()
            val midShift = (wob * 0.35f).roundToInt()
            for (x in 0 until COLS) {
                val px = x + 0.5f
                glyph = ' '
                landMode = true
                val xf = (x + farShift).coerceIn(0, COLS - 1)
                val xm = (x + midShift).coerceIn(0, COLS - 1)
                when {
                    pv >= ridge[FRONT][x] -> {
                        shadeDune(FRONT, x, px, pv, pv - ridge[FRONT][x])
                    }

                    pv >= ridge[TRACK][x] -> {
                        shadeDune(TRACK, x, px, pv, pv - ridge[TRACK][x])
                        shadow(px, pv)
                    }

                    pv >= ridge[1][xm] -> {
                        shadeDune(1, xm, px, pv, pv - ridge[1][xm])
                    }

                    pv >= ridge[0][xf] -> {
                        shadeDune(0, xf, px, pv, pv - ridge[0][xf])
                    }

                    else -> {
                        landMode = false
                        shadeSky(x, y, px, pv)
                    }
                }
                if (pv < ridge[FRONT][x] && pv > 20f) caravan(px, pv)
                put(grid, y * COLS + x, x, y)
            }
        }
        for (gr in grains) sand(grid, gr, t)
    }

    private fun put(
        grid: Grid,
        i: Int,
        x: Int,
        y: Int,
    ) {
        if (glyph == ' ') {
            val n = if (landMode) 4 else 3
            val q = density * n + BAYER[(y and 3) * 4 + (x and 3)] / 16f - 0.47f
            val k = (q + 0.5f).toInt().coerceIn(0, n)
            glyph = if (landMode) LAND_GLYPHS[k] else SKY_GLYPHS[k]
        }
        grid.glyphs[i] = glyph
        grid.colors[i] = paletteArgb[lut[key(r, g, b)].toInt()]
    }

    private fun key(
        cr: Float,
        cg: Float,
        cb: Float,
    ): Int =
        ((cr * 31.99f).toInt().coerceIn(0, 31) shl 10) or
            ((cg * 31.99f).toInt().coerceIn(0, 31) shl 5) or
            (cb * 31.99f).toInt().coerceIn(0, 31)

    private fun smooth(
        a: Float,
        c: Float,
        v: Float,
    ): Float {
        val k = ((v - a) / (c - a)).coerceIn(0f, 1f)
        return k * k * (3f - 2f * k)
    }

    private fun set(
        c: FloatArray,
        d: FloatArray,
        k: Float,
    ) {
        val q = (k.coerceIn(0f, 1f) * (DUNE_SHADES - 1) + 0.5f).toInt() / (DUNE_SHADES - 1f)
        r = c[0] + (d[0] - c[0]) * q
        g = c[1] + (d[1] - c[1]) * q
        b = c[2] + (d[2] - c[2]) * q
    }

    private fun toward(
        c: FloatArray,
        a: Float,
    ) {
        r += (c[0] - r) * a
        g += (c[1] - g) * a
        b += (c[2] - b) * a
    }

    /** Shades one cell of dune [layer], [depth] units under its crest. */
    private fun shadeDune(
        layer: Int,
        xi: Int,
        px: Float,
        pv: Float,
        depth: Float,
    ) {
        val k = smooth(0f, DUNE_DEPTH[layer], depth)
        val faceLit = lit[layer][xi] || (xi > 0 && lit[layer][xi - 1] && depth < CELL)
        density = 1f
        if (faceLit) {
            set(litCrest[layer], litDeep[layer], k)
            if (depth < CELL * 0.95f) {
                r = rim[layer][0]
                g = rim[layer][1]
                b = rim[layer][2]
            }
            // Wind ripples run along the lit face as a few darker lines.
            val rip = sin((depth + 0.12f * px) * 1.15f + layer)
            if (layer >= 1 && rip > 0.82f && depth > CELL && k < 0.9f) glyph = '▓'
        } else {
            set(shadeTop[layer], shadeDeep[layer], k)
        }
        if (layer <= 1 && faceLit) {
            // The sun behind warms the far faces.
            val warm = (1f - abs(px - SUN_X) / 90f).coerceIn(0f, 1f) * (1f - k) * 0.35f
            toward(sunC, warm)
        }
        if (layer == FRONT) {
            // The foreground thins out into calm dark.
            density = 1f - 0.85f * smooth(7f, 30f, depth)
        }
    }

    /** Darkens the dune face where the long shadows of the camels fall, cast from the sun low behind them. */
    private fun shadow(
        px: Float,
        pv: Float,
    ) {
        val drop = (pv - ridge[TRACK][px.toInt().coerceIn(0, COLS - 1)]) / SCALE
        if (drop < 0f || drop > 16f) return
        for (i in 0 until camels) {
            val lx = (px - camelX[i]) / SCALE
            if (lx < -12f || lx > 62f) continue
            val a = shadowCover(lx, drop, i)
            if (a > 0.6f) {
                r = shadowC[0]
                g = shadowC[1]
                b = shadowC[2]
                glyph = '█'
                return
            } else if (a > 0.25f) {
                glyph = '▒'
                return
            }
        }
    }

    /** How much of the point [lx], [drop] (units along and down the face, local to a camel) its shadow covers. */
    private fun shadowCover(
        lx: Float,
        drop: Float,
        index: Int,
    ): Float {
        val s = sin(step + index * 0.9f)
        var d = cap(lx, drop, 20f + 2f * s, 4.8f, 24.5f, 6f, 1.1f)
        d = minOf(d, cap(lx, drop, 29f - 2f * s, 5f, 33f, 6f, 1.1f))
        d = minOf(d, cap(lx, drop, 22f, 6.8f, 36f, 6.8f, 2.5f))
        d = minOf(d, cap(lx, drop, 31f, 8.8f, 31.5f, 8.8f, 1.8f))
        d = minOf(d, cap(lx, drop, 39f, 8.8f, 39.5f, 8.8f, 1.8f))
        d = minOf(d, cap(lx, drop, 29f, 8f, 34f, 12f, 1.3f))
        d = minOf(d, cap(lx, drop, 34f, 12.2f, 41f, 12.6f, 1.1f))
        d = minOf(d, cap(lx, drop, 42f, 10.5f, 54f, 14f, 1.6f))
        return 0.5f - d / 1.6f
    }

    /** Shades one sky cell: the gradient, the huge sun, stars and clouds. */
    private fun shadeSky(
        x: Int,
        y: Int,
        px: Float,
        pv: Float,
    ) {
        r = skyR[y]
        g = skyG[y]
        b = skyB[y]
        density = skyBase[y]
        val dx = px + sunDx - SUN_X
        val dy = pv - SUN_V
        val d2 = dx * dx + dy * dy
        val s2 = SUN_R * SUN_R * 4.2f
        val halo = 1f / (1f + d2 / s2)
        val hz = (SKY_END - pv) / 28f
        val wide = 1f / (1f + (dx / 80f) * (dx / 80f))
        val band = exp(-hz * hz) * (0.35f + 0.65f * wide)
        val glow = halo * halo * 1.1f + band * 0.6f
        r += glow * sunC[0] * 0.5f
        g += glow * sunC[1] * 0.38f
        b += glow * sunC[2] * 0.25f
        density += glow * 0.8f
        val star = stars[y * COLS + x].toInt()
        if (star != 0 && density < 0.3f) {
            val tw = sin(ang * star + x * 1.7f)
            if (tw > -0.4f) {
                glyph = if (tw > 0.5f) '•' else '·'
                toward(starC, 0.75f)
            }
        }
        val cover = ((SUN_R - sqrt(d2)) / 1.4f + 0.5f).coerceIn(0f, 1f)
        if (cover > 0f) {
            val e = ((pv - (SUN_V - SUN_R)) / (2f * SUN_R)).coerceIn(0f, 1f)
            val k = (e * (SUN_SHADES - 1)).roundToInt() / (SUN_SHADES - 1f)
            val c = sunRamp(k)
            r = c[0]
            g = c[1]
            b = c[2]
            glyph =
                when {
                    cover > 0.8f -> '█'
                    cover > 0.45f -> '●'
                    else -> '•'
                }
            density = 1f
        }
        shadeClouds(px, pv, dx)
        r = r.coerceIn(0f, 1f)
        g = g.coerceIn(0f, 1f)
        b = b.coerceIn(0f, 1f)
        density = density.coerceIn(0f, 1f)
    }

    /** Thin bands of cloud, dark on top and lit from below. */
    private fun shadeClouds(
        px: Float,
        pv: Float,
        dx: Float,
    ) {
        for (i in 0 until CLOUDS) {
            val xs = px - sway[i] + PAD
            val x0 = floor(xs).toInt().coerceIn(0, COLS + 2 * PAD - 2)
            val f = xs - x0
            val th = (clouds[i][x0] * (1f - f) + clouds[i][x0 + 1] * f) * CELL
            if (th < 0.5f) continue
            val centre = CLOUD_ROWS[i] * CELL
            val top = centre - th * 0.5f
            val bottom = centre + th * 0.5f
            val lo = maxOf(top, pv - CELL * 0.5f)
            val hi = minOf(bottom, pv + CELL * 0.5f)
            val part = (hi - lo) / CELL
            if (part < 0.3f) continue
            val p = ((lo + hi) * 0.5f - top) / (bottom - top)
            val lit = 0.3f + 0.7f / (1f + (dx / 60f) * (dx / 60f))
            val kk = (p * p * lit * 1.2f + 0.05f + 0.1f * i).coerceIn(0f, 1f)
            val ci = (kk * (CLOUD_SHADES - 1)).toInt() / (CLOUD_SHADES - 1f)
            r = cloudDark[0] + (cloudLit[0] - cloudDark[0]) * ci
            g = cloudDark[1] + (cloudLit[1] - cloudDark[1]) * ci
            b = cloudDark[2] + (cloudLit[2] - cloudDark[2]) * ci
            glyph =
                when {
                    part < 0.8f -> if (top > pv - CELL * 0.5f) '▄' else '▀'
                    else -> CLOUD_BLOCKS[(0.2f + p * 2.6f).toInt().coerceIn(0, 2)]
                }
        }
    }

    private fun cap(
        px: Float,
        py: Float,
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        rad: Float,
    ): Float {
        val pax = px - ax
        val pay = py - ay
        val bax = bx - ax
        val bay = by - ay
        val hh = ((pax * bax + pay * bay) / (bax * bax + bay * bay)).coerceIn(0f, 1f)
        val dx = pax - bax * hh
        val dy = pay - bay * hh
        return sqrt(dx * dx + dy * dy) - rad
    }

    /**
     * The distance to the camel number [index], with its rider or load, at [lx] along it (growing to the back, the
     * camel faces left) and [h] above the ground, in local units. Negative inside.
     */
    private fun camelDist(
        lx: Float,
        h: Float,
        index: Int,
    ): Float {
        val bob = 0.5f * sin(2f * step + index)
        val hb = h - bob
        var d = cap(lx, hb, -6f, 16f, 7f, 16f, 4f)
        d = minOf(d, cap(lx, hb, -2.6f, 20.4f, -2.6f, 20.8f, 3f))
        d = minOf(d, cap(lx, hb, 5f, 20.4f, 5f, 20.8f, 3f))
        d = minOf(d, cap(lx, hb, -8.5f, 17f, -12f, 24f, 1.8f))
        d = minOf(d, cap(lx, hb, -12f, 24f, -13.5f, 28.5f, 1.5f))
        d = minOf(d, cap(lx, hb, -13.5f, 28.8f, -18.5f, 27.2f, 1.3f))
        d = minOf(d, cap(lx, hb, 10.5f, 17.5f, 11f, 12.5f, 0.6f))
        for (leg in 0 until 4) {
            val front = leg % 2 == 1
            val hx = if (front) -6.4f else 6.2f
            val off = (if (leg >= 2) PI_F else 0f) + (if (front) 0.5f else 0f) + index * 0.9f
            val lift = 2.4f * maxOf(0f, -cos(step + off))
            val fx = hx + 3.9f * sin(step + off)
            val kx = (hx + fx) * 0.5f + (if (front) -1.2f else 1.5f)
            val kh = 7f + lift * 0.5f
            d = minOf(d, cap(lx, h, hx, 13.5f + bob, kx, kh, 1.7f))
            d = minOf(d, cap(lx, h, kx, kh, fx, lift + 0.4f, 0.95f))
            d = minOf(d, cap(lx, h, fx - 0.3f, lift + 0.7f, fx + 0.5f, lift + 0.7f, 1.2f))
        }
        val sway = 0.5f * sin(step * 2f + index * 2f)
        when {
            index == 0 -> {
                d = minOf(d, rider(lx, hb, sway))
                d = minOf(d, cap(lx, hb, -1.8f, 22f, -2f, 42f, 0.4f))
                val flutter = 0.7f * sin(ang * 6f + 1f)
                d = minOf(d, cap(lx, hb, -2f, 41.5f, 3f, 40.5f + flutter, 0.9f))
            }

            index % 3 == 2 -> {
                d = minOf(d, cap(lx, hb, -0.6f, 23.5f, 3.4f, 23.5f, 3.2f))
                d = minOf(d, cap(lx, hb, 1.2f, 27.5f, 1.8f, 28f, 2.5f))
            }

            else -> {
                d = minOf(d, rider(lx, hb, sway))
            }
        }
        return d
    }

    private fun rider(
        lx: Float,
        h: Float,
        sway: Float,
    ): Float {
        var d = cap(lx, h, 1.2f, 21.5f, 1.5f + sway * 0.5f, 27f, 2.7f)
        d = minOf(d, cap(lx, h, 1.6f + sway * 0.5f, 30.2f, 1.6f + sway * 0.5f, 30.8f, 1.9f))
        d = minOf(d, cap(lx, h, 2.8f, 26f, 7f, 23.5f + sway, 1.2f))
        return d
    }

    /** Draws the camels over the cell at [px], [pv] when they cover it, as solid dark blocks. */
    private fun caravan(
        px: Float,
        pv: Float,
    ) {
        for (i in 0 until camels) {
            val lx = px - camelX[i]
            if (lx < -24f * SCALE || lx > 16f * SCALE) continue
            val ground = camelBase[i] + camelSlope[i] * lx
            val hTop = (ground - (pv - 0.55f)) / SCALE
            val hBottom = (ground - (pv + 0.55f)) / SCALE
            if (hTop < -3f || hBottom > 44f) continue
            val top = camelDist(lx / SCALE, hTop, i) < 0f
            val bottom = camelDist(lx / SCALE, hBottom, i) < 0f
            if (top || bottom) cover(top, bottom)
        }
    }

    /** Fills the cell with the silhouette, or, when only one half is covered, shows the other half of what is behind. */
    private fun cover(
        top: Boolean,
        bottom: Boolean,
    ) {
        if (top && bottom) {
            r = silC[0]
            g = silC[1]
            b = silC[2]
            glyph = '█'
            return
        }
        val solid = (glyph != ' ' && glyph != '·' && glyph != '•') || density > 0.55f
        if (solid) glyph = if (top) '▄' else '▀'
    }

    private fun sand(
        grid: Grid,
        gr: FloatArray,
        t: Float,
    ) {
        val x = SceneKit.wrap(gr[0] - gr[3] * COLS * t / LOOP, COLS.toFloat())
        val xi = x.toInt()
        val v = ridge[gr[1].toInt()][xi] - gr[2] + sin(ang * gr[5] + gr[4]) * 1.2f
        val yi = (v / CELL).toInt()
        if (yi !in 0 until ROWS) return
        val i = yi * COLS + xi
        if (grid.glyphs[i] == '█') return
        val c = if (gr[2] > 4f) sandDim else sandC
        val fast = gr[3] > 6f
        grid.glyphs[i] = if (fast) '─' else '·'
        grid.colors[i] = paletteArgb[lut[key(c[0], c[1], c[2])].toInt()]
        if (fast && xi + 1 < COLS) {
            grid.glyphs[i + 1] = '·'
            grid.colors[i + 1] = grid.colors[i]
        }
    }
}

/**
 * A camel caravan crossing rolling dunes under a huge low sun, with heat shimmer and blowing sand.
 *
 * @param seed shapes the dunes and the clouds and places the stars and the blowing grains of sand.
 * @param camels the number of camels in the line, 1 to 6.
 * @param sun the color of the sun and of the warm light on the far dunes.
 */
@Composable
public fun AsciiDesertCaravan(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    camels: Int = 4,
    sun: Rgb = AKit.Amber,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A line of camels crossing desert dunes under a huge low sun",
) {
    val piece = remember(seed, camels, sun) { desertCaravanPiece(seed, camels, sun) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
