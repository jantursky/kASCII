package com.jantursky.kascii.samples.gallery.terminal

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.samples.gallery.Gallery

/** A line of text in the ink of the theme, or dimmed when [muted]. */
@Composable
internal fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
) {
    val ink = Color(LocalKasciiTheme.current.ink.argb)
    BasicText(text, modifier, style = Gallery.type.caption.copy(color = if (muted) ink.copy(alpha = MUTED) else ink))
}

private const val MUTED = 0.65f
