package com.jantursky.kascii.components

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.BoxStyle
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.rect
import kotlinx.coroutines.delay

/**
 * A notification line: `(i) message` in a rounded box. It is a polite live region, so screen readers announce it
 * when it appears. With [onDismiss] it shows a close button, and with [durationMillis] it dismisses itself.
 */
@Composable
public fun AsciiToast(
    message: String,
    modifier: Modifier = Modifier,
    tone: AsciiTone = AsciiTone.Info,
    onDismiss: (() -> Unit)? = null,
    durationMillis: Long? = null,
) {
    val currentDismiss by rememberUpdatedState(onDismiss)
    if (durationMillis != null) {
        LaunchedEffect(message, durationMillis) {
            delay(durationMillis)
            currentDismiss?.invoke()
        }
    }
    Row(
        modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = "${tone.name}: $message"
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GridSurface(renderToast(message, tone, LocalKasciiTheme.current))
        onDismiss?.let { AsciiButton("x", it, contentDescription = "Dismiss") }
    }
}

internal fun renderToast(
    message: String,
    tone: AsciiTone,
    theme: KasciiTheme,
): Grid {
    val grid = Grid(message.length + TOAST_CHROME, 3)
    grid.rect(0, 0, grid.cols, 3, BoxStyle.Rounded, color = theme.dim())
    grid.write(2, 1, "(${tone.marker()})", tone.color(theme))
    grid.write(6, 1, message)
    return grid
}

/** Border, padding and the `(i)` marker around the message. */
private const val TOAST_CHROME = 8
