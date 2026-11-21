package com.jantursky.kascii.gallery.l

import com.jantursky.kascii.gallery.GalleryEntry

/** The lit geometric studies of this set, all originals, one entry per piece, each built for a seed. */
public val galleryPiecesL: List<GalleryEntry> =
    listOf(
        GalleryEntry("torus-knot", "Torus knot", "shapes", null) { torusKnotPiece(it) },
        GalleryEntry("icosphere", "Icosphere", "shapes", null) { icospherePiece(it) },
        GalleryEntry("mobius-strip", "Mobius strip", "shapes", null) { mobiusStripPiece(it) },
        GalleryEntry("tesseract", "Tesseract", "shapes", null) { tesseractPiece(it) },
        GalleryEntry("dna-helix", "DNA helix", "shapes", null) { dnaHelixPiece(it) },
        GalleryEntry("spirograph", "Spirograph", "shapes", null) { spirographPiece(it) },
    )
