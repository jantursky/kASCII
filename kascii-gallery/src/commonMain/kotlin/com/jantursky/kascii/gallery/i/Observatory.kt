package com.jantursky.kascii.gallery.i

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
import com.jantursky.kascii.gallery.loopTime
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 30f
private const val POLE_X = 150f
private const val POLE_Y = 4f
private const val TRAILS = 150
private const val SYM = 6
private const val DOME_X = 74f
private const val DOME_Y = 27f
private const val DOME_RX = 22f
private const val DOME_RY = 10.5f
private const val TRAIL_ARC = 0.42f

private class Observatory(
    seed: Int,
) {
    private val lat = ILattice(seed, 31)
    private val base = IFrame()
    private val land = IFrame()
    private val frame = IFrame()
    private val palette: IPalette
    private val hill = IntArray(ICOLS)
    private val trailR = FloatArray(TRAILS)
    private val trailA = FloatArray(TRAILS)
    private val trailMag = FloatArray(TRAILS)
    private val trailTint = FloatArray(TRAILS)
    private val silver = col("#b8c4dc")
    private val silverDark = col("#1c2236")
    private val warm = col("#ffb860")
    private val red = col("#ff4a3a")
    private val sodium = col("#ffb030")

    init {
        for (x in 0 until ICOLS) {
            val n = lat.fbm(x * 0.04f, 3f, 3)
            hill[x] =
                (
                    41f - 14.5f *
                        gauss(
                            x.toFloat(),
                            DOME_X,
                            46f,
                        ) - 3f * (n - 0.5f) - 2f * gauss(x.toFloat(), 175f, 18f)
                ).toInt()
        }
        val rng = irng(seed, 33)
        for (s in 0 until TRAILS) {
            trailR[s] = 3f + 178f * sqrt(rng.float())
            trailA[s] = rng.float() * ITAU / SYM
            trailMag[s] = rng.float()
            trailTint[s] = rng.float()
        }
        buildSky(seed)
        buildLand(seed)
        palette =
            IPalette(
                ramp(10, col("#03040c"), col("#0c1430"), col("#1e2c5c"), col("#3a4c88")) +
                    ramp(5, col("#2a1e4a"), col("#4a3a7a"), col("#7a6aa8")) +
                    ramp(8, col("#2a2e3e"), col("#6a7088"), col("#b4bccc")) +
                    tones(col("#9ab8ff"), 7) +
                    tones(col("#ffd9a0"), 6) +
                    ramp(5, col("#c8d4ff"), col("#ffffff")) +
                    tones(silver, 9) +
                    tones(col("#1e3a3a"), 5) +
                    tones(warm, 7) +
                    tones(red, 6) +
                    tones(sodium, 6) +
                    listOf(col("#ffffff")),
            )
    }

    private fun buildSky(seed: Int) {
        val n = ILattice(seed, 37)
        for (y in 0 until IROWS) {
            for (x in 0 until ICOLS) {
                val i = y * ICOLS + x
                val h = clamp01((y - 8f) / 30f)
                // A Milky Way band arching across, faint and grainy.
                val band = gauss(y - 30f + x * 0.16f, 0f, 6.5f)
                val dust = smooth(0.42f, 0.7f, n.fbm(x * 0.05f, y * 0.14f, 4))
                val mw = band * (0.3f + 0.7f * dust) * 0.55f
                val glow = 0f * h
                val c = col("#1e2c5c").mix(col("#4a3a7a"), 0.5f * h).mix(col("#c8d4ff"), mw * 0.5f)
                base.r[i] = c.r
                base.g[i] = c.g
                base.b[i] = c.b
                base.cov[i] = glow + mw
            }
        }
        // Distant town glow on the right horizon.
        base.glow(166f, 37f, 26f, Col(0.5f, 0.32f, 0.2f), 0.4f)
    }

    private fun buildLand(seed: Int) {
        for (x in 0 until ICOLS) {
            for (y in hill[x] until IROWS) {
                val k = 0.06f + 0.035f * lat.at(x * 0.15f, y * 0.4f) + 0.02f * clamp01((y - hill[x]) / 6f)
                land.put(x, y, Col(k * 0.8f, k * 1.05f, k * 1.4f), if (y == hill[x]) 1.75f else 2f)
            }
        }
        // Pines along the slopes.
        val rng = irng(seed, 35)
        for (p in 0 until 30) {
            val x = rng.int(ICOLS)
            if (abs(x - DOME_X) < 26f) continue
            val h = 3 + rng.int(5)
            val top = hill[x] - h
            for (y in top until hill[x] + 1) {
                val w = ((y - top) * 1.1f).toInt() / 2
                for (dx in -w..w) {
                    val k = 0.06f + 0.03f * (if (dx < 0) 1f else 0f)
                    land.put(
                        x + dx,
                        y,
                        Col(k * 0.6f, k * 1.2f, k * 1.2f),
                        2f,
                        if (dx == -w &&
                            w > 0
                        ) {
                            '▟'
                        } else if (dx == w && w > 0) {
                            '▙'
                        } else {
                            NONE
                        },
                    )
                }
            }
        }
        // Town lights on the far right.
        for (k in 0 until 40) {
            val x = 140 + (lat.at(k * 3.1f, 9f) * 52f).toInt()
            val y = hill[min(ICOLS - 1, x)] + 1 + (lat.at(k * 1.7f, 2f) * 3f).toInt()
            land.put(x, y, warm.scale(0.6f + 0.4f * lat.at(k * 5f, 1f)), 1f, if (k % 3 == 0) '•' else '·')
        }
        // The building drum.
        val by0 = DOME_Y.toInt()
        for (y in by0..by0 + 9) {
            for (x in (DOME_X - DOME_RX + 1).toInt()..(DOME_X + DOME_RX - 1).toInt()) {
                val a = (x + 0.5f - DOME_X) / DOME_RX
                val k = 0.55f - 0.35f * a - 0.03f * (y - by0)
                land.put(x, y, col("#4a5068").mix(col("#9aa4bc"), clamp01(k)), 2f)
            }
        }
        // Door and a little lit window.
        for (y in by0 + 5..by0 + 9) for (x in 78..80) land.put(x, y, warm.scale(0.85f), 2f)
        land.put(60, by0 + 3, warm.scale(0.7f), 2f, '▄')
        land.put(61, by0 + 3, warm.scale(0.7f), 2f, '▄')
        // A path down the hill from the door.
        for (y in by0 + 10 until IROWS) {
            val x = (79 + (y - by0 - 10) * 1.4f).toInt()
            for (dx in 0..(1 + (y - by0 - 10) / 3)) land.put(x + dx, y, Col(0.2f, 0.22f, 0.3f), 1.75f)
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val u = t / LOOP
        frame.copyFrom(base)
        trails(u)
        meteor(t)
        val open = smooth(1f, 6f, t) * (1f - smooth(23f, 28.5f, t))
        laser(t, open)
        frame.over(land)
        dome(t, open)
        frame.glow(79.5f, DOME_Y + 7f, 5f, warm, 0.35f)
        frame.output(grid, palette)
    }

    /** Star trails around the celestial pole, the heads turning a full circle per loop. */
    private fun trails(u: Float) {
        val turn = ITAU * u / SYM
        for (n in 0 until TRAILS * SYM) {
            val s = n / SYM
            val r = trailR[s]
            val head = trailA[s] + turn + ITAU * (n % SYM) / SYM
            if (!visible(r, head)) continue
            val steps = max(4, (r * TRAIL_ARC).toInt())
            val m = 0.35f + 0.65f * trailMag[s] * trailMag[s]
            val tint =
                if (trailTint[s] < 0.25f) {
                    col("#ffd9a0")
                } else if (trailTint[s] > 0.8f) {
                    col("#9ab8ff")
                } else {
                    col("#c8d4ff")
                }
            for (k in 0..steps) {
                val a = head - TRAIL_ARC * k / steps
                val x = POLE_X + r * cos(a)
                val y = POLE_Y + r * sin(a) / ROW_U
                val xi = x.toInt()
                val yi = y.toInt()
                if (xi < 0 || xi >= ICOLS || yi < 0 || yi >= IROWS - 6) continue
                val f = (1f - k.toFloat() / steps).let { it * sqrt(it) } * m
                val i = yi * ICOLS + xi
                if (f < 0.14f || (frame.ch[i] != NONE && frame.cov[i] > f)) continue
                val glyph =
                    if (k == 0) {
                        if (m > 0.75f) '*' else '+'
                    } else if (f > 0.45f) {
                        '•'
                    } else {
                        '·'
                    }
                frame.put(xi, yi, tint.scale(0.5f + 0.5f * f), max(0.5f, f), glyph)
            }
        }
        frame.put(POLE_X.toInt(), POLE_Y.toInt(), Col(1f, 1f, 1f), 1f, '*')
        frame.glow(POLE_X + 0.5f, POLE_Y + 0.5f, 2.5f, col("#c8d4ff"), 0.4f)
    }

    /** Whether any part of a trail of radius [r] ending at angle [head] can fall inside the frame. */
    private fun visible(
        r: Float,
        head: Float,
    ): Boolean {
        for (k in 0..4) {
            val a = head - TRAIL_ARC * k / 4f
            val x = POLE_X + r * cos(a)
            val y = POLE_Y + r * sin(a) / ROW_U
            if (x > -4f && x < ICOLS + 4f && y > -3f && y < IROWS - 4f) return true
        }
        return false
    }

    private fun meteor(t: Float) {
        val s = (t - 14f) / 0.9f
        if (s < 0f || s > 1.4f) return
        for (k in 0 until 16) {
            val p = s - k * 0.035f
            if (p < 0f || p > 1f) continue
            val x = 118f - 70f * p
            val y = 2f + 16f * p
            val f = (1f - k / 16f) * (if (s > 1f) 1f - (s - 1f) / 0.4f else 1f)
            if (f <= 0f) continue
            frame.put(
                x.toInt(),
                y.toInt(),
                Col(0.6f + 0.4f * f, 0.85f + 0.15f * f, 0.75f + 0.25f * f),
                1f,
                if (k ==
                    0
                ) {
                    '*'
                } else {
                    '╱'
                },
            )
            if (k == 0) frame.glow(x, y, 3f, col("#c8ffe0"), 0.5f * f)
        }
    }

    /** A sodium laser guide star beam rising from the slit while the dome is fully open. */
    private fun laser(
        t: Float,
        open: Float,
    ) {
        val on = smooth(8f, 9f, t) * (1f - smooth(20f, 21f, t)) * open
        if (on <= 0.01f) return
        val x0 = DOME_X + 4f
        val y0 = DOME_Y - 6f
        val dx = 0.75f
        for (y in y0.toInt() downTo 0) {
            val p = (y0 - y)
            val x = x0 + dx * p * ROW_U / 2.2f * 1.4f
            val f = on * (0.9f - 0.5f * p / y0)
            val i = y * ICOLS + x.toInt()
            if (x.toInt() >= ICOLS) break
            frame.put(x.toInt(), y, sodium.scale(0.5f + 0.5f * f), max(0.4f, f), '╱')
            if (i + 1 < ICELLS) frame.light(i + 1, sodium, 0.15f * f)
        }
        val tipX = x0 + dx * y0 * 1.4f
        frame.glow(tipX, 0.5f, 3f, sodium, 0.5f * on)
    }

    private fun dome(
        t: Float,
        open: Float,
    ) {
        val half = 0.03f + 0.2f * open
        val slitC = 0.12f
        val aim = 0.35f + 0.12f * sin(ITAU * (t - 6f) / 34f)
        for (y in (DOME_Y - DOME_RY).toInt() - 1..DOME_Y.toInt()) {
            for (x in (DOME_X - DOME_RX).toInt() - 1..(DOME_X + DOME_RX).toInt() + 1) {
                val a = (x + 0.5f - DOME_X) / DOME_RX
                val b = (y + 0.5f - DOME_Y) / DOME_RY
                val d2 = a * a + b * b
                if (d2 > 1f || b > 0.05f) continue
                val nz = sqrt(1f - min(1f, d2))
                // Lit from above and behind by the sky, catching the town glow on the right.
                val k = clamp01(0.35f - 0.25f * a - 0.55f * b + 0.2f * nz)
                var c = silverDark.mix(silver, k)
                c = c.mix(warm, 0.15f * clamp01(a))
                var glyph = NONE
                val inSlit = abs(a - slitC * (1f + b)) < half * (0.4f + 0.6f * nz) && b < -0.08f
                if (inSlit) {
                    // Inside: dim red light and the telescope tube.
                    val ty = (y + 0.5f - (DOME_Y - 2f)) * ROW_U
                    val tx = x + 0.5f - (DOME_X + 1f)
                    val along = tx * aim - ty * (1f - aim)
                    val across = abs(tx * (1f - aim) + ty * aim)
                    c = Col(0.16f, 0.04f, 0.05f).mix(red.scale(0.5f), 0.35f * nz)
                    if (across < 2.4f && along > -2f && along < 20f) {
                        c = silver.mix(Col(1f, 1f, 1f), clamp01(0.4f - across * 0.15f))
                        if (along > 16f) c = col("#3a4060")
                    }
                } else if (abs(abs(a - slitC * (1f + b)) - half * (0.4f + 0.6f * nz)) < 0.07f && b < -0.08f) {
                    glyph = '│'
                    c = c.scale(0.7f)
                }
                // Ribs.
                if (!inSlit && fract(b * 4f) < 0.12f && b < -0.15f) c = c.scale(0.85f)
                frame.put(x, y, c, if (d2 > 0.92f) 1.8f else 2f, glyph)
            }
        }
        // The shutter halves stack on the dome as it opens.
        if (open > 0.02f) frame.glow(DOME_X + 2f, DOME_Y - 4f, 4f, red, 0.25f * open)
        // Equator band.
        for (x in (DOME_X - DOME_RX).toInt()..(DOME_X + DOME_RX).toInt()) {
            frame.put(x, DOME_Y.toInt(), silverDark.mix(silver, 0.35f), 2f, '▀')
        }
    }
}

/**
 * An observatory on a hill at night: the dome's slit slides open, the telescope peers out and a sodium guide-star laser
 * rises into the sky, while the stars wheel around the pole in long-exposure trails across the Milky Way. A meteor
 * streaks past, a town glows on the horizon and the slit closes again. The seed shapes the stars and the hills.
 */
public fun observatoryPiece(seed: Int = 0): AsciiPiece {
    val scene = Observatory(seed)
    return asciiPiece(
        name = "observatory",
        ground = Rgb.fromHex("#03040c"),
        category = "scenes",
        cols = ICOLS,
        rows = IROWS,
        fps = 15,
        still = 12f,
        loop = LOOP,
        note = "an observatory dome opening at night under wheeling star trails, a laser guide star and a meteor",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * An observatory dome opening at night under star trails, a guide-star laser rising and a meteor passing.
 *
 * @param seed shapes the stars and the hills.
 */
@Composable
public fun AsciiObservatory(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "An observatory dome opening at night under circling star trails",
) {
    val piece = remember(seed) { observatoryPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
