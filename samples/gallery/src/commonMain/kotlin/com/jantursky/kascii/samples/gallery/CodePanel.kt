package com.jantursky.kascii.samples.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp

/** The source of the example on the page, selectable, with Kotlin highlighted. */
@Composable
fun CodePanel(
    source: String,
    modifier: Modifier = Modifier,
) {
    val colors = Gallery.colors
    val highlighted = remember(source, colors) { highlight(source, colors) }
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, colors.border, shape)
            .background(colors.code),
    ) {
        BasicText(
            "Kotlin, ${source.lines().size} lines",
            Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
            style = Gallery.type.caption.copy(color = colors.muted),
        )
        BasicText(
            highlighted,
            Modifier
                .semantics { contentDescription = "Source code of the example" }
                .horizontalScroll(rememberScrollState())
                .padding(16.dp),
            style = Gallery.type.code.copy(color = colors.text),
            softWrap = false,
        )
    }
}

private val keywords =
    setOf(
        "fun",
        "val",
        "var",
        "if",
        "else",
        "when",
        "return",
        "class",
        "object",
        "private",
        "public",
        "internal",
        "override",
        "remember",
        "by",
        "in",
        "for",
        "is",
        "null",
        "true",
        "false",
        "it",
        "enum",
        "this",
    )

private val token = Regex("""//[^\n]*|"(?:\\.|[^"\\])*"|\b\d[\d_.]*[fFLdp]*\b|@[A-Za-z]+|\b[A-Za-z_][A-Za-z0-9_]*\b""")

/** A small highlighter: comments, strings, numbers, annotations, keywords and type names. */
internal fun highlight(
    source: String,
    colors: GalleryColors,
): AnnotatedString {
    val string = if (colors.dark) Color(0xFF00BC7D) else Color(0xFF00785A)
    val number = colors.amber
    return buildAnnotatedString {
        append(source)
        for (match in token.findAll(source)) {
            val text = match.value
            val color =
                when {
                    text.startsWith("//") || text.startsWith("@") -> colors.muted
                    text.startsWith("\"") -> string
                    text[0].isDigit() -> number
                    text in keywords -> colors.link
                    text[0].isUpperCase() -> colors.strong
                    else -> null
                }
            if (color != null) addStyle(SpanStyle(color = color), match.range.first, match.range.last + 1)
        }
    }
}
