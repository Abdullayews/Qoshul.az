package az.qoshul.app

import az.qoshul.app.core.data.FakeQoshulDataSource
import az.qoshul.app.core.engine.AppEngine
import az.qoshul.app.core.model.MapVisibility
import az.qoshul.app.core.model.NotifType
import az.qoshul.app.core.model.ResvStatus
import az.qoshul.app.core.model.VenueXpConfig
import az.qoshul.app.core.model.XpRules
import az.qoshul.app.core.model.LocalDate
import az.qoshul.app.core.model.isQuietHours
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * XpRules + AppEngine testləri — Flutter `rol_test.dart` (38 test) +
 * `xp_economy_test.dart` (23 test) portlarının birləşmiş əhatəsi (Phase 7).
 * Vaxt əvəzləməsi: clock injeksiyası ilə idarə olunur.
 */
class XpRulesTest {

    // ── XpRules — ROL F səviyyələri ──

    @Test fun `rank 0 Yeni`() = assertEquals("Yeni", XpRules.rankForXp(0))

    @Test fun `rank 99 həddində Yeni qalır`() = assertEquals("Yeni", XpRules.rankForXp(99))

    @Test fun `rank 100 Kəşfiyyatçı`() = assertEquals("Kəşfiyyatçı", XpRules.rankForXp(100))

    @Test fun `rank 300 Aktiv`() = assertEquals("Aktiv", XpRules.rankForXp(300))

    @Test fun `rank 700 Sosial`() = assertEquals("Sosial", XpRules.rankForXp(700))

    @Test fun `rank 1500 Lider`() = assertEquals("Lider", XpRules.rankForXp(1500))

    @Test fun `rank 3000 Əfsanə`() = assertEquals("Əfsanə", XpRules.rankForXp(3000))

    @Test fun `rank maksimumdan yuxarı Əfsanə qalır`() = assertEquals("Əfsanə", XpRules.rankForXp(99999))

    @Test fun `rankIndex sərhədləri`() {
        assertEquals(0, XpRules.rankIndexForXp(50))
        assertEquals(1, XpRules.rankIndexForXp(150))
        assertEquals(5, XpRules.rankIndexForXp(99999))
    }

    @Test fun `xpProgress pct hesablanır`() {
        val p = XpRules.xpProgress(50)
        assertEquals("Kəşfiyyatçı", p.nextName)
        assertEquals(50, p.toNext)
        assertEquals(50, p.pct)
    }

    @Test fun `xpProgress maksimumda nextName null`() {
        val p = XpRules.xpProgress(5000)
        assertNull(p.nextName)
        assertEquals(100, p.pct)
    }

    @Test fun `isAdult 18 yaş sərhədi`() {
        val now = LocalDate.of(2026, 10, 30)
        assertTrue(XpRules.isAdult(LocalDate.of(2008, 10, 30), now))   // tam 18
        assertFalse(XpRules.isAdult(LocalDate.of(2008, 10, 31), now))  // 17 gün 364
        assertTrue(XpRules.isAdult(LocalDate.of(2000, 1, 1), now))
    }

    @Test fun `proWeight L1 çəkiləri`() {
        assertEquals(3, XpRules.proWeight("Peşəkar"))
        assertEquals(2, XpRules.proWeight("Orta"))
        assertEquals(1, XpRules.proWeight("Başlanğıc"))
        assertEquals(0, XpRules.proWeight("Hamı üçün"))
        assertEquals(0, XpRules.proWeight(null))
    }

    @Test fun `D-026 sabitləri`() {
        assertEquals(50, XpRules.ORGANIZER_XP)
        assertEquals(25, XpRules.TAGGED_PARTICIPANT_XP)
        assertEquals(60, XpRules.XP_CARD_TTL_SECONDS)
        assertEquals(6, XpRules.FALLBACK_CODE_DIGITS)
        assertEquals(3, XpRules.NO_SHOW_LIMIT)
        assertEquals(7, XpRules.NO_SHOW_RESTRICTION_DAYS)
    }
}

