package com.jantursky.kascii.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.Charset
import com.jantursky.kascii.core.Charsets
import com.jantursky.kascii.core.ColorMode
import com.jantursky.kascii.core.Field
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.Rgb

/** Which renderer draws the glyph grid. [Auto] picks the GPU path where it is available and worth it. */
public enum class AsciiRenderer {
    /** GPU shader when the platform supports it and the grid is large, otherwise [Cpu]. */
    Auto,

    /** Glyph atlas plus a runtime shader. Falls back to [Cpu] where unavailable. */
    Gpu,

    /** Glyph grid drawn from a cached atlas. */
    Cpu,

    /** Real selectable text. Slowest, but copyable and readable by accessibility services. */
    Text,
}

/** The theme every Kascii composable reads. Defaults to the dark or light built-in theme by system setting. */
public val LocalKasciiTheme: ProvidableCompositionLocal<KasciiTheme> = compositionLocalOf { KasciiTheme.Dark }

/** Provides [theme] to everything inside [content]. */
@Composable
public fun ProvideKasciiTheme(theme: KasciiTheme, content: @Composable () -> Unit) {
    TODO("implemented by the renderer work")
}

/** The built-in dark or light theme following the system setting. */
@Composable
public fun systemKasciiTheme(): KasciiTheme = TODO("implemented by the renderer work")

/** Builds a theme from Compose colors, for example Material 3's `onSurface` and `surface`. */
public fun kasciiThemeOf(ink: Color, paper: Color, accent: Color = ink, palette: List<Color> = listOf(ink)): KasciiTheme =
    KasciiTheme(ink.toRgb(), paper.toRgb(), accent.toRgb(), palette.map { it.toRgb() })

internal fun Color.toRgb(): Rgb = Rgb(((alpha * 255f + 0.5f).toInt() shl 24) or ((red * 255f + 0.5f).toInt() shl 16) or ((green * 255f + 0.5f).toInt() shl 8) or (blue * 255f + 0.5f).toInt())

/** The bundled monospace font family (JetBrains Mono, with Braille and katakana from a Unifont subset). */
@Composable
public fun kasciiFontFamily(): FontFamily = TODO("implemented by the renderer work")

/** Whether the platform asks for reduced motion. Combine with [Motion] through [rememberReducedMotion]. */
@Composable
public fun rememberReducedMotion(motion: Motion = Motion.System): Boolean = TODO("implemented by the renderer work")

/**
 * Draws a procedural [field] as glyphs. Animates while visible, pauses offscreen and in the background, and holds
 * a still frame under reduced motion. [cell] is the width of one glyph cell.
 */
@Composable
public fun AsciiCanvas(
    field: Field,
    modifier: Modifier = Modifier,
    charset: Charset = Charsets.Standard,
    cell: Dp = 8.dp,
    colorMode: ColorMode = ColorMode.Theme,
    speed: Float = 1f,
    stillTime: Float = 0f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    theme: KasciiTheme = LocalKasciiTheme.current,
    contentDescription: String? = null,
) {
    TODO("implemented by the renderer work")
}

/**
 * Plays an [AsciiPiece]. The grid is [AsciiPiece.meta]'s size, scaled to fit [modifier]. Pauses offscreen, honours
 * reduced motion by showing the piece's still frame.
 */
@Composable
public fun AsciiPieceView(
    piece: AsciiPiece,
    modifier: Modifier = Modifier,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    theme: KasciiTheme = LocalKasciiTheme.current,
    contentDescription: String? = null,
) {
    TODO("implemented by the renderer work")
}

/** Draws a static [grid] once. Use [renderer] = [AsciiRenderer.Text] for selectable text. */
@Composable
public fun AsciiGridView(
    grid: Grid,
    modifier: Modifier = Modifier,
    renderer: AsciiRenderer = AsciiRenderer.Cpu,
    theme: KasciiTheme = LocalKasciiTheme.current,
    contentDescription: String? = null,
) {
    TODO("implemented by the renderer work")
}

/**
 * Turns the element it modifies into ASCII: the content is drawn to a layer, sampled into tones (and colors when
 * the [colorMode] asks for them) and drawn as glyphs on top.
 */
public fun Modifier.ascii(
    charset: Charset = Charsets.Blocks,
    cell: Dp = 8.dp,
    colorMode: ColorMode = ColorMode.Theme,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    theme: KasciiTheme? = null,
): Modifier = TODO("implemented by the renderer work")

/** Rasterises [grid] to a bitmap with the bundled font, for PNG export or sharing. */
public fun Grid.toImageBitmap(theme: KasciiTheme = KasciiTheme.Dark, cellHeightPx: Int = 32): ImageBitmap =
    TODO("implemented by the renderer work")
