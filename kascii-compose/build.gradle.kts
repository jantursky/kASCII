import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.roborazzi)
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    explicitApi()

    // Desktop, iOS and web share Skia, so they share the runtime-effect shader painter.
    applyDefaultHierarchyTemplate {
        common {
            group("skiko") {
                withJvm()
                withWasmJs()
                group("ios") { withIos() }
            }
        }
    }

    android {
        namespace = "com.jantursky.kascii.compose"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        withHostTest { isIncludeAndroidResources = true }
    }

    jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.ui)
            api(libs.compose.animation)
            api(libs.compose.foundation)
            api(project(":kascii-core"))
            implementation(libs.compose.components.resources)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        getByName("androidHostTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.junit)
            implementation(libs.robolectric)
        }
        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(libs.roborazzi.compose.desktop)
            implementation(compose.desktop.currentOs)
        }
    }
}

tasks.withType<Test>().matching { it.name == "testAndroidHostTest" }.configureEach {
    // Robolectric's newest Android sandbox builds file descriptors through JDK internals that recent JDKs
    // no longer export.
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
}

tasks.named<Test>("jvmTest") {
    // Skiko loads its native library; recent JDKs warn unless native access is granted.
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

compose.resources {
    packageOfResClass = "com.jantursky.kascii.compose.resources"
}

roborazzi {
    outputDir.set(layout.projectDirectory.dir("src/jvmTest/screenshots"))
}

apiValidation {
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib { enabled = true }
}

kover {
    reports {
        filters {
            excludes {
                // Reduced-motion and font readers talk to the OS; they are exercised on devices, not in host tests.
                packages("com.jantursky.kascii.compose.platform")
                // The AGSL painter needs Android 13; it is exercised on a device, not in host tests.
                classes("*AgslShaderPainter*")
                // Generated resource accessors.
                packages("com.jantursky.kascii.compose.resources")
                // The Compose compiler gives every composable skip and default-argument branches that depend on how
                // the caller was compiled. UI tests drive the composables; the bound measures the logic behind them.
                annotatedBy("androidx.compose.runtime.Composable")
            }
        }
        verify {
            rule {
                bound { minValue = 90 }
                bound {
                    coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH
                    minValue = 81
                }
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    // Central only needs a javadoc jar to exist. An empty one keeps the upload small: the Dokka HTML was
    // ~700 KB per publication, most of a release against the monthly size and file limits.
    configure(KotlinMultiplatform(javadocJar = JavadocJar.Empty()))
    // CI passes the key as ORG_GRADLE_PROJECT_signingInMemoryKey; local builds publish unsigned.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    coordinates(artifactId = "kascii-compose")
    pom {
        name.set("Kascii Compose")
        description.set(
            "Compose Multiplatform renderers for Kascii: AsciiCanvas, Modifier.ascii, GPU, CPU and text rendering.",
        )
        inceptionYear.set("2026")
        url.set("https://github.com/jantursky/kascii")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("jantursky")
                name.set("Jan Tursky")
                email.set("jan.tursky@gmail.com")
                url.set("https://jantursky.com")
            }
        }
        scm {
            url.set("https://github.com/jantursky/kascii")
            connection.set("scm:git:https://github.com/jantursky/kascii.git")
            developerConnection.set("scm:git:ssh://git@github.com/jantursky/kascii.git")
        }
    }
}

// The wasm and iOS test runs cover commonTest only (pure math, no UI), so they don't need the Skiko
// runtime that this check asks an executable binary for.
tasks.named { it == "checkComposeUiTestConfigurationForWasmJs" }.configureEach { enabled = false }
