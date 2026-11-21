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
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 18f
private const val HELIX_R = 19f
private const val PITCH = 64f
private const val TUBE_R = 3.1f
private const val RUNG_R = 1.15f
private const val SPAN = 100f
private const val GAP = 2.3f
private const val CAM = 280f
private const val DOTS = 90

private class Dna(
    seed: Int,
) {
    private val frame = LFrame()
    private val back = LFrame()
    private val depth = LDepth()
    private val palette: LPalette
    private val glow = FloatArray(LCELLS)
    private val glowC = FloatArray(LCELLS)

    // Every surface sample: object-space position and normal, a colour index and a shade boost.
    private val px: FloatArray
    private val py: FloatArray
    private val pz: FloatArray
    private val nx: FloatArray
    private val ny: FloatArray
    private val nz: FloatArray
    private val kind: ByteArray
    private val count: Int
    private val dotX = FloatArray(DOTS)
    private val dotY = FloatArray(DOTS)
    private val dotZ = FloatArray(DOTS)
    private val dotP = FloatArray(DOTS)
    private val dotV = IntArray(DOTS)
    private val colors =
        arrayOf(
            lcol("#3fd8ff"),
            lcol("#8a6bff"),
            lcol("#ff6b7a"),
            lcol("#ffd166"),
            lcol("#20e0a8"),
            lcol("#ff9a4a"),
        )
    private val tilt = -0.07f

    init {
        val rng = lRng(seed, 71)
        val xs = ArrayList<Float>(70000)
        val ys = ArrayList<Float>(70000)
        val zs = ArrayList<Float>(70000)
        val mx = ArrayList<Float>(70000)
        val my = ArrayList<Float>(70000)
        val mz = ArrayList<Float>(70000)
        val ks = ArrayList<Byte>(70000)
        val k = LTAU / PITCH
        // The two backbones: tubes round helices half a turn less a groove apart.
        for (strand in 0..1) {
            val off = if (strand == 0) 0f else GAP
            val steps = 1150
            for (s in 0 until steps) {
                val x = -SPAN + 2f * SPAN * s / (steps - 1)
                val a = k * x + off
                val cy = HELIX_R * cos(a)
                val cz = HELIX_R * sin(a)
                // Tangent of the helix and a frame around it.
                var tx = 1f
                var ty = -HELIX_R * k * sin(a)
                var tz = HELIX_R * k * cos(a)
                val tl = 1f / sqrt(tx * tx + ty * ty + tz * tz)
                tx *= tl
                ty *= tl
                tz *= tl
                // The radial direction is already normal to the tangent.
                val ux = 0f
                val uy = cos(a)
                val uz = sin(a)
                val vx = ty * uz - tz * uy
                val vy = tz * ux - tx * uz
                val vz = tx * uy - ty * ux
                for (r in 0 until 22) {
                    val b = LTAU * r / 22
                    val c = cos(b)
                    val sn = sin(b)
                    val ox = ux * c + vx * sn
                    val oy = uy * c + vy * sn
                    val oz = uz * c + vz * sn
                    xs += x + ox * TUBE_R
                    ys += cy + oy * TUBE_R
                    zs += cz + oz * TUBE_R
                    mx += ox
                    my += oy
                    mz += oz
                    ks += strand.toByte()
                }
            }
        }
        // Base-pair rungs, ten per turn, each half coloured by its base.
        val perTurn = 10
        val step = PITCH / perTurn
        var x = -SPAN + step * 0.5f
        while (x < SPAN) {
            val a = k * x
            val y1 = HELIX_R * cos(a)
            val z1 = HELIX_R * sin(a)
            val y2 = HELIX_R * cos(a + GAP)
            val z2 = HELIX_R * sin(a + GAP)
            val pair = rng.int(2)
            val flip = rng.int(2) == 1
            var dy = y2 - y1
            var dz = z2 - z1
            val len = sqrt(dy * dy + dz * dz)
            dy /= len
            dz /= len
            val steps = (len / 0.45f).toInt()
            for (s in 0..steps) {
                val f = s.toFloat() / steps
                val half = if ((f < 0.5f) != flip) 0 else 1
                val col = 2 + pair * 2 + half
                for (r in 0 until 10) {
                    val b = LTAU * r / 10
                    // A frame around the rung axis (0, dy, dz): the x axis and their cross.
                    val ox = cos(b)
                    val oy = -dz * sin(b)
                    val oz = dy * sin(b)
                    xs += x + ox * RUNG_R
                    ys += y1 + (y2 - y1) * f + oy * RUNG_R
                    zs += z1 + (z2 - z1) * f + oz * RUNG_R
                    mx += ox
                    my += oy
                    mz += oz
                    ks += col.toByte()
                }
            }
            x += step
        }
        count = xs.size
        px = FloatArray(count) { xs[it] }
        py = FloatArray(count) { ys[it] }
        pz = FloatArray(count) { zs[it] }
        nx = FloatArray(count) { mx[it] }
        ny = FloatArray(count) { my[it] }
        nz = FloatArray(count) { mz[it] }
        kind = ByteArray(count) { ks[it] }
        for (d in 0 until DOTS) {
            dotX[d] = rng.float() * LCOLS
            dotY[d] = rng.float() * LROWS * L_ROW_U
            dotZ[d] = rng.float()
            dotP[d] = rng.float() * LTAU
            dotV[d] = 1 + rng.int(2)
        }
        palette =
            LPalette(
                lramp(12, lcol("#02040a"), lcol("#06102a"), lcol("#0e2050"), lcol("#1a3270")) +
                    lramp(12, lcol("#04202c"), lcol("#14708c"), lcol("#3fd8ff"), lcol("#c8f6ff"), lcol("#ffffff")) +
                    lramp(10, lcol("#120c34"), lcol("#40308a"), lcol("#8a6bff"), lcol("#dcd0ff")) +
                    lramp(8, lcol("#2a0a10"), lcol("#a03040"), lcol("#ff6b7a"), lcol("#ffd0d6")) +
                    lramp(8, lcol("#2a2008"), lcol("#a08030"), lcol("#ffd166"), lcol("#fff4d0")) +
                    lramp(8, lcol("#04261c"), lcol("#108a68"), lcol("#20e0a8"), lcol("#c8fff0")) +
                    lramp(8, lcol("#2a1406"), lcol("#a05820"), lcol("#ff9a4a"), lcol("#ffe0c8")),
            )
        buildBack()
    }

    private fun buildBack() {
        back.clear()
        for (y in 0 until LROWS) {
            for (x in 0 until LCOLS) {
                val dy = (y + 0.5f) / LROWS - 0.5f
                val k = 0.12f * (1f - 2.2f * dy * dy)
                back.set(y * LCOLS + x, 0.008f + 0.05f * k, 0.014f + 0.12f * k, 0.03f + 0.35f * k, k * 0.5f)
            }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = LTAU * t / LOOP
        frame.copyFrom(back)
        depth.clear()
        glow.fill(0f)
        glowC.fill(0f)
        // Far particles behind the helix.
        particles(phi, t, false)
        val turn = phi
        val ct = cos(turn)
        val st = sin(turn)
        val cz = cos(tilt)
        val sz = sin(tilt)
        // The whole molecule rocks a little around the vertical axis.
        val rock = 0.2f * sin(phi)
        val cr = cos(rock)
        val sr = sin(rock)
        var lx = -0.4f
        var ly = 0.75f
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
        for (i in 0 until count) {
            // Turn around the long axis, rock around the vertical, tilt in the picture plane.
            val y1 = py[i] * ct - pz[i] * st
            val z1 = py[i] * st + pz[i] * ct
            val x2 = px[i] * cr + z1 * sr
            val z2 = -px[i] * sr + z1 * cr
            val x3 = x2 * cz - y1 * sz
            val y3 = x2 * sz + y1 * cz
            val p = CAM / (CAM + z2)
            val sx = ox + x3 * p
            val sy = oy - y3 * p
            val c = lcell(sx, sy)
            if (c < 0 || !depth.test(c, z2)) continue
            val n1 = ny[i] * ct - nz[i] * st
            val o1 = ny[i] * st + nz[i] * ct
            val m2 = nx[i] * cr + o1 * sr
            val o2 = -nx[i] * sr + o1 * cr
            val m3 = m2 * cz - n1 * sz
            val n3 = m2 * sz + n1 * cz
            val diff = max(0f, m3 * lx + n3 * ly + o2 * lz)
            val spec = max(0f, m3 * hx + n3 * hy + o2 * hz).pow(24)
            val kd = kind[i].toInt()
            val base = colors[kd]
            val fog = lclamp01(0.95f - z2 / (HELIX_R * 2.6f))
            val lit = (0.16f + 0.9f * diff) * fog
            val sp = if (kd < 2) spec * 0.9f else spec * 0.5f
            val r = base.r * lit + sp
            val g = base.g * lit + sp
            val b = base.b * lit + sp
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
            frame.set(c, r, g, b, cov)
            if (kd < 2 && (i and 7) == 0) splashGlow(sx, sy, fog, kd)
        }
        // The glow lands only on the empty space around the molecule.
        for (i in 0 until LCELLS) {
            val gk = glow[i]
            if (gk <= 0.003f || depth.z[i] != Float.MAX_VALUE) continue
            val mixc = glowC[i] / gk
            val c = colors[0].mix(colors[1], lclamp01(mixc))
            frame.light(i, c, min(0.55f, gk))
        }
        particles(phi, t, true)
        frame.output(grid, palette, 0.045f)
    }

    private fun splashGlow(
        sx: Float,
        sy: Float,
        k: Float,
        strand: Int,
    ) {
        val cx = sx.toInt()
        val cy = (sy / L_ROW_U).toInt()
        for (dy in -3..3) {
            val y = cy + dy
            if (y < 0 || y >= LROWS) continue
            for (dx in -8..8) {
                val x = cx + dx
                if (x < 0 || x >= LCOLS) continue
                val ex = dx.toFloat()
                val ey = dy * L_ROW_U
                val w = 0.0075f * k / (1f + (ex * ex + ey * ey) / 10f)
                val i = y * LCOLS + x
                glow[i] += w
                glowC[i] += w * strand
            }
        }
    }

    /** Motes drifting across at two depths: far ones dim and slow behind, near ones brighter in front. */
    private fun particles(
        phi: Float,
        t: Float,
        near: Boolean,
    ) {
        for (d in 0 until DOTS) {
            if ((dotZ[d] > 0.7f) != near) continue
            val wx = lfract((dotX[d] - t / LOOP * LCOLS * dotV[d]) / LCOLS) * LCOLS
            val wy = dotY[d] + 3f * sin(phi * 2f + dotP[d])
            val i = lcell(wx, wy)
            if (i < 0) continue
            if (!near && depth.z[i] != Float.MAX_VALUE) continue
            val tw = 0.55f + 0.45f * sin(phi * 3f + dotP[d])
            val k = (0.25f + 0.55f * dotZ[d]) * tw
            val c = if (d % 3 == 0) colors[1] else colors[0]
            frame.set(i, c.r * k + 0.04f, c.g * k + 0.04f, c.b * k + 0.06f, 0.9f, if (near && tw > 0.6f) '•' else '·')
        }
    }
}

/**
 * DNA helix: a double helix of two glossy backbones, one cyan and one violet, joined by base-pair rungs whose halves
 * carry the colours of their bases, turns slowly around its long axis while rocking a little, glowing softly into the
 * dark around it, with motes drifting across in front and behind. The seed writes the base sequence. Every motion
 * repeats a whole number of times per loop.
 */
public fun dnaHelixPiece(seed: Int = 0): AsciiPiece {
    val scene = Dna(seed)
    return asciiPiece(
        name = "dna helix",
        ground = Rgb.fromHex("#02040a"),
        category = "shapes",
        cols = LCOLS,
        rows = LROWS,
        fps = 24,
        still = 4f,
        loop = LOOP,
        note = "a glowing double helix with coloured base pairs turning slowly among drifting motes",
    ) { time, grid -> scene.draw(lLoopTime(time, LOOP), grid) }
}

/**
 * A slowly turning DNA double helix with coloured base pairs and drifting motes.
 *
 * @param seed writes the base sequence.
 */
@Composable
public fun AsciiDnaHelix(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A glowing DNA double helix turning slowly",
) {
    val piece = remember(seed) { dnaHelixPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
