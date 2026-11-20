package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density

/**
 * Runs [block] in a 600 x 600 window with density 1, so one dp is one pixel and cell sizes are exact. Tests of
 * components that animate must set `mainClock.autoAdvance = false`: their frame loop never goes idle.
 */
@OptIn(ExperimentalTestApi::class)
fun runUiTest(block: ComposeUiTest.() -> Unit) = runDesktopComposeUiTest(600, 600) { block() }

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.setComponent(content: @Composable () -> Unit) {
    setContent { CompositionLocalProvider(LocalDensity provides Density(1f)) { content() } }
}
