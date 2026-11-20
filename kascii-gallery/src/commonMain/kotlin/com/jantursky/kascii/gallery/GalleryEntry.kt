package com.jantursky.kascii.gallery

import com.jantursky.kascii.core.AsciiPiece

/**
 * One gallery piece as the catalog lists it. [credit] names the work a piece was ported from, or is null for an
 * original. [create] builds the piece for a seed.
 */
public class GalleryEntry(
    /** Stable identifier, also the page id in the demo. */
    public val id: String,
    /** Human-readable title. */
    public val title: String,
    /** Gallery category, such as scenes or shapes. */
    public val category: String,
    /** The work a piece was ported from, or null for an original. */
    public val credit: String?,
    /** Builds the piece for a seed. */
    public val create: (seed: Int) -> AsciiPiece,
)
