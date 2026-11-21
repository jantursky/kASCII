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
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 24f
private const val WAVES = 7
private const val LAMBDA = 13f

private class Turing(
    seed: Int,
) {
    private val frame = MFrame()
    private val palette: MPalette
    private val lat = MLattice(seed, 5)
    private val warp = MLattice(seed, 6)
    private val kx = FloatArray(WAVES)
    private val ky = FloatArray(WAVES)
    private val p0 = FloatArray(WAVES)
    private val wind = IntArray(WAVES)
    private val deep = mcol("#04101a")
    private val plumDeep = mcol("#12061a")
    private val haze = mcol("#1a4a5a")
    private val rimA = mcol("#127a86")
    private val rimB = mcol("#9a2058")
    private val cyan = mcol("#38d8c8")
    private val rose = mcol("#ff5a7e")
    private val mint = mcol("#d8fff2")
    private val gold = mcol("#ffd890")

    init {
        val rng = mRng(seed, 21)
        val k = MTAU / LAMBDA
        val base = rng.float() * MTAU
        for (j in 0 until WAVES) {
            val a = base + j * (MTAU / 2f) / WAVES + (rng.float() - 0.5f) * 0.25f
            kx[j] = k * cos(a)
            ky[j] = k * sin(a)
            p0[j] = rng.float() * MTAU
            wind[j] = (1 + rng.int(2)) * (if (rng.float() < 0.5f) -1 else 1)
        }
        palette =
            MPalette(
                mramp(8, mcol("#020509"), deep, mcol("#0c2a36"), haze) +
                    mramp(5, plumDeep, mcol("#2a0e30"), mcol("#4a1640")) +
                    mramp(8, mcol("#06303a"), rimA, cyan, mint) +
                    mramp(8, mcol("#3a0a24"), rimB, rose, mcol("#ffb0c0")) +
                    mramp(6, mcol("#ff8a7a"), mcol("#ffb48a"), gold, mcol("#fff6e0")) +
                    mramp(5, mcol("#5ab8b0"), mcol("#9ae8d8"), mcol("#eafff8")),
            )
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = MTAU * t / LOOP
        val cp = cos(phi)
        val sp = sin(phi)
        // The slow drift of every wave, a whole number of turns per loop.
        val ph = FloatArray(WAVES) { p0[it] + wind[it] * phi }
        val norm = 1f / sqrt(WAVES / 2f)
        for (y in 0 until MROWS) {
            val wy = (y + 0.5f) * M_ROW_U
            for (x in 0 until MCOLS) {
                val i = y * MCOLS + x
                val wx = x + 0.5f
                // A gentle warp so the pattern curls instead of tiling straight.
                val ox = (warp.at(wx * 0.035f + cp * 0.9f, wy * 0.035f + sp * 0.9f) - 0.5f) * 9f
                val oy = (warp.at(wx * 0.035f + 31f - sp * 0.9f, wy * 0.035f + cp * 0.9f) - 0.5f) * 9f
                val px = wx + ox
                val py = wy + oy
                var p = 0f
                for (j in 0 until WAVES) p += cos(kx[j] * px + ky[j] * py + ph[j])
                p *= norm
                // Where the chemistry is "fed" more, spots fuse into stripes and mazes; elsewhere they stay apart.
                val feed = lat.fbm(wx * 0.016f + cp * 0.55f, wy * 0.016f + sp * 0.55f, 2)
                val th = -0.1f + 1.2f * msmooth(0.3f, 0.72f, feed)
                val v = p - th
                // Two chemistries: teal on the left, rose on the right, with a wandering border.
                val s0 = wx / MCOLS + (lat.at(wx * 0.012f + 50f - sp * 0.3f, wy * 0.012f + cp * 0.3f) - 0.5f) * 0.9f
                val side = msmooth(0.38f, 0.62f, s0)
                val dx = (wx - 96f) / 96f
                val dy = (wy - 52.8f) / 52.8f
                val light = max(0.45f, 1.08f - 0.45f * (dx * dx + dy * dy))
                val rimR = rimA.r + (rimB.r - rimA.r) * side
                val rimG = rimA.g + (rimB.g - rimA.g) * side
                val rimB2 = rimA.b + (rimB.b - rimA.b) * side
                if (v > 0f) {
                    // A dark saturated rim, a bright body and a pale hot core, so each spot reads as a soft bead.
                    val body = msmooth(0f, 0.35f, v)
                    val core = msmooth(0.35f, 1.3f, v)
                    val midR = cyan.r + (rose.r - cyan.r) * side
                    val midG = cyan.g + (rose.g - cyan.g) * side
                    val midB = cyan.b + (rose.b - cyan.b) * side
                    val hiR = mint.r + (gold.r - mint.r) * side
                    val hiG = mint.g + (gold.g - mint.g) * side
                    val hiB = mint.b + (gold.b - mint.b) * side
                    var r = rimR + (midR - rimR) * body
                    var g = rimG + (midG - rimG) * body
                    var b = rimB2 + (midB - rimB2) * body
                    r += (hiR - r) * core
                    g += (hiG - g) * core
                    b += (hiB - b) * core
                    val cov =
                        if (v < 0.06f) {
                            1.6f
                        } else if (v < 0.14f) {
                            1.8f
                        } else {
                            2f
                        }
                    frame.set(i, r * light, g * light, b * light, cov)
                } else {
                    // The ground: dark, with a glow hugging every edge and a faint ripple of the field beneath.
                    val glow = exp(v * 6f)
                    val ripple = 0.5f + 0.5f * p
                    val tint = msmooth(-1.4f, -0.3f, v) * ripple
                    var r = deep.r + (plumDeep.r - deep.r) * side + haze.r * tint * 0.3f
                    var g = deep.g + (plumDeep.g - deep.g) * side + haze.g * tint * 0.3f
                    var b = deep.b + (plumDeep.b - deep.b) * side + haze.b * tint * 0.3f
                    r += rimR * glow * 0.9f * light
                    g += rimG * glow * 0.9f * light
                    b += rimB2 * glow * 0.9f * light
                    val cov = max(0.04f + 0.22f * tint, glow * 0.9f)
                    frame.set(i, r, g, b, cov)
                }
            }
        }
        frame.output(grid, palette)
    }
}

/**
 * Reaction diffusion: a Turing pattern of spots, stripes and mazes, two chemistries side by side in teal and mint and
 * in rose and gold. The pattern is a warped superposition of waves of one wavelength, cut by a slowly drifting feed
 * level, so spots grow, fuse into stripes and break apart again while the waves slide. The seed sets the wave
 * directions and the feed map. Every drift turns a whole number of times per loop.
 */
public fun reactionDiffusionPiece(seed: Int = 0): AsciiPiece {
    val scene = Turing(seed)
    return asciiPiece(
        name = "reaction diffusion",
        ground = Rgb.fromHex("#03080d"),
        category = "generative",
        cols = MCOLS,
        rows = MROWS,
        fps = 24,
        still = 5f,
        loop = LOOP,
        note = "a Turing pattern whose spots grow, fuse into stripes and part again",
    ) { time, grid -> scene.draw(mLoopTime(time, LOOP), grid) }
}

/**
 * A glowing Turing pattern of spots and stripes that grow, fuse and part.
 *
 * @param seed sets the wave directions and the feed map of the pattern.
 */
@Composable
public fun AsciiReactionDiffusion(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A glowing Turing pattern of spots and stripes that grow and fuse",
) {
    val piece = remember(seed) { reactionDiffusionPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
