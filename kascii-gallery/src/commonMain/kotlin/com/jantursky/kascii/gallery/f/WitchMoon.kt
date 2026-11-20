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
import com.jantursky.kascii.core.Rng
import com.jantursky.kascii.core.asciiPiece
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 12f
private const val TAU = AKit.TAU
private const val HEIGHT = F_ROWS * F_ASPECT

private const val MOON_X = 108f
private const val MOON_Y = 50f
private const val MOON_R = 41f
private const val GROUND = 83f
private const val HX = 30f

private const val S_NONE: Byte = 0
private const val S_HILL: Byte = 1
private const val S_RIDGE: Byte = 2
private const val S_CUT: Byte = 3

private val PALETTE: FPalette by lazy {
    FPalette(
        arrayOf(
            floatArrayOf(0.35f, 0.30f, 0.75f),
            floatArrayOf(0.60f, 0.35f, 0.70f),
            floatArrayOf(0.80f, 0.65f, 1.00f),
            floatArrayOf(1.00f, 0.93f, 0.62f),
            floatArrayOf(1.00f, 1.00f, 0.85f),
            floatArrayOf(0.55f, 1.00f, 0.30f),
            floatArrayOf(1.00f, 0.55f, 0.12f),
            floatArrayOf(1.00f, 0.78f, 0.30f),
            floatArrayOf(0.70f, 0.25f, 0.20f),
            floatArrayOf(0.40f, 0.45f, 0.70f),
            floatArrayOf(0.25f, 0.15f, 0.45f),
        ),
    )
}

private fun smooth(v: Float): Float {
    val k = v.coerceIn(0f, 1f)
    return k * k * (3f - 2f * k)
}

