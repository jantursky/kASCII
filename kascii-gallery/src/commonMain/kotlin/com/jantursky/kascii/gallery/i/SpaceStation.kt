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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 24f
private const val SUN_X = 20f
private const val SUN_Y = 6f
private const val PLANET_X = 96f

/** Planet centre in rows, far below the frame, so only the limb shows. */
private const val PLANET_Y = 152.5f
private const val PLANET_R = 265f
private const val ST_X = 100f
private const val ST_Y = 18f
private const val RING_RX = 52f
private const val RING_RY = 9f
private const val RING_IN = 0.9f
private const val TUBE = 3.4f
private const val HUB_X0 = 97
private const val HUB_X1 = 103
private const val PORT_X = 104f
private const val PORT_Y = 13f

private class Station(
    seed: Int,
) {
    private val lat = ILattice(seed, 3)
    private val cloudLat = ILattice(seed, 7, 8)
    private val stars = IStars(seed, 5, 190, 36) { x, y -> planetDist(x, y) > -4f }
    private val base = IFrame()
    private val frame = IFrame()
    private val palette: IPalette
    private val planetCells: IntArray
    private val planetDepth = FloatArray(ICELLS)
    private val planetLight = FloatArray(ICELLS)
    private val planetU = FloatArray(ICELLS)
    private val planetV = FloatArray(ICELLS)
    private val planetLand = FloatArray(ICELLS)
    private val sun = col("#ffe6b0")
    private val metal = col("#a9b0c2")
    private val metalDark = col("#3a4054")
    private val panel = col("#2c4fb8")
    private val gold = col("#d8a848")
    private val warm = col("#ffc070")
    private val engine = col("#7fc4ff")

    init {
        buildSky(seed)
        val cells = ArrayList<Int>()
        for (y in 0 until IROWS) {
            for (x in 0 until ICOLS) {
                val d = planetDist(x, y)
                if (d > 0f) continue
                val i = y * ICOLS + x
                planetDepth[i] = -d
                val nx = (x + 0.5f - PLANET_X) / PLANET_R
                val ny = (y + 0.5f - PLANET_Y) * ROW_U / PLANET_R
                val nz = sqrt(max(0f, 1f - nx * nx - ny * ny))
                val l = (-0.75f * nx - 0.35f * ny - 0.2f * nz) / 0.85f
                planetLight[i] = smooth(-0.08f, 0.55f, l)
                planetU[i] = nx * 34f
                planetV[i] = nz * 34f
                planetLand[i] = smooth(0.54f, 0.6f, lat.fbm(nx * 30f + 5f, nz * 30f + 11f, 4))
                cells.add(i)
            }
        }
        planetCells = cells.toIntArray()
        palette =
            IPalette(
                ramp(8, col("#020309"), col("#141a38"), col("#3a3a70")) +
                    tones(col("#6a3aa0"), 7) +
                    tones(col("#2a7a96"), 7) +
                    tones(col("#1d5fc8"), 9) +
                    tones(col("#5e8a46"), 7) +
                    tones(col("#a08a5a"), 5) +
                    ramp(6, col("#5a6a8a"), col("#dce6f6"), col("#ffffff")) +
                    tones(col("#5ad0ff"), 7) +
                    tones(sun, 7) +
                    tones(metal, 8) +
                    tones(panel, 7) +
                    tones(gold, 6) +
                    tones(warm, 6) +
                    listOf(col("#ff3a3a"), col("#3aff7a"), engine, col("#ffffff")),
            )
    }

    /** Signed distance of a cell from the limb, in width units: negative inside the planet. */
    private fun planetDist(
        x: Int,
        y: Int,
    ): Float = dist(x + 0.5f, y + 0.5f, PLANET_X, PLANET_Y) - PLANET_R

    private fun buildSky(seed: Int) {
        val n = ILattice(seed, 9)
        for (y in 0 until IROWS) {
            for (x in 0 until ICOLS) {
                val i = y * ICOLS + x
                // A faint nebula band running from top right toward the sun.
                val band = gauss(y + (x - 192f) * 0.09f, 6f, 9f)
                val v = n.fbm(x * 0.035f, y * 0.09f, 4)
                val neb = band * smooth(0.45f, 0.75f, v) * 0.55f
                val purple = n.at(x * 0.02f + 30f, y * 0.05f)
                if (neb > 0.04f) {
                    val c = col("#6a3aa0").mix(col("#2a7a96"), purple)
                    base.r[i] = c.r * (0.4f + neb)
                    base.g[i] = c.g * (0.4f + neb)
                    base.b[i] = c.b * (0.4f + neb)
                    base.cov[i] = neb * 0.7f
                }
            }
        }
        base.glow(SUN_X, SUN_Y, 22f, Col(0.5f, 0.42f, 0.3f), 0.45f)
        base.glow(SUN_X, SUN_Y, 7f, sun, 0.9f)
        base.ellipse(SUN_X, SUN_Y, 3.6f, 1.5f) { a, b -> sun.mix(Col(1f, 1f, 1f), 1f - a * a - b * b) }
        // Lens rays.
        for (k in -14..14) {
            if (abs(k) < 4) continue
            val f = 1f - abs(k) / 15f
            base.put((SUN_X + k).toInt(), SUN_Y.toInt(), sun.scale(0.5f + 0.5f * f), f, '─')
        }
        for (k in -4..4) {
            if (abs(k) < 2) continue
            base.put(SUN_X.toInt(), (SUN_Y + k).toInt(), sun.scale(0.7f), 1f - abs(k) / 5f, '│')
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val u = t / LOOP
        frame.copyFrom(base)
        stars.draw(frame, u)
        planet(u)
        val rot = ITAU * u
        ring(rot, back = true)
        spokes(rot, back = true)
        hub(t)
        spokes(rot, back = false)
        ring(rot, back = false)
        panels(u)
        ship(t)
        frame.output(grid, palette)
    }

    private fun planet(u: Float) {
        val drift = u * 8f
        for (i in planetCells) {
            val x = i % ICOLS
            val y = i / ICOLS
            val depth = planetDepth[i]
            val l = planetLight[i]
            val land = planetLand[i]
            val cloud = smooth(0.5f, 0.74f, cloudLat.fbm(planetU[i] * 1.2f + drift, planetV[i] * 1.6f, 3))
            var r = 0.03f + 0.05f * l
            var g = 0.08f + 0.2f * l
            var b = 0.2f + 0.48f * l
            r += (0.2f * l + 0.03f - r) * land
            g += (0.3f * l + 0.04f - g) * land
            b += (0.14f * l + 0.04f - b) * land
            val w = cloud * (0.2f + 0.75f * l)
            r += (0.85f * l + 0.04f - r) * w
            g += (0.9f * l + 0.05f - g) * w
            b += (0.98f * l + 0.08f - b) * w
            // Atmospheric scattering near the limb.
            val limb = gauss(depth, 0f, 5f) * (0.25f + 0.75f * l)
            r += 0.12f * limb
            g += 0.4f * limb
            b += 0.6f * limb
            frame.r[i] = r
            frame.g[i] = g
            frame.b[i] = b
            frame.cov[i] = 2f
            frame.ch[i] = NONE
            // City lights on the night side.
            if (l < 0.2f && land > 0.5f && cloud < 0.3f && lat.at(x * 0.7f, y * 1.3f + 40f) > 0.7f) {
                frame.put(x, y, warm.scale(0.75f + (0.2f - l)), 1f, if ((x + y) % 3 == 0) '•' else '·')
            }
        }
        // The glow of the atmosphere above the limb.
        for (y in 26 until IROWS) {
            for (x in 0 until ICOLS) {
                val d = planetDist(x, y)
                if (d <= 0f || d > 14f) continue
                val i = y * ICOLS + x
                val l = clamp01(1.05f - (x - 20f) / 150f)
                frame.light(i, Col(0.25f, 0.65f, 1f), gauss(d, 0f, 4.5f) * (0.15f + 0.85f * l))
            }
        }
    }

    /** Draws the near ([back] false) or far half of the rotating ring, a tube of constant width around an ellipse. */
    private fun ring(
        rot: Float,
        back: Boolean,
    ) {
        val ryu = RING_RY * ROW_U
        for (y in (ST_Y - RING_RY - 3).toInt()..(ST_Y + RING_RY + 3).toInt()) {
            for (x in (ST_X - RING_RX - 6).toInt()..(ST_X + RING_RX + 6).toInt()) {
                if (x < 0 || x >= ICOLS || y < 0 || y >= IROWS) continue
                val a = (x + 0.5f - ST_X) / RING_RX
                val b = (y + 0.5f - ST_Y) * ROW_U / ryu
                if ((b < 0f) != back) continue
                val e = sqrt(a * a + b * b)
                if (e < 0.5f) continue
                val gx = a / (RING_RX * e)
                val gy = b / (ryu * e)
                val gl = sqrt(gx * gx + gy * gy)
                val s = (e - 1f) / gl / TUBE
                if (s < -1f || s > 1f) continue
                val nz = sqrt(1f - s * s)
                val nx = gx / gl * s
                val ny = gy / gl * s
                var k = clamp01(-0.55f * nx - 0.6f * ny + 0.58f * nz) * 0.85f + 0.12f
                if (back) k *= 0.7f
                val th = atan2(b, a)
                val seg = fract((th - rot) / (ITAU / 18f))
                var c = metalDark.mix(metal.mix(Col(1f, 1f, 1f), 0.25f), k)
                var cov = if (abs(s) > 0.8f) 1.75f else 2f
                if (seg < 0.08f) {
                    c = c.scale(0.55f)
                } else if (abs(s + 0.1f) < 0.28f && seg in 0.2f..0.85f && ((seg * 9f).toInt() and 1) == 0) {
                    c = warm.scale(if (back) 0.6f else 0.95f)
                    cov = 2f
                }
                frame.put(x, y, c, cov)
            }
        }
    }

    private fun spokes(
        rot: Float,
        back: Boolean,
    ) {
        for (k in 0 until 4) {
            val th = rot * 1f + k * ITAU / 4f + 0.4f
            val s = sin(th)
            if ((s < 0f) != back) continue
            val ex = ST_X + RING_RX * RING_IN * cos(th)
            val ey = ST_Y + RING_RY * RING_IN * s
            val c = if (back) metal.scale(0.5f) else metal.scale(0.85f)
            frame.segment(ST_X, ST_Y, ex, ey, 0.8f) { e -> c.scale(1f - 0.25f * e) }
        }
    }

    private fun hub(t: Float) {
        // Spine through the hub.
        for (y in 2..32) {
            val w = if (y in 12..24) 0 else 1
            for (x in HUB_X0 + 2 - w..HUB_X1 - 2 + w) {
                if (y in 12..24) continue
                val k = 0.45f + 0.45f * (HUB_X1 - x) / 6f
                frame.put(x, y, metalDark.mix(metal, k * 0.8f), 2f, if (y % 4 == 0) '═' else NONE)
            }
        }
        // The hub drum.
        frame.ellipse(ST_X, ST_Y, 8.5f, 6.2f) { a, b ->
            val k = 0.55f - 0.45f * a - 0.25f * b
            val c = metalDark.mix(metal, clamp01(k))
            if (abs(b) < 0.12f) c.mix(gold, 0.6f) else c
        }
        frame.ellipse(ST_X, ST_Y, 3.5f, 2.2f) { a, b -> metal.mix(Col(1f, 1f, 1f), clamp01(0.5f - a - b)) }
        // Docking collar and its lights.
        for (y in 12..14) frame.put(HUB_X1 + 1, y, gold, 2f)
        val blink = fract(t / 1.5f) < 0.35f
        frame.put(PORT_X.toInt() - 1, PORT_Y.toInt() - 1, if (blink) col("#3aff7a") else metalDark, 1f, '●')
        if (blink) frame.glow(PORT_X - 0.5f, PORT_Y - 0.5f, 2.5f, col("#3aff7a"), 0.5f)
        val red = fract(t / 2f + 0.5f) < 0.2f
        frame.put(ST_X.toInt(), 1, if (red) col("#ff3a3a") else metalDark, 1f, '●')
        if (red) frame.glow(ST_X + 0.5f, 1.5f, 3f, col("#ff3a3a"), 0.6f)
        // A small radiator fin and dish on the lower spine.
        for (x in 92..108) frame.put(x, 30, metal.scale(0.6f), 1f, '─')
        frame.put(99, 33, metal, 1f, '╱')
        frame.put(100, 33, metal, 1f, '╲')
    }

    /** Solar arrays at the top of the spine, with a glint sweeping across them. */
    private fun panels(u: Float) {
        val glintX = 58f + 84f * fract(u * 2f)
        for (side in 0..1) {
            val x0 = if (side == 0) 64 else 104
            val x1 = if (side == 0) 96 else 136
            for (y in 3..6) {
                for (x in x0..x1) {
                    val edge = y == 3 || y == 6
                    val cellLine = (x - x0) % 4 == 0
                    var c = panel.scale(0.75f + 0.25f * (x - 60f) / 80f)
                    val g = gauss(x.toFloat() - (y - 3) * 2f, glintX, 2.6f)
                    c = c.mix(Col(0.85f, 0.92f, 1f), g * 0.85f)
                    val glyph =
                        if (edge) {
                            '─'
                        } else if (cellLine) {
                            '│'
                        } else {
                            NONE
                        }
                    frame.put(x, y, if (edge || cellLine) gold.scale(0.65f).mix(c, 0.3f) else c, 2f, glyph)
                }
            }
            for (x in (
                if (side ==
                    0
                ) {
                    96
                } else {
                    102
                }
            )..(if (side == 0) 98 else 104)) {
                frame.put(x, 5, metal.scale(0.7f), 2f, '─')
            }
        }
        if (glintX in 64f..136f && glintX !in 97f..103f) {
            frame.glow(glintX + 3f, 4.5f, 3.5f, Col(0.9f, 0.95f, 1f), 0.8f)
            frame.put((glintX + 3f).toInt(), 4, Col(1f, 1f, 1f), 1f, '*')
        }
        // Rim glints on the ring as the sun catches it.
        val gl = ITAU * fract(u * 3f)
        val gx = ST_X + RING_RX * cos(gl) * 0.99f
        val gy = ST_Y + RING_RY * sin(gl) * 0.99f
        if (sin(gl) < -0.2f && cos(gl) < 0.2f) {
            frame.glow(gx, gy, 3f, sun, 0.7f)
            frame.put(gx.toInt(), gy.toInt(), Col(1f, 1f, 1f), 1f, '*')
        }
    }

    /** A shuttle that coasts in from the right, docks at the hub, rests and leaves again. */
    private fun ship(t: Float) {
        val x: Float
        val y: Float
        var burn = 0f
        var rcs = false
        if (t < 9f) {
            val s = smooth(0f, 9f, t)
            val e = 1f - (1f - s) * (1f - s)
            x = 230f + (PORT_X + 1f - 230f) * e
            y = 6f + (PORT_Y - 6f) * e
            rcs = t > 5f && t < 8.6f && fract(t * 2.5f) < 0.5f
        } else if (t < 15f) {
            x = PORT_X + 1f
            y = PORT_Y
        } else {
            val s = (t - 15f) / 9f
            x = PORT_X + 1f + 140f * s * s
            y = PORT_Y - 9f * s * s
            burn = smooth(15.2f, 16.5f, t)
        }
        val xi = x.toInt()
        val yi = y.toInt()
        if (xi > ICOLS + 20) return
        // Body: a blunt nose on the left, cargo bay, engine block on the right.
        val body = metal.mix(Col(1f, 1f, 1f), 0.35f)
        for (k in 0 until 14) {
            frame.put(xi + k, yi, body, 2f, if (k == 0) '▐' else NONE)
            frame.put(xi + k, yi + 1, metal.scale(0.7f), 2f, if (k == 0) '▐' else NONE)
        }
        frame.put(xi + 1, yi, col("#5ad0ff"), 2f, '▀')
        frame.put(xi + 2, yi, col("#5ad0ff"), 2f, '▀')
        for (k in 5 until 12) frame.put(xi + k, yi - 1, metal.scale(0.85f), 2f, '▄')
        frame.put(xi + 11, yi - 2, metal.scale(0.7f), 1.9f, '▄')
        frame.put(xi + 12, yi - 2, metal.scale(0.7f), 1.9f, '▄')
        frame.put(xi + 14, yi, metalDark, 2f, '▌')
        frame.put(xi + 14, yi + 1, metalDark, 2f, '▌')
        for (k in 3 until 10 step 3) frame.put(xi + k, yi + 1, warm, 2f, '▄')
        if (burn > 0f) {
            frame.glow(xi + 16f, yi + 1f, 5f, engine, 0.9f * burn)
            for (k in 1..6) {
                frame.put(
                    xi + 14 + k,
                    yi + (k % 2),
                    engine.mix(Col(1f, 1f, 1f), 1f - k / 6f),
                    1f,
                    if (k <
                        3
                    ) {
                        '='
                    } else {
                        '-'
                    },
                )
            }
        } else if (t < 9f) {
            frame.glow(xi + 15f, yi + 1f, 2f, engine, 0.35f)
        }
        if (rcs) {
            frame.put(xi - 1, yi, Col(0.9f, 0.95f, 1f), 1f, '<')
            frame.put(xi - 2, yi, Col(0.6f, 0.7f, 0.9f), 0.6f, '-')
        }
        frame.glow(xi + 7f, yi.toFloat(), 3f, Col(0.3f, 0.32f, 0.38f), 0.12f)
        val nav = fract(t / 1.2f) < 0.3f
        if (nav) frame.put(xi + 13, yi - 1, col("#ff3a3a"), 1f, '•')
        // Lit approach markers while docking.
        if (t in 5f..15f) {
            frame.put(PORT_X.toInt(), PORT_Y.toInt() + 1, warm, 1f, '•')
        }
    }
}

/**
 * A rotating ring station hangs above the limb of a blue planet, the sun flaring behind it. The habitat ring turns with
 * its spokes and lit windows, a glint sweeps across the solar arrays, the night side of the planet glitters with city
 * lights, and a shuttle coasts in, docks at the hub, rests and burns away again. The seed shapes the stars, the nebula
 * and the continents.
 */
public fun spaceStationPiece(seed: Int = 0): AsciiPiece {
    val scene = Station(seed)
    return asciiPiece(
        name = "space station",
        ground = Rgb.fromHex("#020309"),
        category = "scenes",
        cols = ICOLS,
        rows = IROWS,
        fps = 15,
        still = 12f,
        loop = LOOP,
        note = "a rotating ring station above a planet's limb, a shuttle docking at the hub",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A rotating ring station above the limb of a blue planet, with a shuttle that docks and leaves again.
 *
 * @param seed shapes the stars, the nebula and the continents.
 */
@Composable
public fun AsciiSpaceStation(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A rotating ring space station above a planet, a shuttle docking at its hub",
) {
    val piece = remember(seed) { spaceStationPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
