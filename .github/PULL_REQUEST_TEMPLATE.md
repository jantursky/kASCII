## What and why

<!-- What this changes, and the problem it solves. Link the issue if there is one. -->

## Checklist

- [ ] `./gradlew spotlessCheck apiCheck jvmTest testAndroidHostTest koverVerify` passes
- [ ] `./gradlew iosSimulatorArm64Test` passes, if iOS code changed
- [ ] `./gradlew wasmJsTest` passes, if common code changed
- [ ] `./gradlew apiDump` was run, if the public API changed
- [ ] New behavior has tests
- [ ] `CHANGELOG.md` has an entry under `## [Unreleased]`
