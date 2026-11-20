package com.jantursky.kascii.gallery

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
import com.jantursky.kascii.core.Rng
import com.jantursky.kascii.core.asciiPiece
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val COLS = 192
private const val ROWS = 48
private const val CELLS = COLS * ROWS
private const val LOOP = 8f
private const val HORIZON = 33
private const val TAU = AKit.TAU

private const val M_SKY = 0
private const val M_FAR = 1
private const val M_NEAR = 2
private const val M_STREET = 3

private const val KIND_STEADY = 0
private const val KIND_TOGGLE = 1
private const val KIND_TV = 2
private const val KIND_SOURCE = 3

private const val MAX_EMITTERS = 3000
private const val MAX_SOURCES = 16
private const val LANES = COLS + 160
private const val LANE_OFFSET = 80

private val BAYER =
    FloatArray(16) {
        intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)[it].let { v -> (v + 0.5f) / 16f - 0.5f }
    }

/** Hue families at full brightness; each is offered at six brightness levels in the quantised palette. */
private val HUES =
    arrayOf(
        floatArrayOf(0.30f, 0.30f, 1.00f),
        floatArrayOf(0.25f, 0.55f, 1.00f),
        floatArrayOf(0.45f, 0.75f, 1.00f),
        floatArrayOf(0.15f, 0.95f, 1.00f),
        floatArrayOf(0.20f, 0.90f, 0.75f),
        floatArrayOf(0.62f, 0.35f, 1.00f),
        floatArrayOf(0.85f, 0.30f, 0.95f),
        floatArrayOf(1.00f, 0.20f, 0.65f),
        floatArrayOf(1.00f, 0.20f, 0.20f),
        floatArrayOf(1.00f, 0.55f, 0.15f),
        floatArrayOf(1.00f, 0.75f, 0.30f),
        floatArrayOf(1.00f, 0.92f, 0.60f),
        floatArrayOf(0.55f, 0.65f, 0.85f),
        floatArrayOf(0.90f, 0.93f, 1.00f),
        floatArrayOf(0.40f, 0.42f, 0.62f),
    )
private val LEVELS = floatArrayOf(0.30f, 0.42f, 0.54f, 0.68f, 0.84f, 1.0f)

private val PALETTE_ARGB: IntArray =
    IntArray(HUES.size * LEVELS.size) {
        val h = HUES[it / LEVELS.size]
        val l = LEVELS[it % LEVELS.size]
        (0xFF shl 24) or ((h[0] * l * 255f).toInt() shl 16) or ((h[1] * l * 255f).toInt() shl 8) or
            (h[2] * l * 255f).toInt()
    }

/** 5 bits per channel to the nearest palette entry, built once for every piece. */
private val PALETTE_LUT: ByteArray by lazy {
    ByteArray(32768) { key ->
        val r = ((key shr 10) and 31) / 31f
        val g = ((key shr 5) and 31) / 31f
        val b = (key and 31) / 31f
        var best = 0
        var bestD = Float.MAX_VALUE
        for (p in PALETTE_ARGB.indices) {
            val h = HUES[p / LEVELS.size]
            val l = LEVELS[p % LEVELS.size]
            val dr = h[0] * l - r
            val dg = h[1] * l - g
            val db = h[2] * l - b
            val d = 0.3f * dr * dr + 0.5f * dg * dg + 0.2f * db * db
            if (d < bestD) {
                bestD = d
                best = p
            }
        }
        best.toByte()
    }
}

/** Bold neon letters, rows separated by '/', 5 pixels wide unless the rows are shorter. */
private val LETTERS7 =
    mapOf(
        'H' to "x...x/x...x/x...x/xxxxx/x...x/x...x/x...x",
        'O' to ".xxx./x...x/x...x/x...x/x...x/x...x/.xxx.",
        'T' to "xxxxx/..x../..x../..x../..x../..x../..x..",
        'E' to "xxxx/x.../x.../xxx./x.../x.../xxxx",
        'L' to "x.../x.../x.../x.../x.../x.../xxxx",
        'A' to ".xxx./x...x/x...x/xxxxx/x...x/x...x/x...x",
    )

private val LETTERS9 =
    mapOf(
        'B' to "xxxx./x...x/x...x/x...x/xxxx./x...x/x...x/x...x/xxxx.",
        'A' to ".xxx./x...x/x...x/x...x/xxxxx/x...x/x...x/x...x/x...x",
        'R' to "xxxx./x...x/x...x/x...x/xxxx./x.x../x..x./x...x/x...x",
    )

