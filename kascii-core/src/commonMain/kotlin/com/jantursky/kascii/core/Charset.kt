package com.jantursky.kascii.core

/**
 * Turns tone samples into glyphs. A charset says how many tone samples one cell takes
 * ([subX] by [subY]) and which glyph those samples produce.
 */
public sealed class Charset(
    /** Short unique name of the charset. */
    public val name: String,
    /** Tone samples per cell across. */
    public val subX: Int,
    /** Tone samples per cell down. */
    public val subY: Int,
) {
    /**
     * Light-to-dark glyphs, one tone sample per cell. Tone 0 picks the first glyph, tone 1 the last.
     * Order the glyphs by how much ink they cover, or use [byCoverage] with a measured coverage.
     */
    public class Ramp(
        name: String,
        /** The glyphs, lightest first. */
        public val glyphs: String,
    ) : Charset(name, 1, 1) {
        init {
            require(glyphs.isNotEmpty()) { "A ramp needs at least one glyph" }
        }

        /** The glyph for a tone in 0..1. */
        public fun glyphFor(tone: Float): Char {
            val i = (tone.coerceIn(0f, 1f) * glyphs.length).toInt().coerceAtMost(glyphs.length - 1)
            return glyphs[i]
        }

        /** The built-in values. */
        public companion object {
            /** Reorders [glyphs] from the least to the most ink using a measured [coverage] per glyph. */
            public fun byCoverage(
                name: String,
                glyphs: String,
                coverage: (Char) -> Float,
            ): Ramp = Ramp(name, glyphs.toList().sortedBy(coverage).joinToString(""))
        }
    }

    /** Several samples per cell form a bit mask. A sample above [threshold] sets its bit. */
    public class Mask internal constructor(
        name: String,
        subX: Int,
        subY: Int,
        private val glyphOf: (Int) -> Char,
        private val bitOf: (Int, Int) -> Int,
        /** Samples above this tone set their bit. */
        public val threshold: Float = 0.5f,
    ) : Charset(name, subX, subY) {
        /** The glyph for the lit bits of a cell. [samples] is row major, [subX] by [subY]. */
        public fun glyphFor(samples: FloatArray): Char {
            var mask = 0
            for (y in 0 until subY) {
                for (x in 0 until subX) {
                    if (samples[y * subX + x] > threshold) mask = mask or bitOf(x, y)
                }
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
    public class Edge(
        /** The ramp used where there is no edge. */
        public val base: Ramp,
        /** Gradient strength above which a cell becomes an edge glyph. */
        public val threshold: Float = 0.22f,
    ) : Charset("edge", 1, 1)
}

/** The built-in charsets. */
public object Charsets {
    /** The classic ten-step ramp, one sample per cell. */
    public val Standard: Charset.Ramp = Charset.Ramp("standard", " .:-=+*#%@")

    /** A 69-glyph ramp for smooth gradients and photos. */
    public val Dense: Charset.Ramp =
        Charset.Ramp(
            "dense",
            " .'`^\",:;Il!i><~+_-?][}{1)(|/tfjrxnuvczXYUJCLQ0OZmwqpdbkhao*#MW&8%B@$",
        )

    /** A chunky five-glyph ramp. */
    public val Short: Charset.Ramp = Charset.Ramp("short", " .oO@")

    /** Shade blocks, light to full. */
    public val Blocks: Charset.Ramp = Charset.Ramp("blocks", " ░▒▓█")

    /** Space, zero, one. */
    public val Binary: Charset.Ramp = Charset.Ramp("binary", " 01")

    /** Space and the ten digits. */
    public val Digits: Charset.Ramp = Charset.Ramp("digits", " 1234567890")

    /** Box-drawing and block glyphs for technical looks. */
    public val BoxDrawing: Charset.Ramp = Charset.Ramp("box", " ·┄┈─┼╬▒█")

    /** DOS code page 437 box and block glyphs. */
    public val Cp437: Charset.Ramp = Charset.Ramp("cp437", " ·∙░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬▀▄█")

    /** Block and bar glyphs in the style of Commodore PETSCII. */
    public val Petscii: Charset.Ramp = Charset.Ramp("petscii", " ·▏▎▍▌▋▊▉█▒▓")

    /** A sparse katakana ramp for digital rain. */
    public val Katakana: Charset.Ramp = Charset.Ramp("katakana", " ・ノヲ力ヨ卜ホ")

    /** Dots of growing size, for halftone looks. */
    public val Dots: Charset.Ramp = Charset.Ramp("dots", " ·∙•⦁●")

    /** 2x2 sub-cell quadrant blocks. */
    public val Quadrants: Charset.Mask =
        Charset.Mask(
            "quadrants",
            2,
            2,
            { QUADRANTS[it] },
            { x, y -> 1 shl (y * 2 + x) },
        )

    /** 2x4 sub-cell Braille, the finest built-in resolution. */
    public val Braille: Charset.Mask =
        Charset.Mask(
            "braille",
            2,
            4,
            { (0x2800 + it).toChar() },
            { x, y -> BRAILLE_BITS[y][x] },
        )

    /** Shape-aware charset that draws slashes and bars along sharp edges. */
    public val Edge: Charset.Edge = Charset.Edge(Standard)

    /** Every built-in charset, for pickers. */
    public val All: List<Charset> =
        listOf(
            Standard,
            Dense,
            Short,
            Blocks,
            Binary,
            Digits,
            BoxDrawing,
            Cp437,
            Petscii,
            Katakana,
            Dots,
            Quadrants,
            Braille,
            Edge,
        )

    /** A ramp from your own glyphs, light to dark. */
    public fun custom(glyphs: String): Charset.Ramp = Charset.Ramp("custom", glyphs)
}

private val BRAILLE_BITS =
    arrayOf(
        intArrayOf(0x01, 0x08),
        intArrayOf(0x02, 0x10),
        intArrayOf(0x04, 0x20),
        intArrayOf(0x40, 0x80),
    )

// Index is the mask: bit 0 top-left, 1 top-right, 2 bottom-left, 3 bottom-right.
private val QUADRANTS =
    charArrayOf(
        ' ',
        '▘',
        '▝',
        '▀',
        '▖',
        '▌',
        '▞',
        '▛',
        '▗',
        '▚',
        '▐',
        '▜',
        '▄',
        '▙',
        '▟',
        '█',
    )
