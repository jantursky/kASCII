# Gallery pieces, batch A

Source: `src/commonMain/kotlin/com/jantursky/kascii/gallery/a/`. Registry: `galleryPiecesA` in `CatalogA.kt`.
Every piece is a `xxxPiece(seed, ...)` factory plus a composable wrapper that calls `AsciiPieceView`.

| Piece | Category | Ported from | Status | Platforms checked |
| --- | --- | --- | --- | --- |
| alpine dawn | scenes | Re-imagined from ascii.rest `alpine-dawn` (same scene, noise ridges instead of ray-marching) | text frames reviewed | JVM text grid only |
| hilltop night | scenes | original | text frames reviewed | JVM text grid only |
| donut | shapes | Ported from ascii.rest `donut` | text frames reviewed | JVM text grid only |
| cube | shapes | original | text frames reviewed | JVM text grid only |
| heart | shapes | original | text frames reviewed | JVM text grid only |
| starfield | space | original | text frames reviewed | JVM text grid only |
| solar system | space | original | text frames reviewed | JVM text grid only |
| pond ripples | physics | original | text frames reviewed | JVM text grid only |
| bouncing balls | physics | Ported from ascii.rest `bouncing-balls` (closed-form hop instead of a physics simulation) | text frames reviewed | JVM text grid only |
| rain | nature | original | text frames reviewed | JVM text grid only |
| campfire | nature | original | text frames reviewed | JVM text grid only |
| fractal tree | nature | Ported from ascii.rest `fractal-tree` (same fork-and-sway idea, own geometry) | text frames reviewed | JVM text grid only |
| interference field | shaders | original, in the spirit of asciicn.dev noise and interference fields | text frames reviewed | JVM text grid only |

ascii.rest is MIT, Copyright (c) 2026 bas3line.

## What is verified

- JVM `jvmTest`: determinism at the still frame, non-blank frames, meta size against grid size, seed sensitivity,
  loop closure (`render(t) == render(t + loop)` for pieces that declare a loop), frames change over time, and a
  golden text snapshot per piece in `src/jvmTest/resources/golden/a/`. Regenerate with `KASCII_UPDATE_GOLDEN=1`.
- Goldens were read by eye as text. That is a check of the glyph layout only.

## UNVERIFIED

- Every composable wrapper: they compile against `AsciiPieceView` but were never run, because its body was a stub.
- All rendering: no pixels were drawn, so colors, contrast on the light theme and visual quality are unchecked.
  The pieces use plain ASCII glyphs only.
- Android, iOS and wasmJs: not compiled or tested for this batch.
- Reduced-motion behavior and offscreen pausing (owned by `AsciiPieceView`).
- Cost per frame on a device (the donut samples about 20k points per frame, the interference field and the pond
  ripples touch every cell per source).
