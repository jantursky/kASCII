# Gallery pieces, set F

Two original animated scenes in `src/commonMain/kotlin/com/jantursky/kascii/gallery/f/`. Each file has a
`xxxPiece(...)` factory returning an `AsciiPiece` and an `AsciiXxx(...)` composable that plays it with
`AsciiPieceView`. `galleryPiecesF` in `CatalogF.kt` lists them for the catalog. The art and code are original;
the motifs are common ones, drawn from scratch.

| Piece | Category | Ported from | Status | Platforms checked |
| --- | --- | --- | --- | --- |
| Abyss lure | scenes | original | done | JVM text grid only |
| Witch moon | scenes | original | done | JVM text grid only |

## Verified

- `Grid.toText()` output of every piece on the JVM: determinism, size, seeds, call-order independence, loop
  closure (`PiecesFTest`) and golden frames (`GoldenPiecesFTest`, files in `src/jvmTest/resources/golden/f`).
- Every piece composes through `AsciiPieceView` under reduced motion (`GalleryComposeTest`).

## Not verified

- Colors on real light and dark pages, and frame times on any device.
