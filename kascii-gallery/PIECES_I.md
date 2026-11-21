# Gallery pieces, set I

Seven original animated scenes of built places and space in `src/commonMain/kotlin/com/jantursky/kascii/gallery/i/`,
all 192 by 48 cells with a dark ground. Each file has a `xxxPiece(seed)` factory returning an `AsciiPiece` and an
`AsciiXxx(...)` composable that plays it with `AsciiPieceView`. `galleryPiecesI` in `CatalogI.kt` lists them for the
catalog, and `IKit.kt` holds the shared canvas, noise (with an optional x period, so cloud layers can scroll and still
close their loop), palette, glow and star helpers. The art and code are original.

| Piece | Category | Loop | Ported from | Status | Platforms checked |
| --- | --- | --- | --- | --- | --- |
| Space station | scenes | 24 s | original | done | JVM text grid only |
| Moon base | scenes | 20 s | original | done | JVM text grid only |
| Observatory | scenes | 30 s | original | done | JVM text grid only |
| Desert highway | scenes | 12 s | original | done | JVM text grid only |
| Neon arcade | scenes | 8 s | original | done | JVM text grid only |
| Clock tower | scenes | 36 s | original | done | JVM text grid only |
| Airship sky | scenes | 30 s | original | done | JVM text grid only |

Notes:

- Clock tower runs as a time-lapse: the minute hand turns twelve times per loop and the hour hand once, the only way
  both hands can close the loop.
- Observatory star trails turn a sixth of a circle per loop over a six-fold symmetric star field, so the sky is evenly
  filled at every moment.

## Verified

- `Grid.toText()` output of every piece on the JVM: determinism, size, seeds, call-order independence, loop
  closure and the allowed glyph set (`PiecesITest`) and golden frames (`GoldenPiecesITest`, files in
  `src/jvmTest/resources/golden/i`).
- Frames looked at as rasterised SVG exports during development; a frame renders in roughly 1 to 5 ms on a laptop JVM.

## Not verified

- Not yet wired into the demo gallery or `GalleryComposeTest`.
- Colors on real light and dark pages, and frame times on any device.
