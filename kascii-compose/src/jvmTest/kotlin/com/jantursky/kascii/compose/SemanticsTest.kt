package com.jantursky.kascii.compose

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.core.Fields
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.Motion
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SemanticsTest {
    private val hidden = SemanticsMatcher.keyIsDefined(SemanticsProperties.HideFromAccessibility)
    private val grid = Grid(4, 1).apply { write(0, 0, "ab c") }

    @Test
    fun aDescriptionMakesTheArtAnImage() =
        runAsciiTest {
            setAsciiContent {
                AsciiCanvas(
                    Fields.plasma(),
                    Modifier.size(50.dp),
                    motion = Motion.Reduced,
                    contentDescription = "Plasma",
                )
            }
            onNodeWithContentDescription("Plasma")
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
            onAllNodes(hidden).assertCountEquals(0)
        }

    @Test
    fun artWithoutADescriptionIsHiddenFromAccessibility() =
        runAsciiTest {
            setAsciiContent { AsciiGridView(grid, Modifier.size(50.dp)) }
            onAllNodes(hidden).assertCountEquals(1)
        }

    @Test
    fun textArtStaysReadableWithoutADescription() =
        runAsciiTest {
            setAsciiContent { AsciiGridView(grid, Modifier.size(50.dp), renderer = AsciiRenderer.Text) }
            onAllNodes(hidden).assertCountEquals(0)
            onNode(hasText("ab c")).assertExists()
        }

    @Test
    fun textArtWithADescriptionKeepsBoth() =
        runAsciiTest {
            setAsciiContent {
                AsciiGridView(grid, Modifier.size(50.dp), renderer = AsciiRenderer.Text, contentDescription = "Letters")
            }
            onNodeWithContentDescription("Letters").assertExists()
            onNode(hasText("ab c"), useUnmergedTree = true).assertExists()
        }
}
