package com.jantursky.kascii.gallery.k

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.AsciiPieceView
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.Rng
import com.jantursky.kascii.core.asciiPiece
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 12f
private const val DROPS = 380
private const val CLOUD_BASE = 21f

/** One lightning bolt: its cells, glyphs and weights, and a precomputed glow field around it. */
private class Bolt(
    val start: Float,
    val topX: Float,
    val topY: Float,
    val cells: IntArray,
    val glyphs: CharArray,
    val weight: FloatArray,
    val glow: FloatArray,
    val strikesGround: Boolean,
)

private class Storm(
    seed: Int,
) {
    private val latA = KLattice(seed, 3)
    private val latB = KLattice(seed, 5)
    private val frame = KFrame()
    private val land = KFrame()
    private val palette: KPalette
    private val top = IntArray(KCOLS)
    private val farTop = IntArray(KCOLS)
    private val bolts: List<Bolt>
    private val dropX = FloatArray(DROPS)
    private val dropPhase = FloatArray(DROPS)
    private val dropRate = IntArray(DROPS)
    private val dropNear = BooleanArray(DROPS)
    private val window = kcol("#ffb347")
    private val flashCol = kcol("#cfc6ff")
    private val boltCol = kcol("#f4f0ff")
    private val houseX: Int

    init {
        val rng = kRng(seed, 1)
        val hillX = 52f + rng.float() * 30f
        for (x in 0 until KCOLS) {
            val h =
                40.5f + 1.6f * sin(x * 0.045f + seed) + 0.8f * sin(x * 0.13f + 2f) -
                    5.5f * kgauss(x.toFloat(), hillX, 26f) - 2.5f * kgauss(x.toFloat(), 168f, 30f)
            top[x] = h.toInt()
            farTop[x] = (36.5f + 1.8f * sin(x * 0.07f + seed * 0.7f) + 1.2f * sin(x * 0.023f + 1f)).toInt()
        }
        houseX = 136 + rng.int(16)
        buildLand(seed)
        val list = ArrayList<Bolt>()
        list.add(bolt(kRng(seed, 11), 1.0f, hillX + 2f, true))
        list.add(bolt(kRng(seed, 12), 4.7f, 118f + rng.float() * 20f, true))
        list.add(bolt(kRng(seed, 13), 8.2f, 26f + rng.float() * 30f, false))
        list.add(bolt(kRng(seed, 14), 10.3f, 160f + rng.float() * 18f, true))
        bolts = list
        val dr = kRng(seed, 21)
        for (d in 0 until DROPS) {
            dropX[d] = dr.float() * (KCOLS + 24f)
            dropPhase[d] = dr.float()
            dropNear[d] = dr.float() < 0.3f
            dropRate[d] = if (dropNear[d]) 9 + dr.int(3) else 6 + dr.int(3)
        }
        palette = KPalette(buildPalette())
    }

    private fun buildPalette(): List<KCol> =
        kramp(14, kcol("#05060c"), kcol("#151a28"), kcol("#2e3648"), kcol("#56607a"), kcol("#8e96ae")) +
            kramp(10, kcol("#1a1830"), kcol("#4a4380"), kcol("#8c84d0"), kcol("#d8d2ff"), kcol("#ffffff")) +
            kramp(6, kcol("#20283a"), kcol("#4a5a78"), kcol("#8ea2c4")) +
            kramp(6, kcol("#06070b"), kcol("#11131c"), kcol("#262838"), kcol("#40405a")) +
            kramp(6, kcol("#5a2a08"), kcol("#c8741c"), kcol("#ffb347"), kcol("#ffe0a0")) +
            kramp(6, kcol("#2a2e40"), kcol("#6c6890"), kcol("#b0a8e0")) +
            kramp(5, kcol("#0b0c14"), kcol("#141826"), kcol("#1e2234"))

    private fun buildLand(seed: Int) {
        val lat = KLattice(seed, 9)
        for (x in 0 until KCOLS) {
            for (y in top[x] until KROWS) {
                val n = lat.fbm(x * 0.2f, y * 0.5f, 2)
                val k = 0.03f + 0.012f * n - 0.01f * min(1f, (y - top[x]) / 8f)
                land.put(x, y, KCol(k * 0.9f, k, k * 1.3f), 2f)
            }
            // Grass tufts along the ridge.
            val g = lat.at(x * 0.9f, 3f)
            if (g > 0.55f) land.put(x, top[x] - 1, KCol(0.05f, 0.07f, 0.07f), 2f, if (g > 0.75f) '|' else ',')
        }
        // A farmhouse with one lit window.
        val base = top[houseX + 6]
        for (y in base - 5 until base + 1) {
            for (x in houseX until houseX + 13) {
                land.put(
                    x,
                    y,
                    KCol(0.035f, 0.035f, 0.05f),
                    2f,
                )
            }
        }
        for (k in 0 until 8) {
            for (x in houseX - 1 + k until houseX + 14 - k) {
                if (x >= houseX - 1 + k) land.put(x, base - 6 - k / 2, KCol(0.03f, 0.03f, 0.045f), 2f)
            }
        }
        for (x in houseX + 9..houseX + 10) for (y in base - 12..base - 9) land.put(x, y, KCol(0.03f, 0.03f, 0.045f), 2f)
        // A wind-bent tree on the far left hill.
        val tx = 18 + kRng(seed, 2).int(10)
        val tb = top[tx]
        for (y in tb - 6 until tb + 1) land.put(tx, y, KCol(0.04f, 0.04f, 0.05f), 2f)
        for (y in tb - 13 until tb - 4) {
            for (x in tx - 9 until tx + 8) {
                val dx = (x + 0.5f - tx + 1.5f) / 8f
                val dy = (y + 0.5f - (tb - 9f)) / 4.2f
                if (dx * dx + dy * dy + 0.35f * (lat.at(x * 0.7f, y * 0.9f) - 0.5f) < 1f) {
                    land.put(x, y, KCol(0.04f, 0.05f, 0.05f), 2f)
                }
            }
        }
    }

    private fun bolt(
        rng: Rng,
        start: Float,
        targetX: Float,
        ground: Boolean,
    ): Bolt {
        val px = ArrayList<Float>()
        val py = ArrayList<Int>()
        val pw = ArrayList<Float>()
        val topX = targetX + (rng.float() - 0.5f) * 30f
        val topY = 8 + rng.int(5)

        fun walk(
            x0: Float,
            y0: Int,
            length: Int,
            bias: Float,
            w: Float,
            branches: Boolean,
            goal: Float?,
        ) {
            var x = x0
            var y = y0
            var n = 0
            while (n < length && y < KROWS) {
                if (goal == null && y >= CLOUD_BASE + 8) break
                val ix = x.toInt()
                if (ix in 0 until KCOLS && y >= top[ix]) break
                px.add(x)
                py.add(y)
                pw.add(w)
                if (branches && rng.float() < 0.13f && n > 2) {
                    val dir = if (rng.float() < 0.5f) -1f else 1f
                    walk(x, y + 1, 5 + rng.int(12), dir * (0.9f + rng.float()), w * 0.55f, w > 0.7f, null)
                }
                var dx = (rng.float() - 0.5f) * 3.4f + bias
                if (goal != null) dx += (goal - x) / max(4f, (top[min(KCOLS - 1, max(0, ix))] - y).toFloat()) * 0.9f
                x += dx
                y += 1
                n++
            }
        }
        if (ground) {
            walk(topX, topY, 60, 0f, 1f, true, targetX)
        } else {
            // Cloud-to-cloud: a long sideways crawl along the cloud base with drooping fingers.
            var x = topX - 30f
            var y = CLOUD_BASE - 3f
            var step = 0
            while (x < topX + 40f) {
                px.add(x)
                py.add(y.toInt())
                pw.add(0.85f)
                if (rng.float() <
                    0.12f
                ) {
                    walk(x, y.toInt() + 1, 3 + rng.int(6), (rng.float() - 0.5f) * 2f, 0.5f, false, null)
                }
                x += 1.2f + rng.float() * 1.6f
                y += (rng.float() - 0.5f) * 1.6f
                y = min(CLOUD_BASE + 2f, max(CLOUD_BASE - 7f, y))
                step++
            }
        }
        val cells = ArrayList<Int>()
        val glyphs = ArrayList<Char>()
        val weight = ArrayList<Float>()
        for (k in px.indices) {
            val x = px[k]
            val y = py[k]
            // The next point of the same stroke is the following one when it sits a row lower.
            val next = if (k + 1 < px.size && py[k + 1] == y + 1) px[k + 1] else x
            val dx = next - x
            val g =
                if (!ground && (k + 1 >= px.size || py[k + 1] != y + 1)) {
                    '─'
                } else if (abs(dx) < 0.7f) {
                    '│'
                } else if (dx > 0f) {
                    '╲'
                } else {
                    '╱'
                }
            val x0 = x.toInt()
            val span = if (abs(dx) > 1.5f) (abs(dx) - 0.5f).toInt() else 0
            for (s in 0..span) {
                val cx = if (dx > 0f) x0 + s else x0 - s
                if (cx < 0 || cx >= KCOLS || y < 0 || y >= KROWS) continue
                cells.add(y * KCOLS + cx)
                glyphs.add(if (s > 0 && span > 1) '─' else g)
                weight.add(pw[k])
            }
        }
        val glow = FloatArray(KCELLS)
        val cs = cells.toIntArray()
        for (k in cs.indices) {
            val cx = cs[k] % KCOLS
            val cy = cs[k] / KCOLS
            val w = weight[k]
            for (y in max(0, cy - 6)..min(KROWS - 1, cy + 6)) {
                for (x in max(0, cx - 14)..min(KCOLS - 1, cx + 14)) {
                    val ddx = (x - cx).toFloat()
                    val ddy = (y - cy) * K_ROW_U
                    val v = w * exp(-sqrt(ddx * ddx + ddy * ddy) / 3.2f)
                    val i = y * KCOLS + x
                    if (v > glow[i]) glow[i] = v
                }
            }
        }
        return Bolt(start, topX, topY.toFloat(), cs, glyphs.toCharArray(), weight.toFloatArray(), glow, ground)
    }

    /** How bright a bolt is [age] seconds after it starts: three return strokes, then a fading afterglow. */
    private fun strength(age: Float): Float {
        if (age < 0f || age > 1.6f) return 0f
        var s = 0f
        for (p in floatArrayOf(0f, 0.09f, 0.24f)) if (age >= p) s = max(s, exp(-(age - p) / 0.045f))
        return max(s, 0.32f * exp(-age / 0.45f))
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = KTAU * t / LOOP
        val ages = FloatArray(bolts.size) { kLoopTime(t - bolts[it].start, LOOP) }
        val power = FloatArray(bolts.size) { strength(ages[it]) }
        var flash = 0f
        for (p in power) flash = max(flash, p)
        sky(phi, power, flash)
        // Land, lit from above by the flashes.
        for (i in 0 until KCELLS) {
            if (land.cov[i] <= 0f) continue
            val y = i / KCOLS
            val x = i % KCOLS
            val edge = if (y <= top[x]) 1f else exp(-(y - top[x]) / 2f)
            val k = flash * 0.2f * edge
            frame.set(
                i,
                land.r[i] + flashCol.r * k,
                land.g[i] + flashCol.g * k,
                land.b[i] + flashCol.b * k,
                land.cov[i],
                land.ch[i],
            )
        }
        windowGlow(t)
        rain(t, flash)
        for (b in bolts.indices) boltDraw(bolts[b], power[b], ages[b])
        frame.output(grid, palette)
    }

    private fun sky(
        phi: Float,
        power: FloatArray,
        flash: Float,
    ) {
        val ax = 3.2f * cos(phi)
        val ay = 1.6f * sin(phi)
        for (y in 0 until KROWS) {
            val vy = y * K_ROW_U
            val shelf = ksmooth(CLOUD_BASE + 6f, CLOUD_BASE - 8f, y.toFloat())
            val below = ksmooth(CLOUD_BASE - 2f, CLOUD_BASE + 6f, y.toFloat())
            for (x in 0 until KCOLS) {
                val i = y * KCOLS + x
                if (y >= top[x] - 1 && land.cov[i] > 0f) continue
                var r = 0.025f + 0.03f * y / KROWS
                var g = 0.03f + 0.035f * y / KROWS
                var b = 0.055f + 0.06f * y / KROWS
                var cov = 0.02f
                var lit = flash * 0.14f * (1f - 0.6f * y / KROWS.toFloat())
                if (y >= farTop[x]) {
                    // The far ridge, a shade lighter than the near hills.
                    val k = 0.055f + 0.02f * (y - farTop[x]) / 6f
                    r = k * 0.85f
                    g = k * 0.9f
                    b = k * 1.35f
                    cov = 2f
                    lit *= 1.6f
                } else if (shelf > 0.01f) {
                    val q = latB.fbm(x * 0.03f - ax * 0.7f, vy * 0.05f + ay * 0.7f, 3)
                    val d = latA.fbm(x * 0.021f + 1.6f * q + ax, vy * 0.034f + 1.3f * q + ay, 5)
                    val dens = ksmooth(0.4f, 0.74f, d * (0.8f + 0.3f * shelf) + 0.2f * shelf - 0.14f)
                    val bulge = ksmooth(0.45f, 0.8f, d)
                    val bb = bulge * bulge
                    val cr = 0.03f + 0.24f * bb
                    val cg = 0.035f + 0.26f * bb
                    val cb = 0.06f + 0.33f * bb
                    r += (cr - r) * dens
                    g += (cg - g) * dens
                    b += (cb - b) * dens
                    cov = if (dens < 0.55f) 0.02f + dens * 1.5f else 1.45f + (dens - 0.55f) * 1.25f
                    // Lightning lights the clouds from inside, strongest around where each bolt leaves the cloud.
                    for (k in bolts.indices) {
                        if (power[k] <= 0.01f) continue
                        val bx = x - bolts[k].topX
                        val by = (y - bolts[k].topY) * K_ROW_U
                        lit += power[k] * exp(-sqrt(bx * bx + by * by) / 26f) * (0.25f + 0.9f * dens) * (0.5f + bulge)
                    }
                } else {
                    // Grey rain shafts hanging below the cloud base.
                    val shaft = ksmooth(0.5f, 0.75f, latB.at(x * 0.06f + y * 0.03f + ax * 0.4f, 9f))
                    cov = 0.05f + 0.22f * shaft * below
                    r += 0.03f * shaft
                    g += 0.035f * shaft
                    b += 0.05f * shaft
                }
                r += flashCol.r * lit * 0.8f
                g += flashCol.g * lit * 0.8f
                b += flashCol.b * lit * 0.8f
                if (cov < 1.4f) cov += 0.25f * lit
                frame.set(i, r, g, b, cov)
            }
        }
    }

    private fun windowGlow(t: Float) {
        val base = top[houseX + 6]
        val wy = base - 3
        val flicker = 0.85f + 0.15f * sin(KTAU * 5f * t / LOOP) * sin(KTAU * 3f * t / LOOP)
        for (x in houseX + 3..houseX + 5) {
            frame.put(x, wy, window.scale(flicker), 2f)
            frame.put(x, wy - 1, window.scale(flicker * 0.8f), 2f)
        }
        for (y in max(0, wy - 5)..min(KROWS - 1, wy + 5)) {
            for (x in houseX - 12..houseX + 20) {
                if (x < 0 || x >= KCOLS) continue
                val i = y * KCOLS + x
                if (land.cov[i] > 0f) continue
                val dx = x - (houseX + 4f)
                val dy = (y - wy + 0.5f) * K_ROW_U
                val k = 0.35f * flicker * exp(-(dx * dx + dy * dy) / 60f)
                if (k > 0.02f) frame.light(i, window, k)
            }
        }
    }

    private fun rain(
        t: Float,
        flash: Float,
    ) {
        for (d in 0 until DROPS) {
            val u = kfract(dropPhase[d] + dropRate[d] * t / LOOP)
            val y0 = -4f + u * (KROWS + 6f)
            val x0 = dropX[d] - y0 * 0.45f
            val len = if (dropNear[d]) 3 else 2
            val bright = if (dropNear[d]) 0.42f else 0.26f
            for (k in 0 until len) {
                val y = (y0 - k).toInt()
                val x = (x0 + k * 0.45f).toInt()
                if (x < 0 || x >= KCOLS || y < 0 || y >= KROWS) continue
                val i = y * KCOLS + x
                if (land.cov[i] > 0f && !dropNear[d]) continue
                val f = bright * (1f - k * 0.25f) + flash * 0.45f
                val glyph = if (dropNear[d] || k == 0) '/' else '.'
                frame.set(i, 0.42f * f + 0.06f, 0.5f * f + 0.07f, 0.72f * f + 0.1f, 1f, glyph)
            }
        }
    }

    private fun boltDraw(
        b: Bolt,
        power: Float,
        age: Float,
    ) {
        if (power <= 0.02f) return
        val glow = flashCol
        for (y in 0 until KROWS) {
            for (x in 0 until KCOLS) {
                val i = y * KCOLS + x
                val g = b.glow[i]
                if (g < 0.03f) continue
                frame.light(i, glow, g * power * 0.8f)
            }
        }
        // The channel itself: white while the strokes fire, cooling to violet in the afterglow.
        val hot = ksmooth(0.25f, 0.9f, power)
        val core = KCol(0.55f, 0.45f, 0.95f).mix(boltCol, hot)
        for (k in b.cells.indices) {
            val w = b.weight[k]
            val v = min(1.2f, power * (0.6f + 0.6f * w) + 0.3f)
            if (age > 0.4f && w < 0.6f) continue
            frame.put(b.cells[k] % KCOLS, b.cells[k] / KCOLS, core.scale(v), 2f, b.glyphs[k])
        }
        // A splash of light where it hits the ground.
        if (b.strikesGround && b.cells.isNotEmpty()) {
            var last = 0
            for (k in b.cells.indices) if (b.cells[k] / KCOLS > b.cells[last] / KCOLS && b.weight[k] > 0.9f) last = k
            val cx = b.cells[last] % KCOLS
            val cy = b.cells[last] / KCOLS + 1
            for (y in max(0, cy - 3)..min(KROWS - 1, cy + 3)) {
                for (x in max(0, cx - 16)..min(KCOLS - 1, cx + 16)) {
                    val dx = (x - cx).toFloat()
                    val dy = (y - cy) * K_ROW_U
                    val k = power * 0.7f * exp(-(dx * dx + dy * dy) / 40f)
                    frame.light(y * KCOLS + x, boltCol, k)
                }
            }
        }
    }
}

/**
 * A night storm over dark hills: drifting thunderheads lit from inside, branching bolts that strike the ridge with
 * return strokes and a violet afterglow, a long cloud-to-cloud crawl, slanting rain and a farmhouse window. The seed
 * shapes the clouds, the bolts and the hills. Every motion repeats a whole number of times per loop.
 */
public fun lightningStormPiece(seed: Int = 0): AsciiPiece {
    val scene = Storm(seed)
    return asciiPiece(
        name = "lightning storm",
        ground = Rgb.fromHex("#04050a"),
        category = "effects",
        cols = KCOLS,
        rows = KROWS,
        fps = 20,
        still = 1.25f,
        loop = LOOP,
        note = "branching lightning over dark hills, clouds lit from within, rain and a lone lit window",
    ) { time, grid -> scene.draw(kLoopTime(time, LOOP), grid) }
}

/**
 * A night thunderstorm: branching lightning with afterglow strikes the hills, flashes light the clouds and rain slants
 * past a farmhouse window.
 *
 * @param seed shapes the clouds, the bolts and the hills.
 */
@Composable
public fun AsciiLightningStorm(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A night thunderstorm with branching lightning striking dark hills in the rain",
) {
    val piece = remember(seed) { lightningStormPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
