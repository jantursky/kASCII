package com.jantursky.kascii.compose.platform

import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.platform.Font
import com.jantursky.kascii.compose.resources.Res
import com.jantursky.kascii.compose.resources.kascii_mono
import org.jetbrains.compose.resources.getFontResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment

internal actual suspend fun CompositionLocalConsumerModifierNode.loadBundledFont(): FontFamily =
    FontFamily(Font("kascii_mono", getFontResourceBytes(getSystemResourceEnvironment(), Res.font.kascii_mono)))

internal actual fun standaloneFontResolver(): FontFamily.Resolver? = createFontFamilyResolver()
