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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 20f
private const val MOUTH_X = 118f
private const val MOUTH_Y = 21f
private const val MOUTH_RX = 36f
private const val MOUTH_RY = 13f
private const val FLOOR = 37
private const val FACETS = 140
private const val FLAKES = 90
private const val GLINTS = 34

private class Cave(
    seed: Int,
) {
    private val lat = HLattice(seed, 17)
    private val back = HFrame()
    private val frame = HFrame()
    private val palette: HPalette
    private val deep = hcol("#04081c")
    private val iceDark = hcol("#0e2a5a")
    private val ice = hcol("#2a7ac0")
    private val iceLit = hcol("#8ad8ff")
    private val frost = hcol("#e8f8ff")
    private val skyCol = hcol("#bfe0f8")
    private val inside = BooleanArray(HCELLS)
    private val glintX = IntArray(GLINTS)
    private val glintY = IntArray(GLINTS)
    private val glintP = FloatArray(GLINTS)
    private val flakeP = FloatArray(FLAKES)
    private val flakeA = FloatArray(FLAKES)
    private val flakeS = FloatArray(FLAKES)
    private val flakeK = IntArray(FLAKES)

    init {
        buildMouth()
        buildWalls(seed)
        buildFloor()
        buildIcicles(seed)
        val rng = hrng(seed, 9)
        var g = 0
        while (g < GLINTS) {
            val x = rng.int(HCOLS)
            val y = rng.int(HROWS)
            val i = y * HCOLS + x
            if (inside[i] || mouthDist(x, y) > 2.2f) continue
            glintX[g] = x
            glintY[g] = y
            glintP[g] = rng.float()
            g++
        }
        for (f in 0 until FLAKES) {
            flakeP[f] = rng.float()
            flakeA[f] = rng.float() * 2f - 1f
            flakeS[f] = rng.float()
            flakeK[f] = 1 + rng.int(2)
        }
        palette =
            HPalette(
                hramp(12, deep, hcol("#081440"), iceDark, hcol("#1a4c8a"), ice, hcol("#4aa8e0"), iceLit) +
                    hramp(6, hcol("#5aa0d8"), hcol("#a8dcf8"), frost, hcol("#ffffff")) +
                    hramp(6, hcol("#7aa8d0"), skyCol, hcol("#f4fbff")) +
                    hramp(5, hcol("#3a5a8a"), hcol("#6a8ab8"), hcol("#a8c0e0")) +
                    hramp(4, hcol("#0a1430"), hcol("#1a2848"), hcol("#2a3a5a")) +
                    listOf(hcol("#c04030"), hcol("#ffd890"), hcol("#06070c")),
            )
    }

    /** Distance from the mouth's rim in units of its radii: under 1 is open air. */
    private fun mouthDist(
        x: Int,
        y: Int,
    ): Float {
        val dx = (x + 0.5f - MOUTH_X) / MOUTH_RX
        val dy = (y + 0.5f - MOUTH_Y) / MOUTH_RY
        val a = atan2(dy, dx)
        val wobble = 1f + 0.16f * (lat.fbm(cos(a) * 2f + 5f, sin(a) * 2f + 5f, 3) - 0.5f) * 2f + 0.06f * sin(a * 5f)
        val flat = if (dy > 0.55f) (dy - 0.55f) * 1.8f else 0f
        return sqrt(dx * dx + dy * dy) / wobble + flat
    }

    private fun buildMouth() {
        for (y in 0 until HROWS) {
            for (x in 0 until HCOLS) {
                if (mouthDist(x, y) >= 1f) continue
                val i = y * HCOLS + x
                inside[i] = true
                val v = hclamp((y - (MOUTH_Y - MOUTH_RY)) / (MOUTH_RY * 2f))
                var c = hcol("#2a6ab0").mix(hcol("#7ab4e4"), hclamp(v * 1.8f)).mix(skyCol, hclamp(v * 2f - 0.8f))
                var cov = 2f
                // Distant snowy peaks and a white plain outside.
                val ridge = MOUTH_Y + 4f - 9f * lat.fbm(x * 0.05f, 40f, 3) + 2.5f * sin(x * 0.13f)
                if (y >= ridge) {
                    val lit = lat.at(x * 0.3f, y * 0.8f + 3f)
                    val face = if (lat.at(x * 0.12f, 70f) > 0.45f) 1f else 0.3f
                    c = hcol("#5a7ab0").mix(hcol("#f4fbff"), 0.2f + 0.8f * face * (0.7f + 0.3f * lit))
                    cov = hsolid(0.7f + 0.3f * lit, x, y)
                }
                if (y >= MOUTH_Y + 7) {
                    c = hcol("#b8d4f0").mix(hcol("#f4fbff"), (y - MOUTH_Y - 7f) / 8f)
                    cov = 2f
                }
                back.set(i, c, cov)
            }
        }
    }

    private fun buildWalls(seed: Int) {
        val rng = hrng(seed, 10)
        val sx = FloatArray(FACETS) { rng.float() * HCOLS }
        val sy = FloatArray(FACETS) { rng.float() * HROWS * HROW_U }
        val shade = FloatArray(FACETS) { 0.55f + 0.45f * rng.float() }
        for (y in 0 until HROWS) {
            for (x in 0 until HCOLS) {
                val i = y * HCOLS + x
                if (inside[i]) continue
                val px = x + 0.5f
                val py = (y + 0.5f) * HROW_U
                var d1 = Float.MAX_VALUE
                var d2 = Float.MAX_VALUE
                var best = 0
                for (k in 0 until FACETS) {
                    val dx = px - sx[k]
                    val dy = (py - sy[k]) * 0.8f
                    val d = dx * dx + dy * dy
                    if (d < d1) {
                        d2 = d1
                        d1 = d
                        best = k
                    } else if (d < d2) {
                        d2 = d
                    }
                }
                val edge = sqrt(d2) - sqrt(d1)
                val md = mouthDist(x, y)
                val light = hclamp(1.02f - (md - 1f) * 0.7f)
                val lit = light * light
                var c =
                    deep.mix(iceDark, hclamp(0.3f + lit)).mix(ice, lit * shade[best]).mix(
                        iceLit,
                        hclamp(lit - 0.55f) * shade[best] * 1.6f,
                    )
                var cov = hsolid(0.35f + 0.6f * shade[best] * (0.4f + lit), x, y)
                if (edge < 1.1f) {
                    c = c.mix(frost, 0.15f + 0.45f * lit)
                    cov = 2f
                }
                // A bright rim of frost right at the mouth.
                if (md < 1.12f) {
                    c = c.mix(frost, 0.7f)
                    cov = 2f
                }
                back.set(i, c, cov)
            }
        }
    }

    private fun buildFloor() {
        for (y in FLOOR until HROWS) {
            for (x in 0 until HCOLS) {
                val i = y * HCOLS + x
                if (inside[i]) continue
                val d = (y - FLOOR) / (HROWS - FLOOR).toFloat()
                // A pool of daylight spilling in from the mouth, widening toward us.
                val spread = 18f + 50f * d
                val pool = hgauss(x + 0.5f, MOUTH_X - 8f * d, spread) * (1f - 0.5f * d)
                val c =
                    deep.mix(iceDark, 0.5f + 0.5f * pool).mix(iceLit, pool * 0.6f).mix(
                        frost,
                        hclamp(pool - 0.75f) * 2f,
                    )
                val sheen = lat.at(x * 0.08f, y * 1.6f + 30f)
                back.set(i, c, hsolid(0.5f + pool * 0.5f, x, y))
                if (sheen > 0.66f) back.light(i, iceLit, 0.08f + 0.25f * pool)
            }
        }
    }

    private fun buildIcicles(seed: Int) {
        val rng = hrng(seed, 11)
        for (k in 0 until 46) {
            val x = rng.int(HCOLS)
            // Hang them from the ceiling and from the mouth's upper rim.
            var start = 0
            for (y in 0 until (MOUTH_Y).toInt()) if (!inside[y * HCOLS + x]) start = y
            if (abs(x - MOUTH_X) > MOUTH_RX * 0.95f) start = rng.int(4)
            val len = 2 + rng.int(if (abs(x - MOUTH_X) < MOUTH_RX) 6 else 4)
            for (j in 0..len) {
                val y = start + j
                val lit = hclamp(1.2f - (mouthDist(x, y) - 1f) * 0.6f)
                val c = iceDark.mix(frost, 0.35f + 0.6f * lit)
                back.put(x, y, if (j == len) c.mix(frost, 0.5f) else c, 2f)
                if (j < len / 2) back.put(x + 1, y, c.scale(0.8f), 2f)
            }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = HTAU * t / LOOP
        frame.copyFrom(back)
        outsideSnow(t)
        explorer(phi)
        frame.glow(MOUTH_X, MOUTH_Y + 2f, 46f, hcol("#a8dcf8"), 0.1f + 0.03f * sin(phi))
        glints(t)
        drift(t)
        frame.output(grid, palette)
    }

    private fun outsideSnow(t: Float) {
        for (k in 0 until 60) {
            val x0 = hfract(k * 0.618f) * MOUTH_RX * 2f + MOUTH_X - MOUTH_RX
            val v = hfract(k * 0.371f + t / LOOP * 2f)
            val y = MOUTH_Y - MOUTH_RY + v * MOUTH_RY * 2f
            val x = x0 + 2f * sin(HTAU * (t / LOOP * 2f + k * 0.13f)) - v * 6f
            if (x < 0f || x >= HCOLS || y < 0f || y >= HROWS || !inside[y.toInt() * HCOLS + x.toInt()]) continue
            frame.speck(x.toInt(), y.toInt(), hcol("#ffffff"), 0.6f)
        }
    }

    private fun explorer(phi: Float) {
        // A small figure standing in the mouth for scale, a lamp swinging gently at its side.
        val x = 104
        val c = hcol("#06070c")
        frame.put(x, 28, c, 2f, '●')
        frame.put(x, 29, c, 2f)
        frame.put(x - 1, 29, c, 2f, '▄')
        frame.put(x + 1, 29, c, 2f, '▄')
        frame.put(x, 30, c, 2f)
        frame.put(x - 1, 31, c, 2f, '▌')
        frame.put(x + 1, 31, c, 2f, '▌')
        val lampX = x + 2f + 0.6f * sin(phi * 2f)
        frame.put(lampX.toInt(), 30, hcol("#ffd890"), 2f, '•')
        frame.glow(lampX, 30.5f, 3f, hcol("#ffd890"), 0.3f)
    }

    private fun glints(t: Float) {
        for (g in 0 until GLINTS) {
            val tw = hfract(t / LOOP * 2f + glintP[g])
            val k = max(0f, 1f - abs(tw - 0.5f) * 8f)
            if (k <= 0f) continue
            val x = glintX[g]
            val y = glintY[g]
            frame.speck(x, y, hcol("#ffffff"), k)
            if (k > 0.6f) {
                frame.speck(x - 1, y, iceLit, 0.5f * k)
                frame.speck(x + 1, y, iceLit, 0.5f * k)
            }
            frame.glow(x + 0.5f, y + 0.5f, 3f, iceLit, 0.3f * k)
        }
    }

    private fun drift(t: Float) {
        // Snow blowing in through the mouth and fanning out toward us.
        for (f in 0 until FLAKES) {
            val u = hfract(t / LOOP * flakeK[f] + flakeP[f])
            val ang = flakeA[f]
            val x = MOUTH_X + ang * 20f + (ang * 70f - 40f) * u + 3f * sin(HTAU * (u * 2f + flakeS[f]))
            val y = MOUTH_Y + 4f * flakeS[f] - 3f + (10f + 18f * flakeS[f]) * u * u
            if (x < 0f || x >= HCOLS || y < 0f || y >= HROWS) continue
            frame.speck(x.toInt(), y.toInt(), hcol("#ffffff"), 0.35f + 0.5f * u, if (u < 0.4f) '·' else '•')
        }
    }
}

/**
 * Inside an ice cave looking out: faceted blue walls glow where daylight pours through the mouth, icicles hang from
 * the ceiling, snow blows in and fans out toward the viewer, crystals glint, and a small figure with a lamp stands in
 * the opening. The seed shapes the facets, the icicles and the snow. Every motion repeats a whole number of times
 * per loop.
 */
public fun iceCavePiece(seed: Int = 0): AsciiPiece {
    val scene = Cave(seed)
    return asciiPiece(
        name = "ice cave",
        ground = Rgb.fromHex("#02040e"),
        category = "scenes",
        cols = HCOLS,
        rows = HROWS,
        fps = 15,
        still = 5f,
        loop = LOOP,
        note = "a crystalline blue ice cave lit through its mouth, snow drifting in and crystals glinting",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * An ice cave seen from inside, its faceted walls lit through the mouth, with drifting snow and glinting crystals.
 *
 * @param seed shapes the facets, the icicles and the snow.
 */
@Composable
public fun AsciiIceCave(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A blue crystalline ice cave lit through its mouth, with drifting snow and glints",
) {
    val piece = remember(seed) { iceCavePiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
