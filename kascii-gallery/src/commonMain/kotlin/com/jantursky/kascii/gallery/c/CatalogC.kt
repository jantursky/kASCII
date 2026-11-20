package com.jantursky.kascii.gallery

/** The gallery pieces of this set, one entry per piece, each built for a seed. All of them are originals. */
public val galleryPiecesC: List<GalleryEntry> =
    listOf(
        GalleryEntry("lighthouse", "Lighthouse", "scenes", null) { lighthousePiece(it) },
        GalleryEntry("steam-train", "Steam train", "scenes", null) { steamTrainPiece(it) },
        GalleryEntry("sailboat-sunset", "Sailboat sunset", "scenes", null) { sailboatSunsetPiece(it) },
        GalleryEntry("desert-caravan", "Desert caravan", "scenes", null) { desertCaravanPiece(it) },
    )
