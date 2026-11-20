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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.compose.ProvideKasciiTheme

/** What a page belongs to: the nav lists every group but [Pieces], whose pages open from the grid. */
enum class Group { Start, Showcase, Pieces }

/**
 * One page of the gallery.
 *
 * @property sources File names, without extension, of the examples this page runs; their source is shown under it.
 * @property fills The page is a single stage that fills the window in embed mode, rather than scrolling content.
 * @property wide The page uses more than the 800dp of a prose page, as the grid of pieces does.
 */
class Page(
    val id: String,
    val group: Group,
    val title: String,
    val summary: String,
    val sources: List<String>,
    val fills: Boolean = false,
    val wide: Boolean = false,
    val content: @Composable (GalleryState) -> Unit,
)

/** The panel an example runs on: the theme's paper, so what you see is what the glyphs sit on. */
@Composable
fun Stage(
    state: GalleryState,
    modifier: Modifier = Modifier,
    minHeight: Int = 260,
    content: @Composable () -> Unit,
) {
    val colors = Gallery.colors
    val theme = state.theme
    val shape = RoundedCornerShape(8.dp)
    val frame = if (state.embed) Modifier.fillMaxSize() else Modifier.fillMaxWidth().heightIn(min = minHeight.dp)
    Box(
        modifier
            .then(frame)
            .clip(shape)
            .border(1.dp, colors.border, shape)
            .background(Color(theme.paper.argb))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        ProvideKasciiTheme(theme) { content() }
    }
}

/** The controls of one page, wrapping onto new lines. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Controls(content: @Composable () -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) { content() }
}

/** The stage-and-controls column most pages use. Embeds show the stage alone. */
@Composable
fun Demo(
    state: GalleryState,
    minHeight: Int = 260,
    controls: @Composable () -> Unit = {},
    stage: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Stage(state, minHeight = minHeight, content = stage)
        if (!state.embed) Controls(controls)
    }
}
