package com.jantursky.kascii.gallery.o

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
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 12f
private const val CX = 96f
private const val TABLE = 40
private const val TOP = 26
private const val HALF = 12
private const val BOKEH = 16
private const val FLAME_BASE = 24.4f * O_ROW_U
private const val FLAME_H = 36f
private const val FLAME_W = 7.4f

private class Candle(
    seed: Int,
) {
    private val frame = OFrame()
    private val back = OFrame()
    private val palette: OPalette
    private val flick = FloatArray(6)
    private val flickN = intArrayOf(2, 5, 9, 14, 23, 37)
    private val flickA = floatArrayOf(0.5f, 0.35f, 0.25f, 0.18f, 0.12f, 0.08f)
    private val bx = FloatArray(BOKEH)
    private val by = FloatArray(BOKEH)
    private val br = FloatArray(BOKEH)
    private val bi = FloatArray(BOKEH)
    private val bp = FloatArray(BOKEH)
    private val bHue = IntArray(BOKEH)
    private val dripX = IntArray(4)
    private val dripLen = IntArray(4)
    private val warm = ocol("#ffb455")
    private val wax = ocol("#efe0c4")
    private val bokehHues = listOf(ocol("#ff9a3c"), ocol("#ffcf7a"), ocol("#d0603a"), ocol("#5a8a8a"))

    init {
        val rng = oRng(seed, 21)
        for (k in flick.indices) flick[k] = rng.float() * OTAU
        for (b in 0 until BOKEH) {
            var x: Float
            do {
                x = 8f + rng.float() * 176f
            } while (abs(x - CX) < 18f)
            bx[b] = x
            by[b] = 4f + rng.float() * 30f
            br[b] = 5f + rng.float() * 9f
            bi[b] = 0.16f + rng.float() * 0.2f
            bp[b] = rng.float() * OTAU
            bHue[b] = if (rng.float() < 0.18f) 3 else rng.int(3)
        }
        val slots = intArrayOf(-HALF, -4 - rng.int(3), 3 + rng.int(3), HALF - 1)
        for (d in 0 until 4) {
            dripX[d] = CX.toInt() + slots[d]
            dripLen[d] = 3 + rng.int(9)
        }
        buildBack(seed)
        palette = OPalette(buildPalette())
    }

    private fun buildPalette(): List<OCol> =
        oramp(12, ocol("#070403"), ocol("#1e1008"), ocol("#4a2810"), ocol("#8a5020"), ocol("#d08a3a")) +
            oramp(
                10,
                ocol("#5a2a08"),
                ocol("#c05010"),
                ocol("#ff8a20"),
                ocol("#ffc040"),
                ocol("#fff0b0"),
                ocol("#ffffff"),
            ) +
            oramp(
                14,
                ocol("#1a120c"),
                ocol("#4a3624"),
                ocol("#7a6248"),
                ocol("#c8b08a"),
                ocol("#f4e6c8"),
                ocol("#fff8ea"),
            ) +
            oramp(4, ocol("#10203a"), ocol("#2a5aa0"), ocol("#6aa0ff")) +
            oramp(3, ocol("#24403e"), ocol("#3e6664"), ocol("#5a8a8a"))

    private fun buildBack(seed: Int) {
        val lat = OLattice(seed, 23)
        for (y in 0 until OROWS) {
            for (x in 0 until OCOLS) {
                val i = y * OCOLS + x
                if (y < TABLE) {
                    // The dark room behind, warmed a little toward the candle.
                    val dx = x - CX
                    val dy = (y - 16f) * O_ROW_U
                    val v = 0.025f + 0.05f * exp(-(dx * dx + dy * dy) / 7000f)
                    back.set(i, v * 1.1f, v * 0.65f, v * 0.4f, v * 3f)
                } else {
                    // A wooden table top in perspective: grain lines stretch as they come closer.
                    val depth = (y - TABLE + 1f) / (OROWS - TABLE)
                    val grain = lat.fbm(x * 0.04f / (0.4f + depth), y * 0.7f, 3)
                    val line = osmooth(0.62f, 0.7f, grain)
                    val edge = exp(-((x - CX) / 80f) * ((x - CX) / 80f))
                    val v = (0.03f + 0.03f * depth + 0.015f * line) * (0.35f + 0.65f * edge)
                    back.set(i, v * 1.25f, v * 0.75f, v * 0.42f, 1.8f + 0.2f * line)
                }
            }
        }
    }

    private fun flicker(
        t: Float,
        shift: Int,
    ): Float {
        var s = 0f
        for (k in flick.indices) s += flickA[k] * sin(OTAU * flickN[k] * t / LOOP + flick[k] + shift * 1.7f)
        return s
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        frame.copyFrom(back)
        val bright = 1f + 0.12f * flicker(t, 0)
        val sway = 1.1f * flicker(t, 1)
        val tall = 1f + 0.09f * flicker(t, 2)
        bokeh(t)
        glow(bright, sway)
        table(bright, sway)
        candle(bright)
        smoke(t, sway, tall)
        flame(sway, tall, bright)
        frame.output(grid, palette)
    }

    private fun bokeh(t: Float) {
        for (b in 0 until BOKEH) {
            val phase = OTAU * t / LOOP + bp[b]
            val cx = bx[b] + 2.5f * sin(phase)
            val cy = by[b] + 1.6f * cos(phase * 2f)
            val rad = br[b]
            val inten = bi[b] * (0.8f + 0.2f * sin(phase * 3f))
            val c = bokehHues[bHue[b]]
            val y0 = max(0, ((cy - rad - 1f) / O_ROW_U).toInt())
            val y1 = min(TABLE - 1, ((cy + rad + 1f) / O_ROW_U).toInt() + 1)
            for (y in y0..y1) {
                for (x in max(0, (cx - rad - 1f).toInt())..min(OCOLS - 1, (cx + rad + 1f).toInt())) {
                    val dx = x + 0.5f - cx
                    val dy = (y + 0.5f) * O_ROW_U - cy
                    val d = sqrt(dx * dx + dy * dy) / rad
                    if (d > 1.05f) continue
                    // A soft disc whose rim is a touch brighter, as an out-of-focus highlight is.
                    val disc = osmooth(1.05f, 0.85f, d) * (0.75f + 0.35f * osmooth(0.6f, 0.95f, d))
                    val j = y * OCOLS + x
                    frame.r[j] += c.r * inten * disc
                    frame.g[j] += c.g * inten * disc
                    frame.b[j] += c.b * inten * disc
                    frame.cov[j] = max(frame.cov[j], 0.35f + 0.5f * disc)
                }
            }
        }
    }

    private fun glow(
        bright: Float,
        sway: Float,
    ) {
        val gx = CX + sway * 0.5f
        val gy = FLAME_BASE - 10f
        for (y in 0 until TABLE) {
            for (x in 0 until OCOLS) {
                val dx = x + 0.5f - gx
                val dy = (y + 0.5f) * O_ROW_U - gy
                val r2 = dx * dx + dy * dy
                val k = bright * (0.32f * exp(-r2 / 2400f) + 0.12f * exp(-r2 / 14000f))
                if (k > 0.012f) frame.light(y * OCOLS + x, warm, k)
            }
        }
    }

    private fun table(
        bright: Float,
        sway: Float,
    ) {
        for (y in TABLE until OROWS) {
            for (x in 0 until OCOLS) {
                val i = y * OCOLS + x
                val dx = (x + 0.5f - CX) / 1.0f
                val dy = (y - TABLE + 0.5f) * O_ROW_U * 2.6f
                // A pool of light around the candle foot and a soft reflection of the flame on the polish.
                val pool = bright * 0.4f * exp(-(dx * dx + dy * dy) / 2200f)
                val refl =
                    bright * 0.5f * exp(-((dx - sway) * (dx - sway)) / 18f) *
                        osmooth(OROWS + 1f, TABLE + 2f, y.toFloat())
                frame.r[i] = frame.r[i] * (1f + pool * 2.5f) + warm.r * (pool * 0.28f + refl * 0.45f)
                frame.g[i] = frame.g[i] * (1f + pool * 2.5f) + warm.g * (pool * 0.28f + refl * 0.45f)
                frame.b[i] = frame.b[i] * (1f + pool * 2.5f) + warm.b * (pool * 0.28f + refl * 0.45f)
            }
        }
        // The candle's shadow cast toward the viewer is hidden by its foot; a dark contact line grounds it.
        for (x in CX.toInt() - HALF - 2..CX.toInt() + HALF + 1) {
            val i = TABLE * OCOLS + x
            frame.r[i] *= 0.4f
            frame.g[i] *= 0.4f
            frame.b[i] *= 0.4f
        }
    }

    private fun candle(bright: Float) {
        for (y in TOP until TABLE) {
            for (x in CX.toInt() - HALF until CX.toInt() + HALF) {
                val i = y * OCOLS + x
                val u = (x + 0.5f - CX) / HALF
                val down = (y - TOP).toFloat() / (TABLE - TOP)
                // Wax is translucent: lit from within near the top, rounding off toward its edges.
                val round = sqrt(max(0f, 1f - u * u))
                val inner = exp(-down * 3.2f) * 0.75f
                val side = 0.22f + 0.5f * round * (0.7f - 0.25f * u)
                val v = bright * (side * (1f - 0.45f * down) + inner * round)
                frame.set(i, wax.r * v, wax.g * v * (0.84f + 0.08f * down), wax.b * v * (0.62f + 0.15f * down), 2f)
            }
        }
        // The melted pool on top: a bright elliptical rim.
        for (x in CX.toInt() - HALF until CX.toInt() + HALF) {
            val u = (x + 0.5f - CX) / HALF
            val v = bright * (0.85f + 0.3f * (1f - u * u))
            frame.put(x, TOP, OCol(1f * v, 0.84f * v, 0.55f * v), 2f, if (abs(u) > 0.85f) '▄' else '▀')
        }
        // Drips running down the sides, catching the glow along their crest.
        for (d in 0 until 4) {
            val x = dripX[d]
            for (k in 0..dripLen[d]) {
                val y = TOP + 1 + k
                if (y >= TABLE) break
                val v = bright * (0.95f - k * 0.035f)
                val glyph = if (k == dripLen[d]) '▀' else '█'
                frame.put(x, y, OCol(1f * v, 0.92f * v, 0.76f * v), 2f, glyph)
            }
        }
        // The wick.
        val wy = (FLAME_BASE / O_ROW_U).toInt() + 1
        frame.put(CX.toInt(), wy, OCol(0.12f, 0.08f, 0.05f), 2f, '│')
    }

    private fun flame(
        sway: Float,
        tall: Float,
        bright: Float,
    ) {
        val h = FLAME_H * tall
        val y0 = max(0, ((FLAME_BASE - h) / O_ROW_U).toInt() - 1)
        val y1 = (FLAME_BASE / O_ROW_U).toInt() + 1
        for (y in y0..y1) {
            for (x in CX.toInt() - 10..CX.toInt() + 10) {
                val i = y * OCOLS + x
                val v = (FLAME_BASE - (y + 0.5f) * O_ROW_U) / h
                if (v < -0.04f || v > 1f) continue
                val vv = max(0f, v)
                val w =
                    FLAME_W *
                        if (vv < 0.24f) {
                            sqrt(vv / 0.24f) * 0.85f + 0.15f
                        } else {
                            ((1f - vv) / 0.76f).pow(1.25f)
                        }
                val dx = x + 0.5f - CX - sway * vv * vv * 3.2f
                val q = abs(dx) / max(0.3f, w)
                if (q > 1.25f) continue
                val core = osmooth(0.75f, 0.1f, q) * osmooth(0.05f, 0.2f, vv) * osmooth(0.75f, 0.35f, vv)
                val body = osmooth(1.15f, 0.6f, q)
                val tip = osmooth(0.55f, 1f, vv)
                val blue = osmooth(0.2f, 0f, vv) * osmooth(0.2f, 0.8f, q)
                var r = body * (1f - 0.35f * tip) * bright
                var g = body * (0.55f + 0.2f * core - 0.3f * tip) * bright
                var b = body * (0.12f - 0.1f * tip) * bright
                r += core * 0.25f
                g += core * 0.4f
                b += core * 0.55f
                r = r * (1f - blue) + 0.25f * blue
                g = g * (1f - blue) + 0.45f * blue
                b = b * (1f - blue) + 1f * blue
                val lum = 0.35f * r + 0.45f * g + 0.2f * b
                if (body < 0.3f) {
                    frame.light(i, OCol(r, g, b), lum)
                } else {
                    frame.set(i, r, g, b, 1.5f + 0.5f * osmooth(0.3f, 0.75f, lum))
                }
            }
        }
    }

    private fun smoke(
        t: Float,
        sway: Float,
        tall: Float,
    ) {
        val top = ((FLAME_BASE - FLAME_H * tall) / O_ROW_U).toInt() - 1
        var lastX = CX + sway * 3f
        for (y in top downTo 0) {
            val up = (top - y).toFloat()
            val amp = 0.6f + up * 0.32f
            val x = CX + sway * 3f + amp * sin(up * 0.55f - OTAU * 2f * t / LOOP) + up * 0.35f
            // Puffs travel up the wisp, so it thins and thickens as it rises.
            val puff = 0.5f + 0.5f * sin(up * 0.4f - OTAU * 3f * t / LOOP)
            val alpha = puff * osmooth(0f, 3f, up) * osmooth(top + 1f, top * 0.3f, up) * 0.32f
            val xi = x.toInt()
            if (alpha > 0.05f && xi in 0 until OCOLS) {
                val slope = x - lastX
                val glyph =
                    if (slope > 0.45f) {
                        '/'
                    } else if (slope < -0.45f) {
                        '\\'
                    } else if (puff > 0.75f) {
                        '('
                    } else {
                        ')'
                    }
                frame.put(xi, y, OCol(0.35f + alpha, 0.33f + alpha * 0.9f, 0.3f + alpha * 0.8f), 1f, glyph)
            }
            lastX = x
        }
    }
}

/**
 * A lit candle on a wooden table in a dark room: a teardrop flame with a blue foot and white core sways and flickers,
 * its glow breathes over the wax, the drips and the polished table, a thin smoke wisp curls up from the tip and warm
 * out-of-focus lights drift behind. The seed places the bokeh and the drips and phases the flicker. Every motion
 * repeats a whole number of times per loop.
 */
public fun candleFlamePiece(seed: Int = 0): AsciiPiece {
    val scene = Candle(seed)
    return asciiPiece(
        name = "candle flame",
        ground = Rgb.fromHex("#060302"),
        category = "nature",
        cols = OCOLS,
        rows = OROWS,
        fps = 24,
        still = 3f,
        loop = LOOP,
        note = "a candle with a flickering teardrop flame, warm glow on the wax and table, a smoke wisp and bokeh",
    ) { time, grid -> scene.draw(oLoopTime(time, LOOP), grid) }
}

/**
 * A candle flame flickering on a table, with a smoke wisp and warm bokeh behind.
 *
 * @param seed places the bokeh lights and the wax drips and phases the flicker.
 */
@Composable
public fun AsciiCandleFlame(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A candle flame flickering on a wooden table with warm lights blurred behind",
) {
    val piece = remember(seed) { candleFlamePiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
