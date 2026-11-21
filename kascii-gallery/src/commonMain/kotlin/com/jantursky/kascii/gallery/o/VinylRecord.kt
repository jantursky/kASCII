package com.jantursky.kascii.gallery.o

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
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 18f

/** Turns per loop: 10 turns in 18 seconds is 33 1/3 revolutions per minute. */
private const val TURNS = 10f
private const val PX = 82f
private const val PY = 29.5f * O_ROW_U
private const val TILT = 0.47f
private const val PLATTER = 56f
private const val RECORD = 52f
private const val LABEL = 15f
private const val PIVOT_X = 166f
private const val PIVOT_Y = 12.5f * O_ROW_U
private const val NOTES = 12
private const val MOTES = 34
private const val LIGHT = -2.4f
private const val PLINTH_TOP = 9
private const val PLINTH_FRONT = 44

private class Turntable(
    seed: Int,
) {
    private val frame = OFrame()
    private val back = OFrame()
    private val palette: OPalette
    private val rad = FloatArray(OCELLS)
    private val ang = FloatArray(OCELLS)
    private val sheen = FloatArray(OCELLS)
    private val noteX = FloatArray(NOTES)
    private val noteDx = FloatArray(NOTES)
    private val noteP = FloatArray(NOTES)
    private val noteKind = IntArray(NOTES)
    private val moteX = FloatArray(MOTES)
    private val moteY = FloatArray(MOTES)
    private val moteP = FloatArray(MOTES)
    private val labelA: OCol
    private val labelB: OCol
    private val warm = ocol("#ffc47a")
    private val steel = ocol("#c8ccd4")
    private val seedSalt = seed

    init {
        val rng = oRng(seed, 51)
        val labels = listOf(ocol("#c8402a"), ocol("#e0a030"), ocol("#3a6ab0"), ocol("#2a8a6a"))
        val pick = rng.int(labels.size)
        labelA = labels[pick]
        labelB = ocol("#f4e8d0")
        for (n in 0 until NOTES) {
            noteX[n] = 96f + rng.float() * 40f
            noteDx[n] = -50f + rng.float() * 46f
            noteP[n] = rng.float() * OTAU
            noteKind[n] = rng.int(3)
        }
        for (m in 0 until MOTES) {
            moteX[m] = rng.float() * 90f
            moteY[m] = rng.float() * 24f
            moteP[m] = rng.float()
        }
        for (y in 0 until OROWS) {
            for (x in 0 until OCOLS) {
                val i = y * OCOLS + x
                val u = x + 0.5f - PX
                val v = ((y + 0.5f) * O_ROW_U - PY) / TILT
                rad[i] = sqrt(u * u + v * v)
                val a = atan2(v, u)
                ang[i] = a
                // Concentric grooves throw a bright wedge toward the lamp and a fainter one opposite.
                val across = sin(a - LIGHT)
                sheen[i] = exp(-(across / 0.22f) * (across / 0.22f)) * (if (cos(a - LIGHT) > 0f) 1f else 0.5f)
            }
        }
        buildBack(seed)
        palette = OPalette(buildPalette())
    }

    private fun buildPalette(): List<OCol> =
        oramp(10, ocol("#060403"), ocol("#160c06"), ocol("#2e180a"), ocol("#5a3212"), ocol("#8a5424")) +
            oramp(8, ocol("#040406"), ocol("#0c0c10"), ocol("#1c1c24"), ocol("#3a3a46"), ocol("#6a6a78")) +
            oramp(7, ocol("#3a3c44"), ocol("#8a8e98"), ocol("#d0d4dc"), ocol("#ffffff")) +
            oramp(6, ocol("#4a2a10"), ocol("#c08040"), ocol("#ffc47a"), ocol("#fff0d0")) +
            oramp(5, labelA.scale(0.25f), labelA.scale(0.6f), labelA, labelA.mix(ocol("#ffffff"), 0.4f)) +
            oramp(3, ocol("#8a7a60"), ocol("#d8c8a8"), ocol("#f4e8d0"))

    private fun buildBack(seed: Int) {
        val lat = OLattice(seed, 53)
        for (y in 0 until OROWS) {
            for (x in 0 until OCOLS) {
                val i = y * OCOLS + x
                val lx = x - 20f
                val ly = (y + 6f) * O_ROW_U
                val lamp = exp(-(lx * lx + ly * ly) / 9000f)
                if (y < PLINTH_TOP) {
                    // The wall behind, washed by the lamp.
                    val v = 0.025f + 0.16f * lamp
                    back.set(i, 0f, 0f, 0f, 0f)
                    back.tone(i, v * 1.1f, v * 0.75f, v * 0.48f, 0.26f)
                } else if (y < PLINTH_FRONT) {
                    // The walnut top of the plinth: long grain, lamp light falling off to the right.
                    val grain = lat.fbm(x * 0.03f, y * 0.55f + sin(x * 0.02f) * 2f, 3)
                    val v = (0.05f + 0.16f * lamp + 0.025f * grain) * (0.9f + 0.15f * osmooth(0.55f, 0.7f, grain))
                    back.set(i, v * 1.15f, v * 0.62f, v * 0.34f, 2f)
                } else {
                    // Its front edge, in shade, with a lit lip.
                    val v = if (y == PLINTH_FRONT) 0.2f + 0.25f * lamp else 0.05f + 0.04f * lamp
                    back.set(i, v * 1.1f, v * 0.66f, v * 0.36f, 2f, if (y == PLINTH_FRONT) '▀' else 0.toChar())
                }
            }
        }
        // A soft shadow of the platter falls down and to the right.
        for (y in PLINTH_TOP until PLINTH_FRONT) {
            for (x in 0 until OCOLS) {
                val u = x + 0.5f - PX - 5f
                val v = ((y + 0.5f) * O_ROW_U - PY - 4f) / TILT
                val d = sqrt(u * u + v * v)
                val k = 1f - 0.6f * osmooth(PLATTER + 6f, PLATTER - 2f, d)
                val i = y * OCOLS + x
                back.r[i] *= k
                back.g[i] *= k
                back.b[i] *= k
            }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        frame.copyFrom(back)
        val theta = OTAU * TURNS * t / LOOP
        platter(theta)
        val (sx, sy) = stylus(theta)
        arm(sx, sy)
        notes(t)
        motes(t)
        frame.output(grid, palette)
    }

    private fun platter(theta: Float) {
        val y0 = ((PY - PLATTER * TILT) / O_ROW_U).toInt() - 1
        val y1 = ((PY + PLATTER * TILT) / O_ROW_U).toInt() + 2
        for (y in max(0, y0)..min(OROWS - 1, y1)) {
            for (x in max(0, (PX - PLATTER - 1).toInt())..min(OCOLS - 1, (PX + PLATTER + 1).toInt())) {
                val i = y * OCOLS + x
                val r = rad[i]
                if (r > PLATTER) {
                    // The platter's chrome side, seen below its front edge.
                    val u = x + 0.5f - PX
                    val above = ((y - 0.5f) * O_ROW_U - PY) / TILT
                    if (abs(u) < PLATTER && sqrt(u * u + above * above) <= PLATTER && above > 0f) {
                        val k = 0.3f + 0.35f * (1f - abs(u) / PLATTER)
                        frame.set(i, steel.r * k, steel.g * k, steel.b * k, 2f, '▀')
                    }
                    continue
                }
                val a = ang[i]
                val local = a - theta
                if (r > RECORD) {
                    // The chrome rim with strobe dots that turn with the platter.
                    val k = 0.45f + 0.45f * sheen[i] + 0.15f * cos(a - LIGHT)
                    val dot = cos(48f * local) > 0.6f && r > RECORD + 2f
                    frame.set(i, steel.r * k, steel.g * k, steel.b * k, 2f, if (dot) '•' else 0.toChar())
                    if (dot) frame.cov[i] = 2f
                    continue
                }
                if (r > LABEL) {
                    // Vinyl: near black, ring texture, gaps between tracks, the lamp's groove sheen and turning dust.
                    val ring = 0.82f + 0.18f * sin(r * 2.3f)
                    val gap =
                        1f -
                            0.6f *
                            (
                                ogauss(r, 24f, 0.7f) + ogauss(r, 31.5f, 0.7f) + ogauss(r, 39f, 0.7f) +
                                    ogauss(r, 46f, 0.7f)
                            )
                    val lead = ogauss(r, RECORD - 1.2f, 0.9f)
                    val sh = sheen[i] * ring * gap
                    var v = 0.035f + 0.02f * ring
                    var lr = v + 0.5f * sh * warm.r + 0.06f * lead
                    var lg = v + 0.5f * sh * warm.g + 0.06f * lead
                    var lb = v * 1.2f + 0.5f * sh * warm.b + 0.07f * lead
                    val speck = Noise.hash((r * 1.3f).toInt(), (ofract(local / OTAU) * 90f).toInt(), 3, seedSalt)
                    if (speck > 0.985f) {
                        lr += 0.25f
                        lg += 0.24f
                        lb += 0.22f
                    }
                    val lum = 0.35f * lr + 0.45f * lg + 0.2f * lb
                    v = lum
                    frame.set(i, lr, lg, lb, if (v > 0.2f) 2f else 1.8f)
                    continue
                }
                if (r < 1.3f) {
                    frame.set(i, 0.85f, 0.86f, 0.9f, 1f, '●')
                    continue
                }
                // The label: two colors, a printed band and a ring, turning with the record.
                val band = cos(local) > 0.55f && r > 5f && r < 12f
                val ringed = abs(r - 13.3f) < 0.6f
                val c = if (band) labelB else labelA
                val k = 0.75f + 0.25f * cos(a - LIGHT) * (r / LABEL)
                val m = if (ringed) 0.6f else 1f
                frame.set(i, c.r * k * m, c.g * k * m, c.b * k * m, 2f)
            }
        }
    }

    private fun stylus(theta: Float): Pair<Float, Float> {
        // A slightly off-centre pressing: the arm rides in and out once per turn.
        val rs = 41f + 1.1f * sin(theta + 0.7f)
        val a = -0.38f
        return Pair(PX + rs * cos(a), PY + rs * sin(a) * TILT)
    }

    private fun arm(
        sx: Float,
        sy: Float,
    ) {
        val dx = sx - PIVOT_X
        val dy = sy - PIVOT_Y
        val len = sqrt(dx * dx + dy * dy)
        val ux = dx / len
        val uy = dy / len
        val bx = PIVOT_X - ux * 12f
        val by = PIVOT_Y - uy * 12f
        for (y in 2 until PLINTH_FRONT) {
            for (x in 90 until OCOLS) {
                val i = y * OCOLS + x
                val px = x + 0.5f
                val py = (y + 0.5f) * O_ROW_U
                // Distance to the arm tube, from the counterweight behind the pivot to the headshell.
                val qx = px - bx
                val qy = py - by
                val along = qx * ux + qy * uy
                val across = qx * -uy + qy * ux
                val total = len + 12f
                val pd = sqrt((px - PIVOT_X) * (px - PIVOT_X) + (py - PIVOT_Y) * (py - PIVOT_Y) * 0.8f)
                if (pd < 6.5f) {
                    // The pivot housing.
                    val k = 0.35f + 0.55f * osmooth(6.5f, 0f, pd) * (0.6f + 0.4f * (-(px - PIVOT_X) / 6.5f))
                    frame.set(i, steel.r * k, steel.g * k, steel.b * k, 2f)
                    continue
                }
                if (along < 0f || along > total) {
                    shadowAt(i, px, py, bx, by, ux, uy, total)
                    continue
                }
                val half =
                    if (along < 7f) {
                        3.4f
                    } else if (along > total - 5f) {
                        2.6f
                    } else {
                        1.15f
                    }
                if (abs(across) > half) {
                    shadowAt(i, px, py, bx, by, ux, uy, total)
                    continue
                }
                val c = 1f - abs(across) / half
                var k = 0.35f + 0.65f * c
                if (along < 7f) k *= 0.55f
                if (along > total - 5f) {
                    // The black headshell with a lit top edge, and its orange cartridge.
                    val edge = across < -half + 1.2f
                    if (along > total - 2.2f && abs(across) < 1.4f) {
                        frame.set(i, 0.88f, 0.48f, 0.16f, 2f)
                    } else {
                        val h = if (edge) 0.55f else 0.1f
                        frame.set(i, h, h, h * 1.1f, 2f)
                    }
                    continue
                }
                frame.set(i, steel.r * k, steel.g * k, steel.b * k, 2f)
            }
        }
        // The stylus touching down, with a pin of light.
        val cx = sx.toInt()
        val cy = (sy / O_ROW_U).toInt()
        if (cx in 0 until OCOLS && cy + 1 < OROWS) frame.put(cx, cy + 1, ocol("#fff0d0"), 1f, '\'')
    }

    /** Darkens cell [i] where the arm, lit from the lamp, throws its shadow down and to the right. */
    private fun shadowAt(
        i: Int,
        px: Float,
        py: Float,
        bx: Float,
        by: Float,
        ux: Float,
        uy: Float,
        total: Float,
    ) {
        val qx = px - 3f - bx
        val qy = py - 4f - by
        val along = qx * ux + qy * uy
        val across = qx * -uy + qy * ux
        if (along < 7f || along > total || abs(across) > 1.6f) return
        frame.r[i] *= 0.35f
        frame.g[i] *= 0.35f
        frame.b[i] *= 0.35f
    }

    private fun notes(t: Float) {
        for (n in 0 until NOTES) {
            val p = ofract(2f * t / LOOP + n.toFloat() / NOTES)
            val x = noteX[n] + noteDx[n] * p + 3f * sin(OTAU * p * 2f + noteP[n])
            val y = 22f - 21f * p
            val fade = osmooth(0f, 0.12f, p) * osmooth(1f, 0.7f, p)
            if (fade < 0.08f) continue
            val c = warm.mix(ocol("#fff4e0"), 0.4f).scale(0.35f + 0.65f * fade)
            val xi = x.toInt()
            val yi = y.toInt()
            for (dy in -1..1) {
                for (dx in -3..3) {
                    val xx = xi + dx
                    val yy = yi + dy
                    if (xx in 0 until OCOLS && yy in 0 until OROWS) {
                        frame.light(yy * OCOLS + xx, warm, 0.1f * fade / (1f + abs(dx) * 0.6f + abs(dy)))
                    }
                }
            }
            when (noteKind[n]) {
                0 -> {
                    // An eighth note.
                    put(xi + 1, yi - 2, c, '│')
                    put(xi + 2, yi - 2, c, '╲')
                    put(xi + 1, yi - 1, c, '│')
                    put(xi, yi, c, '●')
                    put(xi + 1, yi, c, '┘')
                }

                1 -> {
                    // Two beamed eighths.
                    put(xi + 1, yi - 2, c, '┌')
                    put(xi + 2, yi - 2, c, '─')
                    put(xi + 3, yi - 2, c, '─')
                    put(xi + 4, yi - 2, c, '┐')
                    put(xi + 1, yi - 1, c, '│')
                    put(xi + 4, yi - 1, c, '│')
                    put(xi, yi, c, '●')
                    put(xi + 1, yi, c, '┘')
                    put(xi + 3, yi, c, '●')
                    put(xi + 4, yi, c, '┘')
                }

                else -> {
                    // A quarter note.
                    put(xi + 1, yi - 2, c, '│')
                    put(xi + 1, yi - 1, c, '│')
                    put(xi, yi, c, '●')
                    put(xi + 1, yi, c, '┘')
                }
            }
        }
    }

    private fun put(
        x: Int,
        y: Int,
        c: OCol,
        glyph: Char,
    ) {
        if (x in 0 until OCOLS && y in 0 until OROWS) frame.put(x, y, c, 1f, glyph)
    }

    private fun motes(t: Float) {
        // Dust turning slowly in the lamp light.
        for (m in 0 until MOTES) {
            val ph = OTAU * (t / LOOP + moteP[m])
            val x = (moteX[m] + 3f * sin(ph)).toInt()
            val y = (moteY[m] + 1.5f * cos(ph * 2f)).toInt()
            if (x !in 0 until OCOLS || y !in 0 until PLINTH_TOP + 6) continue
            val i = y * OCOLS + x
            if (frame.cov[i] > 1.4f) continue
            val tw = 0.5f + 0.5f * sin(ph * 3f)
            frame.set(i, 0.5f + 0.4f * tw, 0.42f + 0.3f * tw, 0.28f + 0.2f * tw, 1f, '·')
        }
    }
}

/**
 * A record spinning on a turntable under a warm lamp: the grooves throw a fixed wedge of sheen while dust specks, the
 * label and the platter's strobe dots turn through it at 33 1/3 revolutions per minute. The tonearm rides the slightly
 * off-centre pressing in and out, little notes drift up from the music and dust motes hang in the lamp light. The seed
 * picks the label color and places the notes. Every motion repeats a whole number of times per loop.
 */
public fun vinylRecordPiece(seed: Int = 0): AsciiPiece {
    val scene = Turntable(seed)
    return asciiPiece(
        name = "vinyl record",
        ground = Rgb.fromHex("#050302"),
        category = "objects",
        cols = OCOLS,
        rows = OROWS,
        fps = 24,
        still = 5f,
        loop = LOOP,
        note = "a record spinning on a turntable with groove sheen, a tracking tonearm and notes drifting up",
    ) { time, grid -> scene.draw(oLoopTime(time, LOOP), grid) }
}

/**
 * A record spinning on a turntable under a warm lamp, with notes drifting out.
 *
 * @param seed picks the label color and places the drifting notes.
 */
@Composable
public fun AsciiVinylRecord(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A vinyl record spinning on a turntable, the tonearm playing and notes drifting up",
) {
    val piece = remember(seed) { vinylRecordPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