/** Everything that does not change from frame to frame, plus the per-frame canvas. */
private class WitchScene(
    private val seed: Int,
    private val glow: FloatArray,
    private val windowLight: FloatArray,
) {
    private val cv = FCanvas(PALETTE)
    private val baseR = FloatArray(F_CELLS)
    private val baseG = FloatArray(F_CELLS)
    private val baseB = FloatArray(F_CELLS)
    private val baseKind = ByteArray(F_CELLS)
    private val sil = ByteArray(F_CELLS)

    private val starX = FloatArray(STARS)
    private val starY = FloatArray(STARS)
    private val starPh = FloatArray(STARS)
    private val starK = IntArray(STARS)
    private val batPh = FloatArray(BATS)
    private val batPh2 = FloatArray(BATS)

    init {
        val rng = AKit.rng(seed, 71)
        for (k in 0 until STARS) {
            var x: Float
            var y: Float
            do {
                x = rng.float() * F_COLS
                y = rng.float() * 82f
            } while (sq(x - MOON_X) + sq(y - MOON_Y) < sq(MOON_R + 6f))
            starX[k] = x
            starY[k] = y
            starPh[k] = rng.float() * TAU
            starK[k] = 2 + rng.int(3)
        }
        for (k in 0 until BATS) {
            batPh[k] = rng.float() * TAU
            batPh2[k] = rng.float() * TAU
        }
        buildSky()
        buildSilhouettes(rng)
    }

    private fun sq(v: Float): Float = v * v

    private fun hillY(ux: Float): Float {
        val a = fGlow(sq(ux - 88f), 70f) * 14f
        val b = fGlow(sq(ux - 160f), 46f) * 9f
        val c = fGlow(sq(ux - 8f), 34f) * 8f
        return 95f - a - b - c + 2f * Noise.fbm(ux * 0.05f, 0f, 2f, 2, seed + 6)
    }

    private fun buildSky() {
        val craters =
            arrayOf(
                floatArrayOf(-14f, -12f, 7f),
                floatArrayOf(12f, -18f, 4.5f),
                floatArrayOf(16f, 8f, 8f),
                floatArrayOf(-22f, 10f, 4f),
                floatArrayOf(2f, 22f, 5f),
                floatArrayOf(-4f, -2f, 3f),
            )
        for (y in 0 until F_ROWS) {
            val uy = (y + 0.5f) * F_ASPECT
            val k = uy / HEIGHT
            for (x in 0 until F_COLS) {
                val i = y * F_COLS + x
                val ux = x + 0.5f
                val n = Noise.fbm(x * 0.05f, y * 0.12f, 4f, 2, seed)
                val level = (0.20f + 0.20f * k) * (0.6f + 0.8f * n)
                // Indigo at the top, plum at the horizon.
                var r = (0.30f + 0.4f * k) * level
                var g = (0.22f + 0.1f * k) * level
                var b = (0.70f - 0.1f * k) * level
                val dx = ux - MOON_X
                val dy = uy - MOON_Y
                val d2 = dx * dx + dy * dy
                val halo = fGlow(d2, 100f) * 0.28f + fGlow(d2, 58f) * 0.22f
                r += halo * 0.85f
                g += halo * 0.78f
                b += halo * 1.0f
                var kind = F_HALF
                if (d2 < MOON_R * MOON_R) {
                    val d = sqrt(d2) / MOON_R
                    val mare = Noise.fbm(ux * 0.045f, uy * 0.06f, 9f, 3, seed + 2)
                    var v = if (mare < 0.46f) 0.52f + 0.3f * mare else 0.80f + 0.2f * mare
                    for (c in craters) {
                        val cd = sqrt(sq(dx - c[0]) + sq(dy - c[1])) / c[2]
                        if (cd < 1f) {
                            v *= 0.62f + 0.38f * cd * cd
                        } else if (cd < 1.2f) {
                            v *= 1.18f
                        }
                    }
                    v *= 1f - 0.3f * d * d * d
                    r = 1.0f * v
                    g = 0.93f * v
                    b = 0.62f * v
                    kind = F_DENSE
                }
                baseR[i] = r
                baseG[i] = g
                baseB[i] = b
                baseKind[i] = kind
            }
        }
    }

    private fun mark(
        x: Float,
        y: Float,
        radius: Float,
        code: Byte,
    ) {
        val x0 = max(0, (x - radius - 1f).toInt())
        val x1 = min(F_COLS - 1, (x + radius + 1f).toInt())
        val y0 = max(0, ((y - radius - 1f) / F_ASPECT).toInt())
        val y1 = min(F_ROWS - 1, ((y + radius + 1f) / F_ASPECT).toInt())
        for (cy in y0..y1) {
            val dy = (cy + 0.5f) * F_ASPECT - y
            for (cx in x0..x1) {
                val dx = cx + 0.5f - x
                if (dx * dx + dy * dy <= radius * radius) sil[cy * F_COLS + cx] = code
            }
        }
    }

    private fun segment(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        w0: Float,
        w1: Float,
    ) {
        val len = sqrt(sq(x1 - x0) + sq(y1 - y0))
        val steps = max(1, (len / 0.8f).toInt())
        for (s in 0..steps) {
            val u = s / steps.toFloat()
            mark(x0 + (x1 - x0) * u, y0 + (y1 - y0) * u, max(1.25f, w0 + (w1 - w0) * u), S_CUT)
        }
    }

    private fun branch(
        rng: Rng,
        x: Float,
        y: Float,
        angle: Float,
        length: Float,
        width: Float,
        depth: Int,
    ) {
        val ex = x + length * sin(angle)
        val ey = y - length * cos(angle)
        segment(x, y, ex, ey, width, width * 0.72f)
        if (depth == 0) return
        val n = if (depth > 2) 2 else 2 + rng.int(2)
        for (k in 0 until n) {
            val a = angle + (rng.float() - 0.5f) * 1.9f - (if (k == 0) 0.35f else -0.35f)
            branch(rng, ex, ey, a, length * (0.62f + 0.2f * rng.float()), width * 0.72f, depth - 1)
        }
    }

    private fun buildSilhouettes(rng: Rng) {
        // The gnarled tree on the right, leaning over the moon.
        branch(Rng(seed * 31L + 5L), 160f, 94f, -0.08f, 25f, 3.2f, 5)
        // The cottage with its crooked tower, chimney and pointed roofs.
        rect(HX + 72f, GROUND - 14f, HX + 98f, GROUND + 6f)
        tri(HX + 66f, GROUND - 13f, HX + 104f, GROUND - 13f, HX + 82f, GROUND - 31f)
        rect(HX + 91f, GROUND - 35f, HX + 95f, GROUND - 22f)
        rect(HX + 90f, GROUND - 37f, HX + 96f, GROUND - 34f)
        rect(HX + 58f, GROUND - 28f, HX + 68f, GROUND + 6f)
        tri(HX + 56f, GROUND - 27f, HX + 70f, GROUND - 27f, HX + 63f, GROUND - 47f)
        // Hill on top, so the cottage stands in it.
        for (x in 0 until F_COLS) {
            val h = hillY(x + 0.5f)
            for (y in 0 until F_ROWS) {
                val uy = (y + 0.5f) * F_ASPECT
                if (uy >= h) {
                    sil[y * F_COLS + x] = if (uy - h < F_ASPECT) S_RIDGE else S_HILL
                }
            }
        }
    }

    private fun rect(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
    ) {
        for (y in max(0, (y0 / F_ASPECT).toInt())..min(F_ROWS - 1, (y1 / F_ASPECT).toInt())) {
            for (x in max(0, x0.toInt())..min(F_COLS - 1, x1.toInt())) sil[y * F_COLS + x] = S_CUT
        }
    }

    private fun tri(
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        tx: Float,
        ty: Float,
    ) {
        // A roof: base from (ax, ay) to (bx, by), apex (tx, ty).
        for (y in max(0, (ty / F_ASPECT).toInt())..min(F_ROWS - 1, (ay / F_ASPECT).toInt())) {
            val uy = (y + 0.5f) * F_ASPECT
            val u = ((uy - ty) / (ay - ty)).coerceIn(0f, 1f)
            val left = tx + (ax - tx) * u
            val right = tx + (bx - tx) * u
            for (x in max(0, left.toInt())..min(F_COLS - 1, right.toInt())) sil[y * F_COLS + x] = S_CUT
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val theta = TAU * t / LOOP
        cv.load(baseR, baseG, baseB, baseKind)
        paintClouds(t, 56f, 7f, 3, 5, 1, 2, 0.0f)
        paintClouds(t, 30f, 5f, 2, 4, 1, 1, 1.3f)
        paintStars(theta)
        paintSilhouettes()
        paintCottageLights(theta)
        paintPumpkins(theta)
        paintCauldron(theta)
        paintOwl(t)
        for (k in 0 until BATS) paintBat(k, theta)
        paintWitch(t, theta)
        paintSmoke(t)
        cv.write(grid)
    }

    private fun paintClouds(
        t: Float,
        centre: Float,
        thick: Float,
        kA: Int,
        kB: Int,
        wA: Int,
        wB: Int,
        shift: Float,
    ) {
        val y0 = max(0, ((centre - thick) / F_ASPECT).toInt())
        val y1 = min(F_ROWS - 1, ((centre + thick) / F_ASPECT).toInt())
        for (y in y0..y1) {
            val uy = (y + 0.5f) * F_ASPECT
            val env = 1f - abs(uy - centre) / thick
            if (env <= 0f) continue
            for (x in 0 until F_COLS) {
                val i = y * F_COLS + x
                val ux = x + 0.5f
                val sa = 0.5f + 0.5f * sin(TAU * (kA * ux / F_COLS - wA * t / LOOP) + shift)
                val sb = 0.5f + 0.5f * sin(TAU * (kB * ux / F_COLS - wB * t / LOOP) + uy * 0.05f + shift * 2f)
                val d = smooth((sa * sb - 0.18f) / 0.4f) * smooth(env * 1.6f)
                if (d <= 0.02f) continue
                if (cv.kind[i] == F_DENSE) {
                    val k = d * 0.9f
                    cv.r[i] += (0.20f - cv.r[i]) * k
                    cv.g[i] += (0.12f - cv.g[i]) * k
                    cv.b[i] += (0.36f - cv.b[i]) * k
                } else {
                    val k = 1f - 0.55f * d
                    cv.r[i] *= k
                    cv.g[i] *= k
                    cv.b[i] *= k
                }
            }
        }
    }

    private fun paintStars(theta: Float) {
        for (k in 0 until STARS) {
            val cx = starX[k].toInt()
            val cy = (starY[k] / F_ASPECT).toInt()
            val i = cy * F_COLS + cx
            if (cv.kind[i] == F_DENSE) continue
            val tw = 0.5f + 0.5f * sin(starK[k] * theta + starPh[k])
            if (tw < 0.25f) continue
            val v = 0.25f + 0.55f * tw
            cv.solid(i, v * 0.9f, v * 0.9f, v, if (tw > 0.78f) '•' else '·')
        }
    }

    /** A solid shape: blank in front of the moon, a dim violet mass against the sky so it still reads. */
    private fun shade(i: Int) {
        if (baseKind[i] == F_DENSE) cv.cut(i) else cv.solid(i, 0.12f, 0.07f, 0.20f)
    }

    private fun paintSilhouettes() {
        for (i in 0 until F_CELLS) {
            when (sil[i]) {
                S_CUT -> {
                    shade(i)
                }

                S_HILL -> {
                    val n = 0.05f + 0.05f * Noise.hash(i % F_COLS, i / F_COLS, 3, seed)
                    cv.solid(i, n * 1.0f, n * 0.6f, n * 1.7f)
                }

                S_RIDGE -> {
                    cv.solid(i, 0.16f, 0.09f, 0.28f)
                }
            }
        }
    }

    private fun paintCottageLights(theta: Float) {
        val flick = 0.86f + 0.14f * sin(5f * theta) * sin(3f * theta + 1f)
        val w = windowLight
        window(HX + 75f, GROUND - 10f, HX + 80f, GROUND - 4f, flick, w)
        window(HX + 91f, GROUND - 10f, HX + 96f, GROUND - 4f, flick * 0.94f, w)
        window(HX + 61f, GROUND - 22f, HX + 66f, GROUND - 15f, 0.9f + 0.1f * sin(4f * theta), w)
        window(HX + 61f, GROUND - 10f, HX + 66f, GROUND - 5f, flick, w)
        window(HX + 84f, GROUND - 7f, HX + 88f, GROUND + 3f, 0.9f, w)
        glowOn(HX + 86f, GROUND - 2f, 24f, 0.14f * flick, w)
    }

    private fun window(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        level: Float,
        c: FloatArray,
    ) {
        for (y in (y0 / F_ASPECT).toInt()..(y1 / F_ASPECT).toInt()) {
            for (x in x0.toInt()..x1.toInt()) {
                val mullion = (x == ((x0 + x1) / 2f).toInt())
                if (mullion) continue
                cv.solid(y * F_COLS + x, c[0] * level, c[1] * level, c[2] * level, '█')
            }
        }
    }

    private fun glowOn(
        cx: Float,
        cy: Float,
        radius: Float,
        gain: Float,
        c: FloatArray,
    ) {
        for (y in max(0, ((cy - radius) / F_ASPECT).toInt())..min(F_ROWS - 1, ((cy + radius) / F_ASPECT).toInt())) {
            val dy = (y + 0.5f) * F_ASPECT - cy
            for (x in max(0, (cx - radius).toInt())..min(F_COLS - 1, (cx + radius).toInt())) {
                val i = y * F_COLS + x
                if (sil[i] != S_NONE) continue
                val wgt = fGlow(sq(x + 0.5f - cx) + dy * dy, radius) * gain
                if (wgt > 0f) cv.add(i, c[0] * wgt, c[1] * wgt, c[2] * wgt)
            }
        }
    }

    private fun paintPumpkins(theta: Float) {
        val pump = floatArrayOf(1f, 0.52f, 0.1f)
        val inner = 0.85f + 0.15f * sin(4f * theta + 2f)
        for (p in 0..1) {
            val px = if (p == 0) 143f else 152f
            val py = if (p == 0) 83f else 85f
            val rx = if (p == 0) 5f else 3.6f
            val ry = rx * 0.46f
            for (y in max(0, ((py - 6f) / F_ASPECT).toInt())..min(F_ROWS - 1, ((py + 6f) / F_ASPECT).toInt())) {
                val ny = ((y + 0.5f) * F_ASPECT - py) / (ry * F_ASPECT)
                for (x in max(0, (px - 7f).toInt())..min(F_COLS - 1, (px + 7f).toInt())) {
                    val nx = (x + 0.5f - px) / rx
                    if (nx * nx + ny * ny < 1f) {
                        val eye = abs(abs(nx) - 0.38f) < 0.2f && abs(ny + 0.3f) < 0.45f
                        val mouth = abs(nx) < 0.4f && ny > 0.35f
                        if (eye || mouth) {
                            cv.solid(y * F_COLS + x, 1f * inner, 0.85f * inner, 0.3f * inner, '█')
                        } else {
                            val v = (0.72f + 0.2f * nx * nx) * inner
                            cv.solid(y * F_COLS + x, pump[0] * v, pump[1] * v, pump[2] * v)
                        }
                    }
                }
            }
        }
    }

    private fun paintCauldron(theta: Float) {
        val cx = 76f
        val cy = 84f
        val pulse = 0.85f + 0.15f * sin(3f * theta)
        glowOn(cx, cy - 4f, 24f, 0.38f * pulse, glow)
        for (y in ((cy - 8f) / F_ASPECT).toInt()..((cy + 8f) / F_ASPECT).toInt()) {
            val ny = ((y + 0.5f) * F_ASPECT - cy) / 7f
            if (ny < -0.6f || ny > 1f) continue
            val half = 11f * sqrt(max(0f, 1f - ny * ny * 0.8f))
            for (x in (cx - 12f).toInt()..(cx + 12f).toInt()) {
                val i = y * F_COLS + x
                if (abs(x + 0.5f - cx) >= half) continue
                if (ny < -0.2f) {
                    cv.solid(i, glow[0] * pulse, glow[1] * pulse, glow[2] * pulse, '█')
                } else {
                    cv.solid(i, 0.13f, 0.08f, 0.22f, '▓')
                }
            }
        }
        for (k in 0 until 6) {
            val a = AKit.frac(k / 6f + theta / TAU)
            val bx = cx + (k % 3 - 1) * 3f + 2f * sin(2f * theta + k * 1.9f)
            val by = cy - 7f - a * 26f
            val gx = bx.toInt()
            val gy = (by / F_ASPECT).toInt()
            if (gx !in 0 until F_COLS || gy !in 0 until F_ROWS) continue
            val v = (1f - a) * 0.9f
            cv.solid(
                gy * F_COLS + gx,
                glow[0] * v,
                glow[1] * v,
                glow[2] * v,
                if (a <
                    0.45f
                ) {
                    '●'
                } else if (a < 0.75f) {
                    '•'
                } else {
                    '·'
                },
            )
        }
    }

    private fun paintOwl(t: Float) {
        val blink = AKit.frac(t / LOOP * 2f) > 0.93f
        if (blink) return
        for (dx in intArrayOf(-4, -3, 3, 4)) {
            cv.solid(OWL_ROW * F_COLS + OWL_X + dx, 1f, 0.8f, 0.2f, '●')
        }
    }

    private fun paintBat(
        k: Int,
        theta: Float,
    ) {
        val cx = BAT[k * 6]
        val cy = BAT[k * 6 + 1]
        val ax = BAT[k * 6 + 2]
        val ay = BAT[k * 6 + 3]
        val kx = BAT[k * 6 + 4].toInt()
        val ky = BAT[k * 6 + 5].toInt()
        val x = (cx + ax * cos(kx * theta + batPh[k])).toInt()
        val y = ((cy + ay * sin(ky * theta + batPh2[k])) / F_ASPECT).toInt()
        val span = 3 + k % 3
        val flap = sin(24f * theta + k * 1.7f)
        cutAt(x, y)
        cutAt(x - 1, y)
        cutAt(x + 1, y)
        cutAt(x - 1, y - 1)
        cutAt(x + 1, y - 1)
        for (s in intArrayOf(-1, 1)) {
            for (d in 2..span + 1) {
                val f = (d - 1f) / span
                val row = (-1.6f * flap * f * f).let { if (it < 0f) it - 0.4f else it + 0.2f }.toInt()
                cutAt(x + s * d, y + row)
                if (d < span + 1) cutAt(x + s * d, y + row + 1)
            }
        }
    }

    private fun cutAt(
        x: Int,
        y: Int,
    ) {
        if (x in 0 until F_COLS && y in 0 until F_ROWS) shade(y * F_COLS + x)
    }

    private fun paintWitch(
        t: Float,
        theta: Float,
    ) {
        val u = t / LOOP
        val wx = -20f + 264f * u
        val wy = 30f - 3f * sin(TAU * u) + 2.5f * sin(4f * theta)
        val left = wx.toInt() - 14
        val top = (wy / F_ASPECT).toInt() - 8
        val tip =
            if (sin(4f * theta) > 0.3f) {
                1
            } else if (sin(4f * theta) < -0.3f) {
                -1
            } else {
                0
            }
        for (r in WITCH.indices) {
            val row = WITCH[r]
            for (c in row.indices) {
                if (row[c] != 'x') continue
                val x = left + c + (if (r == 0) tip else 0)
                val y = top + r
                if (x !in 0 until F_COLS || y !in 0 until F_ROWS) continue
                val i = y * F_COLS + x
                if (baseKind[i] == F_DENSE) cv.cut(i) else cv.solid(i, 0.17f, 0.10f, 0.30f)
            }
        }
        // The lantern at the end of the broom handle and the green sparks it sheds.
        val pulse = 0.8f + 0.2f * sin(3f * theta)
        for (c in 26..27) {
            val x = left + c
            val y = top + 10
            if (x in 0 until F_COLS && y in 0 until F_ROWS) {
                cv.solid(y * F_COLS + x, 1f * pulse, 0.62f * pulse, 0.18f * pulse, '●')
            }
        }
        for (k in 0 until 7) {
            val sx = left - 1f - k * 3.4f
            val sy = wy + 3f * sin(2f * theta + k * 0.8f)
            val gx = sx.toInt()
            val gy = (sy / F_ASPECT).toInt()
            if (gx !in 0 until F_COLS || gy !in 0 until F_ROWS) continue
            val v = 1f - k / 7f
            cv.solid(
                gy * F_COLS + gx,
                glow[0] * v,
                glow[1] * v,
                glow[2] * v,
                if (k <
                    2
                ) {
                    '●'
                } else if (k < 5) {
                    '•'
                } else {
                    '·'
                },
            )
        }
    }

    private fun paintSmoke(t: Float) {
        for (k in 0 until 9) {
            val a = AKit.frac(k / 9f + t / LOOP)
            val x = HX + 93f + 4f * a + 3f * sin(TAU * (a * 1.5f + k * 0.11f))
            val y = GROUND - 39f - a * 30f
            val gx = x.toInt()
            val gy = (y / F_ASPECT).toInt()
            if (gx !in 0 until F_COLS || gy !in 0 until F_ROWS) continue
            val v = 0.7f * (1f - a * 0.7f)
            val ch =
                if (a < 0.3f) {
                    '●'
                } else if (a < 0.65f) {
                    '•'
                } else {
                    '·'
                }
            val front = baseKind[gy * F_COLS + gx] == F_DENSE
            val sr = if (front) 0.22f else v * 0.6f
            val sg = if (front) 0.13f else v * 0.55f
            val sb = if (front) 0.36f else v * 0.8f
            cv.solid(gy * F_COLS + gx, sr, sg, sb, ch)
            if (a < 0.5f) cv.solid(gy * F_COLS + min(F_COLS - 1, gx + 1), sr, sg, sb, ch)
        }
    }

    private companion object {
        const val STARS = 120
        const val BATS = 7

        /** The witch on her broom, one cell per character, facing right. */
        val WITCH =
            arrayOf(
                "             xx                 ",
                "            xxxx                ",
                "           xxxxxx               ",
                "          xxxxxxxx              ",
                "       xxxxxxxxxxxxxxx          ",
                "           xxxxxxxxxxxx         ",
                "           xxxxxxxxxx           ",
                "      xxxxxxxxxxxxxxxx          ",
                "  xxxxxxxxxxxxxxxxxxxxxxxxxxxx  ",
                "xxxxxx   xxxxxxxxxxxx     xx    ",
                "xxxxxxx     xxx  xxxx          ",
                " xxxxx                          ",
            )
        const val OWL_X = 158
        const val OWL_ROW = 26

        /** Centre x, centre y, radius x, radius y, cycles in x, cycles in y, for each bat. */
        val BAT =
            floatArrayOf(
                108f,
                44f,
                62f,
                16f,
                1f,
                2f,
                108f,
                58f,
                50f,
                22f,
                1f,
                1f,
                124f,
                34f,
                70f,
                12f,
                1f,
                3f,
                96f,
                52f,
                34f,
                14f,
                2f,
                1f,
                140f,
                52f,
                40f,
                20f,
                1f,
                2f,
                72f,
                34f,
                52f,
                10f,
                1f,
                2f,
                152f,
                28f,
                36f,
                9f,
                2f,
                3f,
            )
    }
}

/**
 * A witch's cottage under a huge full moon, shaded cell by cell: a crooked cottage with lit windows and a smoking
 * chimney on a hill, a gnarled tree with a blinking owl, grinning pumpkins and a bubbling green cauldron, bats
 * flapping across the moon's face, thin clouds sliding over it and a witch on her broom crossing the sky with a
 * trail of sparks. The seed shapes the sky and the stars; every motion repeats in whole cycles of the loop.
 */
public fun witchMoonPiece(
    seed: Int = 0,
    magic: Rgb = Rgb.fromHex("#8cff4d"),
    window: Rgb = Rgb.fromHex("#ffa030"),
): AsciiPiece {
    val scene =
        WitchScene(
            seed,
            floatArrayOf(magic.red / 255f, magic.green / 255f, magic.blue / 255f),
            floatArrayOf(window.red / 255f, window.green / 255f, window.blue / 255f),
        )
    return asciiPiece(
        name = "witch moon",
        ground = Rgb.fromHex("#0a0614"),
        category = "scenes",
        cols = F_COLS,
        rows = F_ROWS,
        fps = 20,
        still = 5f,
        loop = LOOP,
        note = "a witch crosses a giant moon while bats flap, smoke curls and a green cauldron bubbles below",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A witch on her broom crossing a huge full moon above a crooked cottage, with bats, an owl and a bubbling cauldron.
 *
 * @param seed shapes the sky and the stars.
 * @param magic the color of the cauldron glow and the broom sparks.
 * @param window the color of the lit windows.
 */
@Composable
public fun AsciiWitchMoon(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    magic: Rgb = Rgb.fromHex("#8cff4d"),
    window: Rgb = Rgb.fromHex("#ffa030"),
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A witch flying across a huge moon above a cottage, bats circling in the night sky",
) {
    val piece = remember(seed, magic, window) { witchMoonPiece(seed, magic, window) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
