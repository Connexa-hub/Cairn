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
    id("com.google.dagger.hilt.android") version "2.59.2" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20" apply false
    id("com.google.devtools.ksp") version "2.1.20-1.0.32" apply false
}
