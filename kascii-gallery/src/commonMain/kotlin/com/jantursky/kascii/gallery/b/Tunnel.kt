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
import com.jantursky.kascii.gallery.b.ease
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.sqrt

private const val COLS = 64
private const val ROWS = 24
private const val LOOP = 4f
private const val TILES = 8
private const val SECTORS = 8

/** Floor modulo, so tiles wrap on the negative side too. */
private fun wrap(i: Int): Int = ((i % TILES) + TILES) % TILES

/**
 * A flight down a tiled tunnel. Each cell is turned into the angle and the depth of the wall behind it; the wall
 * is an eight by eight patchwork of tiles whose tone and color come from a hash of the seed, and it slides
 * towards the viewer while it twists, a whole number of tiles per loop so it closes every 4 seconds. The tone
 * fades to nothing at the vanishing point. Glyphs come from [charset], colors from [colors].
 *
 * Ported from ascii.rest `tunnel` (MIT, Copyright (c) 2026 bas3line): the same angle and depth mapping,
 * re-implemented with hashed tiles, a closed loop and colors.
 */
public fun tunnelPiece(
    seed: Int = 0,
    charset: Charset.Ramp = Charsets.Standard,
    colors: List<Rgb> = listOf(Palette.Violet, Palette.Blue, Palette.Red),
): AsciiPiece = asciiPiece(
    name = "tunnel",
    category = "effects",
    cols = COLS,
    rows = ROWS,
    fps = 24,
    still = 1f,
    loop = LOOP,
    note = "a flight down a twisting tiled tunnel",
) { t, grid ->
    val progress = t / LOOP
    for (y in 0 until ROWS) for (x in 0 until COLS) {
        val px = (x + 0.5f) / COLS * 2f - 1f
        val py = ((y + 0.5f) / ROWS * 2f - 1f) * ROWS * 2f / COLS
        val radius = sqrt(px * px + py * py)
        val angle = atan2(py, px) / PI.toFloat()
        val u = floor(angle * SECTORS / 2f + 2f * progress * TILES / 2f).toInt()
        val v = floor(0.6f / radius * 4f + TILES * progress).toInt()
        val tile = Noise.hash(wrap(u), wrap(v), 0, seed)
        val checker = if ((u + v) % 2 == 0) 1f else 0f
        val tone = (0.3f * checker + 0.7f * tile) * ease((radius - 0.04f) / 0.5f)
        val glyph = charset.glyphFor(tone)
        if (glyph != ' ') grid.put(x, y, glyph, colors[(tile * colors.size).toInt() % colors.size])
    }
}

/**
 * A twisting tunnel of colored tiles seen head on. [seed] lays the tiles, [charset] shades them and [colors] are
 * handed out across the tiles. Ported from ascii.rest `tunnel`.
 */
@Composable
public fun AsciiTunnel(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    charset: Charset.Ramp = Charsets.Standard,
    colors: List<Rgb> = listOf(Palette.Violet, Palette.Blue, Palette.Red),
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A flight down a twisting tunnel of colored tiles",
) {
    val piece = remember(seed, charset, colors) { tunnelPiece(seed, charset, colors) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
