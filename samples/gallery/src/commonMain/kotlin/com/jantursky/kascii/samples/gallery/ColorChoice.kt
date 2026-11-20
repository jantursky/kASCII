package com.jantursky.kascii.samples.gallery

import com.jantursky.kascii.core.ColorField
import com.jantursky.kascii.core.ColorMode
import com.jantursky.kascii.core.Rgb

private val Violet = Rgb.fromHex("#7f52ff")
private val Red = Rgb.fromHex("#e44857")
private val Amber = Rgb.fromHex("#f0b100")
private val Green = Rgb.fromHex("#00bc7d")

/** Every [ColorMode] of the library with the arguments the gallery shows it with. */
enum class ColorChoice(
    val label: String,
    val mode: ColorMode,
) {
    Theme("Theme", ColorMode.Theme),
    Mono("Mono", ColorMode.Mono(Green)),
    Gradient("Gradient", ColorMode.Gradient(listOf(Violet, Red, Amber))),
    Duotone("Duotone", ColorMode.Duotone(Violet, Amber)),
    Palette("Palette", ColorMode.Palette),
    Source("Source", ColorMode.Source(ColorField { x, y, t -> Violet.lerp(Red, ((x + y) / 2f + t * 0.05f) % 1f) })),
}
