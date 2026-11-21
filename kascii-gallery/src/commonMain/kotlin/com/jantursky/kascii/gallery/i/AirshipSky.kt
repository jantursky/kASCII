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
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 30f
private const val SUN_X = 150f
private const val SUN_Y = 27f
private const val SHIP_X = 82f
private const val SHIP_Y = 16f
private const val ENV_RX = 42f
private const val ENV_RY = 7.2f

/** Round cloud puffs spaced around a ring [width] cells long, their tops around row [top]. */
private class Puffs(
    seed: Int,
    salt: Int,
    count: Int,
    val width: Float,
    top: Float,
    rMin: Float,
    rMax: Float,
) {
    private val px = FloatArray(count)
    private val py = FloatArray(count)
    private val pr = FloatArray(count)

    init {
        val rng = irng(seed, salt)
        for (k in 0 until count) {
            pr[k] = rMin + (rMax - rMin) * rng.float()
            px[k] = (k + 0.3f * rng.float()) * width / count
            py[k] = top + (rng.float() - 0.5f) * 3f + pr[k] * 0.12f
        }
    }

    /** The row of the cloud top above column [x] with the ring shifted right by [shift]. */
    fun edge(
        x: Float,
        shift: Float,
    ): Float {
        var best = IROWS + 2f
        for (k in px.indices) {
            var dx = x - px[k] - shift
            dx -= kotlin.math.floor(dx / width + 0.5f) * width
            val r = pr[k]
            if (abs(dx) >= r) continue
            val e = py[k] - sqrt(r * r - dx * dx) / ROW_U
            if (e < best) best = e
        }
        return best
    }
}

