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
