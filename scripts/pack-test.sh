#!/usr/bin/env bash
# Publishes the four modules to Maven Local and compiles a fresh consumer project against them for the JVM and the
# browser, so a broken POM, a missing variant or a leaked internal type fails here, not for users.
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
version=$(sed -n 's/^VERSION_NAME=//p' "$root/gradle.properties")
kotlin=$(sed -n 's/^kotlin = "\(.*\)"/\1/p' "$root/gradle/libs.versions.toml")
compose=$(sed -n 's/^compose = "\(.*\)"/\1/p' "$root/gradle/libs.versions.toml")
consumer=$(mktemp -d)
trap 'rm -rf "$consumer"' EXIT

"$root/gradlew" -p "$root" :kascii-core:publishToMavenLocal :kascii-compose:publishToMavenLocal \
  :kascii-components:publishToMavenLocal :kascii-gallery:publishToMavenLocal --quiet

mkdir -p "$consumer/src/commonMain/kotlin"
cat > "$consumer/settings.gradle.kts" <<'EOF'
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal { content { includeGroup("io.github.jantursky") } }
        google()
        mavenCentral()
    }
}

rootProject.name = "consumer"
EOF
cat > "$consumer/build.gradle.kts" <<EOF
@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    kotlin("multiplatform") version "$kotlin"
    id("org.jetbrains.kotlin.plugin.compose") version "$kotlin"
    id("org.jetbrains.compose") version "$compose"
}

kotlin {
    jvm()
    wasmJs { browser() }
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.jantursky:kascii-core:$version")
            implementation("io.github.jantursky:kascii-compose:$version")
            implementation("io.github.jantursky:kascii-components:$version")
            implementation("io.github.jantursky:kascii-gallery:$version")
        }
    }
}
EOF
# kascii-components and kascii-gallery have no public API to call yet; depending on them still makes Gradle resolve
# their POMs and variants.
cat > "$consumer/src/commonMain/kotlin/Consumer.kt" <<'EOF'
import androidx.compose.runtime.Composable
import com.jantursky.kascii.compose.AsciiCanvas
import com.jantursky.kascii.core.Charsets
import com.jantursky.kascii.core.Fields

@Composable
fun Screen() {
    AsciiCanvas(field = Fields.plasma(), charset = Charsets.Standard)
}
EOF

"$root/gradlew" -p "$consumer" compileKotlinJvm compileKotlinWasmJs --quiet
echo "Consumer compiled for jvm and wasmJs against io.github.jantursky:kascii-core, kascii-compose, kascii-components and kascii-gallery $version"
