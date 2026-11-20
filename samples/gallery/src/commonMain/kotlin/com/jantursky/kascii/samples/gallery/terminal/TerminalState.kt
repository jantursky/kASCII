package com.jantursky.kascii.samples.gallery.terminal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jantursky.kascii.components.AsciiTone

/** The screens of the app, in tab order. */
enum class Screen(
    val label: String,
) {
    Servers("Servers"),
    Detail("Detail"),
    Settings("Settings"),
}

/** A short message after an action. */
data class Toast(
    val message: String,
    val tone: AsciiTone,
)

/** Everything the app remembers: the screen, the fleet, the selection and what waits for a confirmation. */
class TerminalState(
    servers: List<Server> = Fleet.servers,
) {
    var screen by mutableStateOf(Screen.Servers)
        private set
    var servers by mutableStateOf(servers)
        private set
    var selectedId by mutableStateOf<String?>(null)
        private set

    /** The server a restart was asked for and not yet confirmed or dismissed. */
    var restartId by mutableStateOf<String?>(null)
        private set
    var toast by mutableStateOf<Toast?>(null)
        private set
    val settings = SettingsState()
    private var readings by mutableIntStateOf(0)

    /** The servers the settings let through. */
    val visible: List<Server> get() = servers.filter(settings::shows)
    val selected: Server? get() = servers.firstOrNull { it.id == selectedId }
    val restarting: Server? get() = servers.firstOrNull { it.id == restartId }
    val needAttention: Int get() = servers.count { it.health.needsAttention }

    fun show(screen: Screen) {
        this.screen = screen
    }

    /** Moves to the tab [delta] places on, stopping at the first and last. */
    fun step(delta: Int) = show(Screen.entries[(screen.ordinal + delta).coerceIn(0, Screen.entries.lastIndex)])

    /** Opens the detail of a server. */
    fun open(id: String) {
        selectedId = id
        show(Screen.Detail)
    }

    fun askRestart() {
        restartId = selected?.id
    }

    fun dismissRestart() {
        restartId = null
    }

    fun confirmRestart() {
        val id = restartId ?: return
        servers = servers.map { if (it.id == id) it.restarted() else it }
        restartId = null
        if (settings.notifications) toast = Toast("$id restarted", AsciiTone.Success)
    }

    fun dismissToast() {
        toast = null
    }

    /** Adds the next load reading to every server. */
    fun refresh() {
        val step = HISTORY + readings++
        servers = servers.map { it.sampled(readingAt(it.seed, step)) }
    }
}
