package com.jantursky.kascii.compose.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import com.jantursky.kascii.compose.resources.Res
import com.jantursky.kascii.compose.resources.kascii_mono
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont

private fun prefersReducedMotion(): Boolean =
    js("typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches")

internal actual fun platformReducedMotion(): Boolean = prefersReducedMotion()

// The browser fetches the font; until it arrives the views draw nothing rather than a fallback font.
@OptIn(ExperimentalResourceApi::class)
@Composable
internal actual fun rememberBundledFont(): Font? = preloadFont(Res.font.kascii_mono).value
