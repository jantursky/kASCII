package com.jantursky.kascii.gallery.i

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
import kotlin.math.sin

private const val LOOP = 8f
private const val CABS = 5
private const val CAB_W = 28
private const val CAB_GAP = 5
private const val CAB_X0 = (ICOLS - CABS * CAB_W - (CABS - 1) * CAB_GAP) / 2
private const val CAB_TOP = 13
private const val SCREEN_TOP = 18
private const val SCREEN_H = 10
private const val FLOOR = 39
private const val SIGN_Y = 2

/** 5 by 7 bitmaps of the sign letters, one string per row, '#' lit. */
private val GLYPHS: Map<Char, List<String>> =
    mapOf(
        'A' to listOf(" ### ", "#   #", "#   #", "#####", "#   #", "#   #", "#   #"),
        'R' to listOf("#### ", "#   #", "#   #", "#### ", "# #  ", "#  # ", "#   #"),
        'C' to listOf(" ####", "#    ", "#    ", "#    ", "#    ", "#    ", " ####"),
        'D' to listOf("#### ", "#   #", "#   #", "#   #", "#   #", "#   #", "#### "),
        'E' to listOf("#####", "#    ", "#    ", "#### ", "#    ", "#    ", "#####"),
    )

private class Arcade(
    seed: Int,
) {
    private val lat = ILattice(seed, 51)
    private val base = IFrame()
    private val frame = IFrame()
    private val palette: IPalette
    private val hues =
        listOf(col("#ff3ea5"), col("#36e2ff"), col("#ffd23a"), col("#8a5cff"), col("#3aff8a"))
    private val pink = col("#ff3ea5")
    private val cyan = col("#36e2ff")
    private val order: IntArray

    init {
        val rng = irng(seed, 53)
        // The seed shuffles which game runs on which cabinet.
        order = IntArray(CABS) { it }
        for (k in CABS - 1 downTo 1) {
            val j = rng.int(k + 1)
            val tmp = order[k]
            order[k] = order[j]
            order[j] = tmp
        }
        buildRoom()
        for (c in 0 until CABS) buildCabinet(c)
        palette =
            IPalette(
                ramp(10, col("#05030a"), col("#140c26"), col("#2a1a4a"), col("#4a3a6e")) +
                    hues.flatMap { tones(it, 9) } +
                    tones(col("#ff8a3a"), 6) +
                    ramp(6, col("#3a3a4a"), col("#8a8aa0"), col("#e8e8f4")) +
                    listOf(Col(1f, 1f, 1f), col("#0a1a10")),
            )
    }

    private fun cabX(c: Int): Int = CAB_X0 + c * (CAB_W + CAB_GAP)

    private fun buildRoom() {
        for (y in 0 until FLOOR) {
            for (x in 0 until ICOLS) {
                // Back wall of dark panels with faint seams.
                val n = lat.at(x * 0.08f, y * 0.2f)
                val seam = x % 24 == 0
                val k = 0.075f + 0.03f * n + 0.03f * (y / FLOOR.toFloat())
                val c = Col(k * 0.7f, k * 0.5f, k * 1.25f)
                base.put(x, y, if (seam) c.scale(0.6f) else c, 2f)
            }
        }
        // Neon strips along the ceiling.
        for (x in 0 until ICOLS) {
            base.put(x, 0, cyan.scale(0.85f), 2f, '▄')
        }
    }

    private fun buildCabinet(c: Int) {
        val x0 = cabX(c)
        val x1 = x0 + CAB_W - 1
        val hue = hues[order[c]]
        for (y in CAB_TOP until FLOOR) {
            for (x in x0..x1) {
                val side = x == x0 || x == x1
                val k = 0.16f + 0.08f * (1f - abs(x - (x0 + x1) / 2f) / (CAB_W / 2f))
                var col = Col(k * 0.55f, k * 0.5f, k * 0.8f)
                if (side) col = hue.scale(0.55f)
                base.put(x, y, col, 2f, if (side) '█' else NONE)
            }
        }
        // Marquee.
        for (y in CAB_TOP..CAB_TOP + 3) {
            for (x in x0 + 1..x1 - 1) base.put(x, y, hue.scale(0.35f + 0.15f * (y - CAB_TOP)), 2f)
        }
        val names = listOf("ZAP!", "PONG", "MAZE", "STARS", "RACER")
        val name = names[order[c]]
        base.text(x0 + (CAB_W - name.length) / 2, CAB_TOP + 1, name, Col(1f, 1f, 1f))
        // Screen bezel.
        for (y in SCREEN_TOP - 1..SCREEN_TOP + SCREEN_H) {
            for (x in x0 + 2..x1 - 2) base.put(x, y, Col(0.04f, 0.03f, 0.07f), 2f)
        }
        // Control deck.
        for (x in x0 + 1..x1 - 1) {
            base.put(x, SCREEN_TOP + SCREEN_H + 1, Col(0.32f, 0.3f, 0.42f), 2f, '▄')
            base.put(x, SCREEN_TOP + SCREEN_H + 2, Col(0.22f, 0.2f, 0.32f), 2f)
        }
        val deck = SCREEN_TOP + SCREEN_H + 2
        base.put(x0 + 7, deck - 1, Col(0.7f, 0.7f, 0.8f), 1f, '│')
        base.put(x0 + 7, deck - 2, Col(1f, 0.25f, 0.3f), 1f, '●')
        base.put(x0 + 15, deck, hues[(order[c] + 1) % CABS], 1f, '●')
        base.put(x0 + 18, deck, hues[(order[c] + 2) % CABS], 1f, '●')
        base.put(x0 + 21, deck, hues[(order[c] + 3) % CABS], 1f, '●')
        // Coin door.
        for (y in deck + 3..deck + 5) {
            base.put(x0 + 11, y, Col(0.25f, 0.22f, 0.3f), 2f)
            base.put(x0 + 16, y, Col(0.25f, 0.22f, 0.3f), 2f)
        }
        base.put(x0 + 12, deck + 4, col("#ff8a3a"), 2f, '▐')
        base.put(x0 + 15, deck + 4, col("#ff8a3a"), 2f, '▌')
        base.put(x0 + 12, deck + 3, Col(0.25f, 0.22f, 0.3f), 2f)
        base.put(x0 + 15, deck + 3, Col(0.25f, 0.22f, 0.3f), 2f)
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val u = t / LOOP
        frame.copyFrom(base)
        sign(t, u)
        for (c in 0 until CABS) {
            val x0 = cabX(c) + 3
            val flicker = if (c == 2 && fract(u * 4f) in 0.6f..0.64f) 0.45f else 1f
            when (order[c]) {
                0 -> invaders(x0, u, hues[0], flicker)
                1 -> pong(x0, u, hues[1], flicker)
                2 -> maze(x0, u, hues[2], flicker)
                3 -> stars(x0, u, hues[3], flicker)
                else -> racer(x0, u, hues[4], flicker)
            }
            val hue = hues[order[c]]
            frame.glow(cabX(c) + CAB_W / 2f, SCREEN_TOP + SCREEN_H / 2f, 14f, hue, 0.12f * flicker)
            frame.glow(cabX(c) + CAB_W / 2f, CAB_TOP + 1.5f, 10f, hue, 0.18f)
        }
        reflection(u)
        frame.output(grid, palette)
    }

    private fun sign(
        t: Float,
        u: Float,
    ) {
        val word = "ARCADE"
        val w = word.length * 12 - 2
        val sx = (ICOLS - w) / 2
        // The second A buzzes and drops out in short bursts.
        val buzz = fract(u * 2f)
        val dead = buzz in 0.40f..0.43f || buzz in 0.46f..0.48f || buzz in 0.5f..0.56f
        for (k in word.indices) {
            val rows = GLYPHS.getValue(word[k])
            val on = !(k == 3 && dead)
            val c = if (k % 2 == 0) pink else pink.mix(col("#ff8a3a"), 0.2f)
            for (r in rows.indices) {
                for (q in 0 until 5) {
                    if (rows[r][q] != '#') continue
                    for (h in 0..1) {
                        val x = sx + k * 12 + q * 2 + h
                        frame.put(x, SIGN_Y + 1 + r, if (on) c.mix(Col(1f, 1f, 1f), 0.35f) else c.scale(0.25f), 2f)
                    }
                }
            }
            if (on) frame.glow(sx + k * 12 + 5f, SIGN_Y + 4.5f, 9f, pink, 0.22f + 0.02f * sin(ITAU * t * 3f))
        }
        // Tubes underline the sign.
        for (x in sx - 2..sx + w + 1) frame.put(x, SIGN_Y + 9, cyan, 2f, '▀')
        frame.glow(ICOLS / 2f, SIGN_Y + 9f, 40f, cyan, 0.06f)
    }

    /** Clears a screen to its dark glass with scanlines, tinted by its [hue]. */
    private fun screen(
        x0: Int,
        hue: Col,
        k: Float,
    ) {
        for (y in SCREEN_TOP until SCREEN_TOP + SCREEN_H) {
            for (x in x0 until x0 + CAB_W - 6) {
                val c = hue.scale((if (y % 2 == 0) 0.1f else 0.06f) * k)
                frame.put(x, y, c, 2f)
            }
        }
    }

    private fun px(
        x: Int,
        y: Int,
        c: Col,
        g: Char = NONE,
    ) {
        frame.put(x, y, c, 2f, g)
    }

    private fun invaders(
        x0: Int,
        u: Float,
        hue: Col,
        k: Float,
    ) {
        screen(x0, hue, k)
        val step = (u * 16f).toInt()
        val off = if (step % 8 < 4) step % 4 else 4 - step % 4
        val frameA = step % 2 == 0
        for (r in 0 until 3) {
            for (q in 0 until 5) {
                val x = x0 + 1 + off + q * 4
                val y = SCREEN_TOP + 1 + r * 2
                val c =
                    (
                        if (r == 0) {
                            col("#ff3ea5")
                        } else if (r == 1) {
                            hue
                        } else {
                            col("#36e2ff")
                        }
                    ).scale(k)
                px(x, y, c, if (frameA) '▙' else '▛')
                px(x + 1, y, c, if (frameA) '▟' else '▜')
            }
        }
        // The player ship slides and fires.
        val sxp = x0 + 2 + ((0.5f + 0.5f * sin(ITAU * u * 2f)) * 17f).toInt()
        px(sxp, SCREEN_TOP + SCREEN_H - 1, col("#3aff8a").scale(k), '▄')
        px(sxp + 1, SCREEN_TOP + SCREEN_H - 1, col("#3aff8a").scale(k), '█')
        px(sxp + 2, SCREEN_TOP + SCREEN_H - 1, col("#3aff8a").scale(k), '▄')
        val shot = fract(u * 4f)
        val sy = SCREEN_TOP + SCREEN_H - 2 - (shot * 8f).toInt()
        px(sxp + 1, sy, Col(1f, 1f, 1f).scale(k), '│')
    }

    private fun pong(
        x0: Int,
        u: Float,
        hue: Col,
        k: Float,
    ) {
        screen(x0, hue, k)
        val w = CAB_W - 7
        for (y in SCREEN_TOP until SCREEN_TOP + SCREEN_H step 2) px(x0 + w / 2, y, hue.scale(0.6f * k), '│')
        // A ball that bounces off both paddles: a triangle wave across, another down.
        val bx = abs(fract(u * 3f) * 2f - 1f)
        val by = abs(fract(u * 5f) * 2f - 1f)
        val ballX = x0 + 2 + (bx * (w - 4)).toInt()
        val ballY = SCREEN_TOP + (by * (SCREEN_H - 1)).toInt()
        px(ballX, ballY, Col(1f, 1f, 1f).scale(k), '●')
        val ly = (SCREEN_TOP + by * (SCREEN_H - 3)).toInt()
        val ry = (SCREEN_TOP + (1f - by) * 0.4f * (SCREEN_H - 3) + by * (SCREEN_H - 3) * 0.6f).toInt()
        for (d in 0..2) {
            px(x0, ly + d, hue.scale(k), '█')
            px(x0 + w, ry + d, hue.scale(k), '█')
        }
        frame.text(x0 + w / 2 - 3, SCREEN_TOP, "3", hue.scale(k))
        frame.text(x0 + w / 2 + 3, SCREEN_TOP, "7", hue.scale(k))
    }

    private fun maze(
        x0: Int,
        u: Float,
        hue: Col,
        k: Float,
    ) {
        screen(x0, hue, k)
        val w = CAB_W - 7
        val wall = col("#3a5cff").scale(k)
        for (x in x0..x0 + w) {
            px(x, SCREEN_TOP, wall, '═')
            px(x, SCREEN_TOP + SCREEN_H - 1, wall, '═')
            if (x in x0 + 3..x0 + w - 3) px(x, SCREEN_TOP + 3, wall, '═')
            if (x in x0 + 3..x0 + w - 3) px(x, SCREEN_TOP + 6, wall, '═')
        }
        // The muncher runs the middle corridor, eating dots that come back each lap.
        val p = fract(u * 2f)
        val mx = x0 + 1 + (p * (w - 1)).toInt()
        for (x in x0 + 1 until x0 + w step 2) {
            if (x > mx) px(x, SCREEN_TOP + 4, col("#ffd23a").scale(0.8f * k), '·')
            px(x, SCREEN_TOP + 1, col("#ffd23a").scale(0.6f * k), '·')
        }
        px(mx, SCREEN_TOP + 4, hue.scale(k), if (fract(u * 16f) < 0.5f) '●' else 'C')
        val gx = x0 + 1 + (fract(p - 0.3f) * (w - 1)).toInt()
        px(gx, SCREEN_TOP + 4, col("#ff3ea5").scale(k), 'm')
        px(x0 + w - 1 - (p * (w - 2)).toInt(), SCREEN_TOP + 7, col("#36e2ff").scale(k), 'm')
    }

    private fun stars(
        x0: Int,
        u: Float,
        hue: Col,
        k: Float,
    ) {
        screen(x0, hue, k * 0.6f)
        val w = CAB_W - 7
        for (s in 0 until 14) {
            val sx = x0 + (s * 7 + 3) % w
            val sy = SCREEN_TOP + ((fract(u * (2f + s % 3) + s * 0.37f)) * SCREEN_H).toInt()
            px(sx, sy, Col(0.8f, 0.8f, 1f).scale(k * (0.5f + 0.2f * (s % 3))), if (s % 3 == 2) '│' else '·')
        }
        val shipX = x0 + w / 2 + (5f * sin(ITAU * u * 2f)).toInt()
        px(shipX, SCREEN_TOP + SCREEN_H - 2, hue.scale(k), '▄')
        px(shipX - 1, SCREEN_TOP + SCREEN_H - 1, hue.scale(k), '▟')
        px(shipX, SCREEN_TOP + SCREEN_H - 1, Col(1f, 1f, 1f).scale(k), '█')
        px(shipX + 1, SCREEN_TOP + SCREEN_H - 1, hue.scale(k), '▙')
        // An enemy that drifts down and bursts.
        val e = fract(u * 2f)
        val ex = x0 + w / 2 + (6f * sin(ITAU * u * 6f)).toInt()
        val ey = SCREEN_TOP + (e * 6f).toInt()
        px(ex, ey, col("#ff3ea5").scale(k), if (e > 0.85f) '*' else 'W')
    }

    private fun racer(
        x0: Int,
        u: Float,
        hue: Col,
        k: Float,
    ) {
        val w = CAB_W - 7
        val cx = x0 + w / 2f
        for (y in SCREEN_TOP until SCREEN_TOP + SCREEN_H) {
            val d = y - SCREEN_TOP + 0.5f
            val half = d * 1.1f
            for (x in x0..x0 + w) {
                val off = x + 0.5f - cx
                val c =
                    if (y < SCREEN_TOP + 3) {
                        col("#8a5cff").scale(0.25f + 0.1f * (y - SCREEN_TOP))
                    } else if (abs(off) < half) {
                        val stripe = fract(8f / d - u * 8f) < 0.5f
                        if (abs(abs(off) - half) < 1f) {
                            (if (stripe) Col(1f, 1f, 1f) else col("#ff3ea5")).scale(0.8f)
                        } else {
                            Col(0.18f, 0.18f, 0.24f)
                        }
                    } else {
                        hue.scale(if (fract(8f / d - u * 8f) < 0.5f) 0.35f else 0.22f)
                    }
                frame.put(x, y, c.scale(k), 2f)
            }
        }
        // Sun on the horizon and the player car.
        px(x0 + w / 2, SCREEN_TOP + 2, col("#ffd23a").scale(k), '●')
        val carX = (cx + 3f * sin(ITAU * u)).toInt()
        px(carX - 1, SCREEN_TOP + SCREEN_H - 2, col("#ff3ea5").scale(k), '▟')
        px(carX, SCREEN_TOP + SCREEN_H - 2, col("#ff3ea5").scale(k), '█')
        px(carX + 1, SCREEN_TOP + SCREEN_H - 2, col("#ff3ea5").scale(k), '▙')
    }

    /** The glossy floor mirrors the cabinets, dimmed, streaked and rippling. */
    private fun reflection(u: Float) {
        for (y in FLOOR until IROWS) {
            val src = (FLOOR - 1 - (y - FLOOR) * 2.2f).toInt()
            val depth = (y - FLOOR).toFloat() / (IROWS - FLOOR)
            val wob = (1.2f * sin(ITAU * u * 2f + y * 1.3f) * (0.4f + depth)).toInt()
            for (x in 0 until ICOLS) {
                val i = y * ICOLS + x
                val sx = (x + wob).coerceIn(0, ICOLS - 1)
                val j = src * ICOLS + sx
                val tile = ((x / 8) + (y - FLOOR)) % 2 == 0
                val k = 0.38f * (1f - depth * 0.7f)
                val baseK = if (tile) 0.06f else 0.045f
                frame.r[i] = baseK * 0.8f + frame.r[j] * k
                frame.g[i] = baseK * 0.6f + frame.g[j] * k
                frame.b[i] = baseK * 1.3f + frame.b[j] * k
                frame.cov[i] = if (frame.cov[j] > 1.4f) 1.75f else frame.cov[j] * 0.7f
                frame.ch[i] = if (frame.ch[j] == '●' || frame.ch[j] == '█') frame.ch[j] else NONE
                if (y == FLOOR) frame.ch[i] = '▀'
            }
        }
        // A neon edge where the cabinets meet the floor, and the pools of light they throw on it.
        for (c in 0 until CABS) {
            val hue = hues[order[c]]
            for (x in cabX(c)..cabX(c) + CAB_W - 1) frame.put(x, FLOOR - 1, hue.scale(0.6f), 2f, '▄')
            frame.glow(cabX(c) + CAB_W / 2f, FLOOR + 3f, 13f, hue, 0.1f)
        }
    }
}

/**
 * A row of arcade cabinets in a dark hall under a buzzing pink ARCADE sign. Every screen runs its own little game:
 * marching invaders, pong, a maze muncher, a space shooter and a racer. Marquees glow in their colours, one screen
 * flickers, and the whole row mirrors on the glossy tiled floor with a slow ripple. The seed decides which game runs
 * on which cabinet.
 */
public fun neonArcadePiece(seed: Int = 0): AsciiPiece {
    val scene = Arcade(seed)
    return asciiPiece(
        name = "neon arcade",
        ground = Rgb.fromHex("#05030a"),
        category = "scenes",
        cols = ICOLS,
        rows = IROWS,
        fps = 15,
        still = 2f,
        loop = LOOP,
        note = "a row of glowing arcade cabinets under a buzzing neon sign, mirrored on a glossy floor",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A row of glowing arcade cabinets under a buzzing neon sign, mirrored on a glossy floor.
 *
 * @param seed decides which game runs on which cabinet.
 */
@Composable
public fun AsciiNeonArcade(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A row of glowing arcade cabinets under a neon sign, reflected on the floor",
) {
    val piece = remember(seed) { neonArcadePiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
