package com.jantursky.kascii.compose.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

/** Whether the platform asks apps to reduce motion. Read when a view composes, so no observer is needed. */
internal expect fun platformReducedMotion(): Boolean

/**
 * The bundled font, or null while it is still loading. Only the web loads fonts asynchronously; elsewhere the
 * first call returns it.
 */
@Composable
internal expect fun rememberBundledFont(): Font?

/** Loads the bundled font outside composition, for modifier nodes that cannot call [rememberBundledFont]. */
internal expect suspend fun CompositionLocalConsumerModifierNode.loadBundledFont(): FontFamily

/** A font resolver that needs no composition, or null where the platform requires a context for one (Android). */
internal expect fun standaloneFontResolver(): FontFamily.Resolver?
