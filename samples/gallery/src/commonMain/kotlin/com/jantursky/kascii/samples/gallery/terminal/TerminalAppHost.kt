package com.jantursky.kascii.samples.gallery.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.samples.gallery.Gallery
import com.jantursky.kascii.samples.gallery.GalleryState
import com.jantursky.kascii.samples.gallery.GalleryTheme

private val PageHeight = 680.dp

/** The app on its own, filling the window below the system bars: what the phone apps show. */
@Composable
fun TerminalAppScreen(
    initialDark: Boolean = isSystemInDarkTheme(),
    onDarkChange: (Boolean) -> Unit = {},
) {
    val gallery = remember { GalleryState(initialDark, Motion.System) }
    LaunchedEffect(gallery.dark) { onDarkChange(gallery.dark) }
    GalleryTheme(gallery.dark) {
        TerminalApp(
            gallery,
            Modifier.fillMaxSize().background(Gallery.colors.void).windowInsetsPadding(WindowInsets.safeDrawing),
        )
    }
}

/** The app as a page of the gallery: framed in a fixed height, or filling an embed. */
@Composable
fun TerminalAppPage(gallery: GalleryState) {
    if (gallery.embed) {
        TerminalApp(gallery, Modifier.fillMaxSize())
        return
    }
    val shape = RoundedCornerShape(8.dp)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TerminalApp(
            gallery,
            Modifier
                .fillMaxWidth()
                .height(PageHeight)
                .clip(shape)
                .border(1.dp, Gallery.colors.border, shape),
        )
        BasicText(
            "Tab moves between controls and Space or Enter presses. Left and Right on a tab, or Alt with 1 to 3, " +
                "switch screens. Escape closes the dialog.",
            style = Gallery.type.caption.copy(color = Gallery.colors.muted),
        )
    }
}
