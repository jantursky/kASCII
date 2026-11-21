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
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 16f
private const val RADIUS = 43f
private const val DIST = 6f

private class Ico(
    seed: Int,
) {
    private val frame = LFrame()
    private val back = LFrame()
    private val palette: LPalette
    private val vx: FloatArray
    private val vy: FloatArray
    private val vz: FloatArray
    private val faces: IntArray
    private val tint: FloatArray
    private val sx: FloatArray
    private val sy: FloatArray
    private val rx: FloatArray
    private val ry: FloatArray
    private val rz: FloatArray
    private val tilt: Float
    private val cool = lcol("#2fa8c8")
    private val deep = lcol("#1b3a8a")
    private val warm = lcol("#ffcf7a")
    private val wire = lcol("#7af0ff")
    private val sky = lcol("#0b1430")

    init {
        val rng = lRng(seed, 47)
        tilt = 0.25f + rng.float() * 0.35f
        // The icosahedron: every vertex of (0, +-1, +-g) and its cyclic turns, faces where three are edge-adjacent.
        val g = (1f + sqrt(5f)) / 2f
        val base = mutableListOf<FloatArray>()
        for (a in listOf(-1f, 1f)) {
            for (b in listOf(-g, g)) {
                base += floatArrayOf(0f, a, b)
                base += floatArrayOf(a, b, 0f)
                base += floatArrayOf(b, 0f, a)
            }
        }
        val tri = mutableListOf<IntArray>()

        fun d2(
            p: FloatArray,
            q: FloatArray,
        ) = (p[0] - q[0]) * (p[0] - q[0]) + (p[1] - q[1]) * (p[1] - q[1]) + (p[2] - q[2]) * (p[2] - q[2])
        for (i in base.indices) {
            for (j in i + 1 until base.size) {
                if (abs(d2(base[i], base[j]) - 4f) > 0.01f) continue
                for (k in j + 1 until base.size) {
                    if (abs(d2(base[i], base[k]) - 4f) > 0.01f || abs(d2(base[j], base[k]) - 4f) > 0.01f) continue
                    tri += intArrayOf(i, j, k)
                }
            }
        }
        // One step of subdivision, every vertex pushed out to the unit sphere.
        val verts = base.map { v -> norm(v) }.toMutableList()
        val mids = HashMap<Long, Int>()

        fun mid(
            a: Int,
            b: Int,
        ): Int {
            val key = min(a, b).toLong() * 1000L + max(a, b)
            return mids.getOrPut(key) {
                val p = verts[a]
                val q = verts[b]
                verts += norm(floatArrayOf(p[0] + q[0], p[1] + q[1], p[2] + q[2]))
                verts.size - 1
            }
        }
        val out = mutableListOf<IntArray>()
        for (f in tri) {
            val ab = mid(f[0], f[1])
            val bc = mid(f[1], f[2])
            val ca = mid(f[2], f[0])
            out += intArrayOf(f[0], ab, ca)
            out += intArrayOf(ab, f[1], bc)
            out += intArrayOf(ca, bc, f[2])
            out += intArrayOf(ab, bc, ca)
        }
        vx = FloatArray(verts.size) { verts[it][0] }
        vy = FloatArray(verts.size) { verts[it][1] }
        vz = FloatArray(verts.size) { verts[it][2] }
        faces = IntArray(out.size * 3)
        for ((n, f) in out.withIndex()) {
            // Wind every face the same way round, seen from outside.
            val ax = vx[f[1]] - vx[f[0]]
            val ay = vy[f[1]] - vy[f[0]]
            val az = vz[f[1]] - vz[f[0]]
            val bx = vx[f[2]] - vx[f[0]]
            val by = vy[f[2]] - vy[f[0]]
            val bz = vz[f[2]] - vz[f[0]]
            val nx = ay * bz - az * by
            val ny = az * bx - ax * bz
            val nz = ax * by - ay * bx
            val out1 = nx * vx[f[0]] + ny * vy[f[0]] + nz * vz[f[0]] > 0f
            faces[n * 3] = f[0]
            faces[n * 3 + 1] = if (out1) f[1] else f[2]
            faces[n * 3 + 2] = if (out1) f[2] else f[1]
        }
        tint = FloatArray(out.size) { rng.float() }
        sx = FloatArray(verts.size)
        sy = FloatArray(verts.size)
        rx = FloatArray(verts.size)
        ry = FloatArray(verts.size)
        rz = FloatArray(verts.size)
        palette =
            LPalette(
                lramp(12, lcol("#03050c"), lcol("#0b1430"), lcol("#18285a"), lcol("#2c4290")) +
                    lramp(16, lcol("#061a26"), lcol("#11506a"), lcol("#2fa8c8"), lcol("#8ce6f4"), lcol("#e8fcff")) +
                    lramp(10, lcol("#2a1c10"), lcol("#7a5428"), lcol("#e0a850"), lcol("#ffcf7a"), lcol("#fff4dc")) +
                    lramp(8, lcol("#0c2a3a"), lcol("#3fa0b8"), lcol("#7af0ff"), lcol("#d8fcff")) +
                    lramp(8, lcol("#24384a"), lcol("#6a8a90"), lcol("#c8d0b0"), lcol("#fff8e8")),
            )
        buildBack(rng)
    }

    private fun norm(v: FloatArray): FloatArray {
        val l = 1f / sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
        return floatArrayOf(v[0] * l, v[1] * l, v[2] * l)
    }

    private fun buildBack(rng: com.jantursky.kascii.core.Rng) {
        back.clear()
        for (y in 0 until LROWS) {
            for (x in 0 until LCOLS) {
                val i = y * LCOLS + x
                val vy = y / (LROWS - 1f)
                val k = 0.35f + 0.65f * vy
                back.set(i, sky.r * k * 0.5f, sky.g * k * 0.5f, sky.b * k * 0.6f, 0.15f * k)
            }
        }
        // A sparse field of faint stars.
        repeat(90) {
            val i = rng.int(LCELLS)
            val b = 0.15f + rng.float() * 0.3f
            back.set(i, b * 0.7f, b * 0.8f, b, 0.9f, if (b > 0.36f) '+' else '·')
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = LTAU * t / LOOP
        frame.copyFrom(back)
        val cx = LCOLS / 2f
        val cy = LROWS * L_ROW_U / 2f
        // The light orbits the sphere once per loop, rising and falling a little.
        val lax = cos(phi) * 1.65f
        val lay = 0.45f + 0.35f * sin(2f * phi)
        val laz = sin(phi) * 1.5f - 0.7f
        val lpers = DIST / (DIST + laz)
        val lsx = cx + lax * RADIUS * lpers * 1.05f
        val lsy = cy - lay * RADIUS * lpers
        val lightBehind = laz > 0f && (lsx - cx) * (lsx - cx) + (lsy - cy) * (lsy - cy) < RADIUS * RADIUS
        halo(cx, cy, lsx, lsy, if (lightBehind) 0.35f else 1f)
        val yaw = phi
        val cyw = cos(yaw)
        val syw = sin(yaw)
        val ct = cos(tilt + 0.15f * sin(phi))
        val st = sin(tilt + 0.15f * sin(phi))
        for (v in vx.indices) {
            val x1 = vx[v] * cyw + vz[v] * syw
            val z1 = -vx[v] * syw + vz[v] * cyw
            val y2 = vy[v] * ct - z1 * st
            val z2 = vy[v] * st + z1 * ct
            rx[v] = x1
            ry[v] = y2
            rz[v] = z2
            val p = DIST / (DIST + z2)
            sx[v] = cx + x1 * RADIUS * p * 1.05f
            sy[v] = cy - y2 * RADIUS * p
        }
        val nf = faces.size / 3
        for (f in 0 until nf) {
            val a = faces[f * 3]
            val b = faces[f * 3 + 1]
            val c = faces[f * 3 + 2]
            // Screen-space winding: skip faces that turn away.
            val area = (sx[b] - sx[a]) * (sy[c] - sy[a]) - (sy[b] - sy[a]) * (sx[c] - sx[a])
            if (area <= 0f) continue
            val mx = (rx[a] + rx[b] + rx[c]) / 3f
            val my = (ry[a] + ry[b] + ry[c]) / 3f
            val mz = (rz[a] + rz[b] + rz[c]) / 3f
            val e1x = rx[b] - rx[a]
            val e1y = ry[b] - ry[a]
            val e1z = rz[b] - rz[a]
            val e2x = rx[c] - rx[a]
            val e2y = ry[c] - ry[a]
            val e2z = rz[c] - rz[a]
            var nx = e1y * e2z - e1z * e2y
            var ny = e1z * e2x - e1x * e2z
            var nz = e1x * e2y - e1y * e2x
            val nl = 1f / sqrt(nx * nx + ny * ny + nz * nz)
            nx *= nl
            ny *= nl
            nz *= nl
            if (nx * mx + ny * my + nz * mz < 0f) {
                nx = -nx
                ny = -ny
                nz = -nz
            }
            var lx = lax - mx
            var ly = lay - my
            var lz = laz - mz
            val ll = 1f / sqrt(lx * lx + ly * ly + lz * lz)
            lx *= ll
            ly *= ll
            lz *= ll
            val diff = max(0f, nx * lx + ny * ly + nz * lz)
            // Half vector between light and the viewer at -z.
            var hx = lx
            var hy = ly
            var hz = lz - 1f
            val hl = 1f / sqrt(hx * hx + hy * hy + hz * hz)
            hx *= hl
            hy *= hl
            hz *= hl
            val spec = max(0f, nx * hx + ny * hy + nz * hz).pow(28)
            val body = deep.mix(cool, 0.45f + 0.4f * tint[f])
            val amb = 0.2f + 0.12f * max(0f, ny) + 0.15f * max(0f, -nx * lx - nz * lz)
            val r = body.r * (amb + 0.85f * diff) + warm.r * diff * 0.35f + spec * 0.9f
            val g = body.g * (amb + 0.85f * diff) + warm.g * diff * 0.35f + spec * 0.9f
            val bb = body.b * (amb + 0.85f * diff) + warm.b * diff * 0.35f + spec * 0.9f
            val lum = 0.3f * r + 0.55f * g + 0.15f * bb
            val cov =
                if (lum > 0.3f) {
                    2f
                } else if (lum > 0.12f) {
                    1.8f
                } else {
                    1.55f
                }
            fill(sx[a], sy[a], sx[b], sy[b], sx[c], sy[c], r, g, bb, cov)
            val wk = 0.22f + 0.95f * diff + spec
            edge(sx[a], sy[a], sx[b], sy[b], wk)
            edge(sx[b], sy[b], sx[c], sy[c], wk)
            edge(sx[c], sy[c], sx[a], sy[a], wk)
        }
        if (!lightBehind) orb(lsx, lsy)
        frame.output(grid, palette, 0.04f)
    }

    /** Fills the cells whose centres fall in the triangle, in one flat colour. */
    private fun fill(
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        cx: Float,
        cy: Float,
        r: Float,
        g: Float,
        b: Float,
        cov: Float,
    ) {
        val x0 = max(0, floor(min(ax, min(bx, cx))).toInt())
        val x1 = min(LCOLS - 1, ceil(max(ax, max(bx, cx))).toInt())
        val y0 = max(0, floor(min(ay, min(by, cy)) / L_ROW_U).toInt())
        val y1 = min(LROWS - 1, ceil(max(ay, max(by, cy)) / L_ROW_U).toInt())
        for (y in y0..y1) {
            val py = (y + 0.5f) * L_ROW_U
            for (x in x0..x1) {
                val px = x + 0.5f
                val w0 = (bx - ax) * (py - ay) - (by - ay) * (px - ax)
                val w1 = (cx - bx) * (py - by) - (cy - by) * (px - bx)
                val w2 = (ax - cx) * (py - cy) - (ay - cy) * (px - cx)
                if (w0 >= 0f && w1 >= 0f && w2 >= 0f) frame.set(y * LCOLS + x, r, g, b, cov)
            }
        }
    }

    /** Draws a wire edge in line glyphs, brighter where the light falls. */
    private fun edge(
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        k: Float,
    ) {
        val dx = bx - ax
        val dy = by - ay
        val glyph = lstroke(dx, dy)
        val steps = (max(abs(dx), abs(dy) / L_ROW_U) * 1.5f).toInt() + 1
        for (s in 0..steps) {
            val f = s.toFloat() / steps
            val i = lcell(ax + dx * f, ay + dy * f)
            if (i < 0) continue
            frame.set(i, wire.r * k * 0.8f, wire.g * k * 0.8f, wire.b * k * 0.8f, 2f, glyph)
        }
    }

    /** A soft warm glow around the light and a rim of it around the sphere. */
    private fun halo(
        cx: Float,
        cy: Float,
        lx: Float,
        ly: Float,
        strength: Float,
    ) {
        for (y in 0 until LROWS) {
            val wy = (y + 0.5f) * L_ROW_U
            for (x in 0 until LCOLS) {
                val wx = x + 0.5f
                val dx = wx - lx
                val dy = wy - ly
                val k = strength * 0.42f / (1f + (dx * dx + dy * dy) / 60f)
                val sd = sqrt((wx - cx) * (wx - cx) + (wy - cy) * (wy - cy)) - RADIUS
                val rim = if (sd > 0f) 0.18f * lgauss(sd, 0f, 6f) else 0f
                val i = y * LCOLS + x
                frame.light(i, warm, k)
                frame.light(i, cool, rim)
            }
        }
    }

    /** The light itself, a small bright orb. */
    private fun orb(
        lx: Float,
        ly: Float,
    ) {
        for (dy in -2..2) {
            for (dx in -4..4) {
                val wx = lx + dx
                val wy = ly + dy * L_ROW_U
                val d = sqrt((dx * dx).toFloat() + (dy * L_ROW_U) * (dy * L_ROW_U))
                val i = lcell(wx, wy)
                if (i < 0 || d > 4.2f) continue
                val k = lclamp01(1.2f - d / 4f)
                frame.set(i, 1f, 0.85f + 0.15f * k, 0.55f + 0.45f * k, if (d < 2f) 2f else 1f, if (d < 2f) '█' else '●')
            }
        }
    }
}

/**
 * Icosphere: a faceted geodesic sphere turns slowly, every face flat-shaded by a warm point light that circles it once
 * per loop and passes behind it, with cyan wire edges brightening where the light falls, a glint on the faces that
 * mirror it and a soft glow around the light. The seed sets the tilt and the shade of each facet. Every motion repeats
 * a whole number of times per loop.
 */
public fun icospherePiece(seed: Int = 0): AsciiPiece {
    val scene = Ico(seed)
    return asciiPiece(
        name = "icosphere",
        ground = Rgb.fromHex("#03050c"),
        category = "shapes",
        cols = LCOLS,
        rows = LROWS,
        fps = 24,
        still = 2.5f,
        loop = LOOP,
        note = "a faceted geodesic sphere turning, lit by a warm light that circles it",
    ) { time, grid -> scene.draw(lLoopTime(time, LOOP), grid) }
}

/**
 * A flat-shaded geodesic sphere with wire edges, lit by a light that circles it.
 *
 * @param seed sets the tilt and the shade of each facet.
 */
@Composable
public fun AsciiIcosphere(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A faceted geodesic sphere lit by a circling light",
) {
    val piece = remember(seed) { icospherePiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
