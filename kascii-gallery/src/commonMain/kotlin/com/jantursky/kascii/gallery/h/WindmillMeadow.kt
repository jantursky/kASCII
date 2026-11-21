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
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 32f
private const val SUN_X = 146f
private const val SUN_Y = 21f
private const val SUN_R = 8f
private const val MILL_X = 54f
private const val MILL_BASE = 33
private const val MILL_TOP = 17
private const val HUB_Y = 16.2f
private const val SAIL = 21f
private const val BLADES = 60
private const val BIRDS = 4

private class Meadow(
    seed: Int,
) {
    private val lat = HLattice(seed, 7)
    private val cloudLat = HLattice(seed, 8)
    private val back = HFrame()
    private val fore = HFrame()
    private val frame = HFrame()
    private val palette: HPalette
    private val sky = listOf(hcol("#2c2660"), hcol("#5a3670"), hcol("#a8546a"), hcol("#e8804a"), hcol("#ffc86e"))
    private val sun = hcol("#fff2c0")
    private val sunGlow = hcol("#ffb050")
    private val farHill = hcol("#a05a6a")
    private val midHill = hcol("#7a6a3a")
    private val meadowLit = hcol("#d8b04a")
    private val meadowDark = hcol("#4a4a1c")
    private val tower = hcol("#f0dcc0")
    private val towerShade = hcol("#7a5a5a")
    private val wood = hcol("#4a2a1c")
    private val cloth = hcol("#f4dcb4")
    private val bladeX = FloatArray(BLADES)
    private val bladeH = FloatArray(BLADES)
    private val bladePhase = FloatArray(BLADES)
    private val hillTop = IntArray(HCOLS)

    init {
        buildBack()
        val rng = hrng(seed, 3)
        for (k in 0 until BLADES) {
            bladeX[k] = rng.float() * HCOLS
            bladeH[k] = 2f + rng.float() * 5f
            bladePhase[k] = rng.float()
        }
        buildFore(seed)
        palette =
            HPalette(
                hramp(14, *sky.toTypedArray()) +
                    hramp(4, sunGlow, hcol("#ffe090"), sun, hcol("#ffffff")) +
                    hramp(6, hcol("#5a3a5e"), farHill, hcol("#e09a8a")) +
                    hramp(6, hcol("#3a3420"), midHill, hcol("#c09a50")) +
                    hramp(8, hcol("#1a1e0a"), meadowDark, hcol("#8a8a30"), meadowLit, hcol("#ffe08a")) +
                    hramp(5, towerShade, hcol("#c8a090"), tower) +
                    hramp(4, hcol("#1a0e0a"), wood, hcol("#8a5a3a")) +
                    hramp(4, hcol("#9a7a7a"), cloth, hcol("#fff0d8")) +
                    hramp(6, hcol("#3a2a50"), hcol("#8a5a7a"), hcol("#e08a6a"), hcol("#ffd0a0")) +
                    listOf(hcol("#e04a3a"), hcol("#f8f0f0"), hcol("#ffd84a"), hcol("#8a6ad0"), hcol("#120a10")),
            )
    }

    private fun buildBack() {
        for (y in 0 until HROWS) {
            for (x in 0 until HCOLS) {
                val v = hclamp(y / 28f)
                val c = hgrad(v * v * 0.35f + v * 0.65f, sky)
                back.put(x, y, c, min(1.25f, 0.45f + 1.25f * v * v))
            }
        }
        // Far ridge, hazy in the low sun.
        for (x in 0 until HCOLS) {
            val top = 27.5f - 3.5f * lat.fbm(x * 0.025f, 2f, 3) - 1.2f * sin(x * 0.04f + 1f)
            for (y in top.toInt() until HROWS) {
                val k = hsmooth(top, top + 6f, y.toFloat())
                back.put(x, y, farHill.mix(hcol("#e09a8a"), 0.45f - 0.3f * k), hsolid(0.75f + 0.2f * k, x, y))
            }
        }
        // Rolling mid hills, the windmill hill on the left.
        for (x in 0 until HCOLS) {
            val xf = x + 0.5f
            val millHill = 33.5f - 3.6f * hgauss(xf, MILL_X, 26f)
            val wobble = 2f * (lat.fbm(xf * 0.04f, 7f, 2) - 0.5f)
            val roll = 33.5f + 1.6f * sin(xf * 0.05f + 2f) - 1.4f * hgauss(xf, 120f, 22f) + wobble
            val top = min(millHill, roll)
            hillTop[x] = top.toInt()
            for (y in top.toInt() until HROWS) {
                val d = y - top
                val stripe = 0.5f + 0.5f * sin(xf * 0.22f + d * 1.8f + lat.at(xf * 0.1f, y * 0.3f) * 3f)
                val lit = 0.35f + 0.35f * hsmooth(0f, 2f, d) * (1f - hsmooth(3f, 12f, d)) + 0.2f * stripe
                val c = hcol("#3a3420").mix(midHill.mix(hcol("#c09a50"), 0.5f), lit)
                back.put(x, y, c, hsolid(0.4f + 0.5f * stripe, x, y))
            }
        }
        // A hedge and two trees on the ridge to the right.
        for (cx in listOf(104f, 112f, 171f)) {
            val ty = hillTop[cx.toInt()] - 2.5f
            back.ellipse(cx, ty, 4.2f, 2.3f) { a, b ->
                hcol("#2a2a12").mix(hcol("#8a7a3a"), hclamp(0.3f + 0.5f * a - 0.3f * b))
            }
            back.put(cx.toInt(), (ty + 2f).toInt(), wood, 2f, '│')
        }
        // The windmill.
        for (y in MILL_TOP until MILL_BASE + 1) {
            val p = (y - MILL_TOP) / (MILL_BASE - MILL_TOP).toFloat()
            val half = 3f + 2.8f * p
            for (x in (MILL_X - half).toInt()..(MILL_X + half).toInt()) {
                val a = (x + 0.5f - MILL_X) / half
                val c = towerShade.mix(tower, hclamp(0.5f + 0.6f * a))
                back.put(x, y, c, 2f)
            }
        }
        for (y in 28..MILL_BASE) {
            back.put(MILL_X.toInt() + 1, y, wood, 2f)
            back.put(MILL_X.toInt() + 2, y, wood, 2f)
        }
        back.put(MILL_X.toInt() + 1, 22, hcol("#ffd84a"), 2f)
        back.put(MILL_X.toInt() - 1, 24, hcol("#120a10"), 2f)
        // The cap.
        for (y in (MILL_TOP - 3) until MILL_TOP) {
            val half = 2f + (y - (MILL_TOP - 3)) * 1.6f
            for (x in (MILL_X - half).toInt()..(MILL_X + half).toInt()) {
                val a = (x + 0.5f - MILL_X) / half
                back.put(x, y, wood.mix(hcol("#8a5a3a"), hclamp(0.4f + 0.6f * a)), 2f)
            }
        }
    }

    private fun buildFore(seed: Int) {
        val rng = hrng(seed, 4)
        for (x in 0 until HCOLS) {
            val top = 40f + 1.5f * sin(x * 0.03f) + 1.5f * (lat.fbm(x * 0.06f, 30f, 2) - 0.5f)
            for (y in top.toInt() until HROWS) {
                val k = hsmooth(top, top + 6f, y.toFloat())
                val n = lat.fbm(x * 0.09f, y * 0.4f + 50f, 2)
                fore.put(
                    x,
                    y,
                    meadowLit.mix(meadowDark, 0.25f + 0.5f * k + 0.3f * (n - 0.5f)),
                    hsolid(0.3f + 0.6f * n, x, y),
                )
            }
        }
        // Wildflowers.
        repeat(70) {
            val x = rng.int(HCOLS)
            val y = 41 + rng.int(7)
            val c = listOf(hcol("#e04a3a"), hcol("#f8f0f0"), hcol("#ffd84a"), hcol("#8a6ad0"))[rng.int(4)]
            fore.put(x, y, c, if (rng.float() < 0.5f) 2f else 1.8f)
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = HTAU * t / LOOP
        frame.copyFrom(back)
        sunDisc()
        clouds(t)
        sails(phi)
        birds(t)
        frame.over(fore)
        grass(t)
        frame.glow(SUN_X, SUN_Y, 34f, sunGlow, 0.32f)
        frame.glow(SUN_X, SUN_Y + 4f, 80f, hcol("#e8804a"), 0.12f)
        frame.glow(SUN_X, SUN_Y, 12f, hcol("#fff0c0"), 0.35f)
        frame.output(grid, palette)
    }

    private fun sunDisc() {
        for (y in (SUN_Y - 5).toInt()..(SUN_Y + 5).toInt()) {
            for (x in (SUN_X - SUN_R - 1).toInt()..(SUN_X + SUN_R + 1).toInt()) {
                val i = y * HCOLS + x
                if (back.cov[i] > 1.4f && y > 24) continue
                val dx = x + 0.5f - SUN_X
                val dy = (y + 0.5f - SUN_Y) * HROW_U
                val d2 = (dx * dx + dy * dy) / (SUN_R * SUN_R)
                if (d2 < 1f) frame.set(i, sun.mix(hcol("#ffd070"), hsmooth(0.4f, 1f, d2)), 2f)
            }
        }
    }

    private fun clouds(t: Float) {
        val drift = t / LOOP * 3f
        for (y in 2 until 21) {
            val band = hsmooth(2f, 6f, y.toFloat()) * (1f - hsmooth(15f, 21f, y.toFloat()))
            for (x in 0 until HCOLS) {
                val n = cloudLat.wrapFbm(x * 0.03f * 0.75f - drift, y * 0.16f, 4, 3)
                val streak = n + 0.4f * (cloudLat.fbm(x * 0.015f + 9f, y * 0.2f, 2) - 0.5f)
                val d = hsmooth(0.48f, 0.72f, streak) * band
                if (d < 0.05f) continue
                val under = hsmooth(0.5f, 0.66f, cloudLat.wrapFbm(x * 0.03f * 0.75f - drift, (y - 1.2f) * 0.16f, 4, 3))
                val warm = hgauss(x.toFloat(), SUN_X, 70f)
                val lit = hclamp(0.35f + 0.5f * (d - under * 0.6f) + 0.4f * warm + 0.02f * y)
                val c = hcol("#3a2a50").mix(hcol("#e08a6a"), lit).mix(hcol("#ffd0a0"), hclamp(lit - 0.6f) * warm)
                val i = y * HCOLS + x
                if (d > 0.9f) frame.set(i, c, hsolid(d - 0.4f, x, y)) else frame.haze(i, c, d)
            }
        }
    }

    private fun sails(phi: Float) {
        val turn = -phi * 3f
        val reach = SAIL + 2f
        for (y in max(0, (HUB_Y - reach / HROW_U).toInt())..min(HROWS - 1, (HUB_Y + reach / HROW_U).toInt() + 1)) {
            for (x in (MILL_X - reach).toInt()..(MILL_X + reach).toInt()) {
                val dx = x + 0.5f - MILL_X
                val dy = (y + 0.5f - HUB_Y) * HROW_U
                val d = sqrt(dx * dx + dy * dy)
                if (d > reach) continue
                for (k in 0 until 4) {
                    val a = turn + k * HTAU / 4f
                    val ux = cos(a)
                    val uy = sin(a)
                    val along = dx * ux + dy * uy
                    val across = -dx * uy + dy * ux
                    if (along < 0f || along > SAIL) continue
                    if (abs(across) < 0.9f) {
                        frame.put(x, y, wood, 2f)
                    } else if (along > 4.5f && across > 0.9f && across < 5.2f) {
                        val rib = hfract(along / 3.4f) < 0.2f || across > 4.6f
                        val k2 = hclamp(0.55f + 0.4f * sin(a + 0.6f))
                        if (rib) {
                            frame.put(x, y, wood.mix(hcol("#8a5a3a"), 0.5f), 2f)
                        } else {
                            frame.put(
                                x,
                                y,
                                hcol("#9a7a7a").mix(cloth, k2),
                                1.8f,
                            )
                        }
                    }
                }
            }
        }
        frame.ellipse(MILL_X, HUB_Y, 1.6f, 0.8f) { _, _ -> hcol("#1a0e0a") }
    }

    private fun birds(t: Float) {
        for (b in 0 until BIRDS) {
            val u = hfract(t / LOOP + b * 0.27f)
            val x = -10f + u * 215f
            val y = 9f + b * 2.2f + 1.5f * sin(HTAU * (t / LOOP * 2f + b * 0.3f))
            val flap = sin(HTAU * (t / LOOP * 40f + b * 0.21f)) > 0f
            val c = hcol("#2a1a2a")
            frame.put(x.toInt(), y.toInt(), c, 1f, if (flap) '\\' else '_')
            frame.put(x.toInt() + 1, y.toInt(), c, 1f, if (flap) '/' else '_')
        }
    }

    private fun grass(t: Float) {
        val phi = HTAU * t / LOOP
        // Waves of wind sheen rolling over the meadow.
        for (y in 34 until HROWS) {
            for (x in 0 until HCOLS) {
                val i = y * HCOLS + x
                if (frame.cov[i] < 1.4f || frame.ch[i] != 0.toChar()) continue
                val wave = sin(phi * 4f - x * 0.09f + y * 0.35f) + 0.5f * sin(phi * 2f - x * 0.04f - y * 0.2f)
                val k = hsmooth(0.6f, 1.5f, wave) * (0.14f + 0.02f * (y - 34))
                frame.r[i] += k * 0.9f
                frame.g[i] += k * 0.7f
                frame.b[i] += k * 0.25f
                if (k > 0.12f) frame.cov[i] = 2f
            }
        }
        // Tall dark blades in the very front, nodding in the gusts.
        for (k in 0 until BLADES) {
            val x0 = bladeX[k]
            val sway = sin(phi * 4f - x0 * 0.09f + bladePhase[k]) + 0.5f * sin(phi * 2f - x0 * 0.04f)
            val h = bladeH[k]
            val steps = (h * 2f).toInt()
            for (s in 0..steps) {
                val p = s.toFloat() / steps
                val x = x0 + (sway * 1.8f + 1f) * p * p
                val y = HROWS - 0.2f - h * p
                val c = hcol("#1a1e0a").mix(hcol("#5a5420"), 0.15f + 0.4f * p)
                frame.put(x.toInt(), y.toInt(), if (p > 0.8f) hcol("#8a7a30") else c, if (p > 0.8f) 1.8f else 2f)
            }
        }
    }
}

/**
 * A windmill on a meadow hill at golden hour: its sails turn slowly against a low sun, warm clouds drift across a dusky
 * sky, birds pass and the grass sways in waves of wind. The seed shapes the hills, the clouds and the flowers. Every
 * motion repeats a whole number of times per loop.
 */
public fun windmillMeadowPiece(seed: Int = 0): AsciiPiece {
    val scene = Meadow(seed)
    return asciiPiece(
        name = "windmill meadow",
        ground = Rgb.fromHex("#0c0812"),
        category = "scenes",
        cols = HCOLS,
        rows = HROWS,
        fps = 15,
        still = 5f,
        loop = LOOP,
        note = "a windmill turning on a meadow hill at golden hour, clouds drifting and grass swaying",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A windmill on a golden-hour meadow with turning sails, drifting clouds and swaying grass.
 *
 * @param seed shapes the hills, the clouds and the flowers.
 */
@Composable
public fun AsciiWindmillMeadow(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A windmill turning on a meadow hill at golden hour, clouds drifting, grass swaying",
) {
    val piece = remember(seed) { windmillMeadowPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
