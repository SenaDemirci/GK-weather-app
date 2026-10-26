package com.kampplus.hava.model

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sorudaki şehir adının doğru çıkarıldığını doğrular. */
class CityMentionTest {

    private fun first(question: String) = CityMention.candidates(question).first()

    @Test
    fun `kesme isaretli sehir adi ekinden ayrilir`() {
        assertEquals("İzmir", first("İzmir'de yarın yağmur var mı?"))
        assertEquals("Ankara", first("Ankara’da bugün ne giymeliyim?"))
    }

    @Test
    fun `soru ve konu kelimeleri aday olmaz`() {
        assertEquals(listOf("Antalya"), CityMention.candidates("Antalya hafta sonu pikniğe uygun mu?"))
        assertTrue(CityMention.candidates("yarın yağmur var mı?").isEmpty())
        assertTrue(CityMention.candidates("Will it rain tomorrow?").isEmpty())
    }

    @Test
    fun `ingilizce sorudaki sehir bulunur`() {
        assertEquals("London", first("What's the weather in London?"))
        assertEquals("Paris", first("Will it rain in Paris tomorrow?"))
    }

    @Test
    fun `iki kelimelik sehir adi tek aday olarak da denenir`() {
        assertTrue("New York" in CityMention.candidates("weather in New York tomorrow"))
    }

    @Test
    fun `kesme isaretsiz yazilan ekli kelimenin eksiz hali de denenir`() {
        val candidates = CityMention.candidates("izmirde hava nasıl")

        assertEquals(listOf("izmirde", "izmir"), candidates)
    }

    @Test
    fun `buyuk harfle yazilan sehir konu kelimesine benzese de aday kalir`() {
        // "Denizli", "deniz" kelimesiyle başlar ama büyük harfle yazıldığı için şehir sayılır.
        assertEquals("Denizli", first("Denizli yarın nasıl"))
    }

    @Test
    fun `sehir eslesmesi benzer ama farkli adlari kabul etmez`() {
        assertTrue(CityMention.matches("izmir", "İzmir"))
        assertTrue(CityMention.matches("new york", "New York"))
        assertFalse(CityMention.matches("para", "Paramaribo".take(2)))
        assertFalse(CityMention.matches("ankara", "Antalya"))
    }

    @Test
    fun `gun kelimeleri ekli ve buyuk harfli halleriyle de sehir sayilmaz`() {
        listOf("Bugün hava nasıl?", "bugünkü hava nasıl", "Yarınki hava", "yarına yağmur var mı", "Today weather", "akşamki hava")
            .forEach { assertTrue("\"$it\" içinde şehir aranmamalı", CityMention.candidates(it).isEmpty()) }
    }

    @Test
    fun `soru kelimesinin yazim hatasi sehir sayilmaz`() {
        // "nsıl" → "nasıl", "hva" kısa; "havs" → "hava".
        assertTrue(CityMention.candidates("bugün hava nsıl").isEmpty())
        assertTrue(CityMention.candidates("yarın havs nasıl").isEmpty())
    }

    @Test
    fun `kisa adayin bas kismi uzak bir yeri eslestirmez`() {
        assertFalse(CityMention.matches("nsıl", "Nsilambanda"))
        assertTrue(CityMention.matches("Kahraman", "Kahramanmaraş"))
    }

    @Test
    fun `81 ilin hicbiri gun ya da yazim hatasi kuralina takilmaz`() {
        TurkishProvinces.forEach { province ->
            assertEquals("${province.name} aday olmalı", province.name, CityMention.candidates("${province.name} hava nasıl").firstOrNull())
        }
    }

    /** Şehir seçmeden açılan sohbetin örnek sorularının her birinde bir şehir bulunmalı. */
    @Test
    fun `genel ornek sorularin hepsinde sehir bulunur`() {
        val keys = (1..4).map { "bot_suggest_city_$it" }
        listOf("src/main/res/values/strings.xml", "src/main/res/values-en/strings.xml").forEach { path ->
            val xml = File(path).readText()
            keys.forEach { key ->
                val question = Regex("""<string name="$key">(.*?)</string>""").find(xml)!!.groupValues[1].replace("\\'", "'")
                assertTrue("$path: \"$question\" içinde şehir bulunamadı", CityMention.candidates(question).isNotEmpty())
            }
        }
    }
}
