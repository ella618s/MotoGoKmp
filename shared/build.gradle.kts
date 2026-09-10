import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("org.jetbrains.compose") version "1.6.11"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    // 建立 XCFramework 輸出目標
    val xcf = XCFramework("shared")

    iosX64 {
        binaries.framework {
            baseName = "shared"
            isStatic = true
            xcf.add(this)
        }
        // 🎯 明確指定此 target 的最低 iOS 版本
        compilations.all {
            kotlinOptions.freeCompilerArgs += "-Xbinary=minimumOSVersion=15.0"
        }
    }
    iosArm64 {
        binaries.framework {
            baseName = "shared"
            isStatic = true
            xcf.add(this)
        }
        compilations.all {
            kotlinOptions.freeCompilerArgs += "-Xbinary=minimumOSVersion=15.0"
        }
    }
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "shared"
            isStatic = true
            xcf.add(this)
        }
        compilations.all {
            kotlinOptions.freeCompilerArgs += "-Xbinary=minimumOSVersion=15.0"
        }
    }

    sourceSets {
        commonMain.dependencies {
            // 恢復 Compose 核心元件支援
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)

            // 網路與序列化
            implementation("io.ktor:ktor-client-core:2.3.12")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
        }
        androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:2.3.12")
            implementation("com.google.android.gms:play-services-location:21.3.0")
            // osmdroid 依賴
            implementation("org.osmdroid:osmdroid-android:6.1.18")
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:2.3.12")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
        }
    }
}

android {
    namespace = "com.example.motogokmp"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
