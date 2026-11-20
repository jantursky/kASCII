package com.jantursky.kascii.gallery.d

import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.gallery.auroraCabinPiece
import com.jantursky.kascii.gallery.cherryBlossomPiece
import com.jantursky.kascii.gallery.rainyCityPiece
import com.jantursky.kascii.gallery.rocketLaunchPiece

/** A piece under test: its file-safe [name], whether a seed changes it, and a builder for a seed. */
internal class PieceCase(
    val name: String,
    val seeded: Boolean,
    val create: (seed: Int) -> AsciiPiece,
)

internal val pieceCases: List<PieceCase> =
    listOf(
        PieceCase("aurora-cabin", true) { auroraCabinPiece(it) },
        PieceCase("rainy-city", true) { rainyCityPiece(it) },
        PieceCase("cherry-blossom", true) { cherryBlossomPiece(it) },
        PieceCase("rocket-launch", true) { rocketLaunchPiece(it) },
    )
