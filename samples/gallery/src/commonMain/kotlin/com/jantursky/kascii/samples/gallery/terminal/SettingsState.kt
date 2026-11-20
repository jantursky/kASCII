package com.jantursky.kascii.samples.gallery.terminal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.samples.gallery.GalleryState
import com.jantursky.kascii.samples.gallery.examples.BuiltInThemes

/** What the settings screen edits. Every setting changes something you can see in the app. */
class SettingsState {
    /** A toast follows each restart. */
    var notifications by mutableStateOf(true)
    var autoRefresh by mutableStateOf(false)
    var intervalSeconds by mutableFloatStateOf(5f)
    var problemsOnly by mutableStateOf(false)
    var region by mutableIntStateOf(0)
    var name by mutableStateOf("")

    /** The name as it shows in the header. */
    val host: String get() = hostLabel(name)

    fun shows(server: Server): Boolean =
        (!problemsOnly || server.health.needsAttention) && (region == 0 || server.region == Regions[region])

    companion object {
        /** The region filter: everything, then each region. */
        val Regions = listOf("all", "eu-west", "us-east", "ap-south")
    }
}

/** A short lowercase label of [raw] made of letters, digits and single dashes; "guest" when nothing is left. */
fun hostLabel(raw: String): String =
    raw
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .take(MAX_HOST)
        .trim('-')
        .ifEmpty { "guest" }

private const val MAX_HOST = 16

/** The motion choices of the settings screen. */
val MotionChoices: List<Pair<String, Motion>> =
    listOf("System" to Motion.System, "Full" to Motion.Full, "Reduced" to Motion.Reduced)

/** The position of the theme in use among the built-in themes. */
fun GalleryState.themeIndex(): Int = BuiltInThemes.indexOfFirst { it.second == theme }.coerceAtLeast(0)

/** Uses the built-in theme at [index]. Light and dark switch the gallery's own mode; the others override it. */
fun GalleryState.chooseTheme(index: Int) {
    when (val theme: KasciiTheme = BuiltInThemes[index].second) {
        KasciiTheme.Light -> useDark(false)
        KasciiTheme.Dark -> useDark(true)
        else -> themeOverride = theme
    }
}
