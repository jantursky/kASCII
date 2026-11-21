package com.jantursky.kascii.gallery.l

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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 16f
private const val NU = 1100
private const val NV = 46
private const val HALF = 0.4f
private const val SCALE = 37f
private const val DIST = 7f

private class Mobius(
    seed: Int,
) {
    private val frame = LFrame()
    private val back = LFrame()
    private val depth = LDepth()
    private val palette: LPalette
    private val n = NU * NV
    private val px = FloatArray(n)
    private val py = FloatArray(n)
    private val pz = FloatArray(n)
    private val nx = FloatArray(n)
    private val ny = FloatArray(n)
    private val nz = FloatArray(n)
    private val su = FloatArray(n)
    private val edge = FloatArray(n)
    private val twist: Int
    private val tone: Float
    private val sideA = lcol("#2fd2c0")
    private val sideB = lcol("#ff6f8e")
    private val bead = lcol("#ffe08a")
    private val haze = lcol("#3a2a6a")

    init {
        val rng = lRng(seed, 53)
        // One half twist, or three for some seeds; both are one-sided.
        twist = if (rng.int(3) == 0) 3 else 1
        tone = rng.float() * LTAU
        for (iu in 0 until NU) {
            val u = LTAU * iu / NU
            for (iv in 0 until NV) {
                val v = -HALF + 2f * HALF * iv / (NV - 1)
                val k = iu * NV + iv
                val p = point(u, v)
                val pu = point(u + 0.001f, v)
                val pv = point(u, v + 0.001f)
                val ax = pu[0] - p[0]
                val ay = pu[1] - p[1]
                val az = pu[2] - p[2]
                val bx = pv[0] - p[0]
                val by = pv[1] - p[1]
                val bz = pv[2] - p[2]
                var cx = ay * bz - az * by
                var cy = az * bx - ax * bz
                var cz = ax * by - ay * bx
                val cl = 1f / sqrt(cx * cx + cy * cy + cz * cz)
                cx *= cl
                cy *= cl
                cz *= cl
                px[k] = p[0]
                py[k] = p[1]
                pz[k] = p[2]
                nx[k] = cx
                ny[k] = cy
                nz[k] = cz
                su[k] = u / LTAU
                edge[k] = abs(v) / HALF
            }
        }
        palette =
            LPalette(
                lramp(12, lcol("#040309"), lcol("#120c22"), lcol("#2a1f4a"), lcol("#3a2a6a")) +
                    lramp(16, lcol("#04201e"), lcol("#127a70"), lcol("#2fd2c0"), lcol("#a8fff2"), lcol("#f0fffc")) +
                    lramp(16, lcol("#260a14"), lcol("#8a2a44"), lcol("#ff6f8e"), lcol("#ffc8d4"), lcol("#fff4f6")) +
                    lramp(10, lcol("#1a1440"), lcol("#5a4aa0"), lcol("#a08ae0"), lcol("#e8e0ff")) +
                    lramp(6, lcol("#d8a840"), lcol("#ffe08a"), lcol("#fffbe8")),
            )
        buildBack(rng)
    }

    private fun point(
        u: Float,
        v: Float,
    ): FloatArray {
        val h = twist * u / 2f
        val r = 1f + v * cos(h)
        return floatArrayOf(r * cos(u), r * sin(u), v * sin(h))
    }

    private fun buildBack(rng: com.jantursky.kascii.core.Rng) {
        back.clear()
        val cx = LCOLS / 2f
        val cy = LROWS * L_ROW_U / 2f
        for (y in 0 until LROWS) {
            for (x in 0 until LCOLS) {
                val i = y * LCOLS + x
                val dx = (x + 0.5f - cx) / 80f
                val dy = ((y + 0.5f) * L_ROW_U - cy) / 50f
                val k = 0.3f / (1f + 2.5f * (dx * dx + dy * dy))
                back.set(i, 0.012f + haze.r * k, 0.01f + haze.g * k, 0.025f + haze.b * k, k)
            }
        }
        // Faint concentric guide circles behind the strip.
        for (ring in 1..3) {
            val rr = 30f + ring * 18f
            val steps = (rr * 9f).toInt()
            for (s in 0 until steps) {
                if ((s / 3) % 2 == 1) continue
                val a = LTAU * s / steps
                val i = lcell(cx + rr * 1.5f * cos(a), cy + rr * 0.85f * sin(a))
                if (i >= 0) back.set(i, 0.13f, 0.1f, 0.26f, 0.8f, '·')
            }
        }
        repeat(60) {
            val i = rng.int(LCELLS)
            back.set(i, 0.2f, 0.17f, 0.36f, 0.8f, '·')
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = LTAU * t / LOOP
        frame.copyFrom(back)
        depth.clear()
        val yaw = phi + tone
        val pitch = 0.62f + 0.2f * sin(phi)
        val cyw = cos(yaw)
        val syw = sin(yaw)
        val cp = cos(pitch)
        val sp = sin(pitch)
        var lx = -0.45f
        var ly = 0.7f
        var lz = -0.55f
        val ll = 1f / sqrt(lx * lx + ly * ly + lz * lz)
        lx *= ll
        ly *= ll
        lz *= ll
        var hx = lx
        var hy = ly
        var hz = lz - 1f
        val hl = 1f / sqrt(hx * hx + hy * hy + hz * hz)
        hx *= hl
        hy *= hl
        hz *= hl
        val ox = LCOLS / 2f
        val oy = LROWS * L_ROW_U / 2f
        for (k in 0 until n) {
            val x1 = px[k] * cyw - py[k] * syw
            val y1 = px[k] * syw + py[k] * cyw
            val y2 = y1 * cp - pz[k] * sp
            val z2 = y1 * sp + pz[k] * cp
            val persp = DIST / (DIST + z2)
            val i = lcell(ox + x1 * SCALE * persp * 1.3f, oy - y2 * SCALE * persp)
            if (i < 0 || !depth.test(i, z2)) continue
            var m = nx[k] * cyw - ny[k] * syw
            val n1 = nx[k] * syw + ny[k] * cyw
            var o = n1 * sp + nz[k] * cp
            var nn = n1 * cp - nz[k] * sp
            // Which way the surface faces the viewer picks the tone, and that tone runs on round the strip so the
            // single side changes colour smoothly instead of at a seam.
            val front = o < 0f
            val s = if (front) su[k] else 1f - su[k]
            if (!front) {
                m = -m
                nn = -nn
                o = -o
            }
            val diff = max(0f, m * lx + nn * ly + o * lz)
            val spec = max(0f, m * hx + nn * hy + o * hz).pow(30)
            val base = sideA.mix(sideB, lsmooth(0.12f, 0.88f, s))
            val rim = lsmooth(0.8f, 1f, edge[k]) * 0.5f
            val fog = lclamp01(1f - (z2 + 1.4f) * 0.12f)
            val lit = (0.16f + 0.86f * diff) * fog
            val r = base.r * (lit + rim * fog) + spec * 0.85f
            val g = base.g * (lit + rim * fog) + spec * 0.85f
            val b = base.b * (lit + rim * fog) + spec * 0.85f
            val lum = 0.3f * r + 0.55f * g + 0.15f * b
            val cov =
                if (lum > 0.26f) {
                    2f
                } else if (lum > 0.1f) {
                    1.8f
                } else if (lum > 0.035f) {
                    1.55f
                } else {
                    0.95f
                }
            frame.set(i, r, g, b, cov)
        }
        drawBead(phi, cyw, syw, cp, sp, ox, oy)
        frame.output(grid, palette, 0.05f)
    }

    /** A glowing bead that rides the centre line twice round per loop, which brings it back to the side it started. */
    private fun drawBead(
        phi: Float,
        cyw: Float,
        syw: Float,
        cp: Float,
        sp: Float,
        ox: Float,
        oy: Float,
    ) {
        for (trail in 22 downTo 0) {
            val u = 2f * phi - trail * 0.045f
            val p = point(u, 0f)
            val pu = point(u + 0.001f, 0f)
            val pv = point(u, 0.001f)
            val ax = pu[0] - p[0]
            val ay = pu[1] - p[1]
            val az = pu[2] - p[2]
            val bx = pv[0] - p[0]
            val by = pv[1] - p[1]
            val bz = pv[2] - p[2]
            var cx = ay * bz - az * by
            var cy = az * bx - ax * bz
            var cz = ax * by - ay * bx
            val cl = 0.07f / sqrt(cx * cx + cy * cy + cz * cz)
            cx *= cl
            cy *= cl
            cz *= cl
            val wx = p[0] + cx
            val wy = p[1] + cy
            val wz = p[2] + cz
            val x1 = wx * cyw - wy * syw
            val y1 = wx * syw + wy * cyw
            val y2 = y1 * cp - wz * sp
            val z2 = y1 * sp + wz * cp
            val persp = DIST / (DIST + z2)
            val sx = ox + x1 * SCALE * persp * 1.3f
            val sy = oy - y2 * SCALE * persp
            if (trail > 0) {
                val i = lcell(sx, sy)
                if (i >= 0 && z2 <= depth.z[i] + 0.05f) {
                    val k = (1f - trail / 23f) * 0.9f
                    frame.light(i, bead, k)
                    if (frame.cov[i] < 1.4f) frame.ch[i] = '•'
                }
                continue
            }
            // The bead itself: a shaded ball with a halo, hidden where the strip is nearer.
            for (dy in -3..3) {
                for (dx in -8..8) {
                    val ex = dx.toFloat()
                    val ey = dy * L_ROW_U
                    val d = sqrt(ex * ex + ey * ey)
                    val i = lcell(sx + ex, sy + ey)
                    if (i < 0 || z2 > depth.z[i] + 0.05f) continue
                    if (d < 3.4f) {
                        val shade = 1f - lclamp01(((ex + 1f) * (ex + 1f) + (ey + 1f) * (ey + 1f)) / 14f)
                        frame.set(
                            i,
                            bead.r * (0.6f + 0.5f * shade) + 0.3f * shade,
                            bead.g * (0.6f + 0.5f * shade) + 0.3f * shade,
                            bead.b * (0.6f + 0.5f * shade) + 0.3f * shade,
                            2f,
                        )
                    } else {
                        frame.light(i, bead, 0.6f * lgauss(d, 3.4f, 3f))
                    }
                }
            }
        }
    }
}

/**
 * Mobius strip: a ribbon with a half twist turns slowly, its single side shading from teal to coral and back as it
 * runs round so the two faces you see at once differ in tone, with bright edges, a moving highlight and a gold bead
 * that rides the centre line twice round per loop, crossing to the far face and back. The seed sets the number of half
 * twists and the starting turn. Every motion repeats a whole number of times per loop.
 */
public fun mobiusStripPiece(seed: Int = 0): AsciiPiece {
    val scene = Mobius(seed)
    return asciiPiece(
        name = "mobius strip",
        ground = Rgb.fromHex("#040309"),
        category = "shapes",
        cols = LCOLS,
        rows = LROWS,
        fps = 24,
        still = 3f,
        loop = LOOP,
        note = "a twisted one-sided ribbon turning, a gold bead travelling along it",
    ) { time, grid -> scene.draw(lLoopTime(time, LOOP), grid) }
}

/**
 * A turning Mobius strip, two-toned, with a bead travelling along it.
 *
 * @param seed sets the number of half twists and the starting turn.
 */
@Composable
public fun AsciiMobiusStrip(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A turning Mobius strip with a bead travelling along it",
) {
    val piece = remember(seed) { mobiusStripPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
