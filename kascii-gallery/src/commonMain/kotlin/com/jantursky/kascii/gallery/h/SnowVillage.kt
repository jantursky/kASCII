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
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 20f
private const val MOON_X = 28f
private const val MOON_Y = 7f
private const val LAMP_X = 99
private const val LAMP_TOP = 17
private const val LAMP_FOOT = 46
private const val STARS = 90
private const val FLAKES = 260
private const val PUFFS = 14

/** A cottage: left column, the row its walls stand on, width, wall height and roof height in rows. */
private class House(
    val x: Int,
    val base: Int,
    val w: Int,
    val wallH: Int,
    val roofH: Int,
    val chimney: Int,
)

private class Village(
    seed: Int,
) {
    private val lat = HLattice(seed, 41)
    private val back = HFrame()
    private val frame = HFrame()
    private val palette: HPalette
    private val night = hcol("#060a1e")
    private val skyLow = hcol("#1e2c5a")
    private val snow = hcol("#dfe8ff")
    private val snowShade = hcol("#5a6aa0")
    private val wall = hcol("#5a3a2a")
    private val wallLit = hcol("#8a5a3a")
    private val window = hcol("#ffc860")
    private val warm = hcol("#ffa040")
    private val pine = hcol("#0e2a24")
    private val houses =
        listOf(
            House(14, 33, 22, 5, 6, 8),
            House(44, 31, 18, 5, 5, 12),
            House(70, 33, 26, 6, 7, 18),
            House(104, 30, 18, 5, 5, 4),
            House(128, 34, 20, 5, 6, 14),
            House(162, 31, 22, 5, 6, 6),
        )
    private val windows = ArrayList<FloatArray>()
    private val starX = IntArray(STARS)
    private val starY = IntArray(STARS)
    private val starP = FloatArray(STARS)
    private val flakeX = FloatArray(FLAKES)
    private val flakeY = FloatArray(FLAKES)
    private val flakeL = IntArray(FLAKES)
    private val flakeP = FloatArray(FLAKES)

    init {
        buildSky()
        buildHills()
        buildPines(seed)
        for (h in houses) buildHouse(h)
        buildSnowField()
        buildLamp()
        val rng = hrng(seed, 15)
        var s = 0
        while (s < STARS) {
            val x = rng.int(HCOLS)
            val y = rng.int(18)
            if (back.cov[y * HCOLS + x] > 1f || (abs(x - MOON_X) < 14 && abs(y - MOON_Y) < 6)) continue
            starX[s] = x
            starY[s] = y
            starP[s] = rng.float()
            s++
        }
        for (f in 0 until FLAKES) {
            flakeX[f] = rng.float() * HCOLS
            flakeY[f] = rng.float()
            flakeL[f] =
                if (f < FLAKES / 2) {
                    0
                } else if (f < FLAKES * 5 / 6) {
                    1
                } else {
                    2
                }
            flakeP[f] = rng.float()
        }
        palette =
            HPalette(
                hramp(8, night, hcol("#0e1640"), skyLow, hcol("#3a4a80")) +
                    hramp(8, hcol("#1a2450"), snowShade, hcol("#9aaad8"), snow, hcol("#ffffff")) +
                    hramp(5, hcol("#2a1a14"), wall, wallLit, hcol("#c08a5a")) +
                    hramp(6, hcol("#a85a20"), warm, window, hcol("#fff0c0")) +
                    hramp(5, hcol("#04100c"), pine, hcol("#2a4a40")) +
                    hramp(4, hcol("#3a2a3a"), hcol("#6a5a6a"), hcol("#9a8a9a")) +
                    hramp(4, hcol("#f0e8d0"), hcol("#fff8e8")) +
                    hramp(5, hcol("#6a5a7a"), hcol("#b89a90"), hcol("#f0d0a0"), hcol("#fff0d0")) +
                    listOf(hcol("#141418"), hcol("#3a3a44"), hcol("#7a2a2a"), hcol("#c8b8a8"), hcol("#e8d0b0")),
            )
    }

    private fun buildSky() {
        for (y in 0 until HROWS) {
            for (x in 0 until HCOLS) {
                val v = hclamp(y / 26f)
                back.put(x, y, night.mix(skyLow, v), 0.2f + 0.6f * v * v)
            }
        }
        back.glow(MOON_X, MOON_Y, 26f, hcol("#6a80c0"), 0.3f)
        back.ellipse(MOON_X, MOON_Y, 5.5f, 2.5f) { a, b ->
            hcol("#fff8e8").mix(hcol("#c8c8d8"), hclamp(0.3f * lat.at(a * 3f + 9f, b * 3f) - 0.3f * a))
        }
    }

    private fun buildHills() {
        for (x in 0 until HCOLS) {
            val top = 21f - 4f * lat.fbm(x * 0.025f, 4f, 3) + 1.5f * sin(x * 0.05f)
            for (y in top.toInt() until HROWS) {
                val k = hsmooth(top, top + 5f, y.toFloat())
                val lit = lat.fbm(x * 0.08f, y * 0.3f, 2)
                back.put(
                    x,
                    y,
                    hcol("#24325e").mix(hcol("#7a8ac0"), hclamp(0.7f - 0.6f * k + 0.3f * (lit - 0.5f))),
                    hsolid(0.8f, x, y),
                )
            }
        }
    }

    private fun buildPines(seed: Int) {
        val rng = hrng(seed, 16)
        var x = 0f
        while (x < HCOLS) {
            val h = 4f + rng.float() * 6f
            val base = 28f + 1.5f * sin(x * 0.07f)
            pineTree(x, base, h)
            x += 2f + rng.float() * 4f
        }
    }

    private fun pineTree(
        cx: Float,
        base: Float,
        h: Float,
    ) {
        for (y in (base - h).toInt()..base.toInt()) {
            val p = (y - (base - h)) / h
            val half = 0.4f + p * h * 0.45f
            for (x in (cx - half).toInt()..(cx + half).toInt()) {
                if (x < 0 || x >= HCOLS) continue
                // Snow settles on the upper side of each tier of branches.
                val tier = hfract(p * 3f)
                val laden = tier < 0.35f && abs(x + 0.5f - cx) < half * 0.9f
                val side = 0.2f * (x - cx) / max(1f, half)
                back.put(x, y, if (laden) snow.mix(snowShade, 0.35f) else pine.mix(hcol("#2a4a40"), side), 2f)
            }
        }
    }

    private fun buildHouse(h: House) {
        val top = h.base - h.wallH
        // Walls of dark timber, lit a little from the windows.
        for (y in top until h.base) {
            for (x in h.x until h.x + h.w) {
                val plank = (y % 2 == 0)
                val c =
                    wall.mix(wallLit, if (plank) 0.35f else 0.15f).mix(
                        hcol("#2a1a14"),
                        if (x < h.x + 2) 0.4f else 0f,
                    )
                back.put(x, y, c, 2f)
            }
        }
        // Windows and a door.
        val n = max(1, (h.w - 4) / 6)
        for (k in 0 until n) {
            val wx = if (n == 1) h.x + 3 else h.x + 3 + k * (h.w - 10) / (n - 1)
            val wy = top + 1
            for (dy in 0 until 2) {
                for (dx in 0 until 3) back.put(wx + dx, wy + dy, window.mix(warm, if (dy == 1) 0.4f else 0f), 2f)
            }
            back.put(wx + 1, wy, window.mix(hcol("#7a4a20"), 0.45f), 2f)
            windows.add(floatArrayOf(wx + 1.5f, wy + 1f))
        }
        val doorX = h.x + h.w - 5
        for (y in h.base - 3 until h.base) {
            back.put(doorX, y, hcol("#2a1a14"), 2f)
            back.put(doorX + 1, y, hcol("#2a1a14"), 2f)
        }
        // A chimney rising through the roof.
        val cx = h.x + h.chimney
        for (y in top - h.roofH - 1 until top - 1) {
            back.put(cx, y, hcol("#6a5a6a"), 2f)
            back.put(cx + 1, y, hcol("#3a2a3a"), 2f)
        }
        back.put(cx, top - h.roofH - 2, snow, 2f, '▄')
        back.put(cx + 1, top - h.roofH - 2, snow, 2f, '▄')
        // A pitched roof heaped with snow, shadowed on its left slope.
        val mid = h.x + h.w / 2f
        for (r in 0 until h.roofH) {
            val y = top - 1 - r
            val half = h.w / 2f + 2f - r * (h.w / 2f + 1f) / h.roofH
            for (x in (mid - half).toInt()..(mid + half).toInt()) {
                if (x < 0 || x >= HCOLS) continue
                val a = (x + 0.5f - mid) / max(1f, half)
                val edge = abs(a) > 0.86f || r == 0
                val depth = lat.at(x * 0.5f, y * 0.9f)
                val c =
                    if (edge && r == 0) {
                        snowShade.mix(snow, 0.4f)
                    } else {
                        snowShade.mix(snow, hclamp(0.55f + 0.4f * a + 0.15f * depth))
                    }
                back.put(x, y, c, if (r == 0) 2f else hsolid(0.75f + depth * 0.3f, x, y))
            }
        }
        // Eave line: a dark band of roof under the snow.
        for (x in h.x - 1..h.x + h.w) back.put(x, top - 1, hcol("#3a2a3a").mix(snowShade, 0.35f), 2f, '▀')
    }

    private fun buildSnowField() {
        for (x in 0 until HCOLS) {
            val top = 35.5f + 1.2f * sin(x * 0.045f + 1f) + 1.2f * (lat.fbm(x * 0.04f, 12f, 2) - 0.5f)
            for (y in top.toInt() until HROWS) {
                val d = y - top
                val drift = lat.fbm(x * 0.05f, y * 0.35f + 30f, 3)
                val lift = 0.1f * hsmooth(6f, 12f, d) - 0.15f * hsmooth(0f, 1.5f, d)
                val shade = hclamp(0.75f - 0.6f * (drift - 0.5f) + lift)
                back.put(
                    x,
                    y,
                    snowShade.mix(snow, shade).scale(0.82f + 0.12f * shade),
                    hsolid(0.65f + 0.4f * drift, x, y),
                )
            }
        }
        // A trodden path winding to the lamp.
        for (y in 37 until HROWS) {
            val cx = 100f - (y - 37) * 1.6f + 2f * sin(y * 0.5f)
            for (x in (cx - 2f - (y - 37) * 0.3f).toInt()..(cx + 2f + (y - 37) * 0.3f).toInt()) {
                val i = y * HCOLS + x
                if (x in 0 until HCOLS) back.set(i, hcol("#9aaad8").mix(snowShade, 0.35f), 2f)
            }
        }
    }

    private fun buildLamp() {
        for (y in LAMP_TOP + 2 until LAMP_FOOT) back.put(LAMP_X, y, hcol("#141418"), 2f)
        back.put(LAMP_X - 1, LAMP_FOOT - 1, hcol("#141418"), 2f, '▄')
        back.put(LAMP_X + 1, LAMP_FOOT - 1, hcol("#141418"), 2f, '▄')
        for (dx in -1..1) {
            back.put(LAMP_X + dx, LAMP_TOP, hcol("#141418"), 2f, '▄')
            back.put(LAMP_X + dx, LAMP_TOP + 1, window.mix(hcol("#fff0c0"), if (dx == 0) 0.6f else 0f), 2f)
        }
        back.put(LAMP_X, LAMP_TOP - 1, snow, 2f, '▄')
        back.put(LAMP_X - 2, LAMP_TOP + 1, hcol("#141418"), 2f, '▐')
        back.put(LAMP_X + 2, LAMP_TOP + 1, hcol("#141418"), 2f, '▌')
        // The pool of lamplight on the snow.
        for (y in 38 until HROWS) {
            for (x in LAMP_X - 26..LAMP_X + 26) {
                if (x < 0 || x >= HCOLS) continue
                val dx = (x + 0.5f - LAMP_X) / 24f
                val dy = (y + 0.5f - LAMP_FOOT + 1f) / 5f
                val k = hclamp(1f - sqrt(dx * dx + dy * dy))
                if (k > 0f) back.haze(y * HCOLS + x, hcol("#f0c890"), 0.45f * k)
            }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = HTAU * t / LOOP
        frame.copyFrom(back)
        stars(t)
        smoke(t)
        for ((k, w) in windows.withIndex()) {
            val flicker = 0.85f + 0.15f * sin(phi * (2 + k % 3) + k * 1.7f)
            frame.glow(w[0], w[1], 4.5f, warm, 0.22f * flicker)
        }
        frame.glow(LAMP_X + 0.5f, LAMP_TOP + 1.5f, 11f, warm, 0.5f + 0.05f * sin(phi * 5f))
        flakes(t, phi)
        frame.output(grid, palette)
    }

    private fun stars(t: Float) {
        for (s in 0 until STARS) {
            val tw = 0.5f + 0.5f * sin(HTAU * (t / LOOP * 2f + starP[s]))
            if (tw < 0.3f) continue
            frame.put(starX[s], starY[s], hcol("#c8d4f0").mix(hcol("#ffffff"), tw), 1f, if (tw > 0.85f) '+' else '·')
        }
    }

    private fun smoke(t: Float) {
        for ((hk, h) in houses.withIndex()) {
            val x0 = h.x + h.chimney + 1f
            val y0 = (h.base - h.wallH - h.roofH - 2).toFloat()
            for (p in 0 until PUFFS) {
                val u = hfract(t / LOOP * 2f + p / PUFFS.toFloat() + hk * 0.13f)
                val y = y0 - u * 14f
                val x = x0 + u * u * 16f + 1.5f * sin(HTAU * (u * 1.5f + hk * 0.3f) + t / LOOP * HTAU)
                val r = 1.6f + u * 5f
                val a = (1f - u) * 0.9f
                val ry = r / HROW_U
                for (yy in (y - ry).toInt()..(y + ry).toInt()) {
                    for (xx in (x - r).toInt()..(x + r).toInt()) {
                        if (xx < 0 || xx >= HCOLS || yy < 0 || yy >= HROWS) continue
                        val dx = (xx + 0.5f - x) / r
                        val dy = (yy + 0.5f - y) / ry
                        val d = dx * dx + dy * dy
                        if (d > 1f) continue
                        frame.haze(yy * HCOLS + xx, hcol("#b4b4c8"), a * (1f - d))
                    }
                }
            }
        }
    }

    private fun flakes(
        t: Float,
        phi: Float,
    ) {
        for (f in 0 until FLAKES) {
            val layer = flakeL[f]
            val u = hfract(flakeY[f] + t / LOOP * (2 + layer * 2))
            val y = u * (HROWS + 2f) - 1f
            val x = flakeX[f] + (2f + layer * 1.5f) * sin(phi * (layer + 1) + flakeP[f] * HTAU) + u * 6f
            val xi = (x.mod(HCOLS.toFloat())).toInt()
            val yi = y.toInt()
            if (yi < 0 || yi >= HROWS) continue
            // Flakes near the lamp catch its warm light.
            val near = hclamp(1f - (abs(xi - LAMP_X) / 18f + abs(yi - LAMP_TOP - 6) / 12f))
            val c = hcol("#ffffff").mix(hcol("#ffe0a0"), near)
            val a = 0.35f + layer * 0.25f
            frame.speck(
                xi,
                yi,
                c,
                min(1f, a + near * 0.3f),
                if (layer == 0) {
                    '·'
                } else if (layer == 1) {
                    '•'
                } else {
                    '*'
                },
            )
        }
    }
}

/**
 * A snowy village at night: cottages with heaped roofs and glowing windows, smoke curling from every chimney, snow
 * falling in three depths, a lamp post pouring warm light on the trodden path, pines and hills under a pale moon. The
 * seed shapes the pines, the drifts, the stars and the snowfall. Every motion repeats a whole number of times per loop.
 */
public fun snowVillagePiece(seed: Int = 0): AsciiPiece {
    val scene = Village(seed)
    return asciiPiece(
        name = "snow village",
        ground = Rgb.fromHex("#03050f"),
        category = "scenes",
        cols = HCOLS,
        rows = HROWS,
        fps = 15,
        still = 5f,
        loop = LOOP,
        note = "a snowy village at night with lit windows, chimney smoke, falling snow and a lamp post",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A snowy village at night with lit windows, chimney smoke, falling snow and a glowing lamp post.
 *
 * @param seed shapes the pines, the drifts, the stars and the snowfall.
 */
@Composable
public fun AsciiSnowVillage(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A snowy village at night with lit windows, chimney smoke, snowfall and a lamp",
) {
    val piece = remember(seed) { snowVillagePiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
