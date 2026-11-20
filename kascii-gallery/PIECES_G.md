# Gallery pieces, set G

Two original animated scenes in `src/commonMain/kotlin/com/jantursky/kascii/gallery/g/`, both 192 by 48 cells with
a dark ground. Each file has a `xxxPiece(...)` factory returning an `AsciiPiece` and an `AsciiXxx(...)` composable
that plays it with `AsciiPieceView`. `galleryPiecesG` in `CatalogG.kt` lists them for the catalog, and `GKit.kt`
holds the small shared canvas, noise and palette helpers. The art and code are original.

| Piece | Category | Ported from | Status | Platforms checked |
| --- | --- | --- | --- | --- |
| Waking giant | scenes | original | done | JVM text grid only |
| Moon angler | scenes | original | done | JVM text grid only |

## Verified

- `Grid.toText()` output of every piece on the JVM: determinism, size, seeds, call-order independence, loop
  closure and the allowed glyph set (`PiecesGTest`) and golden frames (`GoldenPiecesGTest`, files in
  `src/jvmTest/resources/golden/g`).

## Not verified

- Not yet wired into the demo gallery or `GalleryComposeTest`.
- Colors on real light and dark pages, and frame times on any device.
