package com.jantursky.kascii.gallery.k

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
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 16f
private const val BALLS = 7
private const val DEPTH = 15f

private class Metal(
    seed: Int,
) {
    private val frame = KFrame()
    private val palette: KPalette
    private val ax = FloatArray(BALLS)
    private val ay = FloatArray(BALLS)
    private val fx = IntArray(BALLS)
    private val fy = IntArray(BALLS)
    private val px = FloatArray(BALLS)
    private val py = FloatArray(BALLS)
    private val rad = FloatArray(BALLS)
    private val bx = FloatArray(BALLS)
    private val by = FloatArray(BALLS)
    private val br2 = FloatArray(BALLS)
    private val sky = kcol("#c9d6ea")
    private val zenith = kcol("#4a5a7a")
    private val floor = kcol("#2a2018")
    private val teal = kcol("#3fd6c8")
    private val rose = kcol("#ff7a9a")

    init {
        val rng = kRng(seed, 9)
        for (b in 0 until BALLS) {
            ax[b] = 26f + rng.float() * 50f
            ay[b] = 10f + rng.float() * 22f
            fx[b] = 1 + rng.int(2)
            fy[b] = 1 + rng.int(3)
            px[b] = rng.float() * KTAU
            py[b] = rng.float() * KTAU
            rad[b] = if (b == 0) 14f else 6.5f + rng.float() * 6f
        }
        palette = KPalette(buildPalette())
    }

    private fun buildPalette(): List<KCol> =
        kramp(
            18,
            kcol("#030305"),
            kcol("#15171e"),
            kcol("#3a3f4c"),
            kcol("#7a8296"),
            kcol("#c9d6ea"),
            kcol("#ffffff"),
        ) +
            kramp(8, kcol("#1a2234"), kcol("#4a5a7a"), kcol("#8ea2c8"), kcol("#dce6f8")) +
            kramp(6, kcol("#0e0a08"), kcol("#2a2018"), kcol("#5a4430"), kcol("#9a7a58")) +
            kramp(6, kcol("#0a2a2a"), kcol("#1f8a84"), kcol("#3fd6c8"), kcol("#b8fff6")) +
            kramp(5, kcol("#3a1420"), kcol("#a8405a"), kcol("#ff7a9a"), kcol("#ffd0dc")) +
            kramp(4, kcol("#0a0c12"), kcol("#121620"), kcol("#1c2230"))

    /** The studio around the blobs: a bright sky with two softboxes, a sharp horizon and a dark warm floor. */
    private fun env(
        rx: Float,
        ry: Float,
        rz: Float,
        turn: Float,
        out: FloatArray,
    ) {
        // Turn the studio slowly around the vertical axis.
        val c = cos(turn)
        val s = sin(turn)
        val ex = rx * c + rz * s
        val ez = -rx * s + rz * c
        val up = -ry
        if (up > -0.04f) {
            val k = ksmooth(-0.04f, 0.9f, up)
            var r = sky.r + (zenith.r - sky.r) * k
            var g = sky.g + (zenith.g - sky.g) * k
            var b = sky.b + (zenith.b - sky.b) * k
            // Two tall softboxes and a cool rim panel behind.
            val box1 = ksmooth(0.16f, 0.08f, abs(ex - 0.45f)) * ksmooth(0.1f, 0.25f, up) * ksmooth(0.85f, 0.65f, up)
            val box2 = ksmooth(0.1f, 0.05f, abs(ex + 0.55f)) * ksmooth(0.05f, 0.2f, up) * ksmooth(0.7f, 0.5f, up)
            val rim = ksmooth(0.2f, -0.5f, ez) * 0.6f
            r += (1f - r) * box1 + (rose.r - r) * box2 + teal.r * rim * 0.5f
            g += (1f - g) * box1 + (rose.g - g) * box2 + teal.g * rim * 0.5f
            b += (1f - b) * box1 + (rose.b - b) * box2 + teal.b * rim * 0.5f
            out[0] = r
            out[1] = g
            out[2] = b
        } else {
            val k = ksmooth(-0.04f, -0.7f, up)
            // A floor of tiles running to a bright horizon glow.
            val gx = kfract(ex / max(0.05f, -up) * 0.6f)
            val gz = kfract(ez / max(0.05f, -up) * 0.6f)
            val tile = if (gx < 0.08f || gz < 0.08f) 0.55f else 1f
            val glow = ksmooth(-0.25f, -0.02f, up)
            val warm = (0.45f + 0.9f * (1f - k)) * tile
            out[0] = floor.r * warm * 2.2f + 0.85f * glow
            out[1] = floor.g * warm * 2.2f + 0.62f * glow
            out[2] = floor.b * warm * 2.2f + 0.45f * glow
        }
    }

    private val e = FloatArray(3)

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = KTAU * t / LOOP
        for (b in 0 until BALLS) {
            bx[b] = KCOLS / 2f + ax[b] * sin(fx[b] * phi + px[b])
            by[b] = KROWS * K_ROW_U / 2f + ay[b] * sin(fy[b] * phi + py[b])
            // The radius breathes a little.
            val rr = rad[b] * (1f + 0.12f * sin(2f * phi + b))
            br2[b] = rr * rr
        }
        val turn = phi
        val lx = -0.45f
        val ly = -0.65f
        val lz = 0.61f
        for (y in 0 until KROWS) {
            val wy = (y + 0.5f) * K_ROW_U
            for (x in 0 until KCOLS) {
                val i = y * KCOLS + x
                val wx = x + 0.5f
                var f = 0f
                var gx = 0f
                var gy = 0f
                for (b in 0 until BALLS) {
                    val dx = wx - bx[b]
                    val dy = wy - by[b]
                    val d2 = dx * dx + dy * dy + 0.5f
                    val v = br2[b] / d2
                    f += v
                    val k = -2f * v / d2
                    gx += k * dx
                    gy += k * dy
                }
                if (f < 1f) {
                    background(i, x, y, f)
                    continue
                }
                // Height of the blob surface: exactly a sphere for a lone ball.
                val z = sqrt(1f - 1f / f)
                val inv = 1f / (2f * max(z, 0.05f) * f * f)
                // The surface normal of height DEPTH * z, from the gradient of the field.
                var nx = -gx * inv * DEPTH
                var ny = -gy * inv * DEPTH
                var nz = 1f
                val nl = 1f / sqrt(nx * nx + ny * ny + nz * nz)
                nx *= nl
                ny *= nl
                nz *= nl
                // Mirror the view ray (0, 0, -1) about the normal.
                val rx = 2f * nz * nx
                val ry = 2f * nz * ny
                val rz = 2f * nz * nz - 1f
                env(rx, ry, rz, turn, e)
                val fres = 0.75f + 0.25f * (1f - nz).pow(2)
                var r = e[0] * fres
                var g = e[1] * fres
                var b = e[2] * fres
                val spec = max(0f, rx * lx + ry * ly + rz * lz).pow(60f)
                r += spec * 1.2f
                g += spec * 1.2f
                b += spec * 1.2f
                // Contact darkening where blobs merge (the field is high but the surface is low).
                val crease = ksmooth(0.35f, 0.0f, z) * ksmooth(1.4f, 2.4f, f) * 0.6f
                r *= 1f - crease
                g *= 1f - crease
                b *= 1f - crease
                val edge = ksmooth(1f, 1.12f, f)
                frame.set(i, r, g, b, if (edge < 0.5f) 1.6f else 2f, if (spec > 0.6f) '█' else 0.toChar())
            }
        }
        frame.output(grid, palette)
    }

    private fun background(
        i: Int,
        x: Int,
        y: Int,
        f: Float,
    ) {
        // A dim studio backdrop with a dot grid, warped and lit with a teal glow near the blobs.
        val vy = y / KROWS.toFloat()
        val dx = (x - KCOLS / 2f) / KCOLS
        val vign = 1f - 1.4f * (dx * dx + (vy - 0.5f) * (vy - 0.5f))
        var r = 0.03f * vign + 0.02f
        var g = 0.035f * vign + 0.022f
        var b = 0.05f * vign + 0.03f
        var cov = 0.04f
        var glyph = 0.toChar()
        val near = ksmooth(0.35f, 1f, f)
        if (x % 4 == 0 && y % 2 == 0) {
            cov = 0.6f + 0.4f * near
            glyph = if (near > 0.5f) '+' else '·'
            r += 0.12f * vign
            g += 0.14f * vign
            b += 0.2f * vign
        }
        val halo = near * near * 0.8f
        r += teal.r * halo * 0.35f
        g += teal.g * halo * 0.35f
        b += teal.b * halo * 0.35f
        cov = max(cov, halo * 0.6f)
        // A soft floor reflection band near the bottom.
        if (vy > 0.82f) {
            val k = ksmooth(0.82f, 1f, vy) * 0.08f
            r += floor.r * k * 2f
            g += floor.g * k * 2f
            b += floor.b * k * 2f
            cov = max(cov, k * 2f)
        }
        frame.set(i, r, g, b, cov, glyph)
    }
}

/**
 * Liquid metal: chrome metaballs drift on looping paths, swell and merge, every point of their surface mirroring a
 * slowly turning studio of softboxes, a bright horizon and a warm floor, with sharp specular glints, dark creases where
 * blobs fuse and a teal glow on the dotted backdrop around them. The seed shapes the paths and sizes. Every motion
 * repeats a whole number of times per loop.
 */
public fun liquidMetalPiece(seed: Int = 0): AsciiPiece {
    val scene = Metal(seed)
    return asciiPiece(
        name = "liquid metal",
        ground = Rgb.fromHex("#050508"),
        category = "generative",
        cols = KCOLS,
        rows = KROWS,
        fps = 24,
        still = 3f,
        loop = LOOP,
        note = "chrome metaballs merging and parting, mirroring a turning studio of softboxes",
    ) { time, grid -> scene.draw(kLoopTime(time, LOOP), grid) }
}

/**
 * Chrome metaballs that drift, merge and part, reflecting a slowly turning studio.
 *
 * @param seed shapes the paths and the sizes of the blobs.
 */
@Composable
public fun AsciiLiquidMetal(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Chrome liquid-metal blobs merging and parting",
) {
    val piece = remember(seed) { liquidMetalPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
