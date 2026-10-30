plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "az.qoshul.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "az.qoshul.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "0.8.0"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            // Beta mərhələsi: debug imzası ilə birbaşa quraşdırma (D-016 keystore mərhələsi gələcəkdə)
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            // MapLibre nüvə .so ~13.4 MB/ABI (D-048): release-da real telefon ABI-ları;
            // x86_64 (yalnız emulator) buraxılır — debug variantında qalır.
            // Play-də app bundle per-device çatdırır; birbaşa APK üçün universal phone build.
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true // java.time minSdk 24-də (LocalDate və s.)
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            // Robolectric: Compose UI testləri JVM-də (widget-test pariteti, D-042)
            isIncludeAndroidResources = true
        }
    }
    // Robolectric 4.17 + JDK 21: SharedSecrets/FileDescriptor daxili API-lərinə
    // açıq giriş tələb olunur (əks halda FileDescriptorInterceptor IllegalAccessException)
    testOptions.unitTests.all { test ->
        test.jvmArgs(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.io=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
            "--add-opens=java.base/java.text=ALL-UNNAMED",
            "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
            "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
        )
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Kotlin 2.4: köhnə kotlinOptions DSL silinib — compilerOptions istifadə olunur
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    // Baseline profile quraşdırıcısı — API 24-27-də profile install üçün (D-043);
    // transitive 1.4.0 gəlirdi, açıq pin 1.4.1 (son stable)
    implementation(libs.androidx.profileinstaller)

    // Vektor xəritə — MapLibre Compose 0.20 (D-048): OpenFreeMap stili, açarsız;
    // FFI nüvə .so ~1 MB/ABI, OpenGL backend (Vulkan runtime-a ehtiyac yoxdur)
    implementation(libs.maplibre.compose.android)
    runtimeOnly(libs.maplibre.compose.runtime.opengl.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // QR sübut testi: generasiya olunan kodu ZXing ilə dekodlaşdırırıq (D-041)
    testImplementation(libs.zxing.core)
    // Compose UI testləri JVM-də (Robolectric) — Flutter widget-test pariteti (D-042)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