private class Airship(
    seed: Int,
) {
    private val far = ILattice(seed, 71, 4)
    private val mid = Puffs(seed, 73, 13, 240f, 34f, 9f, 21f)
    private val near = Puffs(seed, 77, 12, 240f, 44f, 12f, 22f)
    private val base = IFrame()
    private val frame = IFrame()
    private val palette: IPalette
    private val fabric = col("#b8483a")
    private val fabricDark = col("#2a1020")
    private val brass = col("#d8a040")
    private val wood = col("#6a3a22")
    private val warm = col("#ffc060")
    private val sun = col("#ffd890")
    private val cloudDark = col("#3a2650")
    private val cloudLit = col("#ff9a6a")

    init {
        buildSky()
        palette =
            IPalette(
                ramp(8, col("#0c0820"), col("#1e1648"), col("#4a2a6a")) +
                    ramp(8, col("#4a2a6a"), col("#9a3a72"), col("#e0566a")) +
                    ramp(8, col("#e0566a"), col("#ff8a40"), col("#ffc060"), col("#fff0c8")) +
                    ramp(8, col("#1a1030"), cloudDark, col("#8a5a8a"), col("#d8a0b0")) +
                    tones(cloudLit, 6) +
                    tones(fabric, 8) +
                    tones(brass, 7) +
                    tones(wood, 6) +
                    tones(warm, 5) +
                    ramp(4, col("#9a9aaa"), col("#e8e8f0")) +
                    listOf(Col(1f, 1f, 1f), col("#0a0610")),
            )
    }

    private fun buildSky() {
        for (y in 0 until IROWS) {
            for (x in 0 until ICOLS) {
                val i = y * ICOLS + x
                val h = y / 34f
                val c =
                    if (h < 0.4f) {
                        col("#1e1648").mix(col("#4a2a6a"), h / 0.4f)
                    } else if (h < 0.7f) {
                        col("#4a2a6a").mix(col("#e0566a"), (h - 0.4f) / 0.3f)
                    } else {
                        col("#e0566a").mix(col("#ffc060"), clamp01((h - 0.7f) / 0.3f))
                    }
                base.r[i] = c.r
                base.g[i] = c.g
                base.b[i] = c.b
                base.cov[i] = if (h > 0.75f) 1.55f + 0.45f * clamp01((h - 0.75f) / 0.25f) else 0.3f + 0.8f * h
            }
        }
        base.glow(SUN_X, SUN_Y, 34f, col("#ff9a50"), 0.35f)
        base.glow(SUN_X, SUN_Y, 12f, sun, 0.6f)
        base.ellipse(SUN_X, SUN_Y, 7f, 3f) { a, b -> sun.mix(Col(1f, 1f, 1f), clamp01(0.8f - a * a - b * b)) }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val u = t / LOOP
        frame.copyFrom(base)
        clouds(far, u, 4f, 0.025f, 14, 30, 0.62f, 0.9f, 0.6f)
        sea(mid, u, 1, 0.75f)
        ship(t, u)
        sea(near, u, 2, 0.55f)
        frame.output(grid, palette)
    }

    /**
     * One layer of cloud between [y0] and [y1], scrolling right by one lattice period ([period] units at [scale]
     * per cell) per loop. [cut] is the density threshold, [light] how strongly the sun catches its edges, [solid]
     * its opacity.
     */
    private fun clouds(
        lat: ILattice,
        u: Float,
        period: Float,
        scale: Float,
        y0: Int,
        y1: Int,
        cut: Float,
        light: Float,
        solid: Float,
    ) {
        val shift = u * period
        val mid = (y0 + y1) / 2f
        val half = (y1 - y0) / 2f
        for (y in y0 until y1) {
            val band = 1f - ((y - mid) / half).let { it * it }
            for (x in 0 until ICOLS) {
                val sx = x * scale - shift
                val sy = y * scale * 7f + period * 3f
                val d = (lat.fbm(sx, sy, 3) - cut) * band
                if (d <= 0f) continue
                val i = y * ICOLS + x
                // Edges toward the sun (down and right) catch the light.
                val toward = (lat.fbm(sx + scale * 3f, sy + scale * 7f, 2) - cut) * band
                val edge = clamp01((d - toward) * 9f + 0.3f)
                val sunNear = gauss(dist(x.toFloat(), y.toFloat(), SUN_X, SUN_Y), 0f, 50f)
                val lit = clamp01(edge * light * (0.5f + 0.7f * sunNear) + 0.15f * (y - y0) / (y1 - y0))
                val body = cloudDark.mix(col("#8a5a8a"), 0.25f * (y - y0) / (y1 - y0))
                val c = body.mix(cloudLit, lit).mix(col("#fff0c8"), clamp01(lit - 0.75f) * 1.5f)
                val dens = clamp01(d * 9f)
                frame.r[i] += (c.r - frame.r[i]) * dens * solid
                frame.g[i] += (c.g - frame.g[i]) * dens * solid
                frame.b[i] += (c.b - frame.b[i]) * dens * solid
                frame.cov[i] = if (dens > 0.55f) 1.55f + 0.45f * dens else frame.cov[i].coerceAtLeast(dens * 1.6f)
                frame.ch[i] = NONE
            }
        }
    }

    /**
     * A sea of cloud tops built from round puffs, scrolling [laps] widths of the puff ring per loop, rim-lit by the
     * sun behind. [k] darkens the farther sea.
     */
    private fun sea(
        puffs: Puffs,
        u: Float,
        laps: Int,
        k: Float,
    ) {
        val shift = fract(u * laps) * puffs.width
        for (x in 0 until ICOLS) {
            val edge = puffs.edge(x + 0.5f, shift)
            val sunNear = gauss(x.toFloat(), SUN_X, 40f)
            for (y in edge.toInt().coerceAtLeast(0) until IROWS) {
                val i = y * ICOLS + x
                val depth = y + 0.5f - edge
                if (depth < -0.5f) continue
                val rim = gauss(depth, 0f, 1.4f) * (0.45f + 0.55f * sunNear)
                val body = cloudDark.mix(col("#8a5a8a"), clamp01(0.35f - 0.04f * depth)).scale(k)
                val c =
                    body
                        .mix(
                            cloudLit,
                            clamp01(rim * 1.1f),
                        ).mix(col("#fff0c8"), clamp01(rim - 0.6f) * 1.6f * sunNear)
                frame.r[i] = c.r
                frame.g[i] = c.g
                frame.b[i] = c.b
                frame.cov[i] = if (depth < 0.5f) 1.6f else 2f
                frame.ch[i] = NONE
            }
        }
    }

    private fun ship(
        t: Float,
        u: Float,
    ) {
        val bob = 0.7f * sin(ITAU * u * 2f)
        val cx = SHIP_X + 2.5f * sin(ITAU * u)
        val cy = SHIP_Y + bob
        // Rigging from the envelope down to the gondola.
        val gy = cy + ENV_RY + 3f
        for (k in 0 until 5) {
            val ex = cx - 18f + k * 9f
            val gx = cx - 10f + k * 5f
            frame.line(ex, cy + ENV_RY * 0.8f, gx, gy - 1f, col("#4a3040"), 1f)
        }
        // Swept tail fins above and below the tail.
        val tx = cx + ENV_RX * 0.55f
        for (side in -1..1 step 2) {
            for (r in 0..5) {
                val y = cy + side * (ENV_RY * 0.5f + r)
                val x0 = tx + 4f + r * 2.6f
                val x1 = cx + ENV_RX + 3f - r * 0.4f
                for (x in x0.toInt()..x1.toInt()) {
                    val k = 0.45f + 0.4f * (x - x0) / (x1 - x0 + 1f)
                    val c = fabric.scale(k).mix(col("#ffb070"), if (side > 0) 0.2f else 0.05f)
                    frame.put(x, y.toInt(), c, 2f, if (x == x0.toInt()) (if (side < 0) '▟' else '▜') else NONE)
                }
            }
        }
        // The envelope: a long shaded spindle with seams, straps and brass nose and tail caps.
        for (y in (cy - ENV_RY - 1).toInt()..(cy + ENV_RY + 1).toInt()) {
            for (x in (cx - ENV_RX - 1).toInt()..(cx + ENV_RX + 1).toInt()) {
                val a = (x + 0.5f - cx) / ENV_RX
                val b = (y + 0.5f - cy) / ENV_RY
                // Fuller toward the nose (left), tapering to the tail.
                val taper = if (a > 0f) 1f - 0.35f * a * a else 1f - 0.05f * a * a
                val bb = b / taper
                val d2 = a * a + bb * bb
                if (d2 > 1f) continue
                val nz = sqrt(1f - d2)
                // Lit from the low sun on the right and below, dark on top-left.
                val k = clamp01(0.12f + 0.6f * nz - 0.2f * bb + 0.1f * a)
                var c = fabricDark.mix(fabric, k)
                val rim = smooth(0.72f, 1f, d2) * clamp01(a * 0.8f + bb * 0.6f + 0.1f)
                c = c.mix(col("#ffb070"), rim * 0.9f)
                var glyph = NONE
                if (fract(a * 7f) < 0.07f) c = c.scale(0.72f)
                if (abs(abs(bb) - 0.62f) < 0.05f) c = c.scale(0.8f)
                if (abs(bb) < 0.07f) {
                    c = brass.scale(0.5f + 0.5f * k)
                    glyph = '▄'
                }
                if (a < -0.9f || (a > 0.82f && a < 0.86f)) c = brass.scale(0.45f + 0.55f * k)
                frame.put(x, y, c, if (d2 > 0.93f) 1.8f else 2f, glyph)
            }
        }
        // Name band.
        frame.text((cx - 10f).toInt(), (cy - 3f).toInt(), "KASCII", brass.mix(Col(1f, 1f, 1f), 0.3f))
        // The gondola: a wooden cabin with lit portholes, a brass rail and a little stack puffing steam.
        val g0 = (cx - 13f).toInt()
        val g1 = (cx + 12f).toInt()
        val gyi = gy.toInt()
        for (x in g0..g1) {
            frame.put(x, gyi - 1, brass.scale(0.8f), 1f, '─')
            frame.put(x, gyi, wood.scale(0.85f + 0.2f * (x - g0) / (g1 - g0)), 2f)
            frame.put(x, gyi + 1, wood.scale(0.6f), 2f)
            frame.put(x, gyi + 2, wood.scale(0.4f), 2f, '▀')
        }
        frame.put(g0 - 1, gyi, wood.scale(0.7f), 2f, '▐')
        frame.put(g0 - 1, gyi + 1, wood.scale(0.5f), 2f, '▝')
        for (x in g0 + 2 until g1 - 1 step 4) {
            frame.put(x, gyi, warm, 2f, '●')
            frame.light(gyi * ICOLS + x + 1, warm, 0.15f)
        }
        frame.glow(cx, gy + 0.5f, 9f, warm, 0.12f)
        for (y in gyi - 3..gyi - 1) frame.put(g1 - 4, y, col("#3a2a2a"), 2f)
        for (p in 0 until 6) {
            val age = fract(u * 10f + p / 6f)
            val px = g1 - 4 + age * 14f
            val py = gyi - 4f - age * 4f
            val i = py.toInt() * ICOLS + px.toInt()
            if (i in 0 until ICELLS) {
                frame.haze(i, Col(0.85f, 0.8f, 0.85f), 0.6f * (1f - age))
                frame.ch[i] =
                    if (age < 0.3f) {
                        '●'
                    } else if (age < 0.6f) {
                        '•'
                    } else {
                        '·'
                    }
            }
        }
        // Spinning propellers: one on the gondola's tail, one on a strut behind the envelope.
        prop(g1 + 3f, gyi + 0.5f, 2.8f, t * 7f)
        prop(cx + ENV_RX + 6f, cy, 5.5f, t * 5f)
    }

    /** A propeller seen edge-on: a blur disc and a blade whose length swings with its [turns]. */
    private fun prop(
        x: Float,
        y: Float,
        r: Float,
        turns: Float,
    ) {
        val a = ITAU * turns
        val ry = r / ROW_U
        // Hub and shaft.
        frame.put(x.toInt() - 1, y.toInt(), brass, 2f, '═')
        for (yy in (y - ry - 1).toInt()..(y + ry + 1).toInt()) {
            val dy = (yy + 0.5f - y) * ROW_U
            if (abs(dy) > r) continue
            frame.light(yy * ICOLS + x.toInt(), Col(0.6f, 0.55f, 0.6f), 0.25f)
        }
        val len = r * cos(a)
        val n = (abs(len) / ROW_U + 1).toInt()
        for (k in -n..n) {
            val yy = y + len / ROW_U * k / n
            val glyph =
                if (abs(cos(a)) > 0.7f) {
                    '│'
                } else if (sin(a) * cos(a) > 0f) {
                    '/'
                } else {
                    '\\'
                }
            frame.put(
                x.toInt() + (
                    if (abs(cos(a)) >
                        0.7f
                    ) {
                        0
                    } else {
                        (k * sin(a) * 0.6f).toInt()
                    }
                ),
                yy.toInt(),
                col("#c8c0c8"),
                2f,
                glyph,
            )
        }
        frame.put(x.toInt(), y.toInt(), brass, 2f, '●')
    }
}

/**
 * A steampunk airship cruising through layered sunset clouds: a ribbed crimson envelope with brass bands, a wooden
 * gondola with lit portholes and a puffing stack, propellers spinning. Far, middle and near cloud banks slide past at
 * their own speeds, the nearest drifting in front of the ship, the sun low and glowing behind. The seed shapes the
 * clouds.
 */
public fun airshipSkyPiece(seed: Int = 0): AsciiPiece {
    val scene = Airship(seed)
    return asciiPiece(
        name = "airship sky",
        ground = Rgb.fromHex("#0c0820"),
        category = "scenes",
        cols = ICOLS,
        rows = IROWS,
        fps = 15,
        still = 6f,
        loop = LOOP,
        note = "a steampunk airship with spinning propellers drifting through layered sunset clouds",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A steampunk airship with spinning propellers drifting through layered sunset clouds.
 *
 * @param seed shapes the clouds.
 */
@Composable
public fun AsciiAirshipSky(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A steampunk airship drifting through sunset clouds, its propellers spinning",
) {
    val piece = remember(seed) { airshipSkyPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
