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
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 10f
private const val TAU = AKit.TAU
private const val HEIGHT = F_ROWS * F_ASPECT

private const val FISH_X = 80f
private const val FISH_Y = 54f

private val PALETTE: FPalette by lazy {
    FPalette(
        arrayOf(
            floatArrayOf(0.20f, 0.30f, 0.55f),
            floatArrayOf(0.35f, 0.50f, 0.80f),
            floatArrayOf(0.75f, 0.88f, 1.00f),
            floatArrayOf(1.00f, 0.85f, 0.30f),
            floatArrayOf(1.00f, 0.60f, 0.15f),
            floatArrayOf(1.00f, 0.95f, 0.70f),
            floatArrayOf(0.65f, 0.40f, 1.00f),
            floatArrayOf(0.85f, 0.65f, 1.00f),
            floatArrayOf(0.65f, 1.00f, 0.35f),
            floatArrayOf(0.30f, 0.90f, 0.60f),
            floatArrayOf(0.80f, 0.25f, 0.15f),
            floatArrayOf(0.45f, 0.55f, 1.00f),
        ),
    )
}

private fun smooth(v: Float): Float {
    val k = v.coerceIn(0f, 1f)
    return k * k * (3f - 2f * k)
}

private class Jelly(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
    val r: Float,
    val g: Float,
    val b: Float,
    val dim: Float,
)

