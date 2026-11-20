package com.jantursky.kascii.gallery.g

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
import com.jantursky.kascii.gallery.AKit
import com.jantursky.kascii.gallery.loopTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 16f
private const val STARS = 170
private const val EARTH_X = 142f
private const val EARTH_Y = 22f
private const val EARTH_R = 27.5f
private const val POND_X = 118f
private const val POND_Y = 41f
private const val POND_RX = 38f
private const val POND_RY = 4.6f
private const val REST_X = 101f
private const val BASE_X = 60f
private const val BASE_Y = 28.7f
private const val ROD_LEN = 52f
private const val HEAD_X = 48f
private const val HEAD_Y = 23f

/** The color of the fish that gets away, a bright coral pink. */
private val Coral: Rgb = Rgb.fromHex("#ff5fa2")

private class Angler(
    seed: Int,
    fishColor: Rgb,
) {
    private val lat = Lattice(seed, 11)
    private val cloudLat = Lattice(seed, 29)
    private val fish = Col(fishColor.red / 255f, fishColor.green / 255f, fishColor.blue / 255f)
    private val sky = GFrame()
    private val ground = GFrame()
    private val man = GFrame()
    private val frame = GFrame()
    private val palette: GPalette
    private val starX = IntArray(STARS)
    private val starY = IntArray(STARS)
    private val starPhase = FloatArray(STARS)
    private val starRate = IntArray(STARS)
    private val eShade = FloatArray(GCELLS)
    private val eLand = FloatArray(GCELLS)
    private val eHue = FloatArray(GCELLS)
    private val earthCells: IntArray
    private val pondCells: IntArray
    private val pondDepth = FloatArray(GCELLS)
    private val water = ramp(6, col("#030816"), col("#0c2656"), col("#1f5ab0"))
    private val suit = col("#e8ecf4")
    private val suitDark = col("#6c7392")
    private val glove = col("#3d4258")
    private val visorDark = col("#0a1030")
    private val visorLight = col("#4a68c8")
    private val rodCol = col("#c8ccd8")
    private val red = col("#ff3a3a")
    private val spray = col("#9fd0ff")

    init {
        buildSky(seed)
        val cells = ArrayList<Int>()
        buildEarth(cells)
        earthCells = cells.toIntArray()
        val pond = ArrayList<Int>()
        buildGround(pond)
        pondCells = pond.toIntArray()
        buildMan()
        palette = GPalette(buildPalette())
    }

    private fun buildPalette(): List<Col> {
        val grey = ramp(10, col("#07070c"), col("#3a3a46"), col("#8a8a96"), col("#d8d8e2"))
        val earthshine = grey.map { it.mix(col("#4a78c0"), 0.28f) }
        return grey +
            earthshine +
            ramp(8, col("#04122e"), col("#0d3d8a"), col("#2e7fd6"), col("#8cc4ff")) +
            ramp(5, col("#0c3a1c"), col("#2e7a40"), col("#8ac070")) +
            ramp(4, col("#2a2418"), col("#6a5a34"), col("#b09a60")) +
            ramp(4, col("#6a7e98"), col("#e8f3ff"), col("#ffffff")) +
            water +
            ramp(6, col("#2a2e44"), suitDark, suit) +
            listOf(col("#ff8a20"), col("#a24a10"), visorDark, visorLight, col("#e0b050"), glove, rodCol, red) +
            ramp(4, fish.scale(0.45f), fish, fish.mix(col("#ffffff"), 0.6f)) +
            listOf(col("#2fb85a"), col("#ffffff"), col("#3a5ad8"), col("#c8c8d8"), spray, col("#a8b8d8")) +
            ramp(3, col("#050912"), col("#16224a"), col("#3a64c8")) +
            ramp(3, col("#4a2f99"), col("#7f52ff"), col("#a98aff"))
    }

    private fun groundTop(x: Int): Int = (31.5f + 0.9f * sin(x * 0.06f) + 0.7f * sin(x * 0.19f + 1f)).toInt()

    /** Squared distance from the cell centre to the ellipse centre in units of its radii, with [ry] in rows. */
    private fun ell(
        x: Int,
        y: Int,
        cx: Float,
        cy: Float,
        rx: Float,
        ry: Float,
    ): Float {
        val a = (x + 0.5f - cx) / rx
        val b = (y + 0.5f - cy) / ry
        return a * a + b * b
    }

    private fun buildSky(seed: Int) {
        // Earthlight halo.
        for (y in 0 until GROWS) {
            for (x in 0 until GCOLS) {
                val dx = x + 0.5f - EARTH_X
                val dy = (y + 0.5f - EARTH_Y) * ROW_U
                val d = sqrt(dx * dx + dy * dy)
                if (d > EARTH_R && d < EARTH_R + 22f) {
                    val h = gauss(d, EARTH_R, 9f)
                    sky.put(x, y, Col(0.1f, 0.3f, 0.75f), 0.9f * h, 0.toChar())
                }
            }
        }
        val rng = AKit.rng(seed, 51)
        for (s in 0 until STARS) {
            var x: Int
            var y: Int
            do {
                x = rng.int(GCOLS)
                y = rng.int(30)
            } while (ell(x, y, EARTH_X, EARTH_Y, EARTH_R + 4f, (EARTH_R + 4f) / ROW_U) < 1f)
            starX[s] = x
            starY[s] = y
            starPhase[s] = rng.float()
            starRate[s] = 1 + rng.int(4)
        }
    }

    private fun buildEarth(cells: ArrayList<Int>) {
        for (y in 0 until GROWS) {
            for (x in 0 until GCOLS) {
                val dx = (x + 0.5f - EARTH_X) / EARTH_R
                val dy = (y + 0.5f - EARTH_Y) * ROW_U / EARTH_R
                val d2 = dx * dx + dy * dy
                if (d2 >= 1f) continue
                val i = y * GCOLS + x
                val nz = sqrt(1f - d2)
                val shade = dx * 0.62f - dy * 0.42f + nz * 0.66f
                eShade[i] = smooth(-0.1f, 0.5f, shade)
                val warp = 1f / (nz + 0.55f)
                val n = lat.fbm(dx * warp * 2.4f + 3f, dy * warp * 2.4f + 7f, 3)
                eLand[i] = smooth(0.5f, 0.57f, n)
                eHue[i] = lat.at(dx * 5f + 9f, dy * 5f)
                cells.add(i)
            }
        }
    }

    private fun buildGround(pond: ArrayList<Int>) {
        val craters =
            listOf(
                floatArrayOf(160f, 35f, 14f, 2.4f),
                floatArrayOf(84f, 33.5f, 10f, 1.6f),
                floatArrayOf(176f, 43f, 22f, 3.2f),
                floatArrayOf(12f, 40f, 16f, 3.4f),
                floatArrayOf(66f, 45f, 12f, 1.8f),
            )
        for (y in 0 until GROWS) {
            for (x in 0 until GCOLS) {
                if (y < groundTop(x)) continue
                val n = lat.fbm(x * 0.07f, y * 0.3f, 2)
                var k = 0.27f + 0.2f * (n - 0.5f) + 0.2f * x / GCOLS - 0.22f * smooth(40f, 48f, y.toFloat())
                k += 0.12f * smooth(groundTop(x).toFloat() + 3f, groundTop(x).toFloat(), y.toFloat())
                for (c in craters) {
                    val e = ell(x, y, c[0], c[1], c[2], c[3])
                    if (e < 1f) k *= if (x + 0.5f > c[0]) 0.72f else 0.88f
                    if (e in 1f..1.35f) k += 0.14f * (if (x + 0.5f > c[0]) 1f else 0.4f)
                }
                val pe = ell(x, y, POND_X, POND_Y, POND_RX, POND_RY)
                val i = y * GCOLS + x
                if (pe <= 1f) {
                    pondDepth[i] = 1f - pe
                    pond.add(i)
                    ground.put(x, y, water[2], 2f)
                    continue
                }
                if (pe < 1.3f) k += 0.16f
                val tint = clamp01(0.35f - 0.8f * x / GCOLS + 0.4f)
                val c = Col(k * 0.95f + 0.02f, k * 0.95f + 0.02f, k * (1f + 0.05f * tint) + 0.05f)
                val grain = lat.at(x * 0.9f + 20f, y * 1.4f)
                val cov = if (grain > 0.86f) 1.9f else 2f
                ground.put(x, y, c, cov)
            }
        }
    }

    private fun ellipse(
        f: GFrame,
        cx: Float,
        cy: Float,
        rx: Float,
        ry: Float,
        shade: (Float, Float) -> Col,
        cov: Float = 2f,
    ) {
        for (y in max(0, (cy - ry).toInt() - 1)..min(GROWS - 1, (cy + ry).toInt() + 1)) {
            for (x in max(0, (cx - rx).toInt() - 1)..min(GCOLS - 1, (cx + rx).toInt() + 1)) {
                val a = (x + 0.5f - cx) / rx
                val b = (y + 0.5f - cy) / ry
                if (a * a + b * b <= 1f) f.put(x, y, shade(a, b), cov)
            }
        }
    }

    private fun segment(
        f: GFrame,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        half: Float,
        shade: (Float) -> Col,
    ) {
        val ax = x1 - x0
        val ay = (y1 - y0) * ROW_U
        val len2 = ax * ax + ay * ay
        for (y in max(0, min(y0, y1).toInt() - 3)..min(GROWS - 1, max(y0, y1).toInt() + 3)) {
            for (x in max(0, min(x0, x1).toInt() - 5)..min(GCOLS - 1, max(x0, x1).toInt() + 5)) {
                val px = x + 0.5f - x0
                val py = (y + 0.5f - y0) * ROW_U
                val s = clamp01((px * ax + py * ay) / len2)
                val ex = px - ax * s
                val ey = py - ay * s
                if (ex * ex + ey * ey <= half * half) f.put(x, y, shade(ey), 2f)
            }
        }
    }

    private fun lit(
        base: Col,
        dark: Col,
        k: Float,
    ): Col = dark.mix(base, clamp01(k))

    private fun buildMan() {
        // Rock seat.
        ellipse(man, 44f, 37.5f, 21f, 4.6f, { a, b ->
            val k = 0.42f + 0.25f * a - 0.18f * b + (lat.at(a * 6f + 40f, b * 6f) - 0.5f) * 0.3f
            Col(k, k, k * 1.04f + 0.03f)
        })
        // Flag: pole and a stiff violet flag with a white sun.
        for (y in 21..34) man.put(20, y, col("#c8c8d8"), 2f, '│')
        for (j in 0 until 6) for (k in 0 until 13) man.put(21 + k, 21 + j, col("#7f52ff"), 2f)
        man.put(26, 23, col("#ffffff"), 2f, '●')
        man.put(27, 23, col("#ffffff"), 2f, '●')
        man.put(26, 24, col("#ffffff"), 2f, '●')
        man.put(27, 24, col("#ffffff"), 2f, '●')
        // Backpack.
        for (y in 27..34) for (x in 36..41) man.put(x, y, suitDark.mix(suit, 0.15f * (y - 27) / 7f), 2f)
        // Legs: thigh and a dangling shin, then boots.
        segment(man, 47f, 34f, 58f, 35f, 2.9f) { e -> lit(suit, suitDark, 0.75f - e * 0.08f) }
        segment(man, 58f, 35f, 60f, 40f, 2.9f) { e -> lit(suit, suitDark, 0.7f - e * 0.05f) }
        for (y in 39..41) for (x in 58..64) man.put(x, y, glove, 2f)
        for (x in 58..64) man.put(x, 39, glove.mix(suitDark, 0.5f), 2f)
        // Torso.
        ellipse(man, 46.5f, 30.8f, 6.8f, 4.9f, { a, b -> lit(suit, suitDark, 0.62f + 0.38f * a - 0.18f * b) })
        // Chest control box.
        for (x in 46..50) man.put(x, 29, col("#ff8a20"), 2f)
        man.put(47, 29, col("#2fb85a"), 2f, '●')
        man.put(49, 29, col("#ffffff"), 2f, '●')
        for (x in 46..50) man.put(x, 30, col("#a24a10"), 2f)
        // Arm to the rod.
        segment(man, 50f, 27.8f, 55f, 31f, 2.4f) { e -> lit(suit, suitDark, 0.8f - e * 0.1f) }
        segment(man, 55f, 31f, 59f, 29.2f, 2.4f) { e -> lit(suit, suitDark, 0.85f - e * 0.1f) }
        ellipse(man, 60f, 28.7f, 2.6f, 1.2f, { _, _ -> glove })
    }

    private fun head(
        f: GFrame,
        dy: Int,
    ) {
        ellipse(f, HEAD_X, HEAD_Y + dy, 6.6f, 3.0f, { a, b -> lit(suit, suitDark, 0.65f + 0.4f * a - 0.3f * b) })
        ellipse(f, HEAD_X + 2.6f, HEAD_Y + dy + 0.1f, 4.1f, 1.9f, { a, b ->
            visorDark.mix(visorLight, clamp01(0.25f + 0.4f * a - 0.55f * b))
        })
        f.put((HEAD_X + 4.8f).toInt(), (HEAD_Y + dy - 0.8f).toInt(), col("#e0b050"), 2f)
        f.put((HEAD_X + 3.6f).toInt(), (HEAD_Y + dy - 1f).toInt(), col("#8cc4ff"), 2f)
        // Antenna nub on the helmet.
        f.put((HEAD_X - 3f).toInt(), (HEAD_Y + dy - 3.4f).toInt(), suitDark, 2f)
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = GTAU * t / LOOP
        frame.copyFrom(sky)
        stars(t)
        shootingStar(t)
        earth(phi)
        frame.over(ground)
        pond(t, phi)
        frame.over(man)
        // The helmet sags a little after the fish gets away.
        val droop = smooth(9.6f, 10.6f, t) - smooth(12.6f, 13.6f, t)
        head(frame, if (droop > 0.5f) 1 else 0)
        angling(t)
        frame.output(grid, palette)
    }

    private fun stars(t: Float) {
        for (s in 0 until STARS) {
            val tw = 0.5f + 0.5f * sin(GTAU * (starRate[s] * t / LOOP + starPhase[s]))
            if (tw < 0.25f) continue
            val i = starY[s] * GCOLS + starX[s]
            if (frame.cov[i] > 0.5f) continue
            val bright = tw > 0.8f
            val c = if (bright) Col(1f, 1f, 1f) else Col(0.66f, 0.72f, 0.85f)
            frame.put(
                starX[s],
                starY[s],
                c,
                1f,
                if (bright) {
                    '*'
                } else if (tw > 0.5f) {
                    '+'
                } else {
                    '.'
                },
            )
        }
    }

    private fun shootingStar(t: Float) {
        if (t < 2f || t > 2.9f) return
        val s = (t - 2f) / 0.9f
        for (k in 0 until 7) {
            val u = s - k * 0.035f
            if (u < 0f) continue
            val x = (22f + 62f * u).toInt()
            val y = (3f + 9f * u).toInt()
            val f = 1f - k / 7f
            frame.put(x, y, Col(0.7f + 0.3f * f, 0.75f + 0.25f * f, 0.9f + 0.1f * f), 1f, if (k == 0) '*' else '.')
        }
    }

    private fun earth(phi: Float) {
        val ox = 0.5f * cos(phi)
        val oy = 0.5f * sin(phi)
        for (i in earthCells) {
            val x = i % GCOLS
            val y = i / GCOLS
            if (y >= groundTop(x)) continue
            val lit = eShade[i]
            val land = eLand[i]
            val desert = smooth(0.35f, 0.7f, eHue[i])
            val dx = (x + 0.5f - EARTH_X) / EARTH_R
            val dy = (y + 0.5f - EARTH_Y) * ROW_U / EARTH_R
            val cloud = smooth(0.52f, 0.72f, cloudLat.fbm(dx * 2.8f + 40f + ox, dy * 2.8f + oy, 2))
            val l = 0.12f + 0.88f * lit
            var r = (0.02f + 0.13f * l)
            var g = (0.07f + 0.43f * l)
            var b = (0.2f + 0.68f * l)
            val lr = (0.1f + 0.25f * desert + 0.1f * l) * l
            val lg = (0.35f - 0.1f * desert) * l + 0.04f
            val lb = (0.15f + 0.1f * desert) * l
            r += (lr - r) * land
            g += (lg - g) * land
            b += (lb - b) * land
            val w = cloud * 0.85f * (0.3f + 0.7f * l)
            r += (0.93f * l - r) * w
            g += (0.96f * l - g) * w
            b += (1f * l - b) * w
            val rim = smooth(0.8f, 1f, dx * dx + dy * dy) * lit
            frame.r[i] = r + 0.1f * rim
            frame.g[i] = g + 0.18f * rim
            frame.b[i] = b + 0.2f * rim
            frame.cov[i] = 2f
            frame.ch[i] = 0.toChar()
        }
    }

    private fun ring(
        x: Int,
        y: Int,
        cx: Float,
        cy: Float,
        radius: Float,
    ): Float {
        val dx = x + 0.5f - cx
        val dy = (y + 0.5f - cy) * ROW_U * 2.6f
        val d = sqrt(dx * dx + dy * dy)
        return gauss(d, radius, 1.3f)
    }

    private fun pond(
        t: Float,
        phi: Float,
    ) {
        val wobble = 1.8f
        val splash = t - 9.9f
        val rings = FloatArray(4)
        rings[0] = fract(t / 8f) * 24f
        rings[1] = fract(t / 8f + 0.5f) * 24f
        rings[2] = if (splash in 0f..4f) splash * 9f else -1f
        rings[3] = if (splash in 0.5f..4f) (splash - 0.5f) * 9f else -1f
        val bob = bobDepth(t)
        for (i in pondCells) {
            val x = i % GCOLS
            val y = i / GCOLS
            val depth = pondDepth[i]
            val k = 0.25f + 0.75f * smooth(0f, 0.55f, depth)
            var r = water[1].r * (1f - k) + water[2].r * k * 0.8f
            var g = water[1].g * (1f - k) + water[2].g * k * 0.8f
            var b = water[1].b * (1f - k) + water[2].b * k * 0.8f
            r *= 0.8f + 0.2f * (1f - depth)
            g *= 0.8f + 0.2f * (1f - depth)
            b *= 0.8f + 0.2f * (1f - depth)
            val off = wobble * sin(GTAU * 2f * t / LOOP + y * 0.9f)
            val half = 8f - (y - 37) * 0.8f
            val e = abs(x + 0.5f - EARTH_X + off)
            if (e < half) {
                val band = 0.5f + 0.5f * sin(y * 2.4f + GTAU * t / LOOP * 3f)
                val w = (1f - e / half) * (0.35f + 0.5f * band)
                r += (0.55f - r) * w
                g += (0.78f - g) * w
                b += (1f - b) * w
            }
            var glyph = 0.toChar()
            var rr = 0f
            for (q in 0 until 4) {
                if (rings[q] < 0f) continue
                val cx = if (q < 2) REST_X else 114f
                val cy = if (q < 2) POND_Y + bob * 0.2f else POND_Y
                val fade = if (q < 2) 1f else 1f - smooth(1f, 4f, splash - (if (q == 3) 0.5f else 0f))
                rr = max(rr, ring(x, y, cx, cy, rings[q]) * fade)
            }
            if (rr > 0.45f) {
                glyph = '~'
                r += (0.6f - r) * rr
                g += (0.85f - g) * rr
                b += (1f - b) * rr
            }
            frame.r[i] = r
            frame.g[i] = g
            frame.b[i] = b
            frame.cov[i] = 2f
            frame.ch[i] = glyph
        }
    }

    /** How far the bobber dips below its rest, in rows, during the nibbling at the start of the bite. */
    private fun bobDepth(t: Float): Float = if (t in 5f..6.3f) 1.6f * max(0f, sin(GTAU * 1.5f * (t - 5f))) else 0f

    private class Pt(
        var x: Float,
        var y: Float,
    )

    private val tip = Pt(0f, 0f)
    private val end = Pt(0f, 0f)
    private val fishAt = Pt(0f, 0f)

    private fun angling(t: Float) {
        // Rod angle: rests, snaps up when the fish bites hard and relaxes after the escape.
        val up = smooth(6.4f, 7.4f, t) * (1f - smooth(9f, 10.6f, t))
        val angle = (40f + 18f * up) * PI.toFloat() / 180f
        tip.x = BASE_X + cos(angle) * ROD_LEN
        tip.y = BASE_Y - sin(angle) * ROD_LEN / ROW_U
        if (t in 5f..6.3f) tip.y += 0.5f * sin(GTAU * 3f * (t - 5f))
        // Line end (bobber): from the water, up to the dangling height and swinging, then back down.
        val haul = smooth(6.4f, 7.5f, t) * (1f - smooth(8.8f, 10.4f, t))
        val swing = if (t in 7.5f..9.4f) 6f * sin(GTAU * 1.2f * (t - 7.5f)) * (1f - smooth(8.2f, 9.4f, t)) else 0f
        val dropX = tip.x - 1f
        end.x = REST_X + (dropX - REST_X) * haul + swing * haul
        end.y = POND_Y + bobDepth(t) + (19f - POND_Y) * haul
        // The rod.
        val dx = tip.x - BASE_X
        val dy = tip.y - BASE_Y
        val n = (abs(dx) * 2f).toInt() + 1
        for (k in 0..n) {
            val p = k.toFloat() / n
            val x = BASE_X + dx * p
            val y = BASE_Y + dy * p
            frame.put(x.toInt(), y.toInt(), rodCol, 2f, if (y - y.toInt() < 0.5f) '▀' else '▄')
        }
        // The line.
        val lx = tip.x
        val ly = tip.y
        val ldx = end.x - lx
        val ldy = end.y - ly
        val steps = abs(ldy).toInt() + 2
        for (k in 1 until steps) {
            val p = k.toFloat() / steps
            val x = lx + ldx * p
            val y = ly + ldy * p
            val glyph =
                if (abs(ldx) < 1.2f) {
                    '│'
                } else if (ldx > 0f) {
                    '╲'
                } else {
                    '╱'
                }
            frame.put(x.toInt(), y.toInt(), Col(0.82f, 0.86f, 0.95f), 1f, glyph)
        }
        // The bobber.
        for (k in 0..1) {
            frame.put(end.x.toInt() + k, end.y.toInt(), red, 2f)
            frame.put(end.x.toInt() + k, end.y.toInt() - 1, Col(1f, 1f, 1f), 2f, '▀')
        }
        // The fish: hanging off the line, then leaping back into the pond.
        if (t in 6.4f..9.9f) {
            if (t < 8.8f) {
                val tilt = -0.2f + 0.3f * sin(GTAU * 1.8f * (t - 6.4f))
                fishAt.x = end.x + 0.5f
                fishAt.y = end.y + 4.5f
                fishAt.y = min(fishAt.y, 44f)
                paintFish(fishAt.x, fishAt.y, tilt)
            } else {
                val s = (t - 8.8f) / 1.1f
                val x0 = end.x + 0.5f
                val y0 = 19f + 4.5f
                val x = x0 + (116f - x0) * s
                val y = y0 + (POND_Y - y0) * s * s - 8f * 4f * s * (1f - s)
                paintFish(x, y, -0.2f + 1.5f * s)
            }
        }
        splash(t)
    }

    private fun paintFish(
        cx: Float,
        cy: Float,
        a: Float,
    ) {
        val ca = cos(a)
        val sa = sin(a)
        for (y in (cy - 10f).toInt()..(cy + 10f).toInt()) {
            for (x in (cx - 13f).toInt()..(cx + 13f).toInt()) {
                val dx = x + 0.5f - cx
                val dv = (y + 0.5f - cy) * ROW_U
                val lx = dx * ca + dv * sa
                val ly = -dx * sa + dv * ca
                val body = (lx / 6.4f) * (lx / 6.4f) + (ly / 4.2f) * (ly / 4.2f) <= 1f
                val tail = lx < -4.8f && lx > -11.5f && abs(ly) <= (-lx - 4f) * 0.6f
                if (!body && !tail) continue
                var k = 0.75f - ly * 0.07f
                if (tail) k = 0.85f
                if (body && ly > 1.6f) k = 1.15f
                var c = fish.scale(clamp01(k)).mix(Col(1f, 1f, 1f), if (body && ly > 1.6f) 0.5f else 0f)
                var glyph = 0.toChar()
                if (body && abs(lx - 3.4f) < 1.9f && abs(ly + 1.2f) < 1.7f) {
                    c = Col(0.05f, 0.03f, 0.08f)
                    glyph = '●'
                }
                frame.put(x, y, c, 2f, glyph)
            }
        }
    }

    private fun splash(t: Float) {
        val s = (t - 9.9f) / 0.9f
        if (s < 0f || s > 1f) return
        for (k in 0 until 7) {
            val px = 116f + (k - 3) * 3.2f * s
            val py = POND_Y - 9f * 4f * s * (1f - s) * (0.55f + 0.2f * (k % 3))
            frame.put(px.toInt(), py.toInt(), spray, 1f, if (k % 2 == 0) '•' else '·')
        }
    }
}

