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
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val LOOP = 30f
private const val BIRDS = 520
private const val HORIZON = 36
private const val TRAILS = 4
private const val TRAIL_DT = 0.09f
private const val REEDS = 26

private class Murmuration(
    seed: Int,
) {
    private val frame = MFrame()
    private val palette: MPalette
    private val lat = MLattice(seed, 12)
    private val bu = FloatArray(BIRDS)
    private val bv = FloatArray(BIRDS)
    private val bw = FloatArray(BIRDS)
    private val jf = IntArray(BIRDS)
    private val jp = FloatArray(BIRDS)
    private val jr = FloatArray(BIRDS)
    private val dens = FloatArray(MCELLS)
    private val reedX = FloatArray(REEDS)
    private val reedTop = IntArray(REEDS)
    private val reedPh = FloatArray(REEDS)
    private val ridge = IntArray(MCOLS)
    private val sunX = 132f
    private val pc = FloatArray(4) { 0f }
    private val zenith = mcol("#151236")
    private val violet = mcol("#3a2160")
    private val rose = mcol("#c4466a")
    private val ember = mcol("#ff8a4a")
    private val haze = mcol("#ffcf86")
    private val sun = mcol("#fff1c8")

    init {
        val rng = mRng(seed, 33)
        for (b in 0 until BIRDS) {
            // A thick folded sheet: most birds near its middle plane, thinning to ragged edges.
            val u = rng.float() * 2f - 1f
            val v = rng.float() * 2f - 1f
            bu[b] = u
            bv[b] = v * (1f - 0.35f * u * u)
            bw[b] = (rng.float() + rng.float() - 1f)
            jf[b] = 3 + rng.int(5)
            jp[b] = rng.float() * MTAU
            jr[b] = 0.6f + rng.float() * 1.6f
        }
        pc[0] = rng.float() * MTAU
        pc[1] = rng.float() * MTAU
        pc[2] = rng.float() * MTAU
        pc[3] = rng.float() * MTAU
        for (k in 0 until REEDS) {
            val left = k < REEDS / 2
            reedX[k] = if (left) rng.float() * 34f else MCOLS - 1f - rng.float() * 30f
            reedTop[k] = 27 + rng.int(12)
            reedPh[k] = rng.float() * MTAU
        }
        for (x in 0 until MCOLS) {
            val h = lat.fbm(x * 0.045f, 3.7f, 3)
            val tree = lat.at(x * 0.31f, 9.1f)
            ridge[x] = HORIZON - 1 - max(0f, (h - 0.3f) * 11f + (tree - 0.5f) * 1.4f).toInt()
        }
        palette =
            MPalette(
                mramp(12, mcol("#06050e"), zenith, violet, mcol("#7a2e6a"), rose) +
                    mramp(10, rose, mcol("#e8605a"), ember, haze, sun) +
                    mramp(8, mcol("#0a0812"), mcol("#1c1430"), mcol("#3a2040"), mcol("#6a3048")) +
                    mramp(6, mcol("#3a1a2a"), mcol("#8a3a3a"), mcol("#d06a40"), mcol("#ffb070")) +
                    mramp(4, mcol("#0c0a10"), mcol("#16121c"), mcol("#221a26"), mcol("#2e2030")),
            )
    }

    private fun skyColor(
        x: Int,
        y: Int,
    ): MCol {
        val v = (y + 0.5f) / HORIZON
        val c =
            when {
                v < 0.35f -> zenith.mix(violet, v / 0.35f)
                v < 0.7f -> violet.mix(rose, (v - 0.35f) / 0.35f)
                else -> rose.mix(ember, min(1f, (v - 0.7f) / 0.3f))
            }
        val dx = (x - sunX) / 60f
        val dy = (y - HORIZON) * M_ROW_U / 40f
        val glow = exp(-(dx * dx + dy * dy) * 1.6f)
        return MCol(
            c.r + (haze.r - c.r) * glow * 0.8f,
            c.g + (haze.g - c.g) * glow * 0.8f,
            c.b + (haze.b - c.b) * glow * 0.8f,
        )
    }

    /** Where bird [b] is at loop phase [phi], in width units, written into [out]. */
    private fun bird(
        b: Int,
        phi: Float,
        out: FloatArray,
    ) {
        // The flock as a whole wanders on a closed path over the reeds.
        val cx = 96f + 42f * sin(phi + pc[0]) + 10f * sin(3f * phi + pc[1])
        val cy = 44f + 5f * sin(2f * phi + pc[2]) + 2f * sin(3f * phi + pc[3])
        // It stretches and breathes.
        val span = 52f + 14f * sin(2f * phi + pc[1])
        val u = bu[b]
        val v = bv[b]
        val w = bw[b]
        var px = u * span
        var py = v * 10f + 6f * sin(2.2f * u + 2f * phi + pc[3]) + 4f * sin(4f * u - 3f * phi)
        var pz = w * 10f
        // A twist that runs along the sheet: where it turns edge-on the birds crowd into a dark band.
        val a = 1.4f * sin(1.6f * u + phi + pc[2]) + 0.9f * sin(2f * phi + pc[0])
        val ca = cos(a)
        val sa = sin(a)
        val qy = py * ca - pz * sa
        pz = py * sa + pz * ca
        py = qy
        // The sheet folds over itself around its middle now and then.
        val fold = 0.55f * sin(phi * 2f + pc[1]) * u
        val cf = cos(fold)
        val sf = sin(fold)
        val qx = px * cf - py * sf
        py = px * sf + py * cf
        px = qx
        // Each bird jitters on its own little loop.
        val j = jf[b] * phi + jp[b]
        px += jr[b] * cos(j)
        py += jr[b] * 0.7f * sin(j)
        out[0] = cx + px * (1f + pz * 0.012f)
        out[1] = cy + py * (1f + pz * 0.012f)
    }

    private val pos = FloatArray(2)

    private fun splat(
        x: Float,
        y: Float,
        w: Float,
    ) {
        val fx = x - 0.5f
        val fy = y / M_ROW_U - 0.5f
        val ix = floor(fx).toInt()
        val iy = floor(fy).toInt()
        val ax = fx - ix
        val ay = fy - iy
        add(ix, iy, w * (1f - ax) * (1f - ay))
        add(ix + 1, iy, w * ax * (1f - ay))
        add(ix, iy + 1, w * (1f - ax) * ay)
        add(ix + 1, iy + 1, w * ax * ay)
    }

    private fun add(
        x: Int,
        y: Int,
        w: Float,
    ) {
        if (x < 0 || x >= MCOLS || y < 0 || y >= HORIZON + 2) return
        dens[y * MCOLS + x] += w
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = MTAU * t / LOOP
        dens.fill(0f)
        for (b in 0 until BIRDS) {
            for (k in 0 until TRAILS) {
                bird(b, phi - k * TRAIL_DT * MTAU / LOOP, pos)
                splat(pos[0], pos[1], if (k == 0) 1f else 0.32f / k)
            }
        }
        // Clouds drift a whole lap of their noise circle per loop.
        val cdx = cos(phi) * 1.3f
        val cdy = sin(phi) * 1.3f
        for (y in 0 until MROWS) {
            for (x in 0 until MCOLS) {
                val i = y * MCOLS + x
                if (y < HORIZON) {
                    val c = skyColor(x, y)
                    var r = c.r
                    var g = c.g
                    var b = c.b
                    // Thin lit cloud streaks in the low sky.
                    if (y in 12 until HORIZON - 3) {
                        val n = lat.fbm(x * 0.022f + cdx, y * 0.42f + cdy, 3)
                        val band = msmooth(0.56f, 0.7f, n) * msmooth(10f, 20f, y.toFloat())
                        r += (1f - r) * band * 0.35f
                        g += (0.75f - g) * band * 0.35f
                        b += (0.6f - b) * band * 0.25f
                    }
                    var cov = 2f
                    // The distant tree line on the horizon.
                    if (y >= ridge[x]) cov = 0f
                    // Birds cut the sky: the denser the flock, the more of the ground shows through.
                    val a = min(1f, dens[i] * 0.8f)
                    if (a > 0.02f) {
                        // Birds darken the sky toward a warm black; the thickest bands open into the night behind.
                        val k = 1f - 0.9f * a
                        r = r * k + 0.05f * a
                        g = g * k + 0.03f * a
                        b = b * k + 0.06f * a
                        if (a > 0.95f && cov > 0f) cov = 1.75f
                    }
                    frame.set(i, r, g, b, cov)
                } else {
                    // Still water mirroring the sky, broken by ripples, with a glitter road under the sun.
                    val my = 2 * HORIZON - y - 1
                    val c = skyColor(x, max(0, my))
                    val depth = (y - HORIZON).toFloat() / (MROWS - HORIZON)
                    val k = 0.62f - 0.25f * depth
                    val rip = lat.at(x * 0.08f + cdx * 2f, y * 1.7f + cdy * 0.5f)
                    var cov =
                        if (rip > 0.62f) {
                            1.55f
                        } else if (rip > 0.4f) {
                            1.8f
                        } else {
                            2f
                        }
                    // The flock shows in the water too, softer.
                    val a = if (my in 0 until MROWS) min(1f, dens[my * MCOLS + x] * 0.4f) else 0f
                    val ka = k * (1f - 0.75f * a)
                    var r = c.r * ka
                    var g = c.g * ka
                    var b = c.b * ka
                    val road = exp(-((x - sunX) / (5f + 9f * depth)).let { it * it })
                    val tw = lat.at(x * 0.5f + cdx * 4f, y * 3.1f + cdy * 4f)
                    var glyph = 0.toChar()
                    if (road * tw > 0.42f) {
                        r = sun.r
                        g = sun.g
                        b = sun.b * 0.9f
                        cov = 2f
                        glyph = if (tw > 0.6f) '═' else '─'
                    }
                    frame.set(i, r, g, b, cov, glyph)
                }
            }
        }
        // The sun, half sunk behind the tree line.
        for (y in HORIZON - 5 until HORIZON) {
            for (x in (sunX - 8f).toInt()..(sunX + 8f).toInt()) {
                val dx = x + 0.5f - sunX
                val dy = (y + 0.5f - HORIZON + 0.4f) * M_ROW_U
                if (dx * dx + dy * dy < 7.5f * 7.5f && y < ridge[x] + 1) {
                    val i = y * MCOLS + x
                    if (dens[i] < 0.5f) frame.set(i, sun.r, sun.g, sun.b, 2f)
                }
            }
        }
        // Reeds in the foreground, swaying, as dark stems against the bright water and sky.
        for (k in 0 until REEDS) {
            val sway = 1.6f * sin(phi * 2f + reedPh[k]) + 0.6f * sin(phi * 5f + reedPh[k] * 2f)
            for (y in reedTop[k] until MROWS) {
                val h = (MROWS - y).toFloat() / (MROWS - reedTop[k])
                val x = (reedX[k] + sway * h * h).toInt()
                if (x < 0 || x >= MCOLS) continue
                val i = y * MCOLS + x
                frame.set(i, 0.06f, 0.04f, 0.07f, 0f)
                if (y == reedTop[k] && x + 1 < MCOLS) frame.set(i + 1, 0.06f, 0.04f, 0.07f, 0f)
            }
        }
        frame.output(grid, palette)
    }
}

