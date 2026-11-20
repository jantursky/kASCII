package com.jantursky.kascii.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.compose.kasciiFontFamily
import com.jantursky.kascii.core.Rgb

/**
 * A single-line text field framed as `[ text ]`. The frame turns into solid bars while focused. [label] is the
 * accessible name, [placeholder] shows while the field is empty. [cols] is the visible width in cells.
 */
@Composable
public fun AsciiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    cols: Int = 24,
    placeholder: String = "",
    enabled: Boolean = true,
) {
    val strings = LocalAsciiStrings.current
    val theme = LocalKasciiTheme.current
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val ink = monoTextStyle(if (enabled) theme.ink else theme.dim())
    val frame = monoTextStyle(if (focused) theme.accent else theme.dim())
    val (open, close) = fieldBrackets(focused)
    Row(modifier.heightIn(min = MinTouchTarget), verticalAlignment = Alignment.CenterVertically) {
        BasicText(open.toString(), Modifier.clearAndSetSemantics {}, style = frame)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier.width(CellWidth * cols).heightIn(min = MinTouchTarget).semantics {
                    contentDescription = label ?: strings.textField
                },
            enabled = enabled,
            singleLine = true,
            textStyle = ink,
            cursorBrush = SolidColor(Color(theme.accent.argb)),
            interactionSource = source,
            decorationBox = { field ->
                Box(Modifier.heightIn(min = MinTouchTarget), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) BasicText(placeholder.take(cols), style = monoTextStyle(theme.dim()))
                    field()
                }
            },
        )
        BasicText(close.toString(), Modifier.clearAndSetSemantics {}, style = frame)
    }
}

/** The characters that frame a text field. */
internal fun fieldBrackets(focused: Boolean): Pair<Char, Char> = if (focused) '▌' to '▐' else '[' to ']'

/** Monospace text sized so one glyph is as wide as a grid cell. */
@Composable
internal fun monoTextStyle(color: Rgb): TextStyle {
    val density = LocalDensity.current
    return TextStyle(
        color = Color(color.argb),
        fontFamily = kasciiFontFamily(),
        fontSize = with(density) { (CellWidth.value / GLYPH_ADVANCE).dp.toSp() },
        lineHeight = with(density) { CellHeight.toSp() },
    )
}

/** Advance width of the bundled font as a share of its font size. */
private const val GLYPH_ADVANCE = 0.6f
