package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme

/**
 * [text] as Morse code, keyed out at [symbolsPerSecond] dots and dashes a second. A screen reader gets the plain
 * text. Under reduced motion the whole code is shown. Characters without a Morse code are skipped.
 */
@Composable
public fun AsciiMorse(
    text: String,
    modifier: Modifier = Modifier,
    symbolsPerSecond: Float = 6f,
) {
    AnimatedText("${LocalAsciiStrings.current.morseCode}: $text", TEXT_FPS, modifier) { t, reduced, theme ->
        renderMorse(text, t, symbolsPerSecond, reduced, theme)
    }
}

/** Letters in a word are separated by a space, words by ` / `. */
internal fun morseOf(text: String): String =
    text
        .uppercase()
        .split(' ')
        .map { word -> word.mapNotNull { MORSE[it] }.joinToString(" ") }
        .filter { it.isNotEmpty() }
        .joinToString(" / ")

internal fun renderMorse(
    text: String,
    t: Float,
    symbolsPerSecond: Float,
    reduced: Boolean,
    theme: KasciiTheme,
): Grid {
    val code = morseOf(text)
    val shown = if (reduced) code.length else (t * symbolsPerSecond).toInt()
    val grid = Grid(code.length, 1)
    grid.write(0, 0, code.take(shown), theme.accent)
    val pending = code.drop(shown).map { if (it == ' ' || it == '/') it else '·' }.joinToString("")
    grid.write(shown, 0, pending, theme.dim())
    return grid
}

private val MORSE: Map<Char, String> =
    mapOf(
        'A' to ".-",
        'B' to "-...",
        'C' to "-.-.",
        'D' to "-..",
        'E' to ".",
        'F' to "..-.",
        'G' to "--.",
        'H' to "....",
        'I' to "..",
        'J' to ".---",
        'K' to "-.-",
        'L' to ".-..",
        'M' to "--",
        'N' to "-.",
        'O' to "---",
        'P' to ".--.",
        'Q' to "--.-",
        'R' to ".-.",
        'S' to "...",
        'T' to "-",
        'U' to "..-",
        'V' to "...-",
        'W' to ".--",
        'X' to "-..-",
        'Y' to "-.--",
        'Z' to "--..",
        '0' to "-----",
        '1' to ".----",
        '2' to "..---",
        '3' to "...--",
        '4' to "....-",
        '5' to ".....",
        '6' to "-....",
        '7' to "--...",
        '8' to "---..",
        '9' to "----.",
        '.' to ".-.-.-",
        ',' to "--..--",
        '?' to "..--..",
        '!' to "-.-.--",
        '-' to "-....-",
        '/' to "-..-.",
    )
