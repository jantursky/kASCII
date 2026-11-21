package com.jantursky.kascii.gallery.j

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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 12f
private const val FLAKES = 190
private const val PINES = 5
private const val FOX_X = 104f
private const val MOON_X = 156f
private const val MOON_Y = 7.5f
private const val MOON_R = 8.5f

/** The ground scrolls one screen per loop; the forest a half period of 96 cells. */
private const val MID_PERIOD = 96f

/** When the fox leaps and lands, seconds into the loop. */
private const val LEAP_A = 6f
private const val LEAP_B = 7.4f

private class FoxSnow(
    seed: Int,
) {
    private val lat = JLattice(seed, 5)
    private val sky = JFrame()
    private val frame = JFrame()
    private val palette: JPalette
    private val pineX = FloatArray(PINES)
    private val pineH = IntArray(PINES)
    private val flakeX = FloatArray(FLAKES)
    private val flakeY = FloatArray(FLAKES)
    private val flakeRate = IntArray(FLAKES)
    private val flakeDrift = IntArray(FLAKES)
    private val flakeNear = BooleanArray(FLAKES)
    private val flakePhase = FloatArray(FLAKES)
    private val fur = jcol("#d8661e")
    private val furLit = jcol("#f49a48")
    private val furDark = jcol("#7a3010")
    private val cream = jcol("#f2ece2")
    private val sock = jcol("#2a1812")
    private val snowLit = jcol("#d4e2fa")
    private val snowShade = jcol("#5a6c9e")
    private val pine = jcol("#0a1624")
    private val pineSnow = jcol("#8ea4cc")

    init {
        buildSky()
        val rng = AKit.rng(seed, 31)
        for (k in 0 until PINES) {
            pineX[k] = (k + rng.float() * 0.7f) * MID_PERIOD / PINES
            pineH[k] = rng.jint(9, 17)
        }
        for (k in 0 until FLAKES) {
            flakeX[k] = rng.float()
            flakeY[k] = rng.float()
            flakeRate[k] = 2 + rng.int(3)
            flakeDrift[k] = 1 + rng.int(2)
            flakeNear[k] = rng.float() < 0.3f
            flakePhase[k] = rng.float()
        }
        palette = JPalette(buildPalette())
    }

    private fun buildPalette(): List<JCol> =
        jramp(9, jcol("#050a1a"), jcol("#101c3c"), jcol("#22325e"), jcol("#3c5088")) +
            jramp(9, jcol("#2a3660"), snowShade, jcol("#9aaed8"), snowLit, jcol("#ffffff")) +
            jramp(6, jcol("#0a1622"), pine, jcol("#2a4058"), pineSnow) +
            jramp(8, jcol("#3a1408"), furDark, fur, furLit, jcol("#ffc890")) +
            jramp(4, jcol("#8a8478"), jcol("#c4beb2"), cream) +
            jramp(5, jcol("#3c4a6a"), jcol("#8c9cbc"), jcol("#dfe6f4"), jcol("#fffbe8")) +
            listOf(sock, jcol("#05040a"), jcol("#ffe9b0"))

    private fun farRidge(x: Int): Float =
        28f - 9f * lat.fbm(x * 0.03f + 3f, 1.5f, 3) - 6f * jgauss(x.toFloat(), 60f, 22f) -
            3f * jgauss(x.toFloat(), 140f, 18f)

    private fun buildSky() {
        for (y in 0 until JROWS) {
            val g = y / 30f
            val c = jcol("#070d22").mix(jcol("#1e2e5a"), jclamp(g))
            for (x in 0 until JCOLS) {
                val n = lat.at(x * 0.11f, y * 0.4f) - 0.5f
                sky.put(x, y, c, jclamp(0.28f * jsmooth(0.2f, 1f, g) + n * 0.2f))
            }
        }
        val rng = AKit.rng(7, 3)
        repeat(70) {
            val x = rng.int(JCOLS)
            val y = rng.int(18)
            sky.put(x, y, jcol("#9fb0d8").scale(0.6f + 0.4f * rng.float()), 1f, if (rng.float() < 0.25f) '+' else '.')
        }
        // Moon and halo.
        for (y in 0 until 30) {
            for (x in 0 until JCOLS) {
                val dx = x + 0.5f - MOON_X
                val dy = (y + 0.5f - MOON_Y) * JROW_U
                val d = sqrt(dx * dx + dy * dy)
                val i = y * JCOLS + x
                if (d < MOON_R) {
                    val crater = lat.fbm(x * 0.4f + 50f, y * 0.9f, 2)
                    sky.put(x, y, jcol("#fffbe8").mix(jcol("#c8c4b0"), jsmooth(0.45f, 0.75f, crater) * 0.6f), 2f)
                } else {
                    val h = jgauss(d, MOON_R, 16f)
                    if (h > 0.05f) sky.light(i, jcol("#8c9cd0"), 0.55f * h)
                }
            }
        }
        // Far mountains, snow caps lit from the moon side.
        for (x in 0 until JCOLS) {
            val top = farRidge(x)
            val slope = farRidge(min(JCOLS - 1, x + 1)) - farRidge(max(0, x - 1))
            for (y in top.toInt() until 34) {
                val dn = y - top
                val cap = jsmooth(4f, 0.5f, dn) * jsmooth(19.5f, 15f, top)
                val lit = jclamp(0.5f + slope * 0.9f)
                val base =
                    jcol("#111a3a").mix(jcol("#1c2a56"), lit * 0.6f).mix(
                        jcol("#2a3a6a"),
                        jsmooth(26f, 34f, y.toFloat()) * 0.5f,
                    )
                val c = base.mix(jcol("#7a8ec4").mix(jcol("#b8c8ec"), lit), cap * 0.9f)
                val n = lat.at(x * 0.5f, y * 1.3f)
                sky.put(x, y, c, if (cap > 0.3f || n > 0.45f) 2f else 1.8f)
            }
        }
    }

    /** Height (row) of the snow surface at screen column [x] with the ground scrolled by [scroll] cells. */
    private fun surface(
        x: Float,
        scroll: Float,
    ): Float {
        val w = x + scroll
        return 37.5f + 1.1f * sin(w * JTAU / 96f) + 0.7f * sin(w * JTAU / 64f + 1f)
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        frame.copyFrom(sky)
        val scroll = JCOLS * t / LOOP
        flakes(t, near = false)
        pines(MID_PERIOD * t / LOOP)
        snowField(t, scroll)
        tracks(scroll)
        fox(t, scroll)
        puff(t, scroll)
        flakes(t, near = true)
        frame.output(grid, palette)
    }

    private fun pines(scroll: Float) {
        for (copy in -1..2) {
            for (k in 0 until PINES) {
                val x = pineX[k] - scroll + copy * MID_PERIOD
                if (x < -12f || x > JCOLS + 12f) continue
                tree(x, 34.5f, pineH[k])
            }
        }
        // The forest floor, a dim snow bank in front of the trunks.
        for (x in 0 until JCOLS) {
            val w = x + scroll
            val top = 33.2f + 0.6f * sin(w * JTAU / 48f)
            for (y in top.toInt() until 41) {
                frame.put(
                    x,
                    y,
                    jcol("#4a5c8c").mix(jcol("#6c80b0"), jclamp(top + 1f - y)),
                    1.8f,
                )
            }
        }
    }

    private fun tree(
        cx: Float,
        base: Float,
        h: Int,
    ) {
        for (j in 0 until h) {
            val y = (base - 1 - j).toInt()
            val half = (h - j) * 0.62f + 0.6f
            val tier = (j % 3).toFloat() / 2f
            val w = half * (0.75f + 0.25f * tier)
            for (x in (cx - w).toInt()..(cx + w).toInt()) {
                val dx = (x + 0.5f - cx) / max(w, 0.5f)
                val snow = tier > 0.9f || (dx > 0.2f && j % 3 == 1)
                val c =
                    if (snow) {
                        pineSnow.mix(pine, 0.3f + 0.35f * (1f - dx))
                    } else {
                        pine.mix(
                            jcol("#1c3048"),
                            jclamp(dx * 0.5f + 0.2f),
                        )
                    }
                frame.put(x, y, c, 2f)
            }
        }
        frame.put(cx.toInt(), (base - h - 1).toInt(), pine, 2f, '^')
    }

    private fun snowField(
        t: Float,
        scroll: Float,
    ) {
        for (x in 0 until JCOLS) {
            val top = surface(x + 0.5f, scroll)
            val ti = top.toInt()
            for (y in ti until JROWS) {
                val i = y * JCOLS + x
                val dn = y - top
                val w = x + scroll
                val n = lat.fbm(w * 0.08f, y * 0.5f, 2)
                val rim = jsmooth(1.6f, 0f, dn)
                val c = snowShade.mix(snowLit, jclamp(0.55f + 0.45f * rim - dn * 0.035f + (n - 0.5f) * 0.4f))
                val cv =
                    if (rim > 0.5f) {
                        2f
                    } else if (n > 0.55f) {
                        1.8f
                    } else {
                        1.6f
                    }
                frame.put(x, y, c, cv)
                // Glitter that catches the moon.
                val g = lat.at(w * 1.3f + 7f, y * 2.1f)
                if (g > 0.94f) {
                    val tw = sin(JTAU * (2f * t / LOOP + g * 13f))
                    if (tw > 0.6f) frame.put(x, y, jcol("#ffffff"), 2f, if (tw > 0.85f) '*' else '+')
                }
            }
        }
    }

    /** Paw prints along the fox's path behind it, fixed to the ground. */
    private fun tracks(scroll: Float) {
        val leapW0 = (JCOLS * LEAP_A / LOOP + FOX_X) % JCOLS
        val leapW1 = leapW0 + JCOLS * (LEAP_B - LEAP_A) / LOOP
        var k = 0
        while (k < JCOLS + 8) {
            val w = (scroll - (scroll % 6f)) + k - 6f
            k += 6
            val x = w - scroll
            if (x > FOX_X - 14f || x < 0f) continue
            // No prints where the fox was in the air.
            val ww = w - kotlin.math.floor(w / JCOLS) * JCOLS
            if (leapAirborne(ww, leapW0, leapW1)) continue
            val y = surface(x, scroll) + 3.6f
            val c = snowShade.mix(jcol("#2a3660"), 0.3f)
            val odd = ((w / 6f).toInt() and 1) == 1
            frame.put(x.toInt(), (y + if (odd) 0.6f else 0f).toInt(), c, 1f, '•')
            frame.put(x.toInt() + 2, (y + if (odd) 0.6f else 0f).toInt(), c, 1f, '•')
        }
    }

    private fun leapAirborne(
        w: Float,
        a: Float,
        b: Float,
    ): Boolean = w in (a - 8f)..(b + 6f)

    private fun leap(t: Float): Float =
        if (t in
            LEAP_A..LEAP_B
        ) {
            sin((t - LEAP_A) / (LEAP_B - LEAP_A) * 3.1415927f)
        } else {
            0f
        }

    private fun fox(
        t: Float,
        scroll: Float,
    ) {
        val air = leap(t)
        val s = if (t in LEAP_A..LEAP_B) (t - LEAP_A) / (LEAP_B - LEAP_A) else -1f
        val tilt = if (s >= 0f) -0.35f * cos(s * 3.1415927f) else 0f
        val crouch =
            jsmooth(LEAP_A - 0.5f, LEAP_A, t) * (1f - jsmooth(LEAP_A, LEAP_A + 0.15f, t)) +
                jsmooth(LEAP_B - 0.05f, LEAP_B, t) * (1f - jsmooth(LEAP_B, LEAP_B + 0.5f, t))
        val gait = JTAU * 20f * t / LOOP
        val trot = 1f - jsmooth(LEAP_A - 0.4f, LEAP_A, t) * (1f - jsmooth(LEAP_B, LEAP_B + 0.4f, t))
        val bob = -0.25f * abs(sin(gait)) * trot
        val gy = surface(FOX_X, scroll) + 3.6f + bob - 9f * air + crouch * 0.8f
        val ca = cos(tilt)
        val sa = sin(tilt)
        // Shadow on the snow, shrinking while airborne.
        val sy = surface(FOX_X, scroll) + 3.8f
        frame.ellipse(FOX_X - 2f, sy, 22f * (1f - 0.35f * air), 0.9f, 2f) { a, _ ->
            JCol(0f, 0f, 0f).mix(snowShade.scale(0.7f), 0.5f + 0.5f * abs(a))
        }
        val legSwing = FloatArray(4)
        for (k in 0 until 4) {
            val ph = gait + (if (k == 0 || k == 3) 0f else 3.1415927f)
            legSwing[k] = 0.5f * sin(ph) * trot + (if (k < 2) 0.9f else -0.8f) * air
        }
        val gyU = gy * JROW_U
        for (y in max(0, (gy - 17f).toInt())..min(JROWS - 1, (gy + 3f).toInt())) {
            for (x in max(0, (FOX_X - 40f).toInt())..min(JCOLS - 1, (FOX_X + 34f).toInt())) {
                val dx = x + 0.5f - FOX_X
                val dy = (y + 0.5f) * JROW_U - gyU
                val lx = dx * ca - dy * sa
                val ly = dx * sa + dy * ca
                val c = foxAt(lx, ly + crouch * 2.2f, gait, legSwing, trot) ?: continue
                frame.put(x, y, c.first, 2f, c.second)
            }
        }
    }

    private fun segDist(
        px: Float,
        py: Float,
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
    ): Float {
        val vx = bx - ax
        val vy = by - ay
        val s = jclamp(((px - ax) * vx + (py - ay) * vy) / (vx * vx + vy * vy))
        val ex = px - ax - vx * s
        val ey = py - ay - vy * s
        return sqrt(ex * ex + ey * ey)
    }

    private fun inTri(
        px: Float,
        py: Float,
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        cx: Float,
        cy: Float,
    ): Boolean {
        val d1 = (px - bx) * (ay - by) - (ax - bx) * (py - by)
        val d2 = (px - cx) * (by - cy) - (bx - cx) * (py - cy)
        val d3 = (px - ax) * (cy - ay) - (cx - ax) * (py - ay)
        return !((d1 < 0f || d2 < 0f || d3 < 0f) && (d1 > 0f || d2 > 0f || d3 > 0f))
    }

    private fun shadeFur(
        ly: Float,
        top: Float,
        bottom: Float,
    ): JCol {
        val v = jclamp((ly - top) / (bottom - top))
        return furLit.mix(fur, jsmooth(0f, 0.45f, v)).mix(furDark, jsmooth(0.55f, 1f, v))
    }

    /** The fox in local width units: x forward, y down, feet at 0. Returns a color and an optional glyph. */
    private fun foxAt(
        lx: Float,
        ly: Float,
        gait: Float,
        swing: FloatArray,
        trot: Float,
    ): Pair<JCol, Char>? {
        val none = 0.toChar()
        // Head first, it sits in front of everything.
        val hx = 17f
        val hy = -21f + 0.4f * sin(gait * 2f) * trot
        if (inTri(lx, ly, hx - 4f, hy - 2.5f, hx - 0.5f, hy - 3.5f, hx - 3.2f, hy - 11f) ||
            inTri(lx, ly, hx - 0.5f, hy - 3.5f, hx + 3f, hy - 2.5f, hx + 1.2f, hy - 10.5f)
        ) {
            val inner = inTri(lx, ly, hx - 0.2f, hy - 4f, hx + 2f, hy - 3.2f, hx + 1f, hy - 8f)
            return Pair(
                if (ly < hy - 8.5f) {
                    sock
                } else if (inner) {
                    furDark
                } else {
                    furLit
                },
                none,
            )
        }
        val inSnout = inTri(lx, ly, hx + 2f, hy - 2.2f, hx + 2f, hy + 3.2f, hx + 11f, hy + 1.2f)
        val e = ((lx - hx) / 5.4f).let { it * it } + ((ly - hy) / 4.6f).let { it * it }
        if (e <= 1f || inSnout) {
            if (lx > hx + 9.8f && ly < hy + 2.2f) return Pair(jcol("#05040a"), none)
            if (abs(lx - (hx + 2.2f)) < 1.1f && abs(ly - (hy - 1.3f)) < 1.1f) return Pair(jcol("#05040a"), '●')
            val lower = ly > hy + 0.6f
            if (lower && lx > hx - 3f) return Pair(cream.scale(0.95f), none)
            return Pair(shadeFur(ly, hy - 4f, hy + 4f), none)
        }
        // Neck and chest, white in front.
        val ce = ((lx - 10f) / 6f).let { it * it } + ((ly - (-15f)) / 6.5f).let { it * it }
        if (ce <= 1f) {
            if (lx > 10f + (ly + 15f) * 0.3f &&
                ly > -18f
            ) {
                return Pair(cream.mix(jcol("#c4beb2"), jsmooth(-14f, -9f, ly) * 0.5f), none)
            }
            return Pair(shadeFur(ly, -21f, -9f), none)
        }
        // Legs: two front, two back; near legs drawn darker in socks, far legs dimmer.
        val hips = floatArrayOf(9f, 6.5f, -8.5f, -11f)
        for (k in 0 until 4) {
            val far = k == 1 || k == 3
            val hipY = -10f
            val a = swing[k]
            val kneeX = hips[k] + sin(a) * 5f
            val kneeY = hipY + cos(a) * 5f
            val lift = max(0f, cos(gait + (if (k == 0 || k == 3) 0f else 3.1415927f))) * 2.2f * trot
            val back = if (k >= 2) -1.6f else 1.2f
            val footX = kneeX + sin(a * 0.6f) * 5f + back * (lift / 2.2f)
            val footY = min(0f, kneeY + 5.2f - lift)
            val d = min(segDist(lx, ly, hips[k], hipY, kneeX, kneeY), segDist(lx, ly, kneeX, kneeY, footX, footY))
            if (d < 1.25f) {
                val sockOn = ly > kneeY + 1.5f
                var c = if (sockOn) sock else shadeFur(ly, -12f, -4f)
                if (far) c = c.scale(0.6f)
                return Pair(c, none)
            }
        }
        // Body.
        val be = (lx / 12.5f).let { it * it } + ((ly - (-13f)) / 5f).let { it * it }
        if (be <= 1f) {
            if (ly > -10.2f && lx > -4f) return Pair(cream.mix(jcol("#8a8478"), 0.35f), none)
            return Pair(shadeFur(ly, -18f, -8f), none)
        }
        // Tail: a fat brush that trails behind and sways.
        val sway = 1.6f * sin(gait * 0.5f)
        for (q in 0 until 13) {
            val p = q / 12f
            val tx = -10f - p * 28f
            val ty = -14.5f + p * 7f - 3.5f * sin(p * 3.1415927f) + sway * p
            val rad = 2.6f + 3.6f * sin(p * 2.9f) - p * 0.6f
            val dd = ((lx - tx) / 1f).let { it * it } + ((ly - ty) / 1f).let { it * it }
            if (dd <= rad * rad) {
                if (p > 0.8f) return Pair(cream, none)
                val c = shadeFur(ly - ty, -rad, rad)
                return Pair(c, none)
            }
        }
        return null
    }

    private fun puff(
        t: Float,
        scroll: Float,
    ) {
        val s = (t - LEAP_B) / 1.1f
        if (s < 0f || s > 1f) return
        val gy = surface(FOX_X + 18f, scroll) + 2.5f
        val rng = AKit.rng(3, 9)
        for (k in 0 until 26) {
            val vx = (rng.float() - 0.35f) * 30f
            val vy = 4f + rng.float() * 7f
            val x = FOX_X + 20f + vx * s - 12f * s
            val y = gy - vy * s + 9f * s * s
            val fade = 1f - s
            frame.put(
                x.toInt(),
                y.toInt(),
                snowLit.scale(0.6f + 0.4f * fade),
                1f,
                if (k % 3 ==
                    0
                ) {
                    '*'
                } else if (fade > 0.5f) {
                    '•'
                } else {
                    '·'
                },
            )
        }
    }

    private fun flakes(
        t: Float,
        near: Boolean,
    ) {
        for (k in 0 until FLAKES) {
            if (flakeNear[k] != near) continue
            val y = jfract(flakeY[k] + flakeRate[k] * t / LOOP) * (JROWS + 2f) - 1f
            val x =
                jfract(flakeX[k] - flakeDrift[k] * t / LOOP) * JCOLS +
                    2.5f * sin(JTAU * (3f * t / LOOP + flakePhase[k]))
            val xi = x.toInt()
            val yi = y.toInt()
            if (xi < 0 || xi >= JCOLS || yi < 0 || yi >= JROWS) continue
            val i = yi * JCOLS + xi
            if (frame.cov[i] > 1.4f && (near || yi > 30)) {
                frame.haze(i, jcol("#f4f8ff"), if (near) 0.55f else 0.25f)
            } else if (near) {
                frame.put(xi, yi, jcol("#f4f8ff"), 1f, if (flakePhase[k] > 0.6f) '*' else '•')
            } else {
                if (frame.cov[i] > 1.4f) continue
                frame.put(xi, yi, jcol("#a8b8e0").scale(0.75f), 1f, '·')
            }
        }
    }
}

/**
 * A red fox trots through falling snow under a full moon, leaving a line of paw prints, and once per loop leaps high
 * in a mousing pounce and lands in a puff of powder. Pines and far mountains slide by at their own pace. The seed
 * places the pines, the snowfall and the moon's craters. Every motion repeats a whole number of times per loop.
 */
public fun foxSnowPiece(seed: Int = 0): AsciiPiece {
    val scene = FoxSnow(seed)
    return asciiPiece(
        name = "fox in snow",
        ground = Rgb.fromHex("#04081a"),
        category = "creatures",
        cols = JCOLS,
        rows = JROWS,
        fps = 15,
        still = 2f,
        loop = LOOP,
        note = "a red fox trots through moonlit snowfall leaving paw prints, then pounces into the powder",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A red fox trotting through moonlit snowfall, leaving paw prints, and pouncing into the powder once per loop.
 *
 * @param seed places the pines, the snowfall and the moon's craters.
 */
@Composable
public fun AsciiFoxSnow(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A red fox trotting and pouncing through moonlit snowfall",
) {
    val piece = remember(seed) { foxSnowPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
