package com.jantursky.kascii.gallery.j

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
import com.jantursky.kascii.gallery.AKit
import com.jantursky.kascii.gallery.loopTime
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 16f
private const val SPINE = 12
private const val PADS = 7
private const val RIPPLES = 3
private const val KOI_LEN = 40f

private class KoiPond(
    seed: Int,
) {
    private val lat = JLattice(seed, 13)
    private val water = JFrame()
    private val edge = JFrame()
    private val frame = JFrame()
    private val palette: JPalette
    private val padX = FloatArray(PADS)
    private val padY = FloatArray(PADS)
    private val padR = FloatArray(PADS)
    private val padNotch = FloatArray(PADS)
    private val padFlower = BooleanArray(PADS)
    private val rippleX = FloatArray(RIPPLES)
    private val rippleY = FloatArray(RIPPLES)
    private val spineX = FloatArray(SPINE + 1)
    private val spineY = FloatArray(SPINE + 1)
    private val deep = jcol("#06302c")
    private val shallow = jcol("#1a6a5a")
    private val caustic = jcol("#8ae0c8")
    private val white = jcol("#f4f0ea")
    private val red = jcol("#ff5a24")
    private val gold = jcol("#ffb830")
    private val ink = jcol("#141418")
    private val pad = jcol("#3a8a3a")
    private val padLit = jcol("#8ac860")
    private val lotus = jcol("#ff8ab0")

    /** Each koi: path kind, centre x, centre y (rows), half width, half height (rows), laps per loop, phase, pattern. */
    private val koi =
        listOf(
            floatArrayOf(0f, 96f, 24f, 64f, 15f, 1f, 0.05f, 0f),
            floatArrayOf(1f, 100f, 24f, 70f, 13f, 1f, 0.55f, 1f),
            floatArrayOf(0f, 58f, 21f, 32f, 10f, -2f, 0.3f, 2f),
            floatArrayOf(0f, 140f, 28f, 36f, 11f, 2f, 0.8f, 3f),
        )

    init {
        buildWater()
        buildEdge()
        val rng = AKit.rng(seed, 23)
        val spots = listOf(30f to 12f, 72f to 34f, 118f to 10f, 150f to 38f, 168f to 18f, 44f to 40f, 96f to 22f)
        for (k in 0 until PADS) {
            padX[k] = spots[k].first + (rng.float() - 0.5f) * 10f
            padY[k] = spots[k].second + (rng.float() - 0.5f) * 4f
            padR[k] = 8f + rng.float() * 5f
            padNotch[k] = rng.float() * JTAU
            padFlower[k] = k % 3 == 0
        }
        for (k in 0 until RIPPLES) {
            rippleX[k] = 40f + rng.float() * 110f
            rippleY[k] = 8f + rng.float() * 32f
        }
        palette = JPalette(buildPalette())
    }

    private fun buildPalette(): List<JCol> =
        jramp(10, jcol("#021412"), deep, jcol("#0e4a44"), shallow, jcol("#3a9a80"), caustic, jcol("#e0fff4")) +
            jramp(5, jcol("#8a8680"), jcol("#c8c4bc"), white) +
            jramp(5, jcol("#7a2008"), jcol("#c83c14"), red, jcol("#ff9a6a")) +
            jramp(4, jcol("#8a5a10"), gold, jcol("#ffe08a")) +
            listOf(ink, jcol("#3a3a44")) +
            jramp(6, jcol("#0c2a10"), jcol("#1e5a24"), pad, padLit, jcol("#c8f0a0")) +
            jramp(4, jcol("#a04a70"), lotus, jcol("#ffe0ec")) +
            jramp(6, jcol("#1a1a1c"), jcol("#3c3c40"), jcol("#6a6a6c"), jcol("#a8a8a4")) +
            listOf(jcol("#ffe070"))

    private fun buildWater() {
        for (y in 0 until JROWS) {
            for (x in 0 until JCOLS) {
                val n = lat.fbm(x * 0.03f, y * 0.08f, 3)
                val c = jcol("#041e1c").mix(deep, 0.6f).mix(jcol("#0e4a44"), jsmooth(0.35f, 0.7f, n))
                water.put(x, y, c, 0.3f + 0.5f * jsmooth(0.3f, 0.75f, n))
            }
        }
    }

    private fun rock(
        cx: Float,
        cy: Float,
        rx: Float,
        ry: Float,
    ) {
        edge.ellipse(cx, cy, rx, ry) { a, b ->
            val lit = jclamp(0.55f - 0.4f * a - 0.45f * b)
            val n = lat.at(cx + a * 3f, cy + b * 3f)
            jcol("#2a2a2e").mix(jcol("#a8a8a4"), lit * (0.8f + 0.3f * n))
        }
    }

    private fun buildEdge() {
        // Stones and moss round two corners of the pond.
        val stones =
            listOf(
                floatArrayOf(6f, 44f, 14f, 5f),
                floatArrayOf(22f, 46f, 10f, 3.5f),
                floatArrayOf(-2f, 36f, 9f, 5f),
                floatArrayOf(36f, 47.5f, 9f, 2.5f),
                floatArrayOf(186f, 3f, 14f, 5f),
                floatArrayOf(170f, 1f, 10f, 3f),
                floatArrayOf(192f, 12f, 8f, 5f),
            )
        for (s in stones) {
            // A dark wet halo first so the stones sit in the water.
            edge.ellipse(s[0] + 1f, s[1] + 0.6f, s[2] + 2f, s[3] + 0.8f) { _, _ -> jcol("#041412") }
        }
        for (s in stones) rock(s[0], s[1], s[2], s[3])
        val rng = AKit.rng(2, 5)
        repeat(40) {
            val corner = rng.int(2)
            val x = if (corner == 0) rng.int(34) else 160 + rng.int(32)
            val y = if (corner == 0) 38 + rng.int(10) else rng.int(8)
            if (edge.cov[y * JCOLS + x] > 1.4f) {
                edge.put(
                    x,
                    y,
                    jcol("#5a9a3a").mix(jcol("#a0d060"), rng.float()),
                    1f,
                    if (rng.float() <
                        0.5f
                    ) {
                        '"'
                    } else {
                        '\''
                    },
                )
            }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = JTAU * t / LOOP
        frame.copyFrom(water)
        caustics(phi)
        ripples(t)
        for (k in koi.indices) fish(k, t, shadow = true)
        for (k in koi.indices) fish(k, t, shadow = false)
        pads(phi)
        frame.over(edge)
        frame.output(grid, palette)
    }

    private fun caustics(phi: Float) {
        val ox = 3f * cos(phi)
        val oy = 3f * sin(phi)
        val ox2 = 2.5f * cos(2f * phi + 1f)
        val oy2 = -2.5f * sin(2f * phi + 1f)
        for (y in 0 until JROWS) {
            for (x in 0 until JCOLS) {
                val a = lat.at(x * 0.11f + ox, y * 0.26f + oy)
                val b = lat.at(x * 0.09f + 40f + ox2, y * 0.22f + oy2)
                val ra = 1f - abs(a - 0.5f) * 2f
                val rb = 1f - abs(b - 0.5f) * 2f
                val k = (ra * ra * ra * ra * ra * ra) * 0.6f + (rb * rb * rb * rb * rb * rb) * 0.5f
                if (k < 0.16f) continue
                frame.light(y * JCOLS + x, caustic, (k - 0.12f) * 1.3f)
            }
        }
    }

    private fun ripples(t: Float) {
        for (k in 0 until RIPPLES) {
            val life = jfract(t / (LOOP / 2f) + k / RIPPLES.toFloat())
            val radius = 2f + life * 30f
            val fade = (1f - life) * jsmooth(0f, 0.06f, life)
            if (fade < 0.05f) continue
            for (ring in 0..0) {
                val r = radius - ring * 5f
                if (r < 1f) continue
                val y0 = max(0, (rippleY[k] - (r + 2f) / JROW_U).toInt())
                val y1 = min(JROWS - 1, (rippleY[k] + (r + 2f) / JROW_U).toInt() + 1)
                for (y in y0..y1) {
                    for (x in max(0, (rippleX[k] - r - 2f).toInt())..min(JCOLS - 1, (rippleX[k] + r + 2f).toInt())) {
                        val dx = x + 0.5f - rippleX[k]
                        val dy = (y + 0.5f - rippleY[k]) * JROW_U
                        val e = abs(sqrt(dx * dx + dy * dy) - r)
                        if (e > 1.2f) continue
                        val s = fade * (1f - e / 1.2f) * (if (ring == 0) 1f else 0.55f)
                        if (s < 0.15f) continue
                        frame.put(
                            x,
                            y,
                            caustic.mix(white, s * 0.5f).scale(0.5f + 0.5f * s),
                            1f,
                            if (abs(dx) >
                                abs(dy)
                            ) {
                                (if (dx > 0f) ')' else '(')
                            } else {
                                '-'
                            },
                        )
                    }
                }
            }
        }
    }

    /** Position of a path at parameter [s] (one lap per unit), x in cells and y in width units. */
    private fun pathAt(
        k: Int,
        s: Float,
        out: FloatArray,
    ) {
        val p = koi[k]
        val a = JTAU * s
        if (p[0] == 0f) {
            out[0] = p[1] + p[3] * cos(a)
            out[1] = (p[2] + p[4] * sin(a)) * JROW_U
        } else {
            out[0] = p[1] + p[3] * sin(a)
            out[1] = (p[2] + p[4] * sin(2f * a)) * JROW_U
        }
    }

    private val tmp = FloatArray(2)
    private val tmp2 = FloatArray(2)

    private fun fish(
        k: Int,
        t: Float,
        shadow: Boolean,
    ) {
        val p = koi[k]
        val laps = p[5]
        val s = laps * t / LOOP + p[6]
        // Parameter step per spine joint from the local speed of the path.
        pathAt(k, s, tmp)
        pathAt(k, s + 0.001f * (if (laps > 0f) 1f else -1f), tmp2)
        val speed = sqrt((tmp2[0] - tmp[0]) * (tmp2[0] - tmp[0]) + (tmp2[1] - tmp[1]) * (tmp2[1] - tmp[1])) / 0.001f
        val step = KOI_LEN / SPINE / max(speed, 1f) * (if (laps > 0f) 1f else -1f)
        val wig = JTAU * 24f * t / LOOP + k
        for (j in 0..SPINE) {
            pathAt(k, s - j * step, tmp)
            pathAt(k, s - j * step + step * 0.5f, tmp2)
            val dx = tmp2[0] - tmp[0]
            val dy = tmp2[1] - tmp[1]
            val len = max(1e-3f, sqrt(dx * dx + dy * dy))
            val u = j / SPINE.toFloat()
            val side = 1.6f * sin(wig - j * 0.6f) * u * u
            spineX[j] = tmp[0] - dy / len * side
            spineY[j] = tmp[1] + dx / len * side
        }
        val ox = if (shadow) 3f else 0f
        val oy = if (shadow) 3.5f else 0f
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (j in 0..SPINE) {
            minX = min(minX, spineX[j])
            maxX = max(maxX, spineX[j])
            minY = min(minY, spineY[j])
            maxY = max(maxY, spineY[j])
        }
        val pad = 8f
        for (y in max(0, ((minY + oy - pad) / JROW_U).toInt())..min(JROWS - 1, ((maxY + oy + pad) / JROW_U).toInt())) {
            for (x in max(0, (minX + ox - pad).toInt())..min(JCOLS - 1, (maxX + ox + pad).toInt())) {
                val px = x + 0.5f - ox
                val py = (y + 0.5f) * JROW_U - oy
                var best = Float.MAX_VALUE
                var bu = 0f
                var bside = 0f
                var beyond = false
                for (j in 0 until SPINE) {
                    val ax = spineX[j]
                    val ay = spineY[j]
                    val vx = spineX[j + 1] - ax
                    val vy = spineY[j + 1] - ay
                    val l2 = max(1e-4f, vx * vx + vy * vy)
                    val q0 = ((px - ax) * vx + (py - ay) * vy) / l2
                    val q = jclamp(q0)
                    val ex = px - ax - vx * q
                    val ey = py - ay - vy * q
                    val d = ex * ex + ey * ey
                    if (d < best) {
                        best = d
                        bu = (j + q) / SPINE
                        bside = vx * ey - vy * ex
                        beyond = (j == 0 && q0 < 0f) || (j == SPINE - 1 && q0 > 1f)
                    }
                }
                val d = sqrt(best)
                val w = width(bu)
                val fin = bu in 0.18f..0.32f && d < w + 3.6f && d >= w
                val tail = !beyond && bu > 0.84f && d < 1f + 7f * (bu - 0.84f) / 0.16f
                if (d > w && !fin && !tail) continue
                val i = y * JCOLS + x
                if (shadow) {
                    frame.r[i] *= 0.5f
                    frame.g[i] *= 0.5f
                    frame.b[i] *= 0.5f
                    frame.cov[i] = max(frame.cov[i], 0.6f)
                    continue
                }
                val body = d <= w
                var c = skin(k, bu, if (bside > 0f) d else -d)
                if (!body) c = c.mix(JCol(frame.r[i], frame.g[i], frame.b[i]), 0.55f)
                // Shading across the round back, and the water over the fish.
                val round = if (body) 1f - 0.35f * (d / max(w, 0.5f)).pow(2f) else 1f
                c = c.scale(round).mix(deep, 0.12f)
                if (bu < 0.07f && d < w * 0.9f && abs(d - w * 0.55f) < 0.9f) c = ink
                frame.put(x, y, c, if (body) 2f else 1.7f)
            }
        }
    }

    private fun width(u: Float): Float =
        if (u < 0.16f) {
            2f + 4.6f * sqrt(u / 0.16f)
        } else if (u < 0.86f) {
            6.6f * (1f - 0.8f * ((u - 0.16f) / 0.7f).pow(1.4f))
        } else {
            1f
        }

    /** Skin color of koi [k] along the body ([u] 0 at the head) at signed distance [d] from the spine. */
    private fun skin(
        k: Int,
        u: Float,
        d: Float,
    ): JCol {
        val n = lat.at(u * 7f + k * 31f, d * 0.35f + k * 7f)
        return when (koi[k][7].toInt()) {
            // Kohaku: white with red saddles.
            0 -> {
                if (n > 0.5f && u < 0.8f) red else white
            }

            // Yamabuki ogon: all gold with a pale sheen down the back.
            1 -> {
                gold.mix(jcol("#ffe08a"), jsmooth(2f, 0f, abs(d)) * 0.6f)
            }

            // Showa: black ground with red and white.
            2 -> {
                if (n > 0.6f) {
                    red
                } else if (n < 0.35f) {
                    white
                } else {
                    ink.mix(jcol("#3a3a44"), 0.4f)
                }
            }

            // Tancho: white with a red crown.
            else -> {
                if (u < 0.14f && abs(d) < 2.4f && u > 0.03f) red else white
            }
        }
    }

    private fun pads(phi: Float) {
        for (k in 0 until PADS) {
            val cx = padX[k] + 0.6f * sin(phi + k)
            val cy = padY[k] + 0.2f * cos(phi + k * 2f)
            val r = padR[k]
            val notch = padNotch[k] + 0.15f * sin(phi * 2f + k)
            val ry = r / JROW_U
            for (y in max(0, (cy - ry - 1f).toInt())..min(JROWS - 1, (cy + ry + 1f).toInt())) {
                for (x in max(0, (cx - r - 1f).toInt())..min(JCOLS - 1, (cx + r + 1f).toInt())) {
                    val dx = x + 0.5f - cx
                    val dy = (y + 0.5f - cy) * JROW_U
                    val d = sqrt(dx * dx + dy * dy)
                    if (d > r) continue
                    val a = atan2(dy, dx)
                    var da = abs(a - notch)
                    if (da > JTAU / 2f) da = JTAU - da
                    if (da < 0.28f && d > r * 0.12f) continue
                    val vein = abs(sin((a - notch) * 4.5f)) < 0.16f && d > r * 0.2f && d < r * 0.85f
                    val rim = d > r - 1.5f
                    val lit = jclamp(0.55f - 0.35f * dx / r - 0.35f * dy / r)
                    var c = jcol("#1e5a24").mix(padLit, lit)
                    if (rim) c = c.mix(jcol("#0c2a10"), 0.45f)
                    if (vein) c = c.scale(0.75f)
                    frame.put(x, y, c, 2f)
                }
            }
            if (padFlower[k]) lotusAt(cx + r * 0.15f, cy - 0.3f)
        }
    }

    private fun lotusAt(
        cx: Float,
        cy: Float,
    ) {
        for (y in (cy - 2f).toInt()..(cy + 2f).toInt()) {
            for (x in (cx - 5f).toInt()..(cx + 5f).toInt()) {
                val dx = x + 0.5f - cx
                val dy = (y + 0.5f - cy) * JROW_U
                val d = sqrt(dx * dx + dy * dy)
                val a = atan2(dy, dx)
                val petal = 3.6f + 1.6f * abs(cos(a * 3f))
                if (d > petal) continue
                val c = if (d < 1.4f) jcol("#ffe070") else lotus.mix(jcol("#ffe0ec"), jclamp(1f - d / petal) * 0.8f)
                frame.put(x, y, c, 2f)
            }
        }
    }
}

/**
 * Koi swim in loops through a green pond seen from above, a kohaku, a gold ogon, a showa and a tancho, sliding under
 * lily pads while caustic light wobbles over the bottom, rings ripple out and their shadows follow below. The seed
 * places the pads, the ripples and the koi markings. Every motion repeats a whole number of times per loop.
 */
public fun koiPondPiece(seed: Int = 0): AsciiPiece {
    val scene = KoiPond(seed)
    return asciiPiece(
        name = "koi pond",
        ground = Rgb.fromHex("#02100e"),
        category = "creatures",
        cols = JCOLS,
        rows = JROWS,
        fps = 15,
        still = 1f,
        loop = LOOP,
        note = "koi swim in loops under lily pads in a pond seen from above, caustics wobbling and rings rippling out",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * Koi swimming in loops under lily pads in a pond seen from above, with caustic light and ripples.
 *
 * @param seed places the lily pads, the ripples and the markings of the koi.
 */
@Composable
public fun AsciiKoiPond(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Koi swimming in loops under lily pads in a pond seen from above",
) {
    val piece = remember(seed) { koiPondPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
