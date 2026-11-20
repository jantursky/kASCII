package com.jantursky.kascii.gallery.g

import com.jantursky.kascii.gallery.GalleryEntry

/** The animated scenes of this set, all originals, one entry per piece, each built for a seed. */
public val galleryPiecesG: List<GalleryEntry> =
    listOf(
        GalleryEntry("waking-giant", "Waking giant", "scenes", null) { wakingGiantPiece(it) },
        GalleryEntry("moon-angler", "Moon angler", "scenes", null) { moonAnglerPiece(it) },
    )
