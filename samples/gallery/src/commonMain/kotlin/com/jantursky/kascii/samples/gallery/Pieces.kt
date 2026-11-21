package com.jantursky.kascii.samples.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.PieceMeta
import com.jantursky.kascii.gallery.GalleryEntry
import com.jantursky.kascii.gallery.g.galleryPiecesG
import com.jantursky.kascii.gallery.galleryPiecesA
import com.jantursky.kascii.gallery.galleryPiecesB
import com.jantursky.kascii.gallery.galleryPiecesC
import com.jantursky.kascii.gallery.galleryPiecesD
import com.jantursky.kascii.gallery.galleryPiecesE
import com.jantursky.kascii.gallery.galleryPiecesF
import com.jantursky.kascii.gallery.h.galleryPiecesH
import com.jantursky.kascii.gallery.i.galleryPiecesI
import com.jantursky.kascii.gallery.j.galleryPiecesJ
import com.jantursky.kascii.gallery.k.galleryPiecesK
import com.jantursky.kascii.gallery.l.galleryPiecesL
import com.jantursky.kascii.gallery.m.galleryPiecesM
import com.jantursky.kascii.gallery.n.galleryPiecesN
import com.jantursky.kascii.gallery.o.galleryPiecesO
import com.jantursky.kascii.samples.gallery.examples.PieceExample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Every piece of the gallery module, in catalog order. */
val Pieces: List<GalleryEntry> =
    galleryPiecesA + galleryPiecesB + galleryPiecesC + galleryPiecesD + galleryPiecesE +
        galleryPiecesF + galleryPiecesG + galleryPiecesH + galleryPiecesI + galleryPiecesJ + galleryPiecesK +
        galleryPiecesL +
        galleryPiecesM +
        galleryPiecesN +
        galleryPiecesO

/** The ratio of glyph cell height to width of the bundled font: a line is 1.32 em and a glyph 0.6 em. */
internal const val CELL_ASPECT = 2.2f

private val StageHeight = 440.dp
private val TwoColumns = 640.dp
private val CardGap = 16.dp

internal fun PieceMeta.aspect(): Float = cols / (rows * CELL_ASPECT)

/** The piece, large, on its own stage. Fills the window in embed mode. */
@Composable
fun PieceStage(
    entry: GalleryEntry,
    state: GalleryState,
) {
    Stage(state, minHeight = 420) {
        PieceExample(
            entry,
            state.seed,
            state.speed,
            state.motion,
            state.renderer,
            state.theme,
            if (state.embed) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(StageHeight),
        )
    }
}

/** The grid of every piece, filtered by category. */
@Composable
fun PiecesGrid(state: GalleryState) {
    val categories = remember { listOf(ALL) + Pieces.map { it.category }.distinct() }
    var category by remember { mutableStateOf(ALL) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Choice(categories, category, { category = it }, { it })
        PieceCards(Pieces.filter { category == ALL || it.category == category }, state)
    }
}

/** Cards in as many equal columns as fit. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PieceCards(
    entries: List<GalleryEntry>,
    state: GalleryState,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= TwoColumns) 2 else 1
        val width: Dp = (maxWidth - CardGap * (columns - 1)) / columns
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(CardGap),
            verticalArrangement = Arrangement.spacedBy(CardGap),
        ) {
            entries.forEach { PieceCard(it, state, Modifier.width(width)) }
        }
    }
}

private const val ALL = "all"

/** A card with the live piece, its title, category and credit. Opens the piece's page. */
@Composable
fun PieceCard(
    entry: GalleryEntry,
    state: GalleryState,
    modifier: Modifier = Modifier,
) {
    val colors = Gallery.colors
    val navigate = LocalNavigate.current
    // Building a scene fills palettes and layers, which takes long enough on a phone to stall the page if every card
    // does it on the main thread at once. Cards build theirs on a worker and show a blank stage until it is ready.
    val piece by produceState<AsciiPiece?>(null, entry) { value = withContext(Dispatchers.Default) { entry.create(0) } }
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier
            .pressable(Role.Button, shape) { navigate(entry.id) }
            .background(colors.surface2)
            .border(1.dp, colors.border, shape),
    ) {
        val stage =
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .background(Color(state.theme.paper.argb))
                .padding(8.dp)
        val built = piece
        if (built == null) {
            Box(stage)
        } else {
            PieceExample(entry, 0, state.speed, state.motion, state.renderer, state.theme, stage, built)
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicText(entry.title, style = Gallery.type.body.copy(color = colors.strong))
            BasicText(
                entry.category + (piece?.meta?.let { " · ${it.cols}x${it.rows}" } ?: ""),
                style = Gallery.type.caption.copy(color = colors.muted),
            )
            entry.credit?.let { BasicText(it, style = Gallery.type.caption.copy(color = colors.muted)) }
        }
    }
}