/**
 * An astronaut sits on a rock beside a lunar pond with a fishing rod, Earth rising behind. The bobber nibbles, a coral
 * fish is hauled up, wriggles on the line and falls back in with a splash, and the astronaut hangs his head. A flag
 * stands stiffly in the dust and a shooting star crosses the sky. The seed shapes the stars and the continents. Every
 * motion repeats a whole number of times per loop.
 */
public fun moonAnglerPiece(
    seed: Int = 0,
    fish: Rgb = Coral,
): AsciiPiece {
    val scene = Angler(seed, fish)
    return asciiPiece(
        name = "moon angler",
        ground = Rgb.fromHex("#02030a"),
        category = "scenes",
        cols = GCOLS,
        rows = GROWS,
        fps = 15,
        still = 7.4f,
        loop = LOOP,
        note = "an astronaut fishing in a lunar pond under a rising Earth, and the big one gets away",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * An astronaut fishing in a pond on the Moon with Earth rising behind: the bobber nibbles, a fish is hauled up and
 * escapes back into the water with a splash.
 *
 * @param seed shapes the stars and the continents.
 * @param fish the color of the fish.
 */
@Composable
public fun AsciiMoonAngler(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    fish: Rgb = Coral,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "An astronaut fishing in a pond on the Moon under a rising Earth, a fish escaping",
) {
    val piece = remember(seed, fish) { moonAnglerPiece(seed, fish) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
