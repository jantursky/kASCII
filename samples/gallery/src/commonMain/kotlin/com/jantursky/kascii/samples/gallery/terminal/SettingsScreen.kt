package com.jantursky.kascii.samples.gallery.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.components.AsciiDivider
import com.jantursky.kascii.components.AsciiRadioGroup
import com.jantursky.kascii.components.AsciiSlider
import com.jantursky.kascii.components.AsciiTextField
import com.jantursky.kascii.components.AsciiToggle
import com.jantursky.kascii.samples.gallery.GalleryState
import com.jantursky.kascii.samples.gallery.examples.BuiltInThemes
import kotlin.math.roundToInt

private const val MAX_COLS = 40
private const val MAX_SECONDS = 30f

/** Toggles, a radio group, a slider and a text field for the app, then the theme and the motion. */
@Composable
internal fun SettingsScreen(
    state: TerminalState,
    gallery: GalleryState,
    cols: Int,
    modifier: Modifier = Modifier,
) {
    val settings = state.settings
    val width = minOf(cols, MAX_COLS)
    Column(modifier.verticalScroll(rememberScrollState()), Arrangement.spacedBy(8.dp)) {
        AsciiDivider(cols = width, label = "Behavior")
        AsciiToggle(settings.notifications, { settings.notifications = it }, "Notify after a restart")
        AsciiToggle(settings.autoRefresh, { settings.autoRefresh = it }, "Refresh automatically")
        AsciiSlider(
            value = settings.intervalSeconds,
            onValueChange = { settings.intervalSeconds = it.roundToInt().toFloat() },
            cols = width,
            valueRange = 1f..MAX_SECONDS,
            steps = MAX_SECONDS.toInt() - 2,
            label = "Refresh interval",
        )
        AppText("every ${settings.intervalSeconds.roundToInt()} s", muted = true)
        AsciiDivider(cols = width, label = "List")
        AsciiToggle(settings.problemsOnly, { settings.problemsOnly = it }, "Only servers that need attention")
        AsciiRadioGroup(SettingsState.Regions, settings.region) { settings.region = it }
        AsciiDivider(cols = width, label = "Profile")
        AsciiTextField(settings.name, { settings.name = it }, label = "Name", cols = width, placeholder = "Your name")
        AppText("shown as ${settings.host}", muted = true)
        AsciiDivider(cols = width, label = "Look")
        AsciiRadioGroup(BuiltInThemes.map { it.first }, gallery.themeIndex(), onSelect = gallery::chooseTheme)
        AsciiRadioGroup(
            MotionChoices.map { it.first },
            MotionChoices.indexOfFirst { it.second == gallery.motion },
            onSelect = { gallery.motion = MotionChoices[it].second },
        )
    }
}
