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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val LOOP = 12f
private const val HORIZON = 22
private const val VP_X = 96f

/** Road half width in cells per row of depth below the horizon. */
private const val ROAD_K = 2.3f

/** Objects travel from this depth (world units) toward the camera and leave at [Z_NEAR]. */
private const val Z_FAR = 8f
private const val Z_NEAR = 0.55f
private const val SPAN = Z_FAR - Z_NEAR

/** The rows of depth at which an object stands at distance 1. */
private const val D1 = 24f
private const val CACTI = 12
private const val POLES = 6
private const val CAR_X = 120
private const val CAR_TOP = 32

private class Highway(
    seed: Int,
) {
    private val lat = ILattice(seed, 41, 16)
    private val base = IFrame()
    private val frame = IFrame()
    private val palette: IPalette
    private val stars = IStars(seed, 43, 70, 12)
    private val cactusX = FloatArray(CACTI)
    private val cactusP = FloatArray(CACTI)
    private val cactusH = FloatArray(CACTI)
    private val cactusArms = IntArray(CACTI)
    private val sand = col("#8a5a3a")
    private val asphalt = col("#23202e")
    private val cactus = col("#0e1a16")
    private val red = col("#ff2a2a")
    private val beam = col("#ffe8a8")
    private val sunCol = col("#ffb050")

    init {
        val rng = irng(seed, 45)
        for (c in 0 until CACTI) {
            val side = if (c % 2 == 0) -1f else 1f
            cactusX[c] = side * (1.7f + rng.float() * 3.6f)
            cactusP[c] = (c + rng.float() * 0.7f) / CACTI
            cactusH[c] = 0.75f + rng.float() * 0.5f
            cactusArms[c] = rng.int(3)
        }
        buildSky(seed)
        palette =
            IPalette(
                ramp(8, col("#07061a"), col("#1a1440"), col("#3a2462")) +
                    ramp(8, col("#4a2a6a"), col("#9a3a72"), col("#e0566a")) +
                    ramp(8, col("#e0566a"), col("#ff8a3a"), col("#ffc060"), col("#fff0c0")) +
                    tones(sand, 8) +
                    ramp(6, col("#0c0a14"), asphalt, col("#4a4658")) +
                    tones(cactus, 6) +
                    tones(col("#2a1a3a"), 5) +
                    tones(red, 6) +
                    tones(col("#e8c040"), 5) +
                    ramp(8, col("#5a5260"), col("#c8c0cc"), col("#f0ecf4")) +
                    tones(col("#ffe8a8"), 6) +
                    tones(col("#5a6a9a"), 5) +
                    listOf(Col(1f, 1f, 1f)),
            )
    }

    private fun buildSky(seed: Int) {
        val n = ILattice(seed, 47)
        for (y in 0 until HORIZON + 1) {
            for (x in 0 until ICOLS) {
                val i = y * ICOLS + x
                val h = y / HORIZON.toFloat()
                val c =
                    if (h < 0.45f) {
                        col("#0d0b2a").mix(col("#3a2462"), h / 0.45f)
                    } else if (h < 0.75f) {
                        col("#3a2462").mix(col("#b0406a"), (h - 0.45f) / 0.3f)
                    } else {
                        col("#b0406a").mix(col("#ff8a3a"), (h - 0.75f) / 0.25f)
                    }
                // Thin streaks of high cloud lit from below.
                val streak = smooth(0.55f, 0.75f, n.fbm(x * 0.02f, y * 0.45f, 3)) * gauss(h, 0.55f, 0.2f)
                base.r[i] = c.r + 0.3f * streak
                base.g[i] = c.g + 0.12f * streak
                base.b[i] = c.b + 0.05f * streak
                base.cov[i] = if (h > 0.82f) 1.55f + 0.5f * (h - 0.82f) / 0.18f else 0.3f + 0.8f * h + 0.4f * streak
            }
        }
        // The sun has just set behind the left mesa.
        base.glow(62f, HORIZON.toFloat(), 30f, sunCol, 0.45f)
        base.glow(62f, HORIZON.toFloat(), 10f, col("#fff0c0"), 0.55f)
        // Mesas and buttes along the horizon.
        val mesas =
            listOf(
                floatArrayOf(4f, 46f, 7f),
                floatArrayOf(40f, 58f, 4f),
                floatArrayOf(128f, 168f, 6f),
                floatArrayOf(170f, 182f, 3f),
                floatArrayOf(150f, 156f, 9f),
            )
        for (m in mesas) {
            for (x in m[0].toInt()..m[1].toInt()) {
                val edge = min(x - m[0], m[1] - x)
                val h = min(m[2], 1f + edge * 0.8f) + 0.6f * (lat.at(x * 0.3f, 4f) - 0.5f)
                for (y in (HORIZON - h).toInt()..HORIZON) {
                    val rim = y == (HORIZON - h).toInt()
                    val c =
                        if (rim) {
                            col("#9a3a72").mix(sunCol, 0.3f)
                        } else {
                            col("#2a1a3a").mix(
                                col("#9a3a72"),
                                0.35f * (y - HORIZON + h) / h,
                            )
                        }
                    base.put(x, y, c, if (rim) 1.8f else 2f)
                }
            }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val u = t / LOOP
        // Sky and mesas, with a heat shimmer over the last rows above the horizon.
        for (y in 0 until IROWS) {
            val row = y * ICOLS
            if (y > HORIZON) break
            val wob = if (y >= HORIZON - 3) (1.2f * sin(ITAU * u * 6f + y * 2.1f)).toInt() else 0
            for (x in 0 until ICOLS) {
                val sx = (x + wob).coerceIn(0, ICOLS - 1)
                val i = row + x
                val j = row + sx
                frame.r[i] = base.r[j]
                frame.g[i] = base.g[j]
                frame.b[i] = base.b[j]
                frame.cov[i] = base.cov[j]
                frame.ch[i] = base.ch[j]
            }
        }
        stars.draw(frame, u, 1f) { y -> 1f - y / 12f }
        ground(u)
        objects(u)
        car(u)
        frame.output(grid, palette)
    }

    private fun ground(u: Float) {
        val scroll = u * SPAN
        val dashes = 10f
        for (y in HORIZON + 1 until IROWS) {
            val d = y - HORIZON + 0.5f
            val z = D1 / d
            val half = ROAD_K * d
            val fog = clamp01(1f - d / 9f)
            for (x in 0 until ICOLS) {
                val i = y * ICOLS + x
                val off = x + 0.5f - VP_X
                val lane = off / half
                var c: Col
                var cov = 2f
                var glyph = NONE
                if (abs(lane) < 1f) {
                    val n = lat.at(z * 2.16f + scroll * 2.16f, lane * 3f + 30f)
                    c = asphalt.scale(0.85f + 0.3f * n)
                    // Edge lines and the dashed centre line.
                    if (abs(lane) > 0.93f) {
                        c = Col(0.75f, 0.72f, 0.7f).scale(0.55f + 0.45f * (1f - fog))
                    } else if (abs(off) < max(0.6f, d * 0.07f) && fract(z * dashes / SPAN - u * dashes) < 0.5f) {
                        c = col("#e8c040").scale(0.6f + 0.4f * (1f - fog))
                        if (d < 3f) glyph = '-'
                    }
                } else {
                    val wx = off / d
                    val n = lat.at(z * 2.16f + scroll * 2.16f, wx * 0.35f + 60f)
                    val k = 0.62f - 0.3f * clamp01(d / 26f) + 0.06f * (n - 0.5f)
                    c = sand.scale(k).mix(col("#9a3a72"), 0.2f)
                    if (n > 0.7f && d > 3f) {
                        // Scrub tufts.
                        c = c.scale(0.7f)
                        glyph = if (n > 0.78f) '▓' else '▒'
                        cov = 1.75f
                    }
                }
                // Dusk light fading into the distance.
                c = c.mix(col("#e0566a").scale(0.75f), fog * 0.6f)
                frame.r[i] = c.r
                frame.g[i] = c.g
                frame.b[i] = c.b
                frame.cov[i] = cov
                frame.ch[i] = glyph
            }
        }
        // Headlight pool on the road ahead of the car.
        for (y in HORIZON + 2 until CAR_TOP + 3) {
            val d = y - HORIZON + 0.5f
            val half = ROAD_K * d
            val reach = smooth(1.5f, 7f, d) * (1f - smooth(9f, 13f, d))
            if (reach <= 0f) continue
            for (x in 0 until ICOLS) {
                val lane = (x + 0.5f - VP_X) / half
                val k = gauss(lane, 0.45f, 0.32f) * reach
                if (k > 0.03f) frame.haze(y * ICOLS + x, beam.scale(0.75f), 0.45f * k)
            }
        }
    }

    /** Depth in rows below the horizon for an object at world distance [z]. */
    private fun depthOf(z: Float): Float = D1 / z

    private fun objects(u: Float) {
        // Far to near so nearer objects cover farther ones: cacti and poles share one ordering by their phase.
        val order = ArrayList<FloatArray>()
        for (c in 0 until CACTI) order.add(floatArrayOf(Z_FAR - SPAN * fract(cactusP[c] + u), c.toFloat()))
        for (p in 0 until POLES) order.add(floatArrayOf(Z_FAR - SPAN * fract(p.toFloat() / POLES + u), -1f - p))
        order.sortByDescending { it[0] }
        // Wires between pole tops first, behind everything nearer.
        val tops = FloatArray(POLES * 2)
        for (p in 0 until POLES) {
            val z = Z_FAR - SPAN * fract(p.toFloat() / POLES + u)
            val d = depthOf(z)
            tops[p * 2] = VP_X + 3.1f * ROAD_K * d
            tops[p * 2 + 1] = HORIZON + d - 1.25f * d
        }
        for (p in 0 until POLES) {
            val q = (p + POLES - 1) % POLES
            // Pole q is the one just behind pole p (one spacing farther).
            val zp = Z_FAR - SPAN * fract(p.toFloat() / POLES + u)
            val zq = Z_FAR - SPAN * fract(q.toFloat() / POLES + u)
            if (zq < zp) continue
            wire(tops[p * 2], tops[p * 2 + 1], tops[q * 2], tops[q * 2 + 1], fadeOf(zp))
        }
        for (o in order) {
            val z = o[0]
            val id = o[1].toInt()
            if (id >= 0) cactus(z, id) else pole(z)
        }
    }

    private fun fadeOf(z: Float): Float = smooth(Z_FAR, Z_FAR - 1.2f, z)

    private fun wire(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        fade: Float,
    ) {
        val n = (abs(x1 - x0) + 1f).toInt()
        val c = Col(0.12f, 0.08f, 0.16f).mix(col("#3a2462"), 1f - fade)
        for (k in 0..n) {
            val p = k.toFloat() / n
            val x = x0 + (x1 - x0) * p
            val sag = 4f * p * (1f - p) * (abs(y0 - y1) * 0.25f + 0.6f)
            val y = y0 + (y1 - y0) * p + sag
            val xi = x.toInt()
            val yi = y.toInt()
            if (xi !in 0 until ICOLS || yi !in 0 until IROWS) continue
            frame.put(xi, yi, c, 1f, if (y - yi < 0.5f) '-' else '_')
        }
    }

    private fun pole(z: Float) {
        val d = depthOf(z)
        val x = VP_X + 3.1f * ROAD_K * d
        val baseY = HORIZON + d
        val h = 1.25f * d
        val w = max(1, (d * 0.07f).toInt())
        val c = Col(0.1f, 0.06f, 0.12f).mix(col("#7a3a6a"), (1f - fadeOf(z)) * 0.8f)
        for (y in (baseY - h).toInt()..baseY.toInt()) {
            for (k in 0 until w) frame.put(x.toInt() + k, y, c, 2f, if (w == 1) '│' else NONE)
        }
        val arm = max(2, (d * 0.25f).toInt())
        for (k in -arm..arm) frame.put(x.toInt() + k, (baseY - h).toInt() + 1, c, 2f, if (d < 10f) '─' else '▀')
    }

    private fun cactus(
        z: Float,
        id: Int,
    ) {
        val d = depthOf(z)
        val x = VP_X + cactusX[id] * ROAD_K * d
        if (x < -20f || x > ICOLS + 20f) return
        val baseY = HORIZON + d
        val h = 0.85f * d * cactusH[id]
        val w = max(0.8f, d * 0.11f)
        val fade = fadeOf(z)
        val dark = cactus.mix(col("#3a2462"), 0.6f * (1f - clamp01(d / 14f)))
        val lit = col("#4a6a3a").mix(sunCol, 0.25f)

        fun col(e: Float): Col = dark.mix(lit, clamp01(-e) * 0.6f).mix(col("#b0406a").scale(0.6f), 1f - fade)
        frame.segment(x, baseY, x, baseY - h, w) { col(it) }
        frame.ellipse(x, baseY - h, w, w / ROW_U + 0.3f) { a, _ -> col(a) }
        if (cactusArms[id] >= 1) {
            val ay = baseY - h * 0.5f
            frame.segment(x, ay, x - w * 2.6f, ay, w * 0.75f) { col(it) }
            frame.segment(x - w * 2.6f, ay, x - w * 2.6f, ay - h * 0.3f, w * 0.75f) { col(it) }
        }
        if (cactusArms[id] == 2) {
            val ay = baseY - h * 0.62f
            frame.segment(x, ay, x + w * 2.4f, ay, w * 0.7f) { col(it) }
            frame.segment(x + w * 2.4f, ay, x + w * 2.4f, ay - h * 0.22f, w * 0.7f) { col(it) }
        }
    }

    /** The car from behind: roof, rear window catching the sunset, tail lights, plate and tyres. */
    private fun car(u: Float) {
        val bob = if (sin(ITAU * u * 9f) > 0.6f) 1 else 0
        val top = CAR_TOP + bob
        val x0 = CAR_X - 18
        val x1 = CAR_X + 18
        val body = col("#b83040")
        // Shadow under the car.
        frame.ellipse(CAR_X + 0.5f, top + 11.4f, 21f, 1.1f) { _, _ -> Col(0.05f, 0.04f, 0.07f) }
        // Roof, then the rear window between the pillars, catching a streak of sunset.
        for (x in x0 + 8..x1 - 8) frame.put(x, top, body.scale(0.75f).mix(col("#ff8a3a"), 0.3f), 2f, '▄')
        for (y in top + 1..top + 3) {
            val inset = 8 - (y - top)
            for (x in x0 + inset..x1 - inset) {
                val pillar = x <= x0 + inset + 1 || x >= x1 - inset - 1
                val k = (x - x0 - inset).toFloat() / (x1 - x0 - inset * 2)
                val glare = gauss(k + (y - top) * 0.1f, 0.35f, 0.08f)
                val c = if (pillar) body.scale(0.8f) else col("#140e2a").mix(col("#e0566a"), 0.15f + 0.75f * glare)
                frame.put(x, y, c, 2f)
            }
        }
        // Trunk lid, light bar, plate panel, bumper.
        for (y in top + 4..top + 9) {
            for (x in x0..x1) {
                val k = 0.75f + 0.25f * (1f - abs(x - CAR_X) / 18f) - 0.08f * (y - top - 4)
                var c = body.scale(k)
                var glyph = NONE
                if (y == top + 4) {
                    c = c.mix(col("#ffc060"), 0.35f)
                    glyph = '▄'
                }
                if (y == top + 8) c = col("#2a2232")
                if (y == top + 9) {
                    c = col("#0c0a14")
                    glyph = '▀'
                }
                frame.put(x, y, c, 2f, glyph)
            }
        }
        // Tail lights joined by a thin bar.
        for (y in top + 5..top + 6) {
            for (x in x0..x0 + 6) frame.put(x, y, red.mix(Col(1f, 0.8f, 0.7f), if (y == top + 5) 0.25f else 0f), 2f)
            for (x in x1 - 6..x1) frame.put(x, y, red.mix(Col(1f, 0.8f, 0.7f), if (y == top + 5) 0.25f else 0f), 2f)
        }
        for (x in x0 + 7..x1 - 7) frame.put(x, top + 5, red.scale(0.8f), 2f, '▄')
        val pulse = 0.85f + 0.15f * sin(ITAU * u * 3f)
        frame.glow(x0 + 3f, top + 6f, 6f, red, 0.45f * pulse)
        frame.glow(x1 - 3f, top + 6f, 6f, red, 0.45f * pulse)
        for (x in CAR_X - 3..CAR_X + 3) frame.put(x, top + 7, col("#140e2a"), 2f)
        frame.text(CAR_X - 2, top + 7, "K-66", col("#f0ecf4"))
        // Tyres.
        for (x in x0 + 1..x0 + 6) frame.put(x, top + 10, col("#0c0a14"), 2f)
        for (x in x1 - 6..x1 - 1) frame.put(x, top + 10, col("#0c0a14"), 2f)
        // Exhaust puffs.
        for (p in 0 until 4) {
            val age = fract(u * 6f + p / 4f)
            val px = x0 + 6 - age * 6f
            val py = top + 10f - age * 2.5f
            val i = py.toInt() * ICOLS + px.toInt()
            if (i in 0 until ICELLS) {
                frame.haze(i, Col(0.5f, 0.45f, 0.55f), 0.4f * (1f - age))
                frame.ch[i] = if (age < 0.4f) '·' else NONE
            }
        }
    }
}

/**
 * A car drives an endless desert highway at dusk, seen from behind: tail lights glowing, headlights pooling on the
 * asphalt, the dashed line streaming toward the camera. Saguaros and power poles with sagging wires sweep past, buttes
 * stand on the horizon in the afterglow of the set sun, heat shimmers over the road and the first stars come out. The
 * seed places the cacti, the clouds and the stars.
 */
public fun desertHighwayPiece(seed: Int = 0): AsciiPiece {
    val scene = Highway(seed)
    return asciiPiece(
        name = "desert highway",
        ground = Rgb.fromHex("#07061a"),
        category = "scenes",
        cols = ICOLS,
        rows = IROWS,
        fps = 15,
        still = 4f,
        loop = LOOP,
        note = "a car on an endless desert highway at dusk, cacti and power poles sweeping past",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A car on an endless desert highway at dusk, cacti and power poles sweeping past.
 *
 * @param seed places the cacti, the clouds and the stars.
 */
@Composable
public fun AsciiDesertHighway(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A car driving an endless desert highway at dusk past cacti and power poles",
) {
    val piece = remember(seed) { desertHighwayPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
