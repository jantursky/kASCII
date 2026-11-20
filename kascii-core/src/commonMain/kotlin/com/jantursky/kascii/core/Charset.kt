package com.jantursky.kascii.core

/**
 * Turns tone samples into glyphs. A charset says how many tone samples one cell takes
 * ([subX] by [subY]) and which glyph those samples produce.
 */
public sealed class Charset(public val name: String, public val subX: Int, public val subY: Int) {
    /**
     * Light-to-dark glyphs, one tone sample per cell. Tone 0 picks the first glyph, tone 1 the last.
     * Order the glyphs by how much ink they cover, or use [byCoverage] with a measured coverage.
     */
    public class Ramp(name: String, public val glyphs: String) : Charset(name, 1, 1) {
        init {
            require(glyphs.isNotEmpty()) { "A ramp needs at least one glyph" }
        }

        /** The glyph for a tone in 0..1. */
        public fun glyphFor(tone: Float): Char {
            val i = (tone.coerceIn(0f, 1f) * glyphs.length).toInt().coerceAtMost(glyphs.length - 1)
            return glyphs[i]
        }

        public companion object {
            /** Reorders [glyphs] from the least to the most ink using a measured [coverage] per glyph. */
            public fun byCoverage(name: String, glyphs: String, coverage: (Char) -> Float): Ramp =
                Ramp(name, glyphs.toList().sortedBy(coverage).joinToString(""))
        }
    }

    /** Several samples per cell form a bit mask. A sample above [threshold] sets its bit. */
    public class Mask internal constructor(
        name: String,
        subX: Int,
        subY: Int,
        private val glyphOf: (Int) -> Char,
        private val bitOf: (Int, Int) -> Int,
        public val threshold: Float = 0.5f,
    ) : Charset(name, subX, subY) {
        /** The glyph for the lit bits of a cell. [samples] is row major, [subX] by [subY]. */
        public fun glyphFor(samples: FloatArray): Char {
            var mask = 0
            for (y in 0 until subY) for (x in 0 until subX) {
                if (samples[y * subX + x] > threshold) mask = mask or bitOf(x, y)
            }
            return glyphOf(mask)
        }

        /** Same charset with another lit threshold. */
        public fun withThreshold(value: Float): Mask = Mask(name, subX, subY, glyphOf, bitOf, value)
    }

    /**
     * Shape-aware: a 3x3 Sobel pass picks `-`, `/`, `\` or `|` where the tone changes sharply and falls back
     * to [base] elsewhere.
     */
    public class Edge(public val base: Ramp, public val threshold: Float = 0.22f) : Charset("edge", 1, 1)
}

/** The built-in charsets. */
public object Charsets {
    public val Standard: Charset.Ramp = Charset.Ramp("standard", " .:-=+*#%@")
    public val Dense: Charset.Ramp = Charset.Ramp(
        "dense",
        " .'`^\",:;Il!i><~+_-?][}{1)(|/tfjrxnuvczXYUJCLQ0OZmwqpdbkhao*#MW&8%B@$",
    )
    public val Short: Charset.Ramp = Charset.Ramp("short", " .oO@")
    public val Blocks: Charset.Ramp = Charset.Ramp("blocks", " ░▒▓█")
    public val Binary: Charset.Ramp = Charset.Ramp("binary", " 01")
    public val Digits: Charset.Ramp = Charset.Ramp("digits", " 1234567890")
    public val BoxDrawing: Charset.Ramp = Charset.Ramp("box", " ·┄┈─┼╬▒█")
    public val Cp437: Charset.Ramp = Charset.Ramp("cp437", " ·∙░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬▀▄█")
    public val Petscii: Charset.Ramp = Charset.Ramp("petscii", " ·▏▎▍▌▋▊▉█▒▓")
    public val Katakana: Charset.Ramp = Charset.Ramp("katakana", " ・ノヲ力ヨ卜ホ")
    public val Dots: Charset.Ramp = Charset.Ramp("dots", " ·∙•⦁●")

    /** 2x2 sub-cell quadrant blocks. */
    public val Quadrants: Charset.Mask = Charset.Mask(
        "quadrants",
        2,
        2,
        { QUADRANTS[it] },
        { x, y -> 1 shl (y * 2 + x) },
    )

    /** 2x4 sub-cell Braille, the finest built-in resolution. */
    public val Braille: Charset.Mask = Charset.Mask(
        "braille",
        2,
        4,
        { (0x2800 + it).toChar() },
        { x, y -> BRAILLE_BITS[y][x] },
    )

    public val Edge: Charset.Edge = Charset.Edge(Standard)

    /** Every built-in charset, for pickers. */
    public val All: List<Charset> = listOf(
        Standard, Dense, Short, Blocks, Binary, Digits, BoxDrawing, Cp437, Petscii, Katakana, Dots,
        Quadrants, Braille, Edge,
    )

    /** A ramp from your own glyphs, light to dark. */
    public fun custom(glyphs: String): Charset.Ramp = Charset.Ramp("custom", glyphs)
}

private val BRAILLE_BITS = arrayOf(
    intArrayOf(0x01, 0x08),
    intArrayOf(0x02, 0x10),
    intArrayOf(0x04, 0x20),
    intArrayOf(0x40, 0x80),
)

// Index is the mask: bit 0 top-left, 1 top-right, 2 bottom-left, 3 bottom-right.
private val QUADRANTS = charArrayOf(
    ' ', '▘', '▝', '▀', '▖', '▌', '▞', '▛', '▗', '▚', '▐', '▜', '▄', '▙', '▟', '█',
)
