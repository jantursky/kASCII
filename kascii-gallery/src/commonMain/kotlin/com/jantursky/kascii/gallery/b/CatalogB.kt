package com.jantursky.kascii.gallery

private const val CREDIT_PREFIX = "Ported from ascii.rest `"
private const val CREDIT_SUFFIX = "` (MIT, bas3line)"

private fun ported(piece: String): String = CREDIT_PREFIX + piece + CREDIT_SUFFIX

/** The gallery pieces of this set, one entry per piece, each built for a seed. */
public val galleryPiecesB: List<GalleryEntry> =
    listOf(
        GalleryEntry("jellyfish", "Jellyfish", "creatures", ported("jellyfish")) { jellyfishPiece(it) },
        GalleryEntry("owl", "Owl", "creatures", null) { owlPiece(it) },
        GalleryEntry("analog-clock", "Analog clock", "objects", null) { analogClockPiece() },
        GalleryEntry("hourglass", "Hourglass", "objects", ported("hourglass")) { hourglassPiece(it) },
        GalleryEntry("lava-lamp", "Lava lamp", "objects", ported("lava lamp")) { lavaLampPiece(it) },
        GalleryEntry("mandelbrot", "Mandelbrot set", "generative", ported("mandelbrot")) { mandelbrotPiece() },
        GalleryEntry("glider-gun", "Glider gun", "generative", ported("glider gun")) { gliderGunPiece() },
        GalleryEntry("langtons-ant", "Langton's ant", "generative", ported("langtons ant")) { langtonsAntPiece(it) },
        GalleryEntry("doom-fire", "Doom fire", "effects", ported("doom fire")) { doomFirePiece(it) },
        GalleryEntry("matrix-rain", "Matrix rain", "effects", ported("matrix rain")) { matrixRainPiece(it) },
        GalleryEntry("synthwave", "Synthwave", "effects", ported("synthwave")) { synthwavePiece(it) },
        GalleryEntry("tunnel", "Tunnel", "effects", ported("tunnel")) { tunnelPiece(it) },
        GalleryEntry("crt-filter", "CRT filter", "media", null) { crtFilterPiece(it) },
    )
