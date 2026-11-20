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
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.asciiPiece
import com.jantursky.kascii.gallery.b.Palette
import com.jantursky.kascii.gallery.b.gradient
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

private const val COLS = 64
private const val ROWS = 24
private const val LOOP = 24.0

// Seahorse valley, a spot that stays interesting at every zoom.
private const val CENTER_RE = -0.743643887037151
private const val CENTER_IM = 0.131825904205330
private const val ZOOM_OUT = 1.4
private const val ZOOM_IN = 0.004

/** The smooth escape time of the point `re + i im`, or -1 when it stays bounded within [maxIter] steps. */
private fun escape(re: Double, im: Double, maxIter: Int): Double {
    var zr = 0.0
    var zi = 0.0
    for (i in 0 until maxIter) {
        val r2 = zr * zr
        val i2 = zi * zi
        if (r2 + i2 > 256.0) return i + 1 - ln(ln(sqrt(r2 + i2))) / ln(2.0)
        zi = 2.0 * zr * zi + im
        zr = r2 - i2 + re
    }
    return -1.0
}

/**
 * The Mandelbrot set under a slow zoom into the seahorse valley and back out, so it loops every 24 seconds. The
 * escape time picks a glyph from [charset] and a color from [palette]; the bounded interior stays empty. No seed:
 * the picture is the same every time.
 *
 * Ported from ascii.rest `mandelbrot` (MIT, Copyright (c) 2026 bas3line): escape-time shading in plain glyphs,
 * extended with a smooth zoom loop, a color per cell and doubles for deep zooms.
 */
public fun mandelbrotPiece(
    maxIter: Int = 96,
    charset: Charset.Ramp = Charsets.Standard,
    palette: List<Rgb> = listOf(Palette.Violet, Palette.Blue, Palette.Green, Palette.Amber),
): AsciiPiece = asciiPiece(
    name = "mandelbrot",
    category = "generative",
    cols = COLS,
    rows = ROWS,
    fps = 20,
    still = 4f,
    loop = LOOP.toFloat(),
    note = "the mandelbrot set zooming into the seahorse valley and back",
) { t, grid ->
    val breath = 0.5 - 0.5 * cos(2.0 * PI * t / LOOP)
    val width = ZOOM_OUT * exp(ln(ZOOM_IN / ZOOM_OUT) * breath)
    // A cell is twice as tall as it is wide.
    val height = width * ROWS * 2.0 / COLS
    for (y in 0 until ROWS) for (x in 0 until COLS) {
        val re = CENTER_RE + ((x + 0.5) / COLS - 0.5) * width
        val im = CENTER_IM + ((y + 0.5) / ROWS - 0.5) * height
        val steps = escape(re, im, maxIter)
        if (steps >= 0.0) {
            val tone = (steps / maxIter).pow(0.4).toFloat().coerceIn(0f, 1f)
            val glyph = charset.glyphFor(tone)
            if (glyph != ' ') grid.put(x, y, glyph, gradient(palette, tone))
        }
    }
}

/**
 * The Mandelbrot set zooming in and out of the seahorse valley. [maxIter] sets the detail, [charset] the glyphs
 * and [palette] the colors from fast to slow escape.
 */
@Composable
public fun AsciiMandelbrot(
    modifier: Modifier = Modifier,
    maxIter: Int = 96,
    charset: Charset.Ramp = Charsets.Standard,
    palette: List<Rgb> = listOf(Palette.Violet, Palette.Blue, Palette.Green, Palette.Amber),
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "The Mandelbrot set slowly zooming in and out",
) {
    val piece = remember(maxIter, charset, palette) { mandelbrotPiece(maxIter, charset, palette) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
