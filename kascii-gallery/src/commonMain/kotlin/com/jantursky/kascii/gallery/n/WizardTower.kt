package com.jantursky.kascii.gallery.n

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

private const val LOOP = 16f
private const val TOWER_X = 118f
private const val TOWER_TOP = 10
private const val TOWER_BASE = 30
private const val SEA = 35
private const val SPARKS = 300
private const val BOOKS = 4
private const val MOON_X = 40f
private const val MOON_Y = 8f

private class Wizard(
    seed: Int,
) {
    private val lat = NLattice(seed, 501)
    private val base = NFrame()
    private val frame = NFrame()
    private val palette: NPalette
    private val stars = NStars(seed, 503, 110, 30)
    private val cliffTop = FloatArray(NCOLS)
    private val skyTop = col("#05041a")
    private val skyLow = col("#1e1a4a")
    private val cloudDark = col("#0c0a20")
    private val cloudLit = col("#6a6aa0")
    private val moon = col("#eef0ff")
    private val rock = col("#4a4a62")
    private val stone = col("#8a8098")
    private val roofCol = col("#3a2a6a")
    private val warm = col("#ffb84a")
    private val magic = col("#b06aff")
    private val magic2 = col("#5ae8ff")
    private val sea = col("#0a1238")

    init {
        for (x in 0 until NCOLS) {
            // A cliff rising from the sea to a crown under the tower, its edge broken by noise.
            // Sheer on the sea side, a broken crown under the tower, a shoulder stepping down to the right.
            val face = smooth(72f, 96f, x.toFloat())
            val crown = 30f + 1.5f * smooth(140f, 175f, x.toFloat()) - 1.2f * gauss(x.toFloat(), TOWER_X, 14f)
            val jag = 4f * (lat.fbm(x * 0.11f, 7f, 4) - 0.5f) + 1.5f * (lat.at(x * 0.5f, 3f) - 0.5f)
            cliffTop[x] = NROWS + 2f - face * (NROWS + 2f - crown) + jag * face
        }
        buildSky()
        buildSea()
        buildCliff()
        buildTower()
        palette =
            NPalette(
                ramp(10, skyTop, skyLow, col("#3a3a7a"), col("#8a8ac8")) +
                    ramp(6, cloudDark, cloudLit) +
                    ramp(5, col("#a0a8d8"), moon, Col(1f, 1f, 1f)) +
                    ramp(9, col("#06060e"), col("#1e1e2e"), rock, col("#9a98b8")) +
                    ramp(6, col("#2a2438"), stone, col("#d0c8e0")) +
                    tones(roofCol, 5) +
                    tones(warm, 7) +
                    tones(magic, 7) +
                    tones(magic2, 6) +
                    ramp(5, col("#020414"), sea, col("#2a3a7a")) +
                    listOf(col("#c03a3a"), col("#3a8a5a"), col("#3a5ac0"), col("#c8a070"), col("#2a1a10")),
            )
    }

    private fun buildSky() {
        for (y in 0 until NROWS) {
            val h = y / SEA.toFloat()
            val c = skyTop.mix(skyLow, clamp01(h))
            for (x in 0 until NCOLS) {
                val i = y * NCOLS + x
                base.r[i] = c.r
                base.g[i] = c.g
                base.b[i] = c.b
                base.cov[i] = 0.12f + 0.3f * clamp01(h)
            }
        }
        // The moon, low left, with a broad halo.
        base.glow(MOON_X, MOON_Y, 20f, col("#6a6ac0"), 0.3f)
        base.ellipse(MOON_X, MOON_Y, 9f, 9f / ROW_U) { a, b ->
            moon.scale(0.85f + 0.15f * clamp01(-a - b)).mix(
                col("#a0a8d8"),
                0.3f * lat.at(a * 3f + 9f, b * 3f),
            )
        }
    }

    private fun buildSea() {
        for (y in SEA until NROWS) {
            for (x in 0 until NCOLS) {
                val d = (y - SEA + 1f) / (NROWS - SEA)
                val c = sea.mix(col("#020414"), d * 0.6f)
                // The moon's road on the water.
                val road = gauss(x + 0.5f, MOON_X, 3f + d * 10f)
                val ripple = sin(x * 0.7f + y * 3.1f) * 0.5f + 0.5f
                val i = y * NCOLS + x
                base.r[i] = c.r
                base.g[i] = c.g
                base.b[i] = c.b
                base.cov[i] = 1.5f + 0.4f * (1f - d)
                if (road * ripple > 0.35f) {
                    base.put(x, y, moon.scale(0.5f + 0.5f * road), 1f, if (ripple > 0.8f) '-' else '·')
                } else if (lat.at(x * 0.25f, y * 2.3f) > 0.78f) {
                    base.put(x, y, col("#2a3a7a").scale(1.2f - d * 0.5f), 1f, '~')
                }
            }
        }
        for (x in 0 until NCOLS) base.put(x, SEA, col("#2a3a7a"), 2f, '▄')
    }

    private fun buildCliff() {
        val left = IntArray(NROWS) { NCOLS }
        for (y in 0 until NROWS) for (x in NCOLS - 1 downTo 0) if (y + 1f > cliffTop[x]) left[y] = x
        for (x in 0 until NCOLS) {
            val top = cliffTop[x]
            for (y in top.toInt() until NROWS) {
                if (y + 1f <= top) continue
                val depth = y - top
                val n1 = lat.fbm(x * 0.08f + y * 0.05f, y * 0.32f, 4)
                val n2 = lat.fbm((x + 1) * 0.08f + y * 0.05f, y * 0.32f, 4)
                // Rock facets: the ones turned to the moon on the left catch light, the rest stay in shadow.
                var k = 0.3f + (n1 - n2) * 9f + 0.25f * (n1 - 0.5f)
                k += 0.55f * exp01(depth * 1.6f) + 0.45f * exp01((x - left[y]) * 0.5f)
                k *= 1f - 0.45f * clamp01(depth / 16f)
                val crack = lat.at(x * 0.55f, y * 0.09f + 13f)
                if (crack > 0.74f) k *= 0.45f
                val c = col("#06060e").mix(rock, clamp01(k)).mix(col("#9a98b8"), clamp01(k - 0.8f) * 2f)
                base.put(x, y, c, 2f, if (y == top.toInt() && fract(top) > 0.5f) '▄' else NONE)
            }
        }
        // A rim of mist at the cliff foot.
        for (x in 0 until NCOLS) {
            for (y in SEA - 2..SEA + 2) {
                val n = lat.fbm(x * 0.04f, y * 0.5f + 40f, 3)
                if (n > 0.52f) base.haze(y * NCOLS + x, col("#4a4a80"), (n - 0.52f) * 1.6f)
            }
        }
    }

    private fun exp01(d: Float): Float = 1f / (1f + d * 0.6f)

    /** The tower's crooked centre line at row [y]. */
    private fun axis(y: Float): Float = TOWER_X + 3f * sin(y * 0.17f + 0.5f) + (TOWER_BASE - y) * 0.16f

    private fun halfWidth(y: Float): Float = 8f + (y - TOWER_TOP) * 0.15f

    private fun buildTower() {
        for (y in TOWER_TOP until min(NROWS, cliffTop[TOWER_X.toInt()].toInt() + 2)) {
            val cx = axis(y.toFloat())
            val half = halfWidth(y.toFloat())
            for (x in (cx - half).toInt()..(cx + half).toInt()) {
                val a = (x + 0.5f - cx) / half
                if (abs(a) > 1f) continue
                // Rounded stone: lit on the moon side, courses of blocks offset every row.
                val course = (x + (if (y % 2 == 0) 0 else 2)) % 4 == 0
                val k = 0.6f - 0.45f * a + 0.12f * lat.at(x * 0.7f, y * 1.3f)
                var c = col("#2a2438").mix(stone, clamp01(k))
                if (course) c = c.scale(0.75f)
                base.put(x, y, c, 2f, if (abs(a) > 0.92f) '█' else NONE)
            }
        }
        // A balcony ring and a crooked conical roof bending over at the tip.
        val by = 17
        for (x in (
            axis(
                by.toFloat(),
            ) - halfWidth(by.toFloat()) - 2f
        ).toInt()..(axis(by.toFloat()) + halfWidth(by.toFloat()) + 2f).toInt()) {
            base.put(x, by, stone.scale(0.6f), 2f, '▀')
            if (x % 2 == 0) base.put(x, by - 1, stone.scale(0.5f), 2f, '│')
        }
        for (y in 0 until TOWER_TOP + 1) {
            val q = (TOWER_TOP + 1 - y) / (TOWER_TOP + 1f)
            val half = 12.5f * (1f - q) + 0.5f
            val cx = axis(TOWER_TOP.toFloat()) - q * q * 9f
            for (x in (cx - half).toInt()..(cx + half).toInt()) {
                val a = (x + 0.5f - cx) / half
                if (abs(a) > 1f) continue
                val c =
                    roofCol.scale(0.6f + 0.6f * clamp01(0.6f - a * 0.5f)).let {
                        if ((x + y) % 3 ==
                            0
                        ) {
                            it.scale(0.8f)
                        } else {
                            it
                        }
                    }
                base.put(x, y, c, 2f, if (y == TOWER_TOP) '▀' else NONE)
            }
        }
        // Doorway at the foot.
        val fy = cliffTop[TOWER_X.toInt()].toInt()
        for (y in fy - 4 until fy) {
            for (x in (
                axis(
                    y.toFloat(),
                ) - 2
            ).toInt()..(axis(y.toFloat()) + 1).toInt()) {
                base.put(x, y, col("#2a1a10"), 2f)
            }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val u = t / LOOP
        frame.copyFrom(base)
        val bolt =
            fract(u * 2f + 0.1f).let {
                if (it < 0.025f) {
                    1f
                } else if (it < 0.05f) {
                    0.3f
                } else {
                    0f
                }
            }
        stars.draw(frame, u, 1f) { y -> 1f - y / 34f }
        clouds(u, bolt)
        windows(u)
        spiral(u, false)
        spiral(u, true)
        orb(u)
        books(u)
        owl(u)
        frame.output(grid, palette)
    }

    /** Storm clouds churning in a closed circle, rimmed by moonlight, lit from inside by distant flickers. */
    private fun clouds(
        u: Float,
        bolt: Float,
    ) {
        val ox = 1.4f * cos(NTAU * u)
        val oy = 0.7f * sin(NTAU * u)
        for (y in 0 until 26) {
            val h = y / 26f
            for (x in 0 until NCOLS) {
                val i = y * NCOLS + x
                if (frame.cov[i] > 1.4f) continue
                val d = lat.fbm(x * 0.03f + ox + y * 0.04f, y * 0.14f + oy, 4)
                val dens = smooth(0.45f, 0.68f, d) * (1f - 0.8f * h)
                if (dens < 0.03f) continue
                val m = gauss(dist(x + 0.5f, y + 0.5f, MOON_X, MOON_Y), 0f, 34f)
                val rim = (1f - dens) * dens * 4f
                val c =
                    cloudDark.mix(cloudLit, 0.1f + 0.6f * rim * (0.3f + m)).mix(
                        magic.mix(Col(1f, 1f, 1f), 0.4f),
                        bolt * (0.3f + 0.4f * rim),
                    )
                frame.put(
                    x,
                    y,
                    Col(frame.r[i], frame.g[i], frame.b[i]).mix(c, min(1f, dens * 1.8f)),
                    if (dens <
                        0.25f
                    ) {
                        dens * 1.2f
                    } else {
                        0.6f + 1.4f * dens
                    },
                )
            }
        }
    }

    private fun windows(u: Float) {
        val wins = listOf(Pair(14f, 0f), Pair(21f, 0.3f), Pair(26f, 0.55f))
        for ((wy, ph) in wins) {
            val cx = axis(wy) - 1.5f
            val flick = 0.85f + 0.1f * sin(NTAU * (u * 9f + ph)) + 0.05f * sin(NTAU * (u * 23f + ph * 3f))
            for (y in wy.toInt()..wy.toInt() + 1) {
                for (x in cx.toInt()..cx.toInt() + 2) {
                    frame.put(
                        x,
                        y,
                        warm.scale(flick),
                        2f,
                        if (y ==
                            wy.toInt()
                        ) {
                            '▄'
                        } else {
                            NONE
                        },
                    )
                }
            }
            frame.glow(cx + 1.5f, wy + 1f, 5f, warm, 0.2f * flick)
        }
    }

    /** A helix of sparks winding up around the tower; the back half passes behind the stone. */
    private fun spiral(
        u: Float,
        front: Boolean,
    ) {
        for (k in 0 until SPARKS) {
            val strand = k % 2
            val p = (k / 2) / (SPARKS / 2f)
            val y = TOWER_BASE + 1f - p * (TOWER_BASE + 1f)
            val a = NTAU * (p * 2f + u * 2f + strand * 0.5f)
            val depth = sin(a)
            if ((depth > 0f) != front) continue
            // Pulses of light run up the ribbon four times per loop.
            val pulse = max(0f, sin(NTAU * (p * 3f - u * 4f))).let { it * it * it }
            val r = halfWidth(max(y, TOWER_TOP.toFloat())) * (1f - 0.3f * p) + 11f - 2f * p
            val x = axis(max(y, 0f)) + cos(a) * r
            val b = (0.7f + 0.3f * pulse) * (if (front) 1f else 0.55f)
            val c =
                magic.mix(magic2, p).scale(0.5f + 0.5f * b).mix(
                    Col(1f, 1f, 1f),
                    (0.25f + pulse * 0.4f) * (if (front) 1f else 0f),
                )
            if (front) frame.glow(x, y, 2.5f, magic.mix(magic2, p), 0.12f + 0.3f * pulse)
            val i = y.toInt() * NCOLS + x.toInt()
            if (!front && i >= 0 && i < NCELLS && frame.cov[i] > 1.4f && frame.ch[i] == NONE) continue
            val solid = front && i >= 0 && i < NCELLS && frame.cov[i] > 1.4f
            if (solid) {
                frame.dot(x, y, c, 2f)
            } else {
                frame.dot(
                    x,
                    y,
                    c,
                    1f,
                    if (front && pulse > 0.5f) {
                        '*'
                    } else if (front) {
                        '+'
                    } else {
                        '·'
                    },
                )
            }
        }
    }

    /** A pulsing orb of light on the roof tip. */
    private fun orb(u: Float) {
        val tipX = axis(TOWER_TOP.toFloat()) - 9f
        val pulse = 0.6f + 0.4f * sin(NTAU * u * 4f)
        frame.glow(tipX, 0.5f, 9f, magic, 0.45f * pulse)
        frame.glow(tipX, 0.5f, 3f, magic2, 0.7f * pulse)
        frame.dot(tipX, 0.5f, Col(1f, 1f, 1f), 2f, '●')
    }

    /** Spell books flapping round the tower on a wide tilted orbit. */
    private fun books(u: Float) {
        val cols = listOf(col("#c03a3a"), col("#3a8a5a"), col("#3a5ac0"), col("#c8a070"))
        for (b in 0 until BOOKS) {
            val a = NTAU * (u + b / BOOKS.toFloat())
            val x = TOWER_X + cos(a) * 34f
            val y = 14f + sin(a) * 4f + b * 2.5f
            if (sin(a) < -0.2f && abs(cos(a)) < 0.3f) continue
            val flap = fract(u * 24f + b * 0.25f) < 0.5f
            val c = cols[b]
            if (flap) {
                frame.dot(x - 2f, y - 1f, c, 2f, '▄')
                frame.dot(x - 1f, y, c, 2f, '▀')
                frame.dot(x + 1f, y, c, 2f, '▀')
                frame.dot(x + 2f, y - 1f, c, 2f, '▄')
            } else {
                frame.dot(x - 2f, y, c, 2f, '▄')
                frame.dot(x - 1f, y, c, 2f, '▄')
                frame.dot(x + 1f, y, c, 2f, '▄')
                frame.dot(x + 2f, y, c, 2f, '▄')
            }
            frame.dot(x, y, col("#f0e8d0"), 2f, if (flap) '▀' else '█')
            frame.glow(x, y, 3f, magic, 0.12f)
        }
    }

    /** An owl crossing the sky once per loop, wings beating. */
    private fun owl(u: Float) {
        val p = fract(u + 0.15f)
        val x = NCOLS + 10f - p * (NCOLS + 20f)
        val y = 24f - 12f * p + 1.5f * sin(NTAU * p * 3f)
        val up = fract(u * 20f) < 0.5f
        val c = col("#c8a070")
        val xi = x.toInt()
        val yi = y.toInt()
        // Head with two eyes, a round body, wings up in a V or swept down.
        frame.text(xi - 1, yi - 1, "▄▄▄", c)
        frame.put(xi - 1, yi, col("#ffe08a"), 2f, '●')
        frame.put(xi, yi, c.scale(0.8f), 2f, 'v')
        frame.put(xi + 1, yi, col("#ffe08a"), 2f, '●')
        frame.text(xi - 1, yi + 1, "▀█▀", c.scale(0.85f))
        if (up) {
            frame.text(xi - 6, yi - 2, "▀▄", c.scale(0.8f))
            frame.text(xi - 4, yi - 1, "▀▄", c.scale(0.8f))
            frame.text(xi + 3, yi - 1, "▄▀", c.scale(0.8f))
            frame.text(xi + 5, yi - 2, "▄▀", c.scale(0.8f))
        } else {
            frame.text(xi - 5, yi, "▄▄▄▄", c.scale(0.8f))
            frame.text(xi + 2, yi, "▄▄▄▄", c.scale(0.8f))
            frame.put(xi - 6, yi + 1, c.scale(0.6f), 2f, '▀')
            frame.put(xi + 6, yi + 1, c.scale(0.6f), 2f, '▀')
        }
    }
}

/**
 * A wizard's tower at night. A tall crooked stone tower leans on a sea cliff under churning storm clouds, its roof
 * bending over to a pulsing orb of light. A helix of sparks winds up around it, passing behind the stone, spell books
 * flap round on a wide orbit, candlelit windows flicker, an owl crosses in front of the moon and distant flickers light
 * the clouds from inside. The seed sets the cliff, the clouds and the stars.
 */
public fun wizardTowerPiece(seed: Int = 0): AsciiPiece {
    val scene = Wizard(seed)
    return asciiPiece(
        name = "wizard tower",
        ground = Rgb.fromHex("#04030f"),
        category = "scenes",
        cols = NCOLS,
        rows = NROWS,
        fps = 15,
        still = 4.5f,
        loop = LOOP,
        note = "a crooked wizard tower on a sea cliff under storm clouds, a spiral of magic light winding round it",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A crooked wizard tower on a sea cliff at night, with a spiral of magic light, flying books, an owl and storm clouds.
 *
 * @param seed sets the cliff, the clouds and the stars.
 */
@Composable
public fun AsciiWizardTower(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A crooked wizard tower on a cliff at night, magic light spiralling round it",
) {
    val piece = remember(seed) { wizardTowerPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
