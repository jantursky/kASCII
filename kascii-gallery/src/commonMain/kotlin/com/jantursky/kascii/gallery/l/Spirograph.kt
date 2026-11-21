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

private const val LOOP = 24f

/** One spirograph: a ring of [ring] teeth, a wheel of [wheel] teeth rolling inside, the pen at [pen] of the wheel radius. */
private class Spiro(
    val ring: Int,
    val wheel: Int,
    val pen: Float,
    val cx: Float,
    val cy: Float,
    val size: Float,
    val laps: Int,
    val hue: Float,
) {
    /** Turns of the wheel's centre until the curve closes. */
    val turns: Int = wheel / gcd(ring, wheel)
    val span: Float = LTAU * turns
    private val scale = size / (ring - wheel + pen * wheel)
    val bigR: Float = ring * scale
    val smallR: Float = wheel * scale

    fun x(th: Float): Float {
        val k = (ring - wheel).toFloat() / wheel
        return cx + ((ring - wheel) * cos(th) + pen * wheel * cos(k * th)) * scale
    }

    fun y(th: Float): Float {
        val k = (ring - wheel).toFloat() / wheel
        return cy + ((ring - wheel) * sin(th) - pen * wheel * sin(k * th)) * scale
    }

    private fun gcd(
        a: Int,
        b: Int,
    ): Int = if (b == 0) a else gcd(b, a % b)
}

