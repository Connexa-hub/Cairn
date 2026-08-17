plugins {
    // Version set chosen as a mutually-compatible whole, not independently:
    // Kotlin 2.1.20 is what Room 2.8.4 requires (Room 2.7+ targets Kotlin
    // 2.0+ and recommends KSP2); KSP 2.1.20 matches Kotlin's own version
    // exactly (KSP's versioning scheme now tracks Kotlin 1:1 as of the
    // 2.1.x line, replacing the old "<kotlin>-<ksp-patch>" suffix format);
    // AGP 8.9.2 and Gradle 9.7 (see gradle-wrapper.properties) are the
    // pairing GitHub's runners actually resolve to.
    id("com.android.application") version "8.9.2" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
    // Compose compiler moved into the Kotlin repo at Kotlin 2.0+ and is now
    // configured via this dedicated plugin instead of the old
    // composeOptions { kotlinCompilerExtensionVersion } mechanism.
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20" apply false
    // Hilt is pinned to 2.57.2 rather than the latest — 2.59.x introduced a
    // hard "requires AGP 9.0.0+" gate in the Gradle plugin (confirmed via
    // dagger/dagger#5099, #5083: even Hilt's own AGP 9 support was broken
    // across 2.58/2.59). AGP 9 is itself a large, still-stabilizing breaking
    // migration (built-in Kotlin, new variant APIs) — not worth adopting
    // just to satisfy Hilt's gate. 2.57.2 predates that requirement and
    // works cleanly with AGP 8.x.
    id("com.google.dagger.hilt.android") version "2.57.2" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20" apply false
    id("com.google.devtools.ksp") version "2.1.20-1.0.32" apply false
}
