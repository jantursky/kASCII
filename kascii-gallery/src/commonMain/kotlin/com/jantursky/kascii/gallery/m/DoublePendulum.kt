package com.jantursky.kascii.gallery.m

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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 24f
private const val HALF = LOOP / 2f
private const val SIM_DT = 1.0 / 480.0
private const val STORE_EVERY = 4
private const val SIM_MAX = 13.5
private const val SEARCH_FROM = 8.5
private const val GRAV = 9.81
private const val ARM = 22.5f
private const val PIVOT_X = 96f
private const val PIVOT_Y = 51f
private const val TRAIL = 220
private const val TRAIL_DT = 0.024f
private const val INNER_TRAIL = 70
private const val EASE_FROM = 0.72f

private class Pendulum(
    seed: Int,
) {
    private val frame = MFrame()
    private val palette: MPalette
    private val th1: FloatArray
    private val th2: FloatArray
    private val simEnd: Float
    private val heat = FloatArray(MCELLS)
    private val hr = FloatArray(MCELLS)
    private val hg = FloatArray(MCELLS)
    private val hb = FloatArray(MCELLS)
    private val glow = FloatArray(MCELLS)
    private val blur = FloatArray(MCELLS)
    private val dir = CharArray(MCELLS)
    private val px = FloatArray(TRAIL + 1)
    private val py = FloatArray(TRAIL + 1)
    private val qx = FloatArray(INNER_TRAIL + 1)
    private val qy = FloatArray(INNER_TRAIL + 1)
    private val night = mcol("#070816")
    private val hot = mcol("#c8f6ff")
    private val cyan = mcol("#3fd8ff")
    private val magenta = mcol("#ff3fa4")
    private val violet = mcol("#5a2aa8")
    private val gold = mcol("#ffc45a")
    private val steel = mcol("#b8c4d8")

    init {
        val rng = mRng(seed, 101)
        // Released from rest high up, so it has the energy to flip over and tumble.
        var a1 = 1.9 + rng.float() * 0.9
        var a2 = 2.2 + rng.float() * 1.0
        if (rng.float() < 0.5f) {
            a1 = -a1
            a2 = -a2
        }
        var w1 = 0.0
        var w2 = 0.0
        val n = (SIM_MAX / SIM_DT).toInt()
        val stored = n / STORE_EVERY + 1
        val t1 = FloatArray(stored)
        val t2 = FloatArray(stored)
        val ke = FloatArray(stored)
        t1[0] = a1.toFloat()
        t2[0] = a2.toFloat()
        val s = DoubleArray(4)
        val tmp = DoubleArray(4)
        val k = Array(4) { DoubleArray(4) }
        for (step in 1..n) {
            // One Runge-Kutta step of the exact equations for two equal masses on equal massless rods.
            s[0] = a1
            s[1] = a2
            s[2] = w1
            s[3] = w2
            deriv(s, k[0])
            for (j in 0 until 4) tmp[j] = s[j] + 0.5 * SIM_DT * k[0][j]
            deriv(tmp, k[1])
            for (j in 0 until 4) tmp[j] = s[j] + 0.5 * SIM_DT * k[1][j]
            deriv(tmp, k[2])
            for (j in 0 until 4) tmp[j] = s[j] + SIM_DT * k[2][j]
            deriv(tmp, k[3])
            a1 += SIM_DT / 6.0 * (k[0][0] + 2 * k[1][0] + 2 * k[2][0] + k[3][0])
            a2 += SIM_DT / 6.0 * (k[0][1] + 2 * k[1][1] + 2 * k[2][1] + k[3][1])
            w1 += SIM_DT / 6.0 * (k[0][2] + 2 * k[1][2] + 2 * k[2][2] + k[3][2])
            w2 += SIM_DT / 6.0 * (k[0][3] + 2 * k[1][3] + 2 * k[2][3] + k[3][3])
            if (step % STORE_EVERY == 0) {
                val i = step / STORE_EVERY
                t1[i] = a1.toFloat()
                t2[i] = a2.toFloat()
                ke[i] = (2 * w1 * w1 + w2 * w2 + 2 * w1 * w2 * cos(a1 - a2)).toFloat()
            }
        }
        // Turn back where the pendulum is closest to rest, so the reversal is gentle.
        var best = stored - 1
        val from = (SEARCH_FROM / (SIM_DT * STORE_EVERY)).toInt()
        for (i in from until stored) if (ke[i] < ke[best]) best = i
        th1 = t1.copyOf(best + 1)
        th2 = t2.copyOf(best + 1)
        simEnd = best.toFloat()
        val list = ArrayList<MCol>()
        list += mramp(8, mcol("#020208"), night, mcol("#121530"), mcol("#20244a"))
        list += mramp(10, violet.scale(0.3f), violet, magenta, mcol("#ff9ad0"))
        list += mramp(6, mcol("#0e0620"), mcol("#1e0c3a"), mcol("#2e1250"), mcol("#6a1a6a"))
        list += mramp(8, mcol("#0a3a50"), mcol("#1a8ab8"), cyan, hot)
        list += mramp(6, mcol("#3a2a10"), mcol("#8a6020"), gold, mcol("#fff0c8"))
        list += mramp(5, mcol("#3a4050"), mcol("#6a7488"), steel, mcol("#ffffff"))
        palette = MPalette(list)
    }

    private fun deriv(
        s: DoubleArray,
        out: DoubleArray,
    ) {
        val d = s[0] - s[1]
        val den = 3.0 - cos(2.0 * d)
        val sd = sin(d)
        out[0] = s[2]
        out[1] = s[3]
        out[2] =
            (
                -3.0 * GRAV *
                    sin(
                        s[0],
                    ) - GRAV * sin(s[0] - 2.0 * s[1]) - 2.0 * sd * (s[3] * s[3] + s[2] * s[2] * cos(d))
            ) /
            den
        out[3] = 2.0 * sd * (2.0 * s[2] * s[2] + 2.0 * GRAV * cos(s[0]) + s[3] * s[3] * cos(d)) / den
    }

    /** The stored-sample position for display time [t]: forward through the run, eased to rest, then back. */
    private fun sampleAt(t: Float): Float {
        val w = mLoopTime(t, LOOP)
        val u = if (w < HALF) w / HALF else (LOOP - w) / HALF
        // Constant speed, then a cosine ease to a stop at the turning point.
        val v = 1f / (EASE_FROM + (1f - EASE_FROM) / 2f)
        val p =
            if (u < EASE_FROM) {
                u * v
            } else {
                val e = (u - EASE_FROM) / (1f - EASE_FROM)
                v * (EASE_FROM + (1f - EASE_FROM) * (e + sin(PI.toFloat() * e) / PI.toFloat()) / 2f)
            }
        return min(1f, p) * simEnd
    }

    private fun angles(
        t: Float,
        out: FloatArray,
    ) {
        val f = sampleAt(t)
        val i = min(f.toInt(), th1.size - 2)
        val a = f - i
        out[0] = th1[i] + (th1[i + 1] - th1[i]) * a
        out[1] = th2[i] + (th2[i + 1] - th2[i]) * a
    }

    private val ang = FloatArray(2)

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        for (k in 0..TRAIL) {
            angles(t - k * TRAIL_DT, ang)
            val x1 = PIVOT_X + ARM * sin(ang[0])
            val y1 = PIVOT_Y + ARM * cos(ang[0])
            px[k] = x1 + ARM * sin(ang[1])
            py[k] = y1 + ARM * cos(ang[1])
            if (k <= INNER_TRAIL) {
                qx[k] = x1
                qy[k] = y1
            }
        }
        heat.fill(0f)
        hr.fill(0f)
        hg.fill(0f)
        hb.fill(0f)
        // The outer bob's long trail: white-hot where it is now, through cyan and magenta to a deep violet.
        for (k in TRAIL - 1 downTo 0) {
            val age = k.toFloat() / TRAIL
            val c =
                when {
                    age < 0.06f -> hot.mix(cyan, age / 0.06f)
                    age < 0.22f -> cyan
                    age < 0.5f -> cyan.mix(magenta, (age - 0.22f) / 0.28f)
                    else -> magenta.mix(violet, (age - 0.5f) / 0.5f)
                }
            val k2 = (1f - age) * (1f - age) * 0.9f + 0.1f
            segment(px[k + 1], py[k + 1], px[k], py[k], c, k2)
        }
        for (k in INNER_TRAIL - 1 downTo 0) {
            val age = k.toFloat() / INNER_TRAIL
            segment(qx[k + 1], qy[k + 1], qx[k], qy[k], gold, (1f - age) * 0.45f)
        }
        // A soft halo: a separable blur of the trail strength, five cells across and three rows down.
        for (y in 0 until MROWS) {
            val o = y * MCOLS
            for (x in 0 until MCOLS) {
                var s = heat[o + x]
                if (x > 0) s += heat[o + x - 1]
                if (x < MCOLS - 1) s += heat[o + x + 1]
                if (x > 1) s += 0.5f * heat[o + x - 2]
                if (x < MCOLS - 2) s += 0.5f * heat[o + x + 2]
                blur[o + x] = s
            }
        }
        for (y in 0 until MROWS) {
            for (x in 0 until MCOLS) {
                val i = y * MCOLS + x
                var s = blur[i] * 0.3f
                if (y > 0) s += blur[i - MCOLS] * 0.18f
                if (y < MROWS - 1) s += blur[i + MCOLS] * 0.18f
                glow[i] = s
            }
        }
        for (y in 0 until MROWS) {
            val wy = (y + 0.5f) * M_ROW_U
            for (x in 0 until MCOLS) {
                val i = y * MCOLS + x
                val dx = x + 0.5f - PIVOT_X
                val dy = wy - PIVOT_Y
                val rr = sqrt(dx * dx + dy * dy)
                val back = 1f - min(1f, rr / 120f)
                var r = night.r + 0.05f * back
                var g = night.g + 0.05f * back
                var b = night.b + 0.1f * back
                // Faint rings at the reach of each arm.
                var cov = 0.04f
                if (abs(rr - ARM * 2f) < 0.6f && (x + y) % 3 == 0) cov = 0.3f
                var glyph = 0.toChar()
                val h = heat[i]
                if (h > 0.02f) {
                    val inv = 1f / h
                    val k = min(1f, h)
                    r = hr[i] * inv * (0.5f + 0.7f * k)
                    g = hg[i] * inv * (0.5f + 0.7f * k)
                    b = hb[i] * inv * (0.5f + 0.7f * k)
                    if (k > 0.14f) {
                        glyph = dir[i]
                        cov = 1f
                    } else {
                        cov = 0.3f + 2.5f * k
                    }
                } else {
                    val gl = glow[i]
                    if (gl > 0.01f) {
                        r += magenta.r * gl * 0.5f
                        g += magenta.g * gl * 0.5f
                        b += magenta.b * gl * 0.5f
                        cov = max(cov, min(0.6f, gl * 1.2f))
                    }
                }
                frame.set(i, r, g, b, cov, glyph)
            }
        }
        // The rods, the bobs and the pivot.
        val x1 = qx[0]
        val y1 = qy[0]
        val x2 = px[0]
        val y2 = py[0]
        rod(PIVOT_X, PIVOT_Y, x1, y1)
        rod(x1, y1, x2, y2)
        bob(x1, y1, gold, 0.55f)
        bob(x2, y2, hot, 1f)
        val pr = (PIVOT_Y / M_ROW_U).toInt()
        for (dx in -3..3) {
            frame.set(
                pr * MCOLS + PIVOT_X.toInt() + dx,
                steel.r * 0.7f,
                steel.g * 0.7f,
                steel.b * 0.7f,
                2f,
                if (dx ==
                    0
                ) {
                    '╦'
                } else {
                    '═'
                },
            )
        }
        frame.set(pr * MCOLS + PIVOT_X.toInt(), steel.r, steel.g, steel.b, 2f, '●')
        frame.output(grid, palette)
    }

    /** Adds a glowing line of colour [c] and strength [k] between two points in width units, keeping the strongest. */
    private fun segment(
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        c: MCol,
        k: Float,
    ) {
        val cx0 = ax
        val cy0 = ay / M_ROW_U
        val cx1 = bx
        val cy1 = by / M_ROW_U
        val n = max(1, (max(abs(cx1 - cx0), abs(cy1 - cy0)) * 1.5f).toInt())
        val glyph = lineGlyph(cx1 - cx0, cy1 - cy0)
        for (j in 0..n) {
            val f = j.toFloat() / n
            val x = (cx0 + (cx1 - cx0) * f).toInt()
            val y = (cy0 + (cy1 - cy0) * f).toInt()
            if (x !in 0 until MCOLS || y !in 0 until MROWS) continue
            val i = y * MCOLS + x
            if (k > heat[i]) {
                heat[i] = k
                hr[i] = c.r * k
                hg[i] = c.g * k
                hb[i] = c.b * k
                dir[i] = glyph
            }
        }
    }

    /** The ASCII stroke closest to a step of [dx] columns and [dy] rows. */
    private fun lineGlyph(
        dx: Float,
        dy: Float,
    ): Char =
        when {
            abs(dx) > 2.4f * abs(dy) -> '-'
            abs(dy) > 2.4f * abs(dx) -> '|'
            (dx > 0f) == (dy > 0f) -> '\\'
            else -> '/'
        }

    private fun rod(
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
    ) {
        val dx = bx - ax
        val dy = (by - ay) / M_ROW_U
        val glyph =
            when (lineGlyph(dx, dy)) {
                '-' -> '─'
                '|' -> '│'
                '/' -> '╱'
                else -> '╲'
            }
        val n = max(1, (max(abs(dx), abs(dy)) * 1.2f).toInt())
        for (j in 1 until n) {
            val f = j.toFloat() / n
            val x = (ax + (bx - ax) * f).toInt()
            val y = ((ay + (by - ay) * f) / M_ROW_U).toInt()
            if (x !in 0 until MCOLS || y !in 0 until MROWS) continue
            frame.set(y * MCOLS + x, steel.r, steel.g, steel.b, 2f, glyph)
        }
    }

    private fun bob(
        bx: Float,
        by: Float,
        c: MCol,
        k: Float,
    ) {
        val cx = bx.toInt()
        val cy = (by / M_ROW_U).toInt()
        for (dy in -2..2) {
            for (dx in -5..5) {
                val x = cx + dx
                val y = cy + dy
                if (x !in 0 until MCOLS || y !in 0 until MROWS) continue
                val ex = x + 0.5f - bx
                val ey = (y + 0.5f) * M_ROW_U - by
                val g = exp(-(ex * ex + ey * ey) / 12f) * k
                frame.light(y * MCOLS + x, c, g * 0.8f)
            }
        }
        if (cy !in 0 until MROWS) return
        if (cx in 0 until MCOLS) frame.set(cy * MCOLS + cx, c.r, c.g, c.b, 2f, if (k >= 1f) '█' else '●')
        if (k >= 1f) {
            if (cx - 1 in 0 until MCOLS) frame.set(cy * MCOLS + cx - 1, c.r, c.g, c.b, 2f, '▐')
            if (cx + 1 in 0 until MCOLS) frame.set(cy * MCOLS + cx + 1, c.r, c.g, c.b, 2f, '▌')
        }
    }
}

