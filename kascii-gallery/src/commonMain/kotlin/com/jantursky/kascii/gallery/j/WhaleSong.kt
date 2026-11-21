package com.jantursky.kascii.gallery.j

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
import com.jantursky.kascii.gallery.AKit
import com.jantursky.kascii.gallery.loopTime
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 20f
private const val SNOW = 150
private const val SHAFTS = 5
private const val HEAD_X = 140f
private const val HEAD_Y = 24f

private class WhaleSong(
    seed: Int,
) {
    private val lat = JLattice(seed, 3)
    private val sea = JFrame()
    private val frame = JFrame()
    private val light = FloatArray(JCELLS)
    private val palette: JPalette
    private val snowX = FloatArray(SNOW)
    private val snowY = FloatArray(SNOW)
    private val snowRate = IntArray(SNOW)
    private val snowNear = BooleanArray(SNOW)
    private val snowPhase = FloatArray(SNOW)
    private val shaftX = FloatArray(SHAFTS)
    private val shaftW = FloatArray(SHAFTS)
    private val shaftPhase = FloatArray(SHAFTS)
    private val back = jcol("#22385a")
    private val backLit = jcol("#5f86b0")
    private val belly = jcol("#b4c6d2")
    private val fin = jcol("#d4e0e8")
    private val shaft = jcol("#7fc4e8")
    private val ring = jcol("#a6e4ff")
    private val water = jcol("#0a2d5c")

    init {
        buildSea()
        val rng = AKit.rng(seed, 77)
        for (k in 0 until SNOW) {
            snowX[k] = rng.float()
            snowY[k] = rng.float()
            snowRate[k] = 1 + rng.int(2)
            snowNear[k] = rng.float() < 0.18f
            snowPhase[k] = rng.float()
        }
        for (k in 0 until SHAFTS) {
            shaftX[k] = 20f + k * 44f + rng.float() * 18f
            shaftW[k] = 3.5f + rng.float() * 4f
            shaftPhase[k] = rng.float()
        }
        palette = JPalette(buildPalette())
    }

    private fun buildPalette(): List<JCol> =
        jramp(
            12,
            jcol("#020814"),
            jcol("#062048"),
            jcol("#0f3f78"),
            jcol("#2a78b8"),
            jcol("#8cd0ff"),
            jcol("#e8f8ff"),
        ) +
            jramp(9, jcol("#0a1424"), back, jcol("#2e4664"), backLit, jcol("#7a96b2"), belly, jcol("#eef4f8")) +
            jramp(5, jcol("#0c3a4a"), jcol("#1a7a8a"), jcol("#58d0d0"), jcol("#b8fff4")) +
            jramp(4, jcol("#1c5a78"), jcol("#5ab0d8"), ring) +
            listOf(jcol("#04060c"), jcol("#ffffff"), jcol("#14304a"), jcol("#3c6488"))

    private fun buildSea() {
        for (y in 0 until JROWS) {
            val d = y / (JROWS - 1f)
            val c =
                if (d < 0.4f) {
                    jcol("#1d6aa0").mix(jcol("#0a2d5c"), d / 0.4f)
                } else {
                    jcol("#0a2d5c").mix(jcol("#03091c"), (d - 0.4f) / 0.6f)
                }
            for (x in 0 until JCOLS) {
                val n = lat.fbm(x * 0.05f, y * 0.22f, 3) - 0.5f
                val cv = 0.34f * (1f - jsmooth(0f, 0.5f, d)) + 0.06f * (1f - d) + n * 0.55f - 0.05f
                sea.put(x, y, c, jclamp(cv))
            }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = JTAU * t / LOOP
        frame.copyFrom(sea)
        shaftField(t)
        surface(t)
        snow(t, near = false)
        // A calf far behind and above, lost in the blue.
        whale(
            76f + 4f * sin(phi + 1f),
            10.5f + 0.8f * sin(2f * phi + 0.4f),
            0.42f,
            phi * 4f + 1.3f,
            phi * 2f + 0.8f,
            0.62f,
        )
        shafts(t, 1.1f)
        rings(t)
        whale(HEAD_X + 5f * sin(phi), HEAD_Y + 1.4f * sin(phi + 0.9f), 1f, phi * 4f, phi * 2f, 0f)
        shafts(t, 0.16f)
        snow(t, near = true)
        frame.output(grid, palette)
    }

    private fun surface(t: Float) {
        for (x in 0 until JCOLS) {
            val w = sin(x * 0.21f + JTAU * 3f * t / LOOP) + 0.6f * sin(x * 0.07f - JTAU * 2f * t / LOOP)
            val row = if (w > 0.6f) 0 else 1
            frame.put(
                x,
                row,
                jcol("#bfeaff").mix(jcol("#5aa8d8"), jclamp(0.5f - w * 0.4f)),
                1f,
                if (w >
                    1.1f
                ) {
                    '^'
                } else {
                    '~'
                },
            )
            if (row == 1) frame.put(x, 0, jcol("#4c9ad0"), 0.45f)
        }
    }

    private fun shaftField(t: Float) {
        val phi = JTAU * t / LOOP
        val sway = FloatArray(SHAFTS) { 6f * sin(phi + shaftPhase[it] * JTAU) }
        val pulse = FloatArray(SHAFTS) { 0.65f + 0.35f * sin(2f * phi + shaftPhase[it] * 9f) }
        light.fill(0f)
        for (y in 1 until 42) {
            val fade = (1f - y / 42f).pow(1.1f)
            for (x in 0 until JCOLS) {
                var s = 0f
                val xs = x - y * JROW_U * 0.42f
                for (q in 0 until SHAFTS) {
                    val e = (xs - shaftX[q] + 30f - sway[q]) / shaftW[q]
                    if (e * e < 6f) s += exp(-e * e) * pulse[q]
                }
                light[y * JCOLS + x] = min(1f, s) * fade
            }
        }
    }

    private fun shafts(
        t: Float,
        k: Float,
    ) {
        for (i in 0 until JCELLS) {
            if (light[i] > 0.03f) frame.light(i, shaft, light[i] * k * (if (frame.cov[i] > 1.4f) 0.3f else 1f))
        }
    }

    private fun rings(t: Float) {
        val phi = JTAU * t / LOOP
        val cx = HEAD_X + 6f + 5f * sin(phi)
        val cy = HEAD_Y + 1.4f * sin(phi + 0.9f)
        for (q in 0 until 3) {
            val life = jfract(t / 4f + q / 3f)
            val radius = 6f + life * 76f
            val bright = (1f - life) * jsmooth(0f, 0.08f, life)
            if (bright < 0.05f) continue
            val y0 = max(0, (cy - (radius + 2f) / JROW_U).toInt())
            val y1 = min(JROWS - 1, (cy + (radius + 2f) / JROW_U).toInt() + 1)
            for (y in y0..y1) {
                for (x in max(0, (cx - radius - 2f).toInt())..min(JCOLS - 1, (cx + radius + 2f).toInt())) {
                    val dx = x + 0.5f - cx
                    val dy = (y + 0.5f - cy) * JROW_U
                    val d = sqrt(dx * dx + dy * dy)
                    val e = abs(d - radius)
                    if (e > 1.6f) continue
                    // Only ahead of the singer, strongest straight ahead.
                    val ahead = jsmooth(-0.1f, 0.7f, dx / max(d, 0.1f))
                    val k = bright * ahead * (1f - e / 1.6f)
                    if (k < 0.1f) continue
                    val c = ring.scale(0.45f + 0.7f * k)
                    frame.put(x, y, c, 1f, ringGlyph(dx, dy))
                }
            }
        }
    }

    private fun ringGlyph(
        dx: Float,
        dy: Float,
    ): Char {
        val ax = abs(dx)
        val ay = abs(dy)
        return if (ax > ay * 1.8f) {
            if (dx > 0f) ')' else '('
        } else if (ay > ax * 1.8f) {
            '-'
        } else if ((dx > 0f) == (dy > 0f)) {
            '\\'
        } else {
            '/'
        }
    }

    /** Thickness profile of the body from the snout ([u] = 0) to the tail stock ([u] = 1), 0..1. */
    private fun body(
        u: Float,
        nose: Float = 0.5f,
    ): Float =
        if (u < 0.32f) {
            sin(u / 0.32f * 1.5707964f).pow(nose)
        } else {
            max(0.1f, cos((u - 0.32f) / 0.68f * 1.5707964f).pow(0.9f))
        }

    /**
     * A humpback seen from the side, snout at ([hx], [hy] rows), [scale] 1 is 128 width units long. [tail] and [flap]
     * are the phases of the fluke and the long pectoral fin; [fog] fades it into the water.
     */
    private fun whale(
        hx: Float,
        hy: Float,
        scale: Float,
        tail: Float,
        flap: Float,
        fog: Float,
    ) {
        val len = 112f * scale
        val hyu = hy * JROW_U
        val beat = sin(tail)

        fun bend(u: Float): Float =
            9f * scale * (max(0f, u - 0.4f) / 0.6f).pow(2f) * beat - 1.2f * scale * sin(tail - 1f) * (1f - u)
        val topS = 13f * scale
        val botS = 16.5f * scale
        val x0 = max(0, (hx - len).toInt())
        val x1 = min(JCOLS - 1, hx.toInt() + 1)
        for (x in x0..x1) {
            val u = (hx - x - 0.5f) / len
            if (u < 0f || u > 1f) continue
            val h = body(u)
            val c = hyu + bend(u)
            val top = topS * body(u, 0.95f) + 2.6f * scale * jgauss(u, 0.64f, 0.035f)
            val bot = botS * h * (1f + 0.12f * jgauss(u, 0.2f, 0.12f))
            val yA = max(0, ((c - top) / JROW_U).toInt())
            val yB = min(JROWS - 1, ((c + bot) / JROW_U).toInt() + 1)
            for (y in yA..yB) {
                val yu = (y + 0.5f) * JROW_U
                val v = if (yu < c) (yu - c) / top else (yu - c) / bot
                if (v < -1f || v > 1f) continue
                val mott = lat.fbm(x * 0.12f / scale + 40f, y * 0.45f / scale, 2)
                val pale = jsmooth(0.25f, 0.55f, v + (mott - 0.5f) * 0.5f)
                val lit = jsmooth(-0.2f, -0.95f, v)
                var col = back.mix(backLit, lit * 0.85f).mix(belly, pale * 0.85f)
                col = col.scale(0.9f + 0.2f * mott)
                var glyph = 0.toChar()
                // Throat grooves along the belly behind the jaw.
                if (u in 0.04f..0.45f && v > 0.3f && ((yu - c) / JROW_U * 1.3f + u * 2f).let { jfract(it) } < 0.3f) {
                    col = col.scale(0.7f)
                    glyph = '='
                }
                if (fog > 0f) col = col.mix(water, fog)
                val edge = if (abs(v) > 0.88f) 1.8f else 2f
                frame.put(x, y, col, edge, glyph)
            }
        }
        // Eye and mouth line.
        val eu = 0.15f
        val ex = hx - eu * len
        val ey = (hyu + bend(eu) + 0.32f * botS * body(eu)) / JROW_U
        if (fog < 0.3f) {
            frame.put(ex.toInt(), ey.toInt(), jcol("#04060c"), 2f, '●')
            for (k in 1 until (0.24f * len).toInt()) {
                val u = k / len
                val yy = (hyu + bend(u) + 0.12f * botS * body(u) + k * 0.06f) / JROW_U
                frame.put((hx - k).toInt(), yy.toInt(), jcol("#14304a"), 2f, '_')
            }
        }
        // Fluke: a blade off the tail stock that tilts with the beat.
        val tx = hx - len
        val ty = hyu + bend(1f)
        val slope = (bend(1f) - bend(0.96f)) / (0.04f * len)
        val ang = atan(slope) * 1.6f
        val fl = 14f * scale
        val fx = tx - cos(ang) * fl
        val fy = ty + sin(ang) * fl
        val flukeCol = back.mix(backLit, 0.3f).let { if (fog > 0f) it.mix(water, fog) else it }
        val spread = 13f * scale * (0.6f + 0.4f * abs(cos(tail)))
        frame.tri(
            tx + 2f * scale,
            ty / JROW_U,
            fx,
            (fy - spread) / JROW_U,
            fx + 3f * scale,
            (fy + spread * 0.3f) / JROW_U,
            flukeCol,
        )
        frame.tri(
            tx + 2f * scale,
            ty / JROW_U,
            fx + 3f * scale,
            (fy - spread * 0.3f) / JROW_U,
            fx,
            (fy + spread) / JROW_U,
            flukeCol.scale(0.8f),
        )
        // The long pectoral fin, white and knobbly, rowing slowly.
        val fu = 0.3f
        val bx = hx - fu * len
        val by = (hyu + bend(fu) + 0.6f * botS * body(fu)) / JROW_U
        val a = 0.62f + 0.22f * sin(flap)
        val finLen = 40f * scale
        val ftx = bx - cos(a) * finLen
        val fty = by + sin(a) * finLen / JROW_U
        val finCol = fin.let { if (fog > 0f) it.mix(water, fog + 0.1f) else it }
        frame.segment(bx, by, ftx, fty, 5f * scale, 1.4f * scale) { s, e ->
            val k = 0.8f + 0.2f * (1f - s) - 0.45f * jsmooth(0.5f, 1f, e) - 0.25f * jsmooth(0.3f, 0f, s)
            val knob = if (e < -0.55f && jfract(s * 7f) < 0.35f) 0.75f else 1f
            finCol.scale(k * knob)
        }
    }

    private fun snow(
        t: Float,
        near: Boolean,
    ) {
        for (k in 0 until SNOW) {
            if (snowNear[k] != near) continue
            val y = jfract(snowY[k] + snowRate[k] * t / LOOP) * (JROWS - 2) + 2f
            val x = jfract(snowX[k] - t / LOOP) * JCOLS + 2f * sin(JTAU * (2f * t / LOOP + snowPhase[k]))
            val xi = x.toInt()
            val yi = y.toInt()
            if (xi < 0 || xi >= JCOLS) continue
            val i = yi * JCOLS + xi
            if (frame.cov[i] > 1.4f) continue
            val tw = 0.5f + 0.5f * sin(JTAU * (3f * t / LOOP + snowPhase[k]))
            val depth = 1f - yi / JROWS.toFloat() * 0.5f
            if (near) {
                frame.put(xi, yi, jcol("#b8fff4").scale(0.6f + 0.4f * tw), 1f, if (tw > 0.7f) '•' else '·')
            } else {
                frame.put(xi, yi, jcol("#58d0d0").scale((0.45f + 0.4f * tw) * depth), 1f, '·')
            }
        }
    }
}

/**
 * A humpback whale glides through deep blue water with a calf far behind, its long white fins rowing slowly and its
 * fluke beating. Sound rings of its song spread out ahead of it, light shafts sway down from the surface and marine
 * snow drifts past. The seed places the snow, the shafts and the mottling. Every motion repeats a whole number of
 * times per loop.
 */
public fun whaleSongPiece(seed: Int = 0): AsciiPiece {
    val scene = WhaleSong(seed)
    return asciiPiece(
        name = "whale song",
        ground = Rgb.fromHex("#020814"),
        category = "creatures",
        cols = JCOLS,
        rows = JROWS,
        fps = 15,
        still = 2.5f,
        loop = LOOP,
        note = "a humpback and its calf glide through deep blue water, singing rings into the dark under swaying light",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A humpback whale and its calf glide through deep blue water, sound rings of its song spreading ahead under swaying
 * light shafts.
 *
 * @param seed places the drifting snow, the light shafts and the mottling of the skin.
 */
@Composable
public fun AsciiWhaleSong(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A humpback whale and its calf gliding through deep blue water, singing",
) {
    val piece = remember(seed) { whaleSongPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
