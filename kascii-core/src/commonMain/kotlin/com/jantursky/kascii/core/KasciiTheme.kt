package com.jantursky.kascii.core

/**
 * Colors the engine reads. [ink] draws glyphs, [paper] sits behind them, [accent] and [palette] feed the color
 * modes. The Compose module can build one from Material colors.
 */
public class KasciiTheme(
    /** The color of glyphs. */
    public val ink: Rgb,
    /** The color behind glyphs. */
    public val paper: Rgb,
    /** An accent color for highlights. */
    public val accent: Rgb = ink,
    /** Colors for the palette color mode. Never empty. */
    public val palette: List<Rgb> = listOf(ink),
) {
    init {
        require(palette.isNotEmpty()) { "A theme needs at least one palette color" }
    }

    /** WCAG contrast ratio between [ink] and [paper], 1..21. */
    public fun contrastRatio(): Float {
        val a = luminance(ink)
        val b = luminance(paper)
        val hi = maxOf(a, b)
        val lo = minOf(a, b)
        return (hi + 0.05f) / (lo + 0.05f)
    }

    /** The built-in values. */
    public companion object {
        /** Light page, near black ink. Derived from the Kascii design system. */
        public val Light: KasciiTheme =
            KasciiTheme(
                ink = Rgb.fromHex("#1d293d"),
                paper = Rgb.fromHex("#f4f6fb"),
                accent = Rgb.fromHex("#7f52ff"),
                palette = listOf("#7f52ff", "#e44857", "#00785a", "#9a6a00").map { Rgb.fromHex(it) },
            )

        /** Deep-ocean dark page, the native look. */
        public val Dark: KasciiTheme =
            KasciiTheme(
                ink = Rgb.fromHex("#cad5e2"),
                paper = Rgb.fromHex("#06051d"),
                accent = Rgb.fromHex("#7f52ff"),
                palette = listOf("#7f52ff", "#e44857", "#00bc7d", "#f0b100").map { Rgb.fromHex(it) },
            )

        /** Green phosphor on near black. */
        public val Phosphor: KasciiTheme = KasciiTheme(Rgb.fromHex("#33ff66"), Rgb.fromHex("#020a04"))

        /** Amber phosphor on near black. */
        public val Amber: KasciiTheme = KasciiTheme(Rgb.fromHex("#ffb000"), Rgb.fromHex("#0c0700"))
    }
}

private fun luminance(c: Rgb): Float {
    fun lin(v: Int): Float {
        val s = v / 255f
        return if (s <= 0.03928f) s / 12.92f else kotlin.math.exp(2.4f * kotlin.math.ln((s + 0.055f) / 1.055f))
    }
    return 0.2126f * lin(c.red) + 0.7152f * lin(c.green) + 0.0722f * lin(c.blue)
}
