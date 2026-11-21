package com.jantursky.kascii.gallery.k

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.AsciiPieceView
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.Noise
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
private const val HZ = 32
private const val SHELLS = 15
private const val SPARKS = 64
private const val STARS = 90
private const val RISE = 1.1f

/** One firework shell: when and where it goes up, where it bursts, its color and its kind of burst. */
private class Shell(
    val t0: Float,
    val x0: Float,
    val bx: Float,
    val by: Float,
    val color: KCol,
    val kind: Int,
    val life: Float,
    val angle: FloatArray,
    val speed: FloatArray,
)

private class Harbor(
    seed: Int,
) {
    private val lat = KLattice(seed, 41)
    private val frame = KFrame()
    private val city = KFrame()
    private val palette: KPalette
    private val shells: List<Shell>
    private val starX = IntArray(STARS)
    private val starY = IntArray(STARS)
    private val starPhase = FloatArray(STARS)
    private val winCells: IntArray
    private val winPhase: FloatArray
    private val winCol: Array<KCol>
    private val beaconX: Int
    private val beaconY: Int
    private val hues =
        listOf(
            kcol("#ffc94a"),
            kcol("#ff4fa8"),
            kcol("#4fe3ff"),
            kcol("#7dff6a"),
            kcol("#ff5a3c"),
            kcol("#b98cff"),
            kcol("#f4f1ff"),
        )

    init {
        val rng = kRng(seed, 3)
        val list = ArrayList<Shell>()
        val slots =
            floatArrayOf(0.1f, 0.5f, 1.4f, 2.4f, 3.2f, 4.4f, 5.0f, 6.2f, 6.8f, 8.3f, 9.2f, 9.9f, 11.0f, 12.1f, 13.8f)
        for (s in 0 until SHELLS) {
            val x0 = 22f + rng.float() * 148f
            val kind = rng.int(4)
            val n = SPARKS
            val ang = FloatArray(n) { KTAU * (it + rng.float() * 0.6f) / n }
            val spd = FloatArray(n) { if (kind == 2) 1f else 0.55f + 0.45f * sqrt(rng.float()) }
            list.add(
                Shell(
                    t0 = slots[s] + rng.float() * 0.5f,
                    x0 = x0,
                    bx = x0 + (rng.float() - 0.5f) * 14f,
                    by = 7f + rng.float() * 8f,
                    color = hues[(s * 3 + rng.int(hues.size)) % hues.size],
                    kind = kind,
                    life = if (kind == 1) 3.6f else 2.7f,
                    angle = ang,
                    speed = spd,
                ),
            )
        }
        shells = list
        for (s in 0 until STARS) {
            starX[s] = rng.int(KCOLS)
            starY[s] = rng.int(HZ - 10)
            starPhase[s] = rng.float()
        }
        val wins = ArrayList<Int>()
        val phases = ArrayList<Float>()
        val cols = ArrayList<KCol>()
        var x = 0
        var tallest = 0
        var tallX = 0
        while (x < KCOLS) {
            val w = 5 + rng.int(10)
            val h =
                if (x < 14 || x > 176) {
                    1 + rng.int(3)
                } else {
                    2 + rng.int(6) + (6f * kgauss(x.toFloat(), 104f, 36f)).toInt()
                }
            val shade = 0.035f + rng.float() * 0.03f
            for (bx in x until min(KCOLS, x + w)) {
                for (y in HZ - h until HZ) city.put(bx, y, KCol(shade * 0.9f, shade, shade * 1.5f), 2f)
            }
            if (h > tallest) {
                tallest = h
                tallX = x + w / 2
            }
            // Windows: every other column and row, some lit.
            for (wy in HZ - h + 1 until HZ - 1 step 2) {
                for (wx in x + 1 until min(KCOLS, x + w - 1) step 2) {
                    if (rng.float() < 0.3f) {
                        wins.add(wy * KCOLS + wx)
                        phases.add(rng.float())
                        cols.add(if (rng.float() < 0.75f) kcol("#ffc46a") else kcol("#9fd8ff"))
                    }
                }
            }
            x += w + (if (rng.float() < 0.25f) 1 + rng.int(4) else 0)
        }
        // A radio mast on the tallest block, with a beacon.
        beaconX = tallX
        beaconY = HZ - tallest - 6
        for (y in beaconY + 1 until HZ - tallest) city.put(beaconX, y, KCol(0.12f, 0.12f, 0.16f), 2f, '│')
        winCells = wins.toIntArray()
        winPhase = phases.toFloatArray()
        winCol = cols.toTypedArray()
        palette = KPalette(buildPalette())
    }

    private fun buildPalette(): List<KCol> {
        val out = ArrayList<KCol>()
        out += kramp(8, kcol("#03040c"), kcol("#0c1030"), kcol("#22204e"), kcol("#3a2c62"))
        out += kramp(5, kcol("#05060e"), kcol("#0d1020"), kcol("#1c2034"))
        out += kramp(4, kcol("#4a3a1a"), kcol("#ffc46a"), kcol("#fff0c8"))
        out += kramp(3, kcol("#2a4a6a"), kcol("#9fd8ff"), kcol("#e8f6ff"))
        for (h in hues) out += kramp(6, h.scale(0.18f), h.scale(0.5f), h, h.mix(KCol(1f, 1f, 1f), 0.65f))
        out += kramp(4, kcol("#06102a"), kcol("#123060"), kcol("#3a6aa8"))
        out += listOf(kcol("#ff2a2a"), kcol("#7a1010"), kcol("#ffffff"), kcol("#a8b0d0"))
        return out
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = KTAU * t / LOOP
        skyBase(phi)
        stars(t)
        for (s in shells) halo(s, kLoopTime(t - s.t0, LOOP))
        for (s in shells) shell(s, kLoopTime(t - s.t0, LOOP))
        cityDraw(t)
        water(phi)
        frame.output(grid, palette)
    }

    private fun skyBase(phi: Float) {
        val ox = 2f * cos(phi)
        val oy = 2f * sin(phi)
        for (y in 0 until HZ) {
            val k = y / HZ.toFloat()
            for (x in 0 until KCOLS) {
                val i = y * KCOLS + x
                val smoke = ksmooth(0.55f, 0.8f, lat.fbm(x * 0.03f + ox, y * 0.09f + oy, 3))
                val r = 0.02f + 0.13f * k * k + 0.06f * smoke
                val g = 0.025f + 0.07f * k * k + 0.06f * smoke
                val b = 0.07f + 0.2f * k * k + 0.08f * smoke
                frame.set(i, r, g, b, 0.05f + 0.3f * k * k * k + 0.25f * smoke)
            }
        }
    }

    private fun stars(t: Float) {
        for (s in 0 until STARS) {
            val tw = 0.5f + 0.5f * sin(KTAU * (2f * t / LOOP + starPhase[s]))
            if (tw < 0.3f) continue
            val i = starY[s] * KCOLS + starX[s]
            val c = 0.45f + 0.45f * tw
            frame.set(i, c * 0.9f, c * 0.92f, c, 1f, if (tw > 0.85f) '+' else '.')
        }
    }

    private fun sparkAt(
        s: Shell,
        k: Int,
        a: Float,
        out: FloatArray,
    ) {
        val v = 64f * s.speed[k]
        val drag = if (s.kind == 1) 2.6f else 1.9f
        val reach = v * (1f - exp(-a * drag)) / drag
        var dx = cos(s.angle[k]) * reach
        var dy = sin(s.angle[k]) * reach
        if (s.kind == 2) dy *= 0.45f // ring, seen tilted
        val fall = (if (s.kind == 1) 3.4f else 2.2f) * a * a
        out[0] = s.bx + dx
        out[1] = s.by + dy / K_ROW_U + fall
    }

    private val pt = FloatArray(2)

    /** The line glyph along which spark [k] of [s] travels at age [a], so trails read as streaks. */
    private fun streak(
        s: Shell,
        k: Int,
        a: Float,
    ): Char {
        val drag = if (s.kind == 1) 2.6f else 1.9f
        val slow = 64f * s.speed[k] * exp(-a * drag)
        val vx = cos(s.angle[k]) * slow
        var vy = sin(s.angle[k]) * slow
        if (s.kind == 2) vy *= 0.45f
        vy += 2f * (if (s.kind == 1) 3.4f else 2.2f) * a * K_ROW_U
        return if (abs(vy) < 0.45f * abs(vx)) {
            '─'
        } else if (abs(vx) < 0.45f * abs(vy)) {
            '│'
        } else if ((vx > 0f) == (vy > 0f)) {
            '╲'
        } else {
            '╱'
        }
    }

    private fun halo(
        s: Shell,
        age: Float,
    ) {
        val a = age - RISE
        if (a < 0f || a > 1.6f) return
        val k = 0.9f * exp(-a / 0.3f) + 0.25f * exp(-a / 1.1f)
        for (y in max(0, (s.by - 20f).toInt())..min(KROWS - 1, (s.by + 20f).toInt())) {
            if (y >= HZ) break
            for (x in max(0, (s.bx - 60f).toInt())..min(KCOLS - 1, (s.bx + 60f).toInt())) {
                val dx = x - s.bx
                val dy = (y - s.by) * K_ROW_U
                val v = k * exp(-(dx * dx + dy * dy) / 900f)
                if (v > 0.015f) frame.light(y * KCOLS + x, s.color.scale(0.75f), v * 0.8f)
            }
        }
    }

    private fun shell(
        s: Shell,
        age: Float,
    ) {
        if (age < RISE) {
            // The rising shell: a hot head with a short wobbling trail of sparks.
            val p = age / RISE
            val e = 1f - (1f - p) * (1f - p)
            for (k in 0 until 6) {
                val q = max(0f, e - k * 0.035f)
                val x = s.x0 + (s.bx - s.x0) * q + 0.4f * sin(age * 30f + k)
                val y = HZ + (s.by - HZ) * q
                val f = 1f - k / 6f
                val i = y.toInt() * KCOLS + x.toInt()
                if (y < 0f || y >= HZ || x < 0f || x >= KCOLS) continue
                frame.light(i, KCol(1f, 0.8f, 0.5f), 0.9f * f)
                if (k == 0) frame.ch[i] = '\''
            }
            return
        }
        val a = age - RISE
        if (a > s.life) return
        val fade = (1f - a / s.life).pow(1.4f)
        val hot = exp(-a / 0.25f)
        val tail = if (s.kind == 1) 9 else 6
        for (k in 0 until SPARKS) {
            if (s.kind == 3 && k % 2 == 1) continue
            for (j in 0 until tail) {
                val aj = a - j * 0.085f
                if (aj < 0f) break
                sparkAt(s, k, aj, pt)
                val x = pt[0].toInt()
                val y = pt[1].toInt()
                if (x < 0 || x >= KCOLS || y < 0 || y >= HZ) continue
                val i = y * KCOLS + x
                val tf = 1f - j / tail.toFloat()
                var c = s.color.mix(KCol(1f, 1f, 1f), hot * tf)
                if (s.kind == 1) c = c.mix(KCol(1f, 0.6f, 0.2f), (1f - tf) * 0.6f + a / s.life * 0.4f)
                var b = fade * tf * (if (j == 0) 1.3f else 0.95f)
                // Crackling glitter near the end.
                if (s.kind == 3 && a > s.life * 0.45f) {
                    val on = Noise.hash(k, (a * 14f).toInt(), j, 7) > 0.55f
                    b = if (on) 1f else b * 0.2f
                    if (on && j == 0) frame.ch[i] = '*'
                }
                frame.light(i, c, b)
                if (j == 0 && b > 0.7f && s.kind != 3) {
                    frame.ch[i] = if (hot > 0.3f) '*' else 0.toChar()
                } else if (j > 0 && b > 0.22f && frame.ch[i] == 0.toChar()) {
                    frame.ch[i] = streak(s, k, aj)
                }
            }
        }
        // The crossette burst splits again: little secondary flashes.
        if (s.kind == 3 && a > 0.5f && a < 0.8f) {
            for (k in 0 until SPARKS step 8) {
                sparkAt(s, k, 0.5f, pt)
                val x = pt[0].toInt()
                val y = pt[1].toInt()
                if (x in 0 until KCOLS && y in 0 until HZ) frame.put(x, y, KCol(1f, 1f, 0.95f), 1f, '+')
            }
        }
    }

    private fun cityDraw(t: Float) {
        for (i in 0 until HZ * KCOLS) {
            if (city.cov[i] <= 0f) continue
            // The skyline catches a little of the fireworks above.
            val lift = min(0.12f, (frame.r[i] + frame.g[i] + frame.b[i]) * 0.04f)
            frame.set(i, city.r[i] + lift, city.g[i] + lift, city.b[i] + lift * 1.2f, city.cov[i], city.ch[i])
        }
        for (w in winCells.indices) {
            val on = sin(KTAU * (t / LOOP + winPhase[w])) > -0.8f
            if (!on) continue
            val i = winCells[w]
            val c = winCol[w].scale(0.55f + 0.35f * winPhase[w])
            frame.set(i, c.r, c.g, c.b, 2f)
        }
        val blink = kfract(t / 2f) < 0.35f
        if (beaconY >=
            0
        ) {
            frame.put(beaconX, beaconY, if (blink) KCol(1f, 0.16f, 0.16f) else KCol(0.48f, 0.06f, 0.06f), 1f, '●')
        }
        if (blink) {
            for (dx in -3..3) {
                for (dy in -1..1) {
                    val x = beaconX + dx
                    val y = beaconY + dy
                    if ((dx != 0 || dy != 0) && x in 0 until KCOLS && y in 0 until HZ) {
                        frame.light(y * KCOLS + x, KCol(1f, 0.2f, 0.2f), 0.25f / (1f + abs(dx) + 2f * abs(dy)))
                    }
                }
            }
        }
    }

    private fun water(phi: Float) {
        for (y in HZ until KROWS) {
            val dist = (y - HZ).toFloat()
            val fade = 0.9f * (1f - dist / (KROWS - HZ + 8f))
            for (x in 0 until KCOLS) {
                val i = y * KCOLS + x
                val wob = 1.6f * sin(y * 1.7f + 3f * phi + x * 0.05f) + 1.1f * sin(x * 0.11f - y * 0.9f - 2f * phi)
                val sx = min(KCOLS - 1, max(0, (x + wob * (0.4f + dist * 0.08f)).toInt()))
                val sy = HZ - 1 - (dist * 1.5f).toInt()
                var r = 0.02f
                var g = 0.035f
                var b = 0.08f + 0.03f * (1f - dist / 17f)
                var cov = 0.12f
                var glyph = 0.toChar()
                if (sy >= 0) {
                    val j = sy * KCOLS + sx
                    val bright = frame.r[j] + frame.g[j] + frame.b[j]
                    r += frame.r[j] * fade
                    g += frame.g[j] * fade
                    b += frame.b[j] * fade * 1.05f
                    cov = max(cov, min(1f, frame.cov[j]) * min(1f, bright * fade * 2.2f))
                    if (bright * fade > 0.35f) {
                        glyph = if (bright * fade > 0.9f) '═' else '~'
                        cov = max(cov, 0.9f)
                    } else if (frame.cov[j] > 1.4f && bright > 0.25f) {
                        glyph = '-'
                        cov = 1f
                    }
                }
                // Ripple crests.
                val crest = sin(x * 0.21f + y * 2.9f + 2f * phi) * sin(x * 0.05f - phi + y)
                if (crest > 0.82f && glyph == 0.toChar()) {
                    glyph = '~'
                    cov = max(cov, 0.6f)
                    r += 0.05f
                    g += 0.08f
                    b += 0.14f
                }
                frame.set(i, r, g, b, cov, glyph)
            }
        }
    }
}

/**
 * Fireworks over a night harbor: shells rise from behind the skyline and burst into peonies, golden willows, tilted
 * rings and crackling crossettes, each lighting the smoke around it, while the water below breaks every burst into
 * rippling reflections and a mast beacon blinks. The seed shapes the skyline and the show. Every motion repeats a
 * whole number of times per loop.
 */
public fun fireworksHarborPiece(seed: Int = 0): AsciiPiece {
    val scene = Harbor(seed)
    return asciiPiece(
        name = "fireworks harbor",
        ground = Rgb.fromHex("#03040b"),
        category = "effects",
        cols = KCOLS,
        rows = KROWS,
        fps = 24,
        still = 2.4f,
        loop = LOOP,
        note = "fireworks bursting over a harbor skyline, trails falling and reflections rippling in the water",
    ) { time, grid -> scene.draw(kLoopTime(time, LOOP), grid) }
}

/**
 * Fireworks over a night harbor skyline, with trailing bursts and rippling reflections in the water.
 *
 * @param seed shapes the skyline and the order, place and color of the bursts.
 */
@Composable
public fun AsciiFireworksHarbor(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Fireworks bursting over a harbor skyline at night, reflected in the water",
) {
    val piece = remember(seed) { fireworksHarborPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