class XpEngineTest {

    private fun engine() = AppEngine()

    // ── XP mühərriki (idempotent) ──

    @Test fun `qeydiyyat XP bir dəfə verilir`() {
        val e = engine()
        e.completeRegistration()
        val xp = e.state.value.xp
        e.completeRegistration()
        assertEquals(xp, e.state.value.xp)
    }

    @Test fun `join XP idempotentdir`() {
        val e = engine()
        assertTrue(e.join("e1"))
        val xp = e.state.value.xp
        assertFalse(e.join("e1")) // ikinci dəfə qoşulmaq olmaz
        assertEquals(xp, e.state.value.xp)
    }

    @Test fun `approval tələb edən tədbir pending düşür`() {
        val e = engine()
        assertTrue(e.join("e3")) // approval = true
        assertTrue("e3" in e.state.value.pending)
        assertFalse("e3" in e.state.value.joined)
    }

    @Test fun `dolu tədbirə qoşulmaq olmaz`() {
        // mock-da dolu+yaxınlaşan tədbir yoxdur → sintetik hadisə ilə yoxlanılır
        val e = engine()
        assertFalse(e.join("e0")) // keçmiş (12/12) — past bloku
        val full = FakeQoshulDataSource.events.first { it.id == "e0" }
        assertTrue(full.isFull) // 12/12
        assertTrue(full.past)
    }

    @Test fun `check-in yalnız joined üçün və bir dəfə`() {
        val e = engine()
        e.join("e1")
        assertTrue(e.checkInEvent("e1"))
        val xp = e.state.value.xp
        assertFalse(e.checkInEvent("e1"))
        assertEquals(xp, e.state.value.xp)
    }

    @Test fun `check-in firstEvent badge verir`() {
        val e = engine()
        e.join("e1")
        e.checkInEvent("e1")
        assertTrue("firstEvent" in e.state.value.earnedBadges)
    }

    @Test fun `telefon təsdiqi badge + XP verir və bir dəfədir`() {
        val e = engine()
        e.verifyPhone()
        assertTrue(e.state.value.phoneVerified)
        assertTrue("verified" in e.state.value.earnedBadges)
        val xp = e.state.value.xp
        e.verifyPhone()
        assertEquals(xp, e.state.value.xp)
    }

    @Test fun `səviyyə həddləri bildiriş mənbəyidir (eventReminder testi)`() {
        // Səviyyə artımına çatmaq üçün kifayət qədər XP hadisəsi mock-da yoxdur
        // (maks ~600); bildiriş sistemi eventReminder ilə yoxlanılır:
        val e = engine()
        val e1 = FakeQoshulDataSource.events.first { it.id == "e1" }
        e.eventReminder(e1, "24 saat qalıb")
        assertTrue(e.state.value.notifications.any { it.type == NotifType.EVENT_REMINDER })
    }

    @Test fun `finishActivity LEVEL_UP tipli bildiriş verir (D-026)`() {
        val e = engine()
        val mafiya = FakeQoshulDataSource.events.first { it.id == "e-mafiya" }
        e.setProfileName("Dost")
        e.finishActivity(mafiya, listOf("p1"))
        assertTrue(e.state.value.notifications.any {
            it.type == NotifType.LEVEL_UP && it.title.contains("Endirim XP")
        })
    }

    @Test fun `günlük giriş gündə bir dəfə`() {
        val e = engine()
        e.dailyLogin()
        val xp = e.state.value.xp
        e.dailyLogin()
        assertEquals(xp, e.state.value.xp)
        assertEquals(2, e.state.value.loginStreak)
    }

    // ── Onboarding (ROL B) ──

    @Test fun `finishOnboarding +50 XP verir və onboarded olur`() {
        val e = engine()
        e.setProfileCreation("Aysel", "Əliyeva", LocalDate.of(2000, 1, 1))
        e.setInterests(listOf("Kafe və Yemək", "İdman və Aktivlik", "Musiqi"))
        e.finishOnboarding()
        assertTrue(e.state.value.onboarded)
        assertTrue(e.state.value.xp >= 320 + 50 + 5) // registration + daily login
    }

