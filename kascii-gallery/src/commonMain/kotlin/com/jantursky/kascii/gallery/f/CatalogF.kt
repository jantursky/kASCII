package com.jantursky.kascii.gallery

/** The animated scenes of this set, all originals, one entry per piece, each built for a seed. */
public val galleryPiecesF: List<GalleryEntry> =
    listOf(
        GalleryEntry("abyss-lure", "Abyss lure", "scenes", null) { abyssLurePiece(it) },
        GalleryEntry("witch-moon", "Witch moon", "scenes", null) { witchMoonPiece(it) },
    )
