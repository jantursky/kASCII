package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.compose.rememberReducedMotion
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.Rng

/**
 * Shows the grid a text effect draws at the current time. Screen readers get [description], the finished text,
 * never the scrambled frames. [render] is told when motion is reduced so it can settle on that final text.
 */
@Composable
internal fun AnimatedText(
    description: String,
    fps: Int,
    modifier: Modifier,
    render: (t: Float, reduced: Boolean, theme: KasciiTheme) -> Grid,
) {
    val reduced = rememberReducedMotion()
    val time = rememberClock(fps, reduced).floatValue
    GridSurface(render(time, reduced, LocalKasciiTheme.current), modifier, description)
}

/** A random source that depends only on the seed, the character index and the frame, so frames can be replayed. */
internal fun frameRng(
    seed: Long,
    index: Int,
    frame: Int,
): Rng = Rng(seed * 1_000_003L + index * 7_919L + frame * 104_729L)

internal fun Grid.writeText(
    text: String,
    color: Rgb? = null,
) = write(0, 0, text, color)
