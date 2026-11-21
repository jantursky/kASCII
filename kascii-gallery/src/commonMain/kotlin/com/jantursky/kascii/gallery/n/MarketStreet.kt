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
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val LOOP = 12f
private const val VPX = 96f
private const val VPY = 21f
private const val STREET = 18f
private const val WALL = 96f
private const val FAR_Z = 10f
private const val PEOPLE = 16
private const val STEAM = 34

/** Something drawn at a depth: a stall, a lantern string, a person or the food cart. */
private class Thing(
    val z: Float,
    val kind: Int,
    val index: Int,
)

private class Market(
    seed: Int,
) {
    private val lat = NLattice(seed, 601)
    private val base = NFrame()
    private val frame = NFrame()
    private val palette: NPalette
    private val stars = NStars(seed, 603, 50, 14)
    private val stallX = floatArrayOf(-62f, -64f, -66f, 62f, 64f, 66f)
    private val stallZ = floatArrayOf(1.35f, 2.3f, 3.7f, 1.5f, 2.6f, 4.3f)
    private val stringZ = floatArrayOf(1.9f, 2.9f, 4.4f, 6.6f)
    private val pZ = FloatArray(PEOPLE)
    private val pLaps = IntArray(PEOPLE)
    private val pOff = FloatArray(PEOPLE)
    private val pH = FloatArray(PEOPLE)
    private val steamOff = FloatArray(STEAM)
    private val things: List<Thing>
    private val lanternX = ArrayList<Float>()
    private val lanternY = ArrayList<Float>()
    private val lanternZ = ArrayList<Float>()
    private val night = col("#06061a")
    private val plaster = col("#7a6450")
    private val timber = col("#2a1a14")
    private val stone = col("#3a3442")
    private val warm = col("#ffb24a")
    private val red = col("#d83a3a")
    private val green = col("#3a9a5a")
    private val blue = col("#3a6ad8")
    private val cream = col("#f0e0c0")
    private val shadow = col("#07060c")
    private val steam = col("#c8c8d8")
    private val awnings = listOf(Pair(red, cream), Pair(green, cream), Pair(blue, cream), Pair(red, col("#ffd060")))

    init {
        val rng = nrng(seed, 605)
        for (p in 0 until PEOPLE) {
            pZ[p] = 1.15f + rng.float() * rng.float() * 4.5f
            pLaps[p] = (1 + rng.int(2)) * (if (rng.float() < 0.5f) 1 else -1)
            pOff[p] = rng.float()
            pH[p] = 15f + rng.float() * 3f
        }
        for (s in 0 until STEAM) steamOff[s] = rng.float()
        val list = ArrayList<Thing>()
        for (k in stallX.indices) list.add(Thing(stallZ[k], 0, k))
        for (k in stringZ.indices) list.add(Thing(stringZ[k], 1, k))
        for (p in 0 until PEOPLE) list.add(Thing(pZ[p], 2, p))
        list.add(Thing(1.75f, 3, 0))
        things = list.sortedByDescending { it.z }
        buildSky()
        buildFacades(seed)
        buildStreet()
        palette =
            NPalette(
                ramp(8, night, col("#141a40"), col("#2a2a5a")) +
                    ramp(10, shadow, timber, col("#4a3426"), plaster, col("#e8c898")) +
                    ramp(8, col("#120e18"), stone, col("#6a6070"), col("#b0a0a0")) +
                    tones(warm, 9) +
                    tones(red, 6) +
                    tones(green, 6) +
                    tones(blue, 6) +
                    ramp(5, col("#8a7a60"), cream, Col(1f, 1f, 1f)) +
                    tones(col("#ffd060"), 5) +
                    ramp(5, col("#3a3a4a"), steam, Col(1f, 1f, 1f)) +
                    listOf(col("#ff7a3a"), col("#8a3aff")),
            )
    }

    private fun sx(
        xw: Float,
        z: Float,
    ): Float = VPX + xw / z

    private fun sy(
        yw: Float,
        z: Float,
    ): Float = VPY + yw / z

    private fun buildSky() {
        for (y in 0 until NROWS) {
            val c = night.mix(col("#2a2a5a"), clamp01(y / 22f))
            for (x in 0 until NCOLS) {
                val i = y * NCOLS + x
                base.r[i] = c.r
                base.g[i] = c.g
                base.b[i] = c.b
                base.cov[i] = 0.15f + 0.4f * clamp01(y / 22f)
            }
        }
        base.glow(VPX, VPY, 30f, col("#ff7a3a"), 0.2f)
    }

    /** Roof line (world rows, negative is up) of the houses at depth [z]: gables of several heights. */
    private fun roof(z: Float): Float {
        val s = (z - 1f) / 1.5f
        val k = floor(s).toInt()
        val f = s - k
        val h = -21f + ((k * 7) % 4) * 2.5f
        return h - 5f * (1f - abs(2f * f - 1f))
    }

    private fun buildFacades(seed: Int) {
        val rng = nrng(seed, 607)
        val lit = BooleanArray(256) { rng.float() < 0.7f }
        for (y in 0 until NROWS) {
            for (x in 0 until NCOLS) {
                val dx = x + 0.5f - VPX
                if (abs(dx) < WALL / FAR_Z) continue
                val z = WALL / abs(dx)
                val yw = (y + 0.5f - VPY) * z
                val top = roof(z)
                if (yw < top - 1.2f || yw > STREET) continue
                val s = (z - 1f) / 1.5f
                val house = floor(s).toInt() + (if (dx > 0f) 100 else 0)
                val f = s - floor(s)
                val fog = clamp01((z - 1f) / (FAR_Z - 1f))
                val glowNear = clamp01(1f - abs(yw + 12f) / 14f) * 0.35f
                val wallCol = plaster.mix(col("#a07a50"), ((house * 5) % 3) / 3f).scale(0.42f + glowNear)
                var c = wallCol
                var glyph = NONE
                if (yw < top) {
                    c = col("#3a2a3a")
                    glyph = '▄'
                } else if (abs(yw - 1.5f) < 0.9f || abs(yw + 8f) < 0.9f || f < 0.05f || abs(f - 0.5f) < 0.03f) {
                    c = timber
                } else {
                    val upper = yw < 1f
                    val wy = if (upper) yw > -6.5f && yw < -1.5f else yw > 3.5f && yw < 8.5f
                    val wf = (f > 0.14f && f < 0.34f) || (f > 0.64f && f < 0.84f)
                    val key = (house * 4 + (if (upper) 0 else 2) + (if (f > 0.5f) 1 else 0)) and 255
                    if (wy && wf) {
                        c = if (lit[key]) warm.scale(0.95f) else col("#141020")
                        if (lit[key] && (abs(yw + 4f) < 0.5f || abs(yw - 6f) < 0.5f)) c = timber
                    } else if (yw < 1f) {
                        // Half-timbering: crossed braces in the plaster panels of the upper floors.
                        val fp = fract(f * 4f)
                        val p0 = if (yw < -8f) top else -8f
                        val p1 = if (yw < -8f) -8f else 1.5f
                        val yp = (yw - p0) / (p1 - p0)
                        if (abs(fp - yp) < 0.1f || abs(fp + yp - 1f) < 0.1f) c = timber.scale(1.3f)
                    } else if (yw > 10f) {
                        // Ground floor shop fronts, dark with a lit doorway now and then.
                        c = if (f > 0.4f && f < 0.6f && lit[(key + 37) and 255]) warm.scale(0.7f) else col("#1a1420")
                    }
                }
                c = c.mix(night, fog * 0.55f)
                base.put(x, y, c, 2f, glyph)
            }
        }
        // The end of the street: a gatehouse with a lit arch.
        val gl = sx(-WALL, FAR_Z).toInt()
        val gr = sx(WALL, FAR_Z).toInt()
        for (y in sy(-24f, FAR_Z).toInt()..sy(STREET, FAR_Z).toInt()) {
            for (x in gl..gr) {
                val arch = abs(x + 0.5f - VPX) < 3f && y > sy(4f, FAR_Z)
                base.put(x, y, if (arch) warm.scale(0.8f) else plaster.scale(0.35f).mix(night, 0.4f), 2f)
            }
        }
        base.glow(VPX, sy(12f, FAR_Z), 8f, warm, 0.3f)
    }

    private fun buildStreet() {
        for (y in VPY.toInt() until NROWS) {
            val rel = y + 0.5f - VPY
            if (rel <= 0f) continue
            val z = STREET / rel
            if (z > FAR_Z) continue
            for (x in 0 until NCOLS) {
                val xw = (x + 0.5f - VPX) * z
                if (abs(xw) > WALL) continue
                val row = floor(z * 2.2f)
                val off = if (row.toInt() % 2 == 0) 0f else 0.5f
                val cx = fract(xw / 7f + off)
                val cz = fract(z * 2.2f)
                val joint = cx < 0.12f || cz < 0.14f
                val n = lat.at(xw * 0.2f, z * 3f)
                val fog = clamp01((z - 1f) / (FAR_Z - 1f))
                val c = (if (joint) shadow else stone.scale(0.7f + 0.4f * n)).mix(night, fog * 0.4f)
                base.put(x, y, c, if (joint) 1.8f else 2f)
            }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val u = t / LOOP
        frame.copyFrom(base)
        stars.draw(frame, u, 0.8f) { y -> 1f - y / 14f }
        collectLanterns(u)
        reflections(u)
        for (th in things) {
            when (th.kind) {
                0 -> stall(th.index, u)
                1 -> lanternString(th.index, u)
                2 -> person(th.index, u)
                else -> cart(u)
            }
        }
        frame.output(grid, palette)
    }

    /** Lantern positions for this frame, each swinging on its string. */
    private fun collectLanterns(u: Float) {
        lanternX.clear()
        lanternY.clear()
        lanternZ.clear()
        for (k in stringZ.indices) {
            val z = stringZ[k]
            for (j in 0 until 9) {
                val p = (j + 0.5f) / 9f
                val xw = -WALL + p * 2f * WALL
                val yw = ropeY(-21f, -21f, 7f, p) + 2.5f + 1.2f * cos01(NTAU * (u * 2f + j * 0.13f + k * 0.3f))
                val swing = 2.2f * sin(NTAU * (u * 2f + j * 0.13f + k * 0.3f))
                lanternX.add(sx(xw + swing, z))
                lanternY.add(sy(yw, z))
                lanternZ.add(z)
            }
        }
    }

    private fun cos01(a: Float): Float = 1f - abs(sin(a * 0.5f))

    /** Wet cobbles: each lantern throws a shimmering streak of light down the street below it. */
    private fun reflections(u: Float) {
        for (k in lanternX.indices) {
            val z = lanternZ[k]
            val gy = sy(STREET, z)
            val len = (gy - lanternY[k]) * 0.9f
            for (s in 1..len.toInt()) {
                val y = (gy + s).toInt()
                if (y >= NROWS) break
                val w = sin(NTAU * u * 3f + s * 1.3f + k) * 0.8f
                for (x in (lanternX[k] + w - 1f).toInt()..(lanternX[k] + w + 1f).toInt()) {
                    if (x < 0 || x >= NCOLS) continue
                    val i = y * NCOLS + x
                    val glint = 0.6f + 0.4f * sin(NTAU * u * 4f + x * 2.1f + s * 0.7f)
                    frame.light(i, warm.mix(lanternCol(k), 0.4f), 0.75f * glint * (1f - s / (len + 1f)) / z)
                }
            }
        }
    }

    private fun lanternCol(k: Int): Col =
        when (k % 3) {
            0 -> red
            1 -> col("#ff7a3a")
            else -> col("#ffd060")
        }

    private fun lanternString(
        k: Int,
        u: Float,
    ) {
        val z = stringZ[k]
        val x0 = sx(-WALL, z)
        val x1 = sx(WALL, z)
        val c = col("#5a4a3a").mix(night, clamp01((z - 1f) / 9f) * 0.5f)
        for (x in max(0, x0.toInt())..min(NCOLS - 1, x1.toInt())) {
            val p = (x + 0.5f - x0) / (x1 - x0)
            val y = sy(ropeY(-21f, -21f, 7f, p), z)
            frame.dot(x.toFloat(), y, c, 1f, if (fract(y) < 0.5f) '-' else '_')
        }
        for (j in 0 until 9) {
            val idx = k * 9 + j
            val lx = lanternX[idx]
            val ly = lanternY[idx]
            val lc = lanternCol(idx)
            val ry = 1.3f / z + 0.2f
            val rx = ry * ROW_U * 0.85f
            frame.glow(lx, ly, 3f + 6f / z, lc.mix(warm, 0.4f), 0.35f / z + 0.1f)
            val hot = lc.mix(Col(1f, 1f, 1f), 0.25f)
            val xi = lx.toInt()
            val yi = ly.toInt()
            if (ry < 0.6f) {
                frame.dot(lx, ly, hot, 2f, '●')
            } else if (ry < 0.75f) {
                frame.put(xi - 1, yi, lc.scale(0.8f), 2f, '▐')
                frame.put(xi, yi, hot, 2f)
                frame.put(xi + 1, yi, lc.scale(0.8f), 2f, '▌')
            } else {
                // A round paper lantern: a bright belly between half-block caps.
                for (x in -1..1) frame.put(xi + x, yi - 1, lc.scale(0.85f), 2f, '▄')
                for (x in -2..2) frame.put(xi + x, yi, if (abs(x) < 2) hot else lc.scale(0.8f), 2f)
                for (x in -1..1) frame.put(xi + x, yi + 1, lc.scale(0.7f), 2f, '▀')
            }
        }
    }

    private fun stall(
        k: Int,
        u: Float,
    ) {
        val z = stallZ[k]
        val cx = stallX[k]
        val fog = clamp01((z - 1f) / 9f) * 0.5f
        val (a1, a2) = awnings[k % awnings.size]
        val top = sy(-7f, z)
        val edge = sy(0f, z)
        // Striped awning, widening toward its scalloped front edge.
        for (y in top.toInt()..edge.toInt()) {
            val q = clamp01((y + 0.5f - top) / max(1f, edge - top))
            val half = 18f + 6f * q
            for (x in sx(cx - half, z).toInt()..sx(cx + half, z).toInt()) {
                val xw = (x + 0.5f - VPX) * z - cx
                val stripe = floor((xw + 24f) / 6f).toInt() % 2 == 0
                var c = (if (stripe) a1 else a2).scale(0.55f + 0.45f * q)
                c = c.mix(night, fog)
                val last = y == edge.toInt()
                frame.put(x, y, c, 2f, if (last) (if (stripe) '▀' else '▄') else NONE)
            }
        }
        // Posts, a dark booth with a lamp, and a counter of goods.
        val l = sx(cx - 22f, z)
        val r = sx(cx + 22f, z)
        val counter = sy(9f, z)
        val ground = sy(STREET, z)
        for (y in (edge + 1).toInt()..ground.toInt()) {
            for (x in l.toInt()..r.toInt()) {
                val post = x == l.toInt() || x == r.toInt()
                val c =
                    if (post) {
                        timber.scale(1.4f)
                    } else if (y < counter) {
                        col("#2a1810").mix(
                            warm,
                            0.55f * (1f - 0.7f * (y - edge) / (counter - edge)) *
                                (1f - 0.6f * abs(x + 0.5f - sx(cx, z)) / (r - l + 1f) * 2f),
                        )
                    } else if (y.toFloat() < counter + 1f) {
                        col("#8a5a32")
                    } else {
                        col("#3a2418")
                    }
                frame.put(x, y, c.mix(night, fog), 2f)
            }
        }
        // Goods on the counter: rows of fruit and jars.
        val goods = listOf(red, col("#ffd060"), green, col("#ff7a3a"))
        for (x in (l + 1f).toInt() until r.toInt()) {
            val g = goods[(x * 7 + k) % goods.size]
            frame.put(x, (counter - 0.5f).toInt(), g.mix(night, fog), 2f, if (z < 2.5f) '●' else '•')
        }
        val lampX = sx(cx, z)
        val lampY = sy(2.5f, z)
        val flick = 0.9f + 0.1f * sin(NTAU * (u * 5f + k * 0.21f))
        frame.glow(lampX, lampY, 8f / z + 2f, warm, 0.45f * flick)
        frame.dot(lampX, lampY, col("#fff0c0"), 2f, '●')
    }

    private fun person(
        p: Int,
        u: Float,
    ) {
        val z = pZ[p]
        val pos = fract(pOff[p] + u * pLaps[p])
        val xw = -70f + pos * 140f
        val feet = sy(STREET, z)
        val head = sy(STREET - pH[p], z)
        val h = feet - head
        val cx = sx(xw, z)
        val w = 4.2f / z
        val walk = NTAU * (pos * 140f / 9f)
        val rim = warm.scale(0.7f)
        val body = col("#1c1626")
        for (y in head.toInt()..feet.toInt()) {
            val q = (y + 0.5f - head) / h
            var x0: Float
            var x1: Float
            if (q < 0.15f) {
                val r = w * 0.32f * (1f - ((q - 0.075f) / 0.075f).let { it * it }).coerceAtLeast(0.2f)
                x0 = cx - r
                x1 = cx + r
            } else if (q < 0.6f) {
                val half = w * (0.42f + 0.5f * smooth(0.15f, 0.25f, q) - 0.1f * smooth(0.3f, 0.6f, q))
                x0 = cx - half
                x1 = cx + half
            } else {
                val stride = sin(walk) * w * 0.5f * (q - 0.6f) / 0.4f
                for (leg in -1..1 step 2) {
                    val lx = cx + leg * stride + leg * w * 0.18f
                    for (x in (lx - w * 0.14f - 0.3f).toInt()..(lx + w * 0.14f).toInt()) {
                        frame.put(
                            x,
                            y,
                            body.scale(0.7f),
                            2f,
                        )
                    }
                }
                continue
            }
            for (x in x0.toInt()..x1.toInt()) {
                val edge = x == x0.toInt() || x == x1.toInt()
                frame.put(x, y, if (edge) body.mix(rim, 0.45f) else body, 2f)
            }
        }
    }

    /** A food cart with a glowing grill and a plume of steam curling up. */
    private fun cart(u: Float) {
        val z = 1.75f
        val cx = -24f
        val l = sx(cx - 12f, z)
        val r = sx(cx + 12f, z)
        val top = sy(6f, z)
        val bottom = sy(14f, z)
        for (y in top.toInt()..bottom.toInt()) {
            for (x in l.toInt()..r.toInt()) {
                val c =
                    if (y ==
                        top.toInt()
                    ) {
                        col("#ff7a3a").mix(warm, 0.5f)
                    } else {
                        col("#6a3a22").scale(0.8f + 0.2f * ((x % 3) / 2f))
                    }
                frame.put(x, y, c, 2f, if (y == top.toInt()) '▄' else NONE)
            }
        }
        frame.glow(sx(cx, z), top, 9f, col("#ff7a3a"), 0.45f)
        // Wheels.
        for (wx in listOf(l + 2f, r - 3f)) {
            frame.text(wx.toInt() - 1, bottom.toInt() + 1, "(●)", col("#2a1a14"))
        }
        // Chimney and steam.
        val chx = sx(cx + 7f, z)
        for (y in sy(-1f, z).toInt() until top.toInt()) frame.put(chx.toInt(), y, col("#3a3442"), 2f)
        val sy0 = sy(-1f, z)
        for (s in 0 until STEAM) {
            val p = fract(steamOff[s] + u * 2f)
            val y = sy0 - p * 16f
            val x = chx + p * 6f + sin(NTAU * (p * 1.5f + steamOff[s])) * (1f + p * 3f)
            val v = 1f - p
            frame.dot(
                x,
                y,
                steam.scale(0.5f + 0.5f * v),
                1f,
                if (v >
                    0.7f
                ) {
                    '●'
                } else if (v > 0.4f) {
                    '•'
                } else if (v > 0.2f) {
                    'o'
                } else {
                    '·'
                },
            )
        }
    }
}

/**
 * A night market street in an old town. Timber-framed houses lean in from both sides toward a lit gatehouse, striped
 * stalls glow under their lamps, strings of lanterns sag across the street and swing in the breeze, silhouettes of
 * the crowd walk past at several depths, steam curls from a food cart's chimney and the wet cobbles reflect every
 * lantern in shimmering streaks. The seed sets the lit windows, the crowd and the steam.
 */
public fun marketStreetPiece(seed: Int = 0): AsciiPiece {
    val scene = Market(seed)
    return asciiPiece(
        name = "market street",
        ground = Rgb.fromHex("#05050f"),
        category = "scenes",
        cols = NCOLS,
        rows = NROWS,
        fps = 15,
        still = 3f,
        loop = LOOP,
        note = "a night market street with striped stalls, swinging lantern strings, a walking crowd and wet cobbles",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A night market street with striped stalls, swinging lanterns, crowd silhouettes, a steaming food cart and wet cobbles.
 *
 * @param seed sets the lit windows, the crowd and the steam.
 */
@Composable
public fun AsciiMarketStreet(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A night market street with striped stalls, swinging lanterns and a walking crowd",
) {
    val piece = remember(seed) { marketStreetPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
