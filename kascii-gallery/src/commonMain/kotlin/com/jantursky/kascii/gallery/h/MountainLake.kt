package com.jantursky.kascii.gallery.h

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.AsciiPieceView
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.asciiPiece
import com.jantursky.kascii.gallery.loopTime
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 24f
private const val SHORE = 30
private const val MOON_X = 150f
private const val MOON_Y = 7.5f
private const val MOON_R = 6.5f
private const val STARS = 120
private const val LOON_Y = 39f

private class Lake(
    seed: Int,
) {
    private val lat = HLattice(seed, 3)
    private val mist = HLattice(seed, 5)
    private val upper = HFrame()
    private val frame = HFrame()
    private val palette: HPalette
    private val skyTop = hcol("#04071a")
    private val skyMid = hcol("#101c46")
    private val skyLow = hcol("#2c3f78")
    private val rockDark = hcol("#0e1428")
    private val rockLit = hcol("#4a5a84")
    private val snowShade = hcol("#5a6fa6")
    private val snowLit = hcol("#dfe8ff")
    private val pine = hcol("#050c12")
    private val pineTip = hcol("#1c3240")
    private val moon = hcol("#fff6d8")
    private val moonGlow = hcol("#8aa4e6")
    private val haze = hcol("#7d8fbf")
    private val water = hcol("#060c22")
    private val starX = IntArray(STARS)
    private val starY = IntArray(STARS)
    private val starPhase = FloatArray(STARS)
    private val starRate = IntArray(STARS)
    private val ridge = FloatArray(HCOLS)

    init {
        buildSky()
        buildFarRange()
        buildMountains()
        buildTrees(seed)
        val rng = hrng(seed, 1)
        for (s in 0 until STARS) {
            var x: Int
            var y: Int
            do {
                x = rng.int(HCOLS)
                y = rng.int(22)
            } while (upper.cov[y * HCOLS + x] > 1.4f || (abs(x - MOON_X) < 16f && abs(y - MOON_Y) < 7f))
            starX[s] = x
            starY[s] = y
            starPhase[s] = rng.float()
            starRate[s] = 1 + rng.int(3)
        }
        val base =
            hramp(8, skyTop, skyMid, skyLow, hcol("#5468a0")) +
                hramp(7, rockDark, hcol("#222c4c"), rockLit) +
                hramp(7, snowShade, hcol("#9aacd8"), snowLit) +
                hramp(4, pine, pineTip, hcol("#3a5a6a")) +
                hramp(4, moonGlow, hcol("#d0dcff"), moon, hcol("#ffffff")) +
                hramp(3, haze, hcol("#a8b8e0"), hcol("#c8d4f0")) +
                listOf(hcol("#000000"), hcol("#f4f4f4"), hcol("#8a2020"))
        val mirrored = base.map { it.scale(0.55f).mix(water, 0.25f) }
        palette = HPalette(base + mirrored + hramp(5, water, hcol("#18285a"), hcol("#3a5290")))
    }

    private fun buildSky() {
        for (y in 0 until SHORE) {
            for (x in 0 until HCOLS) {
                val v = hclamp(y / 19f)
                val c = if (v < 0.5f) skyTop.mix(skyMid, v * 2f) else skyMid.mix(skyLow, (v - 0.5f) * 2f)
                upper.put(x, y, c.mix(hcol("#6a80c0"), 0.4f * v * v * v), 0.18f + 0.62f * v * v)
                val dx = x + 0.5f - MOON_X
                val dy = (y + 0.5f - MOON_Y) * HROW_U
                val d = sqrt(dx * dx + dy * dy)
                val i = y * HCOLS + x
                if (d < MOON_R) {
                    val k = 1f - 0.15f * lat.at(x * 0.5f + 70f, y * 1.1f)
                    upper.set(i, moon.scale(k), 2f)
                } else {
                    upper.light(i, moonGlow, 0.5f * hgauss(d, MOON_R, 7f) + 0.25f * hgauss(d, MOON_R, 28f))
                }
            }
        }
    }

    private fun peak(x: Float): Float {
        // Height above the shore in rows: three peaks with ragged ridges.
        val a = 23f - abs(x - 96f) * 0.42f
        val b = 18f - abs(x - 44f) * 0.36f
        val c = 17f - abs(x - 158f) * 0.4f
        val r = (lat.fbm(x * 0.06f, 3f, 3) - 0.5f) * 5f
        return max(max(a, b), c) + r
    }

    private fun buildFarRange() {
        for (x in 0 until HCOLS) {
            val h = 9f + 4f * lat.fbm(x * 0.03f + 20f, 9f, 3) + 2f * sin(x * 0.05f)
            val top = SHORE - h
            for (y in top.toInt() until SHORE) {
                val i = y * HCOLS + x
                val k = hsmooth(top + 3f, top, y.toFloat())
                upper.set(i, hcol("#1a2650").mix(hcol("#3c4e86"), k * 0.6f), hsolid(0.5f + 0.3f * k, x, y))
            }
        }
    }

    private fun buildMountains() {
        for (x in 0 until HCOLS) {
            val xf = x + 0.5f
            val h = peak(xf)
            ridge[x] = SHORE - h
            if (h <= 4f) continue
            // Which peak owns this column decides where the lit side ends.
            val owner =
                if (23f - abs(xf - 96f) * 0.42f >= max(18f - abs(xf - 44f) * 0.36f, 17f - abs(xf - 158f) * 0.4f)) {
                    96f
                } else if (18f - abs(xf - 44f) * 0.36f >= 17f - abs(xf - 158f) * 0.4f) {
                    44f
                } else {
                    158f
                }
            for (y in ridge[x].toInt() until SHORE) {
                val depth = y - ridge[x]
                val wob = (lat.at(xf * 0.08f, y * 0.5f + 40f) - 0.5f) * 10f + depth * 0.9f
                val lit = xf > owner + wob - 1f
                val gully = lat.fbm(xf * 0.18f + y * 0.35f, y * 0.12f, 2)
                val snowLine = 9f + 6f * lat.at(xf * 0.05f, 60f) + (gully - 0.5f) * 8f
                val snow = depth < snowLine
                val shade = 0.35f + 0.65f * hsmooth(0f, 1f, gully)
                val c =
                    if (snow) {
                        if (lit) snowShade.mix(snowLit, 0.35f + 0.5f * shade) else snowShade.scale(0.55f + 0.3f * shade)
                    } else {
                        if (lit) rockDark.mix(rockLit, 0.5f * shade) else rockDark.mix(rockLit, 0.15f * shade)
                    }
                val fade = hsmooth(8f, 22f, depth)
                upper.set(
                    y * HCOLS + x,
                    c.mix(haze.scale(0.45f), 0.35f * fade),
                    hsolid(if (snow) 0.8f else shade, x, y),
                )
            }
        }
    }

    private fun buildTrees(seed: Int) {
        val rng = hrng(seed, 2)
        var x = -2f
        while (x < HCOLS + 2) {
            val near = abs(x - 96f) > 30f
            val h = (if (near) 5f else 2.5f) + rng.float() * (if (near) 6f else 3f)
            val w = h * 0.9f
            val base = SHORE.toFloat()
            for (y in (base - h).toInt() until SHORE) {
                val half = (y + 1f - (base - h)) / h * w * 0.5f
                for (xx in (x - half).toInt()..(x + half).toInt()) {
                    if (xx < 0 || xx >= HCOLS) continue
                    val tip = y < base - h + 1.5f
                    val lit = xx > x
                    val c =
                        if (tip) {
                            pineTip
                        } else if (lit) {
                            pine.mix(pineTip, 0.35f)
                        } else {
                            pine
                        }
                    upper.put(xx, y, c, 2f)
                }
            }
            x += 1.5f + rng.float() * 3.5f
        }
        // A low dark shoreline strip.
        for (x2 in 0 until HCOLS) upper.put(x2, SHORE - 1, pine, 2f)
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = HTAU * t / LOOP
        frame.copyFrom(upper)
        stars(t)
        mistBand(t)
        lake(t, phi)
        loon(t, phi)
        frame.output(grid, palette)
    }

    private fun stars(t: Float) {
        for (s in 0 until STARS) {
            val tw = 0.5f + 0.5f * sin(HTAU * (starRate[s] * t / LOOP + starPhase[s]))
            if (tw < 0.3f) continue
            val c = hcol("#c8d4f0").mix(hcol("#ffffff"), tw)
            frame.put(
                starX[s],
                starY[s],
                c,
                1f,
                if (tw > 0.85f) {
                    '+'
                } else if (tw > 0.55f) {
                    '·'
                } else {
                    '.'
                },
            )
        }
    }

    private fun mistBand(t: Float) {
        val drift = t / LOOP * 4f
        for (y in 20 until SHORE + 3) {
            val band = hgauss(y.toFloat(), 27.5f, 3.6f)
            for (x in 0 until HCOLS) {
                val n = mist.wrapFbm(x * 0.03f - drift, y * 0.35f, 3, 4 + 2)
                val n2 = mist.wrapFbm(x * 0.03f + 20f, y * 0.35f + 9f, 3, 6)
                val d = hsmooth(0.42f, 0.75f, n * 0.75f + n2 * 0.25f) * band
                if (d > 0.02f) frame.haze(y * HCOLS + x, haze, d * 0.55f)
            }
        }
    }

    private fun lake(
        t: Float,
        phi: Float,
    ) {
        for (y in SHORE until HROWS) {
            val near = (y - SHORE + 1f) / (HROWS - SHORE)
            val src = 2 * SHORE - 1 - y
            for (x in 0 until HCOLS) {
                val wave = sin(y * 1.7f + phi * 3f + x * 0.045f) + 0.6f * sin(y * 0.9f - phi * 2f - x * 0.08f)
                val sx = (x + (wave * near * 2.2f).toInt()).coerceIn(0, HCOLS - 1)
                val si = max(0, src) * HCOLS + sx
                val i = y * HCOLS + x
                val dim = 0.5f - 0.12f * near
                var r = frame.r[si] * dim + water.r * 0.5f
                var g = frame.g[si] * dim + water.g * 0.5f
                var b = frame.b[si] * dim + water.b * 0.5f
                var cov = if (frame.cov[si] > 1.4f) 1.75f else max(0.12f, frame.cov[si] * 0.8f)
                var glyph = if (frame.cov[si] > 1.4f) 0.toChar() else frame.ch[si]
                if (glyph == '+' || glyph == '.') glyph = '·'
                // Streaks of sky caught by the ripples.
                val streak = lat.wrapAt(x * 0.06f + t / LOOP * 6f, y * 1.3f, 6)
                if (streak > 0.7f) {
                    glyph = '─'
                    cov = 1f
                    r += 0.06f
                    g += 0.09f
                    b += 0.16f
                }
                // The moon's glitter path.
                val path = abs(x + 0.5f - MOON_X - wave * 0.8f) / (2.5f + 9f * near)
                if (path < 1f) {
                    val sparkle = hfract(lat.at(x * 0.7f, y * 3.1f) * 7f + 2f * t / LOOP * 3f)
                    val k = (1f - path) * (0.4f + 0.6f * sparkle)
                    if (k > 0.35f) {
                        r += (0.9f - r) * k
                        g += (0.9f - g) * k
                        b += (0.85f - b) * k
                        glyph = if (k > 0.7f) '═' else '─'
                        cov = 1f
                    }
                }
                frame.r[i] = r
                frame.g[i] = g
                frame.b[i] = b
                frame.cov[i] = cov
                frame.ch[i] = glyph
            }
        }
    }

    private fun loon(
        t: Float,
        phi: Float,
    ) {
        val x = 70f + 26f * sin(phi)
        val dir = if (cos(phi) >= 0f) 1f else -1f
        val bob = 0.15f * sin(phi * 6f)
        // Rings spread from where the loon dipped its bill, twice a loop.
        for (q in 0 until 2) {
            val age = hfract(t / LOOP * 2f + q * 0.5f)
            val ox = 70f + 26f * sin(HTAU * (t / LOOP - age / 2f))
            val radius = 2f + age * 30f
            val fade = 1f - age
            for (y in (LOON_Y - 6).toInt()..min(HROWS - 1, (LOON_Y + 6).toInt())) {
                for (xx in (ox - radius - 2).toInt()..(ox + radius + 2).toInt()) {
                    if (xx < 0 || xx >= HCOLS) continue
                    val dx = xx + 0.5f - ox
                    val dy = (y + 0.5f - LOON_Y) * HROW_U * 3.2f
                    val d = sqrt(dx * dx + dy * dy)
                    val k = hgauss(d, radius, 1.1f) * fade
                    if (k > 0.35f) {
                        frame.put(
                            xx,
                            y,
                            hcol("#9aacd8").scale(0.5f + 0.5f * fade),
                            1f,
                            if (dy < 0f) '-' else '~',
                        )
                    }
                }
            }
        }
        // Wake behind the swimmer.
        for (k in 1..18) {
            val wx = x - dir * (8f + k * 1.5f)
            val spread = k * 0.16f
            val c = hcol("#8a9cc8").scale(1f - k / 22f)
            val glyph = if (k % 3 == 0) '-' else '·'
            frame.put(wx.toInt(), (LOON_Y - 0.4f - spread).toInt(), c, 1f, glyph)
            frame.put(wx.toInt(), (LOON_Y + 1.4f + spread).toInt(), c, 1f, glyph)
        }
        val black = hcol("#05060a")
        val white = hcol("#e8ecf4")
        val y0 = (LOON_Y + bob).toInt()

        fun at(
            k: Int,
            row: Int,
            c: HCol,
            glyph: Char,
        ) = frame.put((x + k * dir).toInt(), y0 + row, c, 2f, glyph)
        // Head, neck and a dagger bill, raised above a long low hull.
        for (k in 4..6) at(k, -2, black, '▄')
        at(4, -1, black, '█')
        at(5, -1, hcol("#9a2424"), '█')
        at(6, -1, black, '█')
        at(7, -1, hcol("#3a4050"), '▀')
        at(8, -1, hcol("#3a4050"), '▀')
        for (k in -7..3) at(k, -1, if (k in -5..1 && k % 2 == 0) white else black, '▄')
        at(-8, -1, black, '▄')
        for (k in -8..5) at(k, 0, black, '▀')
        // A white necklace and a speckled back.
        at(4, 0, white, '▀')
        // Its dark reflection.
        for (k in -7..5) frame.put((x + k * dir).toInt(), y0 + 1, hcol("#020306"), 1f, '▀')
        frame.glow(x, LOON_Y, 10f, hcol("#3a4a7a"), 0.04f)
    }
}

/**
 * A moonlit mountain lake: snowy peaks above a dark pine shore, a band of mist drifting along the water, and the whole
 * scene mirrored in the still lake with the moon's glitter path. A loon swims slowly back and forth, leaving a wake
 * and rings where it dips its bill. The seed shapes the ridges, the trees and the stars. Every motion repeats a whole
 * number of times per loop.
 */
public fun mountainLakePiece(seed: Int = 0): AsciiPiece {
    val scene = Lake(seed)
    return asciiPiece(
        name = "mountain lake",
        ground = Rgb.fromHex("#03050f"),
        category = "scenes",
        cols = HCOLS,
        rows = HROWS,
        fps = 15,
        still = 6f,
        loop = LOOP,
        note = "moonlit peaks mirrored in a still lake, mist on the water and a loon drifting past",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A moonlit mountain lake with a mirror reflection, drifting mist and a swimming loon.
 *
 * @param seed shapes the ridges, the trees and the stars.
 */
@Composable
public fun AsciiMountainLake(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Moonlit snowy peaks mirrored in a still mountain lake with mist and a swimming loon",
) {
    val piece = remember(seed) { mountainLakePiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
