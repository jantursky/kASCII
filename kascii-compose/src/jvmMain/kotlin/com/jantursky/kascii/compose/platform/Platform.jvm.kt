package com.jantursky.kascii.compose.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import com.jantursky.kascii.compose.resources.Res
import com.jantursky.kascii.compose.resources.kascii_mono
import org.jetbrains.compose.resources.Font

/** Desktop systems share no reduced-motion setting Java can read, so apps can pass `-Dkascii.reducedMotion=true`. */
internal actual fun platformReducedMotion(): Boolean = System.getProperty("kascii.reducedMotion").toBoolean()

@Composable
internal actual fun rememberBundledFont(): Font? = Font(Res.font.kascii_mono)
