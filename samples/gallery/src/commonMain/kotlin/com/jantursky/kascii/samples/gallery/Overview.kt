package com.jantursky.kascii.samples.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.compose.AsciiCanvas
import com.jantursky.kascii.compose.ProvideKasciiTheme
import com.jantursky.kascii.core.Charsets
import com.jantursky.kascii.core.Fields

private const val INSTALL = "implementation(\"io.github.jantursky:kascii-compose:$GALLERY_VERSION\")"

/** A live plasma behind the title, then the way into the gallery. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Overview(state: GalleryState) {
    val colors = Gallery.colors
    val navigate = LocalNavigate.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(64.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 420.dp), contentAlignment = Alignment.Center) {
            ProvideKasciiTheme(state.theme) {
                AsciiCanvas(
                    Fields.plasma(scale = 5f, speed = 0.6f),
                    Modifier.fillMaxSize().alpha(0.1f),
                    charset = Charsets.Standard,
                    cell = 12.dp,
                    stillTime = 2f,
                    motion = state.motion,
                )
            }
            Column(
                Modifier.padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                BasicText(
                    "kASCII",
                    Modifier.semantics { heading() },
                    style = Gallery.type.hero.copy(color = colors.strong, textAlign = TextAlign.Center),
                )
                BasicText(
                    "Text-mode art that generates itself, for Compose Multiplatform: procedural fields, " +
                        "${Pieces.size} live pieces and a modifier that turns any composable into ASCII.",
                    style = Gallery.type.body.copy(color = colors.text, textAlign = TextAlign.Center),
                )
                BasicText(
                    INSTALL,
                    Modifier
                        .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                        .background(colors.code, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    style = Gallery.type.code.copy(color = colors.text),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Pill("Browse the pieces", { navigate("pieces") })
                    Pill("Procedural fields", { navigate("fields") }, tint = Tint.Amber)
                    Pill("Themes", { navigate("theme") }, tint = Tint.Red)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            SectionHeading("Live pieces")
            PieceCards(Pieces.take(FEATURED), state)
        }
    }
}

private const val FEATURED = 6
