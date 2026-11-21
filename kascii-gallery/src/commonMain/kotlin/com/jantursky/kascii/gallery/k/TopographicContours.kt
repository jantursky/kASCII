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
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val LOOP = 20f
private const val STEP = 0.0625f
private const val MAJOR = 5
private const val SEA = 0.3125f
private const val W = KCOLS + 2
private const val H = KROWS + 2

/** Elevation in metres for a height value: one contour step is 100 m and the lake shore is 0 m. */
private fun metres(h: Float): Int = ((h - SEA) * 1600f / 10f).roundToInt() * 10

private class Topo(
    seed: Int,
) {
    private val latA = KLattice(seed, 81)
    private val latB = KLattice(seed, 83)
    private val frame = KFrame()
    private val palette: KPalette
    private val height = FloatArray(W * H)
    private val level = IntArray(W * H)
    private val used = BooleanArray(KCELLS)
    private val benchX: Int
    private val benchY: Int
    private val low = kcol("#0d2a24")
    private val mid = kcol("#34451f")
    private val high = kcol("#6a5232")
    private val peak = kcol("#b6a68a")
    private val water = kcol("#0b2a4a")
    private val minor = kcol("#9a6e34")
    private val major = kcol("#f0c070")
    private val label = kcol("#f4e6c0")
    private val cursor = kcol("#4fe3ff")
    private val ink = kcol("#8a8270")

    init {
        val rng = kRng(seed, 4)
        benchX = 30 + rng.int(130)
        benchY = 8 + rng.int(30)
        palette = KPalette(buildPalette())
    }

    private fun buildPalette(): List<KCol> =
        kramp(10, low.scale(0.5f), low, mid, high, peak) +
            kramp(6, kcol("#05140f"), kcol("#0d2a24"), kcol("#1f4a3a")) +
            kramp(4, kcol("#061426"), water, kcol("#2a5a8a")) +
            kramp(5, kcol("#3a2a14"), minor, kcol("#c89a58")) +
            kramp(4, kcol("#8a6a34"), major, kcol("#fff0c8")) +
            listOf(label, kcol("#0a0c0a"), kcol("#14160f")) +
            kramp(4, kcol("#0e4a5a"), cursor, kcol("#d8f8ff")) +
            kramp(4, kcol("#2a2820"), ink, kcol("#c8c0a8"))

    private fun field(phi: Float) {
        val a1 = 0.9f * cos(phi)
        val a2 = 0.9f * sin(phi)
        val b1 = 0.7f * cos(phi + 2f)
        val b2 = 0.7f * sin(phi + 2f)
        for (y in 0 until H) {
            val vy = (y - 1) * K_ROW_U * 0.0115f
            for (x in 0 until W) {
                val vx = (x - 1) * 0.0115f
                val q = latB.fbm(vx * 1.3f - b1, vy * 1.3f - b2, 3)
                val h = 0.7f * latA.fbm(vx + a1 + 0.6f * q, vy + a2 + 0.6f * q, 3) + 0.3f * q
                val k = y * W + x
                height[k] = (h - 0.5f) * 2.2f + 0.55f
                level[k] = floor(height[k] / STEP).toInt()
            }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val phi = KTAU * t / LOOP
        field(phi)
        used.fill(false)
        relief(phi)
        labels()
        peakMark()
        bench()
        frameAndScale()
        crosshair(phi)
        frame.output(grid, palette)
    }

    private fun relief(phi: Float) {
        for (y in 0 until KROWS) {
            for (x in 0 until KCOLS) {
                val k = (y + 1) * W + x + 1
                val h = height[k]
                val gx = (height[k + 1] - height[k - 1]) * 0.5f
                val gy = (height[k + W] - height[k - W]) * 0.5f / K_ROW_U
                val i = y * KCOLS + x
                if (h < SEA) {
                    // Lakes: flat blue with drifting ripples.
                    val depth = ksmooth(SEA, SEA - 0.12f, h)
                    val c = water.scale(1f - 0.45f * depth)
                    val rip = sin(x * 0.5f + y * 1.7f + 3f * phi) * sin(x * 0.13f - 2f * phi)
                    frame.set(i, c.r, c.g, c.b, 0.55f, if (rip > 0.7f) '~' else 0.toChar())
                    if (level[k] != level[k + 1] || level[k] != level[k + W]) frame.set(i, 0.3f, 0.55f, 0.8f, 1f, '·')
                    continue
                }
                // Hillshade from the north-west.
                val shade = kclamp01(0.55f + (gx * 0.7f + gy * 0.7f) * -30f)
                val e = kclamp01((h - SEA) / 0.75f)
                val tint =
                    if (e < 0.33f) {
                        low.mix(mid, e / 0.33f)
                    } else if (e < 0.7f) {
                        mid.mix(high, (e - 0.33f) / 0.37f)
                    } else {
                        high.mix(peak, (e - 0.7f) / 0.3f)
                    }
                val lit = 0.5f + 1.3f * shade
                var r = tint.r * lit
                var g = tint.g * lit
                var b = tint.b * lit
                var cov = 0.12f + 0.8f * shade * shade
                var glyph = 0.toChar()
                val lv = level[k]
                if (lv != level[k + 1] || lv != level[k + W]) {
                    // A contour crosses this cell: draw it along the slope's tangent, index lines brighter.
                    val top = max(lv, max(level[k + 1], level[k + W]))
                    val isMajor = top % MAJOR == 0
                    glyph = tangent(gx, gy)
                    val c = if (isMajor) major else minor
                    r = c.r
                    g = c.g
                    b = c.b
                    cov = if (isMajor) 1.2f else 0.9f
                }
                frame.set(i, r, g, b, cov, glyph)
            }
        }
    }

    private fun tangent(
        gx: Float,
        gy: Float,
    ): Char {
        // The line runs across the gradient; compare its visual angle against the glyph shapes.
        val tx = abs(gy)
        val ty = abs(gx)
        return if (ty < tx * 0.62f) {
            '─'
        } else if (ty > tx * 4.7f) {
            '│'
        } else if ((gx > 0f) == (gy > 0f)) {
            '╱'
        } else {
            '╲'
        }
    }

    private fun stamp(
        x: Int,
        y: Int,
        text: String,
        c: KCol,
    ): Boolean {
        if (y < 1 || y >= KROWS - 1 || x < 1 || x + text.length >= KCOLS - 1) return false
        for (k in -1..text.length) if (used[y * KCOLS + x + k]) return false
        frame.text(x - 1, y, " $text ", c, KCol(0.03f, 0.035f, 0.03f))
        for (k in -1..text.length) used[y * KCOLS + x + k] = true
        return true
    }

    private val placedX = IntArray(16)
    private val placedY = IntArray(16)

    private fun labels() {
        // Index contours get their elevation written along their flatter stretches, spread over the sheet.
        var n = 0
        for (y in 3 until KROWS - 3 step 2) {
            var x = 6 + (y * 7) % 11
            while (x < KCOLS - 10 && n < placedX.size) {
                val i = y * KCOLS + x
                val flat = frame.ch[i - 1] == '─' && frame.ch[i + 1] == '─'
                if (frame.ch[i] == '─' && frame.cov[i] > 1.1f && flat) {
                    var clear = true
                    for (p in 0 until n) if (abs(placedX[p] - x) < 30 && abs(placedY[p] - y) < 5) clear = false
                    val k = (y + 1) * W + x + 1
                    val lv = max(level[k], max(level[k + 1], level[k + W]))
                    val m = (lv - (SEA / STEP).roundToInt()) * 100
                    if (clear && m > 0 && stamp(x - 1, y, m.toString(), label)) {
                        placedX[n] = x
                        placedY[n] = y
                        n++
                        x += 30
                    }
                }
                x += 3
            }
        }
    }

    private fun peakMark() {
        var best = 0
        var bh = -1f
        for (y in 3 until KROWS - 3) {
            for (x in 6 until KCOLS - 12) {
                val h = height[(y + 1) * W + x + 1]
                if (h > bh) {
                    bh = h
                    best = y * KCOLS + x
                }
            }
        }
        val x = best % KCOLS
        val y = best / KCOLS
        frame.put(x, y, label, 2f, '^')
        used[best] = true
        stamp(x + 2, y, metres(bh).toString(), label)
    }

    private fun bench() {
        val h = height[(benchY + 1) * W + benchX + 1]
        frame.put(benchX, benchY, kcol("#ff6a4a"), 2f, 'X')
        used[benchY * KCOLS + benchX] = true
        stamp(benchX + 2, benchY, "BM ${metres(h)}", kcol("#ffb08a"))
    }

    private fun frameAndScale() {
        val c = ink
        for (x in 0 until KCOLS) {
            val tick = x % 16 == 0 && x > 0 && x < KCOLS - 1
            frame.put(x, 0, c, 2f, if (tick) '┬' else '─')
            frame.put(x, KROWS - 1, c, 2f, if (tick) '┴' else '─')
        }
        for (y in 0 until KROWS) {
            val tick = y % 8 == 0 && y > 0 && y < KROWS - 1
            frame.put(0, y, c, 2f, if (tick) '├' else '│')
            frame.put(KCOLS - 1, y, c, 2f, if (tick) '┤' else '│')
        }
        frame.put(0, 0, c, 2f, '┌')
        frame.put(KCOLS - 1, 0, c, 2f, '┐')
        frame.put(0, KROWS - 1, c, 2f, '└')
        frame.put(KCOLS - 1, KROWS - 1, c, 2f, '┘')
        // Scale bar and north arrow.
        val sx = 4
        val sy = KROWS - 3
        frame.text(sx - 1, sy, "                         ", c, KCol(0.03f, 0.035f, 0.03f))
        for (k in 0 until 20) frame.put(sx + k, sy, if ((k / 5) % 2 == 0) label else ink.scale(0.6f), 2f, '█')
        frame.text(sx + 21, sy, "2 km", label)
        frame.text(KCOLS - 5, 2, "N", label, KCol(0.03f, 0.035f, 0.03f))
        frame.put(KCOLS - 5, 3, label, 2f, '│')
        frame.put(KCOLS - 5, 4, label, 2f, '│')
    }

    private fun crosshair(phi: Float) {
        val cx = (KCOLS / 2f + 68f * sin(phi + 0.6f)).toInt()
        val cy = (KROWS / 2f + 15f * sin(2f * phi)).toInt()
        for (d in 2..5) {
            frame.put(cx - d, cy, cursor, 2f, '─')
            frame.put(cx + d, cy, cursor, 2f, '─')
        }
        for (d in 1..2) {
            frame.put(cx, cy - d, cursor, 2f, '│')
            frame.put(cx, cy + d, cursor, 2f, '│')
        }
        frame.put(cx, cy, KCol(1f, 1f, 1f), 2f, '┼')
        val h = height[(cy + 1) * W + cx + 1]
        val text = if (h < SEA) "WATER" else "${metres(h)} m"
        val coords = "${cx * 13 + 4100}E ${(KROWS - cy) * 81 + 2200}N"
        val left = cx + 7 + 14 > KCOLS - 2
        val tx = if (left) cx - 7 - 14 else cx + 7
        val ty = min(KROWS - 4, max(2, cy - 2))
        val back = KCol(0.02f, 0.06f, 0.07f)
        frame.text(tx, ty, "┌────────────┐", cursor, back)
        frame.text(tx, ty + 1, "│ " + text.padEnd(11) + "│", cursor, back)
        frame.text(tx, ty + 2, "│ " + coords.padEnd(11).take(11) + "│", cursor.scale(0.7f), back)
        frame.text(tx, ty + 3, "└────────────┘", cursor, back)
        for (k in 1 until 12) frame.put(tx + 1 + k, ty + 1, label, 2f, frame.ch[(ty + 1) * KCOLS + tx + 1 + k])
    }
}

/**
 * A living topographic map: an evolving height field drawn as shaded relief in halftone, tinted from green lowlands to
 * pale peaks, with lakes, minor and index contour lines that follow the slopes, elevation labels riding the index
 * lines, a marked summit and benchmark, a map border with ticks, a scale bar and a cursor crosshair sweeping across
 * with a live elevation readout. The seed shapes the terrain and the benchmark. Every motion repeats a whole number of
 * times per loop.
 */
public fun topographicContoursPiece(seed: Int = 0): AsciiPiece {
    val scene = Topo(seed)
    return asciiPiece(
        name = "topographic contours",
        ground = Rgb.fromHex("#070906"),
        category = "generative",
        cols = KCOLS,
        rows = KROWS,
        fps = 15,
        still = 4f,
        loop = LOOP,
        note = "an evolving terrain drawn as labelled contour lines over shaded relief, with a sweeping cursor",
    ) { time, grid -> scene.draw(kLoopTime(time, LOOP), grid) }
}

/**
 * A topographic map whose terrain slowly morphs: labelled contour lines over shaded relief with a sweeping cursor.
 *
 * @param seed shapes the terrain and where the benchmark sits.
 */
@Composable
public fun AsciiTopographicContours(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A topographic map with labelled contour lines and a moving cursor",
) {
    val piece = remember(seed) { topographicContoursPiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
