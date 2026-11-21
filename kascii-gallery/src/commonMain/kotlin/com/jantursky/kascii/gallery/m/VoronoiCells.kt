package com.jantursky.kascii.gallery.m

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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 20f
private const val SEEDS = 26
private const val HEIGHT_U = MROWS * M_ROW_U

private class Cells(
    seed: Int,
) {
    private val frame = MFrame()
    private val palette: MPalette
    private val cx = FloatArray(SEEDS)
    private val cy = FloatArray(SEEDS)
    private val ax = FloatArray(SEEDS)
    private val ay = FloatArray(SEEDS)
    private val fx = IntArray(SEEDS)
    private val fy = IntArray(SEEDS)
    private val px = FloatArray(SEEDS)
    private val py = FloatArray(SEEDS)
    private val hue = IntArray(SEEDS)
    private val sx = FloatArray(SEEDS)
    private val sy = FloatArray(SEEDS)
    private val hues =
        listOf(
            mcol("#2fd4c4"),
            mcol("#5a7cff"),
            mcol("#c55cff"),
            mcol("#ff5c9a"),
            mcol("#ffb347"),
            mcol("#7be06a"),
        )
    private val edge = mcol("#fff2d6")

    init {
        val rng = mRng(seed, 71)
        // Seeds start on a jittered grid so the cells are of a similar size, then wander around their home.
        var s = 0
        val colsN = 7
        val rowsN = 4
        for (r in 0 until rowsN) {
            for (c in 0 until colsN) {
                if (s >= SEEDS) break
                cx[s] = (c + 0.5f + (rng.float() - 0.5f) * 0.6f) * MCOLS / colsN
                cy[s] = (r + 0.5f + (rng.float() - 0.5f) * 0.6f) * HEIGHT_U / rowsN
                ax[s] = 6f + rng.float() * 10f
                ay[s] = 4f + rng.float() * 7f
                fx[s] = 1 + rng.int(2)
                fy[s] = 1 + rng.int(2)
                px[s] = rng.float() * MTAU
                py[s] = rng.float() * MTAU
                s++
            }
        }
        // Neighbouring seeds get different hues: walk the grid order and skip the hue of the cell to the left and
        // above.
        for (k in 0 until SEEDS) {
            var h = rng.int(hues.size)
            val left = if (k % colsN > 0) hue[k - 1] else -1
            val up = if (k >= colsN) hue[k - colsN] else -1
            while (h == left || h == up) h = (h + 1) % hues.size
            hue[k] = h
        }
        val list = ArrayList<MCol>()
        list += mramp(8, mcol("#030308"), mcol("#0a0a16"), mcol("#16142a"), mcol("#26223e"))
        for (c in hues) list += mramp(8, c.scale(0.12f), c.scale(0.35f), c.scale(0.7f), c, c.mix(edge, 0.6f))
        list += mramp(6, mcol("#8a7a60"), mcol("#d8c8a0"), edge, mcol("#ffffff"))
        palette = MPalette(list)
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = MTAU * t / LOOP
        for (s in 0 until SEEDS) {
            sx[s] = cx[s] + ax[s] * sin(fx[s] * phi + px[s])
            sy[s] = cy[s] + ay[s] * sin(fy[s] * phi + py[s])
        }
        // A soft band of light sweeps across the glass once per loop.
        val sweepC = cos(phi)
        val sweepS = sin(phi)
        for (y in 0 until MROWS) {
            val wy = (y + 0.5f) * M_ROW_U
            for (x in 0 until MCOLS) {
                val wx = x + 0.5f
                var d1 = Float.MAX_VALUE
                var d2 = Float.MAX_VALUE
                var i1 = 0
                var i2 = 0
                for (s in 0 until SEEDS) {
                    val dx = wx - sx[s]
                    val dy = wy - sy[s]
                    val d = dx * dx + dy * dy
                    if (d < d1) {
                        d2 = d1
                        i2 = i1
                        d1 = d
                        i1 = s
                    } else if (d < d2) {
                        d2 = d
                        i2 = s
                    }
                }
                val f1 = sqrt(d1)
                val f2 = sqrt(d2)
                // Distance to the bisector between the two nearest seeds, and the cell's extent across it.
                val nx = sx[i2] - sx[i1]
                val ny = sy[i2] - sy[i1]
                val nl = max(0.001f, sqrt(nx * nx + ny * ny))
                val perp = (d2 - d1) / (2f * nl)
                val extent = 0.5f * (abs(nx / nl) + abs(ny / nl) * M_ROW_U)
                val ratio = f1 / (f1 + f2)
                val c1 = hues[hue[i1]]
                val c2 = hues[hue[i2]]
                val i = y * MCOLS + x
                val band =
                    0.5f + 0.5f * cos((wx / MCOLS - 0.5f) * 3f * sweepC + (wy / HEIGHT_U - 0.5f) * 3f * sweepS - phi)
                val lift = 0.75f + 0.35f * band
                if (perp < extent * 0.5f) {
                    // The seam between two panes stays dark, so the glowing rims on each side read as one bright edge.
                    frame.set(i, 0f, 0f, 0f, 0f)
                    continue
                }
                // The pane: dark at its nucleus, richer toward its rim, blazing along the edge.
                val shade = (0.16f + 1.6f * ratio * ratio) * lift
                val glow = exp(-(perp - extent * 0.5f) * 0.42f)
                val hot = c1.mix(edge, 0.25f + 0.35f * glow)
                val r = c1.r * shade + hot.r * glow * 0.85f * lift
                val g = c1.g * shade + hot.g * glow * 0.85f * lift
                val b = c1.b * shade + hot.b * glow * 0.85f * lift
                val cov = min(2f, 0.28f + 5.2f * ratio * ratio + glow * 0.9f)
                frame.set(i, r, g, b, cov)
            }
        }
        // The nuclei: a bright core with a halo.
        for (s in 0 until SEEDS) {
            val c = hues[hue[s]]
            val gx = sx[s].toInt()
            val gy = (sy[s] / M_ROW_U).toInt()
            for (dy in -2..2) {
                for (dx in -5..5) {
                    val x = gx + dx
                    val y = gy + dy
                    if (x !in 0 until MCOLS || y !in 0 until MROWS) continue
                    val ex = x + 0.5f - sx[s]
                    val ey = (y + 0.5f) * M_ROW_U - sy[s]
                    val k = exp(-(ex * ex + ey * ey) / 9f)
                    val i = y * MCOLS + x
                    if (frame.ch[i] != 0.toChar()) continue
                    frame.light(i, c.mix(edge, 0.4f), k * 0.8f)
                }
            }
            if (gx in 0 until MCOLS && gy in 0 until MROWS) {
                val core = c.mix(edge, 0.7f)
                frame.set(gy * MCOLS + gx, core.r, core.g, core.b, 1f, '●')
            }
        }
        frame.output(grid, palette)
    }
}

/**
 * Voronoi cells: a stained-glass window of drifting cells. Every pane belongs to the nearest of a set of wandering
 * seeds, dark at its glowing nucleus and richer toward its rim, and the panes are parted by bright leads drawn along
 * the true bisectors, which slide, split and merge as the seeds move. A soft band of light sweeps over the glass. The
 * seed sets the cell layout, the paths and the colours; every path closes once per loop.
 */
public fun voronoiCellsPiece(seed: Int = 0): AsciiPiece {
    val scene = Cells(seed)
    return asciiPiece(
        name = "voronoi cells",
        ground = Rgb.fromHex("#040409"),
        category = "generative",
        cols = MCOLS,
        rows = MROWS,
        fps = 24,
        still = 4f,
        loop = LOOP,
        note = "drifting voronoi cells like stained glass, with glowing leads between the panes",
    ) { time, grid -> scene.draw(mLoopTime(time, LOOP), grid) }
}

/**
 * A stained-glass field of drifting Voronoi cells with glowing edges.
 *
 * @param seed sets the cell layout, the paths and the colours.
 */
@Composable
public fun AsciiVoronoiCells(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Drifting Voronoi cells like stained glass with glowing edges",
) {
    val piece = remember(seed) { voronoiCellsPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
