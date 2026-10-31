// Qoshul.az native (Kotlin + Jetpack Compose) — D-036.
// Kök build faylı; versiyalar gradle/libs.versions.toml-də mərkəzləşib.
plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9+: Kotlin daxili dəstək (built-in) — kotlin.android plugin silinib
    alias(libs.plugins.kotlin.compose) apply false
}
