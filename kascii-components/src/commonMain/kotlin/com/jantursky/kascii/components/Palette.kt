package com.jantursky.kascii.components

import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.Rgb

/** The meaning a component attaches to a color. Every tone is read from the theme, never hard coded. */
public enum class AsciiTone {
    /** Plain ink. */
    Neutral,

    /** Informational, the theme accent. */
    Info,

    /** Success or a healthy state. */
    Success,

    /** Something that needs attention. */
    Warning,

    /** An error or a failed state. */
    Error,
}

/** Secondary text: the ink pulled towards the paper. */
internal fun KasciiTheme.dim(): Rgb = ink.lerp(paper, 0.35f)

internal fun KasciiTheme.good(): Rgb = palette[2 % palette.size]

internal fun KasciiTheme.warn(): Rgb = palette[3 % palette.size]

internal fun KasciiTheme.bad(): Rgb = palette[1 % palette.size]

/** Palette entry [index], wrapping around when the theme has fewer colors. */
internal fun KasciiTheme.pick(index: Int): Rgb = palette[index.mod(palette.size)]

/** Ink and paper swapped, used while a control is pressed. */
internal fun KasciiTheme.inverse(): KasciiTheme = KasciiTheme(paper, ink, accent, palette)

/** The color of [tone], or null for plain ink. */
internal fun AsciiTone.color(theme: KasciiTheme): Rgb? = when (this) {
    AsciiTone.Neutral -> null
    AsciiTone.Info -> theme.accent
    AsciiTone.Success -> theme.good()
    AsciiTone.Warning -> theme.warn()
    AsciiTone.Error -> theme.bad()
}

/** A one-glyph marker so a tone never relies on color alone. */
internal fun AsciiTone.marker(): Char = when (this) {
    AsciiTone.Neutral -> '*'
    AsciiTone.Info -> 'i'
    AsciiTone.Success -> '+'
    AsciiTone.Warning -> '!'
    AsciiTone.Error -> 'x'
}