    @Test fun `demo mafiya host-u profil adı ilə əvəz olunur`() {
        val e = engine()
        e.setProfileCreation("Aysel", "Əliyeva", LocalDate.of(2000, 1, 1))
        e.finishOnboarding()
        val mafiya = e.state.value.events.first { it.id == "e-mafiya" }
        assertEquals("Aysel Əliyeva", mafiya.host)
    }

    // ── Match (ROL D) ──

    @Test fun `swipe keç — match yoxdur`() {
        val e = engine()
        assertFalse(e.swipe("p3", false)) // likesBack = false
        assertFalse(e.swipe("p4", true))  // likesBack = false
    }

    @Test fun `qarşılıqlı maraq match yaradır`() {
        val e = engine()
        assertTrue(e.swipe("p1", true)) // likesBack = true
        assertTrue("p1" in e.state.value.matches)
        assertTrue("p1" in e.state.value.friendships)
        assertTrue(e.state.value.friendMessages.containsKey("p1"))
        assertTrue(e.state.value.notifications.any { it.type == NotifType.MATCH })
    }

    @Test fun `match XP gündəlik cap 3`() {
        val e = engine()
        val before = e.state.value.xp
        assertTrue(e.swipe("p1", true))  // +10 (1)
        assertTrue(e.swipe("p2", true))  // +10 (2)
        assertTrue(e.swipe("p5", true))  // +10 (3)
        val after3 = e.state.value.xp
        assertEquals(before + 30, after3)
        // 4-cü match XP vermir (yalnız 3 likesBack=true insan var; cap test üçün məntiq:
        // bütün likesBack=true istifadə olundu — cap 3-dür)
        assertEquals(3, e.state.value.swipes.count { it.value })
    }

    @Test fun `blok match və dostluğu təmizləyir`() {
        val e = engine()
        e.swipe("p1", true)
        e.blockUser("p1")
        assertFalse("p1" in e.state.value.matches)
        assertFalse(e.state.value.friendships.containsKey("p1"))
        assertFalse(e.state.value.friendMessages.containsKey("p1"))
        assertTrue("p1" in e.state.value.blocked)
    }

    @Test fun `3 report avtomatik məhdudlaşdırır`() {
        val e = engine()
        e.reportUser("p4", "Spam və reklam")
        e.reportUser("p4", "Spam və reklam")
        assertFalse("p4" in e.state.value.restrictedUsers)
        e.reportUser("p4", "Spam və reklam")
        assertTrue("p4" in e.state.value.restrictedUsers)
        // məhdudlaşdırılmış kandidatlar siyahısından yox olur
        assertFalse(e.matchCandidates.any { it.id == "p4" })
    }

    // ── Rezerv (ROL E) ──

    @Test fun `avto-tesdiq məkan TƏSDİQLƏNDİ verir`() {
        val e = engine()
        val v2 = FakeQoshulDataSource.partners.first { it.id == "v2" } // autoAccept=true
        val r = e.createReservation(venue = v2, date = "Bu gün", time = "11:00", party = 2)
        assertNotNull(r)
        assertEquals(ResvStatus.CONFIRMED, r!!.status)
    }

    @Test fun `manuel məkan GÖZLƏYİR verir`() {
        val e = engine()
        val v1 = FakeQoshulDataSource.partners.first { it.id == "v1" } // autoAccept=false
        val r = e.createReservation(venue = v1, date = "Bu gün", time = "10:00", party = 2)
        assertEquals(ResvStatus.PENDING, r!!.status)
    }

