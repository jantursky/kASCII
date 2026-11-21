package com.jantursky.kascii.gallery.m

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
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val LOOP = 36f
private const val POUR_END = 0.7f
private const val DRAIN_START = 0.76f
private const val DRAIN_END = 0.97f
private const val STEPS = 1200
private const val SUB = 8
private const val LEVELS = MROWS * SUB
private const val LEFT = 26
private const val RIGHT = 165
private const val MID = 95.5f
private const val FLOOR = 24
private const val FUNNEL = 1.25f
private const val WALL_TOP = 11
private const val TOP_LEVEL = (MROWS - WALL_TOP - 1) * SUB
private const val REPOSE = 3
private const val CUT_SLOPE = 3.2f
private const val STREAMS = 3
private const val SPOUT_ROW = 4
private const val GRAVITY = 95f
private const val GRAIN_DT = 0.035f
private const val FALL_MAX = 1.1f
private const val BAND = 150
private const val NONE: Short = Short.MAX_VALUE
private const val PARTIAL = "▁▂▃▄▅▆▇"

private class Sandbox(
    seed: Int,
) {
    private val frame = MFrame()
    private val palette: MPalette
    private val dep = ShortArray(MCOLS * LEVELS) { NONE }
    private val tint = ByteArray(MCOLS * LEVELS)
    private val base = IntArray(MCOLS)
    private val finalTop = IntArray(MCOLS)
    private val surf = IntArray(MCOLS)
    private val spoutBase = FloatArray(STREAMS)
    private val spoutAmp = FloatArray(STREAMS)
    private val spoutFreq = IntArray(STREAMS)
    private val spoutPh = FloatArray(STREAMS)
    private val tintOffset = IntArray(STREAMS)
    private val sands =
        listOf(
            mcol("#ff6f59"),
            mcol("#ffc857"),
            mcol("#3fc1b0"),
            mcol("#f4ecd8"),
            mcol("#b65fc4"),
            mcol("#4f86f7"),
        )
    private val back = mcol("#120c16")
    private val glass = mcol("#7fb8c8")

    init {
        val rng = mRng(seed, 55)
        for (x in 0 until MCOLS) {
            base[x] = if (x < LEFT || x > RIGHT) LEVELS else FLOOR + (FUNNEL * abs(x - MID)).toInt()
        }
        for (k in 0 until STREAMS) {
            spoutBase[k] = LEFT + (RIGHT - LEFT) * (k + 0.5f) / STREAMS + (rng.float() - 0.5f) * 10f
            spoutAmp[k] = 10f + rng.float() * 12f
            spoutFreq[k] = 1 + rng.int(3)
            spoutPh[k] = rng.float() * MTAU
            tintOffset[k] = rng.int(sands.size)
        }
        simulate(rng.int(1 shl 30))
        palette = MPalette(buildPalette())
    }

    private fun buildPalette(): List<MCol> {
        val list = ArrayList<MCol>()
        for (c in sands) list += mramp(7, c.scale(0.28f), c.scale(0.6f), c, c.mix(mcol("#ffffff"), 0.55f))
        list += mramp(8, mcol("#050307"), back, mcol("#24182a"), mcol("#3a2a3e"))
        list += mramp(6, mcol("#16303a"), mcol("#2f5a68"), glass, mcol("#d8f4ff"))
        list += mramp(4, mcol("#2a2630"), mcol("#4a4452"), mcol("#7a7282"), mcol("#b0a8b8"))
        return list
    }

    /** The column a spout pours over at simulation step [s]. */
    private fun spoutX(
        k: Int,
        s: Float,
    ): Float = spoutBase[k] + spoutAmp[k] * sin(MTAU * spoutFreq[k] * s / STEPS + spoutPh[k])

    private fun tintOf(
        k: Int,
        s: Int,
    ): Int = (tintOffset[0] + (s + k * BAND / STREAMS) / BAND) % sands.size

    /** Pours every grain once, letting each roll down to a slope it can rest on, and records when each place filled. */
    private fun simulate(salt: Int) {
        val h = base.copyOf()
        var room = 0
        for (x in LEFT..RIGHT) room += TOP_LEVEL - base[x]
        val rate = room * 0.72f / (STEPS * STREAMS)
        val acc = FloatArray(STREAMS)
        var hash = salt
        for (s in 0 until STEPS) {
            for (k in 0 until STREAMS) {
                acc[k] += rate
                while (acc[k] >= 1f) {
                    acc[k] -= 1f
                    hash = hash * 1103515245 + 12345
                    var c = (spoutX(k, s.toFloat()) + ((hash ushr 16) % 3) - 1).toInt().coerceIn(LEFT, RIGHT)
                    var guard = 0
                    while (guard++ < 400) {
                        val l = if (c > LEFT) h[c - 1] else LEVELS
                        val r = if (c < RIGHT) h[c + 1] else LEVELS
                        val dl = h[c] - l
                        val dr = h[c] - r
                        if (dl <= REPOSE && dr <= REPOSE) break
                        c =
                            if (dl > dr) {
                                c - 1
                            } else if (dr > dl) {
                                c + 1
                            } else {
                                hash = hash * 1103515245 + 12345
                                if ((hash ushr 20) and 1 == 0) c - 1 else c + 1
                            }
                    }
                    if (h[c] >= TOP_LEVEL) continue
                    val i = c * LEVELS + h[c]
                    dep[i] = s.toShort()
                    tint[i] = tintOf(k, s).toByte()
                    h[c]++
                }
            }
        }
        h.copyInto(finalTop)
    }

    /** The first level of column [x] still empty at step [s]. */
    private fun topAt(
        x: Int,
        s: Int,
    ): Int {
        var lo = base[x]
        var hi = finalTop[x]
        val o = x * LEVELS
        while (lo < hi) {
            val m = (lo + hi) ushr 1
            if (dep[o + m] <= s) lo = m + 1 else hi = m
        }
        return lo
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val u = t / LOOP
        val pourT = POUR_END * LOOP
        val step = min(STEPS - 1, floor(t / pourT * STEPS).toInt())
        // The drain: a cone-shaped crater sinks from the neck, eating the strata from the top down.
        val dk = msmooth(DRAIN_START, DRAIN_END, u)
        val drainLevel = if (dk <= 0f) Float.MAX_VALUE else TOP_LEVEL + 40f - dk * (TOP_LEVEL + 40f + 130f)
        for (x in 0 until MCOLS) {
            surf[x] =
                if (x < LEFT || x > RIGHT) {
                    0
                } else {
                    val cut = drainLevel + CUT_SLOPE * abs(x - MID)
                    max(base[x], min(topAt(x, step), if (cut > LEVELS) LEVELS else cut.toInt()))
                }
        }
        background()
        drawSand(step)
        drawGlass()
        drawStreams(t, pourT)
        drawRollers(t, pourT, step)
        if (dk > 0f && dk < 1f) drainStream(t)
        drawSpouts(t, pourT, step)
        frame.output(grid, palette)
    }

    private fun background() {
        for (y in 0 until MROWS) {
            for (x in 0 until MCOLS) {
                val i = y * MCOLS + x
                val dx = (x - MID) / 110f
                val dy = (y - 14f) / 40f
                val light = exp(-(dx * dx + dy * dy) * 2.2f)
                val r = back.r + 0.12f * light
                val g = back.g + 0.08f * light
                val b = back.b + 0.1f * light
                val cov = if ((x + y * 3) % 7 == 0) 0.25f * light + 0.05f else 0.06f * light
                frame.set(i, r, g, b, cov)
            }
        }
    }

    private fun drawSand(step: Int) {
        for (x in LEFT..RIGHT) {
            val top = surf[x]
            val o = x * LEVELS
            for (y in 0 until MROWS) {
                val lo = (MROWS - 1 - y) * SUB
                val from = max(lo, base[x])
                val n = min(top, lo + SUB) - from
                if (n <= 0 || from >= lo + SUB) continue
                val full = from == lo && n == SUB
                val idx = o + min(top, lo + SUB) - 1
                val c = sands[tint[idx].toInt()]
                // Deeper sand is a little darker; grains that just landed glow.
                val depth = msmooth(0f, 110f, (top - lo).toFloat())
                val age = step - dep[idx]
                val fresh = if (age in 0..14) (1f - age / 14f) * 0.5f else 0f
                val grain = (Noise.hash(x, y, 3, 77) - 0.5f) * 0.16f
                val lit = 1.02f - 0.32f * depth + grain + (if (lo + SUB >= top) 0.12f else 0f)
                var r = c.r * lit
                var g = c.g * lit
                var b = c.b * lit
                r += (1f - r) * fresh
                g += (1f - g) * fresh
                b += (1f - b) * fresh
                val i = y * MCOLS + x
                if (full) {
                    frame.set(i, r, g, b, if (grain < -0.074f) 1.8f else 2f)
                } else if (from == lo) {
                    frame.set(i, r, g, b, 2f, PARTIAL[n - 1])
                } else {
                    // A cell cut by the funnel floor: its sand sits on the glass.
                    frame.set(i, r, g, b, 2f, if (n >= 4) '▄' else '▁')
                }
            }
        }
    }

    private fun drawGlass() {
        val gl = glass
        // The funnel floor, a thin glass line, and the straight walls above it.
        for (x in LEFT - 1..RIGHT + 1) {
            val level = if (x < LEFT || x > RIGHT) FLOOR + (FUNNEL * abs(x - MID)).toInt() else base[x]
            val y = MROWS - 1 - (level - 1) / SUB
            if (y !in 0 until MROWS) continue
            val i = y * MCOLS + x
            if (frame.cov[i] >= 1.9f && frame.ch[i] == 0.toChar()) continue
            frame.set(i, gl.r * 0.8f, gl.g * 0.8f, gl.b * 0.8f, 2f, '▔')
        }
        val yWallEnd = MROWS - 1 - (base[LEFT] - 1) / SUB
        for (y in WALL_TOP until yWallEnd) {
            val shine = 0.8f + 0.3f * msmooth(30f, 14f, y.toFloat())
            frame.set(y * MCOLS + LEFT - 1, gl.r * shine, gl.g * shine, gl.b * shine, 2f, '│')
            frame.set(y * MCOLS + RIGHT + 1, gl.r * shine * 0.8f, gl.g * shine * 0.8f, gl.b * shine * 0.8f, 2f, '│')
        }
        frame.set(WALL_TOP * MCOLS + LEFT - 1, gl.r, gl.g, gl.b, 2f, '╷')
        frame.set(WALL_TOP * MCOLS + RIGHT + 1, gl.r, gl.g, gl.b, 2f, '╷')
        // A soft reflection streak down the left side of the glass.
        for (y in WALL_TOP + 2 until yWallEnd - 2) {
            val i = y * MCOLS + LEFT + 2
            frame.light(i, gl, 0.12f)
        }
        // The stand under the neck.
        for (x in 70..121) {
            val i = (MROWS - 1) * MCOLS + x
            frame.set(i, 0.3f, 0.28f, 0.33f, 2f, if (x == 70 || x == 121) '▀' else '▀')
        }
    }

    private fun grainHash(
        n: Int,
        k: Int,
        salt: Int,
    ): Float = Noise.hash(n, k, salt, 911)

    private fun drawStreams(
        t: Float,
        pourT: Float,
    ) {
        val nMax = floor(min(t, pourT) / GRAIN_DT).toInt()
        val nMin = max(0, floor((t - FALL_MAX) / GRAIN_DT).toInt())
        for (k in 0 until STREAMS) {
            for (n in nMin..nMax) {
                val tn = n * GRAIN_DT
                if (tn > t || tn > pourT) continue
                val s = tn / pourT * STEPS
                for (j in 0 until 2) {
                    val a = t - tn - j * GRAIN_DT * 0.5f
                    if (a < 0f) continue
                    val x = (spoutX(k, s) + (grainHash(n, k, j) - 0.5f) * (0.8f + a * 2.2f)).toInt()
                    val yRow = SPOUT_ROW + 0.5f * GRAVITY * a * a
                    val y = yRow.toInt()
                    if (x !in LEFT..RIGHT || y >= MROWS) continue
                    val level = (MROWS - yRow) * SUB
                    if (level < surf[x]) continue
                    val c = sands[tintOf(k, s.toInt().coerceAtMost(STEPS - 1))]
                    val i = y * MCOLS + x
                    val glyph =
                        if (a < 0.25f) {
                            '│'
                        } else if (grainHash(n, k, j + 7) > 0.5f) {
                            '•'
                        } else {
                            '·'
                        }
                    val br = 1.1f
                    frame.set(i, min(1f, c.r * br), min(1f, c.g * br), min(1f, c.b * br), 2f, glyph)
                }
            }
        }
    }

    /** Grains that skip down the flanks of the heap from where a stream lands: small avalanches. */
    private fun drawRollers(
        t: Float,
        pourT: Float,
        step: Int,
    ) {
        if (t > pourT + 1f) return
        val every = 0.11f
        val life = 1.1f
        val nMax = floor(min(t, pourT) / every).toInt()
        val nMin = max(0, floor((t - life) / every).toInt())
        for (k in 0 until STREAMS) {
            for (n in nMin..nMax) {
                val t0 = n * every
                val a = t - t0
                if (a < 0f || a > life) continue
                val s = t0 / pourT * STEPS
                val x0 = spoutX(k, s)
                val dir = if (grainHash(n, k, 21) > 0.5f) 1f else -1f
                val reach = 6f + 16f * grainHash(n, k, 22)
                val p = 1f - (1f - a / life) * (1f - a / life)
                val x = (x0 + dir * reach * p).toInt()
                if (x !in LEFT..RIGHT) continue
                val top = surf[x]
                val y = MROWS - 1 - (top - 1) / SUB - (if ((top - 1) % SUB >= SUB / 2) 1 else 0)
                if (y !in 0 until MROWS) continue
                val c = sands[tintOf(k, min(step, STEPS - 1))]
                val fade = 1f - a / life
                frame.set(
                    y * MCOLS + x,
                    c.r + (1f - c.r) * 0.4f * fade,
                    c.g + (1f - c.g) * 0.4f * fade,
                    c.b,
                    2f,
                    if (fade >
                        0.5f
                    ) {
                        '•'
                    } else {
                        '·'
                    },
                )
            }
        }
    }

    private fun drainStream(t: Float) {
        val neckY = MROWS - 1 - (FLOOR - 1) / SUB
        val x = MID.toInt()
        val idx = x * LEVELS + max(base[x], surf[x] - 1)
        val c = sands[tint[idx].toInt()]
        for (y in neckY + 1 until MROWS - 1) {
            for (dx in 0..1) {
                val h = Noise.hash(y, dx, floor(t * 20f).toInt(), 5)
                frame.set(y * MCOLS + x + dx, c.r, c.g, c.b, 2f, if (h > 0.5f) '•' else '│')
            }
        }
    }

    private fun drawSpouts(
        t: Float,
        pourT: Float,
        step: Int,
    ) {
        // The rail.
        for (x in LEFT - 4..RIGHT + 4) frame.set(MCOLS + x, 0.42f, 0.4f, 0.46f, 2f, '═')
        val s = min(t, pourT) / pourT * STEPS
        for (k in 0 until STREAMS) {
            val cx = spoutX(k, s).toInt()
            val c = sands[tintOf(k, min(step, STEPS - 1))]
            val metal = MCol(0.48f, 0.45f, 0.52f)
            for (dx in -4..4) {
                frame.set(
                    2 * MCOLS + cx + dx,
                    metal.r,
                    metal.g,
                    metal.b,
                    2f,
                    if (abs(dx) ==
                        4
                    ) {
                        '▐'
                    } else {
                        '█'
                    },
                )
            }
            for (dx in -3..3) frame.set(2 * MCOLS + cx + dx, c.r, c.g, c.b, 2f)
            frame.set(2 * MCOLS + cx - 4, metal.r, metal.g, metal.b, 2f, '▐')
            frame.set(2 * MCOLS + cx + 4, metal.r, metal.g, metal.b, 2f, '▌')
            for (dx in -2..2) {
                frame.set(
                    3 * MCOLS + cx + dx,
                    metal.r * 0.8f,
                    metal.g * 0.8f,
                    metal.b * 0.8f,
                    2f,
                    if (abs(dx) ==
                        2
                    ) {
                        '▝'
                    } else {
                        '▀'
                    },
                )
            }
            frame.set(3 * MCOLS + cx - 2, metal.r * 0.8f, metal.g * 0.8f, metal.b * 0.8f, 2f, '▝')
            frame.set(3 * MCOLS + cx + 2, metal.r * 0.8f, metal.g * 0.8f, metal.b * 0.8f, 2f, '▘')
        }
    }
}