/** Everything that does not change from frame to frame, plus the per-frame canvas. */
private class AbyssScene(
    private val seed: Int,
    private val lure: FloatArray,
    jelly: FloatArray,
) {
    private val cv = FCanvas(PALETTE)
    private val baseR = FloatArray(F_CELLS)
    private val baseG = FloatArray(F_CELLS)
    private val baseB = FloatArray(F_CELLS)
    private val baseKind = ByteArray(F_CELLS)

    private val jellies =
        listOf(
            Jelly(166f, 34f, 17f, 0f, jelly[0], jelly[1], jelly[2], 1f),
            Jelly(22f, 26f, 11f, 2.1f, 0.62f, 1f, 0.34f, 0.7f),
            Jelly(180f, 84f, 8f, 4.0f, 0.45f, 0.55f, 1f, 0.55f),
        )

    private val snowX = FloatArray(SNOW)
    private val snowPh = FloatArray(SNOW)
    private val snowCycles = IntArray(SNOW)
    private val stemX = IntArray(STEMS)
    private val stemRow = IntArray(STEMS)
    private val stemLen = IntArray(STEMS)

    private var bulbX = 0f
    private var bulbY = 0f
    private var fishX = FISH_X
    private var fishY = FISH_Y
    private var preyX = 0f
    private var preyY = 0f
    private var preyDir = 1f
    private var gape = 0f

    init {
        val rng = AKit.rng(seed, 61)
        for (k in 0 until SNOW) {
            snowX[k] = rng.float() * F_COLS
            snowPh[k] = rng.float()
            snowCycles[k] = 1 + rng.int(2)
        }
        for (y in 0 until F_ROWS) {
            for (x in 0 until F_COLS) {
                val i = y * F_COLS + x
                val uy = (y + 0.5f) * F_ASPECT
                val k = uy / HEIGHT
                val n = Noise.fbm(x * 0.04f, y * 0.09f, 3f, 2, seed)
                val v = (0.16f * (1f - k) + 0.02f) * (0.5f + 0.9f * n)
                baseR[i] = 0.35f * v
                baseG[i] = 0.55f * v
                baseB[i] = v
                baseKind[i] = F_HALF
                val h = floorY(x + 0.5f)
                if (uy > h) {
                    val tex = 0.7f + 0.6f * Noise.fbm(x * 0.12f, y * 0.25f, 5f, 2, seed + 3)
                    val edge = (uy - h) / F_ASPECT
                    val light =
                        if (edge < 1.2f) {
                            0.34f
                        } else if (edge < 2.4f) {
                            0.2f
                        } else {
                            0.1f * tex
                        }
                    baseR[i] = light * 0.45f
                    baseG[i] = light * 0.65f
                    baseB[i] = light
                    baseKind[i] = F_DENSE
                }
            }
        }
        for (k in 0 until STEMS) {
            val x = 8 + k * 31 + rng.int(12)
            stemX[k] = x
            stemRow[k] = (floorY(x + 0.5f) / F_ASPECT).toInt()
            stemLen[k] = 3 + rng.int(4)
        }
    }

    private fun floorY(ux: Float): Float {
        val hill = fGlow((ux - 10f) * (ux - 10f), 30f) * 15f + fGlow((ux - 180f) * (ux - 180f), 24f) * 9f
        return 94f - hill + 5f * Noise.fbm(ux * 0.03f, 0f, 1f, 2, seed + 4)
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val theta = TAU * t / LOOP
        cv.load(baseR, baseG, baseB, baseKind)
        val pulse = 0.88f + 0.12f * sin(2f * theta)
        fishX = FISH_X + 1.5f * sin(theta)
        fishY = FISH_Y + 2.2f * sin(theta + 1f)
        bulbX = fishX + 46f + 4f * sin(theta + 0.6f)
        bulbY = fishY - 29f + 3f * cos(theta)
        preyX = 150f - 26f * cos(theta)
        preyY = 58f + 11f * sin(theta)
        preyDir = if (sin(theta) >= 0f) 1f else -1f
        val near = 0.5f + 0.5f * cos(theta)
        gape = near * near

        glow(bulbX, bulbY, 74f, 0.24f * pulse, lure)
        glow(bulbX, bulbY, 17f, 0.45f * pulse, lure)
        for (j in jellies) {
            val p = sin(2f * theta + j.phase)
            val jx = j.x + 7f * cos(theta + j.phase)
            val jy = j.y + 5f * sin(theta + j.phase) - 2.5f * p
            glow(jx, jy, j.radius * 2.6f, 0.30f * j.dim, floatArrayOf(j.r, j.g, j.b))
        }
        for (j in jellies) paintJelly(j, theta)
        paintStems(theta)
        paintFish(theta, pulse)
        paintPrey(theta)
        paintLure(pulse)
        paintSnow(theta, pulse)
        cv.write(grid)
    }

    private fun glow(
        cx: Float,
        cy: Float,
        radius: Float,
        gain: Float,
        c: FloatArray,
    ) {
        val x0 = max(0, (cx - radius).toInt())
        val x1 = min(F_COLS - 1, (cx + radius).toInt())
        val y0 = max(0, ((cy - radius) / F_ASPECT).toInt())
        val y1 = min(F_ROWS - 1, ((cy + radius) / F_ASPECT).toInt())
        for (y in y0..y1) {
            val dy = (y + 0.5f) * F_ASPECT - cy
            for (x in x0..x1) {
                val dx = x + 0.5f - cx
                val w = fGlow(dx * dx + dy * dy, radius) * gain
                if (w > 0f) cv.add(y * F_COLS + x, c[0] * w, c[1] * w, c[2] * w)
            }
        }
    }

    private fun paintStems(theta: Float) {
        for (k in 0 until STEMS) {
            val x = stemX[k]
            val top = stemRow[k] - stemLen[k]
            for (y in top until stemRow[k]) {
                if (y !in 0 until F_ROWS) continue
                cv.solid(y * F_COLS + x, 0.20f, 0.30f, 0.45f, '│')
            }
            val tip = top - 1
            if (tip in 0 until F_ROWS) {
                val p = 0.55f + 0.45f * sin(3f * theta + k * 1.7f)
                cv.solid(tip * F_COLS + x, 0.6f * p, 1f * p, 0.35f * p, '●')
            }
        }
    }

    private fun paintJelly(
        j: Jelly,
        theta: Float,
    ) {
        val p = sin(2f * theta + j.phase)
        val jx = j.x + 7f * cos(theta + j.phase)
        val jy = j.y + 5f * sin(theta + j.phase) - 2.5f * p
        val rx = j.radius * (1f + 0.12f * p)
        val ry = j.radius * 0.5f * (1f - 0.16f * p)
        // Tentacles first, so the bell covers their roots.
        val len = (j.radius * 0.9f).toInt() + 4
        for (k in 0..6) {
            val off = (k - 3) / 3f * 0.78f
            for (s in 0 until len) {
                val yy = jy + 0.1f * ry + s * F_ASPECT
                val sway = 0.9f * s * sin(2f * theta - s * 0.45f + k * 1.3f + j.phase) * (0.4f + 0.6f * s / len)
                val xx = jx + off * rx + sway
                val cx = xx.toInt()
                val cy = (yy / F_ASPECT).toInt()
                if (cx !in 0 until F_COLS || cy !in 0 until F_ROWS) continue
                val fade = (1f - s / len.toFloat()) * j.dim
                val glyph =
                    if (s < len * 0.6f) {
                        '│'
                    } else if (s < len * 0.85f) {
                        '╎'
                    } else {
                        '·'
                    }
                cv.solid(cy * F_COLS + cx, j.r * 0.8f * fade, j.g * 0.8f * fade, j.b * 0.8f * fade, glyph)
            }
        }
        val x0 = max(0, (jx - rx - 1f).toInt())
        val x1 = min(F_COLS - 1, (jx + rx + 1f).toInt())
        val y0 = max(0, ((jy - ry - 2f) / F_ASPECT).toInt())
        val y1 = min(F_ROWS - 1, ((jy + 0.25f * ry) / F_ASPECT).toInt())
        for (y in y0..y1) {
            val ny = ((y + 0.5f) * F_ASPECT - jy) / ry
            for (x in x0..x1) {
                val nx = (x + 0.5f - jx) / rx
                val d2 = nx * nx + ny * ny
                val inside = if (ny <= 0.1f) d2 < 1f else abs(nx) < 1f - ny * 0.8f && ny < 0.28f
                if (!inside) continue
                val ribs = 0.12f * sin(nx * 11f)
                val rim = if (d2 > 0.72f || ny > 0.05f) 0.35f else 0f
                val v = (0.30f + 0.22f * d2 + ribs + rim) * j.dim
                cv.solid(y * F_COLS + x, j.r * v, j.g * v, j.b * v)
            }
        }
    }

    private fun paintFish(
        theta: Float,
        pulse: Float,
    ) {
        val x0 = max(0, (fishX - 72f).toInt())
        val x1 = min(F_COLS - 1, (fishX + 42f).toInt())
        val y0 = max(0, ((fishY - 30f) / F_ASPECT).toInt())
        val y1 = min(F_ROWS - 1, ((fishY + 40f) / F_ASPECT).toInt())
        val eyeX = 17f
        val eyeY = -6f
        val lookX = (preyX - fishX - eyeX).coerceIn(-30f, 30f) / 30f
        val lookY = (preyY - fishY - eyeY).coerceIn(-30f, 30f) / 30f
        val finAngle = 0.55f + 0.4f * sin(2f * theta + 1f)
        val fc = cos(finAngle)
        val fs = sin(finAngle)
        for (y in y0..y1) {
            val py = (y + 0.5f) * F_ASPECT - fishY
            for (x in x0..x1) {
                val px = x + 0.5f - fishX
                val i = y * F_COLS + x
                val ux = x + 0.5f
                val uy = py + fishY
                val lit = fGlow((ux - bulbX) * (ux - bulbX) + (uy - bulbY) * (uy - bulbY), 96f) * pulse
                val tex = 0.05f * Noise.value(ux * 0.2f, uy * 0.2f, 2f, seed + 8)
                // Tail: a tapering stem that ends in a fan, waving.
                if (px < -24f) {
                    val u = (-24f - px) / 42f
                    if (u < 1f) {
                        val ofs = 7f * u * sin(2f * theta - 5f * u)
                        val h =
                            if (u < 0.55f) {
                                14f - 9f * u / 0.55f
                            } else {
                                5f + 10f * ((u - 0.55f) / 0.45f)
                            }
                        val rel = py - ofs
                        if (abs(rel) < h) {
                            val fan = if (u > 0.55f) 0.18f + 0.1f * sin(rel * 1.3f) else 0.2f
                            val v = fan * 0.6f + tex + lit * 0.2f
                            cv.solid(i, v * 0.55f + lit * 0.12f, v * 0.75f + lit * 0.1f, v * 1.2f)
                        }
                    }
                }
                // Pectoral fin.
                val fdx = px + 4f
                val fdy = py - 13f
                val fu = fdx * fc + fdy * fs
                val fv = -fdx * fs + fdy * fc
                if ((fu / 11f) * (fu / 11f) + (fv / 3.8f) * (fv / 3.8f) < 1f) {
                    val v = 0.20f + lit * 0.25f
                    cv.solid(i, v * 0.55f, v * 0.8f, v * 1.2f)
                }
                val qE = (px / 32f) * (px / 32f) + (py / 24f) * (py / 24f)
                val qC = ((px - 16f) / 26f) * ((px - 16f) / 26f) + ((py - 22f) / 11f) * ((py - 22f) / 11f)
                if (qE >= 1f && qC >= 1f) continue
                val lip = 8f - gape * 0.46f * max(0f, px - 4f)
                val low = 8f + 11f * min(1f, (px - 4f) / 12f) - 0.1f * max(0f, px - 16f)
                if (px > 4f && py > lip && py < low) {
                    // Inside the mouth.
                    val warm = 0.10f + 0.28f * lit
                    cv.solid(i, warm * 1.4f + lit * 0.1f, warm * 0.5f, warm * 0.35f)
                    val tooth = if (Noise.hash(x, 0, 5, seed) > 0.4f) 6.8f else 4.4f
                    if (x % 3 == 0 && py - lip < tooth) cv.solid(i, 0.95f, 0.95f, 0.82f, '│')
                    val lower = if (Noise.hash(x, 1, 5, seed) > 0.4f) 6.8f else 4.4f
                    if ((x + 1) % 3 == 0 && low - py < lower) cv.solid(i, 0.95f, 0.95f, 0.82f, '│')
                    continue
                }
                val q = min(qE, qC)
                val rim = if (q > 0.72f) (q - 0.72f) * 1.1f else 0f
                val topLit = if (py < 0f) 0.08f else 0f
                val base = 0.07f + tex + topLit
                cv.solid(
                    i,
                    base * 0.55f + lit * lit * (0.34f + rim * 0.3f) + rim * 0.12f,
                    base * 0.8f + lit * lit * (0.26f + rim * 0.3f) + rim * 0.3f,
                    base * 1.25f + lit * lit * 0.06f + rim * 0.62f,
                )
            }
        }
        // Light spots along the belly.
        for (k in 0 until 7) {
            val px = -22f + k * 7f
            val py = 0.74f * 24f * sqrt(1f - (px / 32f) * (px / 32f))
            val cx = (fishX + px).toInt()
            val cy = ((fishY + py) / F_ASPECT).toInt()
            if (cx !in 0 until F_COLS || cy !in 0 until F_ROWS) continue
            val p = 0.55f + 0.45f * sin(2f * theta + k * 0.9f)
            cv.solid(cy * F_COLS + cx, 0.3f * p, 0.9f * p, 0.8f * p, '•')
        }
        // Eye.
        val ex = fishX + eyeX
        val ey = fishY + eyeY
        for (y in max(0, ((ey - 6f) / F_ASPECT).toInt())..min(F_ROWS - 1, ((ey + 6f) / F_ASPECT).toInt())) {
            val dy = (y + 0.5f) * F_ASPECT - ey
            for (x in max(0, (ex - 7f).toInt())..min(F_COLS - 1, (ex + 7f).toInt())) {
                val dx = x + 0.5f - ex
                val d2 = dx * dx + dy * dy
                if (d2 < 19f) {
                    val pdx = dx - lookX * 1.8f
                    val pdy = dy - lookY * 1.4f
                    val c = y * F_COLS + x
                    if (pdx * pdx + pdy * pdy < 3.6f) cv.cut(c) else cv.solid(c, 0.9f, 0.75f, 0.3f, '█')
                }
            }
        }
    }

    private fun paintPrey(theta: Float) {
        val f = preyDir
        val wag = sin(6f * theta)
        val x0 = max(0, (preyX - 14f).toInt())
        val x1 = min(F_COLS - 1, (preyX + 14f).toInt())
        val y0 = max(0, ((preyY - 8f) / F_ASPECT).toInt())
        val y1 = min(F_ROWS - 1, ((preyY + 8f) / F_ASPECT).toInt())
        for (y in y0..y1) {
            val py = (y + 0.5f) * F_ASPECT - preyY
            for (x in x0..x1) {
                val px = (x + 0.5f - preyX) * f
                val i = y * F_COLS + x
                val ldx = x + 0.5f - bulbX
                val ldy = py + preyY - bulbY
                val lit = fGlow(ldx * ldx + ldy * ldy, 70f)
                val v = 0.55f + 0.4f * lit
                if ((px / 8f) * (px / 8f) + (py / 4.4f) * (py / 4.4f) < 1f) {
                    cv.solid(i, v * 0.4f, v * 1f, v * 0.85f)
                } else if (px < -5.5f && px > -14f && abs(py - wag * (-px - 5f) * 0.3f) < (-px - 5.5f) * 0.65f + 0.4f) {
                    cv.solid(i, v * 0.2f, v * 0.6f, v * 0.7f)
                }
                if (abs(px - 4.6f) < 1.6f && abs(py + 0.5f) < 1.3f) cv.cut(i)
            }
        }
    }

    private fun paintLure(pulse: Float) {
        // The stalk is a quadratic curve from the crown of the head to the bulb.
        val p0x = fishX + 14f
        val p0y = fishY - 22f
        val p1x = fishX + 24f
        val p1y = fishY - 58f
        for (s in 0..90) {
            val u = s / 90f
            val a = (1f - u) * (1f - u)
            val b = 2f * u * (1f - u)
            val c = u * u
            val xx = a * p0x + b * p1x + c * bulbX
            val yy = a * p0y + b * p1y + c * bulbY
            val cx = xx.toInt()
            val cy = (yy / F_ASPECT).toInt()
            if (cx !in 0 until F_COLS || cy !in 0 until F_ROWS) continue
            val warm = 0.25f + 0.5f * u
            cv.solid(cy * F_COLS + cx, warm * lure[0], warm * lure[1], warm * lure[2] + 0.1f, '█')
        }
        val x0 = max(0, (bulbX - 6f).toInt())
        val x1 = min(F_COLS - 1, (bulbX + 6f).toInt())
        val y0 = max(0, ((bulbY - 6f) / F_ASPECT).toInt())
        val y1 = min(F_ROWS - 1, ((bulbY + 6f) / F_ASPECT).toInt())
        for (y in y0..y1) {
            val dy = (y + 0.5f) * F_ASPECT - bulbY
            for (x in x0..x1) {
                val dx = x + 0.5f - bulbX
                val d2 = dx * dx + dy * dy
                if (d2 < 30f) {
                    val core = if (d2 < 12f) 1f else 0.8f
                    cv.solid(
                        y * F_COLS + x,
                        min(1f, lure[0] * core * pulse + 0.2f),
                        min(1f, lure[1] * core * pulse + 0.25f),
                        min(1f, lure[2] * core * pulse + 0.3f),
                        '█',
                    )
                }
            }
        }
    }

    private fun paintSnow(
        theta: Float,
        pulse: Float,
    ) {
        for (k in 0 until SNOW) {
            val ph = snowPh[k]
            val yy = ((ph + snowCycles[k] * theta / TAU) % 1f) * HEIGHT
            val xx = snowX[k] + 1.6f * sin(2f * theta + ph * 20f)
            val cx = xx.toInt()
            val cy = (yy / F_ASPECT).toInt()
            if (cx !in 0 until F_COLS || cy !in 0 until F_ROWS) continue
            val i = cy * F_COLS + cx
            if (cv.kind[i] != F_HALF || cv.glyph[i] != 0.toChar()) continue
            val lit = fGlow((xx - bulbX) * (xx - bulbX) + (yy - bulbY) * (yy - bulbY), 80f) * pulse
            val v = 0.16f + 0.5f * lit
            val ch = if (lit > 0.35f) '•' else '·'
            cv.solid(i, v * (0.6f + 0.4f * lure[0]), v * (0.7f + 0.3f * lure[1]), v * (0.9f - 0.3f * lure[2]), ch)
        }
    }

    private companion object {
        const val SNOW = 90
        const val STEMS = 6
    }
}

