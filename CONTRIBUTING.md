# Contributing

Thanks for helping. Bug reports, docs fixes and pull requests are all welcome.

## Before you start

- For anything bigger than a small fix, open an issue first so we can agree on the approach.
- New fields, charsets and components need a clear use. Say what the piece is for and what it adds over a few lines of code.
- Rendering differs between platforms and fonts. Say which target, device and OS version you saw a problem on, and attach a screenshot if you can.

## Setup

You need JDK 17 and an Android SDK. The Gradle wrapper handles the rest. For iOS you need Xcode on macOS.

```sh
./gradlew :kascii-core:jvmTest    # desktop tests, quickest feedback
./gradlew spotlessApply           # format the code (ktlint)
```

## Where things live

| path                          | what                                                                 |
| ----------------------------- | -------------------------------------------------------------------- |
| `kascii-core`                 | The glyph engine: fields, charsets, grids, themes, the frame clock, text, ANSI and SVG export. No Compose. |
| `kascii-compose`              | Compose Multiplatform drawing of fields and pieces as glyphs.        |
| `kascii-components`           | Accessible text-mode UI and data components, built on the two above. |
| `kascii-gallery`              | A gallery of animated ASCII pieces.                                  |

## Checks

```sh
./gradlew spotlessCheck apiCheck jvmTest testAndroidHostTest koverXmlReport koverVerify   # what CI runs
./gradlew iosSimulatorArm64Test     # on macOS
./gradlew wasmJsTest
./gradlew apiDump                   # after changing the public API
./gradlew dokkaGenerate             # the API reference; a public symbol without KDoc fails the build
./scripts/pack-test.sh              # publish to Maven Local and compile a consumer against it
```

CI runs all of them on every pull request. `koverVerify` fails the build when coverage drops below the gate in each module's build file.

## Conventions

- Kotlin in the official style. Run `./gradlew spotlessApply` before you push.
- Everything that isn't public API is `internal`. Changing the public API means running `./gradlew apiDump` and reviewing the diff in each module's `api` folder.
- Pure logic (fields, charsets, grids, export) lives in `commonMain` and is tested in `commonTest`.
- Code ported from another project keeps its license notice and names the source in its KDoc. Add the license text to `THIRD_PARTY_NOTICES` in the same change.
- Every pull request adds at least one bullet under `## [Unreleased]` in `CHANGELOG.md`. CI fails a pull request whose Unreleased section is empty. A change users will not notice still gets a short bullet.

## License

By contributing you agree that your contributions are licensed under the MIT license.
