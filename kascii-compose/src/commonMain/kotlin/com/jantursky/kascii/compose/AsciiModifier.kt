package com.jantursky.kascii.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.compose.internal.AsciiLayerElement
import com.jantursky.kascii.core.Charset
import com.jantursky.kascii.core.Charsets
import com.jantursky.kascii.core.ColorMode
import com.jantursky.kascii.core.KasciiTheme

/**
 * Turns the element it modifies into ASCII: the content is drawn to a layer, sampled into tones (and colors when
 * the [colorMode] asks for them) and drawn as glyphs on top.
 *
 * Sampling runs on a background thread, so a change in the content shows up a frame or so later. Pass
 * [SampledColors] to color glyphs like the content under them. [AsciiRenderer.Text] cannot apply to a modifier
 * and draws like [AsciiRenderer.Cpu].
 *
 * The content is sampled again whenever this modifier redraws, which covers changes in how the element is composed.
 * Content that animates inside a child's own graphics layer (a `graphicsLayer` block, scrolling, video) does not
 * make this modifier redraw, so pass [live] to sample on every frame instead. It costs a sample per frame. Glyphs use the bundled font as soon as it has loaded and the platform
 * monospace font before that.
 */
public fun Modifier.ascii(
    charset: Charset = Charsets.Blocks,
    cell: Dp = 8.dp,
    colorMode: ColorMode = ColorMode.Theme,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    theme: KasciiTheme? = null,
    live: Boolean = false,
): Modifier = this then AsciiLayerElement(charset, cell, colorMode, renderer, theme, live)
