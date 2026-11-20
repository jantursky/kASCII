package com.jantursky.kascii.samples.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.jantursky.kascii.samples.gallery.GalleryApp as Gallery

/**
 * The app for a phone or tablet: the whole gallery, opened on the overview or on the page [entry]. The terminal app
 * is one of its pages.
 *
 * @param entry The id of the gallery page to open, or null for the overview.
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
    Gallery(Modifier, entry, initialDark, onDarkModeChange = onDarkChange)
}
