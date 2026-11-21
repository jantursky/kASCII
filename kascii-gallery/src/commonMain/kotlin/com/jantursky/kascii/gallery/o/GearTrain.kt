package com.jantursky.kascii.gallery.o

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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 18f

/**
 * Teeth that pass every mesh point per loop: a multiple of every gear's teeth per spoke, so each gear looks the same
 * again.
 */
private const val TEETH_PER_LOOP = 36f
private const val MODULE = 2f
private const val LAMP_X = 34f
private const val LAMP_Y = -24f
private const val LIGHT_ANGLE = -2.3f

/** One gear: its teeth, spokes, centre in width units, the gear it meshes with and the direction to it. */
private class Gear(
    val teeth: Int,
    val spokes: Int,
    val cx: Float,
    val cy: Float,
    val parent: Int,
    val tint: OCol,
) {
    val r = MODULE * teeth / 2f
    val hub = max(3.6f, r * 0.26f)
    val rimIn = r - 3.4f * MODULE
    val solid = r < 14f
    var offset = 0f
    val d = FloatArray(OCELLS)
    val cn = FloatArray(OCELLS)
    val sn = FloatArray(OCELLS)
    val cs = FloatArray(OCELLS)
    val ss = FloatArray(OCELLS)
    val spokeCos = FloatArray(OCELLS)
    val facing = FloatArray(OCELLS)
    val sheen = FloatArray(OCELLS)
    var x0 = 0
    var x1 = 0
    var y0 = 0
    var y1 = 0
}

