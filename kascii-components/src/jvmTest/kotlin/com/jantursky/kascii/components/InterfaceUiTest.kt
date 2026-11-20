package com.jantursky.kascii.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class InterfaceUiTest {
    @Test
    fun buttonClicksAndMeetsTouchTarget() =
        runUiTest {
            var clicks = 0
            setComponent { AsciiButton("OK", onClick = { clicks++ }) }
            val node = onNodeWithContentDescription("OK")
            node.assertHasClickAction().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            node.performClick()
            assertEquals(1, clicks)
        }

    @Test
    fun buttonActivatesFromKeyboardWhenFocused() =
        runUiTest {
            var clicks = 0
            val focus = FocusRequester()
            setComponent { AsciiButton("Go", onClick = { clicks++ }, modifier = Modifier.focusRequester(focus)) }
            runOnIdle { focus.requestFocus() }
            onNodeWithContentDescription("Go").assertIsFocused()
            onNodeWithContentDescription("Go").performKeyInput { pressKey(Key.Enter) }
            onNodeWithContentDescription("Go").performKeyInput { pressKey(Key.Spacebar) }
            assertEquals(2, clicks)
        }

    @Test
    fun disabledButtonDoesNotClick() =
        runUiTest {
            var clicks = 0
            setComponent { AsciiButton("No", onClick = { clicks++ }, enabled = false) }
            onNodeWithContentDescription("No").performClick()
            assertEquals(0, clicks)
        }

    @Test
    fun checkboxExposesAndFlipsState() =
        runUiTest {
            var checked by mutableStateOf(false)
            setComponent { AsciiCheckbox(checked, { checked = it }, "Accept") }
            onNodeWithContentDescription("Accept").assertIsOff().performClick()
            onNodeWithContentDescription("Accept").assertIsOn()
            assertTrue(checked)
        }

    @Test
    fun toggleIsASwitch() =
        runUiTest {
            var on by mutableStateOf(false)
            setComponent { AsciiToggle(on, { on = it }, "Dark") }
            onNodeWithContentDescription("Dark").performClick()
            assertTrue(on)
        }

    @Test
    fun radioGroupSelectsOneOption() =
        runUiTest {
            var selected by mutableStateOf(0)
            setComponent { AsciiRadioGroup(listOf("One", "Two"), selected) { selected = it } }
            onNodeWithContentDescription("One").assertIsSelected()
            onNodeWithContentDescription("Two").performClick()
            onNodeWithContentDescription("Two").assertIsSelected()
            assertEquals(1, selected)
        }

    @Test
    fun tabsSelect() =
        runUiTest {
            var selected by mutableStateOf(0)
            setComponent { AsciiTabs(listOf("Home", "Docs"), selected, { selected = it }) }
            onNodeWithContentDescription("Docs").performClick()
            assertEquals(1, selected)
        }

    @Test
    fun sliderReportsRangeAndAcceptsProgressAndKeys() =
        runUiTest {
            var value by mutableStateOf(0.5f)
            val focus = FocusRequester()
            setComponent {
                AsciiSlider(value, { value = it }, Modifier.focusRequester(focus), label = "Volume", steps = 3)
            }
            val node = onNodeWithContentDescription("Volume")
            node.assert(hasStateDescription("50%"))
            node.performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
            assertEquals(1f, value)
            runOnIdle { focus.requestFocus() }
            node.performKeyInput { pressKey(Key.DirectionLeft) }
            assertEquals(0.75f, value)
            node.performKeyInput { pressKey(Key.MoveHome) }
            assertEquals(0f, value)
        }

    @Test
    fun progressBarsDescribeThemselves() =
        runUiTest {
            mainClock.autoAdvance = false
            setComponent {
                Column {
                    AsciiProgressBar(0.4f, label = "Upload")
                    AsciiProgressBar(null, label = "Sync")
                }
            }
            onNodeWithContentDescription("Upload").assert(hasStateDescription("40%"))
            onNodeWithContentDescription("Sync").assert(
                androidx.compose.ui.test.SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    androidx.compose.ui.semantics.ProgressBarRangeInfo.Indeterminate,
                ),
            )
        }

    @Test
    fun spinnerIsNamed() =
        runUiTest {
            mainClock.autoAdvance = false
            setComponent { AsciiSpinner(label = "Saving") }
            onNodeWithContentDescription("Saving").assertExists()
        }

    @Test
    fun textFieldIsFocusableAndNamed() =
        runUiTest {
            var text by mutableStateOf("")
            setComponent { AsciiTextField(text, { text = it }, label = "Name", placeholder = "type") }
            onNodeWithContentDescription("Name").assert(isFocusable())
        }

    @Test
    fun decorativeArtCarriesItsDescription() =
        runUiTest {
            setComponent {
                Column {
                    AsciiCard("Hello world", title = "Greeting")
                    AsciiBadge("NEW", tone = AsciiTone.Success)
                    AsciiAvatar("Jan Tursky")
                    AsciiTable(listOf(AsciiTableColumn("A")), listOf(listOf("1")))
                    AsciiTree(AsciiTreeNode("src", listOf(AsciiTreeNode("a.kt"))))
                    AsciiCalendar(2026, 2, selectedDay = 14)
                    AsciiClock(9, 5)
                    AsciiBigText("Hi")
                    AsciiNotFound()
                }
            }
            listOf(
                "Greeting. Hello world",
                "NEW",
                "Jan Tursky",
                "Table, 1 columns: A. Row 1: 1",
                "src (a.kt)",
                "Calendar, February 2026, selected day 14",
                "09:05",
                "Hi",
                "404, Page not found",
            ).forEach { onNodeWithContentDescription(it).assertExists() }
        }

    @Test
    fun toastIsALiveRegionAndDismisses() =
        runUiTest {
            var dismissed = false
            setComponent { AsciiToast("Saved", tone = AsciiTone.Success, onDismiss = { dismissed = true }) }
            onNodeWithContentDescription("Success: Saved").assertExists()
            onNodeWithContentDescription("Dismiss").performClick()
            assertTrue(dismissed)
        }

    @Test
    fun dialogButtonsWork() =
        runUiTest {
            var confirmed = false
            setComponent { AsciiDialog("Delete", "Sure?", {}, "Yes", { confirmed = true }, dismissLabel = "No") }
            onNodeWithContentDescription("Yes").performClick()
            assertTrue(confirmed)
            onNodeWithContentDescription("No").assertExists()
        }
}
