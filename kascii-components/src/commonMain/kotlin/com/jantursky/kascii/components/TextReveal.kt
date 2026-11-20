package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme

/** Types [text] out at [charsPerSecond] behind a blinking cursor. Under reduced motion the text is complete. */
@Composable
public fun AsciiTypewriter(
    text: String,
    modifier: Modifier = Modifier,
    charsPerSecond: Float = 14f,
) {
    AnimatedText(text, TEXT_FPS, modifier) { t, reduced, _ -> renderTypewriter(text, t, charsPerSecond, reduced) }
}

/** Decodes [text] from random glyphs, left to right, over [durationSeconds]. The glyphs depend on [seed]. */
@Composable
public fun AsciiScrambleText(
    text: String,
    modifier: Modifier = Modifier,
    durationSeconds: Float = 1.5f,
    seed: Long = 0L,
) {
    AnimatedText(text, TEXT_FPS, modifier) { t, reduced, theme ->
        renderScramble(text, t, durationSeconds, seed, reduced, theme)
    }
}

/** Fades [text] in as scattered specks that thicken, over [durationSeconds]. The order depends on [seed]. */
@Composable
public fun AsciiDissolveText(
    text: String,
    modifier: Modifier = Modifier,
    durationSeconds: Float = 1.5f,
    seed: Long = 0L,
) {
    AnimatedText(text, TEXT_FPS, modifier) { t, reduced, _ -> renderDissolve(text, t, durationSeconds, seed, reduced) }
}

/**
 * A split-flap display: every character flips through the alphabet until it lands, one after another. Letters
 * are shown upper case, like the real boards. The start positions depend on [seed].
 */
@Composable
public fun AsciiSplitFlap(
    text: String,
    modifier: Modifier = Modifier,
    seed: Long = 0L,
) {
    AnimatedText(text, FLAP_FPS, modifier) { t, reduced, _ -> renderSplitFlap(text, t, seed, reduced) }
}

internal fun renderTypewriter(text: String, t: Float, charsPerSecond: Float, reduced: Boolean): Grid {
    val grid = Grid(text.length + 1, 1)
    if (reduced) return grid.also { it.writeText(text) }
    val typed = (t * charsPerSecond).toInt().coerceAtMost(text.length)
    val cursorOn = typed < text.length || (t * 2).toInt() % 2 == 0
    grid.writeText(text.take(typed) + if (cursorOn) "▌" else "")
    return grid
}

internal fun renderScramble(
    text: String,
    t: Float,
    durationSeconds: Float,
    seed: Long,
    reduced: Boolean,
    theme: KasciiTheme,
): Grid {
    val grid = Grid(text.length, 1)
    val settled = if (reduced) text.length else (t / durationSeconds * text.length).toInt()
    val frame = (t * TEXT_FPS).toInt()
    text.forEachIndexed { i, c ->
        val glyph = if (i < settled || c == ' ') c else SCRAMBLE[frameRng(seed, i, frame).int(SCRAMBLE.length)]
        grid.put(i, 0, glyph, if (i < settled) null else theme.dim())
    }
    return grid
}

internal fun renderDissolve(text: String, t: Float, durationSeconds: Float, seed: Long, reduced: Boolean): Grid {
    val grid = Grid(text.length, 1)
    val progress = if (reduced) 1f else (t / durationSeconds).coerceIn(0f, 1f)
    text.forEachIndexed { i, c ->
        val start = frameRng(seed, i, 0).float() * DISSOLVE_SPREAD
        val stage = ((progress - start) / (1f - DISSOLVE_SPREAD) * (SPECKS.length + 1)).toInt()
        val glyph = if (c == ' ') ' ' else if (stage > SPECKS.lastIndex) c else SPECKS[stage.coerceAtLeast(0)]
        grid.put(i, 0, glyph)
    }
    return grid
}

internal fun renderSplitFlap(text: String, t: Float, seed: Long, reduced: Boolean): Grid {
    val grid = Grid(text.length, 1)
    text.forEachIndexed { i, c ->
        val target = FLAPS.indexOf(c.uppercaseChar())
        grid.put(i, 0, if (reduced || target < 0) c.uppercaseChar() else flapAt(i, target, t, seed))
    }
    return grid
}

/** The flap showing at [t]: it starts at a seeded letter, flips past a full turn, then rests on [target]. */
private fun flapAt(index: Int, target: Int, t: Float, seed: Long): Char {
    val start = frameRng(seed, index, 0).int(FLAPS.length)
    val flips = (target - start).mod(FLAPS.length) + FLAPS.length
    val done = ((t - index * FLAP_STAGGER) * FLIPS_PER_SECOND).toInt().coerceIn(0, flips)
    return FLAPS[(start + done) % FLAPS.length]
}

private const val SCRAMBLE = "#%&@\$*+=?/\\|<>01"
private const val SPECKS = " .:*"
private const val FLAPS = " ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
private const val DISSOLVE_SPREAD = 0.6f
private const val FLAP_STAGGER = 0.08f
private const val FLIPS_PER_SECOND = 48
private const val FLAP_FPS = 48
internal const val TEXT_FPS = 24
