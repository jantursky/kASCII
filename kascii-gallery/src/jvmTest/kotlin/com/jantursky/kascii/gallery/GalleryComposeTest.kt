package com.jantursky.kascii.gallery

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.compose.AsciiPieceView
import com.jantursky.kascii.core.Motion
import kotlin.test.Test
import kotlin.test.assertTrue

/** Composes every catalog piece through the real view, so a piece that cannot be drawn fails here. */
@OptIn(ExperimentalTestApi::class)
class GalleryComposeTest {
    private val entries = galleryPiecesA + galleryPiecesB + galleryPiecesC + galleryPiecesD

    @Test
    fun catalogIsNotEmpty() {
        assertTrue(entries.size >= 26)
    }

    @Test
    fun everyPieceDrawsUnderReducedMotion() {
        for (entry in entries) {
            runDesktopComposeUiTest(640, 480) {
                setContent {
                    AsciiPieceView(
                        piece = entry.create(0),
                        modifier = Modifier.size(640.dp, 480.dp),
                        motion = Motion.Reduced,
                        contentDescription = entry.title,
                    )
                }
                onNodeWithContentDescription(entry.title).assertIsDisplayed()
            }
        }
    }
}
