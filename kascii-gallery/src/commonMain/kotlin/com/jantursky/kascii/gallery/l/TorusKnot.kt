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
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 14f
private const val NU = 1200
private const val NV = 44
private const val TUBE = 0.4f
private const val DIST = 10f
private const val SCALE = 14f

private class Knot(
    seed: Int,
) {
    private val frame = LFrame()
    private val depth = LDepth()
    private val palette: LPalette
    private val back = LFrame()
    private val n = NU * NV
    private val px = FloatArray(n)
    private val py = FloatArray(n)
    private val pz = FloatArray(n)
    private val nx = FloatArray(n)
    private val ny = FloatArray(n)
    private val nz = FloatArray(n)
    private val hue = FloatArray(n)
    private val dustX = FloatArray(70)
    private val dustY = FloatArray(70)
    private val dustP = FloatArray(70)
    private val dustS = IntArray(70)
    private val warm = lcol("#ffb347")
    private val coral = lcol("#ff5e7a")
    private val violet = lcol("#9b6bff")
    private val teal = lcol("#3fe0d0")
    private val glow = lcol("#5a3cff")

    init {
        val rng = lRng(seed, 31)
        // The knot type and the starting hue come from the seed.
        val kinds = arrayOf(intArrayOf(2, 3), intArrayOf(3, 2), intArrayOf(2, 5))
        val kind = kinds[rng.int(kinds.size)]
        val p = kind[0]
        val q = kind[1]
        val hueOff = rng.float()
        for (iu in 0 until NU) {
            val u = LTAU * iu / NU
            val c = centre(u, p, q)
            val c2 = centre(u + 0.001f, p, q)
            var tx = c2[0] - c[0]
            var ty = c2[1] - c[1]
            var tz = c2[2] - c[2]
            val tl = 1f / sqrt(tx * tx + ty * ty + tz * tz)
            tx *= tl
            ty *= tl
            tz *= tl
            // A frame around the tangent: one axis level with the knot plane, one across it.
            var ax = ty
            var ay = -tx
            val al = 1f / sqrt(ax * ax + ay * ay)
            ax *= al
            ay *= al
            val bx = ty * 0f - tz * ay
            val by = tz * ax - tx * 0f
            val bz = tx * ay - ty * ax
            for (iv in 0 until NV) {
                val v = LTAU * iv / NV
                val cv = cos(v)
                val sv = sin(v)
                val k = iu * NV + iv
                val ox = ax * cv + bx * sv
                val oy = ay * cv + by * sv
                val oz = bz * sv
                px[k] = c[0] + ox * TUBE
                py[k] = c[1] + oy * TUBE
                pz[k] = c[2] + oz * TUBE
                nx[k] = ox
                ny[k] = oy
                nz[k] = oz
                hue[k] = lfract(iu.toFloat() / NU + hueOff)
            }
        }
        for (d in dustX.indices) {
            dustX[d] = rng.float() * LCOLS
            dustY[d] = rng.float() * LROWS * L_ROW_U
            dustP[d] = rng.float() * LTAU
            dustS[d] = 1 + rng.int(2)
        }
        palette =
            LPalette(
                lramp(14, lcol("#04030a"), lcol("#120c26"), lcol("#2a1d55"), lcol("#5a3cff")) +
                    lramp(14, lcol("#1a0c06"), lcol("#6a3410"), lcol("#d9781c"), lcol("#ffb347"), lcol("#fff0c8")) +
                    lramp(12, lcol("#1c0610"), lcol("#7a1c34"), lcol("#ff5e7a"), lcol("#ffc4cf")) +
                    lramp(12, lcol("#0e0824"), lcol("#40287a"), lcol("#9b6bff"), lcol("#e2d4ff")) +
                    lramp(8, lcol("#04161a"), lcol("#14706a"), lcol("#3fe0d0"), lcol("#c8fff8")),
            )
        buildBack()
    }

    private fun centre(
        u: Float,
        p: Int,
        q: Int,
    ): FloatArray {
        val r = 2f + 0.95f * cos(q * u)
        return floatArrayOf(r * cos(p * u), r * sin(p * u), -1.05f * sin(q * u))
    }

    /** The static backdrop: a violet glow behind the knot and a faint ring of the orbit plane. */
    private fun buildBack() {
        back.clear()
        for (y in 0 until LROWS) {
            for (x in 0 until LCOLS) {
                val i = y * LCOLS + x
                val dx = (x + 0.5f - LCOLS / 2f) / 70f
                val dy = ((y + 0.5f) * L_ROW_U - LROWS * L_ROW_U / 2f) / 46f
                val d2 = dx * dx + dy * dy
                val k = 0.22f / (1f + 3.5f * d2)
                back.set(i, 0.015f + glow.r * k, 0.012f + glow.g * k, 0.03f + glow.b * k, k * 1.2f)
            }
        }
        // A thin elliptical ring around the subject.
        for (s in 0 until 900) {
            val a = LTAU * s / 900
            val wx = LCOLS / 2f + 86f * cos(a)
            val wy = LROWS * L_ROW_U / 2f + 13f * sin(a) + 30f
            val i = lcell(wx, wy)
            if (i < 0) continue
            back.set(i, 0.16f, 0.12f, 0.36f, 0.8f, if (s % 3 == 0) '·' else '-')
        }
    }

    /** Darkens tube cells next to a clearly nearer tube cell, so each crossing reads as over and under. */
    private fun outline() {
        val z = depth.z
        for (y in 0 until LROWS) {
            for (x in 0 until LCOLS) {
                val i = y * LCOLS + x
                val d = z[i]
                if (d == Float.MAX_VALUE) continue
                var near = d
                if (x > 0) near = minOf(near, z[i - 1])
                if (x < LCOLS - 1) near = minOf(near, z[i + 1])
                if (y > 0) near = minOf(near, z[i - LCOLS])
                if (y < LROWS - 1) near = minOf(near, z[i + LCOLS])
                if (d - near > 0.5f) {
                    frame.r[i] *= 0.18f
                    frame.g[i] *= 0.18f
                    frame.b[i] *= 0.22f
                    frame.cov[i] = 0.4f
                }
            }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = LTAU * t / LOOP
        frame.copyFrom(back)
        // Drifting dust in front of the glow.
        for (d in dustX.indices) {
            val wx = lfract((dustX[d] + t / LOOP * LCOLS * dustS[d]) / LCOLS) * LCOLS
            val tw = 0.5f + 0.5f * sin(phi * 3 + dustP[d])
            val i = lcell(wx, dustY[d])
            if (i >=
                0
            ) {
                frame.set(
                    i,
                    0.25f * tw + 0.1f,
                    0.2f * tw + 0.08f,
                    0.45f * tw + 0.15f,
                    0.9f,
                    if (tw >
                        0.7f
                    ) {
                        '•'
                    } else {
                        '·'
                    },
                )
            }
        }
        depth.clear()
        val yaw = phi
        val pitch = 0.62f + 0.22f * sin(phi)
        val roll = 0.18f * sin(2f * phi)
        val cy = cos(yaw)
        val sy = sin(yaw)
        val cp = cos(pitch)
        val sp = sin(pitch)
        val cr = cos(roll)
        val sr = sin(roll)
        // Light from the upper left, a little in front.
        var lx = -0.55f
        var ly = 0.62f
        var lz = -0.56f
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
            // Spin around the knot axis, tip it toward the viewer, then roll a little.
            val x1 = px[k] * cy - py[k] * sy
            val y1 = px[k] * sy + py[k] * cy
            val z1 = pz[k]
            val y2 = y1 * cp - z1 * sp
            val z2 = y1 * sp + z1 * cp
            val x3 = x1 * cr - y2 * sr
            val y3 = x1 * sr + y2 * cr
            val persp = DIST / (DIST + z2)
            val i = lcell(ox + x3 * SCALE * persp * 1.15f, oy - y3 * SCALE * persp)
            if (i < 0 || !depth.test(i, z2)) continue
            val m1 = nx[k] * cy - ny[k] * sy
            val n1 = nx[k] * sy + ny[k] * cy
            val o1 = nz[k]
            val n2 = n1 * cp - o1 * sp
            val o2 = n1 * sp + o1 * cp
            val m3 = m1 * cr - n2 * sr
            val n3 = m1 * sr + n2 * cr
            val diff = max(0f, m3 * lx + n3 * ly + o2 * lz)
            val spec = max(0f, m3 * hx + n3 * hy + o2 * hz).pow(36)
            val facing = max(0f, -o2)
            val rim = (1f - facing).pow(3) * 0.55f
            val base = warm.mix(coral, 0.5f - 0.5f * cos(LTAU * hue[k]))
            // A violet fill from the lower right keeps the shadow side readable.
            val fill = max(0f, m3 * 0.6f - n3 * 0.55f - o2 * 0.58f) * 0.22f
            val fog = lclamp01(1f - (z2 + 3f) * 0.07f)
            val lit = (0.14f + 0.9f * diff) * fog
            val r = base.r * lit + spec * 0.95f + (teal.r * rim + violet.r * fill) * fog
            val g = base.g * lit + spec * 0.95f + (teal.g * rim + violet.g * fill) * fog
            val b = base.b * lit + spec * 0.95f + (teal.b * rim + violet.b * fill) * fog
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
        outline()
        frame.output(grid, palette, 0.05f)
    }
}

/**
 * Torus knot: a glossy tube tied as a torus knot turns slowly in front of a violet glow, lit from the upper left so a
 * sharp highlight slides along its length, with a teal rim where the tube turns away, hidden parts sorted by depth and
 * a colour band running warm to coral to violet along the knot. The seed picks the knot type and the colour phase.
 * Every motion repeats a whole number of times per loop.
 */
public fun torusKnotPiece(seed: Int = 0): AsciiPiece {
    val scene = Knot(seed)
    return asciiPiece(
        name = "torus knot",
        ground = Rgb.fromHex("#05040b"),
        category = "shapes",
        cols = LCOLS,
        rows = LROWS,
        fps = 24,
        still = 2f,
        loop = LOOP,
        note = "a glossy tube knot turning in front of a violet glow, highlight sliding along it",
    ) { time, grid -> scene.draw(lLoopTime(time, LOOP), grid) }
}

/**
 * A shaded torus knot tube that turns slowly with a moving specular highlight.
 *
 * @param seed picks the knot type and the colour phase.
 */
@Composable
public fun AsciiTorusKnot(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A glossy torus knot turning slowly",
) {
    val piece = remember(seed) { torusKnotPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