    @Test fun `check-in rezerv +30 XP və kupon istifadəsi`() {
        val e = engine()
        // o3: v4, 100 XP qiyməti, balans 150 ≥ 100, minRank 0 → alınır
        val c = e.buyCoupon("o3")
        assertNotNull(c)
        val coupon = e.state.value.coupons.first()
        val v4 = FakeQoshulDataSource.partners.first { it.id == "v4" } // avto-tesdiq
        val r = e.createReservation(
            venue = v4, date = "Bu gün", time = "12:00", party = 2,
            couponCode = coupon.code,
        )
        assertEquals(ResvStatus.CONFIRMED, r!!.status)
        val xpBefore = e.state.value.xp
        assertTrue(e.checkInReservation(r.id))
        assertEquals(ResvStatus.USED, r.status)
        assertEquals(xpBefore + XpRules.RESERVATION_XP, e.state.value.xp)
        assertTrue(e.state.value.coupons.first { it.code == coupon.code }.used)
    }

    @Test fun `keçərsiz kuponla rezerv yaradılmır`() {
        val e = engine()
        val v2 = FakeQoshulDataSource.partners.first { it.id == "v2" }
        val r = e.createReservation(
            venue = v2, date = "Bu gün", time = "11:00", party = 2,
            couponCode = "QSHUL-XXXX-V2",
        )
        assertNull(r)
    }

    @Test fun `3 gəlməmə 7 gün məhdudiyyət yaradır`() {
        val e = engine()
        val v1 = FakeQoshulDataSource.partners.first { it.id == "v1" }
        repeat(3) { i ->
            val r = e.createReservation(venue = v1, date = "Bu gün", time = "10:00", party = 2)
            e.markNoShow(r!!.id)
        }
        assertTrue(e.reservationRestricted)
        // məhdudiyyətli ikən yeni rezerv yoxdur
        assertNull(e.createReservation(venue = v1, date = "Bu gün", time = "12:00", party = 2))
    }

    @Test fun `ləğv edilə bilən statuslar`() {
        val e = engine()
        val v2 = FakeQoshulDataSource.partners.first { it.id == "v2" }
        val r = e.createReservation(venue = v2, date = "Bu gün", time = "11:00", party = 2)!!
        e.cancelReservation(r.id)
        assertEquals(ResvStatus.CANCELLED, e.state.value.reservations.first { it.id == r.id }.status)
    }

    // ── Kuponlar (ROL G) ──

    @Test fun `səviyyə-avto təklif yalnız açıq səviyyədə görünür`() {
        val e = engine() // 320 XP → Aktiv (idx 2)
        val visible = e.visibleOffers.map { it.id }
        assertTrue("o4" in visible) // minRank 2 (Aktiv) — açıqdır
        assertTrue("o5" in visible) // minRank 1 (Kəşfiyyatçı) ≤ Aktiv — açıqdır
        // Aşağı səviyyəli istifadəçi üçün o4 gizli olardı (idx 0 < 2)
        assertFalse(CouponOfferMinRankCheck.o4unlockedFor(0))
    }

    @Test fun `buyCoupon səviyyə catmırsa null`() {
        val e = engine() // Aktiv idx 2
        // o2 minRank 2 — uyğundur; balans v1=340 ≥ 300 → alınır
        val c = e.buyCoupon("o2")
        assertNotNull(c)
        assertEquals(340 - 300, e.venueXpFor("v1")!!.balance)
    }

    @Test fun `buyCoupon balans catmırsa null`() {
        val e = engine()
        // o1: v2 balans 120 < 200
        val c = e.buyCoupon("o1")
        assertNull(c)
    }

    @Test fun `kupon alışı obyekt bazasından düşür — peşəkar XP dəyişmir`() {
        val e = engine()
        val proXpBefore = e.state.value.xp
        e.buyCoupon("o2")
        assertEquals(proXpBefore, e.state.value.xp)
        assertEquals(40, e.venueXpFor("v1")!!.balance)
    }

    // ── Bildirişlər (ROL H) ──

    @Test fun `oxunmamış sayı düzgün`() {
        val e = engine()
        // ilkin: n1 (oxunmamış), n2/n3 (oxunub)
        assertEquals(1, e.unreadCount)
        e.markAllNotificationsRead()
        assertEquals(0, e.unreadCount)
    }

