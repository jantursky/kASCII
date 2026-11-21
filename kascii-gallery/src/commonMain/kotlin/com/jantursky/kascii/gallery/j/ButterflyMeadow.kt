package com.jantursky.kascii.gallery.j

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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 12f
private const val SUN_X = 52f
private const val SUN_Y = 17f
private const val SUN_R = 8f
private const val HORIZON = 27
private const val STEMS = 26
private const val MOTES = 60
private const val GRASS_FAR = ".,'`.\"',"
private const val GRASS_MID = "v|/\\'|v\",|"
private const val GRASS_NEAR = "|/\\WwV|/|\\vY"

private class ButterflyMeadow(
    seed: Int,
) {
    private val lat = JLattice(seed, 21)
    private val back = JFrame()
    private val frame = JFrame()
    private val palette: JPalette
    private val stemX = FloatArray(STEMS)
    private val stemTop = FloatArray(STEMS)
    private val stemKind = IntArray(STEMS)
    private val stemPhase = FloatArray(STEMS)
    private val moteX = FloatArray(MOTES)
    private val moteY = FloatArray(MOTES)
    private val motePhase = FloatArray(MOTES)
    private val grass = jcol("#1c3a1c")
    private val grassLit = jcol("#9ab040")
    private val gold = jcol("#ffc860")
    private val petals =
        listOf(jcol("#f4f0e8"), jcol("#ff6a8a"), jcol("#ffd040"), jcol("#b08aff"), jcol("#ff8a3a"))
    private val wingSets =
        listOf(
            listOf(jcol("#ff8a1a"), jcol("#1a0e08"), jcol("#fff4e0")),
            listOf(jcol("#4ab0ff"), jcol("#0c1a3a"), jcol("#e0f4ff")),
            listOf(jcol("#ffe050"), jcol("#3a2a08"), jcol("#ffffff")),
        )

    init {
        buildBack()
        val rng = AKit.rng(seed, 61)
        for (k in 0 until STEMS) {
            stemX[k] = (k + 0.2f + rng.float() * 0.6f) * JCOLS / STEMS
            stemTop[k] = 25f + rng.float() * 13f
            stemKind[k] = rng.int(petals.size)
            stemPhase[k] = rng.float()
        }
        for (k in 0 until MOTES) {
            moteX[k] = rng.float()
            moteY[k] = rng.float()
            motePhase[k] = rng.float()
        }
        palette = JPalette(buildPalette())
    }

    private fun buildPalette(): List<JCol> {
        val list = ArrayList<JCol>()
        list +=
            jramp(
                10,
                jcol("#120c2a"),
                jcol("#2a1a4a"),
                jcol("#6a2e5a"),
                jcol("#c4504a"),
                jcol("#f09a50"),
                jcol("#ffd890"),
            )
        list += jramp(6, jcol("#1a1230"), jcol("#3a2650"), jcol("#6a3e68"))
        list += jramp(8, jcol("#08140c"), grass, jcol("#3a5a22"), grassLit, jcol("#e0d070"))
        list += jramp(4, jcol("#ffb040"), gold, jcol("#fff0c0"), jcol("#ffffff"))
        for (p in petals) list += jramp(3, p.scale(0.45f), p, p.mix(jcol("#ffffff"), 0.4f))
        for (w in wingSets) list += jramp(3, w[0].scale(0.5f), w[0], w[0].mix(jcol("#ffffff"), 0.35f)) + w[1] + w[2]
        list += listOf(jcol("#2a1a10"), jcol("#6a4a2a"))
        return list
    }

    private fun hill(
        x: Int,
        base: Float,
        amp: Float,
        f: Float,
        salt: Float,
    ): Float = base - amp * lat.fbm(x * f + salt, salt, 3)

    private fun buildBack() {
        for (y in 0 until JROWS) {
            for (x in 0 until JCOLS) {
                val g = y / HORIZON.toFloat()
                val dx = x + 0.5f - SUN_X
                val dy = (y + 0.5f - SUN_Y) * JROW_U
                val d = sqrt(dx * dx + dy * dy)
                val warm = jsmooth(0.2f, 1f, g)
                var c = jcol("#1e1648").mix(jcol("#5a2a5a"), jsmooth(0f, 0.6f, g)).mix(jcol("#e0784a"), warm * warm)
                c = c.mix(jcol("#ffd890"), 0.7f * jgauss(d, 0f, 40f) * jsmooth(0f, 0.8f, g))
                val n = lat.fbm(x * 0.03f + 9f, y * 0.35f, 3)
                // Long thin clouds catching the light.
                val cloud = jsmooth(0.56f, 0.7f, n) * jsmooth(4f, 10f, y.toFloat()) * jsmooth(22f, 15f, y.toFloat())
                c = c.mix(jcol("#ff9a6a").mix(jcol("#ffd8a0"), jgauss(d, 0f, 50f)), cloud * 0.8f)
                val cv = 0.28f + 0.7f * g + cloud * 0.6f + (n - 0.5f) * 0.2f
                back.put(x, y, c, if (cloud > 0.55f) 1.8f else jclamp(cv))
            }
        }
        // The sun, with a soft corona.
        for (y in 0 until JROWS) {
            for (x in 0 until JCOLS) {
                val dx = x + 0.5f - SUN_X
                val dy = (y + 0.5f - SUN_Y) * JROW_U
                val d = sqrt(dx * dx + dy * dy)
                if (d < SUN_R) back.put(x, y, jcol("#fff4c8").mix(jcol("#ffd070"), d / SUN_R * 0.6f), 2f)
            }
        }
        back.glow(SUN_X, SUN_Y, 14f, jcol("#ffb050"), 0.5f)
        // Far hills, hazy violet, then a nearer rolling ridge.
        for (x in 0 until JCOLS) {
            val far = hill(x, 25f, 6f, 0.02f, 3f)
            val near = hill(x, 29f, 4f, 0.035f, 11f)
            for (y in far.toInt() until JROWS) {
                val haze = jgauss(x.toFloat(), SUN_X, 40f) * 0.35f
                back.put(
                    x,
                    y,
                    jcol("#3a2650").mix(jcol("#a0506a"), haze).mix(
                        jcol("#4a2e58"),
                        jclamp((y - far) / 6f) * 0.3f,
                    ),
                    2f,
                )
            }
            for (y in near.toInt() until JROWS) {
                val dn = y - near
                val rim = jsmooth(1.2f, 0f, dn) * (0.5f + 0.5f * jgauss(x.toFloat(), SUN_X, 50f))
                val n = lat.fbm(x * 0.12f, y * 0.6f, 2)
                val sunny = 0.35f + 0.65f * jgauss(x.toFloat(), SUN_X, 70f)
                if (dn < 3f) {
                    val c = grass.mix(jcol("#3a5a22"), jclamp(n * 0.8f)).mix(grassLit, rim * 0.8f)
                    back.put(x, y, c, if (rim > 0.3f || n > 0.42f) 2f else 1.8f)
                } else {
                    // Grass as strokes: brighter and finer far off, darker and longer up close.
                    val h = lat.at(x * 1.7f + 3f, y * 2.3f)
                    val depth = jclamp((y - near) / (JROWS - near))
                    val set =
                        if (depth < 0.2f) {
                            GRASS_FAR
                        } else if (depth < 0.55f) {
                            GRASS_MID
                        } else {
                            GRASS_NEAR
                        }
                    val g = set[((h * 977f).toInt() and 0x7fffffff) % set.length]
                    val c =
                        jcol(
                            "#5a9a3a",
                        ).mix(jcol("#d8d860"), jclamp(n * 1.3f - 0.25f) * sunny).scale(1.1f - 0.35f * depth)
                    back.put(x, y, c, 1f, g)
                }
            }
        }
        // Far flowers dotted over the meadow.
        val rng = AKit.rng(9, 2)
        repeat(170) {
            val x = rng.int(JCOLS)
            val y = rng.jint(30, 42)
            val p = petals[rng.int(petals.size)]
            val depth = (y - 29f) / 13f
            back.put(x, y, p.scale(0.5f + 0.45f * depth), 2f)
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        frame.copyFrom(back)
        val phi = JTAU * t / LOOP
        stems(phi)
        motes(t)
        butterfly(t, 1, 150f, 22f, 26f, 6f, 2, 3, 1.7f, 0.85f)
        butterfly(t, 2, 44f, 30f, 24f, 5f, 1, 2, 3.9f, 0.75f)
        butterfly(t, 0, 100f, 18f, 40f, 7f, 1, 2, 0.1f, 1.45f)
        foreground(phi)
        frame.output(grid, palette)
    }

    /** A dark fringe of foreground grass along the bottom, its tips swaying and catching the sun. */
    private fun foreground(phi: Float) {
        for (x in 0 until JCOLS) {
            val h0 = lat.at(x * 0.9f, 5f)
            val h = 1f + h0 * h0 * 6f + 1.5f * lat.at(x * 0.05f, 9f)
            val lean = 0.8f * sin(phi * 2f + x * 0.09f) + (h0 - 0.5f) * 0.8f
            val top = JROWS - h
            for (y in top.toInt() until JROWS) {
                val p = (JROWS - y) / h
                val xx = (x + lean * p * p * 2f).toInt()
                if (y == top.toInt()) {
                    val lit = 0.4f + 0.6f * jgauss(x.toFloat(), SUN_X, 70f)
                    val g =
                        if (abs(lean) < 0.35f) {
                            '|'
                        } else if (lean > 0f) {
                            '/'
                        } else {
                            '\\'
                        }
                    frame.put(xx, y, jcol("#3a5a22").mix(grassLit, lit), 1f, g)
                } else {
                    frame.put(xx, y, jcol("#060e08").mix(jcol("#10200e"), jclamp(1.3f - p)), 2f)
                }
            }
        }
    }

    private fun stems(phi: Float) {
        for (k in 0 until STEMS) {
            val bx = stemX[k]
            val top = stemTop[k]
            val sway = 2.4f * sin(phi * 2f + stemPhase[k] * JTAU) + 0.8f * sin(phi * 3f + k)
            val len = JROWS - top
            var hx = bx
            for (j in 0 until len.toInt()) {
                val y = JROWS - 1 - j
                val p = j / len
                val x = bx + sway * p * p
                val dxp = sway * 2f * p / len
                hx = x
                frame.put(x.toInt(), y, jcol("#5a8a32").mix(jcol("#a8c050"), p * 0.6f), 1f, slopeGlyph(dxp, -1f))
            }
            flower(hx, top, stemKind[k], (top - 24f) / 14f)
        }
    }

    /** A flower head at ([x], [y]) of kind [kind], [far] 0 for the tallest (nearest-looking) to 1. */
    private fun flower(
        x: Float,
        y: Float,
        kind: Int,
        far: Float,
    ) {
        val p = petals[kind]
        val size = 1f - far * 0.4f
        val rx = 3.6f * size
        val ry = 1.5f * size
        for (yy in (y - ry - 1f).toInt()..(y + ry + 1f).toInt()) {
            for (xx in (x - rx - 1f).toInt()..(x + rx + 1f).toInt()) {
                val a = (xx + 0.5f - x) / rx
                val b = (yy + 0.5f - y) / ry
                val r = sqrt(a * a + b * b)
                if (r > 1f) continue
                val ang = atan2(b, a)
                val petal = 0.62f + 0.38f * abs(cos(ang * 2.5f))
                if (r > petal) continue
                if (r < 0.32f) {
                    frame.put(xx, yy, if (kind == 2) jcol("#6a4a2a") else gold, 2f)
                } else {
                    val lit = jclamp(0.75f + 0.35f * a * (if (x > SUN_X) -1f else 1f) - 0.3f * b)
                    frame.put(xx, yy, p.scale(0.55f + 0.45f * lit), 2f)
                }
            }
        }
    }

    private fun motes(t: Float) {
        for (k in 0 until MOTES) {
            val x = jfract(moteX[k] + t / LOOP) * JCOLS + 3f * sin(JTAU * (2f * t / LOOP + motePhase[k]))
            val y = 6f + moteY[k] * 34f + 1.5f * sin(JTAU * (3f * t / LOOP + motePhase[k] * 2f))
            val tw = 0.5f + 0.5f * sin(JTAU * (4f * t / LOOP + motePhase[k]))
            if (tw < 0.3f) continue
            val xi = x.toInt()
            val yi = y.toInt()
            if (xi !in 0 until JCOLS) continue
            val i = yi * JCOLS + xi
            if (frame.cov[i] > 1.4f) continue
            frame.put(xi, yi, gold.mix(jcol("#ffffff"), tw * 0.5f), 1f, if (tw > 0.85f) '*' else '·')
        }
    }

    /**
     * A butterfly seen from above on a Lissajous path round ([cx], [cy]) with half widths [ax], [ay] and integer
     * frequencies [fx], [fy] per loop; [scale] 1 is a wingspan of about 22 cells.
     */
    private fun butterfly(
        t: Float,
        kind: Int,
        cx: Float,
        cy: Float,
        ax: Float,
        ay: Float,
        fx: Int,
        fy: Int,
        phase: Float,
        scale: Float,
    ) {
        val phi = JTAU * t / LOOP
        val bx = cx + ax * sin(fx * phi + phase)
        val by = cy + ay * sin(fy * phi + phase * 1.7f)
        val vx = ax * fx * cos(fx * phi + phase)
        val tilt = jclamp(0.5f + vx / (ax * fx * 2f + 1f)) * 0.5f - 0.25f
        val flapRate = if (kind == 0) 26f else 34f
        val f = sin(JTAU * flapRate * t / LOOP + phase)
        val open = 0.3f + 0.7f * sqrt(0.5f + 0.5f * f)
        val (base, edge, spot) = wingSets[kind]
        val span = 7f * scale
        for (side in intArrayOf(-1, 1)) {
            // Forewing then hindwing, each an ellipse stretched sideways by how open the wings are.
            for (w in 0..1) {
                val rx = (if (w == 0) span else span * 0.72f) * open
                val ry = (if (w == 0) 2.6f else 2f) * scale
                val ox = side * rx * 0.92f
                val oy = (if (w == 0) -1.6f else 1.3f) * scale + side * tilt * rx / JROW_U * 0.4f
                val ex = bx + ox
                val ey = by + oy
                for (yy in (ey - ry - 1f).toInt()..(ey + ry + 1f).toInt()) {
                    for (xx in (ex - rx - 1f).toInt()..(ex + rx + 1f).toInt()) {
                        val a = (xx + 0.5f - ex) / max(rx, 0.6f)
                        val reach = jclamp(a * side)
                        val taper = if (w == 0) 1f - 0.35f * reach else 1f
                        val b = (yy + 0.5f - ey) / (ry * taper) + (if (w == 0) 0.5f else -0.2f) * reach
                        val r2 = a * a + b * b
                        if (r2 > 1f) continue
                        val outer = a * side
                        val rim = r2 > 0.62f && outer > -0.3f
                        val vein = abs(sin(atan2(b, outer + 1.1f) * 5f)) < 0.22f && r2 > 0.2f
                        var c =
                            if (rim) {
                                if (jfract((b + 1f) * 2.2f + w * 0.5f) < 0.3f && r2 > 0.75f) spot else edge
                            } else if (vein && kind == 0) {
                                edge
                            } else {
                                base.mix(base.scale(0.55f), jclamp(r2 * 0.6f + (if (w == 1) 0.2f else 0f)))
                            }
                        // The underside flashes when the wing is nearly shut.
                        frame.put(xx, yy, c, 2f)
                    }
                }
            }
        }
        // Body and antennae.
        for (k in -2..2) frame.put(bx.toInt(), (by + k * 0.9f * scale).toInt(), jcol("#2a1a10"), 2f)
        val head = (by - 2.6f * scale).toInt()
        frame.put(bx.toInt() - 1, head - 1, jcol("#2a1a10"), 1f, '\\')
        frame.put(bx.toInt() + 1, head - 1, jcol("#2a1a10"), 1f, '/')
        if (scale > 0.8f) {
            frame.put(bx.toInt() - 2, head - 2, jcol("#2a1a10"), 1f, '•')
            frame.put(bx.toInt() + 2, head - 2, jcol("#2a1a10"), 1f, '•')
        }
    }
}

/**
 * Three butterflies, a monarch, a blue and a yellow, flap over a meadow at golden hour, tracing slow loops between
 * swaying flowers while the low sun glows through the grass and pollen motes drift. The seed places the flowers, the
 * stems and the motes. Every motion repeats a whole number of times per loop.
 */
public fun butterflyMeadowPiece(seed: Int = 0): AsciiPiece {
    val scene = ButterflyMeadow(seed)
    return asciiPiece(
        name = "butterfly meadow",
        ground = Rgb.fromHex("#0c0a16"),
        category = "creatures",
        cols = JCOLS,
        rows = JROWS,
        fps = 15,
        still = 3f,
        loop = LOOP,
        note = "butterflies flap in loops over a meadow of swaying flowers as a low sun glows through the grass",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * Butterflies flapping in slow loops over a golden-hour meadow of swaying flowers.
 *
 * @param seed places the flowers, the stems and the drifting pollen.
 */
@Composable
public fun AsciiButterflyMeadow(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Butterflies flapping over a meadow of swaying flowers at golden hour",
) {
    val piece = remember(seed) { butterflyMeadowPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