private val PINK = floatArrayOf(1.0f, 0.16f, 0.60f)
private val CYAN = floatArrayOf(0.10f, 0.85f, 1.0f)
private val LAMP = floatArrayOf(1.0f, 0.62f, 0.25f)

private class Building(
    val x0: Int,
    val x1: Int,
    val top: Int,
)

private val NEAR_BUILDINGS =
    listOf(
        Building(0, 57, 9),
        Building(60, 79, 18),
        Building(82, 134, 5),
        Building(137, 152, 14),
        Building(155, 191, 8),
    )

/** Everything that does not change from frame to frame, plus the per-frame scratch buffers. */
private class RainyCityScene(
    private val seed: Int,
    private val windowColor: FloatArray,
    private val rainColor: FloatArray,
    private val slant: Float,
) {
    private val mat = ByteArray(CELLS)
    private val baseR = FloatArray(CELLS)
    private val baseG = FloatArray(CELLS)
    private val baseB = FloatArray(CELLS)
    private val wet = FloatArray(CELLS)

    private val emIdx = IntArray(MAX_EMITTERS)
    private val emGlyph = CharArray(MAX_EMITTERS)
    private val emR = FloatArray(MAX_EMITTERS)
    private val emG = FloatArray(MAX_EMITTERS)
    private val emB = FloatArray(MAX_EMITTERS)
    private val emSrc = IntArray(MAX_EMITTERS)
    private val emLetter = IntArray(MAX_EMITTERS)
    private val emKind = IntArray(MAX_EMITTERS)
    private val emCycles = IntArray(MAX_EMITTERS)
    private val emPhase = FloatArray(MAX_EMITTERS)
    private var emitters = 0

    private val srcR = FloatArray(MAX_SOURCES)
    private val srcG = FloatArray(MAX_SOURCES)
    private val srcB = FloatArray(MAX_SOURCES)
    private val srcGain = FloatArray(MAX_SOURCES)
    private val srcKind = IntArray(MAX_SOURCES)
    private val srcI = FloatArray(MAX_SOURCES)
    private val srcFlicker = IntArray(MAX_SOURCES)
    private val glowIdx = Array(MAX_SOURCES) { IntArray(0) }
    private val glowW = Array(MAX_SOURCES) { FloatArray(0) }
    private var sources = 0
    private val signBox = ArrayList<IntArray>()
    private val letterMod = FloatArray(MAX_SOURCES * 8)

    private val skyR = FloatArray(HORIZON)
    private val skyG = FloatArray(HORIZON)
    private val skyB = FloatArray(HORIZON)

    private val laneOn = Array(2) { BooleanArray(LANES) }
    private val lanePhase = Array(2) { FloatArray(LANES) }
    private val laneTail = Array(2) { FloatArray(LANES) }

    private val fr = FloatArray(CELLS)
    private val fg = FloatArray(CELLS)
    private val fb = FloatArray(CELLS)
    private val rv = FloatArray(CELLS)
    private val solid = CharArray(CELLS)
    private val colBias = FloatArray(COLS) { Noise.hash(it, 0, 7, seed) - 0.5f }
    private val acc = FloatArray(CELLS)

    private val rainGlyph =
        if (slant > 0.1f) {
            '╱'
        } else if (slant < -0.1f) {
            '╲'
        } else {
            '│'
        }

    init {
        val rng = AKit.rng(seed, 51)
        buildSky()
        buildFarLayer(rng)
        buildNearLayer()
        buildStreet()
        buildRims()
        buildSignsAndShops()
        buildWindows()
        buildLamps()
        buildRainLanes()
    }

    private fun buildSky() {
        for (y in 0 until HORIZON) {
            val s = y / HORIZON.toFloat()
            val tone = 0.10f + 0.22f * s * s
            val mix = s * s
            skyR[y] = tone * (0.62f + 0.38f * mix)
            skyG[y] = tone * (0.35f + 0.0f * mix)
            skyB[y] = tone * (1.0f - 0.25f * mix)
        }
    }

    private fun buildFarLayer(rng: Rng) {
        var x = -rng.int(5)
        while (x < COLS) {
            val w = 9 + rng.int(11)
            val top = HORIZON - (15 + rng.int(14))
            for (yy in top until HORIZON) {
                for (xx in max(0, x) until min(COLS, x + w)) {
                    val i = yy * COLS + xx
                    mat[i] = M_FAR.toByte()
                    val k = 0.75f + 0.5f * Noise.value(xx * 0.4f, yy * 0.6f, 5f, seed)
                    baseR[i] = 0.020f * k
                    baseG[i] = 0.022f * k
                    baseB[i] = 0.036f * k
                }
            }
            var wy = top + 3
            while (wy < HORIZON - 3) {
                var wx = x + 2
                while (wx < x + w - 2) {
                    if (wx in 0 until COLS && Noise.hash(wx, wy, 31, seed) < 0.13f) {
                        val warm = Noise.hash(wx, wy, 32, seed) < 0.7f
                        val c = if (warm) windowColor else CYAN
                        addEmitter(
                            wy * COLS + wx,
                            '·',
                            c[0] * 0.35f,
                            c[1] * 0.35f,
                            c[2] * 0.35f,
                            KIND_STEADY,
                            -1,
                            0,
                            0,
                            0f,
                        )
                    }
                    wx += 3
                }
                wy += 3
            }
            x += w + rng.int(3)
        }
    }

    private fun buildRims() {
        for (b in NEAR_BUILDINGS) {
            for (x in b.x0..b.x1) addEmitter(b.top * COLS + x, '─', 0.10f, 0.10f, 0.22f, KIND_STEADY, -1, 0, 0, 0f)
            for (y in b.top + 1 until HORIZON) {
                addEmitter(y * COLS + b.x0, '│', 0.07f, 0.07f, 0.16f, KIND_STEADY, -1, 0, 0, 0f)
                addEmitter(y * COLS + b.x1, '│', 0.07f, 0.07f, 0.16f, KIND_STEADY, -1, 0, 0, 0f)
            }
        }
    }

    private fun buildNearLayer() {
        for (b in NEAR_BUILDINGS) {
            for (y in b.top until HORIZON) {
                for (x in b.x0..b.x1) {
                    mat[y * COLS + x] = M_NEAR.toByte()
                    baseR[y * COLS + x] = 0f
                    baseG[y * COLS + x] = 0f
                    baseB[y * COLS + x] = 0f
                }
            }
        }
        dropFarEmittersUnderNear()
    }

    private fun dropFarEmittersUnderNear() {
        var w = 0
        for (e in 0 until emitters) {
            if (mat[emIdx[e]].toInt() == M_NEAR) continue
            if (w != e) copyEmitter(e, w)
            w++
        }
        emitters = w
    }

    private fun copyEmitter(
        from: Int,
        to: Int,
    ) {
        emIdx[to] = emIdx[from]
        emGlyph[to] = emGlyph[from]
        emR[to] = emR[from]
        emG[to] = emG[from]
        emB[to] = emB[from]
        emSrc[to] = emSrc[from]
        emLetter[to] = emLetter[from]
        emKind[to] = emKind[from]
        emCycles[to] = emCycles[from]
        emPhase[to] = emPhase[from]
    }

    private fun buildStreet() {
        for (y in HORIZON until ROWS) {
            for (x in 0 until COLS) {
                val i = y * COLS + x
                mat[i] = M_STREET.toByte()
                val n = Noise.fbm(x * 0.06f, y * 0.25f, 1.5f, 2, seed)
                wet[i] = (0.55f + 1.4f * (n - 0.42f)).coerceIn(0.45f, 1f)
            }
        }
        // curb line and a dashed centre line
        for (x in 0 until COLS) {
            addEmitter(34 * COLS + x, '─', 0.045f, 0.045f, 0.10f, KIND_STEADY, -1, 0, 0, 0f)
            if ((x / 6) % 3 == 0) {
                addEmitter(44 * COLS + x, '─', 0.12f, 0.12f, 0.20f, KIND_STEADY, -1, 0, 0, 0f)
            }
        }
    }

    private fun addEmitter(
        idx: Int,
        glyph: Char,
        r: Float,
        g: Float,
        b: Float,
        kind: Int,
        src: Int,
        letter: Int,
        cycles: Int,
        phase: Float,
    ) {
        if (emitters >= MAX_EMITTERS) return
        val e = emitters++
        emIdx[e] = idx
        emGlyph[e] = glyph
        emR[e] = r
        emG[e] = g
        emB[e] = b
        emKind[e] = kind
        emSrc[e] = src
        emLetter[e] = letter
        emCycles[e] = cycles
        emPhase[e] = phase
    }

    private fun newSource(
        color: FloatArray,
        gain: Float,
        kind: Int,
        flicker: Int,
    ): Int {
        val s = sources++
        srcR[s] = color[0]
        srcG[s] = color[1]
        srcB[s] = color[2]
        srcGain[s] = gain
        srcKind[s] = kind
        srcFlicker[s] = flicker
        for (l in 0 until 8) letterMod[s * 8 + l] = 1f
        return s
    }

    private fun buildSignsAndShops() {
        sign("HOTEL", 2, 12, 2, 2, 1, CYAN, 3, LETTERS7)
        sign("BAR", 83, 8, 3, 3, 1, PINK, 1, LETTERS9)
        sign("TEA", 158, 12, 2, 2, 1, CYAN, 0, LETTERS7)
        // a vertical tube on the corner of the narrow tower
        val tube = newSource(CYAN, 0.4f, 0, -1)
        for (y in 17..29) addEmitter(y * COLS + 139, '█', CYAN[0], CYAN[1], CYAN[2], KIND_SOURCE, tube, -1, 0, 0f)
        buildGlow(tube, 6f, 3f)
        // a horizontal tube above the lower shop
        val bar = newSource(PINK, 0.4f, 0, -1)
        for (x in 62..77) addEmitter(24 * COLS + x, '█', PINK[0], PINK[1], PINK[2], KIND_SOURCE, bar, -1, 0, 0f)
        buildGlow(bar, 6f, 3.2f)
        shop(62, 78, 29, floatArrayOf(1.0f, 0.68f, 0.28f))
        shop(10, 28, 30, floatArrayOf(1.0f, 0.5f, 0.3f))
        shop(86, 100, 30, floatArrayOf(1.0f, 0.55f, 0.30f))
        shop(112, 128, 30, floatArrayOf(0.35f, 0.85f, 1.0f))
        shop(139, 150, 29, floatArrayOf(1.0f, 0.78f, 0.35f))
        shop(160, 176, 30, floatArrayOf(1.0f, 0.62f, 0.3f))
    }

    private fun sign(
        text: String,
        x0: Int,
        y0: Int,
        sx: Int,
        gap: Int,
        sy: Int,
        color: FloatArray,
        flickerLetter: Int,
        font: Map<Char, String>,
    ) {
        val src = newSource(color, 0.46f, 0, flickerLetter)
        var x = x0
        var rowsN = 0
        for ((li, ch) in text.withIndex()) {
            val rows = font.getValue(ch).split('/')
            rowsN = rows.size
            for (py in rows.indices) {
                for (px in rows[py].indices) {
                    if (rows[py][px] != 'x') continue
                    for (k in 0 until sx) {
                        for (j in 0 until sy) {
                            val cx = x + px * sx + k
                            addEmitter(
                                (y0 + py * sy + j) * COLS + cx,
                                '█',
                                color[0],
                                color[1],
                                color[2],
                                KIND_SOURCE,
                                src,
                                li,
                                0,
                                0f,
                            )
                        }
                    }
                }
            }
            x += rows[0].length * sx + gap
        }
        buildGlow(src, 7f + 1.5f * sx, 3.2f + 1.2f * sy)
        signBox.add(intArrayOf(x0, y0, x - gap - 1, y0 + rowsN * sy - 1))
        // keep the lettering crisp: no glow between or on the letters, only around them
        val x1 = x - gap - 1
        val y1 = y0 + rowsN * sy - 1
        val idx = glowIdx[src]
        val w = glowW[src]
        for (k in idx.indices) {
            val cx = idx[k] % COLS
            val cy = idx[k] / COLS
            if (cx in (x0 - 1)..(x1 + 1) && cy in (y0 - 1)..(y1 + 1)) w[k] = 0f
            if (mat[idx[k]].toInt() != M_NEAR) w[k] = 0f
        }
    }

    private fun shop(
        x0: Int,
        x1: Int,
        topRow: Int,
        color: FloatArray,
    ) {
        val src = newSource(color, 0.36f, 2, -1)
        for (y in topRow..HORIZON - 1) {
            for (x in x0..x1) {
                if ((x - x0) % 6 == 5) continue
                addEmitter(
                    y * COLS + x,
                    '█',
                    color[0] * 0.9f,
                    color[1] * 0.9f,
                    color[2] * 0.9f,
                    KIND_SOURCE,
                    src,
                    -1,
                    0,
                    0f,
                )
            }
        }
        buildGlow(src, 8f, 3.4f)
    }

    private fun buildLamps() {
        for (x in intArrayOf(58, 106, 153)) {
            val src = newSource(LAMP, 0.75f, 1, -1)
            addEmitter(22 * COLS + x, '●', LAMP[0], LAMP[1], LAMP[2], KIND_SOURCE, src, -1, 0, 0f)
            addEmitter(
                22 * COLS + x + 1,
                '▄',
                LAMP[0] * 0.8f,
                LAMP[1] * 0.8f,
                LAMP[2] * 0.8f,
                KIND_SOURCE,
                src,
                -1,
                0,
                0f,
            )
            for (y in 23..34) addEmitter(y * COLS + x, '│', 0.24f, 0.25f, 0.34f, KIND_STEADY, -1, 0, 0, 0f)
            for (y in 24..28 step 2) {
                addEmitter(
                    y * COLS + x,
                    '│',
                    LAMP[0] * 0.5f,
                    LAMP[1] * 0.5f,
                    LAMP[2] * 0.5f,
                    KIND_SOURCE,
                    src,
                    -1,
                    0,
                    0f,
                )
            }
            buildGlow(src, 11f, 6.5f, 22 * COLS + x)
        }
    }

    private fun buildWindows() {
        for ((bi, b) in NEAR_BUILDINGS.withIndex()) {
            var y = b.top + 2
            var floorNo = 0
            while (y < HORIZON - 5) {
                val floorBias = 0.10f + 0.6f * Noise.hash(floorNo, bi, 41, seed)
                var x = b.x0 + 2
                while (x + 1 <= b.x1 - 1) {
                    if (!windowBlocked(x, y)) {
                        if (Noise.hash(x, y, 42, seed) < floorBias) addWindow(x, y) else addDarkWindow(x, y)
                    }
                    x += 4
                }
                y += 3
                floorNo++
            }
        }
    }

    private fun windowBlocked(
        x: Int,
        y: Int,
    ): Boolean {
        for (b in signBox) if (y in (b[1] - 2)..(b[3] + 2) && x + 1 >= b[0] - 2 && x <= b[2] + 2) return true
        for (e in 0 until emitters) {
            if (emKind[e] != KIND_SOURCE) continue
            val ey = emIdx[e] / COLS
            val ex = emIdx[e] % COLS
            if (ey in (y - 2)..(y + 2) && ex in (x - 2)..(x + 3)) return true
        }
        return false
    }

    private fun addDarkWindow(
        x: Int,
        y: Int,
    ) {
        for (k in 0..1) addEmitter(y * COLS + x + k, '·', 0.20f, 0.22f, 0.40f, KIND_STEADY, -1, 0, 0, 0f)
    }

    private fun addWindow(
        x: Int,
        y: Int,
    ) {
        val roll = Noise.hash(x, y, 43, seed)
        val c: FloatArray
        var kind = KIND_STEADY
        if (roll < 0.8f) {
            c = windowColor
        } else if (roll < 0.92f) {
            c = floatArrayOf(0.8f, 0.9f, 1.0f)
        } else {
            c = floatArrayOf(0.35f, 0.6f, 1.0f)
            kind = KIND_TV
        }
        val toggles = kind == KIND_STEADY && Noise.hash(x, y, 44, seed) < 0.12f
        if (toggles) kind = KIND_TOGGLE
        val cycles = 1 + (Noise.hash(x, y, 45, seed) * 2f).toInt()
        val phase = Noise.hash(x, y, 46, seed)
        for (k in 0..1) {
            addEmitter((y) * COLS + x + k, '█', c[0] * 0.78f, c[1] * 0.78f, c[2] * 0.78f, kind, -1, 0, cycles, phase)
        }
    }

    /** Splats a soft ellipse around every emitter of [src] (or one anchor cell) into a sparse weight list. */
    private fun buildGlow(
        src: Int,
        rx: Float,
        ry: Float,
        anchor: Int = -1,
    ) {
        acc.fill(0f)
        val splat = { idx: Int ->
            val cy = idx / COLS
            val cx = idx % COLS
            for (dy in -ry.toInt()..ry.toInt()) {
                for (dx in -rx.toInt()..rx.toInt()) {
                    val x = cx + dx
                    val y = cy + dy
                    if (x !in 0 until COLS || y !in 0 until ROWS) continue
                    val d2 = (dx / rx) * (dx / rx) + (dy / ry) * (dy / ry)
                    if (d2 >= 1f) continue
                    val f = 1f - d2
                    acc[y * COLS + x] += f * f
                }
            }
        }
        if (anchor >= 0) {
            splat(anchor)
        } else {
            for (e in 0 until emitters) if (emSrc[e] == src && emKind[e] == KIND_SOURCE) splat(emIdx[e])
        }
        val norm = if (anchor >= 0) 0.75f else 0.16f
        var n = 0
        for (i in 0 until CELLS) if (acc[i] > 0.02f) n++
        val idx = IntArray(n)
        val w = FloatArray(n)
        var k = 0
        for (i in 0 until CELLS) {
            if (acc[i] <= 0.02f) continue
            val recv =
                when (mat[i].toInt()) {
                    M_NEAR -> 1f
                    M_FAR -> 0.2f
                    M_SKY -> 0.08f
                    else -> 0.3f
                }
            idx[k] = i
            w[k] = (1f - exp(-acc[i] * norm)) * recv * srcGain[src]
            k++
        }
        glowIdx[src] = idx
        glowW[src] = w
    }

    private fun buildRainLanes() {
        for (layer in 0..1) {
            for (l in 0 until LANES) {
                laneOn[layer][l] = Noise.hash(l, 0, 20 + layer, seed) < (if (layer == 0) 0.115f else 0.10f)
                lanePhase[layer][l] = Noise.hash(l, 1, 20 + layer, seed)
                laneTail[layer][l] = Noise.hash(l, 2, 20 + layer, seed)
            }
        }
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        val theta = TAU * t / LOOP
        updateSources(t)
        baseR.copyInto(fr, 0, 0, CELLS)
        baseG.copyInto(fg, 0, 0, CELLS)
        baseB.copyInto(fb, 0, 0, CELLS)
        rv.fill(0f)
        solid.fill(0.toChar())
        paintSky(theta)
        paintGlows()
        paintEmitters(t)
        paintStreet(theta)
        paintCars(t)
        output(t, grid)
    }

    private fun flick(
        t: Float,
        cycles: Int,
        phase: Float,
    ): Float {
        val ph = AKit.frac(cycles * t / LOOP + phase)
        return if (ph < 0.05f) {
            0.25f
        } else if (ph < 0.09f) {
            0.8f
        } else {
            1f
        }
    }

    private fun updateSources(t: Float) {
        val theta = TAU * t / LOOP
        for (s in 0 until sources) {
            when (srcKind[s]) {
                0 -> {
                    val shimmer = 0.94f + 0.06f * sin(theta * 3f + s)
                    for (l in 0 until 8) letterMod[s * 8 + l] = 1f
                    var dip = 1f
                    if (srcFlicker[s] >= 0) {
                        dip = flick(t, 2, 0.31f * (s + 1))
                        letterMod[s * 8 + srcFlicker[s]] = dip
                    }
                    srcI[s] = shimmer * (0.88f + 0.12f * dip)
                }

                1 -> {
                    srcI[s] = 0.92f + 0.08f * sin(theta + s * 2f)
                }

                else -> {
                    srcI[s] = 0.95f + 0.05f * sin(theta * 2f + s)
                }
            }
        }
    }

    private fun paintSky(theta: Float) {
        val ox = 3f * cos(theta)
        val oy = 1.5f * sin(theta)
        for (y in 0 until HORIZON) {
            for (x in 0 until COLS) {
                val i = y * COLS + x
                if (mat[i].toInt() != M_SKY) continue
                val c = Noise.fbm(x * 0.05f + ox, y * 0.16f + oy, 2f, 2, seed)
                val k = 0.85f + 0.3f * c
                fr[i] = skyR[y] * k
                fg[i] = skyG[y] * k
                fb[i] = skyB[y] * k
            }
        }
    }

    private fun paintGlows() {
        for (s in 0 until sources) {
            val gi = srcI[s]
            val idx = glowIdx[s]
            val w = glowW[s]
            val r = srcR[s] * gi
            val g = srcG[s] * gi
            val b = srcB[s] * gi
            for (k in idx.indices) {
                val i = idx[k]
                fr[i] += r * w[k]
                fg[i] += g * w[k]
                fb[i] += b * w[k]
            }
        }
    }

    private fun paintEmitters(t: Float) {
        for (e in 0 until emitters) {
            var k = 1f
            when (emKind[e]) {
                KIND_TOGGLE -> {
                    k = if (AKit.frac(emPhase[e] + emCycles[e] * t / LOOP) < 0.6f) 1f else 0f
                }

                KIND_TV -> {
                    k = 0.7f + 0.3f * sin(TAU * (emCycles[e] * t / LOOP + emPhase[e]))
                }

                KIND_SOURCE -> {
                    val s = emSrc[e]
                    k = srcI[s]
                    val l = emLetter[e]
                    if (l >= 0) k *= letterMod[s * 8 + l]
                }
            }
            if (k <= 0.02f) continue
            val i = emIdx[e]
            fr[i] = emR[e] * k
            fg[i] = emG[e] * k
            fb[i] = emB[e] * k
            solid[i] = emGlyph[e]
        }
    }

    private fun paintStreet(theta: Float) {
        for (y in HORIZON + 1 until ROWS) {
            val d = y - HORIZON
            val centre = HORIZON - 1 - d * 2.3f
            val fall = 0.75f * (1f - 0.5f * d / (ROWS - HORIZON))
            for (x in 0 until COLS) {
                val i = y * COLS + x
                val sx = (1.2f * sin(0.7f * d + 2f * theta + x * 0.17f)).toInt()
                val xx = (x + sx).coerceIn(0, COLS - 1)
                var r = 0f
                var g = 0f
                var b = 0f
                for (j in -1..1) {
                    val sy = (centre + j).toInt().coerceIn(0, HORIZON - 1)
                    for (dx in -2..2) {
                        val s = sy * COLS + (xx + dx).coerceIn(0, COLS - 1)
                        val k =
                            if (dx == 0) {
                                2f
                            } else if (dx == 1 || dx == -1) {
                                1.5f
                            } else {
                                0.5f
                            }
                        r += fr[s] * k
                        g += fg[s] * k
                        b += fb[s] * k
                    }
                }
                r = max(0f, r / 18f - 0.12f)
                g = max(0f, g / 18f - 0.12f)
                b = max(0f, b / 18f - 0.12f)
                val f = fall * wet[i] * 1.3f
                r *= f
                g *= f
                b *= f
                fr[i] += r
                fg[i] += g
                fb[i] += b
                rv[i] = max(r, max(g, b))
            }
        }
    }

    private fun paintCars(t: Float) {
        car(t, 38, 6, floatArrayOf(1f, 0.14f, 0.18f), -1, 0.15f)
        car(t, 41, 8, floatArrayOf(1f, 0.9f, 0.62f), 1, 0.55f)
        car(t, 46, 11, floatArrayOf(1f, 0.93f, 0.7f), -1, 0.85f)
    }

    private fun car(
        t: Float,
        row: Int,
        gap: Int,
        c: FloatArray,
        dir: Int,
        phase: Float,
    ) {
        val p = AKit.frac(t / LOOP + phase)
        val env = sqrt(sin(PI.toFloat() * p))
        val x0 = (COLS * (0.25f + 0.5f * (phase * 3.7f % 1f)) + dir * (p - 0.5f) * 70f).toInt()
        for (lamp in 0..1) {
            val cx = x0 + lamp * gap
            for (dy in -2..2) {
                for (dx in -9..9) {
                    val x = cx + dx
                    val y = row + dy
                    if (x !in 0 until COLS || y !in 0 until ROWS) continue
                    val d2 = (dx / 9f) * (dx / 9f) + (dy / 1.8f) * (dy / 1.8f)
                    if (d2 >= 1f) continue
                    val f = (1f - d2) * (1f - d2) * 0.55f * env
                    val i = y * COLS + x
                    fr[i] += c[0] * f
                    fg[i] += c[1] * f
                    fb[i] += c[2] * f
                }
            }
            if (cx !in 0 until COLS || env < 0.1f) continue
            val i = row * COLS + cx
            fr[i] = c[0] * env
            fg[i] = c[1] * env
            fb[i] = c[2] * env
            solid[i] = '●'
            if (cx + 1 < COLS) {
                fr[i + 1] = c[0] * env
                fg[i + 1] = c[1] * env
                fb[i + 1] = c[2] * env
                solid[i + 1] = '●'
            }
            for (k in 1..3) {
                val y = row + k
                if (y >= ROWS) break
                val j = y * COLS + cx
                val f = 0.6f * env * (1f - k / 4f)
                fr[j] += c[0] * f
                fg[j] += c[1] * f
                fb[j] += c[2] * f
                rv[j] = max(rv[j], f)
            }
        }
    }

    private fun rainAt(
        x: Int,
        y: Int,
        t: Float,
    ): Float {
        var best = 0f
        for (layer in 0..1) {
            val period = if (layer == 0) 26f else 40f
            val cycles = if (layer == 0) 7 else 11
            val lane = floor(x + y * slant).toInt() + LANE_OFFSET
            if (lane < 0 || lane >= LANES || !laneOn[layer][lane]) continue
            val s = AKit.frac(y / period - cycles * t / LOOP + lanePhase[layer][lane])
            val tail = (if (layer == 0) 0.14f else 0.09f) + 0.04f * laneTail[layer][lane]
            if (s < tail) best = max(best, (s / tail) * (if (layer == 0) 1f else 0.6f))
        }
        return best
    }

    private fun output(
        t: Float,
        grid: Grid,
    ) {
        val lut = PALETTE_LUT
        val glyphs = grid.glyphs
        val colors = grid.colors
        val rr = rainColor[0]
        val rg = rainColor[1]
        val rb = rainColor[2]
        for (y in 0 until ROWS) {
            for (x in 0 until COLS) {
                val i = y * COLS + x
                var r = fr[i]
                var g = fg[i]
                var b = fb[i]
                val m0 = max(r, max(g, b))
                val bay = BAYER[(y and 3) * 4 + (x and 3)]
                var glyph = solid[i]
                var m = m0
                if (glyph == 0.toChar()) {
                    glyph = ' '
                    if (mat[i].toInt() == M_STREET && rv[i] > 0.02f) {
                        val v = rv[i] + colBias[x] * 0.05f + bay * 0.03f
                        glyph =
                            if (v < 0.05f) {
                                ' '
                            } else if (v < 0.12f) {
                                '·'
                            } else if (v < 0.26f) {
                                '│'
                            } else {
                                '┃'
                            }
                    } else {
                        val v = m0 + bay * 0.16f
                        val mt = mat[i].toInt()
                        glyph =
                            if (mt == M_SKY || mt == M_FAR) {
                                if (v < 0.05f) {
                                    ' '
                                } else if (v < 0.22f) {
                                    '·'
                                } else if (v < 0.42f) {
                                    '░'
                                } else {
                                    '▒'
                                }
                            } else if (v < 0.08f) {
                                ' '
                            } else if (v < 0.2f) {
                                '·'
                            } else if (v < 0.36f) {
                                '•'
                            } else if (v < 0.62f) {
                                '●'
                            } else if (v < 0.9f) {
                                '▒'
                            } else {
                                '▓'
                            }
                    }
                    val rain = rainAt(x, y, t)
                    if (rain > 0f && m0 < 0.5f) {
                        val k = rain * (0.8f + 1.1f * m0)
                        r += rr * k
                        g += rg * k
                        b += rb * k
                        glyph = rainGlyph
                        m = max(r, max(g, b))
                    }
                }
                if (glyph == ' ') {
                    glyphs[i] = ' '
                    colors[i] = 0
                    continue
                }
                if (m < 0.001f) m = 0.001f
                val level = (0.30f + 1.1f * m).coerceAtMost(1f)
                val inv = level / m
                val key =
                    ((min(31, (r * inv * 31.99f).toInt())) shl 10) or
                        ((min(31, (g * inv * 31.99f).toInt())) shl 5) or
                        min(31, (b * inv * 31.99f).toInt())
                glyphs[i] = glyph
                colors[i] = PALETTE_ARGB[lut[key].toInt()]
            }
        }
    }
}