/**
 * Falling sand: three hoppers ride a rail and pour coloured sand into an hourglass-like glass funnel. The grains were
 * dropped one by one when the piece was built, each rolling down until it rests at the angle of repose, so the heap
 * grows real dunes and striped strata as the hoppers sweep and change colour, with fresh grains glowing and small
 * avalanches skipping down the flanks. At the end of the pour the neck opens and a cone-shaped crater drains the basin,
 * cutting through the strata, before the cycle starts again. The seed sets the hopper paths and colours.
 */
public fun fallingSandPiece(seed: Int = 0): AsciiPiece {
    val scene = Sandbox(seed)
    return asciiPiece(
        name = "falling sand",
        ground = Rgb.fromHex("#0a070c"),
        category = "physics",
        cols = MCOLS,
        rows = MROWS,
        fps = 24,
        still = 20f,
        loop = LOOP,
        note = "coloured sand pouring into a glass funnel, piling into striped dunes, then draining away",
    ) { time, grid -> scene.draw(mLoopTime(time, LOOP), grid) }
}

/**
 * Coloured sand pouring into a glass funnel, piling into striped dunes and draining away again.
 *
 * @param seed sets the hopper paths and the sand colours.
 */
@Composable
public fun AsciiFallingSand(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Coloured sand pouring into a glass funnel and piling into striped dunes",
) {
    val piece = remember(seed) { fallingSandPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
