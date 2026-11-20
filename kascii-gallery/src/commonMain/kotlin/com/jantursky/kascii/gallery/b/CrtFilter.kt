package com.jantursky.kascii.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.AsciiPieceView
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.Charset
import com.jantursky.kascii.core.Charsets
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.Noise
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.asciiPiece
import com.jantursky.kascii.gallery.b.Palette
import com.jantursky.kascii.gallery.b.frac
import com.jantursky.kascii.gallery.b.frameIndex
import kotlin.math.abs
import kotlin.math.sqrt

private const val COLS = 64
private const val ROWS = 24
private const val FPS = 24
private const val LOOP = 4f
private const val BARS_END = 0.62f
private const val STEPS_START = 0.74f
private const val CURVE = 0.5f

private val BARS =
    listOf(
        Rgb.fromHex("#cad5e2"),
        Palette.Amber,
        Palette.Blue,
        Palette.Green,
        Palette.Violet,
        Palette.Red,
        Rgb.fromHex("#3b4ba8"),
    )

private fun Rgb.tone(): Float = (0.3f * red + 0.59f * green + 0.11f * blue) / 255f

/** The built-in test pattern at a point of the picture in 0..1: color bars, a ring, a strip and a gray ramp. */
private fun pattern(
    u: Float,
    v: Float,
): Rgb? {
    val ring = abs(sqrt((u - 0.5f) * (u - 0.5f) + (v - 0.31f) * (v - 0.31f) * 2.2f) - 0.2f) < 0.018f
    return when {
        ring -> Rgb.White
        v < BARS_END -> BARS[(u * BARS.size).toInt().coerceAtMost(BARS.size - 1)]
        v < STEPS_START -> if ((u * BARS.size).toInt() % 2 == 0) BARS[BARS.size - 1 - (u * BARS.size).toInt()] else null
        else -> (255 * (u * 8f).toInt() / 7).let { Rgb(it, it, it) }
    }
}

/** The tone of one cell of the bent, jittered screen: pattern, phosphor glow and the rolling hum bar. */
private fun sample(
    x: Int,
    y: Int,
    t: Float,
    frame: Int,
    seed: Int,
): Pair<Float, Int> {
    val cx = (x + 0.5f) / COLS - 0.5f
    val cy = (y + 0.5f) / ROWS - 0.5f
    val bend = 1f + CURVE * (cx * cx + cy * cy)
    val jitter = if (Noise.hash(y, frame, 1, seed) > 0.96f) 0.03f * (if (frame % 2 == 0) 1 else -1) else 0f
    val u = 0.5f + cx * bend + jitter
    val v = 0.5f + cy * bend
    if (u !in 0f..1f || v !in 0f..1f) return 0f to 0
    val color = pattern(u, v) ?: return 0f to 0
    val hum = 0.2f * (1f - 4f * abs(frac(v - t / LOOP) - 0.5f).coerceAtMost(0.25f))
    val vignette = 1f - 1.1f * (cx * cx + cy * cy)
    val grain = 0.06f * Noise.hash(x, y, frame, seed)
    val gray = color.red == color.green && color.green == color.blue
    return ((color.tone() + hum + grain) * vignette) to (if (gray) 0 else color.argb)
}

/**
 * A CRT monitor showing a built-in test pattern: color bars, a ring and a gray ramp, bent at the edges, with
 * dimmed scanlines on every second row, a phosphor glow that bleeds into the neighbouring cells, a slow hum bar
 * rolling up the screen and now and then a row that tears sideways. The seed picks the tears and the grain, which
 * repeat every 4 seconds. Original piece in the style of the media filters of ascii.rest.
 */
public fun crtFilterPiece(
    seed: Int = 0,
    charset: Charset.Ramp = Charsets.Standard,
): AsciiPiece =
    asciiPiece(
        name = "crt filter",
        category = "media",
        cols = COLS,
        rows = ROWS,
        fps = FPS,
        still = 1f,
        loop = LOOP,
        note = "scanlines and phosphor glow over a test pattern",
    ) { t, grid ->
        val frame = frameIndex(t, FPS) % (FPS * LOOP.toInt())
        val cells = Array(COLS * ROWS) { sample(it % COLS, it / COLS, t, frame, seed) }
        for (y in 0 until ROWS) {
            for (x in 0 until COLS) {
                var glow = 0f
                for ((dx, dy) in NEIGHBOURS) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until COLS && ny in 0 until ROWS) glow += cells[ny * COLS + nx].first
                }
                val own = cells[y * COLS + x]
                val lit = (own.first + 0.1f * glow) * (if (y % 2 == 1) 0.6f else 1f)
                val glyph = charset.glyphFor(lit.coerceIn(0f, 1f))
                if (glyph != ' ') grid.put(x, y, glyph, if (own.second == 0) null else Rgb(own.second))
            }
        }
    }

private val NEIGHBOURS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

/**
 * A CRT test pattern with scanlines, phosphor glow and tearing. [seed] picks the tears and the grain, [charset]
 * the glyphs that turn the light into text.
 */
@Composable
public fun AsciiCrtFilter(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    charset: Charset.Ramp = Charsets.Standard,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A CRT monitor test pattern with scanlines and a phosphor glow",
) {
    val piece = remember(seed, charset) { crtFilterPiece(seed, charset) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
