package com.jantursky.kascii.samples.gallery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.Charset
import com.jantursky.kascii.core.Charsets
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.Motion

/** The settings every page shares. */
class GalleryState(
    dark: Boolean,
    motion: Motion = Motion.System,
    val embed: Boolean = false,
) {
    var dark by mutableStateOf(dark)

    /** A theme picked on the theme page; null follows [dark]. */
    var themeOverride by mutableStateOf<KasciiTheme?>(null)
    var speed by mutableFloatStateOf(1f)
    var seed by mutableIntStateOf(0)
    var renderer by mutableStateOf(AsciiRenderer.Auto)
    var motion by mutableStateOf(motion)
    var charset by mutableStateOf<Charset>(Charsets.Standard)
    var colors by mutableStateOf(ColorChoice.Theme)

    /** Switches between the built-in light and dark themes, dropping a theme picked on the theme page. */
    fun useDark(value: Boolean) {
        dark = value
        themeOverride = null
    }

    /** The theme on the stage: the picked one, or the built-in light or dark theme. */
    val theme: KasciiTheme
        get() = themeOverride ?: if (dark) KasciiTheme.Dark else KasciiTheme.Light
}
