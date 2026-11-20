# Security policy

## Supported versions

Only the latest release of the `io.github.jantursky` kASCII artifacts (`kascii-core`, `kascii-compose`, `kascii-components` and `kascii-gallery`) gets fixes.

## Reporting a vulnerability

Please don't open a public issue. Use GitHub's [private vulnerability reporting](https://github.com/jantursky/kASCII/security/advisories/new), or email jan.tursky@gmail.com with a description and, if you can, a minimal reproduction.

You'll get an answer within a week. Once a fix is released, the advisory is published with credit, unless you'd rather stay anonymous.

## Things to know

- kASCII makes no network calls, collects no data and needs no permissions. Its output is text, ANSI and SVG strings, and the pixels drawn on screen.
- The bundled font, JetBrains Mono with a Unifont subset, ships inside the library. kASCII does not load font files or other art from disk or the network.
- Values you pass in, such as grids, fields and charsets, are used as given. Keep sizes sensible for your screen: a very large grid costs memory and time to draw.
