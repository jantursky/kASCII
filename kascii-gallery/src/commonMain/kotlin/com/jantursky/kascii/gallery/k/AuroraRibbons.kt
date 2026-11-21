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
import com.jantursky.kascii.core.Rng
import com.jantursky.kascii.core.asciiPiece
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val LOOP = 20f
private const val STARS = 170
private const val CURTAINS = 3

private class Aurora(
    seed: Int,
) {
    private val lat = KLattice(seed, 71)
    private val frame = KFrame()
    private val land = KFrame()
    private val palette: KPalette
    private val ridge = IntArray(KCOLS)
    private val starX = IntArray(STARS)
    private val starY = IntArray(STARS)
    private val starPhase = FloatArray(STARS)
    private val base = FloatArray(CURTAINS)
    private val height = FloatArray(CURTAINS)
    private val wave = FloatArray(CURTAINS)
    private val shift = FloatArray(CURTAINS)
    private val bright = FloatArray(CURTAINS)
    private val glowR = FloatArray(KCELLS)
    private val glowG = FloatArray(KCELLS)
    private val glowB = FloatArray(KCELLS)
    private val glowA = FloatArray(KCELLS)
    private val column = FloatArray(KCOLS)
    private val edge = FloatArray(KCOLS)
    private val rays = FloatArray(KCOLS)
    private val green = kcol("#5dffa0")
    private val deep = kcol("#1fbf6a")
    private val violet = kcol("#8a4bff")
    private val fringe = kcol("#ff4f8f")

    init {
        val rng = kRng(seed, 5)
        for (c in 0 until CURTAINS) {
            base[c] = 18f + c * 5f + rng.float() * 3f
            height[c] = 11f + rng.float() * 5f - c * 2f
            wave[c] = 0.018f + rng.float() * 0.02f
            shift[c] = rng.float() * KTAU
            bright[c] = 1f - c * 0.22f
        }
        for (s in 0 until STARS) {
            starX[s] = rng.int(KCOLS)
            starY[s] = rng.int(38)
            starPhase[s] = rng.float()
        }
        val mlat = KLattice(seed, 73)
        for (x in 0 until KCOLS) {
            val m = mlat.fbm(x * 0.025f, 1f, 4)
            ridge[x] = (45f - 10f * ksmooth(0.3f, 0.75f, m) - 1.5f * mlat.at(x * 0.2f, 5f)).toInt()
        }
        buildLand(seed, rng)
        palette = KPalette(buildPalette())
    }

    private fun buildPalette(): List<KCol> =
        kramp(8, kcol("#02040c"), kcol("#061424"), kcol("#0c2a36"), kcol("#1a3e4a")) +
            kramp(10, kcol("#06301c"), kcol("#0f7a44"), kcol("#1fbf6a"), kcol("#5dffa0"), kcol("#d8ffe8")) +
            kramp(8, kcol("#1a0c3a"), kcol("#4a2496"), kcol("#8a4bff"), kcol("#c8a8ff")) +
            kramp(5, kcol("#3a0a22"), kcol("#a02a5a"), kcol("#ff4f8f")) +
            kramp(6, kcol("#1a5a6a"), kcol("#3ac8b0"), kcol("#9af0d8")) +
            kramp(8, kcol("#0a0e18"), kcol("#1c2a38"), kcol("#40586a"), kcol("#8aa8b8"), kcol("#d0ecf0")) +
            kramp(4, kcol("#02060a"), kcol("#06100e"), kcol("#0c1c18")) +
            kramp(5, kcol("#4a5068"), kcol("#c8d0e8"), kcol("#ffffff"))

    private fun buildLand(
        seed: Int,
        rng: Rng,
    ) {
        val nl = KLattice(seed, 77)
        for (x in 0 until KCOLS) {
            for (y in ridge[x] until KROWS) {
                val depth = (y - ridge[x]).toFloat()
                // Snow on the upper slopes, lit from the left, darker rock lower down.
                val slope = (ridge[min(KCOLS - 1, x + 1)] - ridge[max(0, x - 1)]).toFloat()
                val snow = ksmooth(9f, 2f, depth) * (0.55f + 0.45f * nl.at(x * 0.4f, y * 0.8f))
                val k = 0.04f + 0.17f * snow * (0.7f - 0.12f * slope)
                land.put(x, y, KCol(k * 0.85f, k * 0.95f, k * 1.15f), if (snow > 0.25f) 2f else 1.75f)
            }
        }
        // A band of pines along the foreground.
        var x = -2
        while (x < KCOLS + 2) {
            val h = 4 + rng.int(5)
            val foot = 47 + rng.int(2)
            for (k in 0 until h) {
                val y = foot - h + k
                val half = (k * 0.55f).toInt()
                for (dx in -half..half) {
                    val px = x + dx
                    if (px in 0 until KCOLS && y in 0 until KROWS) land.put(px, y, KCol(0.012f, 0.03f, 0.028f), 2f)
                }
            }
            for (y in foot until KROWS) if (x in 0 until KCOLS) land.put(x, y, KCol(0.012f, 0.03f, 0.028f), 2f)
            for (y in 46 until KROWS) {
                for (px in x - 3..x + 3) {
                    if (px in
                        0 until KCOLS
                    ) {
                        land.put(px, y, KCol(0.012f, 0.03f, 0.028f), 2f)
                    }
                }
            }
            x += 2 + rng.int(5)
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = KTAU * t / LOOP
        glowR.fill(0f)
        glowG.fill(0f)
        glowB.fill(0f)
        glowA.fill(0f)
        rays.fill(0f)
        var total = 0f
        for (c in 0 until CURTAINS) total += curtain(c, phi)
        sky(t)
        val shine = min(1f, total / (KCOLS * 1.6f))
        for (i in 0 until KCELLS) {
            if (land.cov[i] <= 0f) continue
            val y = i / KCOLS
            // Snow catches the green light; the trees stay black.
            val lift = shine * 0.5f * land.r[i] * 4f * (1f - y / KROWS.toFloat() * 0.4f)
            frame.set(
                i,
                land.r[i] + deep.r * lift * 0.3f,
                land.g[i] + deep.g * lift * 0.5f,
                land.b[i] + deep.b * lift * 0.35f,
                land.cov[i],
            )
        }
        frame.output(grid, palette)
    }

    /** Accumulates curtain [c] into the glow buffers; returns how much light it gave. */
    private fun curtain(
        c: Int,
        phi: Float,
    ): Float {
        val ox = 6f * cos(phi + shift[c])
        val oy = 6f * sin(phi + shift[c])
        val k1 = (c % 2) * 2 - 1
        var sum = 0f
        for (x in 0 until KCOLS) {
            // The bottom edge folds and slides; the rays shimmer along it.
            val fold = 7f * sin(x * wave[c] * 2.1f + (2 + c) * phi * k1 + shift[c])
            val xf = x + fold
            edge[x] =
                base[c] + 6.5f * sin(xf * wave[c] + phi * k1 + shift[c]) +
                2f * sin(xf * wave[c] * 2.7f - 2f * phi + c) +
                3f * (lat.at(xf * 0.03f + ox * 0.2f, c * 7f + oy * 0.2f) - 0.5f)
            val ray = lat.fbm(xf * 0.38f + ox, c * 13f + oy, 2)
            val wide = lat.at(xf * 0.02f + c * 3f + ox * 0.3f, 40f + oy * 0.3f)
            column[x] =
                (0.35f + 0.75f * ksmooth(0.3f, 0.75f, ray)) * (0.45f + 0.65f * ksmooth(0.2f, 0.7f, wide)) * bright[c] *
                0.8f
            rays[x] = max(rays[x], column[x])
        }
        val h = height[c]
        for (x in 0 until KCOLS) {
            val e = edge[x]
            val col = column[x]
            if (col < 0.05f) continue
            val y0 = max(0, (e - h * 2.6f).toInt())
            val y1 = min(KROWS - 1, (e + 2f).toInt())
            for (y in y0..y1) {
                val d = e - (y + 0.5f)
                val v: Float
                val up: Float
                if (d >= 0f) {
                    up = d / (h * 1.5f)
                    val rim = (d - 0.9f) * (d - 0.9f)
                    v = col * (exp(-d / (h * 1.3f)) * 0.8f + 0.9f * exp(-rim / 0.9f)) * ksmooth(0f, 0.8f, d + 0.6f)
                } else {
                    up = 0f
                    v = col * exp(-(d / 0.9f) * (d / 0.9f))
                }
                if (v < 0.02f) continue
                // Green at the edge, violet higher up, a pink fringe right below the edge.
                var r: Float
                var g: Float
                var b: Float
                val tv = ksmooth(0.15f, 0.85f, up)
                val lift = 1f + 0.6f * tv
                r = green.r + (violet.r - green.r) * tv
                g = green.g + (violet.g - green.g) * tv
                b = green.b + (violet.b - green.b) * tv
                if (d < 0.3f) {
                    val f = ksmooth(0.3f, -1.2f, d)
                    r += (fringe.r - r) * f
                    g += (fringe.g - g) * f
                    b += (fringe.b - b) * f
                }
                val i = y * KCOLS + x
                glowR[i] += r * v * lift
                glowG[i] += g * v * lift
                glowB[i] += b * v * lift
                glowA[i] += v * lift
                sum += v
            }
        }
        return sum
    }

    private fun sky(t: Float) {
        for (y in 0 until KROWS) {
            val k = y / KROWS.toFloat()
            for (x in 0 until KCOLS) {
                val i = y * KCOLS + x
                if (land.cov[i] > 0f) continue
                val a = glowA[i]
                var r = 0.01f + 0.03f * k * k
                var g = 0.02f + 0.07f * k * k
                var b = 0.05f + 0.1f * k * k
                var cov = 0.03f + 0.12f * k * k
                var glyph = 0.toChar()
                if (a > 0.02f) {
                    val inv = 1f / a
                    val cr = glowR[i] * inv
                    val cg = glowG[i] * inv
                    val cb = glowB[i] * inv
                    val m = min(1f, a * 1.6f)
                    r += (cr - r) * m
                    g += (cg - g) * m
                    b += (cb - b) * m
                    // Bright light gets a whiter core.
                    val hot = ksmooth(0.9f, 1.6f, a)
                    r += (1f - r) * hot * 0.35f
                    g += (1f - g) * hot * 0.35f
                    b += (1f - b) * hot * 0.35f
                    cov =
                        if (a < 0.85f) {
                            max(cov, a * 1.2f)
                        } else {
                            1.45f + min(0.55f, (a - 0.85f) * 0.9f)
                        }
                    // Ray texture: vertical strokes where the curtain is thin.
                    if (a in 0.18f..0.85f && rays[x] > 0.7f && glowB[i] > glowG[i] * 0.45f) glyph = '│'
                }
                frame.set(i, r, g, b, cov, glyph)
            }
        }
        for (s in 0 until STARS) {
            val i = starY[s] * KCOLS + starX[s]
            if (land.cov[i] > 0f || glowA[i] > 0.35f) continue
            val tw = 0.5f + 0.5f * sin(KTAU * (2f * t / LOOP + starPhase[s]))
            if (tw < 0.2f) continue
            val c = 0.5f + 0.45f * tw
            frame.set(
                i,
                c * 0.9f,
                c * 0.95f,
                c,
                1f,
                if (tw > 0.85f) {
                    '+'
                } else if (tw > 0.5f) {
                    '·'
                } else {
                    '.'
                },
            )
        }
    }
}

/**
 * The northern lights over snowy mountains: three folding curtains of green light that turn violet as they rise, with
 * a pink fringe at the lower edge and shimmering vertical rays, above a ridge whose snow catches the glow and a row of
 * black pines. The seed shapes the curtains, the stars and the mountains. Every motion repeats a whole number of times
 * per loop.
 */
public fun auroraRibbonsPiece(seed: Int = 0): AsciiPiece {
    val scene = Aurora(seed)
    return asciiPiece(
        name = "aurora ribbons",
        ground = Rgb.fromHex("#02040b"),
        category = "effects",
        cols = KCOLS,
        rows = KROWS,
        fps = 20,
        still = 5f,
        loop = LOOP,
        note = "folding green and violet curtains of the northern lights over snowy mountains and pines",
    ) { time, grid -> scene.draw(kLoopTime(time, LOOP), grid) }
}

/**
 * The northern lights: green and violet curtains folding and shimmering over snowy mountains under the stars.
 *
 * @param seed shapes the curtains, the stars and the mountains.
 */
@Composable
public fun AsciiAuroraRibbons(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Green and violet northern lights folding over snowy mountains and pines",
) {
    val piece = remember(seed) { auroraRibbonsPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
