package com.jantursky.kascii.gallery

import com.jantursky.kascii.core.AsciiPiece

/**
 * One gallery piece as the catalog lists it. [credit] names the work a piece was ported from, or is null for an
 * original. [create] builds the piece for a seed.
 */
public class GalleryEntry(
    public val id: String,
    public val title: String,
    public val category: String,
    public val credit: String?,
    public val create: (seed: Int) -> AsciiPiece,
)
