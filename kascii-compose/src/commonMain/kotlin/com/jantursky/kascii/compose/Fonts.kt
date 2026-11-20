package com.jantursky.kascii.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import com.jantursky.kascii.compose.internal.FontRegistry
import com.jantursky.kascii.compose.platform.rememberBundledFont

/**
 * The bundled monospace font family (JetBrains Mono, with Braille and katakana from a Unifont subset). Every glyph
 * is one cell wide and the programming ligatures are removed, so grids keep their columns. On the web the font
 * loads asynchronously and this is [FontFamily.Monospace] until it arrives.
 */
@Composable
public fun kasciiFontFamily(): FontFamily = rememberKasciiFont() ?: FontFamily.Monospace

/** The bundled family, or null while it is still loading. */
@Composable
internal fun rememberKasciiFont(): FontFamily? {
    val font = rememberBundledFont()
    val family = remember(font) { font?.let { FontFamily(it) } }
    val resolver = LocalFontFamilyResolver.current
    if (family != null) SideEffect { FontRegistry.register(family, resolver) }
    return family
}
