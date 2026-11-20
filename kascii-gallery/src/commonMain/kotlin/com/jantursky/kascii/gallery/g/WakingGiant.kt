package com.jantursky.kascii.gallery.g

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
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 16f
private const val CX = 96
private const val STARS = 90
private const val BIRDS = 6

private const val NO_PART = 0
private const val HEAD = 1
private const val NECK = 2
private const val SHOULDER = 3
private const val EAR = 4
private const val CAP = 5
private const val MAST = 6

private const val EYE_Y = 13
private const val EYE_H = 5
private const val EYE_W = 12
private const val LEFT_EYE_X = 79
private const val RIGHT_EYE_X = 102
private const val PERCH_X = 101
private const val PERCH_Y = 5

/** The color of a robot's eyes, a warm amber. */
private val Amber: Rgb = Rgb.fromHex("#ff8a2a")

private class Giant(
    seed: Int,
    eyes: Rgb,
) {
    private val lat = Lattice(seed, 3)
    private val eye = Col(eyes.red / 255f, eyes.green / 255f, eyes.blue / 255f)
    private val sky = GFrame()
    private val robot = GFrame()
    private val front = GFrame()
    private val frame = GFrame()
    private val parts = IntArray(GCELLS)
    private val palette: GPalette
    private val eyeRamp: List<Col>
    private val fog1 = col("#3a7480")
    private val fog2 = col("#234a56")
    private val fog3 = col("#14303a")
    private val lowFog = Array(GROWS) { fog2.scale(1f - 0.8f * smooth(35f, 47f, it.toFloat())) }
    private val glowCol = eye.scale(0.9f)
    private val bird = col("#e6dcc2")
    private val starX = IntArray(STARS)
    private val starY = IntArray(STARS)
    private val starPhase = FloatArray(STARS)
    private val starRate = IntArray(STARS)
    private val birdY = FloatArray(BIRDS)
    private val birdPhase = FloatArray(BIRDS)
    private val slits = ArrayList<Int>()

    init {
        eyeRamp =
            ramp(8, col("#120808"), col("#3a140c"), col("#8a2a12"), eye, eye.mix(col("#fff2c0"), 0.75f))
        buildSky()
        buildRobot()
        buildFront(seed)
        val rng = AKit.rng(seed, 77)
        for (s in 0 until STARS) {
            var x: Int
            var y: Int
            do {
                x = rng.int(GCOLS)
                y = rng.int(24)
            } while (sky.cov[y * GCOLS + x] > 0.3f || parts[y * GCOLS + x] != NO_PART)
            starX[s] = x
            starY[s] = y
            starPhase[s] = rng.float()
            starRate[s] = 2 + rng.int(3)
        }
        for (b in 0 until BIRDS) {
            birdY[b] = rng.float(-3f, 3f)
            birdPhase[b] = rng.float()
        }
        palette = GPalette(buildPalette())
    }

    private fun buildPalette(): List<Col> {
        val midMetal = col("#4a7080")
        val midFog = col("#2b6570")
        val midSky = col("#14344a")
        val mixes = ArrayList<Col>()
        for (base in listOf(midMetal, midFog, midSky)) {
            for (k in listOf(0.25f, 0.5f, 0.75f)) {
                val c = base.mix(eye, k)
                mixes.add(c)
                mixes.add(c.scale(0.5f))
            }
        }
        return ramp(8, col("#050b18"), col("#0d2133"), col("#1d4d5a")) +
            ramp(7, col("#14363f"), col("#2b6570"), col("#6aa0a8")) +
            ramp(9, col("#0a141a"), col("#1f3641"), col("#4a7080"), col("#8fb6c4")) +
            ramp(4, col("#1c3d24"), col("#3f7a45"), col("#7fb066")) +
            eyeRamp +
            ramp(4, col("#7a98a0"), col("#cfe3e8"), col("#f4fbff")) +
            ramp(5, col("#010304"), col("#06141a"), col("#10303a")) +
            listOf(col("#ff3a30"), bird, col("#6f8fb0"), col("#cfe0ff")) +
            mixes
    }

    private fun buildSky() {
        for (y in 0 until GROWS) {
            val g = y / 36f
            val top = col("#050b18")
            val mid = col("#0d2133")
            val low = col("#1d4d5a")
            val c = if (g < 0.55f) top.mix(mid, g / 0.55f) else mid.mix(low, min(1f, (g - 0.55f) / 0.45f))
            for (x in 0 until GCOLS) {
                val n = lat.at(x * 0.09f, y * 0.25f) - 0.5f
                val cv = 0.5f * smooth(0.3f, 1.05f, g) + n * 0.12f
                sky.put(x, y, c, clamp01(cv), 0.toChar())
            }
        }
        // Moon with a halo.
        val mx = 156f
        val my = 8f
        val light = col("#bcd2d8")
        for (y in 0 until 30) {
            for (x in 0 until GCOLS) {
                val dx = x + 0.5f - mx
                val dy = (y + 0.5f - my) * ROW_U
                val d = sqrt(dx * dx + dy * dy)
                val i = y * GCOLS + x
                if (d < 12f) {
                    val crater = lat.fbm(x * 0.35f, y * 0.8f, 2)
                    val c = light.mix(col("#7a98a0"), smooth(0.4f, 0.8f, crater) * 0.45f)
                    sky.put(x, y, c, 2f)
                } else if (d < 46f) {
                    val h = exp(-((d - 12f) / 15f) * ((d - 12f) / 15f))
                    sky.r[i] += (0.28f - sky.r[i]) * h * 0.6f
                    sky.g[i] += (0.52f - sky.g[i]) * h * 0.6f
                    sky.b[i] += (0.6f - sky.b[i]) * h * 0.6f
                    sky.cov[i] = max(sky.cov[i], 0.75f * h)
                }
            }
        }
        val rng = AKit.rng(5, 19)
        // Far pines on the horizon.
        var x = -2
        while (x < GCOLS + 4) {
            val h = rng.int(6, 12)
            pine(sky, x, 35, h, col("#14363f"), 0.8f)
            x += rng.int(3, 6)
        }
        for (xx in 0 until GCOLS) {
            val top = 34 + (2.2f * sin(xx * 0.05f) + 1.5f * sin(xx * 0.13f)).toInt()
            for (y in top until GROWS) sky.put(xx, y, col("#0d2a33"), 2f)
        }
    }

    private fun pine(
        f: GFrame,
        cx: Int,
        base: Int,
        h: Int,
        c: Col,
        vary: Float,
    ) {
        val maxw = h * 0.45f
        for (k in 0 until h) {
            val y = base - h + 1 + k
            val tier = min(1f, ((k / 3) * 3 + 3).toFloat() / h)
            val hw = (maxw * tier + 0.5f).toInt()
            for (x in cx - hw..cx + hw) {
                val j = 1f - vary * 0.15f * ((x * 7 + y * 13) and 3) / 3f
                f.put(x, y, c.scale(j), 2f)
            }
        }
    }

    private fun inPart(
        x: Int,
        y: Int,
    ): Int {
        if (y in 2..5 && x == 84) return MAST
        if (y in 6..7 && x in 77..114) return CAP
        if (y in 8..24 && x in 72..119) {
            val lx = min(x - 72, 119 - x)
            val ly = min(y - 8, 24 - y)
            if (lx + ly * 2 >= 4) return HEAD
        }
        if (y in 12..20 && (x in 66..71 || x in 120..125)) {
            val lx = if (x < 96) x - 66 else 125 - x
            val ly = min(y - 12, 20 - y)
            if (lx + ly >= 1) return EAR
        }
        if (y in 25..28 && x in 87..104) return NECK
        val dx = abs(x - CX + 0.5f)
        if (y >= 26 && y >= 26 + max(0f, dx - 12f) * 0.2f && dx <= 86f) return SHOULDER
        val px = (dx - 62f) / 21f
        val py = (y - 34.5f) / 6.5f
        if (px * px + py * py <= 1f) return SHOULDER
        return NO_PART
    }

    private fun buildRobot() {
        for (y in 0 until GROWS) for (x in 0 until GCOLS) parts[y * GCOLS + x] = inPart(x, y)
        val dark = col("#0f1c23")
        val light = col("#9ec3d0")
        val moss = col("#3f7a45")
        val topEdge = IntArray(GCOLS) { x -> (0 until GROWS).firstOrNull { parts[it * GCOLS + x] != NO_PART } ?: 99 }
        for (y in 0 until GROWS) {
            for (x in 0 until GCOLS) {
                val i = y * GCOLS + x
                val p = parts[i]
                if (p == NO_PART) continue
                val n = lat.at(x * 0.4f, y * 0.9f) - 0.5f
                val k = clamp01(0.5f + 0.4f * (x - CX) / 50f - 0.2f * (y - 8) / 20f + n * 0.12f)
                var c = dark.mix(light, k)
                if (p == NECK) c = c.scale(0.55f)
                if (p == SHOULDER) c = c.scale(clamp01(0.85f - (y - 28) * 0.045f))
                if (p == EAR || p == MAST) c = c.scale(0.75f)
                val rim =
                    (x + 1 < GCOLS && parts[i + 1] == NO_PART) || (y > 0 && parts[i - GCOLS] == NO_PART && p != MAST)
                if (rim) c = c.mix(col("#e4f6ff"), 0.45f)
                if (y - topEdge[x] <= 2 && p != MAST && p != NECK && lat.fbm(x * 0.22f, y * 0.7f, 2) > 0.57f) {
                    robot.put(x, y, moss.mix(col("#7fb066"), n + 0.4f), 1.7f)
                    continue
                }
                robot.put(x, y, c, 2f)
            }
        }
        // Seams and rivets on the head.
        val seam = col("#16272f")
        for (x in 74..117) {
            robot.put(x, 11, seam, 1.7f)
            robot.put(x, 22, seam, 1.7f)
        }
        for (y in 8..10) robot.put(CX, y, seam, 1.7f)
        val rivet = col("#c9e6f2")
        for (p in listOf(74 to 9, 117 to 9, 74 to 23, 117 to 23, 69 to 14, 122 to 14, 69 to 18, 122 to 18)) {
            robot.put(p.first, p.second, rivet, 2f, '●')
        }
        // Eye sockets, then the lamp cells are drawn each frame.
        val socket = col("#08121a")
        for (ex in listOf(LEFT_EYE_X, RIGHT_EYE_X)) {
            for (y in EYE_Y - 1..EYE_Y + EYE_H) for (x in ex - 2 until ex + EYE_W + 2) robot.put(x, y, socket, 2f)
        }
        // Nose and mouth grille.
        for (y in 17..19) for (x in 94..97) robot.put(x, y, col("#0c1a22"), 1.7f)
        for (y in 20..23) {
            for (x in 82..110) {
                if ((x - 82) % 3 == 1) {
                    robot.put(x, y, col("#070f14"), 2f)
                    slits.add(y * GCOLS + x)
                } else {
                    robot.put(x, y, col("#2c4552"), 2f)
                }
            }
        }
        // Cables on the neck and bolts on the shoulders.
        for (x in listOf(89, 93, 99, 102)) for (y in 25..29) robot.put(x, y, col("#7f9aa6"), 2f, '│')
        for (x in listOf(28, 50, 142, 164)) robot.put(x, 33, col("#c9e6f2"), 2f, '●')
        for (x in 30..162) {
            if (parts[31 * GCOLS + x] == SHOULDER && (x % 8 != 0)) robot.put(x, 31, col("#1b3039"), 1.7f, '─')
        }
        // Antenna cap light is drawn each frame.
    }

    private fun buildFront(seed: Int) {
        val rng = AKit.rng(seed, 3)
        val far = col("#0a2028")
        val near = col("#03090c")
        var x = -6
        while (x < GCOLS + 6) {
            if (x < 40 || x > 152) pine(front, x, 46, rng.int(12, 26), far, 1f)
            x += rng.int(5, 9)
        }
        x = -4
        while (x < 36) {
            pine(front, x, 48, rng.int(30, 44), near, 1f)
            x += rng.int(6, 10)
        }
        x = 166
        while (x < GCOLS + 6) {
            pine(front, x, 48, rng.int(30, 36), near, 1f)
            x += rng.int(6, 10)
        }
        for (xx in 0 until GCOLS) {
            val top = 44 + (1.4f * sin(xx * 0.07f + 1f) + 1.0f * sin(xx * 0.19f)).toInt()
            for (y in top until GROWS) front.put(xx, y, col("#020608"), 2f)
        }
    }

    /** Eye level in 0..1 for one eye that wakes at [t0]. */
    private fun awake(
        t: Float,
        t0: Float,
    ): Float {
        val k = t - t0
        val rise =
            when {
                k < 0f -> 0f
                k < 1.4f -> if (sin(22f * k * (1f + k)) > 0.15f) 0.9f * k / 1.4f else 0f
                else -> 0.9f + 0.1f * smooth(1.4f, 2.4f, k)
            }
        return rise * (1f - smooth(12f, 14.5f, t))
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = GTAU * t / LOOP
        val lvlL = awake(t, 3f)
        val lvlR = awake(t, 3.7f)
        val eL = 0.1f + 0.9f * lvlL
        val eR = 0.1f + 0.9f * lvlR
        frame.copyFrom(sky)
        stars(t)
        fog(0, phi, 22, 40)
        frame.over(robot)
        eyes(eL, eR, t)
        glow(eL, eR, 16f, 0.7f, t)
        fog(1, phi, 30, 48)
        glow(eL, eR, 46f, 0.28f, t)
        flock(t)
        frame.over(front)
        fog(2, phi, 38, 48)
        frame.output(grid, palette)
    }

    private fun stars(t: Float) {
        for (s in 0 until STARS) {
            val i = starY[s] * GCOLS + starX[s]
            val tw = 0.55f + 0.45f * sin(GTAU * (starRate[s] * t / LOOP + starPhase[s]))
            if (tw < 0.3f || frame.cov[i] > 0.35f) continue
            val c = if (tw > 0.75f) Col(0.81f, 0.88f, 1f) else Col(0.44f, 0.56f, 0.69f)
            frame.put(starX[s], starY[s], c, 1f, if (tw > 0.75f) '*' else '.')
        }
    }

    private fun fog(
        layer: Int,
        phi: Float,
        y0: Int,
        y1: Int,
    ) {
        val dir = if (layer == 1) -1f else 1f
        val ox = 1.6f * cos(phi + layer * 2.1f) + layer * 17f
        val oy = dir * 1.1f * sin(phi + layer * 2.1f)
        val c =
            if (layer == 0) {
                fog1
            } else if (layer == 1) {
                fog2
            } else {
                fog3
            }
        for (y in y0 until y1) {
            for (x in 0 until GCOLS) {
                val i = y * GCOLS + x
                val n = lat.fbm(x * 0.03f + ox, y * 0.13f + oy, 3)
                when (layer) {
                    0 -> {
                        val d = gauss(y.toFloat(), 32f, 5f) * smooth(0.38f, 0.68f, n) * 0.8f
                        if (d > 0.01f) fogCell(i, c, d, d * 0.4f)
                    }

                    1 -> {
                        val edge = 38f + (n - 0.5f) * 12f
                        val d = smooth(edge - 3f, edge + 4f, y.toFloat())
                        if (d > 0.01f) fogCell(i, lowFog[y], d * 0.8f, d * 0.2f)
                    }

                    else -> {
                        val d = 0.5f * smooth(0.35f, 0.7f, n) * smooth(38f, 44f, y.toFloat())
                        if (d > 0.01f) fogCell(i, c, d * 0.7f, d * 0.4f)
                    }
                }
            }
        }
    }

    private fun fogCell(
        i: Int,
        c: Col,
        tint: Float,
        dots: Float,
    ) {
        frame.r[i] += (c.r - frame.r[i]) * tint
        frame.g[i] += (c.g - frame.g[i]) * tint
        frame.b[i] += (c.b - frame.b[i]) * tint
        if (frame.cov[i] < 1.4f) frame.cov[i] += (1f - frame.cov[i]) * dots
    }

    private fun eyes(
        eL: Float,
        eR: Float,
        t: Float,
    ) {
        for (side in 0..1) {
            val ex = if (side == 0) LEFT_EYE_X else RIGHT_EYE_X
            val e = if (side == 0) eL else eR
            for (j in 0 until EYE_H) {
                for (k in 0 until EYE_W) {
                    val inner = if (side == 0) k / (EYE_W - 1f) else 1f - k / (EYE_W - 1f)
                    val cut = (inner * 1.7f).toInt()
                    if (j < cut) continue
                    val centre = 1f - abs(j - 2f) * 0.18f - abs(k - 5.5f) * 0.025f
                    val idx = (clamp01(e * e * centre) * (eyeRamp.size - 1) + 0.5f).toInt()
                    frame.put(ex + k, EYE_Y + j, eyeRamp[idx], 2f)
                }
            }
        }
        val lit = min(eL, eR)
        for (i in slits) {
            val k = lit * 0.75f
            frame.r[i] = 0.03f + eye.r * 0.5f * k
            frame.g[i] = 0.05f + eye.g * 0.5f * k
            frame.b[i] = 0.07f + eye.b * 0.5f * k
        }
        // The mast lamp blinks.
        val on = fract(t / LOOP * 4f) < 0.3f
        frame.put(84, 1, if (on) Col(1f, 0.23f, 0.19f) else Col(0.35f, 0.08f, 0.08f), 2f, '●')
    }

    private fun glow(
        eL: Float,
        eR: Float,
        radius: Float,
        strength: Float,
        t: Float,
    ) {
        val breath = 1f + 0.06f * sin(GTAU * 3f * t / LOOP)
        for (side in 0..1) {
            val e = if (side == 0) eL else eR
            val level = (e - 0.1f) / 0.9f
            if (level <= 0.01f) continue
            val ex = (if (side == 0) LEFT_EYE_X + 5.5f else RIGHT_EYE_X + 5.5f)
            val ey = EYE_Y + 2.5f
            val rx = (radius * breath).toInt() + 3
            val ry = (radius * breath / ROW_U).toInt() + 2
            val inv = 1f / (radius * breath)
            for (y in max(0, ey.toInt() - ry)..min(GROWS - 1, ey.toInt() + ry)) {
                for (x in max(0, ex.toInt() - rx)..min(GCOLS - 1, ex.toInt() + rx)) {
                    val dx = x + 0.5f - ex
                    val dy = (y + 0.5f - ey) * ROW_U
                    val d2 = (dx * dx + dy * dy) * inv * inv
                    if (d2 > 4f) continue
                    val g = level * strength * exp(-d2 * 1.6f)
                    val i = y * GCOLS + x
                    frame.light(i, glowCol, if (frame.cov[i] > 1.4f) g * 0.3f else g)
                }
            }
        }
    }

    private fun flock(t: Float) {
        // The bird on the head leaves when the eyes flicker and comes back after they close.
        val flap = if ((t * 6f).toInt() % 2 == 0) 'v' else '^'
        var px = PERCH_X.toFloat()
        var py = PERCH_Y.toFloat()
        var flying = false
        if (t in 4.6f..7.6f) {
            val s = (t - 4.6f) / 3f
            px = PERCH_X + (215f - PERCH_X) * s * s
            py = PERCH_Y - 14f * s - 3f * sin(s * 6f)
            flying = true
        } else if (t in 13.2f..15.4f) {
            val s = (t - 13.2f) / 2.2f
            val e = 1f - (1f - s) * (1f - s)
            px = -10f + (PERCH_X + 10f) * e
            py = 14f + (PERCH_Y - 14f) * e - 3f * sin(s * 5f) * (1f - s)
            flying = true
        }
        if (flying) {
            frame.put(px.toInt(), py.toInt(), bird, 1f, flap)
        } else {
            frame.put(PERCH_X, PERCH_Y, bird, 2f, '▄')
            frame.put(PERCH_X + 1, PERCH_Y, bird, 2f, '●')
            frame.put(PERCH_X - 1, PERCH_Y, bird, 2f, '▗')
        }
        for (b in 0 until BIRDS) {
            val t0 = 5.4f + 0.18f * b
            if (t < t0 || t > t0 + 5.5f) continue
            val s = (t - t0) / 5.5f
            val x = 52f + 40f * b / BIRDS + (205f - 52f) * s
            val y = 33f - 4f * b - 42f * s + 2.5f * sin(GTAU * (s * 2.5f + birdPhase[b])) + birdY[b]
            val g = if (((t * 5f).toInt() + b) % 2 == 0) 'v' else '^'
            frame.put(x.toInt(), y.toInt(), bird, 1f, g)
        }
    }
}

