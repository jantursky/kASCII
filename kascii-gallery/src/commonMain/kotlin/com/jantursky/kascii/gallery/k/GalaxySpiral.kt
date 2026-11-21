package com.jantursky.kascii.gallery.k

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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 32f
private const val CX = 96f
private const val CY = 23.5f
private const val TILT = 0.5f
private const val ANGLE = -0.16f
private const val WIND = 3.1f
private const val DISK_STARS = 180
private const val LANE_COS = 0.7648422f
private const val LANE_SIN = 0.64421767f
private val LAYER_COUNT = intArrayOf(230, 110, 40)
private val LAYER_SWAY = floatArrayOf(1.2f, 3.4f, 7f)

private class Galaxy(
    seed: Int,
) {
    private val lat = KLattice(seed, 61)
    private val frame = KFrame()
    private val palette: KPalette
    private val radius = FloatArray(KCELLS)
    private val bulge = FloatArray(KCELLS)
    private val c2 = FloatArray(KCELLS)
    private val s2 = FloatArray(KCELLS)
    private val ca = FloatArray(KCELLS)
    private val sa = FloatArray(KCELLS)
    private val back = FloatArray(KCELLS)
    private val starR: FloatArray
    private val starA: FloatArray
    private val starHue: IntArray
    private val layerX: Array<FloatArray>
    private val layerY: Array<FloatArray>
    private val layerPhase: Array<FloatArray>
    private val core = kcol("#ffe6b0")
    private val armBlue = kcol("#8fb2ff")
    private val pink = kcol("#ff6fae")
    private val dustCol = kcol("#3a2414")
    private val halo = kcol("#3b2f6e")

    init {
        val turn = seed * 0.9f
        val nebula = KLattice(seed, 63)
        for (y in 0 until KROWS) {
            for (x in 0 until KCOLS) {
                val i = y * KCOLS + x
                val sx = x + 0.5f - CX
                val sy = (y + 0.5f - CY) * K_ROW_U
                val u = sx * cos(ANGLE) + sy * sin(ANGLE)
                val v = (-sx * sin(ANGLE) + sy * cos(ANGLE)) / TILT
                val r = sqrt(u * u + v * v) + 0.01f
                val th = atan2(v, u) + turn
                radius[i] = r
                bulge[i] = sqrt(sx * sx + sy * sy * 1.25f)
                c2[i] = cos(2f * th)
                s2[i] = sin(2f * th)
                val a = 2f * th - 2f * WIND * ln(r / 6f + 0.3f)
                ca[i] = cos(a)
                sa[i] = sin(a)
                back[i] = nebula.fbm(x * 0.02f, y * 0.06f, 3)
            }
        }
        val rng = kRng(seed, 7)
        // Disk stars come in pairs half a turn apart, so the half turn of the loop maps the set onto itself.
        starR = FloatArray(DISK_STARS)
        starA = FloatArray(DISK_STARS)
        starHue = IntArray(DISK_STARS)
        var k = 0
        while (k < DISK_STARS) {
            val r = 10f + 70f * rng.float()
            val onArm = WIND * ln(r / 6f + 0.3f) + (rng.float() - 0.5f) * 0.5f
            starR[k] = r
            starA[k] = onArm - turn
            starHue[k] = rng.int(3)
            starR[k + 1] = r
            starA[k + 1] = onArm - turn + 3.1415927f
            starHue[k + 1] = starHue[k]
            k += 2
        }
        layerX = Array(3) { l -> FloatArray(LAYER_COUNT[l]) { rng.float() * (KCOLS + 20f) - 10f } }
        layerY = Array(3) { l -> FloatArray(LAYER_COUNT[l]) { rng.float() * (KROWS + 6f) - 3f } }
        layerPhase = Array(3) { l -> FloatArray(LAYER_COUNT[l]) { rng.float() } }
        palette = KPalette(buildPalette())
    }

    private fun buildPalette(): List<KCol> =
        kramp(10, kcol("#1a1206"), kcol("#6a4a1c"), kcol("#c8904a"), kcol("#ffd890"), kcol("#fff8e8")) +
            kramp(10, kcol("#0a1030"), kcol("#22306c"), kcol("#4a64b8"), kcol("#8fb2ff"), kcol("#dce8ff")) +
            kramp(6, kcol("#3a0f2a"), kcol("#a02a6a"), kcol("#ff6fae"), kcol("#ffc0dc")) +
            kramp(6, kcol("#0e0806"), kcol("#2a1a10"), kcol("#5a3a22")) +
            kramp(8, kcol("#06040e"), kcol("#16123a"), kcol("#3b2f6e"), kcol("#7a6ab0")) +
            kramp(6, kcol("#4a4a5a"), kcol("#b8bcd0"), kcol("#ffffff")) +
            kramp(5, kcol("#5a4a2a"), kcol("#ffe0a0"), kcol("#fff4d8")) +
            kramp(5, kcol("#28345a"), kcol("#a8c4ff"), kcol("#eef4ff"))

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        // Half a turn per loop: the two arms (and the paired disk stars) land on each other.
        val phi = 3.1415927f * t / LOOP
        val cp = cos(2f * phi)
        val sp = sin(2f * phi)
        for (i in 0 until KCELLS) {
            val r = radius[i]
            // cos and sin of (2 theta - 2 phi) and of the arm phase minus 2 phi, by the angle-difference identities.
            val rc = c2[i] * cp + s2[i] * sp
            val rs = s2[i] * cp - c2[i] * sp
            val armPhase = ca[i] * cp + sa[i] * sp
            val dustPhase = armPhase * LANE_COS - (sa[i] * cp - ca[i] * sp) * LANE_SIN
            val n = lat.fbm(r * rc * 0.045f + 40f, r * rs * 0.045f + 40f, 4)
            val nf = lat.fbm(r * rc * 0.13f + 10f, r * rs * 0.13f, 2)
            val armBase = 0.5f + 0.5f * armPhase
            val arm = armBase * armBase * armBase * (0.45f + 1.1f * n)
            val disk = exp(-r / 34f) * ksmooth(96f, 55f, r)
            val dl = 0.5f + 0.5f * dustPhase
            val dust = dl * dl * dl * dl * ksmooth(7f, 16f, r) * ksmooth(80f, 50f, r) * ksmooth(0.35f, 0.6f, n + 0.15f)
            val knots = ksmooth(0.66f, 0.8f, nf) * arm * ksmooth(12f, 22f, r)
            val b = bulge[i]
            val core1 = exp(-(b / 6.5f) * (b / 6.5f))
            val core2 = exp(-b / 16f)
            var lr = 0f
            var lg = 0f
            var lb = 0f
            // The bulge: warm, saturating to cream at the very center.
            val cw = 0.8f * core1 + 0.28f * core2
            lr += core.r * cw
            lg += core.g * cw
            lb += core.b * cw
            // The disk and its blue arms.
            val dw = disk * (0.1f + 2.6f * arm)
            val blue = ksmooth(8f, 36f, r)
            lr += (core.r + (armBlue.r - core.r) * blue) * dw
            lg += (core.g + (armBlue.g - core.g) * blue) * dw
            lb += (core.b + (armBlue.b - core.b) * blue) * dw
            // Star-forming knots.
            val kw = knots * disk * 2.4f
            lr += pink.r * kw
            lg += pink.g * kw
            lb += pink.b * kw
            // Dust lanes absorb, warmest against the bright core.
            val absorb = 1f - 0.75f * dust
            lr = lr * absorb + dustCol.r * dust * disk * 1.6f
            lg = lg * absorb + dustCol.g * dust * disk * 1.6f
            lb = lb * absorb + dustCol.b * dust * disk * 1.6f
            // A faint violet halo and background nebula.
            val hz = 0.18f * exp(-r / 60f) + 0.07f * ksmooth(0.5f, 0.8f, back[i])
            lr += halo.r * hz
            lg += halo.g * hz
            lb += halo.b * hz
            val lum = 0.35f * lr + 0.45f * lg + 0.2f * lb
            val cov =
                if (lum < 0.24f) {
                    lum * 3.8f
                } else {
                    1.45f + min(0.55f, (lum - 0.24f) * 1.1f)
                }
            val k = if (cov > 1.4f) 1f else 1f / max(0.4f, lum * 3.8f)
            frame.set(i, lr * k, lg * k, lb * k, cov)
        }
        diskStars(phi)
        field(t)
        frame.output(grid, palette)
    }

    private fun diskStars(phi: Float) {
        for (k in 0 until DISK_STARS) {
            val a = starA[k] + phi
            val u = starR[k] * cos(a)
            val v = starR[k] * sin(a) * TILT
            val sx = u * cos(ANGLE) - v * sin(ANGLE)
            val sy = u * sin(ANGLE) + v * cos(ANGLE)
            val x = (CX + sx).toInt()
            val y = (CY + sy / K_ROW_U).toInt()
            if (x !in 0 until KCOLS || y !in 0 until KROWS || starR[k] < 18f) continue
            val c =
                when (starHue[k]) {
                    0 -> KCol(0.85f, 0.92f, 1f)
                    1 -> KCol(0.7f, 0.82f, 1f)
                    else -> KCol(1f, 0.85f, 0.95f)
                }
            frame.put(x, y, c, 1f, if (k % 5 == 0) '+' else '·')
        }
    }

    private fun field(t: Float) {
        val phi = KTAU * t / LOOP
        for (l in 0 until 3) {
            val ox = LAYER_SWAY[l] * cos(phi)
            val oy = LAYER_SWAY[l] * 0.5f * sin(phi) / K_ROW_U
            val xs = layerX[l]
            val ys = layerY[l]
            val ph = layerPhase[l]
            for (s in xs.indices) {
                val x = (xs[s] + ox).toInt()
                val y = (ys[s] + oy).toInt()
                if (x !in 0 until KCOLS || y !in 0 until KROWS) continue
                val i = y * KCOLS + x
                if (frame.cov[i] > 0.55f) continue
                val tw = 0.5f + 0.5f * sin(KTAU * (3f * t / LOOP + ph[s]))
                val warm = ph[s] > 0.7f
                when (l) {
                    0 -> {
                        frame.set(i, 0.32f, 0.34f, 0.45f, 0.7f, '.')
                    }

                    1 -> {
                        val b = 0.5f + 0.35f * tw
                        frame.set(
                            i,
                            b * (if (warm) 1f else 0.85f),
                            b * 0.9f,
                            b * (if (warm) 0.75f else 1f),
                            1f,
                            if (tw >
                                0.6f
                            ) {
                                '+'
                            } else {
                                '·'
                            },
                        )
                    }

                    else -> {
                        val b = 0.75f + 0.25f * tw
                        frame.set(i, b * (if (warm) 1f else 0.88f), b * 0.95f, b, 1f, if (tw > 0.5f) '*' else '+')
                        // A soft glint around the nearest stars.
                        for (dx in -1..1 step 2) {
                            val j = y * KCOLS + x + dx
                            if (x + dx in 0 until KCOLS &&
                                frame.cov[j] < 0.5f
                            ) {
                                frame.set(j, b * 0.5f, b * 0.5f, b * 0.6f, 1f, '-')
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A spiral galaxy seen at a tilt, turning slowly: a warm bright bulge, two blue arms flecked with pink star-forming
 * knots and cut by brown dust lanes, a violet halo, and three layers of foreground stars drifting with parallax as the
 * view sways. The seed shapes the arms, the dust and the star field. Every motion repeats a whole number of times per
 * loop.
 */
public fun galaxySpiralPiece(seed: Int = 0): AsciiPiece {
    val scene = Galaxy(seed)
    return asciiPiece(
        name = "galaxy spiral",
        ground = Rgb.fromHex("#020209"),
        category = "space",
        cols = KCOLS,
        rows = KROWS,
        fps = 20,
        still = 6f,
        loop = LOOP,
        note = "a tilted spiral galaxy turning, with dust lanes, a bright core and a parallax star field",
    ) { time, grid -> scene.draw(kLoopTime(time, LOOP), grid) }
}

/**
 * A slowly turning spiral galaxy with dust lanes and a bright core, in front of a drifting parallax star field.
 *
 * @param seed shapes the arms, the dust lanes and the star field.
 */
@Composable
public fun AsciiGalaxySpiral(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A spiral galaxy turning slowly among drifting stars",
) {
    val piece = remember(seed) { galaxySpiralPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
