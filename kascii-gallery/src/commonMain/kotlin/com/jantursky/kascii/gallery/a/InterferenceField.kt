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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val COLS = 72
private const val ROWS = 26
private const val LOOP = 8f
private const val FREQUENCY = 22f
private const val WARP = 0.3f

/**
 * A shader-style field: waves from orbiting point sources interfere, their distances bent by a frozen noise map,
 * and the summed wave picks a glyph and a blend between two colors. Every source makes one orbit per loop and
 * the wave advances two periods, so the loop is seamless. Original, in the spirit of the noise and interference
 * fields on asciicn.dev.
 */
public fun interferencePiece(
    seed: Int = 0,
    sources: Int = 3,
    charset: Charset.Ramp = Charsets.Standard,
    from: Rgb = AKit.Blue,
    to: Rgb = AKit.Violet,
): AsciiPiece {
    require(sources > 0) { "sources must be positive" }
    return asciiPiece(
        name = "interference field",
        category = "shaders",
        cols = COLS,
        rows = ROWS,
        fps = 20,
        still = 1.5f,
        loop = LOOP,
        note = "waves from orbiting sources interfering through a warped medium",
    ) { t, grid ->
        val angle = AKit.TAU * t / LOOP
        val height = ROWS * 2f
        val xs = FloatArray(sources) { 0.69f + 0.45f * cos(angle + AKit.TAU * it / sources) }
        val ys = FloatArray(sources) { 0.5f + 0.3f * sin(angle + AKit.TAU * it / sources) }
        for (row in 0 until ROWS) for (col in 0 until COLS) {
            val ux = (col + 0.5f) / height
            val uy = (row + 0.5f) / ROWS
            val bend = (Noise.value(ux * 3f, uy * 3f, 0f, seed) - 0.5f) * WARP
            var sum = 0f
            for (k in 0 until sources) {
                val dx = ux - xs[k]
                val dy = uy - ys[k]
                sum += sin((sqrt(dx * dx + dy * dy) + bend) * FREQUENCY - 2f * angle)
            }
            val tone = sum / sources * 0.5f + 0.5f
            val glyph = charset.glyphFor(tone)
            if (glyph != ' ') grid.put(col, row, glyph, from.lerp(to, tone))
        }
    }
}

/**
 * An interference field: waves from orbiting sources, warped by noise, drawn as glyphs.
 *
 * @param seed shapes the noise that bends the waves.
 * @param sources how many wave sources.
 * @param charset the glyphs from trough to crest.
 * @param from the color of troughs.
 * @param to the color of crests.
 */
@Composable
public fun InterferenceField(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    sources: Int = 3,
    charset: Charset.Ramp = Charsets.Standard,
    from: Rgb = AKit.Blue,
    to: Rgb = AKit.Violet,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Waves from several moving sources interfering in a rippling pattern of glyphs",
) {
    val piece = remember(seed, sources, charset, from, to) { interferencePiece(seed, sources, charset, from, to) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
