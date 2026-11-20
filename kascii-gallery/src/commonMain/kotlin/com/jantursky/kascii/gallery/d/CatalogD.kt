package com.jantursky.kascii.gallery

/** The animated scenes of this set, all originals, one entry per piece, each built for a seed. */
public val galleryPiecesD: List<GalleryEntry> =
    listOf(
        GalleryEntry("aurora-cabin", "Aurora cabin", "scenes", null) { auroraCabinPiece(it) },
        GalleryEntry("rainy-city", "Rainy city", "scenes", null) { rainyCityPiece(it) },
        GalleryEntry("cherry-blossom", "Cherry blossom", "scenes", null) { cherryBlossomPiece(it) },
        GalleryEntry("rocket-launch", "Rocket launch", "scenes", null) { rocketLaunchPiece(it) },
    )