/**
 * A rainy night in a neon city, shaded cell by cell: a dim hazy sky, dark building silhouettes with a few lit
 * window grids, three big pink and cyan neon signs that bleed a halftone glow onto the walls, warm street lamps and
 * a wet road that mirrors every light as a vertical smear while sparse rain streaks fall and car lights drift past.
 * The seed shapes the far skyline and the pattern of lit windows; a sign flickers at the same moment of every loop.
 */
public fun rainyCityPiece(
    seed: Int = 0,
    window: Rgb = AKit.Amber,
    rain: Rgb = AKit.Blue,
    wind: Float = 0.35f,
): AsciiPiece {
    val scene =
        RainyCityScene(
            seed,
            floatArrayOf(window.red / 255f, window.green / 255f, window.blue / 255f),
            floatArrayOf(rain.red / 255f * 0.55f, rain.green / 255f * 0.55f, rain.blue / 255f * 0.55f),
            (wind * 1.15f).coerceIn(-1f, 1f),
        )
    return asciiPiece(
        name = "rainy city",
        ground = Rgb.fromHex("#07060f"),
        category = "scenes",
        cols = COLS,
        rows = ROWS,
        fps = 20,
        still = 2f,
        loop = LOOP,
        note = "neon signs and lit windows over a wet street, rain streaking down and car lights drifting by",
    ) { time, grid -> scene.draw(time, grid) }
}

/**
 * A rainy night in a neon city: lit windows, neon signs, a wet street with reflections, rain and car lights.
 *
 * @param seed builds the far skyline and the pattern of lit windows.
 * @param window the color of the lit windows.
 * @param rain the color of the rain.
 * @param wind how much the rain slants, 0 for straight down.
 */
@Composable
public fun AsciiRainyCity(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    window: Rgb = AKit.Amber,
    rain: Rgb = AKit.Blue,
    wind: Float = 0.35f,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A city street at night in the rain, neon signs glowing over the wet road",
) {
    val piece = remember(seed, window, rain, wind) { rainyCityPiece(seed, window, rain, wind) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
