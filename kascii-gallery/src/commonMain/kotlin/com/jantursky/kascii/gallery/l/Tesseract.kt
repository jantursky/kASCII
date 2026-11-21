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
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val LOOP = 20f
private const val SCALE = 19f
private const val DIST4 = 4.6f
private const val DIST3 = 5.5f
private const val REACH = 7f

private class Hypercube(
    seed: Int,
) {
    private val frame = LFrame()
    private val back = LFrame()
    private val palette: LPalette
    private val ea = IntArray(32)
    private val eb = IntArray(32)
    private val sx = FloatArray(16)
    private val sy = FloatArray(16)
    private val sz = FloatArray(16)
    private val sw = FloatArray(16)
    private val best = FloatArray(LCELLS)
    private val bestT = FloatArray(LCELLS)
    private val bestE = IntArray(LCELLS)
    private val gr = FloatArray(LCELLS)
    private val gg = FloatArray(LCELLS)
    private val gb = FloatArray(LCELLS)
    private val near = lcol("#9ff8ff")
    private val mid = lcol("#4a8cff")
    private val far = lcol("#7a3cc8")
    private val inner = lcol("#ff5ec8")
    private val tilt: Float

    init {
        val rng = lRng(seed, 61)
        tilt = 0.3f + rng.float() * 0.4f
        var e = 0
        for (a in 0 until 16) {
            for (bit in 0 until 4) {
                val b = a xor (1 shl bit)
                if (b > a) {
                    ea[e] = a
                    eb[e] = b
                    e++
                }
            }
        }
        palette =
            LPalette(
                lramp(12, lcol("#020309"), lcol("#080c22"), lcol("#141a40"), lcol("#222a60")) +
                    lramp(14, lcol("#0a1840"), lcol("#2050b0"), lcol("#4a8cff"), lcol("#9ff8ff"), lcol("#f0ffff")) +
                    lramp(10, lcol("#160a30"), lcol("#40207a"), lcol("#7a3cc8"), lcol("#c8a0ff")) +
                    lramp(10, lcol("#2a0822"), lcol("#901c6a"), lcol("#ff5ec8"), lcol("#ffd0f0")),
            )
        buildBack(rng)
    }

    private fun buildBack(rng: com.jantursky.kascii.core.Rng) {
        back.clear()
        for (y in 0 until LROWS) {
            for (x in 0 until LCOLS) {
                val i = y * LCOLS + x
                val dx = (x + 0.5f - LCOLS / 2f) / 90f
                val dy = ((y + 0.5f) * L_ROW_U - LROWS * L_ROW_U / 2f) / 55f
                val k = 0.16f / (1f + 3f * (dx * dx + dy * dy))
                back.set(i, 0.01f + 0.08f * k, 0.012f + 0.1f * k, 0.03f + 0.4f * k, k * 0.8f)
            }
        }
        // A receding floor grid under the figure.
        val horizon = 38
        for (y in horizon until LROWS) {
            val d = (y - horizon + 1).toFloat()
            for (x in 0 until LCOLS) {
                val u = (x - LCOLS / 2f) / d
                val onCol = lfract(u / 6f) < 0.06f * d / 3f + 0.05f
                val onRow = y == horizon || (y - horizon) % 3 == 2
                if (onCol || onRow) {
                    val k = 0.09f + 0.02f * d
                    back.set(y * LCOLS + x, k * 0.4f, k * 0.6f, k * 1.6f, 0.9f, if (onRow) '-' else '·')
                }
            }
        }
        repeat(70) {
            val i = rng.int(LCOLS * 36)
            back.set(i, 0.18f, 0.2f, 0.4f, 0.8f, '·')
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = LTAU * t / LOOP
        frame.copyFrom(back)
        best.fill(Float.MAX_VALUE)
        gr.fill(0f)
        gg.fill(0f)
        gb.fill(0f)
        // A double rotation in the XW and ZW planes, then a slow turn and a tilt in ordinary space.
        val a1 = phi
        val a2 = 2f * phi
        val yaw = phi + 0.55f
        val c1 = cos(a1)
        val s1 = sin(a1)
        val c2 = cos(a2)
        val s2 = sin(a2)
        val cy = cos(yaw)
        val sy0 = sin(yaw)
        val ct = cos(tilt)
        val st = sin(tilt)
        val ox = LCOLS / 2f
        val oy = LROWS * L_ROW_U / 2f - 1f
        for (v in 0 until 16) {
            var x = if (v and 1 != 0) 1f else -1f
            val y = if (v and 2 != 0) 1f else -1f
            var z = if (v and 4 != 0) 1f else -1f
            var w = if (v and 8 != 0) 1f else -1f
            val xw = x * c1 - w * s1
            w = x * s1 + w * c1
            x = xw
            val zw = z * c2 - w * s2
            w = z * s2 + w * c2
            z = zw
            val p4 = DIST4 / (DIST4 - w)
            val x3 = x * p4
            val y3 = y * p4
            val z3 = z * p4
            val xr = x3 * cy + z3 * sy0
            val zr = -x3 * sy0 + z3 * cy
            val yr = y3 * ct - zr * st
            val zt = y3 * st + zr * ct
            val p3 = DIST3 / (DIST3 + zt)
            sx[v] = ox + xr * SCALE * p3 * 1.25f
            sy[v] = oy - yr * SCALE * p3
            sz[v] = zt
            sw[v] = w
        }
        for (e in 0 until 32) segment(e)
        val zr = 2.4f
        for (i in 0 until LCELLS) {
            // Glow first, then the hard line on top.
            val glow = gr[i] + gg[i] + gb[i]
            if (glow > 0.004f) {
                frame.r[i] += gr[i]
                frame.g[i] += gg[i]
                frame.b[i] += gb[i]
                if (frame.cov[i] < 1.4f) frame.cov[i] = max(frame.cov[i], min(0.8f, glow * 1.6f))
            }
            if (best[i] < 1f) {
                val e = bestE[i]
                val tt = bestT[i]
                val z = sz[ea[e]] + (sz[eb[e]] - sz[ea[e]]) * tt
                val w = sw[ea[e]] + (sw[eb[e]] - sw[ea[e]]) * tt
                val c = edgeColor(z, w, zr)
                val dx = sx[eb[e]] - sx[ea[e]]
                val dy = sy[eb[e]] - sy[ea[e]]
                val glyph =
                    when (val g = lstroke(dx, dy)) {
                        '-' -> '─'
                        '|' -> '│'
                        else -> g
                    }
                frame.set(i, c.r, c.g, c.b, 2f, glyph)
            }
        }
        // Vertices as bright knots, nearest drawn last.
        val order = (0 until 16).sortedByDescending { sz[it] }
        for (v in order) {
            val c = edgeColor(sz[v], sw[v], zr)
            val big = sz[v] < -0.4f
            val i = lcell(sx[v], sy[v])
            if (i < 0) continue
            frame.set(i, c.r * 1.3f + 0.15f, c.g * 1.3f + 0.15f, c.b * 1.3f + 0.15f, 2f, if (big) '█' else '●')
            if (big) {
                val x = i % LCOLS
                if (x > 0) frame.set(i - 1, c.r, c.g, c.b, 2f, '●')
                if (x < LCOLS - 1) frame.set(i + 1, c.r, c.g, c.b, 2f, '●')
            }
        }
        frame.output(grid, palette, 0.04f)
    }

    /** Near edges are hot cyan, far ones deep violet; the inner cube (w toward the viewer) leans pink. */
    private fun edgeColor(
        z: Float,
        w: Float,
        range: Float,
    ): LCol {
        val d = lclamp01((z + range) / (2f * range))
        val base = if (d < 0.5f) near.mix(mid, d * 2f) else mid.mix(far, (d - 0.5f) * 2f)
        val k = 1.3f - 0.75f * d
        val pink = lsmooth(0.2f, 1f, -w) * 0.55f
        return base.mix(inner, pink).scale(k)
    }

    /** Records the nearest-line distance of every cell near edge [e] and adds its glow. */
    private fun segment(e: Int) {
        val ax = sx[ea[e]]
        val ay = sy[ea[e]]
        val bx = sx[eb[e]]
        val by = sy[eb[e]]
        val dx = bx - ax
        val dy = by - ay
        val len2 = max(1e-4f, dx * dx + dy * dy)
        val zm = (sz[ea[e]] + sz[eb[e]]) / 2f
        val wm = (sw[ea[e]] + sw[eb[e]]) / 2f
        val c = edgeColor(zm, wm, 2.4f)
        val strength = 0.17f * (1.2f - lclamp01((zm + 2.4f) / 4.8f))
        val x0 = max(0, floor(min(ax, bx) - REACH).toInt())
        val x1 = min(LCOLS - 1, (max(ax, bx) + REACH).toInt())
        val y0 = max(0, floor((min(ay, by) - REACH) / L_ROW_U).toInt())
        val y1 = min(LROWS - 1, ((max(ay, by) + REACH) / L_ROW_U).toInt())
        for (y in y0..y1) {
            val py = (y + 0.5f) * L_ROW_U
            for (x in x0..x1) {
                val px = x + 0.5f
                val tt = lclamp01(((px - ax) * dx + (py - ay) * dy) / len2)
                val qx = ax + dx * tt - px
                val qw = ay + dy * tt - py
                val qy = qw / L_ROW_U * 1.6f
                val d2 = qx * qx + qy * qy
                val box = max(kotlin.math.abs(qx) / 0.55f, kotlin.math.abs(qw) / 1.35f)
                val i = y * LCOLS + x
                val z = sz[ea[e]] + (sz[eb[e]] - sz[ea[e]]) * tt
                // Prefer the nearer edge where two lines meet the same cell.
                val key = box + (z + 3f) * 0.01f
                if (key < best[i]) {
                    best[i] = key
                    bestT[i] = tt
                    bestE[i] = e
                }
                val g = strength * exp(-d2 / 5f)
                gr[i] += c.r * g
                gg[i] += c.g * g
                gb[i] += c.b * g
            }
        }
    }
}

/**
 * Tesseract: a four-dimensional hypercube turns in a double rotation through the XW and ZW planes while slowly turning
 * in ordinary space, its sixteen corners and thirty-two edges projected twice down to the screen, every edge glowing hot
 * cyan when near and deep violet when far, the inner cube leaning pink, above a receding floor grid. The seed sets the
 * viewing tilt. Every motion repeats a whole number of times per loop.
 */
public fun tesseractPiece(seed: Int = 0): AsciiPiece {
    val scene = Hypercube(seed)
    return asciiPiece(
        name = "tesseract",
        ground = Rgb.fromHex("#020309"),
        category = "shapes",
        cols = LCOLS,
        rows = LROWS,
        fps = 24,
        still = 2f,
        loop = LOOP,
        note = "a four-dimensional hypercube in double rotation, edges glowing by depth",
    ) { time, grid -> scene.draw(lLoopTime(time, LOOP), grid) }
}

/**
 * A glowing four-dimensional hypercube projected to the screen in double rotation.
 *
 * @param seed sets the viewing tilt.
 */
@Composable
public fun AsciiTesseract(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A glowing four-dimensional hypercube rotating",
) {
    val piece = remember(seed) { tesseractPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
