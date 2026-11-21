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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 12f
private const val SHORE = 27
private const val BANK = 41
private const val MOON_X = 46f
private const val MOON_Y = 8f
private const val MOON_R = 7f
private const val FLIES = 70
private const val REEDS = 42
private const val STARS = 90

private class FireflyMarsh(
    seed: Int,
) {
    private val lat = JLattice(seed, 37)
    private val sky = JFrame()
    private val bank = JFrame()
    private val frame = JFrame()
    private val palette: JPalette
    private val flyX = FloatArray(FLIES)
    private val flyY = FloatArray(FLIES)
    private val flyAx = FloatArray(FLIES)
    private val flyAy = FloatArray(FLIES)
    private val flyFx = IntArray(FLIES)
    private val flyFy = IntArray(FLIES)
    private val flyBlink = IntArray(FLIES)
    private val flyPhase = FloatArray(FLIES)
    private val reedX = FloatArray(REEDS)
    private val reedBase = FloatArray(REEDS)
    private val reedTop = FloatArray(REEDS)
    private val reedPhase = FloatArray(REEDS)
    private val reedCattail = BooleanArray(REEDS)
    private val starX = IntArray(STARS)
    private val starY = IntArray(STARS)
    private val starPhase = FloatArray(STARS)
    private val glow = jcol("#c8ff5a")
    private val reed = jcol("#3c5a40")
    private val reedLit = jcol("#9ab8b0")

    init {
        buildSky()
        buildBank()
        val rng = AKit.rng(seed, 43)
        for (k in 0 until FLIES) {
            flyX[k] = 8f + rng.float() * 176f
            flyY[k] = 12f + rng.float() * 28f
            flyAx[k] = 4f + rng.float() * 10f
            flyAy[k] = 1f + rng.float() * 3f
            flyFx[k] = 1 + rng.int(2)
            flyFy[k] = 1 + rng.int(3)
            flyBlink[k] = 3 + rng.int(4)
            flyPhase[k] = rng.float()
        }
        for (k in 0 until REEDS) {
            val front = k % 3 == 0
            // Front reeds stand in clumps at both sides, leaving the middle of the water open.
            reedX[k] =
                if (front) (if (k % 2 == 0) rng.float() * 46f else 150f + rng.float() * 42f) else rng.float() * JCOLS
            reedBase[k] = if (front) JROWS.toFloat() else SHORE + 1f + rng.float() * 6f
            reedTop[k] = if (front) 14f + rng.float() * 18f else SHORE - 6f - rng.float() * 6f
            reedPhase[k] = rng.float()
            reedCattail[k] = rng.float() < 0.55f
        }
        for (s in 0 until STARS) {
            starX[s] = rng.int(JCOLS)
            starY[s] = rng.int(SHORE - 6)
            starPhase[s] = rng.float()
        }
        palette = JPalette(buildPalette())
    }

    private fun buildPalette(): List<JCol> =
        jramp(10, jcol("#03060e"), jcol("#08122a"), jcol("#14284a"), jcol("#2a4a6a"), jcol("#6a8aa8")) +
            jramp(5, jcol("#020604"), jcol("#08140e"), jcol("#14261c"), jcol("#22382a")) +
            jramp(5, jcol("#1a2a22"), reed, jcol("#6a8a80"), reedLit) +
            jramp(4, jcol("#2a1a10"), jcol("#5a3a20"), jcol("#8a6038")) +
            jramp(5, jcol("#2a4a10"), jcol("#6aa020"), glow, jcol("#f4ffc0"), jcol("#ffffff")) +
            jramp(5, jcol("#8a94a8"), jcol("#d8e0ec"), jcol("#fffbe8")) +
            listOf(jcol("#3a5a7a"), jcol("#a8c0d8"))

    private fun treeTop(x: Int): Float =
        SHORE - 2f - 4.5f * lat.fbm(x * 0.06f + 10f, 3f, 3) - 2.5f * jgauss(x.toFloat(), 150f, 18f) -
            1.5f * jgauss(x.toFloat(), 96f, 10f)

    private fun buildSky() {
        for (y in 0 until SHORE) {
            val g = y / SHORE.toFloat()
            for (x in 0 until JCOLS) {
                val dx = x + 0.5f - MOON_X
                val dy = (y + 0.5f - MOON_Y) * JROW_U
                val d = sqrt(dx * dx + dy * dy)
                var c = jcol("#050a1c").mix(jcol("#3a6088"), g * g * g)
                val halo = jgauss(d, MOON_R, 22f)
                c = c.mix(jcol("#6a8aa8"), halo * 0.55f)
                val n = lat.fbm(x * 0.04f, y * 0.3f, 3)
                val cv = 0.1f + 0.75f * g * g * g + halo * 0.55f + (n - 0.5f) * 0.25f
                if (d < MOON_R) {
                    val crater = lat.fbm(x * 0.4f + 70f, y * 0.9f, 2)
                    sky.put(x, y, jcol("#fffbe8").mix(jcol("#b8bcc4"), jsmooth(0.45f, 0.75f, crater) * 0.6f), 2f)
                } else {
                    sky.put(x, y, c, jclamp(cv))
                }
            }
        }
        // Treeline on the far shore.
        for (x in 0 until JCOLS) {
            val top = treeTop(x)
            for (y in top.toInt() until SHORE) {
                val rim = jsmooth(1.2f, 0f, y - top) * jgauss(x.toFloat(), MOON_X, 60f)
                sky.put(
                    x,
                    y,
                    jcol("#010302").mix(jcol("#030805"), jclamp((y - top) / 4f)).mix(
                        jcol("#3a5060"),
                        rim * 0.6f,
                    ),
                    2f,
                )
            }
        }
    }

    private fun buildBank() {
        for (x in 0 until JCOLS) {
            val top = BANK + 1.5f * sin(x * 0.05f + 2f) + 1.2f * lat.fbm(x * 0.2f, 40f, 2)
            for (y in top.toInt() until JROWS) {
                val n = lat.at(x * 0.6f, y * 1.2f)
                bank.put(
                    x,
                    y,
                    jcol("#020604").mix(jcol("#0e2016"), jclamp(n * 0.7f + jsmooth(1.5f, 0f, y - top) * 0.6f)),
                    2f,
                )
            }
            // Tufts of grass on the bank edge.
            val h = lat.at(x * 1.1f, 77f)
            if (h > 0.55f) {
                val ty = top.toInt() - 1
                bank.put(
                    x,
                    ty,
                    jcol("#1e3a26").mix(reedLit, 0.25f),
                    1f,
                    if (h >
                        0.8f
                    ) {
                        '|'
                    } else if (x % 2 == 0) {
                        '/'
                    } else {
                        '\\'
                    },
                )
            }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        frame.copyFrom(sky)
        stars(t)
        reflect(t)
        reeds(t, far = true)
        frame.over(bank)
        reeds(t, far = false)
        flies(t)
        frame.output(grid, palette)
    }

    private fun stars(t: Float) {
        for (s in 0 until STARS) {
            val i = starY[s] * JCOLS + starX[s]
            if (frame.cov[i] > 1.4f) continue
            val tw = 0.5f + 0.5f * sin(JTAU * (2f * t / LOOP + starPhase[s]))
            if (tw < 0.3f) continue
            frame.put(starX[s], starY[s], jcol("#a8c0d8").scale(0.55f + 0.45f * tw), 1f, if (tw > 0.85f) '+' else '.')
        }
    }

    /** The water mirrors the sky, moon and treeline, broken up by slow horizontal ripples. */
    private fun reflect(t: Float) {
        val phi = JTAU * t / LOOP
        for (y in SHORE until JROWS) {
            val depth = (y - SHORE).toFloat()
            val src = SHORE - 1 - (depth * 1.6f).toInt()
            if (src < 0) continue
            for (x in 0 until JCOLS) {
                val wob = 1.4f * sin(y * 1.7f + phi * 3f + x * 0.05f) + 0.8f * sin(y * 0.9f - phi * 2f)
                val sx = min(JCOLS - 1, max(0, (x + wob * (0.4f + depth * 0.08f)).toInt()))
                val j = src * JCOLS + sx
                val i = y * JCOLS + x
                val ripple = lat.at(x * 0.08f + 3f * sin(phi), y * 1.1f + 3f * sin(phi + 1f))
                val k = 0.55f - depth * 0.012f
                frame.r[i] = sky.r[j] * k + 0.01f
                frame.g[i] = sky.g[j] * k + 0.02f
                frame.b[i] = sky.b[j] * k + 0.04f
                val cv = sky.cov[j]
                if (cv > 1.4f && sky.r[j] > 0.6f) {
                    frame.r[i] = sky.r[j] * 0.9f
                    frame.g[i] = sky.g[j] * 0.9f
                    frame.b[i] = sky.b[j] * 0.85f
                }
                var glyph = 0.toChar()
                var cov = if (cv > 1.4f) 1.6f else cv * 0.8f
                // Bright reflections break into dashes.
                if (cv > 1.4f && sky.r[j] > 0.6f) {
                    cov = if (ripple > 0.62f) 2f else 1f
                    glyph = if (ripple > 0.62f) 0.toChar() else '-'
                } else if (abs(x + 0.5f - MOON_X - wob) < 5f + depth * 0.35f && ripple > 0.5f) {
                    // The moon's glitter path across the water.
                    val k2 = jclamp((ripple - 0.5f) * 4f) * (1f - depth / 20f)
                    frame.r[i] += 0.8f * k2
                    frame.g[i] += 0.8f * k2
                    frame.b[i] += 0.72f * k2
                    glyph = if (k2 > 0.6f) '=' else '-'
                    cov = 1f
                } else if (ripple > 0.72f && cv < 1.4f) {
                    glyph = '~'
                    cov = 1f
                    frame.r[i] += 0.04f
                    frame.g[i] += 0.07f
                    frame.b[i] += 0.1f
                }
                frame.cov[i] = cov
                frame.ch[i] = glyph
            }
        }
    }

    private fun reeds(
        t: Float,
        far: Boolean,
    ) {
        val phi = JTAU * t / LOOP
        for (k in 0 until REEDS) {
            val isFar = reedBase[k] < JROWS - 0.5f
            if (isFar != far) continue
            val base = reedBase[k]
            val top = reedTop[k]
            val len = base - top
            val sway = (if (far) 0.8f else 2.2f) * sin(phi * 2f + reedPhase[k] * JTAU) + 0.6f * sin(phi * 3f + k)
            var tipX = reedX[k]
            for (j in 0 until len.toInt()) {
                val y = (base - 1 - j).toInt()
                val p = j / len
                val x = reedX[k] + sway * p * p
                val lit = jsmooth(0.4f, 1f, p) * (0.3f + 0.7f * jgauss(x, MOON_X, 70f))
                val c = (if (far) jcol("#1a2a22") else reed).mix(reedLit, lit * (if (far) 0.4f else 0.8f))
                if (far) {
                    frame.put(x.toInt(), y, c, 1f, slopeGlyph(sway * 2f * p / len, -1f))
                } else {
                    // Near stems are dark solid silhouettes with a moonlit edge.
                    frame.put(x.toInt(), y, jcol("#030805"), 2f)
                    frame.put(x.toInt() + 1, y, c, 1f, slopeGlyph(sway * 2f * p / len, -1f))
                }
                tipX = x
            }
            if (reedCattail[k]) {
                val cy = top + 2.5f
                val cx = reedX[k] + sway * ((base - cy) / len).pow(2f)
                for (q in 0 until (if (far) 2 else 4)) {
                    frame.put(
                        cx.toInt(),
                        (cy + q).toInt(),
                        jcol("#5a3a20").mix(
                            jcol("#8a6038"),
                            if (q ==
                                0
                            ) {
                                0.5f
                            } else {
                                0f
                            },
                        ),
                        2f,
                    )
                }
                frame.put(tipX.toInt(), top.toInt(), reed, 1f, '|')
            } else {
                // A long leaf bending away from the stem.
                for (q in 0 until 5) {
                    val y = top + len * 0.35f + q
                    val p = (base - y) / len
                    val x = reedX[k] + sway * p * p + (q + 1) * 0.9f * (if (k % 2 == 0) 1f else -1f)
                    frame.put(x.toInt(), y.toInt(), reed.mix(reedLit, 0.2f), 1f, if (k % 2 == 0) '\\' else '/')
                }
            }
        }
    }

    /** A soft glow round a firefly that tints the open air and only faintly the solid shapes behind it. */
    private fun halo(
        cx: Float,
        cy: Float,
        k: Float,
    ) {
        for (y in max(0, (cy - 6f).toInt())..min(JROWS - 1, (cy + 6f).toInt())) {
            for (x in max(0, (cx - 13f).toInt())..min(JCOLS - 1, (cx + 13f).toInt())) {
                val dx = (x + 0.5f - cx) / 6.5f
                val dy = (y + 0.5f - cy) * JROW_U / 6.5f
                val d2 = dx * dx + dy * dy
                if (d2 > 4f) continue
                val i = y * JCOLS + x
                frame.light(i, glow, k * kotlin.math.exp(-d2) * (if (frame.cov[i] > 1.4f) 0.25f else 1f))
            }
        }
    }

    private fun flies(t: Float) {
        val phi = JTAU * t / LOOP
        for (k in 0 until FLIES) {
            val x = flyX[k] + flyAx[k] * sin(flyFx[k] * phi + flyPhase[k] * JTAU)
            val y = flyY[k] + flyAy[k] * sin(flyFy[k] * phi + flyPhase[k] * 11f)
            val pulse = max(0f, sin(flyBlink[k] * phi + flyPhase[k] * 23f)).pow(3f)
            if (pulse < 0.04f) continue
            halo(x, y, 0.85f * pulse)
            frame.put(
                x.toInt(),
                y.toInt(),
                glow.mix(jcol("#ffffff"), pulse * 0.5f),
                1f,
                if (pulse >
                    0.6f
                ) {
                    '●'
                } else if (pulse > 0.25f) {
                    '•'
                } else {
                    '·'
                },
            )
            // The reflection in open water, below the shore.
            val ry = 2f * SHORE - y
            if (y < SHORE && ry < BANK && ry >= SHORE) {
                val i = ry.toInt() * JCOLS + x.toInt()
                if (x.toInt() in 0 until JCOLS && frame.cov[i] < 1.4f) {
                    frame.put(
                        x.toInt(),
                        ry.toInt(),
                        glow.scale(0.5f + 0.4f * pulse),
                        1f,
                        if (pulse >
                            0.5f
                        ) {
                            '•'
                        } else {
                            '·'
                        },
                    )
                }
            }
        }
    }
}

/**
 * Fireflies blink over a marsh at night: reeds and cattails sway in front of still water that mirrors the moon and a
 * dark treeline, ripples break the reflection into dashes and each firefly glows twice, once in the air and once in
 * the water. The seed places the fireflies, the reeds and the stars. Every motion repeats a whole number of times per
 * loop.
 */
public fun fireflyMarshPiece(seed: Int = 0): AsciiPiece {
    val scene = FireflyMarsh(seed)
    return asciiPiece(
        name = "firefly marsh",
        ground = Rgb.fromHex("#02040a"),
        category = "creatures",
        cols = JCOLS,
        rows = JROWS,
        fps = 15,
        still = 2f,
        loop = LOOP,
        note = "fireflies blink over a moonlit marsh, reeds swaying and the water mirroring the moon",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * Fireflies blinking over a moonlit marsh with swaying reeds and cattails and still water mirroring the moon.
 *
 * @param seed places the fireflies, the reeds and the stars.
 */
@Composable
public fun AsciiFireflyMarsh(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Fireflies blinking over a moonlit marsh with reeds and still water",
) {
    val piece = remember(seed) { fireflyMarshPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
