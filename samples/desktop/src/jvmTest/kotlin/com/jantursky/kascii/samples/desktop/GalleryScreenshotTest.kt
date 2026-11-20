package com.jantursky.kascii.samples.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.jantursky.kascii.samples.gallery.Catalog
import com.jantursky.kascii.samples.gallery.GalleryApp
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

    @Test fun overview() = shoot("overview")

    @Test fun overviewLight() = shoot("overview", dark = false)

    @Test fun pieces() = shoot("pieces")

    @Test fun fields() = shoot("fields", height = 1400)

    @Test fun renderers() = shoot("renderers")

    @Test fun modifier() = shoot("modifier", height = 1400)

    @Test fun theme() = shoot("theme", height = 1100)

    @Test fun pieceDetail() = shoot("donut")

    @Test fun pieceNarrow() = shoot("campfire", width = 390, height = 844, name = "campfire-narrow")

    @Test fun everyPage() = Catalog.forEach { shoot(it.id) }
}
