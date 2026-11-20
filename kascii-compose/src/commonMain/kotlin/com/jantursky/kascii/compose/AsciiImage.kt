package com.jantursky.kascii.compose

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.compose.internal.AsciiView
import com.jantursky.kascii.compose.internal.ClockSpec
import com.jantursky.kascii.compose.internal.FieldSource
import com.jantursky.kascii.compose.internal.asciiSemantics
import com.jantursky.kascii.core.Charset
import com.jantursky.kascii.core.Charsets
import com.jantursky.kascii.core.ColorMode
import com.jantursky.kascii.core.KasciiTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/** The longest side the image is sampled at: enough for Braille at small cells, cheap to sample. */
private const val MAX_SAMPLES = 512

/**
 * Draws [bitmap] as glyphs, stretched to fill [modifier]'s size. The image is sampled once per bitmap on a
 * background thread; nothing is drawn until that is done. Pass [SampledColors] as [colorMode] to color each glyph
 * like the pixels under it.
 */
@Composable
public fun AsciiImage(
    bitmap: ImageBitmap,
    modifier: Modifier = Modifier,
    charset: Charset = Charsets.Standard,
    cell: Dp = 8.dp,
    colorMode: ColorMode = ColorMode.Theme,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    theme: KasciiTheme = LocalKasciiTheme.current,
    contentDescription: String? = null,
) {
    var tones by remember(bitmap) { mutableStateOf<ImageTones?>(null) }
    val paper = theme.paper.toColor()
    LaunchedEffect(bitmap, paper) {
        val (w, h) = sampleSize(bitmap.width, bitmap.height)
        tones = withContext(Dispatchers.Default) { bitmap.toTones(w, h, paper) }
    }
    val sampled = tones
    if (sampled == null) {
        Spacer(modifier.asciiSemantics(contentDescription, isText = false))
        return
    }
    val source =
        remember(sampled, charset, colorMode) {
            FieldSource(sampled.toField(), charset, colorMode.withSamples(sampled))
        }
    AsciiView(source, cell, ClockSpec(running = false), renderer, theme, contentDescription, modifier)
}

/** The image's own size, scaled down so the longer side is at most [MAX_SAMPLES]. */
internal fun sampleSize(
    width: Int,
    height: Int,
): Pair<Int, Int> {
    val scale = minOf(1f, MAX_SAMPLES.toFloat() / max(width, height))
    return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
}
