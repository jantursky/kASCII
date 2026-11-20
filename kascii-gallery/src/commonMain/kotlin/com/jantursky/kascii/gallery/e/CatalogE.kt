package com.jantursky.kascii.gallery

/** The animated scenes of this set, all originals, one entry per piece, each built for a seed. */
public val galleryPiecesE: List<GalleryEntry> =
    listOf(
        GalleryEntry("orbit-sunrise", "Orbit sunrise", "scenes", null) { orbitSunrisePiece(it) },
        GalleryEntry("volcano-night", "Volcano night", "scenes", null) { volcanoNightPiece(it) },
    )
