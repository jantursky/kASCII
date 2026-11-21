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
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 26f
private const val FIGURES = 2
private const val SAMPLES = 5200
private const val S_END = 46f
private const val DRAW = 0.66f
private const val HOLD = 0.76f

/** One harmonograph drawing: its precomputed pen path in width units and the colours along it. */
private class Figure(
    val x: FloatArray,
    val y: FloatArray,
    val w: FloatArray,
    val glyph: CharArray,
    val from: MCol,
    val mid: MCol,
    val to: MCol,
)

private class Pendulums(
    seed: Int,
) {
    private val frame = MFrame()
    private val palette: MPalette
    private val figures: List<Figure>
    private val ink = FloatArray(MCELLS)
    private val cr = FloatArray(MCELLS)
    private val cg = FloatArray(MCELLS)
    private val cb = FloatArray(MCELLS)
    private val last = CharArray(MCELLS)
    private val glow = FloatArray(MCELLS)
    private val paper = mcol("#070912")
    private val schemes =
        listOf(
            listOf(mcol("#3fe6ff"), mcol("#8a6cff"), mcol("#ff4fb4")),
            listOf(mcol("#ffd166"), mcol("#ff7a45"), mcol("#ff3d6e")),
            listOf(mcol("#7dffb0"), mcol("#2fc6ff"), mcol("#b77dff")),
        )

    init {
        val rng = mRng(seed, 88)
        val pick = rng.int(schemes.size)
        figures =
            List(FIGURES) { f ->
                // Two rotary pendulums, each swinging in a near circle, tuned near a simple ratio and slightly detuned
                // so the figure precesses; the second one turns either way, giving loops inside or outside.
                val ratios = listOf(2f, 3f, 4f, -2f, -3f, 5f)
                val rb = ratios[(rng.int(ratios.size) + f * 2) % ratios.size]
                val base = 0.95f
                val f1 = base * (1f + (rng.float() - 0.5f) * 0.01f)
                val f3 = base * (1f + (rng.float() - 0.5f) * 0.02f)
                val f2 = base * rb * (1f + (rng.float() - 0.5f) * 0.008f)
                val f4 = base * rb * (1f + (rng.float() - 0.5f) * 0.012f)
                val p0 = rng.float() * MTAU
                val p1 = rng.float() * MTAU
                val d = FloatArray(4) { 0.02f + rng.float() * 0.018f }
                val a1 = 0.5f + rng.float() * 0.14f
                val q0 = MTAU / 4f + (rng.float() - 0.5f) * 0.5f
                val xs = FloatArray(SAMPLES)
                val ys = FloatArray(SAMPLES)
                for (k in 0 until SAMPLES) {
                    val s = S_END * k / (SAMPLES - 1)
                    val hx = a1 * sin(f1 * s + p0) * exp(-d[0] * s) + (1f - a1) * sin(f2 * s + p1) * exp(-d[1] * s)
                    val hy =
                        a1 * sin(f3 * s + p0 + q0) * exp(-d[2] * s) +
                            (1f - a1) * sin(f4 * s + p1 + MTAU / 4f) * exp(-d[3] * s)
                    xs[k] = MCOLS / 2f + hx * 74f
                    ys[k] = MROWS * M_ROW_U / 2f + hy * 47f
                }
                val ws = FloatArray(SAMPLES)
                val gs = CharArray(SAMPLES)
                for (k in 1 until SAMPLES) {
                    val dx = xs[k] - xs[k - 1]
                    val dy = (ys[k] - ys[k - 1]) / M_ROW_U
                    ws[k] = min(1.5f, sqrt(dx * dx + dy * dy))
                    gs[k] =
                        when {
                            abs(dx) > 2.4f * abs(dy) -> '-'
                            abs(dy) > 2.4f * abs(dx) -> '|'
                            (dx > 0f) == (dy > 0f) -> '\\'
                            else -> '/'
                        }
                }
                gs[0] = gs[1]
                val sc = schemes[(pick + f) % schemes.size]
                Figure(xs, ys, ws, gs, sc[0], sc[1], sc[2])
            }
        val list = ArrayList<MCol>()
        list += mramp(8, mcol("#020306"), paper, mcol("#121628"), mcol("#1e2440"))
        for (sc in schemes) {
            for (c in sc) {
                list +=
                    mramp(6, c.scale(0.16f), c.scale(0.4f), c.scale(0.75f), c, c.mix(mcol("#ffffff"), 0.6f))
            }
        }
        list += mramp(3, mcol("#d0d8ff"), mcol("#f0f4ff"), mcol("#ffffff"))
        palette = MPalette(list)
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val span = LOOP / FIGURES
        val f = min(FIGURES - 1, (t / span).toInt())
        val u = (t - f * span) / span
        val fig = figures[f]
        // The pen slows as the swing dies away; after it lifts, the drawing glows a moment and fades.
        val q = min(1f, u / DRAW)
        val head =
            ((1f - (1f - q) * (1f - q) * 0.35f - 0.65f * (1f - q)) * (SAMPLES - 1)).toInt().coerceIn(
                1,
                SAMPLES - 1,
            )
        val fade = if (u < HOLD) 1f else 1f - msmooth(HOLD, 0.985f, u)
        ink.fill(0f)
        cr.fill(0f)
        cg.fill(0f)
        cb.fill(0f)
        last.fill(0.toChar())
        for (k in 1..head) {
            val x = fig.x[k].toInt()
            val y = (fig.y[k] / M_ROW_U).toInt()
            if (x !in 0 until MCOLS || y !in 0 until MROWS) continue
            val i = y * MCOLS + x
            val age = (head - k).toFloat() / SAMPLES
            val wt = fig.w[k] * (0.4f + 0.6f * exp(-age * 9f))
            val p = k.toFloat() / SAMPLES
            val c = if (p < 0.5f) fig.from.mix(fig.mid, p * 2f) else fig.mid.mix(fig.to, p * 2f - 1f)
            ink[i] += wt
            cr[i] += c.r * wt
            cg[i] += c.g * wt
            cb[i] += c.b * wt
            last[i] = fig.glyph[k]
        }
        // A soft halo around the ink.
        for (y in 0 until MROWS) {
            for (x in 0 until MCOLS) {
                var s = 0f
                for (dy in -1..1) {
                    val yy = y + dy
                    if (yy !in 0 until MROWS) continue
                    for (dx in -2..2) {
                        val xx = x + dx
                        if (xx !in 0 until MCOLS) continue
                        s += ink[yy * MCOLS + xx] * (if (dy == 0) 1f else 0.6f) * (if (abs(dx) == 2) 0.5f else 1f)
                    }
                }
                glow[y * MCOLS + x] = s
            }
        }
        for (y in 0 until MROWS) {
            for (x in 0 until MCOLS) {
                val i = y * MCOLS + x
                val dx = (x - MCOLS / 2f) / MCOLS
                val dy = (y - MROWS / 2f) / MROWS
                val vign = 1f - 0.9f * (dx * dx + dy * dy)
                var r = paper.r * vign
                var g = paper.g * vign
                var b = paper.b * vign
                var cov = if (x % 6 == 3 && y % 3 == 1) 0.22f * vign else 0.03f
                var glyph = 0.toChar()
                val n = ink[i]
                if (n > 0.05f) {
                    val inv = 1f / n
                    val k = (1f - exp(-n * 1.1f)) * fade
                    val hot = msmooth(1.8f, 5f, n) * 0.5f
                    r = (cr[i] * inv * (0.85f + 0.45f * k)).let { it + (1f - it) * hot } * fade + r * (1f - fade)
                    g = (cg[i] * inv * (0.85f + 0.45f * k)).let { it + (1f - it) * hot } * fade + g * (1f - fade)
                    b = (cb[i] * inv * (0.85f + 0.45f * k)).let { it + (1f - it) * hot } * fade + b * (1f - fade)
                    cov = max(cov, k)
                    if (k > 0.3f) glyph = if (n > 2.8f) '●' else last[i]
                } else {
                    val h = glow[i]
                    if (h > 0.05f) {
                        val k = (1f - exp(-h * 0.45f)) * fade
                        val c = fig.mid
                        r += c.r * k * 0.8f
                        g += c.g * k * 0.8f
                        b += c.b * k * 0.8f
                        cov = max(cov, k * 0.75f)
                    }
                }
                frame.set(i, r, g, b, cov, glyph)
            }
        }
        // The pen: a bright tip with a halo while it draws.
        if (u < DRAW + 0.02f) {
            val px = fig.x[head]
            val py = fig.y[head]
            for (yy in -2..2) {
                for (xx in -4..4) {
                    val x = px.toInt() + xx
                    val y = (py / M_ROW_U).toInt() + yy
                    if (x !in 0 until MCOLS || y !in 0 until MROWS) continue
                    val ex = x + 0.5f - px
                    val ey = (y + 0.5f) * M_ROW_U - py
                    val k = exp(-(ex * ex + ey * ey) / 10f)
                    val i = y * MCOLS + x
                    frame.light(i, mcol("#f0f4ff"), k * 0.7f)
                }
            }
            val i = (py / M_ROW_U).toInt().coerceIn(0, MROWS - 1) * MCOLS + px.toInt().coerceIn(0, MCOLS - 1)
            frame.set(i, 1f, 1f, 1f, 2f, '●')
        }
        frame.output(grid, palette)
    }
}

/**
 * Harmonograph: a pen hung from damped pendulums draws a glowing Lissajous figure that slowly precesses and spirals
 * inward as the swing dies away. The newest line burns brightest and the colour shifts along its length; when the pen
 * lifts the drawing lingers, fades to nothing and a new figure begins. Two figures per loop, both set by the seed,
 * which picks the frequency ratios, detuning, damping and colours.
 */
public fun harmonographPiece(seed: Int = 0): AsciiPiece {
    val scene = Pendulums(seed)
    return asciiPiece(
        name = "harmonograph",
        ground = Rgb.fromHex("#05060c"),
        category = "generative",
        cols = MCOLS,
        rows = MROWS,
        fps = 24,
        still = 7.5f,
        loop = LOOP,
        note = "a damped pendulum pen drawing glowing lissajous figures that fade and redraw",
    ) { time, grid -> scene.draw(mLoopTime(time, LOOP), grid) }
}

/**
 * A damped pendulum pen drawing glowing Lissajous figures that fade and redraw.
 *
 * @param seed picks the frequency ratios, damping and colours of the figures.
 */
@Composable
public fun AsciiHarmonograph(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A pendulum pen drawing glowing Lissajous figures",
) {
    val piece = remember(seed) { harmonographPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
