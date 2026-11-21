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

private const val LOOP = 12f
private const val TRUNK_X = 60f
private const val DECK_Y = 23
private const val FAR_X = 162f
private const val FAR_DECK = 25
private const val GROUND = 42
private const val FLIES = 44
private const val LADDER_X = 46f

private class Treehouse(
    seed: Int,
) {
    private val lat = NLattice(seed, 401)
    private val base = NFrame()
    private val frame = NFrame()
    private val palette: NPalette
    private val stars = NStars(seed, 403, 40, 10)
    private val flyX = FloatArray(FLIES)
    private val flyY = FloatArray(FLIES)
    private val flyA = FloatArray(FLIES)
    private val flyF = IntArray(FLIES)
    private val flyP = FloatArray(FLIES)
    private val skyTop = col("#140f30")
    private val skyMid = col("#4a2458")
    private val skyLow = col("#e0703a")
    private val haze = col("#5a3a6a")
    private val bark = col("#5a3a26")
    private val barkDark = col("#140a0c")
    private val leaf = col("#1e3a2a")
    private val plank = col("#8a5a32")
    private val roof = col("#4a2a2a")
    private val warm = col("#ffb84a")
    private val rope = col("#b08a5a")
    private val fly = col("#d8ff6a")

    init {
        val rng = nrng(seed, 405)
        for (f in 0 until FLIES) {
            flyX[f] = rng.float() * NCOLS
            flyY[f] = 31f + rng.float() * 15f
            flyA[f] = 2f + rng.float() * 5f
            flyF[f] = 1 + rng.int(2)
            flyP[f] = rng.float()
        }
        buildSky()
        buildForest(seed)
        buildTree()
        buildHouse()
        palette =
            NPalette(
                ramp(12, skyTop, skyMid, col("#a04a5a"), skyLow, col("#ffc080")) +
                    ramp(8, col("#120c1e"), haze, col("#9a6a8a")) +
                    ramp(10, barkDark, col("#3a2418"), bark, col("#c08a5a"), col("#ffd090")) +
                    ramp(8, col("#060c0a"), leaf, col("#4a7a3a"), col("#c0b050")) +
                    ramp(6, col("#2a1a10"), plank, col("#e0b070")) +
                    tones(roof, 5) +
                    tones(warm, 8) +
                    tones(fly, 6) +
                    listOf(Col(1f, 1f, 1f), col("#05040a")),
            )
    }

    private fun buildSky() {
        for (y in 0 until NROWS) {
            val h = y / 34f
            val c = if (h < 0.55f) skyTop.mix(skyMid, h / 0.55f) else skyMid.mix(skyLow, clamp01((h - 0.55f) / 0.45f))
            for (x in 0 until NCOLS) {
                val i = y * NCOLS + x
                base.r[i] = c.r
                base.g[i] = c.g
                base.b[i] = c.b
                base.cov[i] = if (h > 0.62f) 2f else 0.3f + 0.75f * h
            }
        }
        base.glow(120f, 34f, 34f, col("#ffc080"), 0.35f)
    }

    private fun buildForest(seed: Int) {
        val rng = nrng(seed, 407)
        // Three layers of trunks: pale in the dusk haze far away, darker nearer.
        for (layer in 0..2) {
            val k = layer / 2f
            val c = haze.mix(col("#e0703a"), 0.15f * (1f - k)).mix(col("#120c1e"), 0.25f + 0.55f * k)
            var x = rng.int(10).toFloat()
            while (x < NCOLS) {
                val w = 1.5f + layer * 1.8f + rng.float() * (1.5f + layer * 1.5f)
                for (y in 0 until GROUND + 2) {
                    val flare = if (y > GROUND - 4) (y - GROUND + 4) * 0.5f else 0f
                    val lean = sin(y * 0.08f + x) * 0.6f
                    for (xx in (x - flare + lean).toInt()..(x + w + flare + lean).toInt()) {
                        if (xx < 0 || xx >= NCOLS) continue
                        val side = (xx - x) / w
                        base.put(xx, y, c.scale(0.85f + 0.3f * clamp01(1f - side)), 2f)
                    }
                }
                // A canopy blob up top, with lighter undersides toward the glow.
                val cy = 2f + rng.float() * 8f
                base.ellipse(x + w / 2f, cy, 9f + layer * 4f + rng.float() * 6f, 3.5f + layer) { a, b ->
                    val n = lat.at((x + a * 9f) * 0.4f, (cy + b * 3f) * 0.9f + layer * 10f)
                    c.mix(leaf, 0.3f + 0.2f * k).scale(0.8f + 0.4f * n)
                }
                x += w + 16f + rng.float() * (30f - layer * 6f)
            }
            // A band of undergrowth for each layer.
            for (x2 in 0 until NCOLS) {
                val top = GROUND - 3 + layer * 2 - (2.5f * lat.fbm(x2 * 0.12f, layer * 5f, 3)).toInt()
                for (y in top until NROWS) {
                    base.put(
                        x2,
                        y,
                        c.mix(leaf, 0.4f).scale(0.8f - 0.1f * layer),
                        2f,
                        if (y ==
                            top
                        ) {
                            '▄'
                        } else {
                            NONE
                        },
                    )
                }
            }
        }
        // Fern tufts in front, dark against the undergrowth.
        for (x in 0 until NCOLS) {
            val h = (4f * lat.fbm(x * 0.15f, 31f, 3) - 0.8f).toInt()
            for (k in 0 until h) {
                val y = NROWS - 1 - k
                base.put(
                    x,
                    y,
                    col("#060c0a").mix(leaf, 0.3f + 0.15f * k),
                    2f,
                    if (k ==
                        h - 1
                    ) {
                        (if (x % 2 == 0) '▄' else '▖')
                    } else {
                        NONE
                    },
                )
            }
        }
    }

    private fun buildTree() {
        // The giant trunk with flared roots, bark grooves, and boughs reaching out.
        for (y in 0 until NROWS) {
            val half = 9f + max(0f, (y - 30f) * 0.25f).let { it * it } * 0.4f - min(0f, y - 4f) * 0.3f
            val sway = sin(y * 0.12f) * 1.2f
            for (x in (TRUNK_X - half + sway).toInt()..(TRUNK_X + half + sway).toInt()) {
                if (x < 0 || x >= NCOLS) continue
                val a = (x + 0.5f - TRUNK_X - sway) / half
                val groove = lat.at(x * 0.9f, y * 0.12f)
                var c = barkDark.mix(bark, clamp01(0.55f - a * 0.45f + 0.2f * groove))
                c = c.mix(col("#e0703a"), 0.18f * clamp01(a))
                base.put(x, y, if (groove > 0.72f) c.scale(0.6f) else c, 2f, if (groove > 0.78f) '│' else NONE)
            }
        }
        for ((dir, y0, len) in listOf(
            Triple(-1f, 6f, 40f),
            Triple(1f, 4f, 50f),
            Triple(1f, 15f, 30f),
            Triple(-1f, 14f, 26f),
        )) {
            for (k in 0 until len.toInt()) {
                val x = TRUNK_X + dir * (6f + k)
                val y = y0 - k * 0.18f + (k * 0.02f).let { it * it } * 8f
                val th = 2.4f - k / len * 1.8f
                for (yy in (y - th / 2f).toInt()..(y + th / 2f).toInt()) base.put(x.toInt(), yy, bark.scale(0.55f), 2f)
            }
        }
        // Canopy masses over everything at the top.
        for (y in 0 until 12) {
            for (x in 0 until NCOLS) {
                val n = lat.fbm(x * 0.06f, y * 0.25f, 4)
                val reach = 1f - y / 12f
                val near = 1f - abs(x - TRUNK_X - 10f) / 95f
                if (n * 0.8f + reach * 0.45f + near * 0.3f > 0.98f) {
                    val c =
                        col("#060c0a").mix(leaf, 0.55f + 0.35f * n).mix(
                            col("#c0b050"),
                            0.12f * clamp01((y - 7f) / 5f),
                        )
                    base.put(x, y, c, if (n > 0.55f) 2f else 1.75f)
                }
            }
        }
    }

    private fun buildHouse() {
        // Deck around the trunk with a railing.
        for (x in (TRUNK_X - 22).toInt()..(TRUNK_X + 28).toInt()) {
            base.put(x, DECK_Y, plank, 2f, '▀')
            base.put(x, DECK_Y + 1, plank.scale(0.5f), 2f, '▀')
            if (x % 3 == 0) base.put(x, DECK_Y - 1, rope.scale(0.8f), 2f, '│')
            base.put(x, DECK_Y - 2, rope.scale(0.85f), 2f, '─')
        }
        // Braces under the deck.
        for (k in 0..6) {
            base.put((TRUNK_X - 18 + k * 3).toInt(), DECK_Y + 2 + k, bark.scale(0.6f), 2f, '╲')
            base.put((TRUNK_X + 24 - k * 3).toInt(), DECK_Y + 2 + k, bark.scale(0.6f), 2f, '╱')
        }
        // The cabin: plank walls, a peaked roof, windows lit per frame.
        for (y in 13 until DECK_Y) {
            for (x in (TRUNK_X - 16).toInt()..(TRUNK_X + 18).toInt()) {
                val c =
                    plank.scale(0.65f + 0.2f * lat.at(x * 0.3f, y * 1f)).let {
                        if (y % 2 ==
                            0
                        ) {
                            it.scale(0.8f)
                        } else {
                            it
                        }
                    }
                base.put(x, y, c.mix(col("#e0703a"), 0.15f * clamp01((x - TRUNK_X) / 18f)), 2f)
            }
        }
        for (y in 5 until 13) {
            val half = 21f - (12 - y) * 2.6f
            for (x in (TRUNK_X + 1 - half).toInt()..(TRUNK_X + 1 + half).toInt()) {
                val edge = y == 12
                val c = roof.scale(0.7f + 0.3f * ((x + y) % 3) / 2f)
                base.put(
                    x,
                    y,
                    if (edge) roof.scale(0.5f) else c,
                    2f,
                    if (x ==
                        (TRUNK_X + 1 - half).toInt()
                    ) {
                        '▟'
                    } else if (x == (TRUNK_X + 1 + half).toInt()) {
                        '▙'
                    } else {
                        NONE
                    },
                )
            }
        }
        // Door.
        for (y in 16 until DECK_Y) {
            for (x in (TRUNK_X - 2).toInt()..(TRUNK_X + 1).toInt()) {
                base.put(
                    x,
                    y,
                    col("#2a1a10"),
                    2f,
                )
            }
        }
        // The far tree on the right with a small platform.
        for (y in 0 until NROWS) {
            val half = 6f + max(0f, y - 36f) * 0.5f
            for (x in (FAR_X - half).toInt()..(FAR_X + half).toInt()) {
                if (x >= NCOLS) continue
                val a = (x + 0.5f - FAR_X) / half
                val groove = lat.at(x * 0.9f + 50f, y * 0.12f)
                base.put(x, y, barkDark.mix(bark, clamp01(0.4f - a * 0.4f + 0.2f * groove)).scale(0.8f), 2f)
            }
        }
        for (x in (FAR_X - 14).toInt()..(FAR_X + 12).toInt()) {
            if (x >= NCOLS) continue
            base.put(x, FAR_DECK, plank.scale(0.8f), 2f, '▀')
            if (x % 3 == 0) base.put(x, FAR_DECK - 1, rope.scale(0.7f), 2f, '│')
            base.put(x, FAR_DECK - 2, rope.scale(0.7f), 2f, '─')
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val u = t / LOOP
        frame.copyFrom(base)
        stars.draw(frame, u, 0.8f) { y -> 1f - y / 10f }
        windows(u)
        bridge(u)
        ladder(u)
        lanterns(u)
        fireflies(u)
        frame.output(grid, palette)
    }

    private fun windows(u: Float) {
        for ((wx, ph) in listOf(Pair(TRUNK_X - 11f, 0f), Pair(TRUNK_X + 8f, 0.4f))) {
            val flick = 0.85f + 0.1f * sin(NTAU * (u * 7f + ph)) + 0.05f * sin(NTAU * (u * 13f + ph))
            for (y in 15..19) {
                for (x in wx.toInt()..(wx + 5).toInt()) {
                    val cross = x == (wx + 2.5f).toInt() || y == 17
                    frame.put(x, y, if (cross) col("#2a1a10") else warm.scale(flick), 2f)
                }
            }
            frame.glow(wx + 3f, 17.5f, 7f, warm, 0.22f * flick)
        }
    }

    /** The rope bridge to the far tree, sagging and swaying, planks and hand ropes. */
    private fun bridge(u: Float) {
        val x0 = TRUNK_X + 28f
        val x1 = FAR_X - 14f
        val sway = sin(NTAU * u * 2f)
        val sag = 5f + 0.9f * sway
        val n = (x1 - x0).toInt()
        for (k in 0..n) {
            val p = k / n.toFloat()
            val x = x0 + k
            val deck = ropeY(DECK_Y.toFloat(), FAR_DECK.toFloat(), sag, p)
            val hand = ropeY(DECK_Y - 2f, FAR_DECK - 2f, sag * 0.55f + 0.4f * sin(NTAU * u * 2f + 0.7f), p)
            frame.dot(x, deck, if (k % 2 == 0) plank else plank.scale(0.7f), 2f, if (fract(deck) < 0.5f) '▀' else '▄')
            frame.dot(x, hand, rope, 1f, '─')
            if (k % 4 == 0) {
                var y = hand + 1f
                while (y < deck) {
                    frame.dot(x, y, rope.scale(0.8f), 1f, '│')
                    y += 1f
                }
            }
        }
    }

    /** The rope ladder hanging from the deck down to the forest floor, swinging like a pendulum. */
    private fun ladder(u: Float) {
        val swing = 2.2f * sin(NTAU * u)
        val top = DECK_Y + 2f
        val len = GROUND + 2f - top
        for (k in 0..len.toInt()) {
            val q = k / len
            val off = swing * q * q
            val y = top + k
            val glyph =
                if (abs(swing) < 0.6f) {
                    '│'
                } else if (swing > 0f) {
                    '╲'
                } else {
                    '╱'
                }
            frame.dot(LADDER_X + off, y, rope, 2f, glyph)
            frame.dot(LADDER_X + 5f + off, y, rope, 2f, glyph)
            if (k % 2 == 1) for (x in 1..4) frame.dot(LADDER_X + x + off, y, plank.scale(0.85f), 2f, '─')
        }
    }

    private fun lanterns(u: Float) {
        val spots = floatArrayOf(TRUNK_X - 20f, TRUNK_X + 22f, FAR_X - 10f)
        val rows = floatArrayOf(13f, 13f, FAR_DECK - 6f)
        for (k in spots.indices) {
            val sw = 1.2f * sin(NTAU * (u * 2f + k * 0.3f))
            val x = spots[k]
            val y = rows[k]
            for (s in 0..1) frame.dot(x + sw * s * 0.5f, y + s, rope, 1f, '│')
            val lx = x + sw
            val ly = y + 2f
            val flick = 0.9f + 0.1f * sin(NTAU * (u * 9f + k * 0.37f))
            frame.glow(lx + 0.5f, ly + 0.5f, 10f, warm, 0.6f * flick)
            frame.dot(lx, ly, roof, 2f, '▄')
            frame.dot(lx, ly + 1f, warm.mix(Col(1f, 1f, 1f), 0.35f), 2f)
            frame.dot(lx, ly + 2f, roof, 2f, '▀')
        }
    }

    private fun fireflies(u: Float) {
        for (f in 0 until FLIES) {
            val a = NTAU * (u * flyF[f] + flyP[f])
            val x = flyX[f] + flyA[f] * 2f * sin(a)
            val y = flyY[f] + flyA[f] * 0.5f * sin(2f * a + flyP[f] * 4f)
            val blink = sin(NTAU * (u * 3f + flyP[f] * 7f))
            if (blink < 0f) continue
            if (blink > 0.7f) frame.glow(x, y, 3f, fly, 0.25f * blink)
            frame.dot(
                x,
                y,
                fly.scale(0.6f + 0.4f * blink),
                1f,
                if (blink >
                    0.7f
                ) {
                    '●'
                } else if (blink > 0.35f) {
                    '•'
                } else {
                    '·'
                },
            )
        }
    }
}

/**
 * A treehouse at dusk. A giant tree carries a plank cabin with glowing windows and swinging lanterns, a rope bridge
 * sags and sways over to a second tree, a rope ladder swings down to the forest floor, fireflies drift and blink
 * through layers of trunks fading into the violet dusk, and the first stars come out above the canopy. The seed sets
 * the forest, the bark and the fireflies.
 */
public fun treehouseDuskPiece(seed: Int = 0): AsciiPiece {
    val scene = Treehouse(seed)
    return asciiPiece(
        name = "treehouse dusk",
        ground = Rgb.fromHex("#07050e"),
        category = "scenes",
        cols = NCOLS,
        rows = NROWS,
        fps = 15,
        still = 2.5f,
        loop = LOOP,
        note = "a lantern-lit treehouse in a giant tree at dusk, a rope bridge swaying and fireflies drifting",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A lantern-lit treehouse in a giant tree at dusk, with a swaying rope bridge, a rope ladder and fireflies.
 *
 * @param seed sets the forest, the bark and the fireflies.
 */
@Composable
public fun AsciiTreehouseDusk(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A lantern-lit treehouse in a giant tree at dusk, a rope bridge swaying, fireflies",
) {
    val piece = remember(seed) { treehouseDuskPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
