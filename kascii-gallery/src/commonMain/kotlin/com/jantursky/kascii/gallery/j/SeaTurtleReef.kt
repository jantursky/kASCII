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
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 16f
private const val SCHOOL = 26
private const val DARTERS = 12
private const val BUBBLES = 24
private const val SCUTES = 9
private const val ANEMONES = 5
private const val TX = 92f
private const val TY = 19f

private class SeaTurtleReef(
    seed: Int,
) {
    private val lat = JLattice(seed, 53)
    private val sea = JFrame()
    private val reef = JFrame()
    private val frame = JFrame()
    private val palette: JPalette
    private val fishDx = FloatArray(SCHOOL)
    private val fishDy = FloatArray(SCHOOL)
    private val fishPhase = FloatArray(SCHOOL)
    private val dartY = FloatArray(DARTERS)
    private val dartPhase = FloatArray(DARTERS)
    private val bubX = FloatArray(BUBBLES)
    private val bubPhase = FloatArray(BUBBLES)
    private val bubRate = IntArray(BUBBLES)
    private val scuteX = FloatArray(SCUTES)
    private val scuteY = FloatArray(SCUTES)
    private val anemX = FloatArray(ANEMONES)
    private val anemY = FloatArray(ANEMONES)
    private val shell = jcol("#6a5424")
    private val shellLit = jcol("#c8a050")
    private val seam = jcol("#e8d090")
    private val skin = jcol("#6e7c58")
    private val skinLit = jcol("#c0c890")
    private val ray = jcol("#9ae8f0")
    private val coralPink = jcol("#ff6a8a")
    private val coralOrange = jcol("#ff9a3a")
    private val coralPurple = jcol("#a46aff")
    private val coralYellow = jcol("#ffd848")
    private val fishYellow = jcol("#ffd020")
    private val fishBlue = jcol("#3ac8ff")

    init {
        buildSea()
        buildReef(seed)
        val rng = AKit.rng(seed, 59)
        for (k in 0 until SCHOOL) {
            val a = rng.float() * JTAU
            val r = sqrt(rng.float())
            fishDx[k] = cos(a) * r * 22f
            fishDy[k] = sin(a) * r * 4.5f
            fishPhase[k] = rng.float()
        }
        for (k in 0 until DARTERS) {
            dartY[k] = 26f + rng.float() * 8f
            dartPhase[k] = rng.float()
        }
        for (k in 0 until BUBBLES) {
            bubX[k] = 10f + rng.float() * 172f
            bubPhase[k] = rng.float()
            bubRate[k] = 1 + rng.int(2)
        }
        // Scute centres on the shell, in shell units (-1..1).
        val sx = floatArrayOf(-0.62f, -0.2f, 0.22f, 0.6f, -0.42f, 0f, 0.42f, -0.75f, 0.78f)
        val sy = floatArrayOf(-0.35f, -0.55f, -0.55f, -0.35f, 0.15f, 0.1f, 0.15f, 0.3f, 0.25f)
        for (k in 0 until SCUTES) {
            scuteX[k] = sx[k]
            scuteY[k] = sy[k]
        }
        palette = JPalette(buildPalette())
    }

    private fun buildPalette(): List<JCol> {
        val list = ArrayList<JCol>()
        list += jramp(12, jcol("#021020"), jcol("#06284a"), jcol("#0e5a8a"), jcol("#2a9ab8"), ray, jcol("#e8ffff"))
        list += jramp(6, jcol("#2a2410"), shell, shellLit, seam)
        list += jramp(6, jcol("#2a3220"), skin, skinLit, jcol("#eef0d0"))
        for (c in listOf(coralPink, coralOrange, coralPurple, coralYellow)) {
            list +=
                jramp(4, c.scale(0.35f), c.scale(0.7f), c, c.mix(jcol("#ffffff"), 0.4f))
        }
        list += jramp(3, fishYellow.scale(0.6f), fishYellow, jcol("#fff4b0"))
        list += jramp(3, fishBlue.scale(0.5f), fishBlue, jcol("#c0f0ff"))
        list += jramp(5, jcol("#0a2a3a"), jcol("#1a4a5a"), jcol("#2a6a6a"), jcol("#4a8a7a"))
        list += listOf(jcol("#0a0a10"), jcol("#ffffff"), jcol("#2a7a4a"), jcol("#5ab06a"))
        return list
    }

    private fun buildSea() {
        for (y in 0 until JROWS) {
            val d = y / (JROWS - 1f)
            val c =
                if (d < 0.45f) {
                    jcol("#2a9ab8").mix(jcol("#0e5a8a"), d / 0.45f)
                } else {
                    jcol("#0e5a8a").mix(jcol("#06284a"), (d - 0.45f) / 0.55f)
                }
            for (x in 0 until JCOLS) {
                val n = lat.fbm(x * 0.05f, y * 0.2f, 3) - 0.5f
                sea.put(x, y, c, jclamp(0.62f - 0.45f * d + n * 0.4f))
            }
        }
        // Far reef in blue haze.
        for (x in 0 until JCOLS) {
            val top = 33f - 5f * lat.fbm(x * 0.06f + 30f, 4f, 3)
            for (y in top.toInt() until JROWS) {
                val n = lat.at(x * 0.4f, y * 0.8f)
                sea.put(x, y, jcol("#0e3a5a").mix(jcol("#1a5a7a"), n * 0.6f), 0.55f + 0.4f * n)
            }
        }
    }

    private fun buildReef(seed: Int) {
        val rng = AKit.rng(seed, 71)
        // Sand floor.
        for (x in 0 until JCOLS) {
            val top = 44f + 1.2f * sin(x * 0.07f) + 0.8f * sin(x * 0.19f + 2f)
            for (y in top.toInt() until JROWS) {
                val n = lat.at(x * 0.5f + 9f, y * 1.4f)
                reef.put(x, y, jcol("#8a8a6a").mix(jcol("#c8c098"), n * 0.6f).scale(0.75f), if (n > 0.6f) 1.8f else 2f)
            }
        }
        // Brain corals: domes with a maze texture.
        repeat(6) {
            val cx = 8f + it * 34f + rng.float() * 14f
            val cy = 43f + rng.float() * 2f
            val r = 7f + rng.float() * 6f
            val c = if (it % 2 == 0) coralOrange else coralYellow
            reef.ellipse(cx, cy, r, r * 0.5f / JROW_U * 1.6f) { a, b ->
                val maze = sin((a * 7f + lat.at(a * 3f + cx, b * 3f) * 4f) * 2f) * sin(b * 9f)
                val lit = jclamp(0.85f - 0.5f * b - 0.2f * a)
                c.scale(0.4f + 0.6f * lit).scale(if (maze > 0.3f) 0.75f else 1f)
            }
        }
        // Staghorn corals: branching fingers.
        repeat(5) {
            val bx = 20f + it * 38f + rng.float() * 10f
            branch(
                bx,
                45f,
                -1.5708f,
                9f + rng.float() * 3f,
                3,
                if (it % 2 ==
                    0
                ) {
                    coralPink
                } else {
                    coralPurple
                },
                rng.float(),
            )
        }
        // Sea fans: a fine net of lines.
        repeat(3) {
            val fx = 30f + it * 62f + rng.float() * 12f
            val base = 44f
            for (y in 0 until 14) {
                val half = if (y < 3) 0.6f else 8f * sqrt(jclamp(1f - ((y - 8.5f) / 5.6f).let { it * it }))
                for (x in (fx - half).toInt()..(fx + half).toInt()) {
                    val c = coralPurple.mix(coralPink, y / 14f).scale(0.75f + 0.25f * (y / 14f))
                    reef.put(
                        x,
                        (base - y).toInt(),
                        if (((x + y) and 1) ==
                            0
                        ) {
                            c.scale(0.7f)
                        } else {
                            c
                        },
                        if (((x + y) and 1) == 0) 1.8f else 2f,
                    )
                }
            }
        }
        for (k in 0 until ANEMONES) {
            anemX[k] = 14f + k * 40f + rng.float() * 16f
            anemY[k] = 44f
        }
    }

    private fun branch(
        x: Float,
        y: Float,
        a: Float,
        len: Float,
        depth: Int,
        c: JCol,
        r: Float,
    ) {
        val ex = x + cos(a) * len
        val ey = y + sin(a) * len / JROW_U
        reef.segment(x, y, ex, ey, 1.6f, 1.1f) { s, _ -> c.scale(0.6f + 0.4f * s) }
        reef.put(ex.toInt(), ey.toInt(), c.mix(jcol("#ffffff"), 0.4f), 2f)
        if (depth == 0) return
        val spread = 0.55f + r * 0.2f
        branch(ex, ey, a - spread, len * 0.72f, depth - 1, c, jfract(r * 7.3f))
        branch(ex, ey, a + spread * 0.9f, len * 0.68f, depth - 1, c, jfract(r * 3.1f))
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = JTAU * t / LOOP
        frame.copyFrom(sea)
        rays(phi)
        darters(t)
        frame.over(reef)
        anemones(phi)
        bubbles(t)
        turtle(t)
        school(t)
        frame.output(grid, palette)
    }

    private fun rays(phi: Float) {
        for (y in 0 until 38) {
            val fade = (1f - y / 38f).pow(1.2f)
            for (x in 0 until JCOLS) {
                val xs = x + y * JROW_U * 0.35f
                var s = 0f
                for (q in 0 until 4) {
                    val c = 30f + q * 46f + 5f * sin(phi + q * 1.7f)
                    val e = (xs - c) / (3.5f + q % 2 * 2f)
                    if (e * e < 6f) s += exp(-e * e) * (0.7f + 0.3f * sin(2f * phi + q))
                }
                if (s < 0.04f) continue
                frame.light(y * JCOLS + x, ray, min(1f, s) * fade * 0.9f)
            }
        }
        for (x in 0 until JCOLS) {
            val w = sin(x * 0.23f + phi * 3f) + 0.6f * sin(x * 0.09f - phi * 2f)
            frame.put(
                x,
                0,
                jcol("#c8f8ff").mix(jcol("#5ac0d8"), jclamp(0.5f - w * 0.4f)),
                1f,
                if (w >
                    0.9f
                ) {
                    '^'
                } else {
                    '~'
                },
            )
        }
    }

    private fun anemones(phi: Float) {
        for (k in 0 until ANEMONES) {
            for (j in 0 until 7) {
                val bx = anemX[k] + (j - 3) * 1.4f
                val sway = 1.8f * sin(phi * 2f + k + j * 0.4f)
                for (q in 0 until 5) {
                    val p = q / 4f
                    val x = bx + sway * p * p + (j - 3) * 0.5f * p
                    val y = anemY[k] - 1f - q
                    val c = jcol("#2a7a4a").mix(coralPink, p).mix(jcol("#ffffff"), if (q == 4) 0.3f else 0f)
                    frame.put(x.toInt(), y.toInt(), c, 2f)
                }
            }
        }
    }

    private fun bubbles(t: Float) {
        for (k in 0 until BUBBLES) {
            val life = jfract(bubPhase[k] + bubRate[k] * t / LOOP)
            val y = 44f - life * 46f
            val x = bubX[k] + 1.5f * sin(JTAU * (4f * t / LOOP + bubPhase[k]))
            if (y < 1f) continue
            val big = (k % 4 == 0)
            frame.put(
                x.toInt(),
                y.toInt(),
                jcol("#c0f0ff").scale(0.6f + 0.4f * life),
                1f,
                if (big &&
                    life > 0.4f
                ) {
                    'O'
                } else if (life > 0.2f) {
                    'o'
                } else {
                    '.'
                },
            )
        }
    }

    private fun school(t: Float) {
        val s = jfract(t / LOOP)
        val cx = -30f + s * (JCOLS + 60f)
        val cy = 10f + 2.5f * sin(JTAU * 2f * t / LOOP)
        for (k in 0 until SCHOOL) {
            val wig = sin(JTAU * (6f * t / LOOP + fishPhase[k]))
            val x = cx + fishDx[k] + 2f * wig
            val y = cy + fishDy[k] + 0.6f * sin(JTAU * (3f * t / LOOP + fishPhase[k] * 2f))
            val xi = x.toInt()
            val yi = y.toInt()
            val c = if (k % 5 == 0) fishBlue else fishYellow
            // A little "><>" fish swimming right.
            frame.put(xi, yi, c.scale(0.8f), 1f, '>')
            frame.put(xi + 1, yi, c, 1f, '<')
            frame.put(xi + 2, yi, c.mix(jcol("#ffffff"), 0.3f), 1f, '>')
        }
    }

    private fun darters(t: Float) {
        for (k in 0 until DARTERS) {
            val s = jfract(2f * t / LOOP + dartPhase[k])
            val x = JCOLS + 10f - s * (JCOLS + 20f)
            val y = dartY[k] + 1.5f * sin(JTAU * (5f * t / LOOP + dartPhase[k]))
            frame.put(x.toInt(), y.toInt(), fishBlue.scale(0.75f), 1f, '<')
            frame.put(x.toInt() + 1, y.toInt(), fishBlue.scale(0.55f), 1f, '=')
        }
    }

    private fun segDist(
        px: Float,
        py: Float,
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
    ): Pair<Float, Float> {
        val vx = bx - ax
        val vy = by - ay
        val s = jclamp(((px - ax) * vx + (py - ay) * vy) / (vx * vx + vy * vy))
        val ex = px - ax - vx * s
        val ey = py - ay - vy * s
        return Pair(sqrt(ex * ex + ey * ey), s)
    }

    private fun turtle(t: Float) {
        val phi = JTAU * t / LOOP
        val ox = TX + 7f * sin(phi)
        val oy = TY + 1.6f * sin(2f * phi + 0.5f)
        // Two strokes of the front flippers per drift cycle of four seconds.
        val stroke = JTAU * 4f * t / LOOP
        val sweep = sin(stroke)
        val oyU = oy * JROW_U
        for (y in max(0, (oy - 9f).toInt())..min(JROWS - 1, (oy + 13f).toInt())) {
            for (x in max(0, (ox - 40f).toInt())..min(JCOLS - 1, (ox + 40f).toInt())) {
                val lx = x + 0.5f - ox
                val ly = (y + 0.5f) * JROW_U - oyU
                val c = turtleAt(lx, ly, sweep) ?: continue
                frame.put(x, y, c, 2f)
            }
        }
    }

    /** The turtle in local width units: x forward, y down, shell centred at 0. */
    private fun turtleAt(
        lx: Float,
        ly: Float,
        sweep: Float,
    ): JCol? {
        // Near front flipper: a long paddle that sweeps down and back, then up and forward.
        val fa = 0.15f + 0.6f * sweep
        val bx = 9f
        val by = 5f
        val tipX = bx - cos(fa) * 26f
        val tipY = by + sin(fa) * 20f
        val (fd, fs) = segDist(lx, ly, bx, by, tipX, tipY)
        val fw = 5f * (1f - fs * 0.8f)
        if (fd < fw) return flipperShade(lx, ly, fs, fd / fw)
        // Head and neck.
        val hx = 26f
        val hy = 2f
        val he = ((lx - hx) / 6.2f).let { it * it } + ((ly - hy) / 4.2f).let { it * it }
        val (nd, _) = segDist(lx, ly, 14f, 3f, hx - 2f, hy + 0.5f)
        if (he <= 1f || nd < 3.6f) {
            if (abs(lx - (hx + 2.2f)) < 1.1f && abs(ly - (hy - 1.2f)) < 1.2f) return jcol("#0a0a10")
            return scales(lx, ly, (ly - hy) / 4.2f)
        }
        // Shell: a dome with scutes and a pale rim of marginals.
        val sx = lx / 22f
        val sy = (ly - 0.5f) / (if (ly < 0.5f) 10.5f else 5f)
        val se = sx * sx + sy * sy
        if (se <= 1f) {
            if (se > 0.82f && ly > -2f) return seam.mix(shell, 0.45f).scale(0.85f + 0.15f * sx)
            if (ly > 3.5f) return jcol("#d8c88a").scale(0.9f)
            var best = 9f
            var second = 9f
            for (k in 0 until SCUTES) {
                val dx = sx - scuteX[k]
                val dy = sy - scuteY[k]
                val d = dx * dx * 1.2f + dy * dy
                if (d < best) {
                    second = best
                    best = d
                } else if (d < second) {
                    second = d
                }
            }
            val edge = sqrt(second) - sqrt(best) < 0.09f
            val lit = jclamp(0.55f - 0.55f * sy + 0.2f * sx)
            val base = shell.mix(shellLit, lit)
            val streak = lat.at(lx * 0.6f + 20f, ly * 0.4f)
            return if (edge) seam.scale(0.7f + 0.3f * lit) else base.scale(0.85f + 0.3f * streak)
        }
        // Rear flipper.
        val (rd, rs) = segDist(lx, ly, -16f, 4f, -26f, 9f + 2f * sweep)
        if (rd < 3.4f * (1f - rs * 0.6f)) return flipperShade(lx, ly, rs, rd / 3.4f)
        return null
    }

    private fun flipperShade(
        lx: Float,
        ly: Float,
        along: Float,
        across: Float,
    ): JCol {
        val base = scales(lx, ly, across)
        val edge = if (across > 0.75f) 0.7f else 1f
        return base.scale(edge * (1f - along * 0.15f))
    }

    /** Skin with a pattern of pale polygonal scales outlined in darker lines. */
    private fun scales(
        lx: Float,
        ly: Float,
        v: Float,
    ): JCol {
        val n = lat.at(lx * 0.55f + 60f, ly * 0.5f + 60f)
        val lit = jclamp(0.75f - 0.45f * v)
        val c = skin.mix(skinLit, lit * (0.5f + 0.5f * n))
        return if (jfract(n * 5f) < 0.18f) c.scale(0.65f) else c
    }
}

/**
 * A green sea turtle glides over a coral reef, its long front flippers rowing in slow strokes. A school of little
 * yellow fish crosses above, blue darters flick the other way, anemones sway, bubbles rise and sun rays waver down
 * from the surface. The seed shapes the corals and places the fish and bubbles. Every motion repeats a whole number
 * of times per loop.
 */
public fun seaTurtleReefPiece(seed: Int = 0): AsciiPiece {
    val scene = SeaTurtleReef(seed)
    return asciiPiece(
        name = "sea turtle reef",
        ground = Rgb.fromHex("#021020"),
        category = "creatures",
        cols = JCOLS,
        rows = JROWS,
        fps = 15,
        still = 3f,
        loop = LOOP,
        note = "a sea turtle glides over a coral reef as a school of fish crosses above and sun rays waver down",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A sea turtle gliding over a coral reef, with a school of fish, rising bubbles and sun rays.
 *
 * @param seed shapes the corals and places the fish and the bubbles.
 */
@Composable
public fun AsciiSeaTurtleReef(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A sea turtle gliding over a coral reef with a school of fish",
) {
    val piece = remember(seed) { seaTurtleReefPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
