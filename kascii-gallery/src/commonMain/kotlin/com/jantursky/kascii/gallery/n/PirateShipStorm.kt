package com.jantursky.kascii.gallery.n

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
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

private const val LOOP = 12f
private const val HORIZON = 21
private const val BANDS = 6
private const val SHIP_X = 92f
private const val SHIP_BAND = 3
private const val DROPS = 150
private const val SPRAY = 46
private const val BOLT = 26

/** One band of rolling sea: baseline row, height in rows, wavelength in cells, cycles per loop, phase. */
private class Band(
    val base: Float,
    val amp: Float,
    val len: Float,
    val speed: Int,
    val phase: Float,
)

private class Storm(
    seed: Int,
) {
    private val clouds = NLattice(seed, 101)
    private val tex = NLattice(seed, 103, 16)
    private val base = NFrame()
    private val frame = NFrame()
    private val palette: NPalette
    private val bands: List<Band>
    private val surf = Array(BANDS) { FloatArray(NCOLS) }
    private val crest = Array(BANDS) { FloatArray(NCOLS) }
    private val dropX = FloatArray(DROPS)
    private val dropP = FloatArray(DROPS)
    private val dropR = IntArray(DROPS)
    private val sprayP = FloatArray(SPRAY)
    private val sprayV = FloatArray(SPRAY)
    private val sprayA = FloatArray(SPRAY)
    private val boltX = Array(2) { FloatArray(BOLT + 1) }
    private val boltFork = IntArray(2)
    private val skyTop = col("#06090f")
    private val skyMid = col("#16202c")
    private val skyLow = col("#34464e")
    private val cloudDark = col("#0a0d14")
    private val cloudLit = col("#56677a")
    private val waterDeep = col("#02090e")
    private val water = col("#0e3440")
    private val waterLit = col("#2c6a70")
    private val foam = col("#cfe2dc")
    private val wood = col("#5a3820")
    private val woodDark = col("#1c100a")
    private val gold = col("#b08a3a")
    private val sail = col("#d8ccb0")
    private val warm = col("#ffb24a")
    private val flashCol = col("#d8d0ff")

    init {
        val rng = nrng(seed, 105)
        bands =
            listOf(
                Band(HORIZON + 1.6f, 0.35f, 9f, 2, rng.float() * NTAU),
                Band(HORIZON + 4.2f, 0.7f, 16f, 2, rng.float() * NTAU),
                Band(HORIZON + 8f, 1.2f, 27f, 2, rng.float() * NTAU),
                Band(HORIZON + 13f, 2.4f, 64f, 1, rng.float() * NTAU),
                Band(HORIZON + 19.5f, 3.8f, 58f, 2, rng.float() * NTAU),
                Band(HORIZON + 26.5f, 4.8f, 84f, 2, rng.float() * NTAU),
            )
        for (d in 0 until DROPS) {
            dropX[d] = rng.float() * (NCOLS + 30f) - 30f
            dropP[d] = rng.float()
            dropR[d] = 6 + rng.int(5)
        }
        for (s in 0 until SPRAY) {
            sprayP[s] = rng.float()
            sprayV[s] = 0.5f + rng.float()
            sprayA[s] = rng.float()
        }
        for (b in 0..1) {
            var x = if (b == 0) 158f + rng.float() * 14f else 18f + rng.float() * 12f
            for (k in 0..BOLT) {
                boltX[b][k] = x
                x += (rng.float() - 0.5f) * 3.2f
            }
            boltFork[b] = 7 + rng.int(8)
        }
        buildSky()
        palette =
            NPalette(
                ramp(10, skyTop, skyMid, skyLow, col("#6a7c88")) +
                    ramp(6, cloudDark, cloudLit) +
                    ramp(10, waterDeep, water, waterLit, foam) +
                    listOf(Col(1f, 1f, 1f)) +
                    ramp(8, woodDark, wood, col("#a87448")) +
                    tones(gold, 6) +
                    ramp(9, col("#2a261e"), col("#7a705a"), sail, col("#fff6e0")) +
                    tones(warm, 8) +
                    ramp(6, col("#4a4a7a"), col("#9a92e0"), flashCol) +
                    listOf(col("#05070a"), col("#7a8a96")),
            )
    }

    private fun buildSky() {
        for (y in 0..HORIZON) {
            for (x in 0 until NCOLS) {
                val h = y / HORIZON.toFloat()
                val c = if (h < 0.55f) skyTop.mix(skyMid, h / 0.55f) else skyMid.mix(skyLow, (h - 0.55f) / 0.45f)
                val i = y * NCOLS + x
                base.r[i] = c.r
                base.g[i] = c.g
                base.b[i] = c.b
                base.cov[i] = 0.3f + 0.55f * h
            }
        }
        // A pale break low on the horizon, behind the ship.
        base.glow(70f, HORIZON - 1f, 30f, col("#5a6e74"), 0.35f)
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val u = t / LOOP
        val flash = flash(u)
        frame.copyFrom(base)
        sky(u, flash)
        bolts(u, flash)
        surfaces(u)
        for (k in 0..SHIP_BAND) band(k, u, flash)
        ship(u, flash)
        for (k in SHIP_BAND + 1 until BANDS) band(k, u, flash)
        spray(u, flash)
        rain(u, flash)
        frame.output(grid, palette)
    }

    /** Lightning brightness: a double strike early in the loop and a single fainter one later. */
    private fun flash(u: Float): Float {
        val a = u - 0.3f
        val b = u - 0.76f
        var f = 0f
        if (a in 0f..0.07f) f = max(f, if (a < 0.012f || a in 0.022f..0.04f) 1f else 0.35f * (1f - a / 0.07f))
        if (b in 0f..0.05f) f = max(f, if (b < 0.015f) 0.75f else 0.25f * (1f - b / 0.05f))
        return f
    }

    private fun sky(
        u: Float,
        flash: Float,
    ) {
        val ox = 1.6f * cos(NTAU * u)
        val oy = 0.8f * sin(NTAU * u)
        for (y in 0..HORIZON) {
            val h = y / HORIZON.toFloat()
            for (x in 0 until NCOLS) {
                val i = y * NCOLS + x
                val d = clouds.fbm(x * 0.035f + ox + y * 0.05f, y * 0.16f + oy, 4)
                val dens = smooth(0.38f, 0.66f, d) * (1f - 0.55f * smooth(0.6f, 1f, h))
                if (dens > 0.02f) {
                    // Thick cloud is dark, its thinner rims catch the light from below.
                    val rim = (1f - dens) * dens * 4f
                    var c = cloudDark.mix(cloudLit, 0.15f + 0.55f * rim * (0.5f + h))
                    c = c.mix(flashCol, flash * (0.25f + 0.5f * rim))
                    val under = Col(frame.r[i], frame.g[i], frame.b[i])
                    frame.put(x, y, under.mix(c, min(1f, dens * 1.6f)), 0.4f + 1.6f * dens)
                }
                if (flash > 0f) frame.haze(i, flashCol, flash * 0.3f * (1f - 0.5f * h))
            }
        }
    }

    private fun bolts(
        u: Float,
        flash: Float,
    ) {
        if (flash < 0.3f) return
        val b = if (u < 0.5f) 0 else 1
        val step = (HORIZON - 1f) / BOLT
        for (k in 0 until BOLT) {
            val x0 = boltX[b][k]
            val x1 = boltX[b][k + 1]
            frame.glow(x0, k * step, 4f, flashCol, 0.1f * flash)
            frame.line(x0, k * step, x1, (k + 1) * step, Col(1f, 1f, 1f), 2f)
        }
        // One fork splitting off part way down.
        val f = boltFork[b]
        var x = boltX[b][f]
        for (k in f until min(BOLT, f + 8)) {
            val nx = x + 1.8f + 0.6f * sin(k * 2.3f)
            frame.line(x, k * step, nx, (k + 1) * step, flashCol, 1f)
            x = nx
        }
    }

    private fun theta(
        b: Band,
        x: Float,
        u: Float,
    ): Float = NTAU * (x / b.len - b.speed * u) + b.phase

    private fun surfaces(u: Float) {
        for (k in 0 until BANDS) {
            val b = bands[k]
            for (x in 0 until NCOLS) {
                val th = theta(b, x + 0.5f, u)
                val c = ((sin(th) + 1f) * 0.5f).pow(1.9f)
                val env = 0.75f + 0.25f * sin(NTAU * ((x + 0.5f) / (b.len * 3.3f) - u) + k * 1.7f)
                val h =
                    env * b.amp * (2f * c - 0.5f) +
                        b.amp * 0.25f * sin(NTAU * ((x + 0.5f) / (b.len * 0.61f) - 2f * b.speed * u) + k)
                surf[k][x] = b.base - h
                crest[k][x] = c * env
            }
        }
    }

    private fun band(
        k: Int,
        u: Float,
        flash: Float,
    ) {
        val b = bands[k]
        val near = k / (BANDS - 1f)
        val s = surf[k]
        for (x in 0 until NCOLS) {
            val top = s[x]
            val slope = (s[min(NCOLS - 1, x + 1)] - s[max(0, x - 1)]) * 0.5f
            val c = crest[k][x]
            val tx = (x + 0.5f) / b.len - b.speed * u
            for (y in max(0, top.toInt())..min(NROWS - 1, top.toInt() + 12 + k * 3)) {
                val d = y + 1f - top
                if (d <= 0f) continue
                val i = y * NCOLS + x
                // Wave faces turned toward the pale break are lit, the troughs and the depths go dark.
                val lit = clamp01(0.45f - slope * (0.9f - near * 0.5f) + c * 0.5f - d * (0.17f - near * 0.08f))
                var col = waterDeep.mix(water, clamp01(lit * 1.6f)).mix(waterLit, clamp01(lit * 1.4f - 0.55f))
                val n = tex.at(tx * 16f, y * 0.9f + k * 7f)
                var glyph = NONE
                var cov = 1.5f + 0.5f * clamp01(lit * 1.5f)
                if (d < 1.05f) {
                    // The surface cell: a half block when the surface sits in its lower half, foam on the crests.
                    glyph = if (d < 0.55f) '▄' else NONE
                    if (c > 0.72f) col = col.mix(foam, (c - 0.72f) * 3f)
                    cov = 2f
                } else if (n > 0.78f - c * 0.12f && d < 4f + near * 6f) {
                    col = col.mix(foam, 0.25f + 0.3f * c)
                    glyph = if (n > 0.8f) '~' else '-'
                    cov = 2f
                } else if (c > 0.85f && d < 2.2f) {
                    col = col.mix(foam, 0.55f)
                }
                col = col.mix(flashCol, flash * 0.2f * clamp01(1.2f - d * 0.2f))
                frame.put(x, y, col, cov, glyph)
            }
            // Spindrift dots blown off the highest crests.
            if (c > 0.9f && k >= 2) {
                val p = fract(u * 6f + x * 0.137f)
                val sy = top - 0.6f - p * (1.5f + near * 2f)
                if (sy > 0f) frame.dot(x + p * 2f, sy, foam.scale(0.9f - p * 0.4f), 1f, if (p < 0.4f) '•' else '·')
            }
        }
    }

    /** The galleon, posed on the swell under it and drawn cell by cell in its own rotated frame. */
    private fun ship(
        u: Float,
        flash: Float,
    ) {
        val s = surf[SHIP_BAND]
        val bow = s[(SHIP_X - 26f).toInt()]
        val stern = s[(SHIP_X + 26f).toInt()]
        val wl = (bow + stern) * 0.5f + 0.6f
        val ang = atan((stern - bow) * ROW_U / 52f) * 0.85f + 0.03f * sin(NTAU * u * 3f)
        val ca = cos(ang)
        val sa = sin(ang)
        val gust = 0.5f + 0.5f * sin(NTAU * u * 2f)
        val lift = flash * 0.25f
        for (y in 0 until NROWS) {
            for (x in max(0, (SHIP_X - 58f).toInt())..min(NCOLS - 1, (SHIP_X + 44f).toInt())) {
                if (y + 0.2f > s[x]) continue
                val dx = x + 0.5f - SHIP_X
                val dy = (wl - (y + 0.5f)) * ROW_U
                val lx = dx * ca - dy * sa
                val ly = dx * sa + dy * ca
                val c = shipCell(lx, ly, gust) ?: continue
                frame.put(x, y, c.first.mix(flashCol, lift), c.second, c.third)
            }
        }

        // Rigging: stays from the mast heads to the bowsprit and the stern, in thin lines.
        fun at(
            lx: Float,
            ly: Float,
        ): Pair<Float, Float> = Pair(SHIP_X + lx * ca + ly * sa, wl - (-lx * sa + ly * ca) / ROW_U)
        val rope = col("#7a6a50").mix(flashCol, lift)
        val lines =
            listOf(
                floatArrayOf(-14f, 62f, -52f, 19f),
                floatArrayOf(4f, 70f, -14f, 62f),
                floatArrayOf(22f, 54f, 4f, 70f),
                floatArrayOf(22f, 54f, 33f, 15f),
                floatArrayOf(4f, 70f, 31f, 14f),
            )
        for (l in lines) {
            val a = at(l[0], l[1])
            val e = at(l[2], l[3])
            val n = (max(abs(e.first - a.first), abs(e.second - a.second)) * 1.4f).toInt() + 1
            for (k in 0..n) {
                val p = k.toFloat() / n
                val px = a.first + (e.first - a.first) * p
                val py = a.second + (e.second - a.second) * p
                val i = py.toInt() * NCOLS + px.toInt()
                if (py < 0f || px < 0f || px >= NCOLS || py >= NROWS) continue
                if (frame.cov[i] > 1.4f && frame.ch[i] == NONE && frame.r[i] > 0.3f) continue
                if (py + 0.2f > s[px.toInt()]) continue
                frame.line(px, py, px, py, rope, 1f)
                frame.ch[i] =
                    if (abs(e.first - a.first) >
                        abs(e.second - a.second) * 2.2f
                    ) {
                        '─'
                    } else if ((e.first - a.first) * (e.second - a.second) >
                        0f
                    ) {
                        '╲'
                    } else {
                        '╱'
                    }
            }
        }
        // Stern lantern and lit stern windows.
        val lan = at(33f, 19f)
        frame.glow(lan.first, lan.second, 3.2f, warm, 0.45f + 0.1f * sin(NTAU * u * 5f))
        frame.dot(lan.first, lan.second, col("#fff0c0"), 2f, '●')
        // The flag streaming from the main mast head.
        val fl = at(4f, 72f)
        for (k in 0..7) {
            val wave = sin(NTAU * (u * 6f) - k * 0.9f) * 0.6f
            frame.dot(fl.first + 1f + k, fl.second + wave, col("#05070a"), 2f, if (k == 3) 'x' else '▀')
        }
    }

    /** Colour, coverage and glyph of the ship at local point [lx], [ly] (width units, waterline origin), or null. */
    private fun shipCell(
        lx: Float,
        ly: Float,
        gust: Float,
    ): Triple<Col, Float, Char>? {
        // Hull: a raised stern castle, a forecastle and a raked bow.
        val deck = 8f + 6f * smooth(16f, 26f, lx) + 2.5f * smooth(-18f, -30f, lx)
        val front = -27f - (ly + 2f) * 0.75f
        if (ly > -4f && ly < deck && lx > front && lx < 32f + ly * 0.12f) {
            var c = woodDark.mix(wood, clamp01(0.35f + ly * 0.05f))
            var glyph = NONE
            if (abs(ly - 4.5f) < 0.9f) c = gold.scale(0.75f)
            if (fract(ly / 2.6f) < 0.22f) c = c.scale(0.7f)
            // A row of gun ports, and the lit stern gallery.
            if (abs(ly - 2.2f) < 0.9f && fract((lx + 40f) / 7f) < 0.28f && lx > -22f && lx < 22f) c = col("#05070a")
            if (lx > 25f && ly > 8.5f && ly < 12.5f && fract(lx / 2.5f) < 0.6f) c = warm.scale(0.9f)
            if (ly > deck - 1.2f) {
                c = c.mix(col("#a87448"), 0.4f)
                glyph = '▀'
            }
            return Triple(c, 2f, glyph)
        }
        // Rail posts along the deck.
        if (ly >= deck && ly < deck + 2f && lx > front + 2f && lx < 31f && fract(lx / 3f) < 0.3f) {
            return Triple(wood, 2f, '│')
        }
        // Bowsprit.
        if (lx < -26f && lx > -54f) {
            val by = 11f + (-26f - lx) * 0.3f
            if (abs(ly - by) < 0.7f) return Triple(wood, 2f, NONE)
        }
        // Sails on three masts, bellied by the wind.
        val masts = floatArrayOf(-14f, 4f, 22f)
        val heights = floatArrayOf(62f, 70f, 54f)
        for (m in 0..2) {
            val mx = masts[m]
            val top = heights[m]
            if (m < 2) {
                for (k in 0..2) {
                    val y1 = top - 4f - k * (top - 18f) / 3f
                    val y0 = y1 - (top - 18f) / 3f + 1.6f
                    val half = 8.5f + k * 3.2f - m * 0.2f
                    val p = (lx - mx) / half
                    if (abs(p) > 1f) continue
                    val belly = (1.6f + 1.4f * gust) * (1f - p * p)
                    val bottom = y0 - belly
                    if (ly <= y1 && ly >= bottom) {
                        if (ly > y1 - 0.9f) return Triple(woodDark, 2f, '▀')
                        val q = (y1 - ly) / (y1 - bottom)
                        val shade = 0.62f + 0.32f * (1f - p * p) * (0.7f + 0.3f * gust) - 0.25f * q * q - 0.08f * p
                        val c = col("#2a261e").mix(sail, clamp01(shade))
                        return Triple(
                            if (shade >
                                0.95f
                            ) {
                                c.mix(col("#fff6e0"), 0.4f)
                            } else {
                                c
                            },
                            if (shade < 0.55f) 1.8f else 2f,
                            NONE,
                        )
                    }
                }
            } else {
                // A gaff spanker on the mizzen.
                if (lx > mx && lx < mx + 16f && ly > 16f && ly < 46f - (lx - mx) * 0.55f) {
                    val q = (lx - mx) / 16f
                    val shade = 0.82f - 0.3f * q + 0.15f * gust * sin(q * 3.1f)
                    return Triple(col("#2a261e").mix(sail, clamp01(shade)), 2f, NONE)
                }
            }
            if (abs(lx - mx) < 0.7f && ly > 0f && ly < top + 2f) return Triple(woodDark.mix(wood, 0.5f), 2f, '█')
        }
        // Jib sails from the fore mast to the bowsprit.
        if (lx < -16f && lx > -50f) {
            val upper = 58f + (lx + 16f) * 1.15f
            val lower = 14f + (-16f - lx) * 0.28f
            if (ly < upper && ly > lower) {
                val shade = 0.6f + 0.3f * gust * (ly - lower) / max(1f, upper - lower)
                return Triple(col("#2a261e").mix(sail, shade), 2f, NONE)
            }
        }
        return null
    }

    private fun spray(
        u: Float,
        flash: Float,
    ) {
        val s = surf[SHIP_BAND]
        val bx = SHIP_X - 30f
        val by = s[bx.toInt()]
        // Bursts thrown up when the bow dips into the swell, three per loop.
        for (k in 0 until SPRAY) {
            val p = fract(u * 3f + sprayP[k] * 0.35f)
            if (p > 0.4f) continue
            val tau = p / 0.4f
            val x = bx - sprayV[k] * 14f * tau + sprayA[k] * 6f
            val y = by - (sprayV[k] * 9f * tau - 9f * tau * tau) - 0.5f
            val c = foam.mix(flashCol, flash * 0.5f).scale(1f - tau * 0.5f)
            frame.dot(
                x,
                y,
                c,
                1f,
                if (tau < 0.3f) {
                    '●'
                } else if (tau < 0.7f) {
                    '•'
                } else {
                    '·'
                },
            )
        }
    }

    private fun rain(
        u: Float,
        flash: Float,
    ) {
        val c = col("#5a6a76").mix(flashCol, flash * 0.6f)
        for (d in 0 until DROPS) {
            val p = fract(dropP[d] + u * dropR[d])
            val y = p * (NROWS + 6f) - 3f
            val x = dropX[d] + y * 0.6f
            for (k in 0..1) {
                val yy = y - k
                if (yy < 0f || x < 0f || x >= NCOLS || yy >= NROWS) continue
                val i = yy.toInt() * NCOLS + x.toInt()
                if (frame.cov[i] > 1.4f && frame.ch[i] == NONE && k == 0 && frame.r[i] > 0.5f) continue
                frame.put(x.toInt(), yy.toInt(), c.scale(if (k == 0) 1f else 0.7f), 1f, '/')
            }
        }
    }
}

/**
 * A pirate galleon pitching through a night storm. Big rolling waves run under it in layers that shrink toward the
 * horizon, its sails belly and slacken with the gusts, spray bursts from the bow each time it dips, rain slants across
 * everything and twice per loop lightning forks down behind the ship and lights the whole sea. The seed sets the wave
 * phases, the rain and the shape of the bolts.
 */
public fun pirateShipStormPiece(seed: Int = 0): AsciiPiece {
    val scene = Storm(seed)
    return asciiPiece(
        name = "pirate ship storm",
        ground = Rgb.fromHex("#04070b"),
        category = "scenes",
        cols = NCOLS,
        rows = NROWS,
        fps = 15,
        still = 5.6f,
        loop = LOOP,
        note = "a pirate galleon pitching on big rolling waves in a storm, sails billowing, lightning forking behind",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A pirate galleon pitching on big rolling waves in a night storm, with lightning, spray and slanting rain.
 *
 * @param seed sets the wave phases, the rain and the shape of the lightning bolts.
 */
@Composable
public fun AsciiPirateShipStorm(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A pirate galleon pitching on big waves in a storm, lightning flashing behind it",
) {
    val piece = remember(seed) { pirateShipStormPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
