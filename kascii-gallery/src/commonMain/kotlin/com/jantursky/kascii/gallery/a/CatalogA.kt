package com.jantursky.kascii.gallery

private const val CREDIT_PREFIX = "Ported from ascii.rest"

/** The pieces of the first gallery batch: scenes, shapes, space, physics, nature and one shader field. */
public val galleryPiecesA: List<GalleryEntry> =
    listOf(
        GalleryEntry("alpine-dawn", "Alpine dawn", "scenes", "Re-imagined from ascii.rest `alpine-dawn`") {
            alpineDawnPiece(it)
        },
        GalleryEntry("hilltop-night", "Hilltop night", "scenes", null) { hilltopNightPiece(it) },
        GalleryEntry("donut", "Donut", "shapes", "$CREDIT_PREFIX `donut`") { donutPiece() },
        GalleryEntry("cube", "Spinning cube", "shapes", null) { cubePiece() },
        GalleryEntry("heart", "Beating heart", "shapes", null) { heartPiece() },
        GalleryEntry("starfield", "Starfield", "space", null) { starfieldPiece(it) },
        GalleryEntry("solar-system", "Solar system", "space", null) { solarSystemPiece(it) },
        GalleryEntry("pond-ripples", "Pond ripples", "physics", null) { pondRipplesPiece(it) },
        GalleryEntry("bouncing-balls", "Bouncing balls", "physics", "$CREDIT_PREFIX `bouncing-balls`") {
            bouncingBallsPiece(it)
        },
        GalleryEntry("rain", "Rain shower", "nature", null) { rainPiece(it) },
        GalleryEntry("campfire", "Campfire", "nature", null) { campfirePiece(it) },
        GalleryEntry("fractal-tree", "Fractal tree", "nature", "$CREDIT_PREFIX `fractal-tree`") {
            fractalTreePiece(it)
        },
        GalleryEntry("interference-field", "Interference field", "shaders", null) { interferencePiece(it) },
    )