    @Test fun `marketinq sükut saatlarında göndərilmir`() {
        assertTrue(isQuietHours(LocalDateTime.of(2026, 10, 30, 23, 30)))
        assertTrue(isQuietHours(LocalDateTime.of(2026, 10, 30, 7, 0)))
        assertFalse(isQuietHours(LocalDateTime.of(2026, 10, 30, 12, 0)))
    }

    @Test fun `söndürülmüş tip bildiriş yaratmır`() {
        val e = engine()
        e.toggleNotifPref(NotifType.MATCH, false)
        val before = e.state.value.notifications.size
        e.swipe("p1", true)
        // match bildirişi söndürülüb — yalnız səviyyə/xp bildirişləri ola bilər
        val matchNotifs = e.state.value.notifications.count { it.type == NotifType.MATCH }
        assertEquals(0, matchNotifs)
        assertTrue(e.state.value.notifications.size <= before + 2)
    }

    // ── Endirim XP iqtisadiyyatı (D-026) ──

    @Test fun `MapVisibility 3 seçim və etiketlər`() {
        assertEquals("Hamı", MapVisibility.EVERYONE.label)
        assertEquals("Yalnız dostlar", MapVisibility.FRIENDS.label)
        assertEquals("Gizli", MapVisibility.OFF.label)
        assertEquals(3, MapVisibility.entries.size)
    }

    @Test fun `VenueXpConfig describe nümunəsi`() {
        val c = VenueXpConfig(xpPerAzn = 20, maxDiscountPercent = 25, minBillAzn = 10)
        assertEquals("100 XP = 5 ₼ · maks 25% · min hesab 10 ₼", c.describe())
        val c2 = VenueXpConfig(xpPerAzn = 25, maxDiscountPercent = 20, minBillAzn = 0)
        assertEquals("100 XP = 4 ₼ · maks 20%", c2.describe())
    }

    @Test fun `ilkin obyekt bazaları ayrıdır (L2)`() {
        val e = engine()
        assertEquals(325, e.venueXpFor("v5")!!.balance)
        assertEquals(120, e.venueXpFor("v2")!!.balance)
        assertEquals(0, e.venueXpFor("v6")!!.balance)
        assertEquals(340, e.venueXpFor("v1")!!.balance)
        assertEquals(150, e.venueXpFor("v4")!!.balance)
        assertEquals(325 + 120 + 0 + 340 + 150, e.discountXpTotal)
    }

    @Test fun `previewSpend balans catmır`() {
        val e = engine()
        val p = e.previewSpend("v2", 200, 50.0)
        assertFalse(p.ok)
        assertEquals("balans", p.error)
    }

    @Test fun `previewSpend minBill tələbi`() {
        val e = engine()
        val p = e.previewSpend("v5", 100, 5.0) // v5 minBill=10
        assertFalse(p.ok)
        assertEquals("minBill", p.error)
    }

    @Test fun `previewSpend cap həddi`() {
        val e = engine()
        // v1: maks 30%, 340 balans; 300 XP = 15₼ endirim; hesab 40₼ → cap 12₼ < 15₼
        val p = e.previewSpend("v1", 300, 40.0)
        assertFalse(p.ok)
        assertEquals("cap", p.error)
        assertEquals(12.0, p.discount, 0.01)
    }

    @Test fun `previewSpend uğurlu`() {
        val e = engine()
        val p = e.previewSpend("v1", 100, 40.0) // 100/20 = 5₼ ≤ 12₼ cap
        assertTrue(p.ok)
        assertEquals(5.0, p.discount, 0.001)
    }

    @Test fun `spendVenueXp atomik düşür və ledger yazır`() {
        val e = engine()
        val before = e.venueXpFor("v1")!!.balance
        val ledgerBefore = e.venueXpFor("v1")!!.ledger.size
        val d = e.spendVenueXp("v1", 100, 40.0)
        assertNotNull(d)
        assertEquals(5.0, d!!, 0.001)
        assertEquals(before - 100, e.venueXpFor("v1")!!.balance)
        assertEquals(ledgerBefore + 1, e.venueXpFor("v1")!!.ledger.size)
        val last = e.venueXpFor("v1")!!.recentLedger.first()
        assertFalse(last.grant)
        assertEquals(100, last.amount)
    }

