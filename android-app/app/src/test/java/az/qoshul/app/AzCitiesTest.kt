package az.qoshul.app

import az.qoshul.app.core.data.FakeQoshulDataSource
import az.qoshul.app.core.model.AzCollation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * D-126 (v0.44.0): BÜTÜN Azərbaycan şəhərləri sertifikatı — 79 rəsmi şəhər,
 * AzCollation A-Z sıralı, bölgələr 2021 prezident fərmanının 14 iqtisadi
 * rayonu. Mənbə: az.wikipedia „Azərbaycan şəhərlərinin siyahısı“ (79),
 * EN Wikipedia „List of cities in Azerbaijan“ (78 @2013 + Ağdərə 2023),
 * president.az/az/articles/view/52389 (bölgü).
 */
class AzCitiesTest {

    private val cities = FakeQoshulDataSource.cities

    /** 2021 fərmanı: 14 iqtisadi rayon (+ Bakı ayrıca). */
    private val officialRegions = setOf(
        "Bakı", "Abşeron-Xızı", "Gəncə-Daşkəsən", "Qazax-Tovuz",
        "Şəki-Zaqatala", "Lənkəran-Astara", "Quba-Xaçmaz", "Dağlıq Şirvan",
        "Mərkəzi Aran", "Mil-Muğan", "Şirvan-Salyan", "Qarabağ",
        "Şərqi Zəngəzur", "Naxçıvan MR",
    )

    @Test
    fun `79 rəsmi şəhər var`() {
        assertEquals("Azərbaycanın rəsmi şəhər sayı 79-dur", 79, cities.size)
    }

    @Test
    fun `adlar unikaldır`() {
        val dupes = cities.groupBy { it.name }.filterValues { it.size > 1 }.keys
        assertTrue("Təkrar şəhər adları: $dupes", dupes.isEmpty())
    }

    @Test
    fun `siyahi AzCollation A-Z sıralıdır`() {
        val sorted = cities.sortedWith { a, b -> AzCollation.compare(a.name, b.name) }
        assertEquals(
            "Şəhər siyahısı Azərbaycan əlifbası ilə A-Z olmalıdır",
            sorted.map { it.name }, cities.map { it.name },
        )
    }

    @Test
    fun `bütün bölgələr rəsmi 14 iqtisadi rayondandır`() {
        val bad = cities.filter { it.region !in officialRegions }
        assertTrue("Naməlum bölgə etiketləri: ${bad.map { "${it.name}→${it.region}" }}", bad.isEmpty())
    }

    @Test
    fun `əsas şəhərlər mövcuddur`() {
        val names = cities.map { it.name }
        listOf(
            "Bakı", "Sumqayıt", "Gəncə", "Lənkəran", "Mingəçevir", "Naxçıvan",
            "Şəki", "Şirvan", "Xankəndi", "Şuşa", "Ağdam", "Füzuli",
            "Zaqatala", "Quba", "Qəbələ", "Horadiz", "Göytəpə", "Ağdərə",
        ).forEach { n ->
            assertTrue("$n siyahıda olmalıdır", n in names)
        }
    }

    @Test
    fun `koordinatlar Azərbaycan hüdudlarındadır`() {
        // AZ təxmini sərhədləri: lat 38.4–41.9, lng 44.7–50.6
        val bad = cities.filter { it.lat !in 38.4..41.9 || it.lng !in 44.7..50.6 }
        assertTrue("Hüdud xarici koordinatlar: ${bad.map { "${it.name}(${it.lat},${it.lng})" }}", bad.isEmpty())
    }
}

/** AzCollation vahid-testi — əlifba sırası (ç/ə/ğ/ı/i/ö/ş/ü). */
class AzCollationTest {

    @Test
    fun `ə hərfi e den sonra gelir`() {
        assertTrue(AzCollation.compare("Ermədulan", "Əbdül") < 0)
        assertTrue(AzCollation.compare("Əbdül", "Fərman") < 0)
    }

    @Test
    fun `c ve ç ferqlidir`() {
        assertTrue(AzCollation.compare("Cəbrayıl", "Çərşənbə") < 0)
        assertTrue(AzCollation.compare("Çərşənbə", "Dərviş") < 0)
    }

    @Test
    fun `i ve ı ferqlidir`() {
        assertTrue(AzCollation.compare("İmişli", "Iğdır") != 0)
        assertTrue(AzCollation.compare("Hacıqabul", "Xaçmaz") < 0)
    }

    @Test
    fun `o ve ö ferqli yerdedir`() {
        assertTrue(AzCollation.compare("Oğuz", "Ördəkli") < 0)
        assertTrue(AzCollation.compare("Ömər", "Padar") < 0)
    }

    @Test
    fun `q hərfi k dan sonra l den əvvəl gəlir`() {
        // əlifba: … j k q l m n o ö p r s ş t u ü v y z
        assertTrue(AzCollation.compare("Kürdəmir", "Qax") < 0)
        assertTrue(AzCollation.compare("Qax", "Laçın") < 0)
    }

    @Test
    fun `s ve ş ferqlidir`() {
        assertTrue(AzCollation.compare("Salyan", "Şabran") < 0)
        assertTrue(AzCollation.compare("Şabran", "Tərtər") < 0)
    }

    @Test
    fun `u ve ü ferqlidir`() {
        assertTrue(AzCollation.compare("Ucar", "Ümid") < 0)
        assertTrue(AzCollation.compare("Ümid", "Vətən") < 0)
    }

    @Test
    fun `prefiks qisadir qalindan onde`() {
        assertTrue(AzCollation.compare("Şəki", "Şəkilistan") < 0)
    }
}
