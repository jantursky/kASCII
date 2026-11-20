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
import com.jantursky.kascii.core.asciiPiece
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 20f
private const val CX = 98f
private const val LIP = 16f
private const val SEA = 36
private const val STARS = 170
private const val PUFFS = 40
private const val EMBERS = 240
private const val RIVERS = 10
private const val BOLTS = 3
private const val BOLT_ROWS = 12
private const val GRAVITY = 15f
private const val FLIGHT = 4f
private const val SPREAD = 38f

/** How far from the centre line the flank reaches on row [y]: the inverse of the cone profile. */
private fun reachAt(y: Float): Float {
    val p = ((SEA - y) / (SEA - LIP)).coerceIn(0.0001f, 1f)
    return max(7f, 7f - SPREAD * ln(0.03f + 0.97f * p))
}

private class VolcanoScene(
    seed: Int,
    lava: Rgb,
) {
    private val lat = ELattice(seed)
    private val lr = lava.red / 255f
    private val lg = lava.green / 255f
    private val lb = lava.blue / 255f
    private val palette: EPalette
    private val frame = EFrame()

    private val mountain = BooleanArray(E_CELLS)
    private val topCol = IntArray(E_COLS)
    private val rock = FloatArray(E_CELLS)
    private val glow = FloatArray(E_CELLS)
    private val lavaW = FloatArray(E_CELLS)
    private val lavaHeat = FloatArray(E_CELLS)
    private val lavaU = FloatArray(E_CELLS)
    private val lavaPh = FloatArray(E_CELLS)
    private val lavaCells: IntArray
    private val plume = FloatArray(E_CELLS)
    private val foreTop = IntArray(E_COLS)

    private val starCell = IntArray(STARS)
    private val starPhase = FloatArray(STARS)
    private val starRate = IntArray(STARS)
    private val starPower = FloatArray(STARS)

    private val emberDx = FloatArray(EMBERS)
    private val emberVx = FloatArray(EMBERS)
    private val emberVy = FloatArray(EMBERS)
    private val emberRate = IntArray(EMBERS)
    private val emberOff = FloatArray(EMBERS)
    private val emberPower = FloatArray(EMBERS)
    private val boltX = Array(BOLTS) { IntArray(BOLT_ROWS) }
    private val boltAt = floatArrayOf(2.6f, 8.9f, 14.4f)

    private val puffAmp = FloatArray(PUFFS)
    private val puffSeed = FloatArray(PUFFS)

    private fun top(xc: Float): Float {
        val dx = abs(xc - CX)
        val prof = if (dx <= 7f) 1f else (exp(-(dx - 7f) / SPREAD) - 0.03f) / 0.97f
        var y = SEA - (SEA - LIP) * max(0f, prof)
        if (dx < 6f) y += 1.9f * (1f - (dx / 6f) * (dx / 6f))
        if (dx > 7f) y += (lat.fbm(xc * 0.16f, 0.5f, 2) - 0.5f) * 3f * min(1f, (dx - 7f) / 12f)
        return y
    }

    init {
        val dark = ECol(0.02f, 0.01f, 0.012f)
        palette =
            EPalette(
                eRamp(
                    26,
                    ECol(0.1f, 0.012f, 0.01f),
                    ECol(0.45f, 0.05f, 0.02f),
                    ECol(0.9f, 0.32f, 0.04f),
                    ECol(1f, 0.7f, 0.18f),
                    ECol(1f, 0.93f, 0.62f),
                    ECol(1f, 1f, 1f),
                ) +
                    eRamp(12, ECol(0.012f, 0.008f, 0.012f), ECol(0.06f, 0.04f, 0.048f), ECol(0.2f, 0.1f, 0.07f)) +
                    eRamp(8, ECol(0.2f, 0.1f, 0.07f), ECol(0.45f, 0.2f, 0.1f)) +
                    eRamp(12, ECol(0.03f, 0.03f, 0.04f), ECol(0.14f, 0.12f, 0.14f), ECol(0.4f, 0.33f, 0.32f)) +
                    eRamp(10, ECol(0.08f, 0.04f, 0.04f), ECol(0.36f, 0.15f, 0.08f), ECol(0.75f, 0.32f, 0.12f)) +
                    eRamp(10, dark, ECol(0.05f, 0.03f, 0.1f), ECol(0.22f, 0.09f, 0.14f)) +
                    eRamp(10, ECol(0.05f, 0.07f, 0.18f), ECol(0.5f, 0.6f, 1f), ECol(1f, 1f, 1f)) +
                    eRamp(10, ECol(0.014f, 0.012f, 0.03f), ECol(0.06f, 0.05f, 0.12f), ECol(0.12f, 0.12f, 0.24f)) +
                    eRamp(6, ECol(0.3f, 0.3f, 0.45f), ECol(0.9f, 0.92f, 1f)) +
                    eRamp(10, ECol(0.035f, 0.025f, 0.035f), ECol(0.1f, 0.07f, 0.09f), ECol(0.2f, 0.13f, 0.14f)) +
                    eRamp(8, ECol(0.15f, 0.02f, 0.01f), ECol(0.95f, 0.22f, 0.04f)) +
                    eRamp(8, ECol(0.3f, 0.12f, 0.06f), ECol(0.95f, 0.5f, 0.2f)),
            )
        for (x in 0 until E_COLS) topCol[x] = top(x + 0.5f).toInt()
        for (y in 0 until SEA) {
            for (x in 0 until E_COLS) {
                val i = y * E_COLS + x
                if (y + 0.5f > top(x + 0.5f)) {
                    mountain[i] = true
                    val ridge = lat.at(x * 0.3f + 11f, y * 0.16f)
                    rock[i] = 0.6f + 0.7f * ridge * ridge + 0.5f * lat.at(x * 0.2f, y * 0.35f)
                }
            }
        }
        buildLava(seed)
        val cells = ArrayList<Int>()
        for (i in 0 until E_CELLS) if (lavaW[i] > 0.04f) cells.add(i)
        lavaCells = cells.toIntArray()
        for (i in lavaCells) {
            val w = lavaW[i] * lavaHeat[i]
            if (w < 0.25f) continue
            val x0 = i % E_COLS
            val y0 = i / E_COLS
            for (y in max(0, y0 - 4)..min(SEA - 1, y0 + 4)) {
                for (x in max(0, x0 - 5)..min(E_COLS - 1, x0 + 5)) {
                    val dx = (x - x0).toFloat()
                    val dy = (y - y0) * 1.6f
                    glow[y * E_COLS + x] += w * exp(-(dx * dx + dy * dy) / 5f) * 0.2f
                }
            }
        }
        for (s in 0 until STARS) {
            var x: Int
            var y: Int
            var n = 0
            do {
                x = (Noise.hash(s, n, 61, seed) * E_COLS).toInt()
                y = (Noise.hash(s, n, 62, seed) * 30f).toInt()
                n++
            } while (mountain[y * E_COLS + x] || (x - CX) * (x - CX) + (y - 14f) * (y - 14f) * 4f < 400f)
            starCell[s] = y * E_COLS + x
            starPhase[s] = Noise.hash(s, 0, 63, seed)
            starRate[s] = 1 + (Noise.hash(s, 0, 64, seed) * 3f).toInt()
            val q = Noise.hash(s, 0, 65, seed)
            starPower[s] = 0.3f + 0.7f * q * q
        }
        for (e in 0 until EMBERS) {
            val h = Noise.hash(e, 0, 71, seed)
            val side = if (Noise.hash(e, 0, 72, seed) < 0.5f) -1f else 1f
            val sp = Noise.hash(e, 0, 73, seed)
            emberDx[e] = (Noise.hash(e, 0, 74, seed) - 0.5f) * 8f
            emberVx[e] = side * (6f + 30f * sp)
            val apex = 5f + 15f * h
            emberVy[e] = sqrt(2f * GRAVITY * apex)
            emberRate[e] = 3 + (Noise.hash(e, 0, 75, seed) * 3f).toInt().coerceAtMost(2)
            emberOff[e] = Noise.hash(e, 0, 76, seed)
            emberPower[e] = Noise.hash(e, 0, 77, seed)
        }
        for (k in 0 until BOLTS) {
            var x = (CX - 12f + 26f * Noise.hash(k, 0, 81, seed)).toInt()
            for (r in 0 until BOLT_ROWS) {
                boltX[k][r] = x
                x += (Noise.hash(k, r, 82, seed) * 3f).toInt() - 1
            }
        }
        for (p in 0 until PUFFS) {
            puffAmp[p] = 0.7f + 0.6f * Noise.hash(p, 0, 83, seed)
            puffSeed[p] = 20f * Noise.hash(p, 0, 84, seed)
        }
        for (x in 0 until E_COLS) {
            val left = 48.5f - 6f * exp(-(x / 24f) * (x / 24f)) - 0.8f * sin(x * 0.5f)
            val right = 48.5f - 5f * exp(-((E_COLS - x) / 30f) * ((E_COLS - x) / 30f)) - 0.8f * sin(x * 0.45f + 1f)
            foreTop[x] = min(left, right).roundToInt().coerceIn(40, E_ROWS)
        }
    }

    private fun buildLava(seed: Int) {
        val sf = FloatArray(RIVERS)
        val sf1 = FloatArray(RIVERS)
        val y0 = FloatArray(RIVERS)
        val y1 = FloatArray(RIVERS)
        val amp = FloatArray(RIVERS)
        val freq = FloatArray(RIVERS)
        val ph = FloatArray(RIVERS)
        val width = FloatArray(RIVERS)
        val mains = 6
        val fan = floatArrayOf(-0.8f, -0.45f, -0.12f, 0.16f, 0.5f, 0.85f)
        for (r in 0 until RIVERS) {
            val parent = if (r < mains) r else (r * 3 + 1) % mains
            sf[r] = fan[parent] + (Noise.hash(r, 0, 51, seed) - 0.5f) * 0.08f
            amp[r] = 1.2f + 1.8f * Noise.hash(parent, 1, 51, seed)
            freq[r] = 0.3f + 0.3f * Noise.hash(parent, 2, 51, seed)
            ph[r] = E_TAU * Noise.hash(parent, 3, 51, seed)
            width[r] = 0.45f + 0.4f * Noise.hash(r, 4, 51, seed)
            if (r < mains) {
                y0[r] = LIP + 1f
                y1[r] = if (r % 2 == 1) 29f + 4f * Noise.hash(r, 5, 51, seed) else SEA.toFloat() + 1f
                sf1[r] = sf[r]
            } else {
                y0[r] = LIP + 7f + 11f * Noise.hash(r, 6, 51, seed)
                y1[r] = SEA.toFloat() + 1f
                val dir = if (Noise.hash(r, 7, 51, seed) < 0.5f) -1f else 1f
                sf1[r] = sf[r] + dir * (0.12f + 0.12f * Noise.hash(r, 8, 51, seed))
            }
        }
        for (r in 0 until RIVERS) {
            var y = y0[r]
            while (y < y1[r]) {
                val k = eSmooth(y0[r], y1[r], y)
                val s = sf[r] + (sf1[r] - sf[r]) * k
                val cx = CX + s * (reachAt(y) - 5f) + amp[r] * sin(freq[r] * y + ph[r]) * (0.3f + k)
                val w = width[r] + (y - LIP) * 0.03f
                val along = (y - y0[r]) / (y1[r] - y0[r])
                val down = eSmooth(0f, 1f, (y - LIP) / (SEA - LIP))
                val heat = (1f - 0.62f * down) * (1f - 0.7f * eSmooth(0.7f, 1f, along))
                val row = y.toInt()
                if (row in 0 until SEA) {
                    for (x in (cx - 3f * w).toInt()..(cx + 3f * w).toInt() + 1) {
                        if (x !in 0 until E_COLS) continue
                        val i = row * E_COLS + x
                        if (!mountain[i]) continue
                        val d = (x + 0.5f - cx) / w
                        val wt = exp(-d * d)
                        if (wt > lavaW[i]) {
                            lavaW[i] = wt
                            lavaHeat[i] = heat
                            lavaU[i] = y - LIP
                            lavaPh[i] = ph[r]
                        }
                    }
                }
                y += 0.5f
            }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = E_TAU * t / LOOP
        val f = frame
        f.clear()
        val flick = 0.82f + 0.1f * sin(phi * 5f + 0.6f) + 0.08f * sin(phi * 13f + 2f)
        val flash = flash(t)
        sky(f, flick, flash)
        mountain(f, flick, flash, phi)
        plume(f, t, flick, flash, phi)
        stars(f, t)
        sea(f, phi, flick)
        shore(f, flick)
        eOutput(f, grid, palette)
        bolts(grid, t)
        embers(grid, t, flick)
    }

    /** Lightning brightness 0 to 1: a quick double flicker at three moments of the loop. */
    private fun flash(t: Float): Float {
        var best = 0f
        for (k in 0 until BOLTS) {
            var tau = t - boltAt[k]
            if (tau < 0f) tau += LOOP
            if (tau < 0.8f) {
                val v = max(exp(-tau * 8f), if (tau > 0.2f) 0.85f * exp(-(tau - 0.2f) * 10f) else 0f)
                if (v > best) best = v
            }
        }
        return best
    }

    private fun sky(
        f: EFrame,
        flick: Float,
        flash: Float,
    ) {
        for (y in 0 until SEA) {
            for (x in 0 until E_COLS) {
                val i = y * E_COLS + x
                if (mountain[i]) continue
                val dx = x + 0.5f - CX
                val dy = (y + 0.5f - LIP) * E_ASPECT
                val d = sqrt(dx * dx + dy * dy)
                val g = exp(-d / 32f) * flick
                val horizon = exp(-(SEA - y) / 3f) * 0.12f
                f.add(
                    i,
                    0.012f + lr * g * 0.55f + horizon * 0.5f + flash * 0.05f,
                    0.01f + lg * g * 0.2f + horizon * 0.15f + flash * 0.06f,
                    0.03f + lb * g * 0.08f + horizon * 0.1f + flash * 0.12f,
                    g * 1.5f + horizon * 1.5f,
                )
            }
        }
    }

    private fun mountain(
        f: EFrame,
        flick: Float,
        flash: Float,
        phi: Float,
    ) {
        for (y in 0 until SEA) {
            for (x in 0 until E_COLS) {
                val i = y * E_COLS + x
                if (!mountain[i]) continue
                val dx = x + 0.5f - CX
                val dy = (y + 0.5f - LIP) * E_ASPECT
                val d = sqrt(dx * dx + dy * dy)
                val crown = exp(-d / 17f) * flick
                val k = rock[i]
                val gl = glow[i] * flick
                f.set(
                    i,
                    0.06f * k + lr * (gl * 0.6f + crown * 0.3f) + flash * 0.05f,
                    0.042f * k + lg * (gl * 0.3f + crown * 0.12f) + flash * 0.06f,
                    0.055f * k + lb * (gl * 0.1f + crown * 0.04f) + flash * 0.11f,
                    if (rock[i] > 1.1f) 1.97f else 1.9f,
                )
            }
        }
        // The molten crater, flickering, and the rivers of lava running down the flanks.
        for (y in (LIP - 2f).toInt()..(LIP + 3f).toInt()) {
            for (x in (CX - 8f).toInt()..(CX + 8f).toInt()) {
                val i = y * E_COLS + x
                if (!mountain[i]) continue
                val dx = abs(x + 0.5f - CX)
                if (dx > 6.2f) continue
                val n = lat.at(x * 0.5f + 3f * cos(phi * 2f), y * 0.8f + 3f * sin(phi * 2f))
                val heat = eClamp(0.55f + 0.6f * n + 0.25f * flick - dx * 0.05f - (LIP + 2.2f - y) * 0.0f)
                fire(f, i, heat, 1.97f)
            }
        }
        for (i in lavaCells) {
            val w = lavaW[i]
            val flow = 0.7f + 0.3f * sin(E_TAU * (lavaU[i] * 0.05f - 3f * (phi / E_TAU)) + lavaPh[i])
            val x = i % E_COLS
            val y = i / E_COLS
            val crust = 0.8f + 0.4f * lat.at(x * 0.35f + 5f * cos(phi), y * 0.6f + 5f * sin(phi))
            val heat = w * lavaHeat[i] * flow * crust * (0.85f + 0.3f * flick)
            if (heat < 0.07f) continue
            if (w > 0.45f) {
                fire(f, i, heat * 1.0f + 0.05f, 1.97f)
            } else {
                val q = eClamp(heat * 1.3f)
                f.r[i] += lr * q * 1.0f
                f.g[i] += lg * q * q * 0.8f
                f.b[i] += lb * q * q * q * 0.3f
                if (f.cov[i] < 1.5f) f.cov[i] = 1.6f
            }
        }
    }

    /** Writes the colour of molten rock at [heat] (0 to 1.2) into cell [i]. */
    private fun fire(
        f: EFrame,
        i: Int,
        heat: Float,
        cover: Float,
    ) {
        val h = heat.coerceIn(0f, 1.25f)
        f.set(i, eClamp(h * 1.6f), eClamp(h * h * 1.35f - 0.04f), eClamp(h * h * h * 0.95f - 0.12f), cover)
    }

    private fun plume(
        f: EFrame,
        t: Float,
        flick: Float,
        flash: Float,
        phi: Float,
    ) {
        for (y in 0..LIP.toInt() + 2) {
            for (x in 0 until E_COLS) plume[y * E_COLS + x] = 0f
        }
        val churn = phi * 2f
        for (p in 0 until PUFFS) {
            val age = eFract(2f * t / LOOP + p / PUFFS.toFloat())
            val rise = age * (LIP + 5f)
            val cy = LIP - 1f - rise
            val cx = CX + 30f * age * age + 2f * sin(E_TAU * age * 1.5f + puffSeed[p])
            val rad = (6f + age * 18f + 15f * age * age) * puffAmp[p]
            val dens = eSmooth(0f, 0.07f, age) * (1f - eSmooth(0.7f, 1f, age)) * 1.7f
            val ry = rad / E_ASPECT
            val ax = cos(churn + puffSeed[p]) * 1.8f
            val ay = sin(churn + puffSeed[p]) * 1.8f
            for (y in max(0, (cy - ry).toInt())..min(LIP.toInt() + 2, (cy + ry).toInt() + 1)) {
                for (x in max(0, (cx - rad).toInt())..min(E_COLS - 1, (cx + rad).toInt() + 1)) {
                    val dx = x + 0.5f - cx
                    val dy = (y + 0.5f - cy) * E_ASPECT
                    val q = 1f - (dx * dx + dy * dy) / (rad * rad)
                    if (q <= 0f) continue
                    val n = lat.at(x * 0.28f + puffSeed[p] + ax, y * 0.6f + ay)
                    plume[y * E_COLS + x] += q * (0.55f + 1.2f * n) * dens * 1.7f
                }
            }
        }
        for (y in 0..LIP.toInt() + 2) {
            for (x in 0 until E_COLS) {
                val i = y * E_COLS + x
                if (mountain[i]) continue
                val d = plume[i]
                if (d < 0.1f) continue
                val dx = x + 0.5f - CX
                val dy = (y + 0.5f - LIP) * E_ASPECT
                val dist = sqrt(dx * dx + dy * dy)
                val under = (0.5f * exp(-dist / 26f) + 0.9f * exp(-(LIP - y) / 8f)) * flick
                val tex = 0.55f + 0.7f * lat.at(x * 0.3f + 40f, y * 0.7f + 2f * sin(phi))
                val lit = min(0.8f, under * 0.55f * (0.6f + 0.8f * tex))
                val bolt = flash * exp(-((x - CX - 4f) * (x - CX - 4f)) / 700f) * eSmooth(0f, 6f, 20f - y)
                val dd = eClamp(d)
                f.set(
                    i,
                    0.13f * tex + lr * lit * 0.9f * dd + bolt * 0.5f + f.r[i] * (1f - dd),
                    0.1f * tex + lg * lit * 0.36f * dd + bolt * 0.6f + f.g[i] * (1f - dd),
                    0.125f * tex + lb * lit * 0.1f * dd + bolt * 1f + f.b[i] * (1f - dd),
                    max(f.cov[i], min(1.97f, d * 1.05f)),
                )
            }
        }
    }

    private fun stars(
        f: EFrame,
        t: Float,
    ) {
        for (s in 0 until STARS) {
            val i = starCell[s]
            if (f.cov[i] > 0.5f) continue
            val tw = (0.55f + 0.45f * sin(E_TAU * (starRate[s] * t / LOOP + starPhase[s]))) * starPower[s]
            if (tw < 0.22f) continue
            f.set(i, 0.55f + 0.4f * tw, 0.55f + 0.4f * tw, 0.8f + 0.2f * tw, 0.9f)
        }
    }

    private fun sea(
        f: EFrame,
        phi: Float,
        flick: Float,
    ) {
        for (y in SEA until E_ROWS) {
            val d = (y - SEA).toFloat()
            val wobble = 0.4f + 0.9f * d / (E_ROWS - SEA - 1f)
            val ys = max(0, (SEA - 1 - d * 1.25f).toInt())
            for (x in 0 until E_COLS) {
                val i = y * E_COLS + x
                val sx = (x + wobble * sin(0.85f * y + 0.21f * x + 2f * phi)).roundToInt().coerceIn(0, E_COLS - 1)
                val j = ys * E_COLS + sx
                val shimmer = 0.8f + 0.2f * sin(1.9f * y + 3f * phi + 0.15f * x)
                val w = (0.9f - 0.03f * d) * shimmer
                f.set(
                    i,
                    0.012f + f.r[j] * w * 1.1f,
                    0.011f + f.g[j] * w * 1.0f,
                    0.03f + f.b[j] * w * 1.0f,
                    min(1.2f, 0.06f + min(1f, f.cov[j] * 0.9f + 0.05f) * w),
                )
            }
        }
    }

    /** A low dark headland in front, rimmed by the red light of the lava. */
    private fun shore(
        f: EFrame,
        flick: Float,
    ) {
        for (x in 0 until E_COLS) {
            val t0 = foreTop[x]
            if (t0 >= E_ROWS) continue
            val reflect = exp(-abs(x - CX) / 45f) * flick
            for (y in t0 until E_ROWS) {
                val i = y * E_COLS + x
                if (y == t0) {
                    f.set(i, 0.1f + 0.4f * lr * reflect, 0.03f + 0.08f * lg * reflect, 0.02f, 1.97f)
                } else {
                    f.set(i, 0.008f, 0.006f, 0.01f, 1.97f)
                }
            }
        }
    }

    private fun bolts(
        grid: Grid,
        t: Float,
    ) {
        val fl = flash(t)
        if (fl < 0.15f) return
        for (k in 0 until BOLTS) {
            var tau = t - boltAt[k]
            if (tau < 0f) tau += LOOP
            if (tau >= 0.8f) continue
            val xs = boltX[k]
            for (r in 0 until BOLT_ROWS) {
                val y = 2 + r
                val glyph =
                    if (r + 1 < BOLT_ROWS && xs[r + 1] > xs[r]) {
                        '\\'
                    } else if (r + 1 < BOLT_ROWS && xs[r + 1] < xs[r]) {
                        '/'
                    } else {
                        '|'
                    }
                ePut(grid, palette, xs[r], y, glyph, 0.8f + 0.2f * fl, 0.85f + 0.15f * fl, 1f)
            }
        }
    }

    private fun embers(
        grid: Grid,
        t: Float,
        flick: Float,
    ) {
        val burst = 0.86f + 0.14f * sin(E_TAU * (2f * t / LOOP) + 0.5f)
        for (e in 0 until EMBERS) {
            if (emberPower[e] > burst) continue
            val u = eFract(emberRate[e] * t / LOOP + emberOff[e])
            val s = u * FLIGHT
            for (j in 0..4) {
                val sj = s - j * 0.07f
                if (sj < 0f) break
                val x = CX + emberDx[e] + emberVx[e] * sj
                val y = LIP - emberVy[e] * sj + 0.5f * GRAVITY * sj * sj
                val xi = x.toInt()
                val yi = y.toInt()
                if (x < 0f || xi >= E_COLS || y < 0f || yi >= E_ROWS) continue
                if (yi >= topCol[xi] && yi >= LIP.toInt() + 2) break
                val heat = eClamp(1.15f - sj / 3.6f - j * 0.09f)
                val glyph =
                    when (j) {
                        0 -> if (heat > 0.55f) '●' else '•'
                        1, 2 -> if (heat > 0.45f) '•' else '·'
                        else -> '·'
                    }
                ePut(
                    grid,
                    palette,
                    xi,
                    yi,
                    glyph,
                    eClamp(heat * 1.6f),
                    eClamp(heat * heat * 1.35f - 0.04f),
                    eClamp(heat * heat * heat - 0.15f),
                )
            }
        }
    }
}

/**
 * A volcano erupting at night: a dark cone with a molten crater, rivers of lava running down its flanks, an ash
 * plume lit from below and cut by lightning, and a fountain of glowing embers that arcs out and falls back, all under
 * stars and over a calm sea that mirrors the glow. Every cell is computed from its position and the time, shaded in
 * its own colour and drawn as a four level dithered halftone or a solid block. The seed shapes the flanks, the lava
 * rivers, the embers and the stars. Every motion completes a whole number of cycles per loop.
 */
public fun volcanoNightPiece(
    seed: Int = 0,
    lava: Rgb = Rgb.fromHex("#ff6a14"),
): AsciiPiece {
    val scene = VolcanoScene(seed, lava)
    return asciiPiece(
        name = "volcano night",
        ground = Rgb.fromHex("#0c0608"),
        category = "scenes",
        cols = E_COLS,
        rows = E_ROWS,
        fps = 15,
        still = 5f,
        loop = LOOP,
        note = "a volcano erupts at night, lava runs down its flanks and embers arc over a calm sea",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A volcano erupting at night: glowing lava rivers, an ash plume with lightning, a fountain of embers, stars and a
 * calm sea reflecting the glow.
 *
 * @param seed shapes the flanks, the lava rivers, the embers and the stars.
 * @param lava the main color of the lava glow.
 */
@Composable
public fun AsciiVolcanoNight(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    lava: Rgb = Rgb.fromHex("#ff6a14"),
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A volcano erupting at night, with lava on its flanks and embers in the air",
) {
    val piece = remember(seed, lava) { volcanoNightPiece(seed, lava) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
