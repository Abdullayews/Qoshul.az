# Qoshul.az native — R8 qaydaları (D-036)
# Compose öz qaydalarını gətirir; layihəyə xas əlavələr burada.

# M1: Supabase/ktor (D-068) — reflection əsaslı serializasiya və CIO engine.
# Kitabxanalar consumer qaydalar gətirir; bu əlavələr sığortadır (beta).
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class io.github.jan.tennert.supabase.** { *; }
-dontwarn io.github.jan.tennert.supabase.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# D-150: Credential Manager + Google ID (native sign-in). R8 playservices
# implementasiyasını kəsə bilər — rəsmi sənəd qaydası (supabase.com/docs
# auth-google; minSdk 24-də play-services fallback həmişə işləyir).
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
# GoogleIdTokenCredential Parcelable data reflection ilə oxunur:
-keep class com.google.android.libraries.identity.googleid.** { *; }
