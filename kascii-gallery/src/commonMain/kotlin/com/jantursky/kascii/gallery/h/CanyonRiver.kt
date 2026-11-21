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

private const val LOOP = 24f
private const val VX = 96f
private const val VY = 23f
private const val STARS = 70

/**
 * One slab of canyon wall: its rim runs from ([outerX], [outerRim]) to ([innerX], [innerRim]) and its inner face drops
 * from there to ([footX], [footY]). [depth] 0 is nearest, 1 the farthest and haziest.
 */
private class Wall(
    val outerX: Float,
    val outerRim: Float,
    val innerX: Float,
    val innerRim: Float,
    val footX: Float,
    val footY: Float,
    val depth: Float,
    val west: Boolean,
)

private class Canyon(
    seed: Int,
) {
    private val lat = HLattice(seed, 31)
    private val back = HFrame()
    private val frame = HFrame()
    private val palette: HPalette
    private val skyStops =
        listOf(
            hcol("#141846"),
            hcol("#2e2260"),
            hcol("#6a2a62"),
            hcol("#c8505a"),
            hcol("#ffa060"),
            hcol("#ffd890"),
        )
    private val bands =
        listOf(
            hcol("#8a3a2a"),
            hcol("#b85a3a"),
            hcol("#7a2e28"),
            hcol("#d08a5a"),
            hcol("#a04a32"),
            hcol("#e0b080"),
            hcol("#6a2a2a"),
        )
    private val shadow = hcol("#2a1438")
    private val glowCol = hcol("#ff9a5a")
    private val river = hcol("#3a2a5a")
    private val starX = IntArray(STARS)
    private val starY = IntArray(STARS)
    private val starP = FloatArray(STARS)
    private val walls =
        listOf(
            Wall(66f, 19.5f, 90f, 21.5f, 85f, 25f, 1f, true),
            Wall(124f, 20f, 103f, 21.8f, 108f, 25f, 1f, false),
            Wall(36f, 12f, 80f, 18f, 64f, 31f, 0.55f, true),
            Wall(156f, 13f, 113f, 18.5f, 130f, 31f, 0.55f, false),
            Wall(0f, 2f, 58f, 11f, 34f, 48f, 0f, true),
            Wall(192f, 4f, 139f, 12f, 160f, 48f, 0f, false),
        )

    init {
        buildSky()
        buildMesas()
        buildFloor()
        for (w in walls) buildWall(w)
        val rng = hrng(seed, 14)
        var s = 0
        while (s < STARS) {
            val x = rng.int(HCOLS)
            val y = rng.int(14)
            if (back.cov[y * HCOLS + x] > 1.4f) continue
            starX[s] = x
            starY[s] = y
            starP[s] = rng.float()
            s++
        }
        palette =
            HPalette(
                hramp(14, *skyStops.toTypedArray()) +
                    hramp(6, hcol("#3a1a3e"), hcol("#5a2a4a"), hcol("#8a4a5a")) +
                    bands.flatMap { listOf(it, it.mix(shadow, 0.35f), it.mix(shadow, 0.65f), it.mix(glowCol, 0.4f)) } +
                    hramp(6, hcol("#14081e"), shadow, hcol("#4a2448")) +
                    hramp(8, hcol("#1a1030"), river, hcol("#8a5a8a"), hcol("#ff9a6a"), hcol("#ffe0b0")) +
                    hramp(4, hcol("#2a1c14"), hcol("#6a4a30"), hcol("#a07a50")) +
                    hramp(3, hcol("#1a2414"), hcol("#3a4a24"), hcol("#5a6a34")) +
                    listOf(hcol("#e8e0ff"), hcol("#ffffff"), hcol("#0a0610")),
            )
    }

    private fun buildSky() {
        for (y in 0 until HROWS) {
            for (x in 0 until HCOLS) {
                val v = hclamp(y / VY)
                val near = hgauss(x.toFloat(), VX, 60f)
                val c = hgrad(v * (0.7f + 0.3f * near), skyStops)
                back.put(x, y, c, 0.55f + 1.3f * v * kotlin.math.sqrt(v) * (0.6f + 0.4f * near))
            }
        }
        back.glow(VX, VY, 30f, glowCol, 0.35f)
        back.glow(VX, VY, 10f, hcol("#ffe0a0"), 0.4f)
    }

    private fun buildMesas() {
        for (x in 72 until 122) {
            val xf = x + 0.5f
            val mesa =
                if (abs(xf - 92f) < 6f || abs(xf - 108f) < 4f) {
                    3.5f
                } else if (abs(xf - 100f) < 11f) {
                    1.2f
                } else {
                    0.4f
                }
            val top = VY - mesa - 0.6f * lat.at(xf * 0.3f, 3f)
            for (y in top.toInt() until VY.toInt() + 2) back.put(x, y, hcol("#5a2a4a").mix(hcol("#c8505a"), 0.25f), 2f)
        }
    }

    private fun riverCentre(y: Float): Float = VX + 2f + 11f * sin((y - VY) * 0.16f) * (y - VY) / 24f

    private fun riverHalf(y: Float): Float = 0.6f + (y - VY) * 1.05f

    private fun buildFloor() {
        for (y in VY.toInt() until HROWS) {
            for (x in 0 until HCOLS) {
                val i = y * HCOLS + x
                val d = (y - VY) / (HROWS - VY)
                val n = lat.fbm(x * 0.12f, y * 0.6f + 20f, 2)
                val c =
                    hcol("#1e1020")
                        .mix(
                            hcol("#5a3a30"),
                            0.2f + 0.55f * n * (1f - d * 0.4f),
                        ).mix(glowCol, 0.15f * (1f - d))
                back.set(i, c, hsolid(0.4f + n * 0.5f, x, y))
                if (lat.at(x * 0.5f + 7f, y * 1.2f) > 0.8f && d > 0.3f) back.set(i, hcol("#3a4a24"), 2f)
            }
        }
    }

    private fun buildWall(w: Wall) {
        val dir = if (w.west) 1f else -1f
        for (y in 0 until HROWS) {
            for (x in 0 until HCOLS) {
                val xf = x + 0.5f
                val yf = y + 0.5f
                // Inside the slab: past the outer side, below the rim, and on the outer side of the inner face.
                val span = (w.innerX - w.outerX)
                val p = (xf - w.outerX) / span
                if (p > 1f) continue
                if (p < 0f && (if (w.west) xf > w.outerX else xf < w.outerX)) continue
                val rough = 0.7f * (lat.at(xf * 0.4f, w.depth * 9f) - 0.5f)
                val rim = w.outerRim + (w.innerRim - w.outerRim) * hclamp(p) + rough
                if (yf < rim) continue
                val face = w.innerX + (w.footX - w.innerX) * hclamp((yf - w.innerRim) / (w.footY - w.innerRim))
                if ((xf - face) * dir > 0f) continue
                if (yf > w.footY) continue
                // Strata converge toward the vanishing point; erosion runs straight down.
                val slope = (yf - VY) / max(2f, abs(xf - VX))
                val k = slope * 11f + (lat.at(xf * 0.05f, yf * 0.12f + w.depth * 7f) - 0.5f) * 0.9f + w.depth * 3f
                val band =
                    bands[
                        kotlin.math
                            .floor(k)
                            .toInt()
                            .mod(bands.size),
                    ]
                val ledge = hfract(k) < 0.14f
                val erosion = lat.fbm(xf * 0.5f, yf * 0.05f + w.depth * 5f, 2)
                val edge = hsmooth(2.5f, 0f, abs(xf - face))
                val below = yf - rim
                var c = band.mix(shadow, 0.5f + 0.35f * (erosion - 0.5f) + (if (ledge) 0.25f else 0f))
                if (!w.west) {
                    // West-facing cliffs still catch the afterglow near their rims.
                    val lit = hsmooth(16f, 0f, below) * (0.6f + 0.4f * erosion)
                    c = c.mix(band.mix(glowCol, 0.35f), lit * (if (ledge) 0.5f else 1f))
                } else {
                    c = c.mix(band, 0.4f * edge)
                }
                if (below < 0.9f) c = c.mix(hcol("#ffb080"), 0.35f * (1f - w.depth))
                c = c.mix(hcol("#6a2a62").mix(hcol("#c8505a"), 0.3f), w.depth * 0.55f)
                back.put(x, y, c, hsolid(0.65f + 0.5f * erosion, x, y))
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
        river(t)
        bird(phi)
        frame.output(grid, palette)
    }

    private fun stars(t: Float) {
        for (s in 0 until STARS) {
            val tw = 0.5f + 0.5f * sin(HTAU * (t / LOOP * 2f + starP[s]))
            if (tw < 0.35f) continue
            val fade = 1f - starY[s] / 16f
            frame.put(
                starX[s],
                starY[s],
                hcol("#e8e0ff").scale(0.6f + 0.4f * tw * fade),
                1f,
                if (tw > 0.85f && fade > 0.5f) '+' else '·',
            )
        }
    }

    private fun river(t: Float) {
        for (y in VY.toInt() until HROWS) {
            val yf = y + 0.5f
            val cx = riverCentre(yf)
            val half = riverHalf(yf)
            val d = (yf - VY) / (HROWS - VY)
            val z = 6f / (yf - VY + 1f)
            for (x in (cx - half - 1f).toInt()..(cx + half + 1f).toInt()) {
                if (x < 0 || x >= HCOLS) continue
                val i = y * HCOLS + x
                val a = (x + 0.5f - cx) / half
                if (abs(a) > 1f) {
                    if (abs(a) < 1f + 1.5f / half) frame.set(i, hcol("#a07a50").mix(glowCol, 0.2f), 2f)
                    continue
                }
                // The water mirrors the glowing notch far away and the dark sky overhead close by.
                val mirror = hgrad(1f - d * 0.85f, skyStops).mix(river, 0.25f + 0.4f * d)
                val flow = hfract(z * 3f - t / LOOP * 6f + 0.3f * lat.at(x * 0.15f, y * 0.4f))
                val ripple = hfract(z * 7f + a * 0.6f - t / LOOP * 12f)
                var c = mirror
                var cov = hsolid(0.65f + 0.3f * flow, x, y)
                if (ripple < 0.18f && abs(a) < 0.9f) {
                    c = c.mix(hcol("#ffe0b0"), 0.18f + 0.3f * (1f - d))
                    cov = 2f
                }
                if (abs(a) > 0.82f) c = c.mix(hcol("#e8e0ff"), 0.25f)
                frame.set(i, c, cov)
            }
        }
    }

    private fun bird(phi: Float) {
        // A hawk circling over the canyon against the glowing sky, banking as it turns.
        val x = 98f + 40f * cos(phi)
        val y = 13.5f + 2.5f * sin(phi)
        val bank = sin(phi)
        val flap = sin(phi * 9f) > 0.8f
        val c = hcol("#0a0610")
        val ix = x.toInt()
        val iy = y.toInt()
        frame.put(ix, iy, c, 2f)
        frame.put(ix, iy + 1, c, 2f, '▀')
        frame.put(ix - 1, iy, c, 2f)
        frame.put(ix + 1, iy, c, 2f, '▀')
        for (k in 1..6) {
            val tipL = (bank > 0.3f && k > 3) || (flap && k > 2)
            val tipR = (bank < -0.3f && k > 3) || (flap && k > 2)
            frame.put(ix - k - 1, iy - (if (tipL) 1 else 0), c, 2f, if (tipL) '▄' else '▀')
            frame.put(ix + k + 1, iy - (if (tipR) 1 else 0), c, 2f, if (tipR) '▄' else '▀')
        }
    }
}

/**
 * A desert canyon at dusk: layered red walls converge on a glowing notch where the sun has just set, mesas stand dark
 * in the distance, a river winds toward the viewer with ripples flowing down it, the first stars come out and a hawk
 * circles overhead. The seed shapes the strata, the floor and the stars. Every motion repeats a whole number of times
 * per loop.
 */
public fun canyonRiverPiece(seed: Int = 0): AsciiPiece {
    val scene = Canyon(seed)
    return asciiPiece(
        name = "canyon river",
        ground = Rgb.fromHex("#0a0612"),
        category = "scenes",
        cols = HCOLS,
        rows = HROWS,
        fps = 15,
        still = 4f,
        loop = LOOP,
        note = "a river winding through red canyon walls at dusk with a hawk circling overhead",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A river winding through layered red canyon walls at dusk, with a hawk circling overhead.
 *
 * @param seed shapes the strata, the floor and the stars.
 */
@Composable
public fun AsciiCanyonRiver(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A river winding through red canyon walls at dusk with a hawk circling overhead",
) {
    val piece = remember(seed) { canyonRiverPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