private class Gears(
    seed: Int,
) {
    private val frame = OFrame()
    private val back = OFrame()
    private val palette: OPalette
    private val gears: List<Gear>
    private val brass = ocol("#d9a441")
    private val bronze = ocol("#c27a3e")
    private val pale = ocol("#e8c46a")
    private val steel = ocol("#9aa4b4")
    private val lx = cos(LIGHT_ANGLE)
    private val ly = sin(LIGHT_ANGLE)

    init {
        val rng = oRng(seed, 5)
        val tilt = (rng.float() - 0.5f) * 0.3f
        // A chain of meshing gears: each child sits at the sum of both pitch radii from its parent.
        val spec =
            listOf(
                floatArrayOf(30f, 5f, -1f, 0f),
                floatArrayOf(12f, 4f, 0f, -0.44f + tilt),
                floatArrayOf(24f, 6f, 1f, 0.35f - tilt),
                floatArrayOf(18f, 6f, 2f, 1.05f + tilt),
                floatArrayOf(16f, 4f, 0f, 2.62f - tilt),
            )
        val tints = listOf(brass, pale, bronze, brass, bronze)
        val list = ArrayList<Gear>()
        val alphas = FloatArray(spec.size)
        for ((k, s) in spec.withIndex()) {
            val n = s[0].toInt()
            val p = s[2].toInt()
            var cx = 64f
            var cy = 52f
            if (p >= 0) {
                val dist = MODULE * (n + list[p].teeth) / 2f
                cx = list[p].cx + dist * cos(s[3])
                cy = list[p].cy + dist * sin(s[3])
            }
            alphas[k] = s[3]
            list.add(Gear(n, s[1].toInt(), cx, cy, p, tints[k]))
        }
        gears = list
        gears[0].offset = rng.float() * OTAU
        for (k in 1 until gears.size) {
            val g = gears[k]
            val p = gears[g.parent]
            // Tooth of the parent at the contact point always meets a gap of the child.
            g.offset = (g.teeth * (alphas[k] + 3.1415927f) + p.teeth * alphas[k] - 3.1415927f) / g.teeth
        }
        for (g in gears) prepare(g)
        buildBack(seed)
        palette = OPalette(buildPalette())
    }

    private fun prepare(g: Gear) {
        val reach = g.r + MODULE + 1f
        g.x0 = max(0, (g.cx - reach).toInt())
        g.x1 = min(OCOLS - 1, (g.cx + reach).toInt() + 1)
        g.y0 = max(0, ((g.cy - reach) / O_ROW_U).toInt())
        g.y1 = min(OROWS - 1, ((g.cy + reach) / O_ROW_U).toInt() + 1)
        val spokeHalf = max(2f, g.r * 0.12f)
        for (y in g.y0..g.y1) {
            for (x in g.x0..g.x1) {
                val i = y * OCOLS + x
                val dx = x + 0.5f - g.cx
                val dy = (y + 0.5f) * O_ROW_U - g.cy
                val d = sqrt(dx * dx + dy * dy) + 1e-4f
                val phi = atan2(dy, dx)
                g.d[i] = d
                g.cn[i] = cos(g.teeth * phi)
                g.sn[i] = sin(g.teeth * phi)
                g.cs[i] = cos(g.spokes * phi)
                g.ss[i] = sin(g.spokes * phi)
                g.spokeCos[i] = cos(g.spokes * min(1.5f, spokeHalf / d))
                g.facing[i] = (dx * lx + dy * ly) / d
                // Concentric brushing throws a radial streak toward the lamp and a weaker one away from it.
                val across = sin(phi - LIGHT_ANGLE)
                val toward = cos(phi - LIGHT_ANGLE)
                g.sheen[i] = exp(-(across / 0.2f) * (across / 0.2f)) * (if (toward > 0f) 1f else 0.45f)
            }
        }
    }

    private fun buildBack(seed: Int) {
        val lat = OLattice(seed, 11)
        for (y in 0 until OROWS) {
            for (x in 0 until OCOLS) {
                val i = y * OCOLS + x
                val px = x + 0.5f
                val py = (y + 0.5f) * O_ROW_U
                val ldx = px - LAMP_X
                val ldy = py - LAMP_Y
                val lamp = exp(-(ldx * ldx + ldy * ldy) / 9000f)
                val grain = 0.85f + 0.3f * lat.fbm(x * 0.015f, y * 0.12f, 2)
                var shade = 1f
                for (g in gears) {
                    val sx = px - g.cx - 4f
                    val sy = py - g.cy - 5f
                    val sd = sqrt(sx * sx + sy * sy)
                    shade *= 1f - 0.75f * osmooth(g.r + MODULE + 3f, g.r - 2f, sd)
                }
                val v = (0.035f + 0.3f * lamp) * grain * shade
                back.set(i, v * 0.62f, v * 0.5f, v * 0.36f, 0f)
                back.tone(i, back.r[i], back.g[i], back.b[i], 0.3f)
                // Rivets along the edges of the backing plate.
                val rivet = ((y == 1 || y == OROWS - 2) && x % 24 == 6) || ((x == 3 || x == OCOLS - 4) && y % 10 == 4)
                if (rivet) back.set(i, 0.32f + lamp * 0.5f, 0.27f + lamp * 0.4f, 0.2f + lamp * 0.25f, 1f, '●')
            }
        }
    }

    private fun buildPalette(): List<OCol> =
        oramp(
            18,
            ocol("#120a03"),
            ocol("#3a2208"),
            ocol("#7a4e16"),
            ocol("#c08a30"),
            ocol("#f0c860"),
            ocol("#fff4d0"),
        ) +
            oramp(8, ocol("#1a0c06"), ocol("#5a2c14"), ocol("#a8582a"), ocol("#e8a060")) +
            oramp(6, ocol("#07070a"), ocol("#16130f"), ocol("#2a2118")) +
            listOf(ocol("#b8c2d0"), ocol("#ffffff"), ocol("#fff0c0"))

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        frame.copyFrom(back)
        val u = t / LOOP
        val theta = FloatArray(gears.size)
        theta[0] = gears[0].offset + OTAU * TEETH_PER_LOOP * u / gears[0].teeth
        for (k in 1 until gears.size) {
            val g = gears[k]
            val p = gears[g.parent]
            theta[k] = -(p.teeth.toFloat() / g.teeth) * theta[g.parent] + g.offset
        }
        // A slow shine sweeps diagonally across the brass twice per loop.
        val sweep = ofract(2f * u) * 320f - 60f
        for ((k, g) in gears.withIndex()) gear(g, theta[k], sweep)
        for ((k, g) in gears.withIndex()) glint(g, theta[k])
        frame.output(grid, palette)
    }

    private fun gear(
        g: Gear,
        theta: Float,
        sweep: Float,
    ) {
        val ct = cos(g.teeth * theta)
        val st = sin(g.teeth * theta)
        val cst = cos(g.spokes * theta)
        val sst = sin(g.spokes * theta)
        val holeOut = g.rimIn
        val holeIn = g.hub + 2.2f
        for (y in g.y0..g.y1) {
            for (x in g.x0..g.x1) {
                val i = y * OCOLS + x
                val d = g.d[i]
                val c = g.cn[i] * ct + g.sn[i] * st
                val outer = g.r + MODULE * 0.95f * max(-1f, min(1f, 1.9f * c))
                if (d > outer + 0.5f) continue
                // Edge distance and the direction its bevel faces, for the lit or shadowed rim.
                var edge = outer - d
                var normal = g.facing[i]
                if (!g.solid && d > holeIn && d < holeOut) {
                    val sc = g.cs[i] * cst + g.ss[i] * sst
                    if (sc < g.spokeCos[i]) {
                        continue
                    }
                    val toHole = min(holeOut - d, d - holeIn)
                    if (toHole < edge) {
                        edge = toHole
                        normal = if (holeOut - d < d - holeIn) -g.facing[i] else g.facing[i]
                    }
                }
                if (d < 1.7f) {
                    frame.set(i, 0.03f, 0.025f, 0.02f, 2f)
                    continue
                }
                val px = x + 0.5f
                val py = (y + 0.5f) * O_ROW_U
                val ldx = px - LAMP_X
                val ldy = py - LAMP_Y
                val lamp = 0.55f + 0.6f * exp(-(ldx * ldx + ldy * ldy) / 22000f)
                val bevel = osmooth(1.6f, 0.2f, edge)
                val brushed = 0.9f + 0.1f * sin(d * 2.6f)
                val cone = 0.82f + 0.3f * g.facing[i] * min(1f, d / g.r)
                var lit = lamp * brushed * (0.62f + 0.55f * bevel * normal) * cone
                if (d < g.hub) lit *= 0.85f + 0.3f * osmooth(g.hub, g.hub - 1.2f, d) * g.facing[i]
                val shine = g.sheen[i] * 0.55f + ogauss(px + py * 0.7f, sweep, 7f) * 0.45f
                var r = g.tint.r * lit + shine
                var gg = g.tint.g * lit + shine * 0.9f
                var b = g.tint.b * lit + shine * 0.6f
                if (d < g.hub && d > g.hub - 1f) {
                    r *= 0.6f
                    gg *= 0.6f
                    b *= 0.6f
                }
                // A faint ring groove on the small solid gears.
                if (g.solid && abs(d - g.rimIn) < 0.6f) {
                    r *= 0.55f
                    gg *= 0.55f
                    b *= 0.55f
                }
                val lum = 0.35f * r + 0.45f * gg + 0.2f * b
                val cov = if (edge < 0.25f) 1.8f else 2f
                frame.set(i, r, gg, b, cov)
            }
            // The axle: a steel pin with a lit cap.
            val ay = (g.cy / O_ROW_U).toInt()
            if (y == ay) {
                val ax = g.cx.toInt()
                frame.put(ax, ay, steel, 1f, '●')
            }
        }
    }

    private fun glint(
        g: Gear,
        theta: Float,
    ) {
        // A tooth tip turning through the lamp's direction flashes.
        val c = cos(g.teeth * (LIGHT_ANGLE - theta))
        if (c < 0.6f) return
        val k = (c - 0.6f) / 0.4f
        val tipR = g.r + MODULE * 0.6f
        val x = (g.cx + tipR * lx).toInt()
        val y = ((g.cy + tipR * ly) / O_ROW_U).toInt()
        for (dy in -2..2) {
            for (dx in -6..6) {
                val px = x + dx
                val py = y + dy
                if (px !in 0 until OCOLS || py !in 0 until OROWS) continue
                val f = k * exp(-(dx * dx) / 9f - (dy * dy) / 1.2f)
                if (f > 0.04f) frame.light(py * OCOLS + px, ocol("#fff0c0"), f * 0.7f)
            }
        }
        if (x in 0 until OCOLS && y in 0 until OROWS) {
            frame.put(x, y, OCol(1f, 0.97f, 0.88f), 1f, if (k > 0.6f) '*' else '+')
            if (k > 0.5f) {
                for (dx in listOf(-1, 1)) frame.put(x + dx, y, OCol(0.95f, 0.85f, 0.6f), 1f, '-')
            }
        }
    }
}

