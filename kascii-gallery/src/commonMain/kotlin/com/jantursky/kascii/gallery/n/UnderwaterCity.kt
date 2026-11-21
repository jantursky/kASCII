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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 16f
private const val FLOOR = 40
private const val BUBBLES = 46
private const val KELP = 14
private const val FISH = 30

/** A glass dome: centre column, base row, radius in width units. */
private class Dome(
    val x: Float,
    val y: Float,
    val r: Float,
)

private class Deep(
    seed: Int,
) {
    private val lat = NLattice(seed, 301)
    private val base = NFrame()
    private val frame = NFrame()
    private val palette: NPalette
    private val domes = listOf(Dome(96f, FLOOR + 1f, 36f), Dome(34f, FLOOR + 1f, 20f), Dome(160f, FLOOR + 1f, 23f))
    private val bubX = FloatArray(BUBBLES)
    private val bubP = FloatArray(BUBBLES)
    private val bubR = IntArray(BUBBLES)
    private val kelpX = FloatArray(KELP)
    private val kelpH = FloatArray(KELP)
    private val kelpP = FloatArray(KELP)
    private val fishLane = IntArray(FISH)
    private val fishOff = FloatArray(FISH)
    private val fishCol = IntArray(FISH)
    private val surface = col("#3aa8c8")
    private val water = col("#0c3a5a")
    private val abyss = col("#031024")
    private val glass = col("#8af0ff")
    private val sand = col("#c8b088")
    private val warm = col("#ffc46a")
    private val hull = col("#ffcc3a")
    private val tower = col("#5a7a9a")
    private val fishCols = listOf(col("#ff8a3a"), col("#ffe05a"), col("#c8e0f0"), col("#ff5a7a"))

    init {
        val rng = nrng(seed, 303)
        for (b in 0 until BUBBLES) {
            val src = rng.int(5)
            bubX[b] = floatArrayOf(70f, 118f, 30f, 166f, 140f)[src] + (rng.float() - 0.5f) * 4f
            bubP[b] = rng.float()
            bubR[b] = 1 + rng.int(2)
        }
        for (k in 0 until KELP) {
            kelpX[k] =
                if (k <
                    KELP / 2
                ) {
                    2f + rng.float() * 14f + k * 1.5f
                } else {
                    178f - rng.float() * 14f + (k - KELP / 2) * 1.5f
                }
            kelpH[k] = 10f + rng.float() * 14f
            kelpP[k] = rng.float()
        }
        for (f in 0 until FISH) {
            fishLane[f] = rng.int(3)
            fishOff[f] = rng.float()
            fishCol[f] = rng.int(fishCols.size)
        }
        buildWater(seed)
        buildCity(seed)
        buildFloor(seed)
        palette =
            NPalette(
                ramp(12, abyss, water, col("#1a6a8a"), surface, col("#a8f0ff")) +
                    ramp(8, col("#0a2a3a"), tower, col("#a8c8e0")) +
                    tones(glass, 7) +
                    ramp(8, col("#2a2418"), col("#6a5a40"), sand, col("#fff0c8")) +
                    tones(warm, 8) +
                    tones(hull, 6) +
                    fishCols.flatMap { tones(it, 4) } +
                    ramp(5, col("#0a2418"), col("#2a7a3a"), col("#7ac05a")) +
                    listOf(Col(1f, 1f, 1f), col("#ff5a8a"), col("#020814")),
            )
    }

    private fun buildWater(seed: Int) {
        for (y in 0 until NROWS) {
            val h = y / (NROWS - 1f)
            val c =
                if (h <
                    0.35f
                ) {
                    surface.scale(0.75f).mix(water, h / 0.35f)
                } else {
                    water.mix(abyss, clamp01((h - 0.35f) / 0.5f))
                }
            for (x in 0 until NCOLS) {
                val i = y * NCOLS + x
                val m = 0.85f + 0.3f * lat.fbm(x * 0.05f, y * 0.12f + 20f, 3)
                base.r[i] = c.r * m
                base.g[i] = c.g * m
                base.b[i] = c.b * m
                base.cov[i] = 0.55f + 0.45f * (1f - h)
            }
        }
        // Far towers, pale in the blue haze.
        val rng = nrng(seed, 305)
        var x = 0
        while (x < NCOLS) {
            val w = 4 + rng.int(6)
            val top = 16 + rng.int(14)
            for (xx in x until min(NCOLS, x + w)) {
                for (y in top until FLOOR) {
                    val round = if (y == top && (xx == x || xx == x + w - 1)) true else false
                    if (round) continue
                    val i = y * NCOLS + xx
                    val c = Col(base.r[i], base.g[i], base.b[i]).mix(col("#2a6a8a"), 0.35f)
                    val lit = (y - top) % 3 == 1 && (xx - x) % 2 == 1 && rng.float() < 0.25f
                    base.put(xx, y, if (lit) c.mix(warm, 0.35f) else c, 2f)
                }
            }
            x += w + 2 + rng.int(6)
        }
    }

    private fun buildCity(seed: Int) {
        val rng = nrng(seed, 307)
        // Two tall towers with bulb tops between the domes.
        for ((tx, top) in listOf(Pair(64f, 7), Pair(132f, 11))) {
            for (y in top until FLOOR + 1) {
                val half = if (y < top + 5) 4.5f - abs(y - top - 2.5f) * 0.6f + 1.5f else 2.6f + (y - top) * 0.03f
                for (x in (tx - half).toInt()..(tx + half).toInt()) {
                    val a = (x + 0.5f - tx) / half
                    if (abs(a) > 1f) continue
                    var c = col("#0a2a3a").mix(tower, 0.35f + 0.55f * clamp01(0.6f - a * 0.6f))
                    if (y > top + 5 && (y - top) % 3 == 0 && abs(a) < 0.7f && rng.float() < 0.75f) c = warm.scale(0.85f)
                    if (y < top + 5 && abs(a) < 0.5f && y == top + 2) c = glass
                    base.put(x, y, c, 2f)
                }
            }
            base.glow(tx, top + 2f, 6f, glass, 0.3f)
            base.put(tx.toInt(), top - 1, tower, 2f, '│')
            base.put(tx.toInt(), top - 2, tower, 2f, '│')
        }
        // Domes: buildings inside, tinted glass, a bright rim.
        for (d in domes) {
            val ry = d.r / ROW_U
            var bx = (d.x - d.r).toInt() + 2
            while (bx < d.x + d.r - 2) {
                val w = 3 + rng.int(5)
                val inside = sqrt(max(0f, 1f - ((bx + w / 2f - d.x) / d.r).let { it * it }))
                val h = (ry * inside * (0.4f + 0.5f * rng.float())).toInt()
                for (x in bx until bx + w) {
                    for (y in (d.y - h).toInt() until d.y.toInt()) {
                        val win = (y + x) % 2 == 0 && x != bx && x != bx + w - 1 && (y % 2 == 0) && rng.float() < 0.7f
                        val c = if (win) warm else col("#0e2a3e").mix(tower, 0.25f + 0.2f * (x - bx) / w)
                        base.put(x, y, c, 2f)
                    }
                }
                bx += w + 1
            }
            for (y in (d.y - ry - 1).toInt()..d.y.toInt()) {
                for (x in (d.x - d.r - 1).toInt()..(d.x + d.r + 1).toInt()) {
                    if (x < 0 || x >= NCOLS) continue
                    val dd = dist(x + 0.5f, y + 0.5f, d.x, d.y) / d.r
                    if (dd > 1f) continue
                    val i = y * NCOLS + x
                    if (dd > 0.94f) {
                        base.put(x, y, glass.scale(0.75f + 0.25f * clamp01((d.x - x) / d.r + 0.5f)), 2f)
                    } else {
                        // The glass tints what is behind it and catches a soft sheen up and to the left.
                        val sheen = gauss(dd, 0.82f, 0.06f) * clamp01((d.x - x) / d.r + 0.3f) * clamp01((d.y - y) / ry)
                        base.haze(i, glass, 0.12f + 0.45f * sheen)
                    }
                }
            }
            base.glow(d.x, d.y - ry * 0.4f, d.r * 0.6f, warm, 0.12f)
        }
        // Tubes between the domes, with lit rings.
        for ((x0, x1) in listOf(Pair(53, 61), Pair(68, 77), Pair(115, 129), Pair(135, 140))) {
            for (x in x0..x1) {
                base.put(x, FLOOR - 2, glass.scale(0.7f), 2f, '▄')
                base.put(x, FLOOR - 1, if (x % 3 == 0) warm else col("#2a6a8a"), 2f)
                base.put(x, FLOOR, glass.scale(0.5f), 2f, '▀')
            }
        }
    }

    private fun buildFloor(seed: Int) {
        for (y in FLOOR until NROWS) {
            for (x in 0 until NCOLS) {
                val dune = FLOOR + 0.6f + 1.4f * lat.fbm(x * 0.03f, 3f, 3)
                if (y + 1f < dune) continue
                val i = y * NCOLS + x
                if (base.cov[i] > 1.4f && y <= FLOOR && base.ch[i] != NONE) continue
                val n = lat.at(x * 0.4f, y * 1.2f)
                val k = 0.42f + 0.12f * n + 0.05f * (y - FLOOR) + 0.08f * sin(x * 0.5f + y * 1.7f + n * 3f)
                base.put(x, y, col("#2a2418").mix(sand, clamp01(k)), 2f, if (y + 0.5f < dune) '▄' else NONE)
            }
        }
        // Rocks and a few coral fans.
        val rng = nrng(seed, 309)
        for (k in 0 until 9) {
            val rx = rng.int(NCOLS).toFloat()
            val rr = 2f + rng.float() * 4f
            base.ellipse(rx, NROWS - 3f - rng.float() * 4f, rr, rr / ROW_U + 0.6f) { a, b ->
                col("#1a2030").mix(col("#5a6a7a"), clamp01(0.5f - a * 0.3f - b * 0.5f))
            }
        }
        for (k in 0 until 7) {
            val cx = rng.int(NCOLS)
            val c = if (k % 2 == 0) col("#ff5a8a") else col("#ff8a3a")
            base.text(cx - 2, FLOOR, "\\ | /", c)
            base.text(cx - 1, FLOOR + 1, "\\|/", c.scale(0.85f))
            base.text(cx, FLOOR + 2, "|", c.scale(0.7f))
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val u = t / LOOP
        frame.copyFrom(base)
        rays(u)
        caustics(u)
        kelp(u)
        fish(u, 0)
        sub(u)
        fish(u, 1)
        fish(u, 2)
        bubbles(u)
        frame.output(grid, palette)
    }

    /** Slanted shafts of light from the surface, breathing in and out. */
    private fun rays(u: Float) {
        for (k in 0 until 6) {
            val x0 = 20f + k * 32f + 8f * sin(NTAU * (u + k * 0.17f))
            val w = 5f + 2f * sin(NTAU * (2f * u + k * 0.31f))
            val str = 0.3f + 0.12f * sin(NTAU * (u * 2f + k * 0.23f))
            for (y in 0 until FLOOR) {
                val cx = x0 + y * 0.9f
                val fade = 1f - y / (FLOOR + 6f)
                for (x in (cx - w).toInt()..(cx + w).toInt()) {
                    if (x < 0 || x >= NCOLS) continue
                    val a = 1f - abs(x + 0.5f - cx) / w
                    frame.light(y * NCOLS + x, surface, str * a * fade)
                }
            }
        }
        // The rippling underside of the surface.
        for (x in 0 until NCOLS) {
            val s = sin(x * 0.21f + NTAU * u * 3f) + sin(x * 0.13f - NTAU * u * 2f)
            frame.put(x, 0, surface.mix(col("#a8f0ff"), clamp01(0.5f + s * 0.3f)), 2f, if (s > 0.6f) '▀' else NONE)
            if (s > 1.1f) frame.put(x, 1, col("#a8f0ff"), 1f, '~')
        }
    }

    /** Dancing caustic light on the sand and the dome tops. */
    private fun caustics(u: Float) {
        for (y in FLOOR - 16 until NROWS) {
            for (x in 0 until NCOLS) {
                val i = y * NCOLS + x
                if (frame.cov[i] < 1.4f) continue
                val onFloor = y >= FLOOR
                val onDome = !onFloor && frame.b[i] > 0.6f && frame.g[i] > 0.6f
                if (!onFloor && !onDome) continue
                val a = sin(x * 0.35f + NTAU * u * 2f + sin(y * 0.9f + NTAU * u))
                val b = sin(x * 0.22f - y * 0.7f - NTAU * u * 3f)
                val v = 1f - abs(a + b) * 0.9f
                if (v > 0.45f) frame.light(i, col("#a8f0ff"), (v - 0.45f) * (if (onFloor) 0.5f else 0.35f))
            }
        }
    }

    private fun kelp(u: Float) {
        for (k in 0 until KELP) {
            val h = kelpH[k]
            for (s in 0 until h.toInt()) {
                val q = s / h
                val sway = 2.4f * q * q * sin(NTAU * (u * 2f + kelpP[k]) - q * 2f)
                val x = kelpX[k] + sway
                val y = NROWS - 2f - s
                val c = col("#1e5a2a").mix(col("#7ac05a"), 0.2f + 0.5f * q)
                frame.dot(x, y, c, 2f)
                if (s % 3 == 1) frame.dot(x + 1f, y, c.scale(0.8f), 2f, '▀')
                if (s % 3 == 2) frame.dot(x - 1f, y, c.scale(0.7f), 2f, '▄')
            }
        }
    }

    /** Three lanes of fish traffic, each school swimming its own way at its own pace. */
    private fun fish(
        u: Float,
        lane: Int,
    ) {
        val rows = floatArrayOf(9f, 22f, 31f)
        val laps = intArrayOf(1, -2, 1)
        for (f in 0 until FISH) {
            if (fishLane[f] != lane) continue
            val p = fract(fishOff[f] * 0.35f + u * laps[lane])
            val right = laps[lane] > 0
            val x = if (right) -6f + p * (NCOLS + 12f) else NCOLS + 6f - p * (NCOLS + 12f)
            val y = rows[lane] + (fishOff[f] - 0.5f) * 7f + sin(NTAU * (u * 4f + fishOff[f])) * 0.6f
            val c = fishCols[fishCol[f]]
            val s = if (right) "><>" else "<><"
            val big = lane == 2 && f % 3 == 0
            frame.text(x.toInt(), y.toInt(), if (big) (if (right) ">=●>" else "<●=<") else s, c)
        }
    }

    /** The yellow submarine crossing once per loop with its headlight cone and turning propeller. */
    private fun sub(u: Float) {
        val x = -30f + u * (NCOLS + 60f)
        val y = 15f + 1.2f * sin(NTAU * u * 2f)
        val len = 15f
        // Headlight cone ahead.
        for (k in 1..42) {
            val spread = 0.5f + k * 0.16f
            val cx = x + len + 2f + k
            for (dy in -spread.toInt()..spread.toInt()) {
                val yy = (y + 0.5f + dy * 1f).toInt()
                if (cx < 0f || cx >= NCOLS || yy < 0 || yy >= NROWS) continue
                frame.light(
                    yy * NCOLS + cx.toInt(),
                    col("#fff0c8"),
                    0.3f * (1f - k / 43f) * (1f - abs(dy) / (spread + 1f)),
                )
            }
        }
        frame.ellipse(x, y + 0.5f, len, 2.6f) { a, b ->
            hull.scale(0.55f + 0.45f * clamp01(0.7f - b * 0.6f + a * 0.1f))
        }
        // Conning tower and periscope.
        for (xx in (x - 3f).toInt()..(x + 4f).toInt()) {
            frame.put(xx, (y - 2.5f).toInt(), hull.scale(0.8f), 2f, if (xx == (x - 3f).toInt()) '▟' else NONE)
        }
        frame.put((x + 2f).toInt(), (y - 3.5f).toInt(), hull.scale(0.6f), 2f, '│')
        frame.put((x + 3f).toInt(), (y - 4.5f).toInt(), hull.scale(0.6f), 2f, '▄')
        frame.put((x + 2f).toInt(), (y - 4.5f).toInt(), hull.scale(0.6f), 2f, '▄')
        for (k in 0 until 4) {
            val px = (x - 8f + k * 5f).toInt()
            frame.put(px, (y + 0.5f).toInt(), col("#fff0c8"), 2f, '●')
        }
        frame.glow(x + len, y + 0.5f, 3f, col("#fff0c8"), 0.6f)
        // Propeller.
        val spin = fract(u * 40f)
        val px = (x - len - 1f).toInt()
        frame.put(px, (y + 0.5f).toInt(), hull.scale(0.5f), 2f, '█')
        frame.put(px - 1, (y + 0.5f).toInt() - 1, tower, 2f, if (spin < 0.5f) '╱' else '│')
        frame.put(px - 1, (y + 0.5f).toInt() + 1, tower, 2f, if (spin < 0.5f) '╱' else '│')
        // A wake of small bubbles behind.
        for (k in 0 until 8) {
            val bx = px - 3f - k * 2.5f
            val by = y + 0.5f + sin(k * 1.7f + NTAU * u * 6f) * 0.8f - k * 0.15f
            frame.dot(bx, by, col("#a8f0ff").scale(1f - k * 0.08f), 1f, if (k < 3) 'o' else '·')
        }
    }

    private fun bubbles(u: Float) {
        for (b in 0 until BUBBLES) {
            val p = fract(bubP[b] + u * bubR[b])
            val y = FLOOR - 1f - p * (FLOOR + 2f)
            val x = bubX[b] + sin(NTAU * (p * 3f + bubP[b])) * 1.2f
            if (y < 1f) continue
            frame.dot(
                x,
                y,
                col("#a8f0ff").scale(0.6f + 0.4f * p),
                1f,
                if (p >
                    0.6f
                ) {
                    'O'
                } else if (p > 0.25f) {
                    'o'
                } else {
                    '·'
                },
            )
        }
    }
}

/**
 * A city under the sea. Glass domes full of lit buildings sit on the sand between two tall towers and tubes, a
 * yellow submarine crosses with its headlight cutting the blue, schools of fish swim in three lanes of traffic,
 * kelp sways, bubbles rise from the vents, shafts of light slant down from the rippling surface and caustics dance
 * on the sea floor and the glass. The seed sets the buildings, the bubbles, the kelp and the fish.
 */
public fun underwaterCityPiece(seed: Int = 0): AsciiPiece {
    val scene = Deep(seed)
    return asciiPiece(
        name = "underwater city",
        ground = Rgb.fromHex("#020a18"),
        category = "scenes",
        cols = NCOLS,
        rows = NROWS,
        fps = 15,
        still = 7f,
        loop = LOOP,
        note = "glass domes and towers on the sea floor, a submarine and schools of fish passing, light from above",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * Glass domes and towers on the sea floor with a submarine, fish traffic, bubbles and caustic light from above.
 *
 * @param seed sets the buildings, the bubbles, the kelp and the fish.
 */
@Composable
public fun AsciiUnderwaterCity(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Glass domes on the sea floor, a yellow submarine and schools of fish passing by",
) {
    val piece = remember(seed) { underwaterCityPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
