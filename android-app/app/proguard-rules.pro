# Qoshul.az native — R8 qaydaları (D-036)
# Compose öz qaydalarını gətirir; layihəyə xas əlavələr burada.

# D-163 („Serializer for class 'z35' is not found“): köhnə qayda
# io.github.jan.TENNERT.supabase paketini saxlamağa çalışırdı — bu yalnız
# MAVEN qrup ID-sidir (io.github.jan-tennert.supabase); Java paketi
# io.github.jan.supabase-dir. Qayda boş idi → R8 supabase-kt siniflərini
# kəsib z35 kimi obfuskasiya etdi → SettingsSessionManager UserSession
# serializerini tapa bilmədi. Bu həm də 0.55.4-də birbaşa GoTrue yolunun
# niyə uğursuz olub m4 fallback-a düşdüyünü izah edir.
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**
-dontwarn io.github.jan.supabase.logging.KermitSupabaseLogger
-dontwarn io.github.jan.supabase.logging.**

# kotlinx.serialization rəsmi R8 qaydaları (github.com/Kotlin/kotlinx.serialization
# guide-safety.md#rules-for-r8): serializer() companion metodları saxlanılır.
# R8-in built-in serialization support-u tam deyil — serializerlookup
# reflection üçün bu qaydalar MÜTLƏQDİR (z35 xətasının 2-ci tərəfi).
-keepattributes *Annotation*, InnerClasses, Signature
-dontnote kotlinx.serialization.**
-keepclasseswithmembers class kotlinx.serialization.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class **$$serializer { *** INSTANCE; }
-dontwarn kotlinx.serialization.**

# ktor: CIO engine + serialization məzmunu (D-068-dən gəlir, sığorta).
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# multiplatform-settings (SettingsSessionManager-ın storage-ı):
-keep class com.russhwolf.settings.** { *; }
-dontwarn com.russhwolf.settings.**

# D-150: Credential Manager + Google ID (native sign-in). R8 playservices
# implementasiyasını kəsə bilər — rəsmi sənəd qaydası (supabase.com/docs
# auth-google; minSdk 24-də play-services fallback həmişə işləyir).
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
# GoogleIdTokenCredential Parcelable data reflection ilə oxunur:
-keep class com.google.android.libraries.identity.googleid.** { *; }
