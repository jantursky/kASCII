package com.jantursky.kascii.gallery.o

import com.jantursky.kascii.core.AsciiPiece

/** A piece under test: its file-safe [name], whether a seed changes it, and a builder for a seed. */
internal class PieceCase(
    val name: String,
    val seeded: Boolean,
    val create: (seed: Int) -> AsciiPiece,
)

internal val pieceCases: List<PieceCase> =
    listOf(
        PieceCase("gear-train", true) { gearTrainPiece(it) },
        PieceCase("candle-flame", true) { candleFlamePiece(it) },
        PieceCase("snow-globe", true) { snowGlobePiece(it) },
        PieceCase("lantern-festival", true) { lanternFestivalPiece(it) },
        PieceCase("vinyl-record", true) { vinylRecordPiece(it) },
        PieceCase("ocean-waves", true) { oceanWavesPiece(it) },
    )
