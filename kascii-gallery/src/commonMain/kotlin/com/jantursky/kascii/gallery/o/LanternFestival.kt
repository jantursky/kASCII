package com.jantursky.kascii.gallery.o

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
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 24f
private const val HZ = 30
private const val SKY_LANTERNS = 420
private const val RIVER_LANTERNS = 16
private const val STARS = 70

private class Festival(
    seed: Int,
) {
    private val frame = OFrame()
    private val sky = OFrame()
    private val palette: OPalette
    private val lx = FloatArray(SKY_LANTERNS)
    private val lz = FloatArray(SKY_LANTERNS)
    private val lPhase = FloatArray(SKY_LANTERNS)
    private val lSpeed = IntArray(SKY_LANTERNS)
    private val lDrift = FloatArray(SKY_LANTERNS)
    private val lSway = FloatArray(SKY_LANTERNS)
    private val lHue = FloatArray(SKY_LANTERNS)
    private val rx = FloatArray(RIVER_LANTERNS)
    private val ry = IntArray(RIVER_LANTERNS)
    private val rp = FloatArray(RIVER_LANTERNS)
    private val starX = IntArray(STARS)
    private val starY = IntArray(STARS)
    private val starP = FloatArray(STARS)
    private val winCells: IntArray
    private val winPhase: FloatArray
    private val amber = ocol("#ff9a3a")
    private val gold = ocol("#ffe08a")
    private val ember = ocol("#ff5a2a")

    init {
        val rng = oRng(seed, 41)
        for (k in 0 until SKY_LANTERNS) {
            // Mostly far lanterns, fewer near ones: depth skewed toward the distance.
            val near = rng.float() < 0.08f
            val z = rng.float()
            lz[k] = if (near) 0.3f + 0.7f * z else 0.3f * z * z * z
            // Far lanterns leave the town in a broad stream; near ones are let go anywhere along the near bank.
            val spread = (rng.float() + rng.float() + rng.float() - 1.5f) * 46f
            lx[k] = if (near) rng.float() * (OCOLS + 10f) - 5f else 92f + spread
            lPhase[k] = rng.float()
            lSpeed[k] = if (lz[k] > 0.25f && rng.float() < 0.5f) 2 else 1
            lDrift[k] = 14f + rng.float() * 40f
            lSway[k] = rng.float() * OTAU
            lHue[k] = rng.float()
        }
        for (k in 0 until RIVER_LANTERNS) {
            rx[k] = 10f + rng.float() * 172f
            ry[k] = HZ + 2 + rng.int(OROWS - HZ - 4)
            rp[k] = rng.float() * OTAU
        }
        for (s in 0 until STARS) {
            starX[s] = rng.int(OCOLS)
            starY[s] = rng.int(HZ - 14)
            starP[s] = rng.float()
        }
        val wins = ArrayList<Int>()
        val phases = ArrayList<Float>()
        buildSky(seed, rng, wins, phases)
        winCells = wins.toIntArray()
        winPhase = phases.toFloatArray()
        palette = OPalette(buildPalette())
    }

    private fun buildPalette(): List<OCol> =
        oramp(4, ocol("#07050c"), ocol("#0e0814"), ocol("#160c1a"), ocol("#1e1020")) +
            oramp(10, ocol("#03040e"), ocol("#0a0c24"), ocol("#1a1636"), ocol("#3a2040"), ocol("#6a3438")) +
            oramp(8, ocol("#04060c"), ocol("#0c1020"), ocol("#141a30"), ocol("#20203a")) +
            oramp(
                10,
                ocol("#3a1004"),
                ocol("#8a3010"),
                ocol("#e0601a"),
                ocol("#ff9a3a"),
                ocol("#ffd070"),
                ocol("#fff4c8"),
            ) +
            oramp(5, ocol("#5a1a10"), ocol("#c03a20"), ocol("#ff6a3a")) +
            oramp(6, ocol("#060a18"), ocol("#0e1a34"), ocol("#1e3050"), ocol("#3a4a6a")) +
            listOf(ocol("#a8b0d0"), ocol("#e8ecff"))

    private fun buildSky(
        seed: Int,
        rng: com.jantursky.kascii.core.Rng,
        wins: ArrayList<Int>,
        phases: ArrayList<Float>,
    ) {
        val lat = OLattice(seed, 43)
        for (y in 0 until HZ) {
            val k = y / (HZ - 1f)
            for (x in 0 until OCOLS) {
                val i = y * OCOLS + x
                // Night blue overhead warming to an ember haze over the town.
                val town = exp(-((x - 96f) / 70f) * ((x - 96f) / 70f))
                val haze = k * sqrt(k) * (0.3f + 0.7f * town)
                val cloud = osmooth(0.55f, 0.75f, lat.fbm(x * 0.025f, y * 0.12f, 3)) * (0.3f + 0.7f * k)
                val r = 0.015f + 0.62f * haze + 0.05f * cloud
                val g = 0.02f + 0.22f * haze + 0.03f * cloud
                val b = 0.07f + 0.08f * haze + 0.05f * cloud
                sky.set(i, r, g, b, 0f)
                sky.tone(i, r, g, b, 0.22f)
            }
        }
        // Far hills, then the town on the bank.
        for (x in 0 until OCOLS) {
            val hill = (HZ - 5 - 4f * lat.fbm(x * 0.02f, 3f, 3) - 2f * sin(x * 0.03f)).toInt()
            for (y in hill until HZ) sky.set(y * OCOLS + x, 0.07f, 0.05f, 0.12f, 2f)
        }
        var x = 4
        while (x < OCOLS - 4) {
            val w = 4 + rng.int(7)
            val h = 2 + rng.int(3) + (2f * exp(-((x - 96f) / 50f) * ((x - 96f) / 50f))).toInt()
            for (bx in x until min(OCOLS, x + w)) {
                for (y in HZ - h until HZ) sky.set(y * OCOLS + bx, 0.025f, 0.015f, 0.03f, 2f)
                // Upturned eaves.
                sky.put(bx, HZ - h - 1, OCol(0.035f, 0.02f, 0.035f), 2f, '▄')
            }
            sky.put(x - 1, HZ - h - 1, OCol(0.04f, 0.025f, 0.04f), 2f, '╯')
            sky.put(x + w, HZ - h - 1, OCol(0.04f, 0.025f, 0.04f), 2f, '╰')
            for (y in HZ - h + 1 until HZ) {
                for (wx in x + 1 until min(OCOLS - 1, x + w - 1) step 2) {
                    if (rng.float() < 0.45f) {
                        wins.add(y * OCOLS + wx)
                        phases.add(rng.float())
                    }
                }
            }
            x += w + 1 + rng.int(3)
        }
        // A pagoda over the town, tier on tier.
        val px = 58 + rng.int(12)
        for (tier in 0 until 5) {
            val half = 7 - tier
            val top = HZ - 5 - tier * 3
            for (y in top until top + 3) {
                for (bx in px - half + 1..px + half - 1) sky.set(y * OCOLS + bx, 0.025f, 0.015f, 0.03f, 2f)
            }
            for (bx in px - half - 1..px + half + 1) sky.put(bx, top, OCol(0.035f, 0.02f, 0.035f), 2f, '▄')
            sky.put(px - half - 2, top, OCol(0.04f, 0.025f, 0.04f), 2f, '╯')
            sky.put(px + half + 2, top, OCol(0.04f, 0.025f, 0.04f), 2f, '╰')
            wins.add((top + 2) * OCOLS + px)
            phases.add(rng.float())
        }
        sky.put(px, HZ - 21, OCol(0.12f, 0.07f, 0.08f), 2f, '│')
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        frame.copyFrom(sky)
        stars(t)
        townGlow(t)
        skyLanterns(t, near = false)
        water(t)
        riverLanterns(t)
        skyLanterns(t, near = true)
        frame.output(grid, palette)
    }

    private fun stars(t: Float) {
        for (s in 0 until STARS) {
            val tw = 0.5f + 0.5f * sin(OTAU * (2f * t / LOOP + starP[s]))
            val i = starY[s] * OCOLS + starX[s]
            if (frame.cov[i] > 0.6f || tw < 0.25f) continue
            frame.set(i, 0.5f * tw + 0.2f, 0.5f * tw + 0.2f, 0.6f * tw + 0.25f, 1f, if (tw > 0.85f) '+' else '.')
        }
    }

    private fun townGlow(t: Float) {
        for (w in winCells.indices) {
            val k = 0.75f + 0.25f * sin(OTAU * (3f * t / LOOP + winPhase[w]))
            val i = winCells[w]
            frame.set(i, amber.r * k, amber.g * k * 0.9f, amber.b * k * 0.6f, 2f)
        }
        // Warm light hanging over the roofs.
        for (y in HZ - 12 until HZ) {
            for (x in 0 until OCOLS) {
                val i = y * OCOLS + x
                if (frame.cov[i] > 1.4f) continue
                val near = (y - (HZ - 12f)) / 12f
                val k = 0.12f * near * near * exp(-((x - 96f) / 80f) * ((x - 96f) / 80f))
                frame.light(i, amber, k)
            }
        }
    }

    private fun skyLanterns(
        t: Float,
        near: Boolean,
    ) {
        for (k in 0 until SKY_LANTERNS) {
            val z = lz[k]
            if ((z > 0.3f) != near) continue
            val p = ofract(lPhase[k] + lSpeed[k] * t / LOOP)
            // Far lanterns rise from the town and shrink into the sky; near ones come up from the river in front.
            val start = if (near) OROWS + 3f else HZ - 3f - 6f * z
            val end = if (near) -4f else 1f + 4f * (1f - z)
            val y = start + (end - start) * (1f - (1f - p) * (1f - p * 0.3f))
            val sway = (0.6f + 2.5f * z) * sin(OTAU * 2f * t / LOOP + lSway[k])
            val fan = if (near) 0f else (lx[k] - 92f) * 0.7f * p
            val x = lx[k] + fan + lDrift[k] * p * (if (near) 0.4f else 1f) + sway
            val xi = x.toInt()
            val yi = y.toInt()
            val fade = osmooth(0f, 0.06f, p) * osmooth(1f, 0.75f, p)
            val flick = 0.85f + 0.15f * sin(OTAU * (7f * t / LOOP) + lSway[k] * 3f)
            val b = fade * flick * (0.45f + 0.55f * min(1f, z * 3f + 0.25f))
            if (b < 0.05f) continue
            val c = amber.mix(ember, lHue[k] * 0.6f)
            glowAt(x, y, c, b * (0.08f + 0.5f * z), 2f + 9f * z)
            if (z < 0.05f) {
                plot(xi, yi, c.scale(0.6f + 0.6f * b), 1f, if (b > 0.5f) '•' else '·')
            } else if (z < 0.16f) {
                plot(xi, yi, c.mix(gold, 0.3f).scale(0.5f + 0.6f * b), 1f, '●')
            } else if (z < 0.42f) {
                plot(xi, yi, c.mix(gold, 0.4f).scale(0.6f + 0.5f * b), 2f, '█')
            } else if (z < 0.7f) {
                plot(xi, yi, c.scale(0.55f + 0.4f * b), 2f, '█')
                plot(xi + 1, yi, c.scale(0.55f + 0.4f * b), 2f, '█')
                plot(xi, yi + 1, gold.scale(0.6f + 0.4f * b), 2f, '▀')
                plot(xi + 1, yi + 1, gold.scale(0.6f + 0.4f * b), 2f, '▀')
            } else {
                // A big paper lantern close by: rounded top, glowing belly, flame light at its open base.
                val body = c.scale(0.5f + 0.4f * b)
                val belly = c.mix(gold, 0.55f).scale(0.6f + 0.4f * b)
                plot(xi - 1, yi, body, 2f, '▄')
                plot(xi, yi, body, 2f, '█')
                plot(xi + 1, yi, body, 2f, '█')
                plot(xi + 2, yi, body, 2f, '▄')
                plot(xi - 1, yi + 1, belly.scale(0.8f), 2f, '█')
                plot(xi, yi + 1, belly, 2f, '█')
                plot(xi + 1, yi + 1, belly, 2f, '█')
                plot(xi + 2, yi + 1, belly.scale(0.8f), 2f, '█')
                plot(xi, yi + 2, gold.scale(0.7f + 0.3f * b), 2f, '▀')
                plot(xi + 1, yi + 2, gold.scale(0.7f + 0.3f * b), 2f, '▀')
            }
        }
    }

    private fun plot(
        x: Int,
        y: Int,
        c: OCol,
        cov: Float,
        glyph: Char,
    ) {
        if (x !in 0 until OCOLS || y !in 0 until OROWS) return
        frame.put(x, y, c, cov, glyph)
    }

    private fun glowAt(
        x: Float,
        y: Float,
        c: OCol,
        k: Float,
        rad: Float,
    ) {
        val r = rad.toInt() + 1
        val ry = max(1, (rad / O_ROW_U).toInt() + 1)
        for (yy in max(0, y.toInt() - ry)..min(OROWS - 1, y.toInt() + ry)) {
            for (xx in max(0, x.toInt() - r)..min(OCOLS - 1, x.toInt() + r)) {
                val dx = xx - x
                val dy = (yy - y) * O_ROW_U
                val v = k * exp(-(dx * dx + dy * dy) / (rad * rad * 0.5f))
                if (v > 0.01f) frame.light(yy * OCOLS + xx, c, v)
            }
        }
    }

    private fun water(t: Float) {
        val phi = OTAU * t / LOOP
        for (y in HZ until OROWS) {
            val dist = (y - HZ).toFloat()
            val fade = 0.8f * (1f - dist / (OROWS - HZ + 10f))
            for (x in 0 until OCOLS) {
                val i = y * OCOLS + x
                val wob = 1.4f * sin(y * 1.9f + 4f * phi + x * 0.04f) + 0.9f * sin(x * 0.13f - y * 1.1f - 3f * phi)
                val sx = min(OCOLS - 1, max(0, (x + wob * (0.5f + dist * 0.1f)).toInt()))
                val sy = HZ - 1 - (dist * 1.25f).toInt()
                var r = 0.012f
                var g = 0.02f
                var b = 0.05f
                var cov = 0.15f
                var glyph = 0.toChar()
                if (sy >= 0) {
                    val j = sy * OCOLS + sx
                    val bright = frame.r[j] + frame.g[j] + frame.b[j]
                    r += frame.r[j] * fade
                    g += frame.g[j] * fade * 0.9f
                    b += frame.b[j] * fade
                    cov = max(cov, min(1f, frame.cov[j]) * min(1f, bright * fade * 2f))
                    if (bright * fade > 0.5f) {
                        glyph = if (bright * fade > 1f) '═' else '~'
                        cov = max(cov, 0.95f)
                    }
                }
                val crest = sin(x * 0.19f + y * 3.1f + 3f * phi) * sin(x * 0.045f - 2f * phi + y)
                if (crest > 0.8f && glyph == 0.toChar()) {
                    glyph = '~'
                    cov = max(cov, 0.55f)
                    r += 0.04f
                    g += 0.05f
                    b += 0.1f
                }
                frame.set(i, r, g, b, cov, glyph)
            }
        }
    }

    private fun riverLanterns(t: Float) {
        for (k in 0 until RIVER_LANTERNS) {
            val x = rx[k] + 1.5f * sin(OTAU * t / LOOP + rp[k])
            val y = ry[k]
            val bob = 0.85f + 0.15f * sin(OTAU * 4f * t / LOOP + rp[k])
            val xi = x.toInt()
            glowAt(x, y.toFloat(), amber, 0.25f * bob, 5f)
            plot(xi, y, gold.scale(bob), 2f, '▄')
            // Its broken reflection beneath.
            for (d in 1..3) {
                if (y + d >= OROWS) break
                val wx = xi + if ((d + (t * 3f).toInt()) % 2 == 0) 0 else 1
                plot(wx, y + d, amber.scale(0.55f * bob / d), 1f, if (d == 1) '═' else '-')
            }
        }
    }
}

/**
 * A lantern festival over a river at night: hundreds of paper lanterns rise over a glowing town and its pagoda, the
 * far ones climbing slowly and shrinking into the sky, the near ones floating up from the water close by and swaying.
 * The river breaks the town glow and the lanterns into rippling reflections and small lanterns bob on the current. The
 * seed shapes the town and every lantern's path. Every motion repeats a whole number of times per loop.
 */
public fun lanternFestivalPiece(seed: Int = 0): AsciiPiece {
    val scene = Festival(seed)
    return asciiPiece(
        name = "lantern festival",
        ground = Rgb.fromHex("#03030b"),
        category = "nature",
        cols = OCOLS,
        rows = OROWS,
        fps = 24,
        still = 6f,
        loop = LOOP,
        note = "hundreds of paper lanterns rising over a river at night, a glowing town and rippling reflections",
    ) { time, grid -> scene.draw(oLoopTime(time, LOOP), grid) }
}

/**
 * Paper lanterns rising over a river and a glowing town at night, with reflections on the water.
 *
 * @param seed shapes the town and the path of every lantern.
 */
@Composable
public fun AsciiLanternFestival(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Hundreds of glowing paper lanterns rising over a river and a town at night",
) {
    val piece = remember(seed) { lanternFestivalPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
