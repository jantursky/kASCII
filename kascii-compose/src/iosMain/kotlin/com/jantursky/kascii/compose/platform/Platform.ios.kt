package com.jantursky.kascii.compose.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import com.jantursky.kascii.compose.resources.Res
import com.jantursky.kascii.compose.resources.kascii_mono
import org.jetbrains.compose.resources.Font
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

internal actual fun platformReducedMotion(): Boolean = UIAccessibilityIsReduceMotionEnabled()

@Composable
internal actual fun rememberBundledFont(): Font? = Font(Res.font.kascii_mono)