/**
 * Double pendulum: two rods on a pivot, released from rest high up, tumbling chaotically and painting long fading
 * trails, white-hot at the outer bob and cooling through cyan and magenta to violet, with a shorter gold trail from the
 * elbow. The motion is integrated exactly once from the seed; the piece plays it forward, eases to a stop where the
 * pendulum comes closest to rest, and plays it back, and since a run released from rest is symmetric in time, the loop
 * closes without a jump. The seed sets the release angles.
 */
public fun doublePendulumPiece(seed: Int = 0): AsciiPiece {
    val scene = Pendulum(seed)
    return asciiPiece(
        name = "double pendulum",
        ground = Rgb.fromHex("#04050c"),
        category = "physics",
        cols = MCOLS,
        rows = MROWS,
        fps = 30,
        still = 5f,
        loop = LOOP,
        note = "a chaotic double pendulum tumbling with long fading colour trails",
    ) { time, grid -> scene.draw(mLoopTime(time, LOOP), grid) }
}

/**
 * A chaotic double pendulum tumbling with long fading colour trails.
 *
 * @param seed sets the release angles.
 */
@Composable
public fun AsciiDoublePendulum(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A chaotic double pendulum tumbling with long fading colour trails",
) {
    val piece = remember(seed) { doublePendulumPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
