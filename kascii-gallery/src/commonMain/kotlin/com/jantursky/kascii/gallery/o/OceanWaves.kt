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
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 24f
private const val HZ = 17
private const val FOCAL = 120f
private const val CAM_H = 5.5f
private const val SUN_X = 120f
private const val SUN_UP = 3.6f
private const val WAVES = 6

private class Sea(
    seed: Int,
) {
    private val frame = OFrame()
    private val sky = OFrame()
    private val palette: OPalette
    private val kx = floatArrayOf(0.6f, -1.1f, 1.7f, -2.6f, 3.6f, 1.5f)
    private val kz = floatArrayOf(1.6f, 1.3f, 2.1f, 3.2f, -2f, 5.8f)
    private val amp = floatArrayOf(0.22f, 0.13f, 0.06f, 0.03f, 0.02f, 0.012f)
    private val turns = intArrayOf(3, 4, 6, 9, 13, 17)
    private val phase = FloatArray(WAVES)
    private val wx = FloatArray(OCELLS)
    private val wz = FloatArray(OCELLS)
    private val vx = FloatArray(OCELLS)
    private val vy = FloatArray(OCELLS)
    private val vz = FloatArray(OCELLS)
    private val lod = Array(WAVES) { FloatArray(OCELLS) }
    private val foamNoise = FloatArray(OCELLS)
    private val sunX: Float
    private val sunY: Float
    private val sunZ: Float
    private val seedSalt = seed
    private val tmp = FloatArray(3)

    init {
        val rng = oRng(seed, 61)
        for (j in 0 until WAVES) phase[j] = rng.float() * OTAU
        val sx0 = (SUN_X - 96f) / FOCAL
        val sy0 = SUN_UP * O_ROW_U / FOCAL
        val sl = sqrt(sx0 * sx0 + sy0 * sy0 + 1f)
        sunX = (SUN_X - 96f) / FOCAL / sl
        sunY = SUN_UP * O_ROW_U / FOCAL / sl
        sunZ = 1f / sl
        val lat = OLattice(seed, 63)
        for (y in HZ until OROWS) {
            val s = y + 0.5f - HZ
            for (x in 0 until OCOLS) {
                val i = y * OCOLS + x
                // The view ray through this cell and where it meets the mean sea level.
                val dx = (x + 0.5f - 96f) / FOCAL
                val dy = -s * O_ROW_U / FOCAL
                val z = CAM_H / (-dy)
                val len = sqrt(dx * dx + dy * dy + 1f)
                vx[i] = dx / len
                vy[i] = dy / len
                vz[i] = 1f / len
                wx[i] = dx * z
                wz[i] = z
                // Far away a cell covers many short waves, so they fade out instead of aliasing.
                val foot = z * z * O_ROW_U / (FOCAL * CAM_H)
                for (j in 0 until WAVES) {
                    val k = sqrt(kx[j] * kx[j] + kz[j] * kz[j])
                    val f = k * foot
                    lod[j][i] = exp(-(f / 2.6f) * (f / 2.6f))
                }
                foamNoise[i] = lat.fbm(wx[i] * 0.9f, z * 0.9f, 3)
            }
        }
        buildSky(seed)
        palette = OPalette(buildPalette())
    }

    private fun buildPalette(): List<OCol> =
        oramp(
            10,
            ocol("#060818"),
            ocol("#141838"),
            ocol("#3a2a5a"),
            ocol("#8a4a6a"),
            ocol("#e07a5a"),
            ocol("#ffb070"),
        ) +
            oramp(10, ocol("#02060c"), ocol("#06141e"), ocol("#0c2834"), ocol("#1a4452"), ocol("#3a6a78")) +
            oramp(8, ocol("#5a3a3a"), ocol("#c07060"), ocol("#ffa070"), ocol("#ffd0a0")) +
            oramp(6, ocol("#a07030"), ocol("#ffd070"), ocol("#fff4c0"), ocol("#ffffff")) +
            oramp(5, ocol("#4a5a6a"), ocol("#a8b8c8"), ocol("#e8f0f8")) +
            listOf(ocol("#1a1a26"), ocol("#2a2a38"))

    /** Sky color seen along a direction of elevation [e] (0 at the horizon) and sideways [h]. */
    private fun skyAt(
        e: Float,
        h: Float,
        out: FloatArray,
    ) {
        val k = oclamp01(e * 5f)
        val sunward = exp(-((h - sunX) / 0.35f) * ((h - sunX) / 0.35f))
        val warmth = (1f - k) * (1f - k) * (0.5f + 0.5f * sunward)
        out[0] = 0.05f + 0.1f * (1f - k) + 0.8f * warmth
        out[1] = 0.06f + 0.05f * (1f - k) + 0.38f * warmth
        out[2] = 0.26f + 0.12f * (1f - k) * (1f - warmth) + 0.12f * warmth
    }

    private fun buildSky(seed: Int) {
        val lat = OLattice(seed, 65)
        for (y in 0 until HZ) {
            for (x in 0 until OCOLS) {
                val i = y * OCOLS + x
                val dx = (x + 0.5f - 96f) / FOCAL
                val dy = (HZ - y - 0.5f) * O_ROW_U / FOCAL
                skyAt(dy, dx, tmp)
                var r = tmp[0]
                var g = tmp[1]
                var b = tmp[2]
                // Thin streaks of cloud, their undersides lit by the low sun.
                val streak =
                    osmooth(0.58f, 0.74f, lat.fbm(x * 0.012f, y * 0.35f, 4)) * osmooth(1f, 5f, y.toFloat()) *
                        osmooth(HZ - 1f, HZ - 6f, y.toFloat())
                r = r * (1f - 0.3f * streak) + 0.4f * streak
                g = g * (1f - 0.3f * streak) + 0.18f * streak
                b = b * (1f - 0.3f * streak) + 0.18f * streak
                // The sun and its glow.
                val sdx = x + 0.5f - SUN_X
                val sdy = (y + 0.5f - (HZ - SUN_UP)) * O_ROW_U
                val sd = sqrt(sdx * sdx + sdy * sdy)
                val glow = 0.5f * exp(-sd / 9f) + 0.14f * exp(-sd / 30f)
                r += glow
                g += glow * 0.75f
                b += glow * 0.4f
                if (sd < 4.2f) {
                    sky.set(i, 1f, 0.95f, 0.8f, 2f)
                    continue
                }
                sky.tone(i, r, g, b, 0.26f)
            }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        frame.copyFrom(sky)
        sea(t)
        gull(t)
        frame.output(grid, palette)
    }

    private fun sea(t: Float) {
        val wt = FloatArray(WAVES) { OTAU * turns[it] * t / LOOP - phase[it] }
        val sparkle = (t * 8f).toInt()
        val ampSum = amp[0] + amp[1] + amp[2]
        for (y in HZ until OROWS) {
            for (x in 0 until OCOLS) {
                val i = y * OCOLS + x
                var h = 0f
                var hx = 0f
                var hz = 0f
                for (j in 0 until WAVES) {
                    val a = amp[j] * lod[j][i]
                    if (a < 0.002f) continue
                    // Swells roll toward the viewer: phase grows with distance minus time.
                    val p = kx[j] * wx[i] + kz[j] * wz[i] + wt[j]
                    val sp = sin(p)
                    // A sharpened profile: crests narrow, troughs broad.
                    val shape = sp + 0.25f * (sp * sp - 0.5f)
                    h += a * shape
                    val c = cos(p) * a * (1f + 0.5f * sp)
                    hx += c * kx[j]
                    hz += c * kz[j]
                }
                // Normal of the surface, then the mirror direction of the view ray.
                var nx = -hx
                var ny = 1f
                var nz = -hz
                val nl = sqrt(nx * nx + ny * ny + nz * nz)
                nx /= nl
                ny /= nl
                nz /= nl
                val dvn = vx[i] * nx + vy[i] * ny + vz[i] * nz
                val rx = vx[i] - 2f * dvn * nx
                var ry = vy[i] - 2f * dvn * ny
                val rz = vz[i] - 2f * dvn * nz
                if (ry < 0.002f) ry = 0.002f
                skyAt(ry, rx / max(0.2f, rz), tmp)
                val cosT = min(1f, max(0f, -dvn))
                val fres = 0.3f + 0.7f * (1f - cosT).pow(5)
                val sd = max(0f, rx * sunX + ry * sunY + rz * sunZ)
                val spec = sd.pow(900) * 2.5f + sd.pow(90) * 0.22f
                val depth = (y - HZ).toFloat() / (OROWS - HZ)
                // Light through the swell: crests facing us glow green-blue.
                val sss = max(0f, h) * 0.9f * (0.4f + depth)
                var r = 0.008f + 0.02f * sss
                var g = 0.05f + 0.04f * depth + 0.3f * sss
                var b = 0.08f + 0.06f * depth + 0.26f * sss
                val mirror = fres * 0.85f * (0.45f + 0.55f * osmooth(0f, 6f, (y - HZ).toFloat()))
                r = r * (1f - mirror) + tmp[0] * mirror + spec
                g = g * (1f - mirror) + tmp[1] * mirror + spec * 0.8f
                b = b * (1f - mirror) + tmp[2] * mirror + spec * 0.5f
                // Foam where crests run high, broken up by a static texture.
                val crest = h / ampSum
                val foam = osmooth(0.55f, 0.85f, crest + 0.35f * (foamNoise[i] - 0.5f)) * (0.3f + 0.7f * depth)
                r += (0.75f - r) * foam
                g += (0.8f - g) * foam
                b += (0.85f - b) * foam
                frame.tone(i, r, g, b, 0.2f)
                if (foam > 0.45f && frame.cov[i] < 1.4f) frame.cov[i] = 1.5f
                // Glitter: facets catching the sun flash on and off along its path.
                if (sd.pow(150) > 0.3f && Noise.hash(x, y, sparkle, seedSalt) > 0.82f) {
                    frame.set(i, 1f, 0.92f, 0.7f, 1f, if (sd > 0.995f) '*' else '+')
                } else if (foam > 0.3f && foam < 0.45f && frame.ch[i] == 0.toChar()) {
                    frame.ch[i] = '~'
                    frame.cov[i] = 1f
                }
            }
        }
    }

    private fun gull(t: Float) {
        val p = OTAU * t / LOOP
        val x = (66f + 44f * sin(p)).toInt()
        val y = (7f + 1.6f * sin(2f * p) + 0.8f * cos(3f * p)).toInt()
        val flap = ofract(t * 1.25f)
        // The wings beat for a while, then the gull glides.
        val gliding = sin(p * 2f) > 0.3f
        val (l, r) =
            if (gliding) {
                Pair('~', '~')
            } else if (flap < 0.5f) {
                Pair('\\', '/')
            } else {
                Pair('/', '\\')
            }
        val c = OCol(0.12f, 0.1f, 0.14f)
        frame.put(x - 1, y, c, 1f, l)
        frame.put(x, y, c, 1f, 'v')
        frame.put(x + 1, y, c, 1f, r)
    }
}

/**
 * The open sea toward a low sun, drawn like a shader: rolling swells made of several wave trains come toward the
 * viewer, each cell mirrors the sunset sky by its surface normal with a Fresnel blend, crests glow through and break
 * into foam, glitter flashes along the sun's path, cloud streaks catch the light and a distant gull wheels and glides.
 * The seed phases the wave trains and the clouds. Every wave repeats a whole number of times per loop.
 */
public fun oceanWavesPiece(seed: Int = 0): AsciiPiece {
    val scene = Sea(seed)
    return asciiPiece(
        name = "ocean waves",
        ground = Rgb.fromHex("#03060c"),
        category = "effects",
        cols = OCOLS,
        rows = OROWS,
        fps = 24,
        still = 5f,
        loop = LOOP,
        note = "a shader-like sea at sunset, rolling swells with foam, a glitter path, a sky gradient and a gull",
    ) { time, grid -> scene.draw(oLoopTime(time, LOOP), grid) }
}

/**
 * A sunset sea with rolling swells, foam and a glitter path, and a distant gull.
 *
 * @param seed phases the wave trains and shapes the clouds.
 */
@Composable
public fun AsciiOceanWaves(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Ocean swells rolling toward a low sun, glittering, with a gull in the distance",
) {
    val piece = remember(seed) { oceanWavesPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
