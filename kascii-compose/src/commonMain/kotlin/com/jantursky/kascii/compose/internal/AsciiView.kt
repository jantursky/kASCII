package com.jantursky.kascii.compose.internal

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.compose.rememberKasciiFont
import com.jantursky.kascii.core.KasciiTheme

/** The cell width a fitted grid prefers when nothing constrains its size. */
internal val DefaultCell: Dp = 8.dp

/**
 * The one view behind every public grid composable. Nothing is drawn until the bundled font is ready, so the web
 * never shows a frame in a fallback font.
 */
@Composable
internal fun AsciiView(
    source: GridSource,
    cell: Dp,
    clock: ClockSpec,
    renderer: AsciiRenderer,
    theme: KasciiTheme,
    contentDescription: String?,
    modifier: Modifier,
) {
    val font = rememberKasciiFont()
    val described = modifier.asciiSemantics(contentDescription, renderer == AsciiRenderer.Text)
    when {
        font == null -> Spacer(described)
        renderer == AsciiRenderer.Text -> TextAsciiView(source, cell, font, theme, clock, described)
        else -> Spacer(described.then(AsciiElement(source, cell, font, theme, renderer, clock)))
    }
}

/**
 * A description makes the art an image to accessibility services. Art without one is decorative and hidden from
 * them, unless it is real text, which stays readable.
 */
internal fun Modifier.asciiSemantics(
    contentDescription: String?,
    isText: Boolean,
): Modifier =
    when {
        contentDescription != null -> {
            semantics {
                this.contentDescription = contentDescription
                role = Role.Image
            }
        }

        isText -> {
            this
        }

        else -> {
            semantics { hideFromAccessibility() }
        }
    }
