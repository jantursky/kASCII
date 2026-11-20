package com.jantursky.kascii.compose.platform

import android.animation.ValueAnimator
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.jantursky.kascii.compose.resources.Res
import com.jantursky.kascii.compose.resources.kascii_mono
import org.jetbrains.compose.resources.Font as ResourceFont

// Below API 26 there is no static check. Compose's MotionDurationScale, which follows the animator duration
// scale on every API level, covers those versions.
internal actual fun platformReducedMotion(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ValueAnimator.areAnimatorsEnabled()

@Composable
internal actual fun rememberBundledFont(): Font? = ResourceFont(Res.font.kascii_mono)

private const val ASSET_URI = "file:///android_asset/"

// Compose resources ship fonts as assets; an asset font is opened lazily by the resolver that draws it.
internal actual suspend fun CompositionLocalConsumerModifierNode.loadBundledFont(): FontFamily {
    val path = Res.getUri("font/kascii_mono.ttf").removePrefix(ASSET_URI)
    return FontFamily(Font(path, currentValueOf(LocalContext).assets))
}

internal actual fun standaloneFontResolver(): FontFamily.Resolver? = null
