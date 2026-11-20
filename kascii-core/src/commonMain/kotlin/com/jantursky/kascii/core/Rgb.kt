package com.jantursky.kascii.core

import kotlin.jvm.JvmInline

/** An opaque or translucent color packed as ARGB, so the engine needs no UI toolkit. */
@JvmInline
public value class Rgb(public val argb: Int) {
    public val red: Int get() = (argb shr 16) and 0xFF
    public val green: Int get() = (argb shr 8) and 0xFF
    public val blue: Int get() = argb and 0xFF
    public val alpha: Int get() = (argb ushr 24) and 0xFF

    /** Linear blend towards [other] by [t] in 0..1, channel by channel. */
    public fun lerp(other: Rgb, t: Float): Rgb {
        val k = t.coerceIn(0f, 1f)
        fun mix(a: Int, b: Int) = (a + (b - a) * k).toInt().coerceIn(0, 255)
        return Rgb(mix(alpha, other.alpha), mix(red, other.red), mix(green, other.green), mix(blue, other.blue))
    }

    /** `#rrggbb`, lower case. */
    public fun toHex(): String = "#" + hex2(red) + hex2(green) + hex2(blue)

    public companion object {
        /** Black, the default "not set" value in a [Grid]. */
        public val Black: Rgb = Rgb(0xFF000000.toInt())
        public val White: Rgb = Rgb(0xFFFFFFFF.toInt())

        /** Builds an opaque color from 0..255 channels. */
        public operator fun invoke(red: Int, green: Int, blue: Int): Rgb = Rgb(255, red, green, blue)

        /** Builds a color from 0..255 channels including alpha. */
        public operator fun invoke(alpha: Int, red: Int, green: Int, blue: Int): Rgb =
            Rgb((alpha shl 24) or (red shl 16) or (green shl 8) or blue)

        /** Parses `#rgb`, `#rrggbb` or `#aarrggbb`. */
        public fun fromHex(hex: String): Rgb {
            val h = hex.removePrefix("#")
            return when (h.length) {
                3 -> Rgb(h.map { "$it$it".toInt(16) }.let { Rgb(it[0], it[1], it[2]) }.argb)
                6 -> Rgb(0xFF000000.toInt() or h.toLong(16).toInt())
                8 -> Rgb(h.toLong(16).toInt())
                else -> throw IllegalArgumentException("Not a color: $hex")
            }
        }
    }
}

private fun hex2(v: Int): String = v.toString(16).padStart(2, '0')
