package com.jantursky.kascii.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.jantursky.kascii.core.ColorField
import com.jantursky.kascii.core.ColorMode
import com.jantursky.kascii.core.Field
import com.jantursky.kascii.core.Fields
import com.jantursky.kascii.core.Rgb
import kotlin.math.abs
import kotlin.math.max

/**
 * An image sampled into a [width] by [height] grid: a tone in 0..1 and an opaque ARGB color per sample, both row
 * major. A tone says how far the sample stands out from the paper it was sampled against, so 0 is paper and 1 the
 * strongest contrast.
 *
 * @property width Samples per row.
 * @property height Rows of samples.
 * @property tones One tone per sample, in 0..1.
 * @property colors One opaque ARGB color per sample: the average of the pixels it covers, over the paper.
 */
public class ImageTones internal constructor(
    public val width: Int,
    public val height: Int,
    public val tones: FloatArray,
    public val colors: IntArray,
) {
    /** The tones as a still [Field], to draw with [AsciiCanvas] or rasterise with the core engine. */
    public fun toField(): Field = Fields.fromTones(tones, width, height)

    /** The colors as a [ColorField], for [ColorMode.Source]. */
    public fun toColorField(): ColorField =
        ColorField { x, y, _ ->
            val ix = (x * width).toInt().coerceIn(0, width - 1)
            val iy = (y * height).toInt().coerceIn(0, height - 1)
            Rgb(colors[iy * width + ix])
        }
}

/**
 * Colors each glyph with the average color of the pixels under its cell. Only [ascii] and [AsciiImage]
 * have pixels to sample; anywhere else glyphs take the theme ink.
 */
public val SampledColors: ColorMode = ColorMode.Source { _, _, _ -> Rgb(0) }

/**
 * Samples the image into a [width] by [height] grid by averaging the pixels each sample covers. [paper] is the
 * background the tones are measured against: on a dark paper bright pixels get high tones, on a light paper dark
 * ones do. Transparent pixels count as paper.
 */
public fun ImageBitmap.toTones(
    width: Int,
    height: Int,
    paper: Color = Color.Black,
): ImageTones {
    require(width > 0 && height > 0) { "Sample grid must not be empty" }
    val pixels = IntArray(this.width * this.height)
    readPixels(pixels)
    return sampleTones(pixels, this.width, this.height, width, height, paper.toRgb().argb)
}

/** [SampledColors] made concrete with [tones]' colors; any other mode as it is. */
internal fun ColorMode.withSamples(tones: ImageTones): ColorMode =
    if (this === SampledColors) ColorMode.Source(tones.toColorField()) else this

internal fun sampleTones(
    pixels: IntArray,
    imageWidth: Int,
    imageHeight: Int,
    width: Int,
    height: Int,
    paper: Int,
): ImageTones {
    val paperLuma = luma(paper)
    // Never below 0.5, so a mid-gray paper cannot blow tones up.
    val range = max(paperLuma, 1f - paperLuma)
    val tones = FloatArray(width * height)
    val colors = IntArray(width * height)
    for (sy in 0 until height) {
        val y0 = sy * imageHeight / height
        val y1 = max(y0 + 1, (sy + 1) * imageHeight / height)
        for (sx in 0 until width) {
            val x0 = sx * imageWidth / width
            val x1 = max(x0 + 1, (sx + 1) * imageWidth / width)
            val box = BoxAverage()
            for (y in y0 until y1) for (x in x0 until x1) box.add(pixels[y * imageWidth + x])
            val mean = box.mean(paper)
            tones[sy * width + sx] = (abs(luma(mean) - paperLuma) / range).coerceIn(0f, 1f)
            colors[sy * width + sx] = mean
        }
    }
    return ImageTones(width, height, tones, colors)
}

/** Sums colors weighted by alpha, so transparent pixels fall back to the paper. */
private class BoxAverage {
    private var r = 0f
    private var g = 0f
    private var b = 0f
    private var a = 0f
    private var n = 0

    fun add(argb: Int) {
        val alpha = (argb ushr 24) / 255f
        r += ((argb shr 16) and 0xFF) * alpha
        g += ((argb shr 8) and 0xFF) * alpha
        b += (argb and 0xFF) * alpha
        a += alpha
        n++
    }

    /** The opaque mean color, composited over [paper]. */
    fun mean(paper: Int): Int {
        val cover = a / n
        val rest = 1f - cover

        fun channel(
            sum: Float,
            shift: Int,
        ): Int = ((sum / n) + ((paper shr shift) and 0xFF) * rest + 0.5f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (channel(r, 16) shl 16) or (channel(g, 8) shl 8) or channel(b, 0)
    }
}

/** Rec. 709 luma of the gamma-encoded channels, 0..1. Cheap and close enough to perceived lightness for glyphs. */
private fun luma(argb: Int): Float =
    (0.2126f * ((argb shr 16) and 0xFF) + 0.7152f * ((argb shr 8) and 0xFF) + 0.0722f * (argb and 0xFF)) / 255f
