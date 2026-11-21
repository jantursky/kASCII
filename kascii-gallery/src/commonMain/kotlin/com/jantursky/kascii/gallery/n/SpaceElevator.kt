package com.jantursky.kascii.gallery.n

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
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 20f
private const val TX = 96f
private const val RADIUS = 300f
private const val CENTER_Y = 160f
private const val SUN_X = 150f
private const val ISLAND_Y = 37.5f

private class Elevator(
    seed: Int,
) {
    private val lat = NLattice(seed, 201)
    private val base = NFrame()
    private val frame = NFrame()
    private val palette: NPalette
    private val limb = FloatArray(NCOLS)
    private val stars: NStars
    private val lon = FloatArray(NCELLS)
    private val latitude = FloatArray(NCELLS)
    private val lit = FloatArray(NCELLS)
    private val space = col("#03040c")
    private val deep = col("#0a1238")
    private val air = col("#3a7ae0")
    private val sunCol = col("#ff9a48")
    private val sunCore = col("#fff4d8")
    private val ocean = col("#0a2a5a")
    private val night = col("#030a1c")
    private val cloud = col("#d0dcf0")
    private val steel = col("#c0ccdc")
    private val warm = col("#ffc46a")
    private val lagoon = col("#3ad0c0")

    init {
        for (x in 0 until NCOLS) {
            val dx = x + 0.5f - TX
            limb[x] = CENTER_Y - sqrt(RADIUS * RADIUS - dx * dx) / ROW_U
        }
        stars = NStars(seed, 203, 130, 34) { x, y -> y > limb[x] - 2f }
        buildSpace()
        buildPlanet()
        palette =
            NPalette(
                ramp(10, space, deep, col("#1a3080"), air, col("#a8d0ff")) +
                    ramp(10, col("#2a0c20"), col("#8a2a3a"), col("#e05a3a"), sunCol, sunCore) +
                    ramp(10, night, ocean, col("#1e5a9a"), col("#6aa0d0")) +
                    ramp(7, col("#1a2238"), col("#5a6a8a"), cloud, Col(1f, 1f, 1f)) +
                    ramp(5, col("#6a3a2a"), col("#e09060"), col("#ffd0a0")) +
                    ramp(7, col("#1a1e28"), col("#5a6474"), steel, Col(1f, 1f, 1f)) +
                    tones(warm, 6) +
                    tones(lagoon, 5) +
                    listOf(col("#2a8a3a"), col("#e8d090"), col("#ff3a3a"), col("#3aff7a")),
            )
    }

    private fun buildSpace() {
        for (y in 0 until NROWS) {
            for (x in 0 until NCOLS) {
                val h = limb[x] - (y + 0.5f)
                if (h < 0f) continue
                val i = y * NCOLS + x
                val sun = gauss(x + 0.5f, SUN_X, 40f)
                val side = clamp01((x - 40f) / 110f)
                // A thin shell of air along the limb, blazing orange by the sun and blue on the day side.
                val shell = exp(-h / 1.3f) * (0.6f + 0.4f * side)
                val wide = exp(-h / 6f) * (0.15f + 0.55f * sun)
                var c = space.mix(deep, clamp01(wide * 1.6f + 0.12f))
                c = c.mix(air.mix(sunCol, sun), clamp01(shell * 1.3f))
                c = c.mix(sunCore, clamp01(shell * sun * 1.4f - 0.55f))
                base.r[i] = c.r
                base.g[i] = c.g
                base.b[i] = c.b
                base.cov[i] =
                    if (shell >
                        0.5f
                    ) {
                        1.45f + 0.55f * clamp01((shell - 0.5f) * 2.5f)
                    } else {
                        clamp01(0.04f + shell * 1.7f + wide * 1.3f)
                    }
            }
        }
        // The crisp edge of the air right on the limb.
        for (x in 0 until NCOLS) {
            val y = (limb[x] - 0.5f).toInt()
            val sun = gauss(x + 0.5f, SUN_X, 36f)
            base.put(x, y, air.mix(col("#a8d0ff"), 0.3f).mix(sunCol, sun).mix(sunCore, sun * sun * 0.6f), 2f, '▄')
        }
        // A thin crescent moon high on the left.
        for (y in 2..8) {
            for (x in 20..34) {
                val a = dist(x + 0.5f, y + 0.5f, 27f, 5f)
                val b = dist(x + 0.5f, y + 0.5f, 29.6f, 4.3f)
                if (a < 7f && b > 6.2f) base.put(x, y, col("#e8eef8").scale(0.7f + 0.3f * (a / 7f)), 2f)
            }
        }
        base.glow(26f, 5f, 8f, air, 0.1f)
    }

    private fun buildPlanet() {
        // Each cell below the limb is a point on the sphere: its longitude and latitude carry the clouds, and its
        // normal against the low sun gives the day side, the terminator and the night side.
        val lx = 0.55f
        val ly = 0.14f
        val lz = -0.82f
        for (y in 0 until NROWS) {
            for (x in 0 until NCOLS) {
                if (y + 0.5f < limb[x]) continue
                val i = y * NCOLS + x
                val nx = (x + 0.5f - TX) / RADIUS
                val ny = (CENTER_Y - (y + 0.5f)) * ROW_U / RADIUS
                val nz = sqrt(max(0f, 1f - nx * nx - ny * ny))
                lon[i] = atan2(nx, nz)
                latitude[i] = asin(min(1f, ny))
                val l = nx * lx + ny * ly + nz * lz
                lit[i] = l
                val day = smooth(-0.02f, 0.25f, l)
                val dusk = gauss(l, 0f, 0.06f)
                val glare = gauss(x + 0.5f, SUN_X, 6f + (y - limb[x]) * 0.8f) * exp(-(y + 0.5f - limb[x]) / 3f)
                var c = night.mix(ocean, 0.25f + 0.75f * day).mix(col("#8a2a3a"), dusk * 0.35f)
                c = c.mix(sunCore, clamp01(glare * 0.9f))
                c = c.mix(col("#1e5a9a"), day * 0.4f * (1f - nz))
                base.r[i] = c.r
                base.g[i] = c.g
                base.b[i] = c.b
                base.cov[i] = 2f
            }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val u = t / LOOP
        frame.copyFrom(base)
        stars.draw(frame, u, 1f) { y -> clamp01(1.1f - y / 28f) }
        clouds(u)
        island(u)
        sun(u)
        satellite(u)
        val p = u.pow(1.35f)
        val cy = ISLAND_Y - 0.8f - p * 41f
        tether(u, cy)
        climber(cy, 0.3f + 2.4f * p, u)
        frame.output(grid, palette)
    }

    /** Cloud fields on the sphere, sunlit near the terminator, turning slowly in a closed circle. */
    private fun clouds(u: Float) {
        val ox = 0.5f * cos(NTAU * u)
        val oy = 0.5f * sin(NTAU * u)
        for (y in 0 until NROWS) {
            for (x in 0 until NCOLS) {
                if (y + 0.5f < limb[x]) continue
                val i = y * NCOLS + x
                val d = lat.fbm(lon[i] * 7f + ox + 30f, latitude[i] * 20f + oy, 4)
                val swirl = lat.at(lon[i] * 2.5f + 11f, latitude[i] * 6f)
                val dens = smooth(0.48f, 0.72f, d * 0.8f + swirl * 0.25f)
                if (dens < 0.04f) continue
                val l = lit[i]
                val day = smooth(-0.02f, 0.25f, l)
                val dusk = gauss(l, 0.01f, 0.08f)
                val c = col("#24304e").mix(cloud, day * (0.55f + 0.45f * dens)).mix(sunCol, dusk * 0.6f)
                frame.r[i] += (c.r - frame.r[i]) * dens
                frame.g[i] += (c.g - frame.g[i]) * dens
                frame.b[i] += (c.b - frame.b[i]) * dens
            }
        }
    }

    /** The anchor island: reef ring, beach and jungle, with the station lights blinking around the tether foot. */
    private fun island(u: Float) {
        for (y in (ISLAND_Y - 3).toInt()..(ISLAND_Y + 3).toInt()) {
            for (x in (TX - 14).toInt()..(TX + 14).toInt()) {
                val ex = (x + 0.5f - TX) / 12f
                val ey = (y + 0.5f - ISLAND_Y) / 2.2f
                val r = sqrt(ex * ex + ey * ey) + (lat.at(x * 0.7f, y * 1.9f) - 0.5f) * 0.3f
                if (r > 1.15f) continue
                val c =
                    if (r > 0.95f) {
                        lagoon.scale(0.8f)
                    } else if (r > 0.72f) {
                        col("#e8d090")
                    } else {
                        col("#2a8a3a").scale(0.7f + 0.3f * lat.at(x * 1.3f, y * 2f))
                    }
                frame.put(x, y, c, if (r > 0.95f) 1.7f else 2f)
            }
        }
        frame.glow(TX + 0.5f, ISLAND_Y, 9f, lagoon, 0.12f)
        for (k in 0 until 8) {
            val a = NTAU * k / 8f
            val on = fract(u * 10f + k / 8f) < 0.4f
            if (on) frame.dot(TX + 0.5f + cos(a) * 5f, ISLAND_Y + sin(a) * 1.2f, warm, 2f, '•')
        }
        frame.glow(TX + 0.5f, ISLAND_Y, 3f, warm, 0.5f)
    }

    /** The sun breaking over the limb: core, glow and a flat flare along the horizon. */
    private fun sun(u: Float) {
        val sy = limb[SUN_X.toInt()] - 0.3f
        for (y in 0 until NROWS) {
            for (x in max(0, (SUN_X - 70f).toInt()) until NCOLS) {
                val d2 = dist(x + 0.5f, y + 0.5f, SUN_X, sy).let { it * it }
                val below = y + 0.5f - limb[x]
                val k = 0.85f * exp(-d2 / 900f) * (if (below > 0f) exp(-below / 1.5f) else 1f) + 0.7f * exp(-d2 / 60f)
                if (k > 0.03f) frame.light(y * NCOLS + x, if (d2 < 60f) sunCore else sunCol, k)
            }
        }
        for (y in (sy - 2f).toInt()..(sy + 1f).toInt()) {
            for (x in (SUN_X - 5f).toInt()..(SUN_X + 5f).toInt()) {
                val d = dist(x + 0.5f, y + 0.5f, SUN_X, sy)
                if (d < 4.5f &&
                    y + 0.5f < limb[x] + 0.6f
                ) {
                    frame.put(x, y, sunCore.mix(Col(1f, 1f, 1f), 1f - d / 4.5f), 2f)
                }
            }
        }
        val breathe = 0.85f + 0.15f * sin(NTAU * u * 2f)
        for (x in 0 until NCOLS) {
            val k = exp(-abs(x + 0.5f - SUN_X) / 30f) * 0.5f * breathe
            if (k < 0.04f) continue
            val y = sy.toInt()
            frame.light(y * NCOLS + x, sunCore, k)
            if (y + 1 < NROWS) frame.light((y + 1) * NCOLS + x, sunCol, k * 0.5f)
        }
        // Four long rays, slowly turning a quarter turn per loop so the loop closes.
        val rot = NTAU * u / 4f
        for (r in 0 until 4) {
            val a = rot + NTAU * r / 4f + 0.4f
            for (k in 3..34) {
                val x = SUN_X + cos(a) * k
                val y = sy + sin(a) * k / ROW_U
                if (y > limb[x.toInt().coerceIn(0, NCOLS - 1)] - 0.5f) break
                val v = 0.5f * (1f - k / 35f)
                if (x < 0f || x >= NCOLS || y < 0f) break
                frame.light(y.toInt() * NCOLS + x.toInt(), sunCore, v)
            }
        }
    }

    private fun satellite(u: Float) {
        val p = fract(u * 2f + 0.3f)
        val x = -10f + p * (NCOLS + 20f)
        val y = 2f + p * 8f
        frame.dot(x, y, steel, 1f, if (fract(u * 16f) < 0.3f) '+' else '·')
    }

    /** The ribbon, widening as it nears the camera, with pulses of light running up it. */
    private fun tether(
        u: Float,
        cy: Float,
    ) {
        for (y in 0 until ISLAND_Y.toInt()) {
            val near = (ISLAND_Y - y) / ISLAND_Y
            val half = 0.35f + near * near * 2.4f
            val pulse = max(0f, sin(NTAU * (u * 5f + near * 3f))).pow(4f)
            for (x in (TX - half).toInt()..(TX + half).toInt()) {
                val a = (x + 0.5f - TX) / half
                if ((abs(a) > 1f && half > 0.6f) || (half <= 0.6f && x != TX.toInt())) continue
                val shade = if (half < 0.6f) 0.8f else 0.55f + 0.35f * a
                val c = steel.scale(shade + 0.25f * pulse)
                frame.put(x, y, c, 2f, if (half < 0.6f) '│' else NONE)
            }
        }
    }

    /** The climber car, small far down by the island and growing as it rises toward the camera. */
    private fun climber(
        cy: Float,
        s: Float,
        u: Float,
    ) {
        val hw = 3.2f * s + 0.6f
        val hh = 1.1f * s + 0.4f
        if (cy + hh < 0f) return
        frame.glow(TX + 0.5f, cy, 5f + 5f * s, warm, 0.18f + 0.06f * s)
        for (y in (cy - hh - 1).toInt()..(cy + hh + 1).toInt()) {
            for (x in (TX - hw - 2).toInt()..(TX + hw + 2).toInt()) {
                val a = (x + 0.5f - TX) / hw
                val b = (y + 0.5f - cy) / hh
                // A rounded capsule: chamfered corners, a window band in the middle.
                if (abs(b) > 1f || abs(a) > 1f || abs(a) + abs(b) * 0.45f > 1.2f) continue
                var c = steel.scale(0.55f + 0.4f * clamp01(a * 0.5f + 0.5f))
                var glyph = NONE
                if (abs(b) < 0.38f && abs(a) < 0.82f && s > 0.6f) {
                    c = if (fract((x - TX) / max(2f, s * 1.6f)) < 0.6f) warm else col("#1a1e28")
                }
                if (b < -0.75f) glyph = '▄'
                if (b > 0.75f) glyph = '▀'
                frame.put(x, y, c, 2f, glyph)
            }
        }
        if (fract(u * 12f) < 0.35f) {
            frame.dot(TX - hw - 1.2f, cy, col("#ff3a3a"), 2f, '●')
            frame.dot(TX + hw + 1.2f, cy, col("#3aff7a"), 2f, '●')
        }
        // Searchlight down the ribbon.
        for (k in 1..8) {
            val y = (cy + hh + k).toInt()
            if (y in 0 until NROWS) frame.light(y * NCOLS + TX.toInt(), warm, 0.3f * (1f - k / 9f))
        }
    }
}

/**
 * A space elevator at sunrise, seen from orbit. Far below, the curved limb of the planet glows orange where the sun
 * breaks over it and blue along the day side; clouds turn slowly over the ocean. From a small tropical island a bright
 * ribbon rises straight toward the camera, widening as it comes, and a lit climber car rides it up from the island,
 * growing as it nears, while light pulses run up the ribbon, the station lights blink and a satellite crosses the
 * stars. The seed sets the stars, the clouds and the island coast.
 */
public fun spaceElevatorPiece(seed: Int = 0): AsciiPiece {
    val scene = Elevator(seed)
    return asciiPiece(
        name = "space elevator",
        ground = Rgb.fromHex("#03040c"),
        category = "scenes",
        cols = NCOLS,
        rows = NROWS,
        fps = 15,
        still = 13f,
        loop = LOOP,
        note = "a space elevator ribbon rising from a tropical island over the curved planet limb at sunrise",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A space elevator rising from a tropical island over the curved limb of the planet at sunrise, a climber riding it.
 *
 * @param seed sets the stars, the clouds and the island coast.
 */
@Composable
public fun AsciiSpaceElevator(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A space elevator rising from a tropical island into the stars, a climber riding it",
) {
    val piece = remember(seed) { spaceElevatorPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
