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
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 16f
private const val GX = 96f
private const val GY = 42f
private const val RG = 39f
private const val BASE_TOP = 34
private const val BASE_BOTTOM = 43
private const val FLAKES = 320
private const val LOFT = 1.1f
private const val SHAKE = 1.4f

private class Globe(
    seed: Int,
) {
    private val frame = OFrame()
    private val room = OFrame()
    private val inner = OFrame()
    private val glass = OFrame()
    private val palette: OPalette
    private val homeX = FloatArray(FLAKES)
    private val homeY = FloatArray(FLAKES)
    private val upX = FloatArray(FLAKES)
    private val upY = FloatArray(FLAKES)
    private val fall = FloatArray(FLAKES)
    private val wob = FloatArray(FLAKES)
    private val wobP = FloatArray(FLAKES)
    private val spin = FloatArray(FLAKES)
    private val size = IntArray(FLAKES)
    private val winCells: IntArray
    private val winPhase: FloatArray
    private val px = FloatArray(2)

    init {
        val rng = oRng(seed, 31)
        val wins = ArrayList<Int>()
        val phases = ArrayList<Float>()
        buildRoom()
        buildInner(seed, rng, wins, phases)
        buildGlass()
        winCells = wins.toIntArray()
        winPhase = phases.toFloatArray()
        for (f in 0 until FLAKES) {
            // Thrown somewhere inside the upper globe, then each flake follows its own fall and drift.
            val a = rng.float() * OTAU
            val rr = sqrt(rng.float()) * (RG - 4f)
            upX[f] = GX + rr * cos(a)
            upY[f] = GY - 6f + rr * sin(a) * 0.8f
            fall[f] = 2.2f + rng.float() * 3.6f
            wob[f] = 1.5f + rng.float() * 3.5f
            wobP[f] = rng.float() * OTAU
            spin[f] = (if (rng.float() < 0.5f) -1f else 1f) * (0.6f + rng.float() * 1.4f)
            size[f] =
                if (rng.float() < 0.15f) {
                    2
                } else if (rng.float() < 0.5f) {
                    1
                } else {
                    0
                }
            // Where it comes to rest is wherever its fall ends; it waits there until the next shake.
            var s = 0f
            while (s < LOOP - LOFT) {
                flight(f, s, px)
                if (px[1] >= floorAt(px[0])) break
                s += 0.05f
            }
            flight(f, s, px)
            homeX[f] = px[0]
            homeY[f] = min(px[1], floorAt(px[0]))
        }
        palette = OPalette(buildPalette())
    }

    private fun floorAt(x: Float): Float = GY + 17f - 4.5f * exp(-((x - GX + 6f) / 22f) * ((x - GX + 6f) / 22f))

    /** A flake [s] seconds after it was thrown: falling, spinning about the globe centre and drifting side to side. */
    private fun flight(
        f: Int,
        s: Float,
        out: FloatArray,
    ) {
        val ang = spin[f] * (1f - exp(-s / 1.6f))
        val bx = upX[f] - GX + wob[f] * sin(0.9f * s + wobP[f]) * (0.4f + 0.6f * exp(-s / 3f))
        val by = upY[f] - GY + fall[f] * s
        var x = bx * cos(ang) - by * sin(ang)
        var y = bx * sin(ang) + by * cos(ang)
        val d = sqrt(x * x + y * y)
        if (d > RG - 2.5f) {
            x *= (RG - 2.5f) / d
            y *= (RG - 2.5f) / d
        }
        out[0] = GX + x
        out[1] = GY + y
    }

    private fun buildPalette(): List<OCol> =
        oramp(10, ocol("#04050c"), ocol("#0c1428"), ocol("#1a2c50"), ocol("#2e4a7a"), ocol("#5a7aa8")) +
            oramp(10, ocol("#1c2230"), ocol("#3a4660"), ocol("#8aa0c0"), ocol("#d8e4f4"), ocol("#ffffff")) +
            oramp(8, ocol("#120804"), ocol("#3a1e0c"), ocol("#6a3a18"), ocol("#9a5a28"), ocol("#c88848")) +
            oramp(6, ocol("#3a2a08"), ocol("#a07a2a"), ocol("#e8c060"), ocol("#fff0c0")) +
            oramp(5, ocol("#4a2a08"), ocol("#ffb040"), ocol("#ffe090")) +
            oramp(5, ocol("#061a10"), ocol("#14402a"), ocol("#2a6a44")) +
            oramp(4, ocol("#2a0c10"), ocol("#7a2228"), ocol("#b84a40"))

    private fun buildRoom() {
        for (y in 0 until OROWS) {
            for (x in 0 until OCOLS) {
                val i = y * OCOLS + x
                if (y < BASE_BOTTOM) {
                    val dx = x - GX
                    val dy = (y - 18f) * O_ROW_U
                    val v = 0.02f + 0.07f * exp(-(dx * dx + dy * dy) / 9000f)
                    room.set(i, v * 0.7f, v * 0.75f, v * 1.3f, v * 4f)
                } else {
                    // The table, with the base's soft shadow.
                    val dx = (x - GX - 6f) / 46f
                    val sh = 1f - 0.7f * exp(-dx * dx)
                    val v = (0.06f + 0.02f * (y - BASE_BOTTOM)) * sh
                    room.set(i, v * 1.2f, v * 0.8f, v * 0.55f, 2f)
                }
            }
        }
    }

    private fun buildInner(
        seed: Int,
        rng: com.jantursky.kascii.core.Rng,
        wins: ArrayList<Int>,
        phases: ArrayList<Float>,
    ) {
        val lat = OLattice(seed, 33)
        for (y in 0 until OROWS) {
            for (x in 0 until OCOLS) {
                val i = y * OCOLS + x
                val ux = x + 0.5f - GX
                val uy = (y + 0.5f) * O_ROW_U - GY
                val d = sqrt(ux * ux + uy * uy)
                if (d > RG || y >= BASE_TOP) continue
                val wy = (y + 0.5f) * O_ROW_U
                if (wy >= floorAt(x + 0.5f)) {
                    // Snow on the ground of the little world, lit warm from the windows.
                    val n = lat.fbm(x * 0.15f, y * 0.5f, 2)
                    val v = 0.42f + 0.2f * n - 0.16f * (wy - floorAt(x + 0.5f)) / 8f
                    inner.set(i, v * 0.86f, v * 0.9f, v * 1.02f, 2f)
                } else {
                    // Water in the glass: deep blue, a little lighter low down where the village lights reach.
                    val low = osmooth(-RG, RG * 0.5f, uy)
                    val v = 0.05f + 0.07f * low
                    inner.set(i, v * 0.55f, v * 0.8f, v * 1.6f, 0.3f + 0.25f * low)
                }
            }
        }
        // The village: a row of houses, a chapel and two pines on the snow.
        val houses = listOf(-26, -15, -2, 9, 20)
        for ((k, hx) in houses.withIndex()) {
            val cx = (GX + hx + rng.float() * 2f).toInt()
            val w = if (k == 2) 7 else 5 + rng.int(2)
            val ground = (floorAt(cx.toFloat()) / O_ROW_U).toInt()
            val wall = if (k == 2) 3 else 2
            val wallCol = if (k % 2 == 0) ocol("#7a2228") else ocol("#9a5a28")
            for (yy in ground - wall until ground) {
                for (xx in cx - w / 2..cx + w / 2) inner.put(xx, yy, wallCol, 2f)
            }
            // Roof: a snowy gable.
            val roofY = ground - wall - 1
            for (xx in cx - w / 2 - 1..cx + w / 2 + 1) inner.put(xx, roofY, ocol("#e8eef8"), 2f, '▄')
            for (xx in cx - w / 2 + 1..cx + w / 2 - 1) inner.put(xx, roofY - 1, ocol("#dfe6f2"), 2f, '▄')
            inner.put(cx - w / 2, roofY - 1, ocol("#c8d2e4"), 2f, '/')
            inner.put(cx + w / 2, roofY - 1, ocol("#c8d2e4"), 2f, '\\')
            if (k == 2) {
                // The chapel steeple.
                for (yy in roofY - 5 until roofY - 1) inner.put(cx, yy, ocol("#c8d2e4"), 2f, '█')
                inner.put(cx, roofY - 6, ocol("#ffe090"), 1f, '+')
            }
            for (xx in cx - w / 2 + 1..cx + w / 2 - 1 step 2) {
                wins.add((ground - 1) * OCOLS + xx)
                phases.add(rng.float())
            }
        }
        for (tx in listOf(-33, 29)) {
            val cx = (GX + tx).toInt()
            val ground = (floorAt(cx.toFloat()) / O_ROW_U).toInt()
            for (lvl in 0 until 4) {
                val yy = ground - 1 - lvl
                val half = 3 - lvl
                for (xx in cx - half..cx + half) {
                    val snowy = lvl % 2 == 1 && abs(xx - cx) < half
                    inner.put(xx, yy, if (snowy) ocol("#d8e4f4") else ocol("#2a6a44"), 2f, if (snowy) '▀' else '█')
                }
            }
            inner.put(cx, ground - 5, ocol("#d8e4f4"), 1f, '^')
        }
    }

    private fun buildGlass() {
        val lAng = -2.35f
        for (y in 0 until BASE_TOP) {
            for (x in 0 until OCOLS) {
                val i = y * OCOLS + x
                val ux = x + 0.5f - GX
                val uy = (y + 0.5f) * O_ROW_U - GY
                val d = sqrt(ux * ux + uy * uy)
                if (d > RG + 1.5f) continue
                val a = atan2(uy, ux)
                val rim = osmooth(RG - 2.2f, RG, d) * osmooth(RG + 1.2f, RG, d)
                // A curved highlight upper left and a small window reflection upper right.
                var da = a - lAng
                if (da > 3.1415927f) da -= OTAU
                if (da < -3.1415927f) da += OTAU
                val arc = ogauss(d, RG * 0.87f, 1.5f) * ogauss(da, 0f, 0.5f)
                val dot = ogauss(ux, -RG * 0.42f, 3f) * ogauss(uy, -RG * 0.58f, 2.2f)
                val wx = ux - RG * 0.5f
                val wyy = uy + RG * 0.42f
                val window =
                    if (abs(wx) < 6f && abs(wyy) < 5f && abs(wx) > 0.8f && abs(wyy) > 1.1f) 0.28f else 0f
                val k = 0.22f * rim + 0.85f * arc + 0.9f * dot + window
                if (k < 0.04f) continue
                glass.set(i, k * 0.85f, k * 0.92f, k, 1f)
            }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        // The shake: the globe rocks side to side for a moment at the start of every loop.
        val shake = if (t < SHAKE) 2.6f * sin(OTAU * 3f * t / SHAKE) * sin(3.1415927f * t / SHAKE) else 0f
        val ox = shake.roundToInt()
        frame.copyFrom(room)
        layer(inner, ox, false)
        windows(t, ox)
        flakes(t, ox)
        layer(glass, ox, true)
        base(ox)
        frame.output(grid, palette)
    }

    private fun layer(
        src: OFrame,
        ox: Int,
        add: Boolean,
    ) {
        for (y in 0 until OROWS) {
            for (x in 0 until OCOLS) {
                val sx = x - ox
                if (sx !in 0 until OCOLS) continue
                val j = y * OCOLS + sx
                if (src.cov[j] <= 0f) continue
                val i = y * OCOLS + x
                if (add) {
                    frame.light(i, OCol(src.r[j], src.g[j], src.b[j]), 1f)
                    if (src.r[j] > 0.5f) frame.cov[i] = max(frame.cov[i], 1.6f)
                } else {
                    frame.set(i, src.r[j], src.g[j], src.b[j], src.cov[j], src.ch[j])
                }
            }
        }
    }

    private fun windows(
        t: Float,
        ox: Int,
    ) {
        for (w in winCells.indices) {
            val i = winCells[w] + ox
            if (i < 0 || i >= OCELLS) continue
            val k = 0.8f + 0.2f * sin(OTAU * (2f * t / LOOP + winPhase[w]))
            frame.set(i, 1f * k, 0.72f * k, 0.3f * k, 2f)
            val y = i / OCOLS
            val x = i % OCOLS
            for (dy in -1..1) {
                for (dx in -3..3) {
                    val xx = x + dx
                    val yy = y + dy
                    if ((dx == 0 && dy == 0) || xx !in 0 until OCOLS || yy !in 0 until OROWS) continue
                    val j = yy * OCOLS + xx
                    frame.r[j] += 0.08f * k / (1f + abs(dx) + abs(dy))
                    frame.g[j] += 0.05f * k / (1f + abs(dx) + abs(dy))
                }
            }
        }
    }

    private fun flakes(
        t: Float,
        ox: Int,
    ) {
        for (f in 0 until FLAKES) {
            var x: Float
            var y: Float
            if (t < LOFT) {
                // Swept up from where it lay.
                flight(f, 0f, px)
                val p = osmooth(0f, 1f, t / LOFT)
                x = homeX[f] + (px[0] - homeX[f]) * p
                y = homeY[f] + (px[1] - homeY[f]) * p
            } else {
                flight(f, t - LOFT, px)
                x = px[0]
                y = px[1]
                if (y >= homeY[f]) {
                    x = homeX[f]
                    y = homeY[f]
                }
            }
            val cx = (x + ox).toInt()
            val cy = (y / O_ROW_U).toInt()
            if (cx !in 0 until OCOLS || cy !in 0 until BASE_TOP) continue
            val i = cy * OCOLS + cx
            val resting = y >= homeY[f] - 0.01f
            if (resting) {
                frame.light(i, OCol(0.9f, 0.95f, 1f), 0.15f)
                continue
            }
            val glyph =
                when (size[f]) {
                    2 -> '*'
                    1 -> '•'
                    else -> '·'
                }
            val v = 0.75f + 0.12f * size[f]
            frame.put(cx, cy, OCol(v * 0.92f, v * 0.96f, v), 1f, glyph)
        }
    }

    private fun base(ox: Int) {
        for (y in BASE_TOP until BASE_BOTTOM) {
            val k = (y - BASE_TOP).toFloat() / (BASE_BOTTOM - BASE_TOP - 1)
            val half = 27f + 8f * k
            for (x in (GX - half).toInt()..(GX + half).toInt()) {
                val xx = x + ox
                if (xx !in 0 until OCOLS) continue
                val u = (x + 0.5f - GX) / half
                val round = sqrt(max(0f, 1f - u * u * 0.85f))
                val lit = (0.4f + 0.6f * round) * (1f - 0.35f * u)
                if (y == BASE_TOP + 1 || y == BASE_TOP + 2) {
                    // A brass band with a moving lamp glint.
                    val g = lit * (0.75f + 0.3f * ogauss(u, -0.4f, 0.18f))
                    frame.put(xx, y, OCol(0.9f * g, 0.68f * g, 0.3f * g), 2f, if (y == BASE_TOP + 1) '▄' else '▀')
                    continue
                }
                val grain = 0.85f + 0.15f * sin(x * 0.35f + y * 2.1f + sin(x * 0.07f) * 3f)
                val v = lit * grain * (if (y == BASE_TOP) 0.7f else 0.55f - 0.05f * k)
                frame.put(xx, y, OCol(0.78f * v, 0.45f * v, 0.22f * v), 2f)
            }
        }
    }
}

/**
 * A snow globe on a wooden base: inside the glass a tiny village of lit houses, a chapel and two pines sits on snow.
 * Every loop the globe is shaken, the snow is thrown up and spun around, then drifts down flake by flake and settles
 * until the next shake. Glass highlights, a window reflection and a brass band catch the light. The seed shapes the
 * village and every flake's flight.
 */
public fun snowGlobePiece(seed: Int = 0): AsciiPiece {
    val scene = Globe(seed)
    return asciiPiece(
        name = "snow globe",
        ground = Rgb.fromHex("#03040a"),
        category = "objects",
        cols = OCOLS,
        rows = OROWS,
        fps = 24,
        still = 3.5f,
        loop = LOOP,
        note = "a snow globe with a tiny lit village, shaken so the snow swirls and settles, on a wooden base",
    ) { time, grid -> scene.draw(oLoopTime(time, LOOP), grid) }
}

/**
 * A snow globe with a tiny village, shaken once per loop so the snow swirls and settles.
 *
 * @param seed shapes the village and the flight of every flake.
 */
@Composable
public fun AsciiSnowGlobe(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A snow globe with a tiny village inside, snow swirling after a shake",
) {
    val piece = remember(seed) { snowGlobePiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