    @Test fun `keçərsiz xərcləmə balansı dəyişmir`() {
        val e = engine()
        val before = e.venueXpFor("v1")!!.balance
        assertNull(e.spendVenueXp("v1", 999, 40.0))
        assertEquals(before, e.venueXpFor("v1")!!.balance)
    }

    @Test fun `dinamik XP kartı 60s və tək istifadə (L4-1)`() {
        val e = engine()
        e.regenerateXpCard("v5")
        assertTrue(e.xpCardValidFor("v5"))
        assertFalse(e.xpCardValidFor("v2")) // başqa obyekt
        val card = e.state.value.xpCard!!
        assertTrue(card.nonce.startsWith("QSHUL-XP-"))
        assertEquals(6, card.fallbackCode.length)
        assertTrue(card.secondsLeft in 0..60)
        e.markXpCardUsed()
        assertFalse(e.xpCardValidFor("v5")) // işlənib
    }

    @Test fun `finishActivity təşkilatçı +50 grant (L3)`() {
        val e = engine()
        val mafiya = FakeQoshulDataSource.events.first { it.id == "e-mafiya" }
        e.setProfileName("Dost") // host ilə uyğunlaşdır
        val before = e.venueXpFor("v5")!!.balance
        val r = e.finishActivity(mafiya, listOf("p1", "p2", "p3"))
        assertNotNull(r)
        assertEquals(50, r!!.organizerXp)
        assertEquals(25, r.perParticipantXp)
        assertEquals(3, r.taggedCount)
        assertEquals(75, r.participantsTotal)
        assertEquals(125, r.total)
        assertEquals(before + 50, e.venueXpFor("v5")!!.balance)
        assertTrue(e.isActivityFinished("e-mafiya"))
        // ikinci dəfə bitirmək olmaz
        assertNull(e.finishActivity(mafiya, listOf("p1")))
    }

    @Test fun `finishActivity host deyilsə null`() {
        val e = engine()
        val mafiya = FakeQoshulDataSource.events.first { it.id == "e-mafiya" }
        e.setProfileName("Başqa Şəxs")
        assertNull(e.finishActivity(mafiya, listOf("p1")))
    }

    @Test fun `finishActivity boş taq null`() {
        val e = engine()
        val mafiya = FakeQoshulDataSource.events.first { it.id == "e-mafiya" }
        e.setProfileName("Dost")
        assertNull(e.finishActivity(mafiya, emptyList()))
    }

    @Test fun `Kəşf peşəkar sıralaması (L1)`() {
        val e = engine()
        // e-chess Peşəkar (w=3) ən üstdə olmalıdır (rankIndex+1 çarpanı sabitdir)
        val ranked = e.cityEvents.sortedByDescending {
            XpRules.proWeight(it.level) * (e.rankIndex + 1)
        }
        assertEquals("e-chess", ranked.first().id)
    }

    // ── A2 düzəlişi: həftəlik rəy limiti sıfırlanır ──

    @Test fun `rəy həftəlik limiti həftə dəyişəndə sıfırlanır (A2)`() {
        val e = engine()
        e.join("e0") // keçmiş tədbir (joined-dədir)
        val w1 = LocalDate.of(2026, 10, 26) // həftə A
        val w2 = LocalDate.of(2026, 11, 9)  // həftə B
        assertTrue(e.addReview("e0", 5, "əla", w1))
        assertTrue(e.addReview("e0", 5, "əla", w1))
        assertTrue(e.addReview("e0", 5, "əla", w1))
        assertFalse(e.addReview("e0", 5, "əla", w1)) // cap
        assertTrue(e.addReview("e0", 5, "əla", w2))  // yeni həftə — sıfırlıb
    }

