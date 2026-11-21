# Gallery pieces, set H

Seven original animated landscape scenes in `src/commonMain/kotlin/com/jantursky/kascii/gallery/h/`, all 192 by 48
cells with a dark ground. Each file has a `xxxPiece(seed)` factory returning an `AsciiPiece` and an `AsciiXxx(...)`
composable that plays it with `AsciiPieceView`. `galleryPiecesH` in `CatalogH.kt` lists them for the catalog, and
`HKit.kt` holds the shared canvas, noise (including x-periodic noise for fields that drift a whole period per loop),
palette and drawing helpers. The art and code are original.

| Piece | Category | Ported from | Status | Platforms checked |
| --- | --- | --- | --- | --- |
| Mountain lake | scenes | original | done | JVM text grid only |
| Windmill meadow | scenes | original | done | JVM text grid only |
| Jungle temple | scenes | original | done | JVM text grid only |
| Ice cave | scenes | original | done | JVM text grid only |
| Floating islands | scenes | original | done | JVM text grid only |
| Canyon river | scenes | original | done | JVM text grid only |
| Snow village | scenes | original | done | JVM text grid only |

## Verified

- `Grid.toText()` output of every piece on the JVM: determinism, size, seeds, call-order independence, loop
  closure and the allowed glyph set (`PiecesHTest`) and golden frames (`GoldenPiecesHTest`, files in
  `src/jvmTest/resources/golden/h`).
- Frames looked at as rasterized SVG previews at three moments each during development.

## Not verified

- Not yet wired into the demo gallery or `GalleryComposeTest`.
- Colors on real light and dark pages, and frame times on any device.
