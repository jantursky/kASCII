package com.jantursky.kascii.gallery.i

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
import kotlin.math.sin

private const val LOOP = 36f
private const val TX = 96f
private const val FACE_Y = 16f
private const val FACE_R = 9.5f
private const val BELL_Y = 6f
private const val SQUARE = 40
private const val MAX_WINDOWS = 160
private const val BIRDS = 7

private class Tower(
    seed: Int,
) {
    private val lat = ILattice(seed, 61)
    private val base = IFrame()
    private val frame = IFrame()
    private val palette: IPalette
    private val stars = IStars(seed, 63, 60, 14)
    private val winX = IntArray(MAX_WINDOWS)
    private val winY = IntArray(MAX_WINDOWS)
    private val winPhase = FloatArray(MAX_WINDOWS)
    private val winMode = IntArray(MAX_WINDOWS)
    private val winDim = FloatArray(MAX_WINDOWS)
    private var windows = 0
    private val stone = col("#c0a8a0")
    private val stoneDark = col("#2a2034")
    private val warm = col("#ffc46a")
    private val face = col("#fff0c8")
    private val bronze = col("#c08a3a")
    private val roofCol = col("#3a4a6a")

    init {
        buildSky(seed)
        buildTown(seed)
        buildTower()
        palette =
            IPalette(
                ramp(8, col("#06061a"), col("#141a40"), col("#2a2a5a")) +
                    ramp(8, col("#2a2a5a"), col("#5a3270"), col("#a0446a")) +
                    ramp(8, col("#a0446a"), col("#e07040"), col("#ffb060"), col("#ffe0a0")) +
                    ramp(8, col("#120c1c"), stoneDark, stone, col("#d0c4cc")) +
                    tones(stone, 8) +
                    tones(warm, 8) +
                    tones(face, 5) +
                    tones(bronze, 6) +
                    tones(roofCol, 6) +
                    listOf(Col(1f, 1f, 1f), col("#1a1420")),
            )
    }

    private fun buildSky(seed: Int) {
        val n = ILattice(seed, 67)
        for (y in 0 until SQUARE) {
            for (x in 0 until ICOLS) {
                val i = y * ICOLS + x
                val h = y / 32f
                val c =
                    if (h < 0.5f) {
                        col("#141a40").mix(col("#5a3270"), h / 0.5f)
                    } else if (h < 0.8f) {
                        col("#5a3270").mix(col("#e07040"), (h - 0.5f) / 0.3f)
                    } else {
                        col("#e07040").mix(col("#ffb060"), clamp01((h - 0.8f) / 0.2f))
                    }
                val cloud = smooth(0.56f, 0.74f, n.fbm(x * 0.018f, y * 0.35f, 4)) * gauss(h, 0.5f, 0.22f)
                val cc = c.mix(col("#ff8a6a"), cloud * 0.6f)
                base.r[i] = cc.r
                base.g[i] = cc.g
                base.b[i] = cc.b
                base.cov[i] =
                    if (h > 0.78f) 1.6f + 0.4f * clamp01((h - 0.78f) / 0.2f) else 0.25f + 0.75f * h + 0.5f * cloud
            }
        }
        base.glow(150f, 31f, 26f, col("#ffb060"), 0.35f)
    }

    private fun buildTown(seed: Int) {
        val rng = irng(seed, 65)
        // Two rows of houses: the far row paler in the haze, the near row darker with lit windows.
        for (layer in 0..1) {
            var x = -rng.int(8)
            while (x < ICOLS) {
                val w = 10 + rng.int(14)
                val wall = if (layer == 0) 24 + rng.int(4) else 28 + rng.int(4)
                val pitch = 3 + rng.int(4)
                val near = layer == 1
                if (near && x + w > 78 && x < 114) {
                    x += w
                    continue
                }
                val body = if (near) col("#100a16") else col("#2a1e3a").mix(col("#a0446a"), 0.3f)
                val roof = if (near) roofCol.scale(0.3f) else roofCol.scale(0.5f).mix(col("#a0446a"), 0.4f)
                for (xx in x until x + w) {
                    val mid = x + w / 2f
                    val top = wall - pitch + (abs(xx + 0.5f - mid) / (w / 2f) * pitch).toInt()
                    for (y in top until SQUARE) {
                        val c = if (y < wall) roof else body
                        base.put(xx, y, c, 2f, if (y == top) (if (xx < mid) '▟' else '▙') else NONE)
                    }
                }
                // A chimney now and then.
                if (rng.float() < 0.5f) {
                    val cx = x + 2 + rng.int(max(1, w - 4))
                    for (y in wall - pitch - 2 until wall - pitch + 1) base.put(cx, y, roof.scale(0.8f), 2f)
                }
                if (near || rng.float() < 0.6f) {
                    for (wy in wall + 1 until SQUARE - 1 step 3) {
                        for (wx in x + 2 until x + w - 2 step 4) {
                            if (windows >= MAX_WINDOWS) break
                            winX[windows] = wx
                            winY[windows] = wy
                            winPhase[windows] = rng.float()
                            // 0 dark, 1 lit, 2 switching once per loop.
                            winMode[windows] =
                                if (rng.float() < 0.25f) {
                                    2
                                } else if (rng.float() < 0.2f) {
                                    0
                                } else {
                                    1
                                }
                            winDim[windows] = if (near) 1f else 0.6f
                            windows++
                        }
                    }
                }
                x += w + (if (near) rng.int(3) else 0)
            }
        }
        // The square: cobbles in perspective, lit by lamps.
        for (y in SQUARE until IROWS) {
            val d = y - SQUARE + 1f
            for (x in 0 until ICOLS) {
                val cell = ((x - TX) / (2f + d * 0.6f))
                val joint = fract(cell) < 0.12f || (y % 2 == 0 && fract(cell + 0.5f) < 0.12f)
                val n = lat.at(x * 0.5f, y * 1.3f)
                val k = 0.15f + 0.025f * n + 0.04f * d / 8f
                val c = col("#3a2e44").scale(k / 0.2f)
                base.put(x, y, if (joint) c.scale(0.55f) else c, if (joint) 1.75f else 2f)
            }
        }
        // Lamp posts.
        for (lx in intArrayOf(38, 154)) {
            for (y in 31 until 45) base.put(lx, y, col("#1a1420"), 2f, '│')
            base.put(lx - 1, 30, col("#1a1420"), 2f, '▗')
            base.put(lx + 1, 30, col("#1a1420"), 2f, '▖')
            base.put(lx, 30, warm.mix(Col(1f, 1f, 1f), 0.4f), 2f)
            base.put(lx, 29, col("#1a1420"), 2f, '▄')
            base.glow(lx + 0.5f, 30.5f, 7f, warm, 0.55f)
            base.glow(lx + 0.5f, 44f, 10f, warm, 0.25f)
        }
    }

    private fun buildTower() {
        // Base with an arched, lit doorway.
        for (y in 28 until SQUARE + 1) {
            for (x in 80..112) {
                val k = 0.55f - 0.3f * (x - 80) / 32f + 0.1f * lat.at(x * 0.4f, y * 0.8f)
                var c = stoneDark.mix(stone, clamp01(k) * 0.75f)
                if ((y % 2 == 0 && x % 6 == 0) || (y % 2 == 1 && x % 6 == 3)) c = c.scale(0.75f)
                base.put(x, y, c, 2f, if (y == 28) '▄' else NONE)
            }
        }
        for (y in 33..SQUARE) {
            val w = if (y == 33) 2 else 3
            for (x in (TX - w).toInt()..(TX + w).toInt()) base.put(x, y, warm.scale(0.9f), 2f)
        }
        base.glow(TX + 0.5f, 38f, 8f, warm, 0.35f)
        // The shaft.
        for (y in 4 until 28) {
            for (x in 83..109) {
                val k = 0.6f - 0.4f * (x - 83) / 26f + 0.08f * lat.at(x * 0.4f, y * 0.8f + 30f)
                var c = stoneDark.mix(stone, clamp01(k) * 0.8f)
                c = c.mix(col("#ffb060"), 0.12f * clamp01((x - 96f) / 11f))
                val edge = x == 83 || x == 109
                base.put(x, y, if (edge) c.scale(0.7f) else c, 2f, if (edge) '█' else NONE)
            }
        }
        // Cornices.
        for (x in 81..111) {
            base.put(x, 27, stone.scale(0.7f), 2f, '▀')
            base.put(x, 10, stone.scale(0.75f), 2f, '▄')
            base.put(x, 3, stone.scale(0.65f), 2f, '▄')
        }
        // Belfry opening, dark with the warm bell inside drawn per frame.
        for (y in 4..9) {
            for (x in 89..103) {
                val a = (x + 0.5f - TX) / 7.5f
                val arch = y > 4 || abs(a) < 0.6f
                if (arch) base.put(x, y, col("#0e0a14"), 2f)
            }
        }
        // Spire.
        for (y in 0..2) {
            val w = 2 + y * 3
            for (x in (TX - w).toInt()..(TX + w).toInt()) {
                base.put(
                    x,
                    y,
                    roofCol.mix(
                        col("#ffb060"),
                        if (x >
                            TX
                        ) {
                            0.2f
                        } else {
                            0f
                        },
                    ),
                    2f,
                    if (x == (TX - w).toInt()) {
                        '▟'
                    } else if (x == (TX + w).toInt()) {
                        '▙'
                    } else {
                        NONE
                    },
                )
            }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val u = t / LOOP
        frame.copyFrom(base)
        stars.draw(frame, u, 0.9f) { y -> 1f - y / 14f }
        windows(u)
        birds(u)
        bell(u)
        clock(u)
        frame.output(grid, palette)
    }

    private fun windows(u: Float) {
        for (k in 0 until windows) {
            // A few windows switch on and off once per loop.
            val lit = if (winMode[k] == 2) fract(u + winPhase[k]) < 0.55f else winMode[k] == 1
            val x = winX[k]
            val y = winY[k]
            val c = if (lit) warm.scale(winDim[k]) else col("#120c1c")
            frame.put(x, y, c, 2f)
            frame.put(x + 1, y, c, 2f)
            if (lit) {
                frame.light(y * ICOLS + x - 1, warm, 0.12f)
                frame.light(y * ICOLS + x + 2, warm, 0.12f)
            }
        }
    }

    /** A loose V of birds crossing the sky twice per loop, wings flapping. */
    private fun birds(u: Float) {
        for (b in 0 until BIRDS) {
            val rank = (b + 1) / 2
            val side = if (b % 2 == 0) 1 else -1
            val p = fract(u * 2f)
            val x = -20f + (ICOLS + 40f) * p - rank * 5f
            val y = 17f + side * rank * 1.2f + 1.5f * sin(ITAU * (u * 4f + b * 0.13f))
            val xi = x.toInt()
            val yi = y.toInt()
            if (xi < 1 || xi >= ICOLS - 1 || yi < 0 || yi >= IROWS) continue
            val flap = fract(u * 48f + b * 0.29f) < 0.5f
            val c = col("#06040a")
            if (flap) {
                frame.put(xi - 1, yi, c, 1f, '\\')
                frame.put(xi, yi, c, 1f, 'v')
                frame.put(xi + 1, yi, c, 1f, '/')
            } else {
                frame.put(xi - 1, yi, c, 1f, '-')
                frame.put(xi, yi, c, 1f, 'v')
                frame.put(xi + 1, yi, c, 1f, '-')
            }
        }
    }

    /** The bell swings four times per loop; at each end of its swing a ring of sound spreads from the belfry. */
    private fun bell(u: Float) {
        val swing = 0.45f * sin(ITAU * u * 4f)
        for (y in 0..3) {
            val r = BELL_Y - 1f + y
            val off = sin(swing) * (y + 1) * ROW_U
            val w = 1.5f + y * 1.1f
            for (x in (TX + off - w).toInt()..(TX + off + w).toInt()) {
                val k = 0.75f - 0.35f * (x - (TX + off - w)) / (2 * w)
                frame.put(x, r.toInt(), bronze.scale(k + 0.3f), 2f, if (y == 3) '▀' else NONE)
            }
        }
        frame.put((TX + sin(swing) * 5f * ROW_U).toInt(), BELL_Y.toInt() + 3, bronze.scale(0.6f), 2f, '•')
        frame.glow(TX + 0.5f, BELL_Y + 1.5f, 5f, warm, 0.18f)
        // Sound rings at the far ends of the swing.
        val beat = fract(u * 8f)
        val ring = beat * 26f
        val fade = 1f - beat
        if (fade > 0.1f) {
            for (s in -1..1 step 2) {
                for (k in -2..2) {
                    val ay = BELL_Y + 1f + k * 1.3f
                    val ax = TX + s * (10f + ring) - s * abs(k) * 1.4f
                    frame.put(ax.toInt(), ay.toInt(), face.scale(0.5f + 0.5f * fade), fade, if (s > 0) ')' else '(')
                }
            }
        }
    }

    /** The clock: a glowing face with hour marks, the minute hand turning twelve times per loop, the hour hand once. */
    private fun clock(u: Float) {
        frame.glow(TX + 0.5f, FACE_Y + 0.5f, FACE_R * 1.3f, warm, 0.25f)
        val ry = FACE_R / ROW_U
        for (y in (FACE_Y - ry - 1).toInt()..(FACE_Y + ry + 1).toInt()) {
            for (x in (TX - FACE_R - 2).toInt()..(TX + FACE_R + 2).toInt()) {
                val d = dist(x + 0.5f, y + 0.5f, TX + 0.5f, FACE_Y + 0.5f)
                if (d > FACE_R + 1.6f) continue
                if (d > FACE_R) {
                    frame.put(x, y, bronze, 2f)
                    continue
                }
                val k = 1f - 0.25f * d / FACE_R
                frame.put(x, y, face.scale(k).mix(warm, 0.3f * d / FACE_R), 2f)
            }
        }
        for (h in 0 until 12) {
            val a = ITAU * h / 12f
            val x = TX + 0.5f + sin(a) * (FACE_R - 1.6f)
            val y = FACE_Y + 0.5f - cos(a) * (FACE_R - 1.6f) / ROW_U
            frame.put(x.toInt(), y.toInt(), col("#2a1a14"), 2f, if (h % 3 == 0) '█' else '·')
        }
        val minute = ITAU * fract(u * 12f)
        val hour = ITAU * fract(u + 0.36f)
        hand(hour, FACE_R * 0.55f, 0.9f)
        hand(minute, FACE_R * 0.85f, 0.6f)
        frame.put(TX.toInt(), FACE_Y.toInt(), bronze, 2f, '●')
    }

    private fun hand(
        a: Float,
        len: Float,
        half: Float,
    ) {
        val x1 = TX + 0.5f + sin(a) * len
        val y1 = FACE_Y + 0.5f - cos(a) * len / ROW_U
        val n = (len * 2f).toInt()
        for (k in 0..n) {
            val p = k.toFloat() / n
            val x = TX + 0.5f + (x1 - TX - 0.5f) * p
            val y = FACE_Y + 0.5f + (y1 - FACE_Y - 0.5f) * p
            frame.put(x.toInt(), y.toInt(), col("#1a1014"), 2f)
            frame.put((x + half).toInt(), y.toInt(), col("#1a1014"), 2f)
        }
    }
}

/**
 * A town square at dusk under a stone clock tower. The minute hand races round the glowing face while the hour hand
 * creeps, the bell swings in the belfry sending rings of sound across the rooftops, a V of birds crosses the sunset,
 * windows in the houses switch on and off and two street lamps pool warm light on the cobbles. The seed builds the
 * houses, the windows, the clouds and the stars.
 */
public fun clockTowerPiece(seed: Int = 0): AsciiPiece {
    val scene = Tower(seed)
    return asciiPiece(
        name = "clock tower",
        ground = Rgb.fromHex("#06061a"),
        category = "scenes",
        cols = ICOLS,
        rows = IROWS,
        fps = 15,
        still = 7f,
        loop = LOOP,
        note = "a clock tower over a town square at dusk, its bell swinging and birds crossing the sunset",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A clock tower over a town square at dusk, its hands turning, its bell swinging and birds crossing the sunset.
 *
 * @param seed builds the houses, the windows, the clouds and the stars.
 */
@Composable
public fun AsciiClockTower(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A clock tower over a town square at dusk, its bell swinging, birds flying past",
) {
    val piece = remember(seed) { clockTowerPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
