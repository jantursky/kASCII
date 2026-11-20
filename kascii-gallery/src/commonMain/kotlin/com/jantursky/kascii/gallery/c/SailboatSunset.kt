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
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val COLS = 192
private const val ROWS = 48
private const val SEA_ROW = 30 // the first row of water
private const val CELL = 2.2f // a cell is this much taller than wide; all shapes live in square units
private const val HORIZON = SEA_ROW * CELL
private const val SEA_ROWS = ROWS - SEA_ROW
private const val LOOP = 24f
private const val FPS = 12
private const val RADIUS = 19f // of the sun, in units
private const val SUN_X = 132f
private const val SUN_V = HORIZON - 9f
private const val BOAT_X = 62f
private const val WATERLINE = (SEA_ROW + 3.2f) * CELL
private const val CLOUDS = 5
private const val PAD = 12
private const val STARS = 46
private const val GLYPHS = " ·•●"

// Palette ramps are sampled this many times; the whole palette is the sum of them plus a few silhouettes.
private const val SKY_SHADES = 18
private const val SEA_SHADES = 14
private const val CLOUD_SHADES = 10
private const val SUN_SHADES = 10
private const val SAIL_SHADES = 8

private val BAYER = floatArrayOf(0f, 8f, 2f, 10f, 12f, 4f, 14f, 6f, 3f, 11f, 1f, 9f, 15f, 7f, 13f, 5f)
private val CLOUD_ROWS = floatArrayOf(7f, 12f, 17f, 21.5f, 25.5f)
private val CLOUD_THICK = floatArrayOf(2.4f, 3.0f, 4.0f, 4.0f, 3.0f)
private val CLOUD_SWAY = floatArrayOf(2f, 3f, 5f, 4f, 6f)
private const val CLOUD_BLOCKS = "░▒▓"

/**
 * A sailboat at sunset, shaded cell by cell on a wide frame. Every cell is a pure function of its position and the
 * time: a sparse halftone sky that deepens into violet overhead, a big low sun with a halo, thin banded clouds lit
 * from below, a dark sailboat that bobs on a calm sea and a glittering path of light under the sun. Brightness
 * becomes a four level halftone, ordered dithered, solid shapes use block glyphs, and each cell takes the nearest
 * color of a fixed palette. The piece paints its own dark ground, so it reads on light and dark pages alike. Every
 * motion repeats a whole number of times per loop.
 */
public fun sailboatSunsetPiece(
    seed: Int = 0,
    sun: Rgb = AKit.Amber,
    sky: Rgb = AKit.Red,
): AsciiPiece {
    val scene = SunsetScene(seed, sun, sky)
    return asciiPiece(
        name = "sailboat sunset",
        ground = Rgb.fromHex("#140a22"),
        category = "scenes",
        cols = COLS,
        rows = ROWS,
        fps = FPS,
        still = 9f,
        loop = LOOP,
        note = "a sailboat bobbing under a big low sun, its light glittering on a calm sea",
    ) { time, grid ->
        scene.draw(SceneKit.loopTime(time, FPS, LOOP), grid)
    }
}

