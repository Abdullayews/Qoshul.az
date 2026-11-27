package az.qoshul.app.backend

import az.qoshul.app.core.backend.SupabaseSync
import az.qoshul.app.core.backend.SyncEvent
import az.qoshul.app.core.data.FakeQoshulDataSource
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.model.Partner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * D-177/D-178 (ikinci dalğa): venue şəkli və rəy testləri.
 *  • Partner.imageUrl nullable (image_url tam URL; NULL → kateqoriya placeholder)
 *  • fallback `image ?: "friends"` SİLİNDİ — kateqoriya açarına düşür
 *  • rəy UI → fn_add_venue_review; bir məkana bir rəy
 *  • readVenueSlots boş → RezerveSheet fallback saat siyahısı YOXDUR
 */
class VenueImageTest {

    private fun engine() = AppEngine(dataSource = FakeQoshulDataSource)

    @Test fun `Partner imageUrl nullable default null`() {
        val p = Partner(
            id = "vX", name = "X", category = "İdman", address = "", city = "Bakı",
            hours = "", image = "padel", lat = 0.0, lng = 0.0,
        )
        assertEquals(null, p.imageUrl)
    }

    @Test fun `kateqoriya açarı fallback friends sabiti deyil`() {
        // D-177: Taxonomy.imageKeyForCategory — qrupa görə; „friends“ yalnız
        // qeyri-uyğun qruplarda (Səyahət və digər) görünür
        val t = az.qoshul.app.core.data.Taxonomy
        assertEquals("padel", t.imageKeyForCategory("Futbol"))       // İdman
        assertEquals("coffee", t.imageKeyForCategory("Qəhvə"))       // Yemək və içki
        // Kino "Mədəniyyət" qrupundadır → FRIENDS (tech YOX)
        assertEquals("friends", t.imageKeyForCategory("Kino"))
        assertEquals("friends", t.imageKeyForCategory("Naməlum"))    // fallback qrupu
    }

    @Test fun `reader-də sabit friends fallback silinib (grep-paritet)`() {
        val src = java.io.File(
            "src/main/java/az/qoshul/app/core/backend/SupabaseReader.kt",
        ).readText()
        assertFalse(src.contains("""?: "friends""""))
    }

    @Test fun `AddVenueReviewIntent fn_add_venue_review RPC-sinə xəritələnir`() {
        val call = SupabaseSync.mapEvent(
            SyncEvent.AddVenueReviewIntent("v1", 5, "Əla meydanlar"),
        )
        require(call != null)
        assertEquals("fn_add_venue_review", call.fn)
        assertTrue(call.params.containsKey("p_venue_id"))
        assertTrue(call.params.containsKey("p_stars"))
        assertTrue(call.params.containsKey("p_body"))
    }

    @Test fun `addVenueReview validasiya və tək-rəy qaydası`() {
        val e = engine()
        // ulduz aralığı xarici → rədd
        assertFalse(e.addVenueReview("v1", 0, "x"))
        assertFalse(e.addVenueReview("v1", 6, "x"))
        // birinci rəy → qəbul, ikinci → rədd (server myVenueReviews-dən gəlir)
        assertTrue(e.addVenueReview("v1", 4, "Yaxşı"))
        assertFalse(e.addVenueReview("v1", 5, "İkinci"))
        assertTrue(e.venueReviewedByMe("v1"))
        assertFalse(e.venueReviewedByMe("v2"))
    }

    @Test fun `slot siyahısı boş olanda fallback siyahı YOXDUR (engine səviyyəsi)`() {
        val e = engine()
        // ServerDataSource boş slot xəritəsi ilə → venueSlots BOŞ qayıdır
        // (əvvəl uydurma 09:00-21:00 toru yox idi, UI-da da EmptyState var)
        val slots = e.venueSlots("naməlum-venue")
        assertTrue(slots.isEmpty())
    }

    @Test fun `0012 migration venues image_url əlavə edir (SQL mətn paritesi)`() {
        // CI working dir = android-app/app → repo kökündən axtarış
        // (lokal repo kökündən da işləyir — hər ikisindən yoxlayırıq)
        val sql = sequenceOf(
            java.io.File("supabase/migrations/0012_second_wave_server_authority.sql"),
            java.io.File("../../supabase/migrations/0012_second_wave_server_authority.sql"),
            java.io.File("../../../supabase/migrations/0012_second_wave_server_authority.sql"),
        ).firstOrNull { it.exists() }?.readText()
            ?: error("0012 migration faylı tapılmadı (repo kökünü axtar)")
        assertTrue(sql.contains("add column if not exists image_url text"))
        assertTrue(sql.contains("fn_assistant_reply"))
        assertTrue(sql.contains("fn_recommend_events"))
        assertTrue(sql.contains("fn_request_friend"))
        assertTrue(sql.contains("fn_respond_friend"))
        assertTrue(sql.contains("fn_add_venue_review"))
        assertTrue(sql.contains("fn_read_swipes"))
    }

    @Test fun `0013 migration login-profil bərpası + xəritə koordinatı (SQL mətn paritesi)`() {
        val sql = sequenceOf(
            java.io.File("supabase/migrations/0013_login_profile_recovery.sql"),
            java.io.File("../../supabase/migrations/0013_login_profile_recovery.sql"),
            java.io.File("../../../supabase/migrations/0013_login_profile_recovery.sql"),
        ).firstOrNull { it.exists() }?.readText()
            ?: error("0013 migration faylı tapılmadı (repo kökünü axtar)")
        // D-180 (1): trigger INSERT+UPDATE — login-də profil bərpası
        assertTrue(sql.contains("after insert or update of raw_user_meta_data, email on auth.users"))
        // D-180 (1): ad boşdursa metadata-dan doldurulur (klient win qorunur)
        assertTrue(sql.contains("case when btrim(first_name) = ''"))
        // D-180 (3): zədələnmiş userlər üçün birdəfəlik bərpa (no-op UPDATE)
        assertTrue(sql.contains("update auth.users set email = email"))
        // D-180 (4): profile_cards view — yuvarlaqlaşdırılmış koordinat
        assertTrue(sql.contains("round(st_y(p.last_location::geometry)::numeric, 2)"))
        assertTrue(sql.contains("round(st_x(p.last_location::geometry)::numeric, 2)"))
        // privacy: OFF → null; ROL I (dəqiq koordinat YOX) qorunur
        assertTrue(sql.contains("when p.map_visibility = 'OFF' or p.last_location is null then null"))
        // seed idempotent
        assertTrue(sql.contains("on conflict do nothing"))
    }

    @Test fun `0014 migration maraq FK guard (SQL mətn paritesi)`() {
        val sql = sequenceOf(
            java.io.File("supabase/migrations/0014_interests_fk_guard.sql"),
            java.io.File("../../supabase/migrations/0014_interests_fk_guard.sql"),
            java.io.File("../../../supabase/migrations/0014_interests_fk_guard.sql"),
        ).firstOrNull { it.exists() }?.readText()
            ?: error("0014 migration faylı tapılmadı (repo kökünü axtar)")
        // D-181: insert yalnız categories-də MÖVCUD slüqlarla (join filtri)
        assertTrue(sql.contains("join public.categories c on c.slug = t.slug"))
        // zərərli tarixi qalıqların təmizliyi
        assertTrue(sql.contains("delete from public.user_interests ui"))
        // köhnə APK-lardan gələn zərərli slüq artıq əməliyyatı partlatmır
        assertTrue(sql.contains("on conflict do nothing"))
    }
}

}

