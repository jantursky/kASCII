package com.jantursky.kascii.gallery.h

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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

private const val LOOP = 24f
private const val SEA = 35f
private const val SUN_X = 156f
private const val SUN_Y = 31f
private const val PUFF_ROWS = 4

/** One floating island: centre column, resting top row, half width, underside depth in rows and how hazy it is. */
private class Isle(
    val cx: Float,
    val top: Float,
    val rx: Float,
    val depth: Float,
    val haze: Float,
    val bobRows: Float,
    val bobK: Int,
    val bobP: Float,
    val salt: Float,
)

private class Islands(
    seed: Int,
) {
    private val lat = HLattice(seed, 21)
    private val cloudLat = HLattice(seed, 22)
    private val sky = HFrame()
    private val frame = HFrame()
    private val palette: HPalette
    private val grass = hcol("#5aa04a")
    private val grassLit = hcol("#c8e070")
    private val rock = hcol("#7a5a5a")
    private val rockDark = hcol("#2a1e34")
    private val rockLit = hcol("#d8a080")
    private val water = hcol("#9adcff")
    private val cloudLit = hcol("#ffe8e0")
    private val cloudShade = hcol("#7a6aa8")
    private val skyHaze = hcol("#b49ac8")
    private val isles =
        listOf(
            Isle(30f, 12f, 8f, 6f, 0.55f, 0.5f, 2, 0.1f, 3f),
            Isle(148f, 14f, 16f, 11f, 0.15f, 0.8f, 1, 0.55f, 7f),
            Isle(74f, 21f, 25f, 15f, 0f, 1f, 1, 0f, 11f),
        )

    private val puffP = FloatArray(PUFF_ROWS * 16)

    init {
        buildSky(seed)
        val prng = hrng(seed, 13)
        for (k in puffP.indices) puffP[k] = prng.float()
        palette =
            HPalette(
                hramp(
                    12,
                    hcol("#101a44"),
                    hcol("#2e3a78"),
                    hcol("#6a5a9a"),
                    hcol("#c88aa8"),
                    hcol("#ffc8a0"),
                    hcol("#fff0d0"),
                ) +
                    hramp(7, cloudShade.scale(0.6f), cloudShade, hcol("#c8a0c0"), cloudLit, hcol("#ffffff")) +
                    hramp(6, hcol("#1a3a20"), grass, grassLit, hcol("#f0ffb0")) +
                    hramp(8, rockDark, hcol("#4a3448"), rock, hcol("#a8806a"), rockLit) +
                    hramp(5, hcol("#4a8ac0"), water, hcol("#e8f8ff")) +
                    hramp(4, hcol("#3a2418"), hcol("#6a4a30"), hcol("#9a7a50")) +
                    listOf(hcol("#e8d8c0"), hcol("#c04a3a"), hcol("#ffe070"), hcol("#ffffff")),
            )
    }

    private fun buildSky(seed: Int) {
        for (y in 0 until HROWS) {
            for (x in 0 until HCOLS) {
                val v = hclamp(y / SEA)
                val c =
                    hgrad(
                        v,
                        listOf(hcol("#101a44"), hcol("#2e3a78"), hcol("#6a5a9a"), hcol("#c88aa8"), hcol("#ffc8a0")),
                    )
                sky.put(x, y, c, 0.35f + 0.7f * v * v)
            }
        }
        val rng = hrng(seed, 12)
        repeat(50) {
            val x = rng.int(HCOLS)
            val y = rng.int(12)
            sky.put(x, y, hcol("#e8e0ff"), 1f, if (rng.float() < 0.3f) '+' else '·')
        }
        sky.glow(SUN_X, SUN_Y, 60f, hcol("#ffb080"), 0.3f)
        sky.glow(SUN_X, SUN_Y, 14f, hcol("#fff0d0"), 0.6f)
        sky.ellipse(SUN_X, SUN_Y, 7f, 3.2f) { _, _ -> hcol("#fff8e8") }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = HTAU * t / LOOP
        frame.copyFrom(sky)
        farClouds(t)
        for (isle in isles) {
            val bob = isle.bobRows * sin(phi * isle.bobK + isle.bobP * HTAU)
            waterfall(isle, bob, t)
            island(isle, bob, phi)
        }
        cloudSea(phi)
        frame.glow(74f + 0.78f * 25f, SEA + 1f, 6f, hcol("#ffffff"), 0.35f)
        frame.glow(148f - 0.7f * 16f, SEA + 1f, 5f, hcol("#ffffff"), 0.3f)
        frame.output(grid, palette)
    }

    private fun farClouds(t: Float) {
        val drift = t / LOOP * 2f
        for (y in 4 until 30) {
            val band = hgauss(y.toFloat(), 18f, 7f)
            for (x in 0 until HCOLS) {
                val n = cloudLat.wrapFbm(x * 0.016f - drift, y * 0.28f + 50f, 3, 2)
                val d = hsmooth(0.56f, 0.72f, n) * band
                if (d > 0.05f) {
                    frame.haze(
                        y * HCOLS + x,
                        skyHaze.mix(hcol("#ffd0b0"), hclamp((x - 90f) / 100f)),
                        d * 0.6f,
                    )
                }
            }
        }
    }

    /** The row where the underside ends below the top, for a column `dx` from the centre in units of the half width. */
    private fun underside(
        isle: Isle,
        a: Float,
        x: Int,
    ): Float {
        val body = (1f - a * a).coerceAtLeast(0f).pow(0.8f)
        val jag = lat.at(x * 0.45f + isle.salt * 10f, isle.salt) * 0.35f + 0.65f
        val spikes = max(0f, sin(x * 0.9f + isle.salt) - 0.6f) * 2.2f
        return isle.depth * body * jag + spikes * body
    }

    private fun island(
        isle: Isle,
        bob: Float,
        phi: Float,
    ) {
        val top = isle.top + bob
        val y0 = max(0, (top - 2f).toInt())
        val y1 = min(HROWS - 1, (top + isle.depth + 3f).toInt())
        for (x in max(0, (isle.cx - isle.rx - 1).toInt())..min(HCOLS - 1, (isle.cx + isle.rx + 1).toInt())) {
            val a = (x + 0.5f - isle.cx) / isle.rx
            if (abs(a) > 1f) continue
            val surf = top + 0.6f * (lat.at(x * 0.2f + isle.salt, 5f) - 0.5f) + 0.5f * a * a
            val bottom = top + underside(isle, a, x)
            for (y in y0..y1) {
                val upper = surf - y
                val lower = bottom - y
                if (upper >= 1f || lower <= 0f) continue
                val ly = y + 0.5f - surf
                val half =
                    if (upper > 0.3f) {
                        '▄'
                    } else if (lower < 0.6f) {
                        '▀'
                    } else {
                        0.toChar()
                    }
                val sun = hclamp(0.5f + 0.5f * a)
                var c: HCol
                if (ly < 1.3f) {
                    c = grass.mix(grassLit, 0.3f + 0.6f * sun - 0.2f * ly)
                } else {
                    val p = (y - surf) / max(1f, bottom - surf)
                    val strata = 0.5f + 0.5f * sin((y - top) * 1.7f + lat.at(x * 0.1f, y * 0.3f) * 3f)
                    c =
                        rockDark
                            .mix(
                                rock,
                                hclamp(0.8f - 0.7f * p + 0.15f * strata),
                            ).mix(rockLit, hclamp(sun - 0.55f) * (1f - p) * 1.6f)
                }
                c = c.mix(skyHaze, isle.haze)
                frame.put(x, y, c, 2f, half)
            }
            // Roots dangling from the underside.
            if ((x * 7 + isle.salt.toInt()) % 9 == 0 && isle.haze < 0.3f) {
                val len = 2 + ((x * 13) % 4)
                for (k in 0 until len) {
                    frame.put(
                        x,
                        (bottom + k).toInt(),
                        hcol("#3a2418").mix(skyHaze, 0.3f + 0.15f * k),
                        1f,
                        if (k == len - 1) '\'' else '│',
                    )
                }
            }
        }
        // A tree on the big island and a small stone hut on the middle one.
        if (isle.rx > 20f) tree(isle.cx - 8f, top, phi)
        if (isle.rx in 12f..20f) hut(isle.cx + 2f, top)
    }

    private fun tree(
        x: Float,
        top: Float,
        phi: Float,
    ) {
        val sway = 0.6f * sin(phi * 2f)
        for (k in 0 until 4) frame.put(x.toInt(), (top - 1f - k).toInt(), hcol("#3a2418"), 2f)
        frame.ellipse(x + sway, top - 6.2f, 7f, 2.8f) { a, b ->
            hcol("#1a3a20").mix(grassLit, hclamp(0.35f + 0.45f * a - 0.35f * b + 0.2f * lat.at(a * 5f + 30f, b * 5f)))
        }
        frame.ellipse(x - 3f + sway * 0.7f, top - 4.6f, 4.5f, 1.8f) { a, b ->
            hcol("#1a3a20").mix(grass, hclamp(0.5f + 0.3f * a - 0.3f * b))
        }
    }

    private fun hut(
        x: Float,
        top: Float,
    ) {
        val base = (top - 0.2f).toInt()
        for (y in base - 3..base) {
            for (xx in (x - 3).toInt()..(x + 3).toInt()) {
                frame.put(
                    xx,
                    y,
                    hcol("#e8d8c0").mix(hcol("#7a5a5a"), if (xx < x) 0.35f else 0f),
                    2f,
                )
            }
        }
        for (k in 0..4) {
            val y = base - 4 - k / 2
            for (xx in (x - 4 + k).toInt()..(x + 4 - k).toInt()) {
                frame.put(
                    xx,
                    y,
                    hcol("#c04a3a").mix(hcol("#5a2418"), if (xx < x) 0.3f else 0f),
                    2f,
                    if (k % 2 == 1) '▄' else 0.toChar(),
                )
            }
        }
        frame.put(x.toInt(), base - 1, hcol("#ffe070"), 2f)
        frame.put(x.toInt(), base, hcol("#3a2418"), 2f)
    }

    private fun waterfall(
        isle: Isle,
        bob: Float,
        t: Float,
    ) {
        if (isle.haze > 0.4f) return
        val side = if (isle.rx > 20f) 0.78f else -0.7f
        val x0 = isle.cx + side * isle.rx
        val yTop = isle.top + bob + 0.5f
        for (y in yTop.toInt() until HROWS) {
            val fall = y - yTop
            val spread = 1.2f + fall * 0.06f
            for (dx in -2..2) {
                val x = (x0 + dx * spread * 0.6f + 0.4f * fall * 0.04f).toInt()
                val w = 1f - abs(dx) / 2.6f
                val streak = hfract((y - yTop) * 0.18f - t / LOOP * 18f + dx * 0.37f + lat.at(dx * 3f + 1f, 0f))
                val k = w * (0.55f + 0.45f * streak)
                val c = hcol("#4a8ac0").mix(water, k).mix(hcol("#e8f8ff"), hclamp(streak - 0.7f) * 2f)
                if (abs(dx) == 2) frame.put(x, y, c, 1f, if (streak > 0.5f) '│' else ':') else frame.put(x, y, c, 2f)
            }
        }
    }

    private fun cloudSea(phi: Float) {
        // Rows of round billows, back to front, each one shaded like a ball lit from the low sun on the right.
        for (row in 0 until PUFF_ROWS) {
            val n = puffCount(row)
            val cy0 = SEA + row * 3.6f
            val r = 8f + row * 3.5f
            for (k in 0 until n) {
                val p = puffP[row * 16 + k]
                val cx = (k + 0.5f * (row % 2)) * (HCOLS + 20f) / n - 10f + 3f * sin(phi * (1 + row % 2) + p * HTAU)
                val rr = r * (0.8f + 0.4f * p) * (1f + 0.04f * sin(phi * 2f + p * 9f))
                val ry = rr / HROW_U * 0.95f
                val cy = cy0 + (p - 0.5f) * 3f
                frame.ellipse(cx, cy, rr, ry) { a, b ->
                    val nz = kotlin.math.sqrt(hclamp(1f - a * a - b * b))
                    val lit = hclamp(0.25f + 0.45f * a * 0.6f - 0.55f * b + 0.45f * nz - row * 0.06f)
                    val warm = hgauss(cx, SUN_X, 46f) * hclamp(0.6f - b)
                    cloudShade.mix(cloudLit, lit).mix(hcol("#ffd0a0"), warm * 0.55f)
                }
            }
        }
        for (y in (SEA + 6f).toInt() until HROWS) {
            for (x in 0 until HCOLS) {
                val i = y * HCOLS + x
                if (frame.cov[i] < 1.4f) frame.set(i, cloudShade.scale(0.8f), 2f)
            }
        }
    }

    private fun puffCount(row: Int): Int = 15 - row * 3
}

/**
 * Floating islands at dawn: rocky islands bob slowly above a sea of clouds, waterfalls pour off their edges into the
 * mist, roots dangle from their undersides and a tree and a red-roofed hut sit on top. The seed shapes the clouds and
 * the stars. Every motion repeats a whole number of times per loop.
 */
public fun floatingIslandsPiece(seed: Int = 0): AsciiPiece {
    val scene = Islands(seed)
    return asciiPiece(
        name = "floating islands",
        ground = Rgb.fromHex("#080a1c"),
        category = "scenes",
        cols = HCOLS,
        rows = HROWS,
        fps = 15,
        still = 6f,
        loop = LOOP,
        note = "islands floating above a sea of clouds at dawn, waterfalls spilling off their edges",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * Islands bobbing above a sea of clouds at dawn with waterfalls pouring off their edges.
 *
 * @param seed shapes the clouds and the stars.
 */
@Composable
public fun AsciiFloatingIslands(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Rocky islands floating above a sea of clouds at dawn, waterfalls spilling off them",
) {
    val piece = remember(seed) { floatingIslandsPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