/** The static layers and the palette of one scene, built once, and the per cell shading of a frame. */
private class SunsetScene(
    private val seed: Int,
    sun: Rgb,
    sky: Rgb,
) {
    // Palette as ARGB, and a 5 bits per channel table from a color to the nearest palette entry.
    private val paletteArgb: IntArray
    private val lut = ByteArray(1 shl 15)

    private val sunC = floats(sun)
    private val horizonC = floats(sky)
    private val core = floatArrayOf(1f, 0.95f, 0.72f)
    private val violet = floatArrayOf(0.30f, 0.17f, 0.50f)
    private val magenta = floatArrayOf(0.62f, 0.24f, 0.55f)
    private val cloudDark = floatArrayOf(0.30f, 0.14f, 0.40f)
    private val cloudLit = mix3(horizonC, sunC, 0.6f)
    private val deepSea = floatArrayOf(0.13f, 0.16f, 0.36f)
    private val nearSea = floatArrayOf(0.62f, 0.27f, 0.40f)
    private val hullC = floatArrayOf(0.20f, 0.07f, 0.15f)
    private val hullTop = floatArrayOf(0.62f, 0.27f, 0.30f)
    private val sailC = floatArrayOf(0.50f, 0.26f, 0.42f)
    private val sailLit = floatArrayOf(1f, 0.82f, 0.58f)
    private val starC = floatArrayOf(0.85f, 0.78f, 0.95f)

    private val skyR = FloatArray(SEA_ROW)
    private val skyG = FloatArray(SEA_ROW)
    private val skyB = FloatArray(SEA_ROW)
    private val skyBase = FloatArray(SEA_ROW)
    private val bandBase = FloatArray(ROWS)
    private val clouds = Array(CLOUDS) { FloatArray(COLS + 2 * PAD) }
    private val stars = ByteArray(COLS * SEA_ROW)
    private val seaScratch = FloatArray(3)

    // Per frame state and the result of the last shaded cell.
    private var r = 0f
    private var g = 0f
    private var b = 0f
    private var glyph = ' '
    private var ang = 0f
    private var sunV = 0f
    private var boatX = 0f
    private var boatV = 0f
    private var roll = 0f
    private val sway = FloatArray(CLOUDS)

    init {
        val colors = ArrayList<FloatArray>()
        for (i in 0 until SKY_SHADES) colors += skyRamp(i / (SKY_SHADES - 1f))
        for (i in 0 until SEA_SHADES) colors += seaRamp(i / (SEA_SHADES - 1f))
        for (i in 0 until CLOUD_SHADES) colors += mix3(cloudDark, cloudLit, i / (CLOUD_SHADES - 1f))
        for (i in 0 until SUN_SHADES) colors += sunRamp(i / (SUN_SHADES - 1f))
        for (i in 0 until 4) colors += mix3(hullC, hullTop, i / 3f)
        colors += hullC
        colors += mix3(hullC, deepSea, 0.5f)
        for (i in 0 until SAIL_SHADES) colors += mix3(sailC, sailLit, i / (SAIL_SHADES - 1f))
        colors += starC
        colors += core
        colors += mix3(core, sunC, 0.5f)
        paletteArgb = IntArray(colors.size) { pack(colors[it]) }
        buildLut(colors)

        for (row in 0 until SEA_ROW) {
            val t = (row + 0.5f) / SEA_ROW
            val c = skyRamp(t.pow(1.5f))
            skyR[row] = c[0]
            skyG[row] = c[1]
            skyB[row] = c[2]
            skyBase[row] = 0.05f + 0.34f * t.pow(2.6f)
        }
        for (row in 0 until ROWS) {
            val k = ((row - SEA_ROW + 0.5f) / SEA_ROWS).coerceAtLeast(0.02f)
            bandBase[row] = 6.5f / (k + 0.16f)
        }
        for (i in 0 until CLOUDS) {
            val bandSeed = seed * 17 + i * 5
            for (x in 0 until COLS + 2 * PAD) {
                val n = Noise.fbm(x * 0.02f, i * 3.7f, 0f, 3, bandSeed)
                clouds[i][x] = ((n - 0.48f) * 4.2f).coerceIn(0f, 1f) * CLOUD_THICK[i]
            }
        }
        val rng = AKit.rng(seed, 31)
        for (i in 0 until STARS) {
            val x = rng.int(COLS)
            val y = rng.int(SEA_ROW - 12)
            stars[y * COLS + x] = (1 + rng.int(3)).toByte()
        }
    }

    private fun floats(c: Rgb) = floatArrayOf(c.red / 255f, c.green / 255f, c.blue / 255f)

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

    /** Top of the sky to the horizon: violet, magenta, the horizon colour, then the sun colour. */
    private fun skyRamp(t: Float): FloatArray =
        when {
            t < 0.4f -> mix3(violet, magenta, t / 0.4f)
            t < 0.78f -> mix3(magenta, horizonC, (t - 0.4f) / 0.38f)
            else -> mix3(horizonC, sunC, (t - 0.78f) / 0.22f)
        }

    /** Warm water at the horizon at 0, deep indigo at 1. */
    private fun seaRamp(t: Float): FloatArray =
        if (t <
            0.5f
        ) {
            mix3(nearSea, mix3(magenta, deepSea, 0.55f), t * 2f)
        } else {
            mix3(
                mix3(magenta, deepSea, 0.55f),
                deepSea,
                (t - 0.5f) * 2f,
            )
        }

    private fun sunRamp(t: Float): FloatArray =
        if (t < 0.5f) mix3(horizonC, sunC, t * 2f) else mix3(sunC, core, (t - 0.5f) * 2f)

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

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        ang = AKit.TAU * t / LOOP
        sunV = SUN_V + 3.5f * sin(ang)
        boatX = BOAT_X + 5f * sin(ang)
        boatV = WATERLINE + 1.1f * sin(2f * ang)
        roll = 0.035f * sin(3f * ang + 0.8f)
        for (i in 0 until CLOUDS) sway[i] = CLOUD_SWAY[i] * sin(ang + i * 1.3f)
        val rc = kotlin.math.cos(roll)
        val rs = sin(roll)
        for (y in 0 until ROWS) {
            val pv = (y + 0.5f) * CELL
            val sea = y >= SEA_ROW
            val band = if (sea) sin(bandBase[y] - 2f * ang) else 0f
            for (x in 0 until COLS) {
                val px = x + 0.5f
                glyph = ' '
                val density =
                    if (sea) {
                        shadeSea(x, y, px, pv, band, rc, rs)
                    } else {
                        shadeSky(x, y, px, pv)
                    }
                boat(px, pv, rc, rs)
                put(grid, y * COLS + x, x, y, density)
            }
        }
    }

    private fun put(
        grid: Grid,
        i: Int,
        x: Int,
        y: Int,
        density: Float,
    ) {
        if (glyph == ' ') {
            val q = density * 3f + BAYER[(y and 3) * 4 + (x and 3)] / 16f - 0.47f
            glyph = GLYPHS[(q + 0.5f).toInt().coerceIn(0, 3)]
        }
        grid.glyphs[i] = glyph
        val key =
            ((r * 31.99f).toInt().coerceIn(0, 31) shl 10) or
                ((g * 31.99f).toInt().coerceIn(0, 31) shl 5) or
                (b * 31.99f).toInt().coerceIn(0, 31)
        grid.colors[i] = paletteArgb[lut[key].toInt()]
    }

    private fun smooth(
        a: Float,
        c: Float,
        v: Float,
    ): Float {
        val k = ((v - a) / (c - a)).coerceIn(0f, 1f)
        return k * k * (3f - 2f * k)
    }

    private fun add(
        c: FloatArray,
        a: Float,
    ) {
        r += c[0] * a
        g += c[1] * a
        b += c[2] * a
    }

    private fun toward(
        c: FloatArray,
        a: Float,
    ) {
        r += (c[0] - r) * a
        g += (c[1] - g) * a
        b += (c[2] - b) * a
    }

    /** Shades one sky cell and returns the halftone density. */
    private fun shadeSky(
        x: Int,
        y: Int,
        px: Float,
        pv: Float,
    ): Float {
        r = skyR[y]
        g = skyG[y]
        b = skyB[y]
        var density = skyBase[y]
        val dx = px - SUN_X
        val dy = pv - sunV
        val d2 = dx * dx + dy * dy
        val s2 = RADIUS * RADIUS * 3.4f
        val halo = 1f / (1f + d2 / s2)
        val hz = (HORIZON - pv) / 14f
        val wide = 1f / (1f + (dx / 75f) * (dx / 75f))
        val band = exp(-hz * hz) * (0.3f + 0.7f * wide)
        val glow = halo * halo * 1.15f + band * 0.55f
        r += glow * sunC[0] * 0.55f
        g += glow * sunC[1] * 0.42f
        b += glow * sunC[2] * 0.28f
        density += glow * 0.85f
        val star = stars[y * COLS + x].toInt()
        if (star != 0 && density < 0.3f) {
            val tw = sin(ang * star + x * 1.7f)
            if (tw > -0.4f) {
                glyph = if (tw > 0.5f) '•' else '·'
                toward(starC, 0.75f)
            }
        }
        // The sun: a solid disc with a soft edge.
        val cover = ((RADIUS - sqrt(d2)) / 1.4f + 0.5f).coerceIn(0f, 1f)
        if (cover > 0f) {
            val e = ((pv - (sunV - RADIUS)) / (2f * RADIUS)).coerceIn(0f, 1f)
            r = core[0] + (sunC[0] * 1.05f - core[0]) * e
            g = core[1] + (sunC[1] - core[1]) * e
            b = core[2] + (sunC[2] * 0.6f - core[2]) * e
            glyph =
                when {
                    cover > 0.8f -> '█'
                    cover > 0.45f -> '●'
                    else -> '•'
                }
            density = 1f
        }
        shadeClouds(px, pv, dx, cover)
        if (pv > HORIZON - CELL * 0.5f) {
            // The horizon line itself, bright against the dark sea.
            toward(core, 0.35f)
            density = maxOf(density, 0.8f)
        }
        r = r.coerceIn(0f, 1f)
        g = g.coerceIn(0f, 1f)
        b = b.coerceIn(0f, 1f)
        return density.coerceIn(0f, 1f)
    }

    /** Thin bands of cloud, dark on top and lit from below. */
    private fun shadeClouds(
        px: Float,
        pv: Float,
        dx: Float,
        cover: Float,
    ) {
        for (i in 0 until CLOUDS) {
            val xs = px - sway[i] + PAD
            val x0 = floor(xs).toInt().coerceIn(0, COLS + 2 * PAD - 2)
            val f = xs - x0
            val clear = if (i >= 2) smooth(24f, 62f, abs(dx)) else 1f
            val th = (clouds[i][x0] * (1f - f) + clouds[i][x0 + 1] * f) * CELL * clear
            if (th < 0.5f) continue
            val centre = CLOUD_ROWS[i] * CELL
            val top = centre - th * 0.5f
            val bottom = centre + th * 0.5f
            val lo = maxOf(top, pv - CELL * 0.5f)
            val hi = minOf(bottom, pv + CELL * 0.5f)
            val part = (hi - lo) / CELL
            if (part < 0.3f) continue
            val p = ((lo + hi) * 0.5f - top) / (bottom - top)
            val lit = 0.35f + 0.65f / (1f + (dx / 70f) * (dx / 70f))
            val k = (p * p * lit + 0.1f).coerceIn(0f, 1f)
            val ci = (k * (CLOUD_SHADES - 1)).toInt()
            val c = mix3(cloudDark, cloudLit, ci / (CLOUD_SHADES - 1f))
            toward(c, if (cover > 0f) 0.88f else 1f)
            glyph =
                when {
                    part < 0.8f -> if (top > pv - CELL * 0.5f) '▄' else '▀'
                    else -> CLOUD_BLOCKS[(0.2f + p * 2.6f).toInt().coerceIn(0, 2)]
                }
        }
    }

    /** Shades one water cell: calm bands, the warm horizon, a glitter path under the sun. */
    private fun shadeSea(
        x: Int,
        y: Int,
        px: Float,
        pv: Float,
        band: Float,
        rc: Float,
        rs: Float,
    ): Float {
        val k = (y - SEA_ROW + 0.5f) / SEA_ROWS
        val c = seaScratch
        val deep = smooth(0f, 0.9f, k)
        val ramp = seaRamp(deep.pow(0.8f))
        c[0] = ramp[0]
        c[1] = ramp[1]
        c[2] = ramp[2]
        r = c[0]
        g = c[1]
        b = c[2]
        var density = 0.32f - 0.12f * k
        val dx = px - SUN_X
        val hz = (pv - HORIZON) / 16f
        val wide = 1f / (1f + (dx / 90f) * (dx / 90f))
        val glow = exp(-hz * hz) * (0.45f + 0.55f * wide)
        add(sunC, glow * 0.55f)
        density += glow * 0.55f
        // Calm bands that widen towards the viewer; their crests catch the light.
        val crestRun = sin(px * (0.07f + 0.05f * (1f - k)) + y * 1.9f + ang * 2f)
        if (band > 0.55f && crestRun > 0.2f) {
            toward(horizonC, 0.5f)
            density += 0.25f
            glyph = '─'
        } else if (band < -0.6f) {
            density -= 0.05f
        }
        // The glitter path: a column that widens towards the viewer, broken into sparks.
        val wobble = sin(pv * 0.8f - 3f * ang) * (0.6f + 2f * k)
        val w = 7f + 30f * k
        val path = 1f - abs(dx + wobble) / w
        if (path > 0f) {
            val spark = sin(px * 0.85f + y * 2.3f + 6f * ang) * sin(px * 0.31f - y * 1.4f - 3f * ang)
            val a = path * (0.35f + 0.65f * smooth(0.0f, 0.7f, spark))
            toward(sunC, path * 0.3f)
            density += path * 0.3f
            if (spark > 0.55f && a > 0.45f) {
                toward(core, 0.8f)
                glyph = if (a > 0.75f) '●' else '•'
                density = 1f
            } else if (spark > 0.15f && a > 0.35f) {
                toward(core, 0.55f)
                glyph = '─'
                density = 0.9f
            }
        }
        r = r.coerceIn(0f, 1f)
        g = g.coerceIn(0f, 1f)
        b = b.coerceIn(0f, 1f)
        return density.coerceIn(0f, 1f)
    }

    /**
     * Draws the boat over the cell at [px], [pv] when it covers it, as solid blocks, or its reflection below the
     * waterline. The boat space has its origin at the waterline, in units, with y down.
     */
    private fun boat(
        px: Float,
        pv: Float,
        rc: Float,
        rs: Float,
    ) {
        val dxw = px - boatX
        if (abs(dxw) > 24f) return
        val below = pv > boatV
        val dv = if (below) 2f * boatV - pv - boatV else pv - boatV
        if (dv < -40f) return
        if (below && dv < -16f) return
        val wob = if (below) 1.4f * sin(pv * 0.9f - 4f * ang) * (0.4f + (pv - boatV) / 12f) else 0f
        val bx = (dxw + wob) * rc + dv * rs
        val bv = -(dxw + wob) * rs + dv * rc
        var part = 0
        // Hull: a curved sliver, higher at the bow on the right.
        val top = -3.6f - 0.012f * bx * bx - 0.1f * bx
        val bottom = 3f - 0.008f * bx * bx
        if (abs(bx) < 17f && bv >= top && bv <= bottom) part = 1
        // Mast and boom.
        if (abs(bx) < 0.9f && bv < top + 1f && bv > -37f) part = 1
        if (bv > -6.5f && bv < -4.7f && bx > -14f && bx < 0f) part = 1
        // Mainsail aft of the mast, jib towards the bow.
        if (part == 0 && bx < -0.9f && bv in -35f..-6.9f) {
            val w = 14f * (bv + 35f) / 27f
            if (bx >= -0.9f - w - 1.5f * sin((bv + 35f) * 0.12f)) part = 2
        }
        if (part == 0 && bx > 3.2f && bv in -29f..-6.9f) {
            val w = 14f * (bv + 29f) / 20f
            if (bx <= 3.2f + w) part = 3
        }
        if (part == 0) return
        if (below) {
            // Reflection: the hull only, a few dim stripes fading with depth.
            val depth = (pv - boatV) / 9f
            if (part == 1 && depth < 1f && bv > top) {
                toward(hullC, 0.7f * (1f - depth))
                glyph = if (depth < 0.5f) '▒' else '░'
            }
            return
        }
        when (part) {
            1 -> {
                val kk = ((bv - top) / 3f).coerceIn(0f, 1f)
                toward(mix3(hullTop, hullC, (kk * 3f).toInt() / 3f), 1f)
                glyph = '█'
            }

            2 -> {
                val k = 0.7f * smooth(-15f, -1f, bx) * (0.5f + 0.5f * smooth(-35f, -8f, bv))
                toward(sailShade(k), 1f)
                glyph = if (k > 0.3f) '█' else '▓'
            }

            else -> {
                val k = 0.6f + 0.4f * smooth(-29f, -9f, bv)
                toward(sailShade(k), 1f)
                glyph = '█'
            }
        }
    }

    private val sailScratch = FloatArray(3)

    private fun sailShade(k: Float): FloatArray {
        val i = (k * (SAIL_SHADES - 1)).toInt().coerceIn(0, SAIL_SHADES - 1) / (SAIL_SHADES - 1f)
        for (c in 0 until 3) sailScratch[c] = sailC[c] + (sailLit[c] - sailC[c]) * i
        return sailScratch
    }
}

/**
 * A sailboat bobbing under a big low sun, the light glittering on a calm sea.
 *
 * @param seed shapes the clouds and places the stars.
 * @param sun the color of the sun and of its path on the water.
 * @param sky the color the sky turns near the horizon.
 */
@Composable
public fun AsciiSailboatSunset(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    sun: Rgb = AKit.Amber,
    sky: Rgb = AKit.Red,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A sailboat on a calm sea at sunset, the sun's light glittering on the water",
) {
    val piece = remember(seed, sun, sky) { sailboatSunsetPiece(seed, sun, sky) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