/**
 * A giant robot sleeps half buried in a misty pine forest under a pale moon. Its eyes flicker, catch and burn, a
 * startled bird leaves its head, a flock follows, and then the eyes slowly close again. The seed shapes the forest,
 * the stars and the mist. Every motion repeats a whole number of times per loop.
 */
public fun wakingGiantPiece(
    seed: Int = 0,
    eyes: Rgb = Amber,
): AsciiPiece {
    val scene = Giant(seed, eyes)
    return asciiPiece(
        name = "waking giant",
        ground = Rgb.fromHex("#04080f"),
        category = "scenes",
        cols = GCOLS,
        rows = GROWS,
        fps = 15,
        still = 8f,
        loop = LOOP,
        note = "a giant robot wakes in a misty forest, its eyes flickering on while a bird bolts from its head",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A giant robot waking in a moonlit, misty pine forest: its eyes flicker on and glow, a bird flies off its head and a
 * flock scatters.
 *
 * @param seed shapes the forest, the stars and the mist.
 * @param eyes the color of the robot's eyes.
 */
@Composable
public fun AsciiWakingGiant(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    eyes: Rgb = Amber,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A giant robot waking in a misty forest, its eyes lighting up",
) {
    val piece = remember(seed, eyes) { wakingGiantPiece(seed, eyes) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
