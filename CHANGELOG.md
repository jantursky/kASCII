# Changelog

All notable changes to kASCII are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project follows [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Changed

- The demo's pages of pieces and the overview use up to 1680dp of width, so wide screens show more cards per row.
- Every piece page in the demo has a "Use it in your app" section: the dependency, the code that plays the piece and a link to its source on GitHub.

## [1.1.0] - 2026-11-21

### Added

- Twenty-seven new animated pieces in `kascii-gallery`. Seven landscapes: mountain lake, windmill meadow, jungle temple, ice cave, floating islands, canyon river and snow village. Seven built places and space scenes: space station, moon base, observatory, desert highway, neon arcade, clock tower and airship sky. Seven creatures: whale song, fox in snow, butterfly meadow, dragon over castle, koi pond, firefly marsh and sea turtle reef. Six shader-style visuals: lightning storm, fireworks harbor, galaxy spiral, aurora ribbons, liquid metal and topographic contours.

### Fixed

- The glyph atlas is frozen once it is built, so Skia uploads it to the GPU once instead of on every draw. Pages with a large atlas, such as the Braille renderer, used several gigabytes of memory on iOS and crashed the app.
- Gallery cards build their piece off the main thread, so the Pieces page no longer stalls while it opens.

## [1.0.0] - 2026-11-20

### Added

- Fourteen new animated scenes in `kascii-gallery`: lighthouse, steam train, sailboat sunset, desert caravan, aurora cabin, rainy city, cherry blossom, rocket launch, orbit sunrise, volcano night, abyss lure, witch moon, waking giant and moon angler. They are 192x48 computed scenes on their own dark ground.
- `PieceMeta.ground` and the `ground` parameter of `asciiPiece` give a piece its own backdrop color, which `AsciiPieceView` paints behind it on any theme.
- `AsciiStrings` and `LocalAsciiStrings` translate the words components speak to screen readers and the calendar's month and weekday names.
- `Modifier.ascii(live = true)` samples on every frame, for content that animates inside a child's own graphics layer.
- `LocalKasciiMotion` sets the motion preference for components that have no `motion` parameter.
- `kascii-core`: the glyph engine for text-mode art, with fields, charsets, grids, themes, a frame clock and text, ANSI and SVG export.
- `kascii-compose`: Compose Multiplatform drawing of fields and pieces as glyphs.
- `kascii-components`: accessible text-mode UI and data components.
- `kascii-gallery`: a gallery of animated ASCII pieces.
