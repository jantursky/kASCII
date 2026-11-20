package com.jantursky.kascii.samples.desktop

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.jantursky.kascii.samples.gallery.Catalog
import com.jantursky.kascii.samples.gallery.GalleryApp
import com.jantursky.kascii.samples.gallery.Group
import com.jantursky.kascii.samples.gallery.terminal.TerminalAppScreen
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class GalleryScreenshotTest {
    private fun shoot(
        page: String,
        dark: Boolean = true,
        width: Int = 1280,
        height: Int = 900,
        name: String = "$page${if (dark) "" else "-light"}",
    ) = runDesktopComposeUiTest(width, height) {
        // Pieces ask for a frame forever, so the window is never idle: step the clock instead.
        mainClock.autoAdvance = false
        setContent { GalleryApp(initialPage = page, initialDark = dark) }
        mainClock.advanceTimeBy(800)
        onRoot().captureRoboImage("src/jvmTest/screenshots/$name.png")
    }

    /** The terminal app as the phone apps show it, after [steps] have been done in it. */
    private fun shootApp(
        name: String,
        width: Int = 390,
        height: Int = 844,
        dark: Boolean = true,
        dialog: Boolean = false,
        steps: ComposeUiTest.() -> Unit = {},
    ) = runDesktopComposeUiTest(width, height) {
        mainClock.autoAdvance = false
        setContent { TerminalAppScreen(initialDark = dark) }
        mainClock.advanceTimeBy(300)
        steps()
        mainClock.advanceTimeBy(300)
        // A dialog is a root of its own, drawn on a transparent window over the app.
        val roots = onAllNodes(isRoot())
        (if (dialog) roots.onLast() else roots.onFirst()).captureRoboImage("src/jvmTest/screenshots/$name.png")
    }

    private fun ComposeUiTest.press(
        description: String,
        index: Int = 0,
    ) {
        onAllNodesWithContentDescription(description)[index].performClick()
        mainClock.advanceTimeBy(100)
    }

    private fun ComposeUiTest.open(server: String) {
        onNodeWithText(server).performClick()
        mainClock.advanceTimeBy(100)
    }

    @Test fun overview() = shoot("overview")

    @Test fun overviewLight() = shoot("overview", dark = false)

    @Test fun pieces() = shoot("pieces")

    @Test fun fields() = shoot("fields", height = 1400)

    @Test fun renderers() = shoot("renderers")

    @Test fun modifier() = shoot("modifier", height = 1400)

    @Test fun theme() = shoot("theme", height = 1100)

    @Test fun pieceDetail() = shoot("donut")

    @Test fun pieceNarrow() = shoot("campfire", width = 390, height = 844, name = "campfire-narrow")

    @Test fun forms() = shoot("forms", height = 1000)

    @Test fun formsNarrow() = shoot("forms", width = 390, height = 1400, name = "forms-narrow")

    @Test fun surfaces() = shoot("surfaces", height = 1000)

    @Test fun navigation() = shoot("navigation", height = 1400)

    @Test fun terminal() = shoot("terminal", height = 1300)

    @Test fun progress() = shoot("progress", height = 900)

    @Test fun charts() = shoot("charts", height = 1000)

    @Test fun chartsNarrow() = shoot("charts", width = 390, height = 2200, name = "charts-narrow")

    @Test fun meters() = shoot("meters", height = 1000)

    @Test fun textEffects() = shoot("text-effects", height = 1100)

    @Test fun terminalAppPage() = shoot("terminal-app", height = 1000)

    @Test fun terminalAppServers() = shootApp("terminal-app-servers")

    @Test fun terminalAppServersLight() = shootApp("terminal-app-servers-light", dark = false)

    @Test fun terminalAppDetail() = shootApp("terminal-app-detail") { open("web-2") }

    @Test fun terminalAppDialog() =
        shootApp("terminal-app-dialog", dialog = true) {
            open("web-2")
            press("Restart")
        }

    @Test fun terminalAppToast() =
        shootApp("terminal-app-toast") {
            open("web-2")
            press("Restart")
            press("Restart", index = 1)
        }

    @Test fun terminalAppSettings() = shootApp("terminal-app-settings", height = 1500) { press("Settings") }

    @Test fun terminalAppPhosphor() =
        shootApp("terminal-app-phosphor", height = 1500) {
            press("Settings")
            press("Phosphor")
        }

    @Test fun everyPiece() = Catalog.filter { it.group == Group.Pieces }.forEach { shoot(it.id) }
}
