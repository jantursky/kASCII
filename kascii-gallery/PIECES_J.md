# Gallery pieces, set J

Seven original animated creature scenes in `src/commonMain/kotlin/com/jantursky/kascii/gallery/j/`, all 192 by 48
cells with a dark ground. Each file has a `xxxPiece(seed)` factory returning an `AsciiPiece` and an `AsciiXxx(...)`
composable that plays it with `AsciiPieceView`. `galleryPiecesJ` in `CatalogJ.kt` lists them for the catalog under
the `creatures` category, and `JKit.kt` holds the small shared canvas, noise and palette helpers. The art and code are
original.

| Piece | Category | Ported from | Status | Platforms checked |
| --- | --- | --- | --- | --- |
| Whale song | creatures | original | done | JVM text grid only |
| Fox in snow | creatures | original | done | JVM text grid only |
| Butterfly meadow | creatures | original | done | JVM text grid only |
| Dragon over castle | creatures | original | done | JVM text grid only |
| Koi pond | creatures | original | done | JVM text grid only |
| Firefly marsh | creatures | original | done | JVM text grid only |
| Sea turtle reef | creatures | original | done | JVM text grid only |

## Verified

- `Grid.toText()` output of every piece on the JVM: determinism, size, seeds, call-order independence, loop
  closure and the allowed glyph set (`PiecesJTest`) and golden frames (`GoldenPiecesJTest`, files in
  `src/jvmTest/resources/golden/j`).
- Common code compiles for wasmJs and iosSimulatorArm64.

## Not verified

- Not yet wired into the demo gallery or `GalleryComposeTest`.
- Colors on real light and dark pages, and frame times on any device.
