package com.jantursky.kascii.gallery

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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 24f
private const val STARS = 230
private const val CENTERS = 18

/** The planet is a circle of this radius in column units, centred far below the frame. */
private const val RADIUS = 250f
private const val PLANET_X = 100f
private const val PLANET_Y = 143f
private const val SUN_X = 66.5f

private fun limbY(x: Float): Float {
    val dx = x - PLANET_X
    return PLANET_Y - sqrt(RADIUS * RADIUS - dx * dx) / E_ASPECT
}

private class OrbitScene(
    seed: Int,
    atmosphere: Rgb,
    sun: Rgb,
) {
    private val ar = atmosphere.red / 255f
    private val ag = atmosphere.green / 255f
    private val ab = atmosphere.blue / 255f
    private val sr = sun.red / 255f
    private val sg = sun.green / 255f
    private val sb = sun.blue / 255f
    private val sunY = limbY(SUN_X) - 1f
    private val lat = ELattice(seed)

    private val palette: EPalette
    private val frame = EFrame()

    // Per cell geometry, built once.
    private val planet = BooleanArray(E_CELLS)
    private val above = FloatArray(E_CELLS)
    private val litLimb = FloatArray(E_CELLS)
    private val light = FloatArray(E_CELLS)
    private val haze = FloatArray(E_CELLS)
    private val landV = FloatArray(E_CELLS)
    private val dustV = FloatArray(E_CELLS)

    // City lights on the night side.
    private val cityCell: IntArray
    private val cityPhase: FloatArray
    private val cityRate: IntArray
    private val cityPower: FloatArray

    // Stars.
    private val starCell = IntArray(STARS)
    private val starPhase = FloatArray(STARS)
    private val starRate = IntArray(STARS)
    private val starPower = FloatArray(STARS)

    init {
        val dark = ECol(0.012f, 0.016f, 0.045f)
        val white = ECol(1f, 1f, 1f)
        val a = ECol(ar, ag, ab)
        val s = ECol(sr, sg, sb)
        palette =
            EPalette(
                eRamp(26, dark, ECol(0.03f, 0.14f, 0.45f), ECol(0.08f, 0.4f, 0.9f), ECol(0.45f, 0.82f, 1f), white) +
                    eRamp(6, dark, ECol(0.04f, 0.07f, 0.16f)) +
                    eRamp(22, dark, ECol(ar * 0.3f, ag * 0.3f, ab * 0.36f), a, white) +
                    eRamp(16, dark, ECol(0.38f, 0.08f, 0.03f), s, ECol(1f, 0.95f, 0.8f), white) +
                    eRamp(12, dark, ECol(0.05f, 0.14f, 0.06f), ECol(0.13f, 0.34f, 0.12f), ECol(0.36f, 0.52f, 0.2f)) +
                    eRamp(7, ECol(0.2f, 0.1f, 0.03f), ECol(1f, 0.78f, 0.35f), ECol(1f, 0.95f, 0.8f)) +
                    eRamp(6, dark, ECol(0.5f, 0.12f, 0.6f), ECol(1f, 0.55f, 0.95f)) +
                    eRamp(6, dark, ECol(0.1f, 0.5f, 0.4f), ECol(0.6f, 1f, 0.9f)) +
                    eRamp(6, dark, ECol(0.2f, 0.26f, 0.5f), ECol(0.7f, 0.8f, 1f)) +
                    eRamp(5, ECol(0.35f, 0.26f, 0.1f), ECol(0.95f, 0.8f, 0.45f)) +
                    eRamp(5, ECol(0.03f, 0.06f, 0.2f), ECol(0.25f, 0.5f, 0.95f)) +
                    eRamp(6, dark, ECol(0.5f, 0.15f, 0.05f), ECol(1f, 0.5f, 0.15f)) +
                    eRamp(6, ECol(0.35f, 0.55f, 0.85f), ECol(0.9f, 0.97f, 1f)),
            )
        buildGeometry()
        val cities = ArrayList<Int>()
        val phases = ArrayList<Float>()
        val rates = ArrayList<Int>()
        val powers = ArrayList<Float>()
        val cx = FloatArray(CENTERS) { 70f + 125f * Noise.hash(it, 1, 91, seed) }
        val cy = FloatArray(CENTERS) { 31f + 16f * Noise.hash(it, 2, 91, seed) }
        val cs = FloatArray(CENTERS) { 2.5f + 6f * Noise.hash(it, 3, 91, seed) }
        for (y in 0 until E_ROWS) {
            for (x in 0 until E_COLS) {
                val i = y * E_COLS + x
                if (!planet[i] || light[i] > 0.02f || landV[i] < 0.44f) continue
                var p = 0f
                for (k in 0 until CENTERS) {
                    val dx = (x + 0.5f - cx[k]) / cs[k]
                    val dy = (y + 0.5f - cy[k]) * E_ASPECT / cs[k]
                    p = max(p, exp(-(dx * dx + dy * dy)))
                }
                p = p * 0.75f + 0.1f * dustV[i]
                val h = Noise.hash(x, y, 92, seed)
                if (h < p) {
                    cities.add(i)
                    phases.add(Noise.hash(x, y, 93, seed))
                    rates.add(1 + (Noise.hash(x, y, 94, seed) * 3f).toInt())
                    powers.add(0.4f + 0.6f * (p * 0.6f + 0.4f * Noise.hash(x, y, 95, seed)))
                }
            }
        }
        cityCell = cities.toIntArray()
        cityPhase = phases.toFloatArray()
        cityRate = rates.toIntArray()
        cityPower = powers.toFloatArray()
        for (s2 in 0 until STARS) {
            var x: Int
            var y: Int
            var n = 0
            do {
                x = (Noise.hash(s2, n, 96, seed) * E_COLS).toInt()
                y = (Noise.hash(s2, n, 97, seed) * 33f).toInt()
                n++
            } while (planet[y * E_COLS + x] || above[y * E_COLS + x] < 3f)
            starCell[s2] = y * E_COLS + x
            starPhase[s2] = Noise.hash(s2, 0, 98, seed)
            starRate[s2] = 1 + (Noise.hash(s2, 0, 99, seed) * 3f).toInt()
            val q = Noise.hash(s2, 0, 100, seed)
            starPower[s2] = 0.3f + 0.7f * q * q
        }
    }

    private fun buildGeometry() {
        // The sun sits just behind the left limb, so the light runs in from the left and a little from behind.
        val slen = sqrt(0.95f * 0.95f + 0.2f * 0.2f + 0.1f * 0.1f)
        val lx = -0.95f / slen
        val ly = -0.2f / slen
        val lz = -0.1f / slen
        for (y in 0 until E_ROWS) {
            for (x in 0 until E_COLS) {
                val i = y * E_COLS + x
                val dx = x + 0.5f - PLANET_X
                val dy = (y + 0.5f - PLANET_Y) * E_ASPECT
                val dist = sqrt(dx * dx + dy * dy)
                val nx = dx / dist
                val ny = dy / dist
                if (dist < RADIUS) {
                    planet[i] = true
                    val px = dx / RADIUS
                    val py = dy / RADIUS
                    val pz = sqrt(max(0f, 1f - px * px - py * py))
                    light[i] = px * lx + py * ly + pz * lz
                    val f = 1f - pz
                    haze[i] = f * f * f
                    landV[i] = lat.fbm(x * 0.035f + 9f, y * 0.14f + 3f, 3)
                    dustV[i] = lat.fbm(x * 0.2f + 30f, y * 0.5f, 2)
                } else {
                    above[i] = (dist - RADIUS) / E_ASPECT
                    litLimb[i] = eSmooth(-0.25f, 0.55f, nx * lx + ny * ly)
                    dustV[i] = lat.fbm(x * 0.05f + 50f, y * 0.12f + (x * 0.02f), 3)
                }
            }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = E_TAU * t / LOOP
        val f = frame
        f.clear()
        val cloudX = 2.4f * cos(phi)
        val cloudY = 2.4f * sin(phi)
        for (y in 0 until E_ROWS) {
            for (x in 0 until E_COLS) {
                val i = y * E_COLS + x
                if (planet[i]) planetCell(f, i, x, y, cloudX, cloudY) else skyCell(f, i, x, y)
            }
        }
        flare(f, phi)
        stars(f, t)
        eOutput(f, grid, palette)
        cities(grid, t, phi)
        satellite(grid, t, phi)
    }

    private fun skyCell(
        f: EFrame,
        i: Int,
        x: Int,
        y: Int,
    ) {
        val a = above[i]
        val lit = litLimb[i]
        // The dusty band of the Milky Way, a faint diagonal across the sky.
        val band = (x * 0.35f + y * 1.1f) - 52f
        val dust = exp(-(band * band) / 200f) * eSmooth(0.4f, 0.8f, dustV[i])
        if (dust > 0.05f) f.add(i, 0.14f * dust, 0.16f * dust, 0.3f * dust, 0.45f * dust)
        if (a > 26f) return
        val g2 = exp(-a / 3.2f)
        val g3 = exp(-a / 9f)
        val k = 0.1f + 0.9f * lit
        val strength = (0.75f * g2 + 0.3f * g3) * k
        // The sunrise end of the limb burns warm where the light first grazes the air.
        val warm = exp(-((lit - 0.5f) * (lit - 0.5f)) / 0.06f) * (g2 * 0.9f)
        val white = g2 * lit * lit * lit * 0.5f
        f.add(
            i,
            ar * strength + warm * 0.6f + white,
            ag * strength + warm * 0.28f + white,
            ab * strength + warm * 0.08f + white,
            strength * 1.2f + warm * 0.4f,
        )
        // Airglow on the night side: a faint green layer a few rows above the limb.
        val air = (1f - lit) * 0.2f * exp(-((a - 4f) * (a - 4f)) / 6f)
        if (air > 0.01f) f.add(i, 0.08f * air, 0.55f * air, 0.38f * air, 0.9f * air)
        // The solid band hugging the limb: bright and white-blue on the lit side, a thin blue thread on the dark.
        if (a < 1.15f) {
            val w = 0.4f + 0.6f * lit
            val l2 = lit * lit
            f.set(i, ar * w * 1.1f + 0.5f * l2, ag * w * 1.1f + 0.6f * l2, ab * w + 0.7f * l2, 1.97f)
        } else if (a < 2.4f) {
            val w = 0.08f + 0.6f * lit
            f.set(i, ar * w, ag * w, ab * w + 0.05f, 1.6f)
        }
    }

    private fun planetCell(
        f: EFrame,
        i: Int,
        x: Int,
        y: Int,
        cloudX: Float,
        cloudY: Float,
    ) {
        val l = light[i]
        val lit = eSmooth(-0.04f, 0.36f, l)
        val land = eSmooth(0.55f, 0.6f, landV[i])
        val cloud = eSmooth(0.52f, 0.76f, lat.fbm(x * 0.04f + cloudX + 20f, y * 0.16f + cloudY, 3))
        var r = 0.03f + (0.1f - 0.03f) * land
        var g = 0.2f + (0.3f - 0.2f) * land
        var b = 0.62f + (0.08f - 0.62f) * land
        r += (0.78f - r) * cloud * 0.92f
        g += (0.9f - g) * cloud * 0.92f
        b += (1f - b) * cloud * 0.92f
        val shade = 1.15f * lit
        val shadeR = shade * sqrt(shade)
        val shadeG = sqrt(shade) * shade
        val hz = haze[i] * (0.1f + 0.95f * lit) * (1f - 0.6f * land)
        val tw = exp(-((l - 0.01f) * (l - 0.01f)) / 0.0006f) * 0.55f
        val ambient = 0.012f * land
        val pr = r * shadeR + ar * hz * 0.9f + 0.008f
        val pg = g * shadeG + ag * hz * 0.9f + 0.012f
        val pb = b * shade + ab * hz * 0.9f + 0.045f
        f.set(
            i,
            pr + (0.8f - pr) * tw + ambient,
            pg + (0.26f - pg) * tw + ambient,
            pb + (0.08f - pb) * tw,
            1.97f,
        )
    }

    private fun flare(
        f: EFrame,
        phi: Float,
    ) {
        val pulse = 1f + 0.07f * sin(2f * phi + 0.4f) + 0.04f * sin(5f * phi + 1f)
        val spin = 0.5f * sin(phi)
        val reach = 55f + 6f * sin(3f * phi)
        for (y in 0 until E_ROWS) {
            val dyw = (y + 0.5f - sunY) * E_ASPECT
            for (x in 0 until E_COLS) {
                val i = y * E_COLS + x
                val dxw = x + 0.5f - SUN_X
                val d2 = dxw * dxw + dyw * dyw
                val d = sqrt(d2)
                var inten = 2.3f / (1f + d2 / 55f) + 0.4f * exp(-d / 26f)
                if (d < 70f) {
                    val th = atan2(dyw, dxw)
                    var rr = 0.5f + 0.5f * cos(8f * th + spin)
                    rr *= rr
                    rr *= rr
                    rr *= rr
                    inten += rr * exp(-d / 30f) * 0.5f
                }
                inten *= pulse
                var streak = 0f
                if (abs(dyw) < 9f) streak += 1.2f * exp(-abs(dxw) / reach) * exp(-(dyw * dyw) / 6f)
                if (abs(dxw) < 5f) streak += 0.4f * exp(-abs(dyw) / 10f) * exp(-(dxw * dxw) / 2f)
                streak *= pulse
                if (inten < 0.02f && streak < 0.02f) continue
                val k = if (planet[i]) 0.4f else 1f
                val hot = eClamp(inten * 0.9f)
                val white = max(0f, inten - 0.8f) * 0.8f
                val cover = min(1.4f, inten * 0.9f + streak * 0.9f)
                f.add(
                    i,
                    (sr * inten * 1.05f + 0.4f * streak + white + 0.5f * max(0f, streak - 0.6f)) * k,
                    (sg * inten * (0.4f + 0.6f * hot) + 0.7f * streak + white + 0.5f * max(0f, streak - 0.6f)) * k,
                    (sb * inten * hot * hot + 1f * streak + white + 0.4f * max(0f, streak - 0.6f)) * k,
                    cover * k,
                )
                if (!planet[i]) {
                    if (d < 4.2f) {
                        f.set(i, 1f, 0.98f, 0.9f, 2f)
                    } else if (d < 5.8f) {
                        f.set(i, 1f, 0.9f, 0.62f, 1.97f)
                    }
                }
            }
        }
        // Lens ghosts strung along the line from the sun through the middle of the frame.
        ghost(f, 1.5f, 5f, 0.8f, 0.3f, 0.95f, 0.22f * pulse)
        ghost(f, 2.1f, 8.5f, 0.2f, 0.85f, 0.6f, 0.16f * pulse)
        ghost(f, 2.9f, 3.4f, 1f, 0.55f, 0.2f, 0.2f * pulse)
        ghost(f, 3.7f, 12f, 0.4f, 0.55f, 1f, 0.1f * pulse)
    }

    private fun ghost(
        f: EFrame,
        k: Float,
        radius: Float,
        r: Float,
        g: Float,
        b: Float,
        power: Float,
    ) {
        val gx = SUN_X + (96f - SUN_X) * k
        val gy = sunY + (22f - sunY) * k
        val ry = (radius / E_ASPECT).toInt() + 2
        val rx = radius.toInt() + 3
        for (y in (gy.toInt() - ry)..(gy.toInt() + ry)) {
            for (x in (gx.toInt() - rx)..(gx.toInt() + rx)) {
                if (x !in 0 until E_COLS || y !in 0 until E_ROWS) continue
                val dx = x + 0.5f - gx
                val dy = (y + 0.5f - gy) * E_ASPECT
                val d = sqrt(dx * dx + dy * dy)
                val ring = exp(-((d - radius) * (d - radius)) / 1.6f)
                val fill = if (d < radius) 0.35f else 0f
                val v = power * (ring + fill)
                if (v < 0.02f) continue
                f.add(y * E_COLS + x, r * v, g * v, b * v, v * 1.7f)
            }
        }
    }

    private fun stars(
        f: EFrame,
        t: Float,
    ) {
        for (s in 0 until STARS) {
            val i = starCell[s]
            if (f.cov[i] > 0.45f) continue
            val tw = (0.55f + 0.45f * sin(E_TAU * (starRate[s] * t / LOOP + starPhase[s]))) * starPower[s]
            if (tw < 0.2f) continue
            f.set(i, 0.55f + 0.45f * tw, 0.6f + 0.4f * tw, 0.85f + 0.15f * tw, 0.9f)
        }
    }

    private fun cities(
        grid: Grid,
        t: Float,
        phi: Float,
    ) {
        for (k in cityCell.indices) {
            val i = cityCell[k]
            val tw = 0.65f + 0.35f * sin(E_TAU * (cityRate[k] * t / LOOP + cityPhase[k]))
            val v = cityPower[k] * tw
            if (v < 0.22f) continue
            val x = i % E_COLS
            val y = i / E_COLS
            val glyph =
                if (v > 0.8f) {
                    '●'
                } else if (v > 0.45f) {
                    '•'
                } else {
                    '·'
                }
            ePut(grid, palette, x, y, glyph, 1f * (0.6f + 0.4f * v), 0.78f * (0.6f + 0.4f * v), 0.36f * v)
        }
    }

    /** A satellite with a gold body and two solar panels crosses the sky once per loop, off screen while it wraps. */
    private fun satellite(
        grid: Grid,
        t: Float,
        phi: Float,
    ) {
        val u = eFract(t / LOOP)
        val sx = (-22f + 236f * u).roundToInt()
        val sy = (limbY(sx.toFloat()) - 15f).roundToInt()
        val shear = 0.075f * sin(phi * 2f)
        // The panels, tilted slowly about the long axis.
        for (side in intArrayOf(-1, 1)) {
            for (c in 1..13) {
                val x = sx + side * (3 + c)
                val off = (shear * c * side).roundToInt()
                for (row in 0..2) {
                    val y = sy + off + row - 1
                    val frameLine = c % 4 == 0 || (row == 0 && c == 1)
                    val sun = if (side < 0) 1f else 0.7f
                    if (frameLine) {
                        ePut(grid, palette, x, y, '▒', 0.62f * sun, 0.72f * sun, 0.92f * sun)
                    } else {
                        val glint = 0.5f + 0.5f * (1f - c / 13f)
                        val gs = glint * sun
                        ePut(grid, palette, x, y, '█', 0.05f + 0.1f * gs, 0.12f + 0.25f * gs, 0.34f + 0.5f * gs)
                    }
                }
            }
            ePut(grid, palette, sx + side * 4, sy, '═', 0.75f, 0.75f, 0.8f)
        }
        // The body, foil gold, lit from the sun side.
        for (row in -1..2) {
            for (c in -3..3) {
                val lit =
                    if (c < 0) {
                        1f
                    } else if (c == 0) {
                        0.8f
                    } else {
                        0.55f
                    }
                val shade = if (row >= 1) 0.7f else 1f
                ePut(
                    grid,
                    palette,
                    sx + c,
                    sy + row,
                    if (row >= 1) '▓' else '█',
                    (0.45f + 0.5f * lit) * shade,
                    (0.34f + 0.46f * lit) * shade,
                    (0.12f + 0.3f * lit) * shade,
                )
            }
        }
        ePut(grid, palette, sx, sy - 2, '│', 0.8f, 0.8f, 0.85f)
        val blink = if (sin(phi * 12f) > 0.2f) 1f else 0.3f
        ePut(grid, palette, sx, sy - 3, '●', blink, 0.2f * blink, 0.15f * blink)
        // A glint now and then: a small star of light off the panel.
        val glint = sin(phi * 3f + 0.8f)
        if (glint > 0.82f) {
            val gx = sx - 9
            val gy = sy - 1
            ePut(grid, palette, gx, gy, '*', 1f, 1f, 1f)
            ePut(grid, palette, gx - 1, gy, '-', 0.8f, 0.9f, 1f)
            ePut(grid, palette, gx + 1, gy, '-', 0.8f, 0.9f, 1f)
            ePut(grid, palette, gx, gy - 1, '|', 0.8f, 0.9f, 1f)
        }
    }
}

/**
 * A planet seen from orbit: the curved limb of a blue world fills the lower frame under a thin band of atmosphere,
 * a sunrise breaks over the horizon with lens streaks and ghosts, and a small satellite with solar panels slowly
 * crosses the sky above clouds and the lit coasts. On the night side city lights twinkle under the stars. Every cell
 * is computed from its position and the time, shaded in its own colour and drawn as a four level dithered halftone
 * or a solid block. The seed shapes the continents, the clouds, the cities and the stars. Every motion completes a
 * whole number of cycles per loop.
 */
public fun orbitSunrisePiece(
    seed: Int = 0,
    atmosphere: Rgb = Rgb.fromHex("#3d94ff"),
    sun: Rgb = Rgb.fromHex("#ffb054"),
): AsciiPiece {
    val scene = OrbitScene(seed, atmosphere, sun)
    return asciiPiece(
        name = "orbit sunrise",
        ground = Rgb.fromHex("#03040c"),
        category = "scenes",
        cols = E_COLS,
        rows = E_ROWS,
        fps = 15,
        still = 5f,
        loop = LOOP,
        note = "a sunrise flares over the limb of a blue planet while a satellite crosses above the city lights",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * Sunrise seen from orbit: the limb of a blue planet with an atmosphere band, a sun flare with lens streaks, a
 * slowly crossing satellite, city lights on the night side and a field of stars.
 *
 * @param seed shapes the continents, the clouds, the cities and the stars.
 * @param atmosphere the color of the atmosphere glow.
 * @param sun the color of the sunrise flare.
 */
@Composable
public fun AsciiOrbitSunrise(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    atmosphere: Rgb = Rgb.fromHex("#3d94ff"),
    sun: Rgb = Rgb.fromHex("#ffb054"),
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A sunrise flaring over the curved blue limb of a planet, seen from orbit",
) {
    val piece = remember(seed, atmosphere, sun) { orbitSunrisePiece(seed, atmosphere, sun) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