/**
 * Boids flock: a murmuration of hundreds of starlings over a reed bed at dusk. The flock is a folded, twisting sheet of
 * birds; wherever it turns edge-on the birds crowd into dark rolling bands, and each bird leaves a short trail. The
 * birds are holes cut in a glowing sky of violet, rose and ember, over still water with a glitter road under the
 * setting sun. Every bird moves on a closed path, so the whole flock repeats once per loop. The seed shapes the flock
 * and the reeds.
 */
public fun boidsFlockPiece(seed: Int = 0): AsciiPiece {
    val scene = Murmuration(seed)
    return asciiPiece(
        name = "boids flock",
        ground = Rgb.fromHex("#07050c"),
        category = "physics",
        cols = MCOLS,
        rows = MROWS,
        fps = 24,
        still = 7f,
        loop = LOOP,
        note = "a starling murmuration rolling over a reed bed at dusk",
    ) { time, grid -> scene.draw(mLoopTime(time, LOOP), grid) }
}

/**
 * A starling murmuration swirling over reeds and still water at dusk.
 *
 * @param seed shapes the flock and the reeds.
 */
@Composable
public fun AsciiBoidsFlock(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A flock of starlings swirling over reeds at dusk",
) {
    val piece = remember(seed) { boidsFlockPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