private class Spirograph(
    seed: Int,
) {
    private val frame = LFrame()
    private val back = LFrame()
    private val palette: LPalette
    private val ir = FloatArray(LCELLS)
    private val ig = FloatArray(LCELLS)
    private val ib = FloatArray(LCELLS)
    private val ik = FloatArray(LCELLS)
    private val iglyph = CharArray(LCELLS)
    private val spiros: List<Spiro>
    private val hues =
        arrayOf(
            lcol("#ff4fa8"),
            lcol("#ff8a3d"),
            lcol("#ffd34a"),
            lcol("#3fe0b0"),
            lcol("#4aa8ff"),
            lcol("#a46bff"),
        )
    private val steel = lcol("#8a9ab8")

    init {
        val rng = lRng(seed, 83)
        val mains = arrayOf(intArrayOf(100, 36), intArrayOf(96, 30), intArrayOf(105, 40), intArrayOf(96, 36))
        val m = mains[rng.int(mains.size)]
        val h0 = rng.float()
        val cy = LROWS * L_ROW_U / 2f
        spiros =
            listOf(
                Spiro(m[0], m[1], 0.5f + rng.float() * 0.12f, LCOLS / 2f, cy, 49f, 1, h0),
                Spiro(80, 30, 0.6f, 30f, cy, 25f, 2, h0 + 0.33f),
                Spiro(84, 48, 0.55f, LCOLS - 30f, cy, 25f, 2, h0 + 0.66f),
            )
        palette =
            LPalette(
                lramp(12, lcol("#030308"), lcol("#0c0c1c"), lcol("#1a1a36"), lcol("#2c2c54")) +
                    lramp(8, lcol("#2a0a1c"), lcol("#a02a6a"), lcol("#ff4fa8"), lcol("#ffd0ea")) +
                    lramp(8, lcol("#2a1406"), lcol("#a05020"), lcol("#ff8a3d"), lcol("#ffe0c8")) +
                    lramp(8, lcol("#2a2208"), lcol("#a08020"), lcol("#ffd34a"), lcol("#fff6d0")) +
                    lramp(8, lcol("#06261e"), lcol("#18906c"), lcol("#3fe0b0"), lcol("#d0fff0")) +
                    lramp(8, lcol("#081a36"), lcol("#2060b0"), lcol("#4aa8ff"), lcol("#d8ecff")) +
                    lramp(8, lcol("#160a30"), lcol("#5a30a0"), lcol("#a46bff"), lcol("#e8dcff")) +
                    lramp(6, lcol("#1a2030"), lcol("#5a6a8a"), lcol("#a8b4cc"), lcol("#ffffff")),
            )
        back.clear()
        for (y in 0 until LROWS) {
            for (x in 0 until LCOLS) {
                val i = y * LCOLS + x
                // Faint graph paper: a dot at every grid crossing.
                val dot = x % 6 == 0 && y % 3 == 0
                back.set(i, 0.035f, 0.035f, 0.07f, if (dot) 0.75f else 0.05f, if (dot) '·' else 0.toChar())
            }
        }
    }

    private fun hueAt(f: Float): LCol {
        val p = lfract(f) * hues.size
        val i = p.toInt() % hues.size
        return hues[i].mix(hues[(i + 1) % hues.size], p - p.toInt())
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val u = t / LOOP
        frame.copyFrom(back)
        ir.fill(0f)
        ig.fill(0f)
        ib.fill(0f)
        ik.fill(0f)
        iglyph.fill(' ')
        for ((n, s) in spiros.withIndex()) {
            val head = s.span * lfract(u * s.laps)
            // The whole figure as a faint ghost, then a long trail that fades with age behind the pen.
            val trail = s.span * 0.88f
            val steps = (s.span * s.size * 1.2f).toInt()
            var px = s.x(head - trail)
            var py = s.y(head - trail)
            for (k in 1..steps) {
                val f = k.toFloat() / steps
                val th = head - trail + trail * f
                val x = s.x(th)
                val y = s.y(th)
                val age = 1f - f
                val bright = 0.14f + 0.86f * (1f - age).pow(2.2f)
                val col = hueAt(s.hue + th / s.span + u)
                stroke(px, py, x, y, col, bright)
                px = x
                py = y
            }
            val ghostSteps = steps / 2
            for (k in 0 until ghostSteps) {
                val th = s.span * k / ghostSteps
                val col = hueAt(s.hue + th / s.span + u)
                plot(s.x(th), s.y(th), col, 0.13f)
            }
            if (n == 0) gears(s, head)
            pen(s.x(head), s.y(head), hueAt(s.hue + head / s.span + u))
        }
        composite()
        frame.output(grid, palette, 0.035f)
    }

    /** Draws the line from one pen position to the next, keeping the brighter of what a cell already has. */
    private fun stroke(
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        c: LCol,
        k: Float,
    ) {
        val n = (max(kotlin.math.abs(bx - ax), kotlin.math.abs(by - ay) / L_ROW_U) * 2f).toInt() + 1
        val glyph = lstroke(bx - ax, by - ay)
        for (s in 0 until n) {
            val f = s.toFloat() / n
            val i = plot(ax + (bx - ax) * f, ay + (by - ay) * f, c, k)
            if (i >= 0) iglyph[i] = glyph
        }
    }

    private fun plot(
        x: Float,
        y: Float,
        c: LCol,
        k: Float,
    ): Int {
        val i = lcell(x, y)
        if (i < 0 || k <= ik[i]) return -1
        ik[i] = k
        ir[i] = c.r
        ig[i] = c.g
        ib[i] = c.b
        iglyph[i] = ' '
        return i
    }

    /** The fixed toothed ring, the wheel rolling inside it, and the arm from the wheel's centre to the pen. */
    private fun gears(
        s: Spiro,
        th: Float,
    ) {
        val ringSteps = 520
        for (k in 0 until ringSteps) {
            val a = LTAU * k / ringSteps
            val tooth = if (((a / LTAU * s.ring).toInt() and 1) == 0) 0f else 1.4f
            val r = s.bigR + 1.6f - tooth
            gearDot(s.cx + r * cos(a), s.cy + r * sin(a), 0.4f)
        }
        val wx = s.cx + (s.bigR - s.smallR) * cos(th)
        val wy = s.cy + (s.bigR - s.smallR) * sin(th)
        // The wheel turns backwards as it rolls.
        val spin = -th * (s.ring - s.wheel).toFloat() / s.wheel
        val wheelSteps = 300
        for (k in 0 until wheelSteps) {
            val a = LTAU * k / wheelSteps
            val tooth = if ((((a / LTAU) * s.wheel).toInt() and 1) == 0) 0f else 1.3f
            val r = s.smallR - 0.4f - tooth
            gearDot(wx + r * cos(a + spin), wy + r * sin(a + spin), 0.75f, true)
        }
        // Spokes and the hub.
        for (spoke in 0 until 3) {
            val a = spin + LTAU * spoke / 3
            for (k in 1 until 10) {
                val r = s.smallR * 0.6f * k / 10f
                gearDot(wx + r * cos(a), wy + r * sin(a), 0.45f, true)
            }
        }
        val pxp = s.x(th)
        val pyp = s.y(th)
        val n = 30
        for (k in 0..n) {
            val f = k.toFloat() / n
            gearDot(wx + (pxp - wx) * f, wy + (pyp - wy) * f, 0.6f, true)
        }
        gearDot(wx, wy, 1f, true)
    }

    private fun gearDot(
        x: Float,
        y: Float,
        k: Float,
        top: Boolean = false,
    ) {
        val i = lcell(x, y)
        if (i < 0 || (!top && ik[i] > k + 0.15f)) return
        ik[i] = if (top) k else max(ik[i], k)
        iglyph[i] = ' '
        ir[i] = steel.r
        ig[i] = steel.g
        ib[i] = steel.b
    }

    /** The pen tip, a hot dot with a soft halo. */
    private fun pen(
        x: Float,
        y: Float,
        c: LCol,
    ) {
        for (dy in -2..2) {
            for (dx in -5..5) {
                val ex = dx.toFloat()
                val ey = dy * L_ROW_U
                val d2 = ex * ex + ey * ey
                val i = lcell(x + ex, y + ey)
                if (i < 0) continue
                val h = 0.4f / (1f + d2 / 4f)
                frame.light(i, c, h)
            }
        }
        val i = lcell(x, y)
        if (i >= 0) {
            ik[i] = 1.5f
            ir[i] = 0.6f + c.r * 0.5f
            ig[i] = 0.6f + c.g * 0.5f
            ib[i] = 0.6f + c.b * 0.5f
        }
    }

    /** Lays the line buffer over the paper with a soft glow bled into the neighbours. */
    private fun composite() {
        for (y in 0 until LROWS) {
            for (x in 0 until LCOLS) {
                val i = y * LCOLS + x
                var gr = 0f
                var gg = 0f
                var gb = 0f
                for (dy in -1..1) {
                    val yy = y + dy
                    if (yy < 0 || yy >= LROWS) continue
                    for (dx in -2..2) {
                        val xx = x + dx
                        if (xx < 0 || xx >= LCOLS || (dx == 0 && dy == 0)) continue
                        val j = yy * LCOLS + xx
                        val w = ik[j] * 0.05f
                        gr += ir[j] * w
                        gg += ig[j] * w
                        gb += ib[j] * w
                    }
                }
                val gm = max(gr, max(gg, gb))
                if (gm > 0.004f) frame.light(i, LCol(gr / gm, gg / gm, gb / gm), gm)
                val k = ik[i]
                if (k <= 0.02f) continue
                val b = min(1.2f, 0.25f + k)
                val line = iglyph[i] != ' ' && k > 0.3f
                val cov =
                    if (k > 1.2f) {
                        2f
                    } else if (line) {
                        2f
                    } else {
                        min(1f, 0.15f + k * 1.1f)
                    }
                frame.set(i, ir[i] * b + gr, ig[i] * b + gg, ib[i] * b + gb, cov, if (line) iglyph[i] else 0.toChar())
            }
        }
    }
}