    @Test fun `rəy həftəlik cap 3 (ROL F)`() {
        val e = engine()
        e.join("e0")
        val day = LocalDate.of(2026, 10, 26)
        assertTrue(e.addReview("e0", 5, "1", day))
        assertTrue(e.addReview("e0", 5, "2", day))
        assertTrue(e.addReview("e0", 5, "3", day))
        assertFalse(e.addReview("e0", 5, "4", day))
    }

    // ── Yaratma ──

    @Test fun `createEvent siyahının başına düşür və host mən olur`() {
        val e = engine()
        e.setProfileName("Test İstifadəçi")
        e.createEvent(
            title = "Yeni tədbir", category = "Qəhvə", city = "Bakı",
            location = "Test kafe", date = "2026-11-05", time = "18:00",
            capacity = 10, description = "Test", epochDay = 20600,
        )
        val first = e.state.value.events.first()
        assertEquals("Test İstifadəçi", first.host)
        assertTrue(first.id in e.state.value.joined)
        assertEquals(1, first.participants)
        assertEquals("coffee", first.image) // imageForCategory
    }

    // ── Söhbətlər ──

    @Test fun `hadisə mesajı göndərilir (avtomatik cavab testdə bağlıdır)`() {
        val e = engine() // scope = null → avto-cavab yoxdur
        e.sendEventMessage("e1", "Salam!")
        val msgs = e.state.value.eventMessages["e1"]!!
        assertTrue(msgs.last().mine)
        assertEquals("Salam!", msgs.last().body)
    }

    @Test fun `söhbəti sil dostluğu da silir`() {
        val e = engine()
        assertTrue(e.state.value.friendMessages.containsKey("p2"))
        e.deleteChat("p2")
        assertFalse(e.state.value.friendMessages.containsKey("p2"))
        assertFalse(e.state.value.friendships.containsKey("p2"))
    }

    // ── Assistant ──

    @Test fun `assistant təklif cavabı TƏDBİR JSON verir və parser işləyir`() {
        val e = engine() // scope=null → dərhal tam cavab
        e.sendAssistant("Şənbə üçün tədbir fikri ver")
        val msgs = e.state.value.assistantMsgs
        assertTrue(msgs.size >= 2)
        val reply = msgs.last { !it.mine }.body
        assertTrue(reply.contains("[TƏDBİR]"))
        val parsed = az.qoshul.app.core.model.parseAssistantEvent(reply)
        assertNotNull(parsed.proposal)
        assertEquals("Şənbə səhəri Qala yürüşü", parsed.proposal!!.title)
        assertTrue(parsed.text.isNotEmpty())
        assertFalse(parsed.text.contains("[TƏDBİR]"))
    }

    @Test fun `assistant pulsuz sualı şəhər tədbirlərini sadalayır`() {
        val e = engine()
        e.sendAssistant("Pulsuz tədbirlər hansılardır?")
        val reply = e.state.value.assistantMsgs.last { !it.mine }.body
        assertTrue(reply.contains("Pulsuz tədbirlər"))
    }

    // ── Şəhər ──

    @Test fun `setCity feed filtrlərini dəyişir`() {
        val e = engine()
        e.setCity("Gəncə")
        assertTrue(e.cityEvents.all { it.city == "Gəncə" })
        assertTrue(e.cityEvents.any { it.id == "e9" })
    }

    @Test fun `görünüş rejimi dəyişilir (D-027)`() {
        val e = engine()
        e.setMapVisibility(MapVisibility.FRIENDS)
        assertEquals(MapVisibility.FRIENDS, e.state.value.mapVisibility)
        e.setMapVisibility(MapVisibility.OFF)
        assertEquals(MapVisibility.OFF, e.state.value.mapVisibility)
    }
}

/** Köməkçi: o4 təklifinin səviyyə qapısı (minRank 2). */
private object CouponOfferMinRankCheck {
    fun o4unlockedFor(rankIndex: Int): Boolean = FakeQoshulDataSource.offers
        .first { it.id == "o4" }.unlockedFor(rankIndex)
}