/**
 * A deep-sea anglerfish in the dark water, drawn cell by cell: a big toothy fish whose glowing lure lights the
 * murk gold, a tiny fish that circles the light and the jaws that open wider each time it comes near, three
 * drifting jellyfish that pulse with their own colour, marine snow sinking past and glowing tube worms on the
 * seabed. The seed shapes the water and the snow; every motion repeats in whole cycles of the loop.
 */
public fun abyssLurePiece(
    seed: Int = 0,
    lure: Rgb = Rgb.fromHex("#ffcf45"),
    jelly: Rgb = Rgb.fromHex("#a77bff"),
): AsciiPiece {
    val scene =
        AbyssScene(
            seed,
            floatArrayOf(lure.red / 255f, lure.green / 255f, lure.blue / 255f),
            floatArrayOf(jelly.red / 255f, jelly.green / 255f, jelly.blue / 255f),
        )
    return asciiPiece(
        name = "abyss lure",
        ground = Rgb.fromHex("#02060d"),
        category = "scenes",
        cols = F_COLS,
        rows = F_ROWS,
        fps = 20,
        still = 3f,
        loop = LOOP,
        note = "an anglerfish dangles its lamp in the deep while a little fish circles the light",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A deep-sea anglerfish with a glowing lure, a curious little fish and drifting jellyfish.
 *
 * @param seed shapes the water, the marine snow and the seabed.
 * @param lure the color of the glowing lure.
 * @param jelly the color of the largest jellyfish.
 */
@Composable
public fun AsciiAbyssLure(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    lure: Rgb = Rgb.fromHex("#ffcf45"),
    jelly: Rgb = Rgb.fromHex("#a77bff"),
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "An anglerfish in the deep sea, its glowing lure lighting a small fish and jellyfish",
) {
    val piece = remember(seed, lure, jelly) { abyssLurePiece(seed, lure, jelly) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