/**
 * Spirograph: hypotrochoids drawn as if by a pen in a toothed wheel rolling inside a fixed ring, the main figure with
 * its ring, wheel, spokes and pen arm outlined in steel, two smaller figures beside it, each line a long trail fading
 * behind the pen over a faint ghost of the whole curve, its colour shifting along the curve and with time, on faint
 * graph paper. The seed picks the main gear pair and the colour phase. Every curve closes a whole number of times per
 * loop.
 */
public fun spirographPiece(seed: Int = 0): AsciiPiece {
    val scene = Spirograph(seed)
    return asciiPiece(
        name = "spirograph",
        ground = Rgb.fromHex("#030308"),
        category = "shapes",
        cols = LCOLS,
        rows = LROWS,
        fps = 24,
        still = 9f,
        loop = LOOP,
        note = "hypotrochoids drawn by rolling gears, colour-shifting trails fading behind the pen",
    ) { time, grid -> scene.draw(lLoopTime(time, LOOP), grid) }
}

/**
 * Spirograph curves drawn progressively by rolling gears, with fading colour-shifting trails.
 *
 * @param seed picks the main gear pair and the colour phase.
 */
@Composable
public fun AsciiSpirograph(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Spirograph curves drawn by rolling gears",
) {
    val piece = remember(seed) { spirographPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
