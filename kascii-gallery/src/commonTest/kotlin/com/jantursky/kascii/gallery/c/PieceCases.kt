package com.jantursky.kascii.gallery.c

import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.gallery.desertCaravanPiece
import com.jantursky.kascii.gallery.lighthousePiece
import com.jantursky.kascii.gallery.sailboatSunsetPiece
import com.jantursky.kascii.gallery.steamTrainPiece

/** A piece under test: its file-safe [name], whether a seed changes it, and a builder for a seed. */
internal class PieceCase(
    val name: String,
    val seeded: Boolean,
    val create: (seed: Int) -> AsciiPiece,
)

internal val pieceCases: List<PieceCase> =
    listOf(
        PieceCase("lighthouse", true) { lighthousePiece(it) },
        PieceCase("steam-train", true) { steamTrainPiece(it) },
        PieceCase("sailboat-sunset", true) { sailboatSunsetPiece(it) },
        PieceCase("desert-caravan", true) { desertCaravanPiece(it) },
    )
