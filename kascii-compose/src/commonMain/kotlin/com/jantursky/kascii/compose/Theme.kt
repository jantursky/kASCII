package com.jantursky.kascii.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.Rgb

/**
 * The theme every kASCII composable reads. Defaults to [KasciiTheme.Dark], the native look; provide
 * [systemKasciiTheme] through [ProvideKasciiTheme] to follow the system's dark or light setting.
 */
public val LocalKasciiTheme: ProvidableCompositionLocal<KasciiTheme> = compositionLocalOf { KasciiTheme.Dark }

/** Provides [theme] to everything inside [content]. */
@Composable
public fun ProvideKasciiTheme(
    theme: KasciiTheme,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalKasciiTheme provides theme, content = content)
}

/** The built-in dark or light theme following the system setting. */
@Composable
public fun systemKasciiTheme(): KasciiTheme = if (isSystemInDarkTheme()) KasciiTheme.Dark else KasciiTheme.Light

/** Builds a theme from Compose colors, for example Material 3's `onSurface` and `surface`. */
public fun kasciiThemeOf(
    ink: Color,
    paper: Color,
    accent: Color = ink,
    palette: List<Color> = listOf(ink),
): KasciiTheme = KasciiTheme(ink.toRgb(), paper.toRgb(), accent.toRgb(), palette.map { it.toRgb() })

internal fun Color.toRgb(): Rgb = Rgb(channel(alpha), channel(red), channel(green), channel(blue))

internal fun Rgb.toColor(): Color = Color(argb)

private fun channel(value: Float): Int = (value * 255f + 0.5f).toInt()
