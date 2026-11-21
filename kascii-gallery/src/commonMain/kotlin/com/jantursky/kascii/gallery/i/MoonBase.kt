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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 20f
private const val EARTH_X = 158f
private const val EARTH_Y = 8f
private const val EARTH_R = 12f
private const val ROVER_Y = 40
private const val MAST_X = 112
private const val MAST_TOP = 12

private class Base(
    seed: Int,
) {
    private val lat = ILattice(seed, 21)
    private val cloudLat = ILattice(seed, 23, 8)
    private val base = IFrame()
    private val frame = IFrame()
    private val palette: IPalette
    private val ridge = IntArray(ICOLS)
    private val earthCells: IntArray
    private val earthLight = FloatArray(ICELLS)
    private val earthLand = FloatArray(ICELLS)
    private val earthU = FloatArray(ICELLS)
    private val earthV = FloatArray(ICELLS)
    private val stars: IStars
    private val shell = col("#d6d8de")
    private val shellDark = col("#2c2f3a")
    private val warm = col("#ffbe5c")
    private val red = col("#ff3b30")
    private val roverBody = col("#e8e4da")
    private val roverTrim = col("#e07a2c")

    /** The domes: centre x, base row, radius in width units, height in rows. */
    private val domes =
        listOf(
            floatArrayOf(52f, 34f, 21f, 8.5f),
            floatArrayOf(88f, 35f, 12f, 5f),
            floatArrayOf(22f, 35.5f, 9f, 4f),
        )

    init {
        for (x in 0 until ICOLS) {
            val h = lat.fbm(x * 0.025f + 3f, 1.5f, 4)
            ridge[x] = (29.5f - 9f * smooth(0.3f, 0.75f, h) - 2f * gauss(x.toFloat(), 150f, 22f)).toInt()
        }
        stars =
            IStars(seed, 25, 220, 28) { x, y ->
                y >= ridge[x] - 1 ||
                    dist(x + 0.5f, y + 0.5f, EARTH_X, EARTH_Y) < EARTH_R + 3f
            }
        val cells = ArrayList<Int>()
        for (y in 0 until IROWS) {
            for (x in 0 until ICOLS) {
                val nx = (x + 0.5f - EARTH_X) / EARTH_R
                val ny = (y + 0.5f - EARTH_Y) * ROW_U / EARTH_R
                val d2 = nx * nx + ny * ny
                if (d2 >= 1f) continue
                val i = y * ICOLS + x
                val nz = sqrt(1f - d2)
                earthLight[i] = smooth(-0.05f, 0.3f, -0.95f * nx - 0.1f * ny + 0.3f * nz)
                val w = 1f / (nz + 0.6f)
                earthU[i] = nx * w * 4f
                earthV[i] = ny * w * 2.6f
                earthLand[i] = smooth(0.5f, 0.58f, lat.fbm(nx * w * 3f + 40f, ny * w * 3f + 9f, 4))
                cells.add(i)
            }
        }
        earthCells = cells.toIntArray()
        buildGround()
        buildDomes()
        buildMast()
        palette =
            IPalette(
                ramp(16, col("#06070c"), col("#2a2b33"), col("#6e6f78"), col("#b8b9c0"), col("#eceef2")) +
                    tones(col("#1e5cc8"), 8) +
                    tones(col("#4a8a4a"), 5) +
                    tones(col("#a89060"), 4) +
                    tones(col("#5ab4ff"), 6) +
                    tones(warm, 8) +
                    tones(red, 6) +
                    tones(roverTrim, 6) +
                    tones(col("#8a9ab8"), 6) +
                    listOf(col("#ffffff"), col("#fff2c8")),
            )
    }

    private fun buildGround() {
        for (y in 0 until IROWS) {
            for (x in 0 until ICOLS) {
                val top = ridge[x]
                if (y < top) continue
                val i = y * ICOLS + x
                if (y < 30) {
                    // Far hills: lit on their left slopes.
                    val slope = ridge[min(ICOLS - 1, x + 2)] - ridge[max(0, x - 2)]
                    val k =
                        0.22f + 0.05f * slope + 0.12f * (lat.at(x * 0.2f, y * 0.6f + 50f) - 0.5f) - 0.04f * (y - top)
                    val c = Col(k * 0.95f, k * 0.96f, k * 1.05f + 0.02f)
                    base.put(x, y, c, if (y == top) 1.8f else 2f)
                    continue
                }
                // The plain, finer grain toward the viewer.
                val z = 8f / (y - 26f)
                val n = lat.fbm(x * 0.06f * z + 11f, y * 0.4f + 70f, 3)
                var k = 0.24f + 0.14f * (n - 0.5f) + 0.05f * (y - 30f) / 18f
                val craters =
                    listOf(
                        floatArrayOf(140f, 38f, 18f),
                        floatArrayOf(176f, 33f, 9f),
                        floatArrayOf(8f, 44f, 20f),
                        floatArrayOf(118f, 45.5f, 14f),
                        floatArrayOf(160f, 46f, 10f),
                    )
                for (c in craters) {
                    val a = (x + 0.5f - c[0]) / c[2]
                    val b = (y + 0.5f - c[1]) / (c[2] * 0.16f)
                    val e = a * a + b * b
                    if (e < 1f) k *= if (a < 0f) 0.55f + 0.2f * (1f + a) else 0.95f
                    if (e in 1f..1.6f) k += if (a > 0f) 0.12f else 0.04f
                }
                val c = Col(k * 0.96f, k * 0.97f, k * 1.03f + 0.01f)
                val grain = lat.at(x * 1.3f + 7f, y * 2.1f)
                base.put(x, y, c, if (grain > 0.82f) 1.75f else 2f)
            }
        }
        // Long shadows of the domes and the mast to the right, the sun is low on the left.
        for (d in domes) {
            for (y in d[1].toInt() - 1..d[1].toInt() + 2) {
                for (x in d[0].toInt()..min(ICOLS - 1, (d[0] + d[2] * 2.3f).toInt())) {
                    val i = y * ICOLS + x
                    val a = (x + 0.5f - d[0] - d[2] * 0.9f) / (d[2] * 1.5f)
                    val b = (y + 0.5f - d[1] - 0.3f) / 1.6f
                    if (a * a + b * b < 1f && base.cov[i] > 1f) {
                        base.r[i] *= 0.4f
                        base.g[i] *= 0.4f
                        base.b[i] *= 0.45f
                    }
                }
            }
        }
        for (x in MAST_X + 1..MAST_X + 26) {
            val i = 35 * ICOLS + x
            base.r[i] *= 0.45f
            base.g[i] *= 0.45f
            base.b[i] *= 0.5f
        }
    }

    private fun buildDomes() {
        // Connecting tubes first, the domes sit over their ends.
        tube(22f, 52f, 34)
        tube(52f, 88f, 34)
        for (d in domes) {
            val cx = d[0]
            val by = d[1]
            val rx = d[2]
            val ry = d[3]
            for (y in (by - ry).toInt() - 1..by.toInt()) {
                for (x in (cx - rx).toInt() - 1..(cx + rx).toInt() + 1) {
                    val a = (x + 0.5f - cx) / rx
                    val b = (y + 0.5f - by) / ry
                    val d2 = a * a + b * b
                    if (d2 > 1f || b > 0f) continue
                    val nz = sqrt(1f - d2)
                    val k = clamp01(-0.62f * a - 0.5f * b + 0.5f * nz)
                    var c = shellDark.mix(shell, k)
                    // Geodesic seams.
                    val lon = atan2(a, nz)
                    val lat = b
                    var glyph = NONE
                    if (fract(lon * 3.2f + 0.5f) < 0.14f) {
                        c = c.scale(0.78f)
                    }
                    if (fract(lat * 3f) < 0.18f && b < -0.2f) c = c.scale(0.85f)
                    // A rim of lit windows near the base.
                    if (b > -0.42f && b < -0.18f && fract(a * rx / 5f) < 0.4f && abs(a) < 0.85f) {
                        c = warm
                        glyph = '▄'
                    }
                    base.put(x, y, c, if (d2 > 0.9f) 1.8f else 2f, glyph)
                }
            }
            // Footing.
            for (x in (cx - rx).toInt()..(cx + rx).toInt()) base.put(x, by.toInt() + 1, shellDark.scale(1.3f), 2f, '▀')
        }
        // The main airlock with its open, lit door.
        for (y in 31..34) for (x in 64..67) base.put(x, y, shell.scale(0.7f), 2f)
        for (y in 32..34) for (x in 65..66) base.put(x, y, warm.mix(Col(1f, 1f, 1f), 0.3f), 2f)
    }

    private fun tube(
        x0: Float,
        x1: Float,
        y: Int,
    ) {
        for (x in x0.toInt()..x1.toInt()) {
            base.put(x, y - 1, shell.scale(0.8f), 2f, '▄')
            base.put(x, y, shellDark.mix(shell, 0.45f), 2f, if (x % 4 == 0) '│' else NONE)
        }
    }

    private fun buildMast() {
        val metal = col("#9a9ca8")
        for (y in MAST_TOP + 2..35) {
            val w = ((y - MAST_TOP) / 8)
            base.put(MAST_X - w, y, metal, 1f, '│')
            base.put(MAST_X + w + 1, y, metal.scale(0.6f), 1f, '│')
            if (y % 3 == 0 && w > 0) {
                for (x in MAST_X - w + 1..MAST_X + w) {
                    base.put(
                        x,
                        y,
                        metal.scale(0.75f),
                        1f,
                        if ((x + y) % 2 ==
                            0
                        ) {
                            '╱'
                        } else {
                            '╲'
                        },
                    )
                }
            }
        }
        // The dish, tilted toward the Earth.
        for (k in -5..5) {
            val x = MAST_X + 1 + k
            val y = MAST_TOP + 1 - (k + 5) / 4
            base.put(x, y, shell.scale(0.9f - 0.04f * k), 2f, if (k > -5 && k < 5) '▄' else '▗')
            base.put(x, y + 1, shellDark.mix(shell, 0.35f), 2f, '▀')
        }
        base.line(MAST_X + 1f, MAST_TOP + 0f, MAST_X + 4f, MAST_TOP - 2f, col("#c8c8d0"))
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val u = t / LOOP
        frame.copyFrom(base)
        stars.draw(frame, u, 1f) { y -> 0.5f + 0.5f * (1f - y / 28f) }
        earth(u)
        lights(t, u)
        rover(t, u)
        frame.output(grid, palette)
    }

    private fun earth(u: Float) {
        frame.glow(EARTH_X, EARTH_Y, EARTH_R * 1.1f, Col(0.12f, 0.3f, 0.7f), 0.35f)
        val drift = u * 8f
        for (i in earthCells) {
            val l = earthLight[i]
            val land = earthLand[i]
            val cloud = smooth(0.5f, 0.75f, cloudLat.fbm(earthU[i] + drift, earthV[i] * 1.6f + 3f, 3))
            var r = 0.02f + 0.12f * l
            var g = 0.04f + 0.41f * l
            var b = 0.09f + 0.79f * l
            r += (0.25f * l + 0.02f - r) * land
            g += (0.42f * l + 0.03f - g) * land
            b += (0.16f * l + 0.04f - b) * land
            val w = cloud * (0.15f + 0.8f * l)
            r += (0.92f * l + 0.02f - r) * w
            g += (0.95f * l + 0.03f - g) * w
            b += (l + 0.06f - b) * w
            frame.r[i] = r
            frame.g[i] = g
            frame.b[i] = b
            frame.cov[i] = if (l < 0.05f) 1.75f else 2f
            frame.ch[i] = NONE
        }
    }

    private fun lights(
        t: Float,
        u: Float,
    ) {
        // Beacon on the mast.
        val on = fract(t / 2f) < 0.25f
        frame.put(MAST_X + 4, MAST_TOP - 3, if (on) red else red.scale(0.3f), 1f, '●')
        if (on) frame.glow(MAST_X + 4.5f, MAST_TOP - 2.5f, 4f, red, 0.75f)
        // Warm spill from the airlock and the windows.
        frame.glow(65.5f, 34f, 6f, warm, 0.35f + 0.05f * sin(ITAU * u * 5f))
        for (d in domes) frame.glow(d[0], d[1] - d[3] * 0.3f, d[2] * 0.7f, warm, 0.1f)
        // One window blinks off now and then.
        if (fract(u * 2f) in 0.3f..0.55f) frame.put(46, 31, Col(0.2f, 0.18f, 0.16f), 2f, '▄')
    }

    /** A pressurised rover crossing the plain left to right, headlights on, kicking up dust, leaving tracks. */
    private fun rover(
        t: Float,
        u: Float,
    ) {
        val speed = (ICOLS + 60f) / LOOP
        val rx = -30f + speed * t
        // Tracks: each cell remembers when the rover passed and fades over half a loop, wrapping across the loop.
        for (x in 0 until ICOLS) {
            val passed = (x + 30f) / speed
            val age = fract((t - passed) / LOOP)
            val k = 1f - age / 0.6f
            if (k <= 0f) continue
            val i = (ROVER_Y + 3) * ICOLS + x
            val dark = 0.5f + 0.5f * (1f - k)
            frame.r[i] *= dark
            frame.g[i] *= dark
            frame.b[i] *= dark
            if (k > 0.3f && x % 2 == 0) frame.ch[i] = '='
        }
        val x0 = rx.toInt()
        // Headlight beam ahead and down to the ground.
        for (y in ROVER_Y - 1..ROVER_Y + 6) {
            for (x in x0 + 19..x0 + 60) {
                if (x < 0 || x >= ICOLS || y >= IROWS) continue
                val dx = (x - x0 - 18).toFloat()
                val dy = (y - ROVER_Y - 0.5f) * ROW_U
                val spread = 1.5f + dx * 0.22f
                val k = gauss(dy, dx * 0.08f, spread) * (1f - dx / 42f)
                if (k > 0.02f) frame.light(y * ICOLS + x, col("#fff2c8"), 0.8f * k)
            }
        }
        // Dust plume behind.
        for (p in 0 until 18) {
            val age = fract(u * 24f + p / 18f)
            val px = rx + 1f - age * 16f - (p % 3) * 1.5f
            val py = ROVER_Y + 3f - age * 4.5f * (0.6f + (p % 4) * 0.15f)
            val xi = px.toInt()
            val yi = py.toInt()
            if (xi !in 0 until ICOLS || yi !in 0 until IROWS) continue
            val i = yi * ICOLS + xi
            if (frame.cov[i] > 1.4f && yi < ROVER_Y) continue
            frame.haze(i, col("#8a8c94"), 0.5f * (1f - age))
            if (age < 0.5f) frame.ch[i] = if (age < 0.25f) '•' else '·'
        }
        if (x0 > ICOLS + 2 || x0 < -24) return
        // Body: a long cabin with a dark window band and an orange stripe.
        for (x in x0 + 2..x0 + 17) {
            val k = (x - x0).toFloat() / 17f
            frame.put(x, ROVER_Y - 2, roverBody.scale(0.85f + 0.15f * (1f - k)), 2f, if (x == x0 + 2) '▟' else '▄')
            frame.put(x, ROVER_Y - 1, roverBody.scale(0.95f - 0.1f * k), 2f)
            frame.put(x, ROVER_Y, roverTrim, 2f)
            frame.put(x, ROVER_Y + 1, roverBody.scale(0.55f), 2f, '▀')
        }
        for (x in x0 + 12..x0 + 16) {
            frame.put(
                x,
                ROVER_Y - 1,
                col("#1a2840").mix(col("#5ab4ff"), (x - x0 - 12) / 6f),
                2f,
            )
        }
        frame.put(x0 + 17, ROVER_Y - 1, roverBody, 2f, '▙')
        frame.put(x0 + 18, ROVER_Y, col("#fff2c8"), 2f, '▌')
        frame.glow(x0 + 18.5f, ROVER_Y + 0.5f, 2.5f, col("#fff2c8"), 0.9f)
        frame.put(x0 + 2, ROVER_Y, red, 2f, '▐')
        // Antenna and a little whip with a light.
        frame.put(x0 + 5, ROVER_Y - 3, col("#c8c8d0"), 1f, '│')
        frame.put(x0 + 5, ROVER_Y - 4, if (fract(t) < 0.5f) col("#5ab4ff") else col("#2a4060"), 1f, '•')
        // Wheels turning.
        val spin = "|/-\\"
        for (w in intArrayOf(4, 9, 14)) {
            val wx = x0 + w
            val g = spin[(fract(u * 60f + w * 0.1f) * 4f).toInt() and 3]
            frame.put(wx, ROVER_Y + 2, col("#30323a"), 2f)
            frame.put(wx + 1, ROVER_Y + 2, col("#30323a"), 2f)
            frame.put(wx, ROVER_Y + 2, col("#a0a2aa"), 2f, g)
            frame.put(wx + 1, ROVER_Y + 2, col("#60626a"), 2f, '▌')
        }
    }
}

/**
 * A moon base of three geodesic domes joined by tubes, a lattice mast with a dish and a blinking beacon, the Earth
 * hanging in the black sky. A pressurised rover crosses the regolith with its headlights on, wheels turning and dust
 * trailing behind it, its tracks fading slowly. The seed shapes the hills, the stars and the continents.
 */
public fun moonBasePiece(seed: Int = 0): AsciiPiece {
    val scene = Base(seed)
    return asciiPiece(
        name = "moon base",
        ground = Rgb.fromHex("#03040a"),
        category = "scenes",
        cols = ICOLS,
        rows = IROWS,
        fps = 15,
        still = 8f,
        loop = LOOP,
        note = "domes on the Moon under the Earth, a rover crossing the regolith with its headlights on",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A moon base of domes and a beacon mast under the Earth, a rover crossing the regolith.
 *
 * @param seed shapes the hills, the stars and the continents.
 */
@Composable
public fun AsciiMoonBase(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A moon base of domes under the Earth, a rover crossing the grey plain",
) {
    val piece = remember(seed) { moonBasePiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
