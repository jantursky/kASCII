package com.jantursky.kascii.core

/** How a cell gets its color. */
public sealed interface ColorMode {
    /** Every glyph in one fixed color. */
    public class Mono(public val color: Rgb) : ColorMode

    /** The theme ink, so the art follows light and dark. Stored as "no explicit color". */
    public data object Theme : ColorMode

    /** The color a [ColorField] gives for the cell. */
    public class Source(public val field: ColorField) : ColorMode

    /** Blends [stops] by tone, light end first. */
    public class Gradient(public val stops: List<Rgb>) : ColorMode {
        init {
            require(stops.size >= 2) { "A gradient needs at least two stops" }
        }
    }

    /** Blends from [dark] to [light] by tone. */
    public class Duotone(public val dark: Rgb, public val light: Rgb) : ColorMode

    /** Picks a theme palette color by tone. */
    public data object Palette : ColorMode
}

internal fun ColorMode.resolve(theme: KasciiTheme, tone: Float, x: Float, y: Float, t: Float): Rgb? = when (this) {
    is ColorMode.Mono -> color
    ColorMode.Theme -> null
    is ColorMode.Source -> field.color(x, y, t)
    is ColorMode.Gradient -> {
        val p = tone.coerceIn(0f, 1f) * (stops.size - 1)
        val i = p.toInt().coerceAtMost(stops.size - 2)
        stops[i].lerp(stops[i + 1], p - i)
    }
    is ColorMode.Duotone -> dark.lerp(light, tone)
    ColorMode.Palette -> theme.palette[(tone.coerceIn(0f, 1f) * theme.palette.size).toInt().coerceAtMost(theme.palette.size - 1)]
}
