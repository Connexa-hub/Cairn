plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.cairn.app"
    // 36 is AGP 8.9.2's max recommended compileSdk (37+ requires AGP 9.1+,
    // which we're deliberately not adopting yet — see Hilt version note below).
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cairn.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // CI (GitHub Actions) provides these via env vars decoded from secrets — see
        // .github/workflows/android-release.yml and RELEASE.md. Local debug builds
        // never need this block; only `assembleRelease`/`bundleRelease` read it.
        create("release") {
            val keystorePath = System.getenv("CAIRN_KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("CAIRN_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("CAIRN_KEY_ALIAS")
                keyPassword = System.getenv("CAIRN_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // A real release keystore (via CAIRN_KEYSTORE_* env vars, see RELEASE.md)
            // is used when present. Falling back to debug signing — rather than no
            // signing at all — keeps CI builds on forks/PRs actually installable for
            // testing; Android refuses to install a fully unsigned APK. Never ship
            // a debug-signed build to production/Play — only tag releases with real
            // secrets configured are meant to be distributed.
            signingConfig = if (System.getenv("CAIRN_KEYSTORE_PATH") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Compose compiler version/config now lives in the org.jetbrains.kotlin.plugin.compose
    // plugin (applied above) as of Kotlin 2.0+ — the old composeOptions {
    // kotlinCompilerExtensionVersion } mechanism is retired and no longer needed.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Deprecated (warns about Gradle 10 incompatibility) but proven working
    // across every CI run so far — not touching it while there's an active
    // build failure to fix. Worth migrating to the compilerOptions DSL in
    // its own isolated change later, once verifiable against a real build.
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources.excludes.add("META-INF/*")
    }
}

// Room's schema JSON export (exportSchema = true in CairnDatabase.kt) needs an
// explicit output directory for the KSP-based Room compiler — without this,
// KSP only warns (schema history just isn't written), but tracking real
// migration history matters for a decade-scale archive, so export it for real.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // Room + SQLCipher
    // Room 2.8.4 is current stable and the version that requires/targets Kotlin
    // 2.0+ with KSP2 support — pairs with the Kotlin 2.1.20/KSP 2.1.20-1.0.32
    // toolchain declared in the root build.gradle.kts.
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    implementation("androidx.room:room-paging:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    // net.zetetic:sqlcipher-android is the actively-maintained successor to the
    // deprecated net.zetetic:android-database-sqlcipher (which stopped at 4.5.4
    // and won't get 16KB-page-size support Google now requires for Play Store).
    // Uses SupportOpenHelperFactory (package net.zetetic.database.sqlcipher)
    // rather than the old SupportFactory — see CairnDatabase.kt.
    implementation("net.zetetic:sqlcipher-android:4.17.0")
    implementation("androidx.sqlite:sqlite:2.4.0")

    // Paging
    implementation("androidx.paging:paging-runtime-ktx:3.3.2")
    implementation("androidx.paging:paging-compose:3.3.2")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.57.2")
    ksp("com.google.dagger:hilt-android-compiler:2.57.2")
    // Pinned to 1.2.0 rather than latest — 1.4.0 hard-requires AGP 9.1.0+
    // (same class of gate as core Hilt's AGP-9 requirement, confirmed by CI)
    // and transitively drags in androidx.lifecycle 2.11.0, overriding our
    // explicit 2.8.4 pin above. 1.2.0 has no such gate and was never the
    // actual source of any real failure — it was bumped preemptively in an
    // earlier pass without cause. Revisit once AGP 9 is actually adopted.
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Biometric
    implementation("androidx.biometric:biometric:1.1.0")

    // Security crypto (Keystore-backed EncryptedFile / MasterKey)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Networking (backup upload/download only — no analytics)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Image loading (avatars — local only)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
