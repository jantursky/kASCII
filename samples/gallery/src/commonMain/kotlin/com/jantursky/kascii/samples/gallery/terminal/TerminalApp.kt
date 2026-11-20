package com.jantursky.kascii.samples.gallery.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.components.AsciiBadge
import com.jantursky.kascii.components.AsciiDialog
import com.jantursky.kascii.components.AsciiTabs
import com.jantursky.kascii.components.AsciiToast
import com.jantursky.kascii.compose.LocalKasciiMotion
import com.jantursky.kascii.compose.ProvideKasciiTheme
import com.jantursky.kascii.samples.gallery.GalleryState
import kotlinx.coroutines.delay

private val Padding = 16.dp
private const val MAX_COLS = 60
private const val DIALOG_COLS = 36
private const val TOAST_MILLIS = 4000L
private const val MILLIS = 1000

/**
 * A terminal-style master and detail app, built from the components and layout only: a server list, the detail of
 * one server and the settings, under tabs. Left and Right on a tab, and Alt with 1 to 3 anywhere, switch screens.
 */
@Composable
fun TerminalApp(
    gallery: GalleryState,
    modifier: Modifier = Modifier,
) {
    val state = remember { TerminalState() }
    val theme = gallery.theme
    val settings = state.settings
    LaunchedEffect(settings.autoRefresh, settings.intervalSeconds) {
        while (settings.autoRefresh) {
            delay((settings.intervalSeconds * MILLIS).toLong())
            state.refresh()
        }
    }
    CompositionLocalProvider(LocalKasciiMotion provides gallery.motion) {
        ProvideKasciiTheme(theme) {
            BoxWithConstraints(
                modifier.background(Color(theme.paper.argb)).onPreviewKeyEvent { it.switchScreen(state) },
            ) {
                val cols = fitCols((maxWidth - Padding * 2).value, MAX_COLS)
                Column(Modifier.fillMaxSize().padding(Padding), Arrangement.spacedBy(8.dp)) {
                    Header(state)
                    Box(Modifier.onPreviewKeyEvent { it.moveTab(state) }) {
                        AsciiTabs(
                            Screen.entries.map { it.label },
                            state.screen.ordinal,
                            { state.show(Screen.entries[it]) },
                        )
                    }
                    val screen = Modifier.weight(1f).fillMaxWidth()
                    when (state.screen) {
                        Screen.Servers -> ServersScreen(state, screen)
                        Screen.Detail -> DetailScreen(state, cols, screen)
                        Screen.Settings -> SettingsScreen(state, gallery, cols, screen)
                    }
                }
                state.toast?.let {
                    AsciiToast(
                        it.message,
                        Modifier.align(Alignment.BottomCenter).padding(Padding),
                        tone = it.tone,
                        onDismiss = state::dismissToast,
                        durationMillis = TOAST_MILLIS,
                    )
                }
                state.restarting?.let {
                    AsciiDialog(
                        title = "Restart ${it.id}?",
                        message = "Open connections will be dropped.",
                        onDismissRequest = state::dismissRestart,
                        confirmLabel = "Restart",
                        onConfirm = state::confirmRestart,
                        dismissLabel = "Cancel",
                        cols = minOf(cols, DIALOG_COLS),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(state: TerminalState) {
    FlowRow(
        Modifier.fillMaxWidth(),
        Arrangement.spacedBy(12.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        AppText("kascii@${state.settings.host}")
        AppText("${state.servers.size} servers, ${state.needAttention} need attention", muted = true)
        if (state.settings.autoRefresh) AsciiBadge("auto ${state.settings.intervalSeconds.toInt()}s")
    }
}

/** Alt and a digit open the screen at that place; true when the key was one. */
private fun KeyEvent.switchScreen(state: TerminalState): Boolean {
    val index = DIGITS.indexOf(key)
    if (type != KeyEventType.KeyDown || !isAltPressed || index !in Screen.entries.indices) return false
    state.show(Screen.entries[index])
    return true
}

/** Left and Right move to the previous and next tab; true when the key was one. */
private fun KeyEvent.moveTab(state: TerminalState): Boolean {
    if (type != KeyEventType.KeyDown) return false
    when (key) {
        Key.DirectionLeft -> state.step(-1)
        Key.DirectionRight -> state.step(1)
        else -> return false
    }
    return true
}

private val DIGITS = listOf(Key.One, Key.Two, Key.Three)
