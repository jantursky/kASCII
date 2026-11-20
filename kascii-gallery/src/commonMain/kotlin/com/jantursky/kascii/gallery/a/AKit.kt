package com.jantursky.kascii.gallery

import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.Rng
import kotlin.math.floor

/** Shared colors and small helpers of the pieces in this directory. Mid-tones, so they read on both themes. */
internal object AKit {
    const val TAU: Float = 6.2831855f

    val Violet: Rgb = Rgb.fromHex("#7f52ff")
    val Red: Rgb = Rgb.fromHex("#e44857")
    val Green: Rgb = Rgb.fromHex("#00bc7d")
    val Amber: Rgb = Rgb.fromHex("#f0b100")
    val Blue: Rgb = Rgb.fromHex("#63b3ed")

    fun frac(v: Float): Float = v - floor(v)

    /** A generator for one purpose ([salt]) of a piece, so adding a draw call never shifts another one. */
    fun rng(
        seed: Int,
        salt: Int,
    ): Rng = Rng(seed * 1_000_003L + salt)

    /** The glyph of [ramp] (light to dark) for a tone in 0..1. */
    fun glyph(
        ramp: String,
        tone: Float,
    ): Char = ramp[(tone.coerceIn(0f, 1f) * ramp.length).toInt().coerceAtMost(ramp.length - 1)]
}
