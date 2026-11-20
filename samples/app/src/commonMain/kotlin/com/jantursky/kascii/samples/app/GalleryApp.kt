package com.jantursky.kascii.samples.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.jantursky.kascii.samples.gallery.terminal.TerminalAppScreen
import com.jantursky.kascii.samples.gallery.GalleryApp as Gallery

/**
 * The app for a phone or tablet: the terminal app, or the gallery page [entry] when one is named.
 *
 * @param entry The id of the gallery page to open, or null for the terminal app.
 * @param dark Starts in the dark theme when true, the light one when false and the system's when null.
 * @param onDarkChange Called with the theme in use, so the host can match its system bars.
 */
@Composable
fun GalleryApp(
    entry: String? = null,
    dark: Boolean? = null,
    onDarkChange: (Boolean) -> Unit = {},
) {
    val initialDark = dark ?: isSystemInDarkTheme()
    LaunchedEffect(initialDark) { onDarkChange(initialDark) }
    if (entry == null) {
        TerminalAppScreen(initialDark, onDarkChange)
    } else {
        Gallery(Modifier, entry, initialDark, onDarkModeChange = onDarkChange)
    }
}
