plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.quickened"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.quickened"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Physical 64-bit phones only: drop 32-bit ARM and x86 emulator ABIs.
            // Debug keeps all ABIs so the emulator still works.
            ndk {
                abiFilters += listOf("arm64-v8a")
            }
            val keystoreFile = rootProject.file("keystore/quickened-release.jks")
            if (keystoreFile.exists()) {
                val localSecrets = rootProject.file("local.properties").takeIf { it.exists() }
                    ?.readLines().orEmpty()
                    .mapNotNull { line ->
                        val parts = line.split("=").map { it.trim() }
                        if (parts.size == 2) parts[0] to parts[1] else null
                    }.toMap()
                signingConfigs {
                    create("release") {
                        storeFile = keystoreFile
                        storePassword = localSecrets["release.storePassword"]
                            ?: System.getenv("QUICKENED_STORE_PASSWORD")
                        keyAlias = "quickened"
                        keyPassword = localSecrets["release.keyPassword"]
                            ?: System.getenv("QUICKENED_KEY_PASSWORD")
                    }
                }
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
    }
    lint {
        checkReleaseBuilds = false
    }
}

dependencies {
    val roomVersion = "2.6.1"
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation(project(":pipertts"))
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("com.positiondev.epublib:epublib-core:3.1") {
        exclude(group = "xmlpull", module = "xmlpull")
    }

    debugImplementation("androidx.compose.ui:ui-tooling")
}
