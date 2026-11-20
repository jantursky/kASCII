package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A digital clock showing the time it is given, in 24 hour format: `HH:MM`, or `HH:MM:SS` when [second] is set.
 * The caller supplies the time, so the component has no platform clock and stays easy to test.
 */
@Composable
public fun AsciiClock(
    hour: Int,
    minute: Int,
    modifier: Modifier = Modifier,
    second: Int? = null,
    font: AsciiBigFont = AsciiBigFont.Compact,
) {
    val text = clockText(hour, minute, second)
    GridSurface(renderBigText(text, font), modifier, text)
}

internal fun clockText(hour: Int, minute: Int, second: Int?): String {
    require(hour in 0..23 && minute in 0..59 && (second ?: 0) in 0..59) { "Not a time of day" }
    val parts = listOfNotNull(hour, minute, second).map { it.toString().padStart(2, '0') }
    return parts.joinToString(":")
}