/**
 * Five brass and bronze gears in a meshing train on a lamp-lit backing plate. Each gear turns at exactly its tooth
 * ratio, neighbours turn opposite ways, teeth always meet gaps, and the loop closes after the common period in which
 * every gear shows the same face again. Bevels catch the lamp, brushed metal throws a radial sheen, a shine sweeps
 * across and tooth tips flash as they pass the light. The seed tilts the train and turns its starting phase.
 */
public fun gearTrainPiece(seed: Int = 0): AsciiPiece {
    val scene = Gears(seed)
    return asciiPiece(
        name = "gear train",
        ground = Rgb.fromHex("#070605"),
        category = "objects",
        cols = OCOLS,
        rows = OROWS,
        fps = 24,
        still = 4f,
        loop = LOOP,
        note = "interlocking brass gears turning at their meshing ratios under a warm lamp, with a sheen and a glint",
    ) { time, grid -> scene.draw(oLoopTime(time, LOOP), grid) }
}

/**
 * Interlocking brass gears turning at exact meshing ratios under a warm lamp.
 *
 * @param seed tilts the gear train and turns its starting phase.
 */
@Composable
public fun AsciiGearTrain(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Brass gears of different sizes turning together, meshing tooth to gap",
) {
    val piece = remember(seed) { gearTrainPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
