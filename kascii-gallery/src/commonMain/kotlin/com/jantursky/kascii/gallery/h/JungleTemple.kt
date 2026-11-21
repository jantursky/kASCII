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

private const val LOOP = 24f
private const val TEMPLE_X = 98f
private const val BASE = 43
private const val TIERS = 5
private const val TIER_H = 4
private const val FLIES = 26
private const val VINES = 22
private const val MOTES = 60

private class Temple(
    seed: Int,
) {
    private val lat = HLattice(seed, 13)
    private val mistLat = HLattice(seed, 14)
    private val back = HFrame()
    private val fore = HFrame()
    private val frame = HFrame()
    private val palette: HPalette
    private val haze = hcol("#5a7a58")
    private val canopy = hcol("#14301e")
    private val stone = hcol("#b4ac8a")
    private val stoneDark = hcol("#3a382c")
    private val moss = hcol("#4a7a30")
    private val leaf = hcol("#3a8a3a")
    private val leafDark = hcol("#06140a")
    private val sunlight = hcol("#ffe6a0")
    private val fly = hcol("#d8ff6a")
    private val flyX = FloatArray(FLIES)
    private val flyY = FloatArray(FLIES)
    private val flyAx = FloatArray(FLIES)
    private val flyAy = FloatArray(FLIES)
    private val flyP = FloatArray(FLIES)
    private val flyK = IntArray(FLIES)
    private val vineX = FloatArray(VINES)
    private val vineTop = FloatArray(VINES)
    private val vineLen = FloatArray(VINES)
    private val vinePhase = FloatArray(VINES)
    private val moteX = FloatArray(MOTES)
    private val moteY = FloatArray(MOTES)

    init {
        buildBack()
        buildTemple()
        buildFore(seed)
        val rng = hrng(seed, 5)
        for (f in 0 until FLIES) {
            flyX[f] = 20f + rng.float() * 152f
            flyY[f] = 28f + rng.float() * 17f
            flyAx[f] = 3f + rng.float() * 8f
            flyAy[f] = 1f + rng.float() * 2.5f
            flyP[f] = rng.float()
            flyK[f] = 1 + rng.int(3)
        }
        for (v in 0 until VINES) {
            val tier = rng.int(TIERS + 1)
            val half = if (tier == TIERS) 10f else halfWidth(tier)
            vineX[v] = TEMPLE_X + (rng.float() * 2f - 1f) * (half - 2f)
            if (abs(vineX[v] - TEMPLE_X) < 7f) vineX[v] += if (vineX[v] < TEMPLE_X) -8f else 8f
            vineTop[v] = if (tier == TIERS) 15f else (BASE - (tier + 1) * TIER_H).toFloat()
            vineLen[v] = 3f + rng.float() * 9f
            vinePhase[v] = rng.float()
        }
        for (m in 0 until MOTES) {
            moteX[m] = rng.float()
            moteY[m] = rng.float()
        }
        palette =
            HPalette(
                hramp(8, hcol("#0a1a12"), hcol("#1e3a2a"), haze, hcol("#a8c08a"), hcol("#f0f0c0")) +
                    hramp(6, leafDark, canopy, hcol("#2a5a34")) +
                    hramp(10, hcol("#141a14"), stoneDark, hcol("#6a6650"), stone, hcol("#ece4c0")) +
                    hramp(6, hcol("#1a3010"), moss, hcol("#8aba4a")) +
                    hramp(6, leafDark, leaf, hcol("#9ad05a")) +
                    hramp(5, hcol("#8a7a4a"), hcol("#e0c870"), sunlight, hcol("#fffbe8")) +
                    hramp(3, hcol("#6a8a2a"), fly, hcol("#fbffd8")) +
                    listOf(hcol("#050806"), hcol("#e06a3a")),
            )
    }

    private fun halfWidth(tier: Int): Float = 48f - tier * 8.5f

    private fun buildBack() {
        for (y in 0 until HROWS) {
            for (x in 0 until HCOLS) {
                // Hazy green air, brighter up and to the left where the sun breaks through.
                val dx = (x - 40f) / 130f
                val dy = y / 34f
                val k = hclamp(1f - sqrt(dx * dx + dy * dy))
                back.put(x, y, hcol("#2a4a38").mix(hcol("#d8e4b0"), k), 0.3f + 0.6f * k)
            }
        }
        // Far canopy as pale dotted silhouettes in the haze, then a darker, nearer wall of leaves.
        for (x in 0 until HCOLS) {
            val top = 5f + 8f * lat.fbm(x * 0.05f, 0f, 3) + 3f * sin(x * 0.07f)
            for (y in max(0, top.toInt()) until HROWS) {
                val bump = lat.fbm(x * 0.15f, y * 0.4f, 2)
                back.put(x, y, hcol("#3e5e48").mix(hcol("#7a9a78"), bump - 0.2f), 1.05f)
            }
        }
        for (x in 0 until HCOLS) {
            val n = lat.fbm(x * 0.06f + 30f, 11f, 3)
            val top = 15f + 9f * n + 3f * sin(x * 0.09f + 1f)
            for (y in max(0, top.toInt()) until HROWS) {
                val bump = lat.fbm(x * 0.18f + 9f, y * 0.5f, 2)
                val rim = hsmooth(top + 2f, top, y.toFloat())
                val c = hcol("#0c2016").mix(hcol("#2e5a3a"), bump * 0.8f + rim * 0.4f)
                back.put(x, y, c, hsolid(0.5f + bump * 0.5f, x, y))
            }
        }
    }

    private fun buildTemple() {
        for (tier in 0 until TIERS) {
            val half = halfWidth(tier)
            val bottom = BASE - tier * TIER_H
            for (y in bottom - TIER_H until bottom) {
                val top = y == bottom - TIER_H
                for (x in (TEMPLE_X - half).toInt()..(TEMPLE_X + half).toInt()) {
                    val a = (x + 0.5f - TEMPLE_X) / half
                    val weather = lat.fbm(x * 0.2f, y * 0.6f + 20f, 3)
                    var k = 0.55f - 0.3f * a + (weather - 0.5f) * 0.4f
                    if (top) k += 0.25f
                    if (y == bottom - 1) k -= 0.2f
                    val block = (x + (if ((y / 2) % 2 == 0) 0 else 3)) % 6 == 0 && !top
                    if (block) k -= 0.18f
                    var c = stoneDark.mix(stone, hclamp(k))
                    val mossy = hsmooth(0.55f, 0.7f, lat.fbm(x * 0.1f + 50f, y * 0.35f, 3)) * (if (top) 1f else 0.7f)
                    c = c.mix(moss.scale(0.6f + 0.6f * hclamp(k)), mossy)
                    back.put(x, y, c, hsolid(0.55f + 0.5f * weather, x, y))
                }
            }
        }
        // The central stair.
        for (y in 22 until BASE) {
            val half = 6.5f + (y - 22) * 0.12f
            for (x in (TEMPLE_X - half).toInt()..(TEMPLE_X + half).toInt()) {
                val a = (x + 0.5f - TEMPLE_X) / half
                val tread = y % 2 == 0
                val k = (if (tread) 0.75f else 0.4f) - 0.2f * a
                var c = stoneDark.mix(stone, hclamp(k))
                if (abs(a) > 0.85f) c = stoneDark.mix(stone, 0.3f)
                val mossy = hsmooth(0.58f, 0.7f, lat.fbm(x * 0.12f, y * 0.3f + 80f, 3))
                back.put(x, y, c.mix(moss, mossy * 0.7f), 2f, if (tread && abs(a) <= 0.85f) '▀' else 0.toChar())
            }
        }
        // The shrine on top, with a dark doorway and a carved crest.
        for (y in 14 until 23) {
            for (x in (TEMPLE_X - 11f).toInt()..(TEMPLE_X + 11f).toInt()) {
                val a = (x + 0.5f - TEMPLE_X) / 11f
                val weather = lat.fbm(x * 0.25f, y * 0.7f + 3f, 2)
                var c = stoneDark.mix(stone, hclamp(0.6f - 0.3f * a + (weather - 0.5f) * 0.5f))
                if (y == 14 || y == 17) c = stoneDark.mix(stone, hclamp(0.85f - 0.3f * a))
                back.put(x, y, c, hsolid(0.6f + weather * 0.4f, x, y))
            }
        }
        for (y in 18 until 23) {
            for (x in (TEMPLE_X - 3f).toInt()..(TEMPLE_X + 2f).toInt()) back.put(x, y, hcol("#050806"), 2f)
        }
        for (x in (TEMPLE_X - 7f).toInt()..(TEMPLE_X + 7f).toInt()) {
            if (x % 3 == 0) back.put(x, 13, stone.mix(stoneDark, 0.3f), 2f, '▄')
            back.put(x, 15, stoneDark, 1f, if (x % 2 == 0) '=' else '-')
        }
        // Roots and a strangler fig gripping the right side.
        for (k in 0 until 5) {
            val x0 = TEMPLE_X + 22f + k * 4f
            for (y in (BASE - 12 + k) until BASE) {
                val x = x0 + 1.5f * sin(y * 0.6f + k)
                back.put(x.toInt(), y, hcol("#3a2a1a").mix(hcol("#7a5a3a"), 0.3f), 2f)
            }
        }
    }

    private fun buildFore(seed: Int) {
        val rng = hrng(seed, 6)
        // Big leaves framing both sides and a dark forest floor.
        for (side in 0 until 2) {
            repeat(12) {
                val cx = if (side == 0) rng.float() * 30f - 4f else HCOLS - rng.float() * 30f + 4f
                val cy = 6f + rng.float() * 40f
                val len = 12f + rng.float() * 12f
                val ang = (if (side == 0) 0.2f else HTAU / 2f - 0.2f) + (rng.float() - 0.5f) * 1.2f
                leafShape(cx, cy, len, ang)
            }
        }
        for (x in 0 until HCOLS) {
            val top = 44.5f + 1.2f * sin(x * 0.11f) + 1.5f * (lat.fbm(x * 0.1f, 90f, 2) - 0.5f)
            for (y in top.toInt() until HROWS) {
                fore.put(x, y, leafDark.mix(canopy, 0.4f * lat.at(x * 0.4f, y.toFloat())), 2f)
            }
        }
    }

    private fun leafShape(
        cx: Float,
        cy: Float,
        len: Float,
        ang: Float,
    ) {
        val ux = cos(ang)
        val uy = sin(ang)
        for (y in max(0, (cy - len / HROW_U).toInt() - 1)..min(HROWS - 1, (cy + len / HROW_U).toInt() + 1)) {
            for (x in max(0, (cx - len).toInt() - 1)..min(HCOLS - 1, (cx + len).toInt() + 1)) {
                val dx = x + 0.5f - cx
                val dy = (y + 0.5f - cy) * HROW_U
                val along = dx * ux + dy * uy
                val across = -dx * uy + dy * ux
                if (along < 0f || along > len) continue
                val p = along / len
                val w = len * 0.28f * sin(p * 3.1416f) * (1f - 0.3f * p)
                if (abs(across) > w) continue
                val vein = abs(across) < 0.6f
                val k = 0.2f + 0.4f * (1f - abs(across) / w) + (if (across < 0f) 0.12f else 0f)
                fore.put(x, y, if (vein) leafDark.mix(leaf, 0.15f) else leafDark.mix(leaf, k), 2f)
            }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = HTAU * t / LOOP
        frame.copyFrom(back)
        vines(phi)
        shafts(t, phi)
        mist(t)
        frame.over(fore)
        fireflies(phi)
        frame.glow(30f, -2f, 40f, sunlight, 0.35f)
        frame.output(grid, palette)
    }

    private fun vines(phi: Float) {
        for (v in 0 until VINES) {
            val n = vineLen[v].toInt()
            for (k in 0..n) {
                val p = k.toFloat() / max(1, n)
                val x = vineX[v] + 0.9f * p * p * sin(phi * 2f + vinePhase[v] * HTAU)
                val y = vineTop[v] + k
                val c = leafDark.mix(leaf, 0.5f + 0.3f * sin(k + vinePhase[v] * 9f))
                frame.put(x.toInt(), y.toInt(), c, 2f)
                if ((k + (vinePhase[v] * 7f).toInt()) % 3 == 0) {
                    frame.put(
                        x.toInt() + 1,
                        y.toInt(),
                        leaf.mix(hcol("#9ad05a"), 0.4f),
                        2f,
                        '▄',
                    )
                }
            }
        }
    }

    private fun shafts(
        t: Float,
        phi: Float,
    ) {
        val ux = 0.5f
        val uy = 0.866f
        for (y in 0 until HROWS) {
            for (x in 0 until HCOLS) {
                val px = x + 0.5f
                val py = (y + 0.5f) * HROW_U
                val perp = px * uy - py * ux
                var k = 0f
                k += hgauss(perp, 52f, 5f) * (0.75f + 0.25f * sin(phi))
                k += hgauss(perp, 78f, 3f) * (0.6f + 0.4f * sin(phi * 2f + 1f))
                k += hgauss(perp, 104f, 7f) * (0.7f + 0.3f * sin(phi + 2.5f))
                k += hgauss(perp, 138f, 2.5f) * (0.5f + 0.5f * sin(phi * 3f + 4f))
                if (k < 0.04f) continue
                val along = px * ux + py * uy
                val shimmer = 0.7f + 0.3f * lat.wrapAt(along * 0.06f - t / LOOP * 4f, perp * 0.1f, 4)
                val fade = 1f - hsmooth(40f, 105f, py)
                frame.light(y * HCOLS + x, sunlight, k * shimmer * fade * 0.55f)
            }
        }
        // Dust motes drifting down through the light.
        for (m in 0 until MOTES) {
            val u = hfract(moteY[m] + t / LOOP)
            val y = u * HROWS
            val x = moteX[m] * HCOLS + 2f * sin(phi * 3f + m)
            val perp = x * uy - y * HROW_U * ux
            val inShaft = max(max(hgauss(perp, 52f, 5f), hgauss(perp, 104f, 7f)), hgauss(perp, 78f, 3f))
            if (inShaft > 0.4f) frame.speck(x.toInt(), y.toInt(), hcol("#fffbe8"), 0.5f * inShaft, '·')
        }
    }

    private fun mist(t: Float) {
        val drift = t / LOOP * 4f
        for (y in 30 until HROWS) {
            val band = hsmooth(30f, 42f, y.toFloat())
            for (x in 0 until HCOLS) {
                val n = mistLat.wrapFbm(x * 0.03f - drift, y * 0.3f, 3, 6)
                val d = hsmooth(0.38f, 0.75f, n) * band * 0.75f
                if (d > 0.03f) frame.haze(y * HCOLS + x, hcol("#a8c08a"), d)
            }
        }
    }

    private fun fireflies(phi: Float) {
        for (f in 0 until FLIES) {
            val k = flyK[f]
            val x = flyX[f] + flyAx[f] * sin(phi * k + flyP[f] * HTAU)
            val y = flyY[f] + flyAy[f] * sin(phi * (k + 1) + flyP[f] * 9f)
            val blink = hsmooth(0.3f, 0.9f, sin(phi * (k + 2) + flyP[f] * 17f))
            if (blink <= 0.02f) continue
            frame.glow(x, y, 3.5f, fly, 0.25f * blink)
            frame.speck(x.toInt(), y.toInt(), fly.mix(hcol("#fbffd8"), blink), 0.4f + 0.6f * blink)
        }
    }
}

/**
 * A stepped temple ruin swallowed by jungle: vines sway from its terraces, shafts of sunlight slant through the canopy
 * with dust drifting in them, mist creeps along its base and fireflies blink in the shade. The seed shapes the
 * weathering, the vines, the leaves and the fireflies. Every motion repeats a whole number of times per loop.
 */
public fun jungleTemplePiece(seed: Int = 0): AsciiPiece {
    val scene = Temple(seed)
    return asciiPiece(
        name = "jungle temple",
        ground = Rgb.fromHex("#030805"),
        category = "scenes",
        cols = HCOLS,
        rows = HROWS,
        fps = 15,
        still = 4f,
        loop = LOOP,
        note = "a vine-grown stepped temple in the jungle, sunbeams slanting through mist and fireflies",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A stepped jungle temple ruin with swaying vines, slanting shafts of light, creeping mist and fireflies.
 *
 * @param seed shapes the weathering, the vines, the leaves and the fireflies.
 */
@Composable
public fun AsciiJungleTemple(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A vine-covered jungle temple with shafts of sunlight, mist and fireflies",
) {
    val piece = remember(seed) { jungleTemplePiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
