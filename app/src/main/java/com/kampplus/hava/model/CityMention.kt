package com.kampplus.hava.model

/**
 * Sorudaki şehir adını bulur: "İzmir'de yarın yağmur var mı?" → "İzmir".
 *
 * Hava Robotu bir dil modeli olmadığı için cümleyi anlamaz; bunun yerine bilinen
 * kelimeleri (soru kelimeleri, günler, konu kelimeleri) eler, geriye kalan kelimeleri
 * **aday** olarak döndürür. Hangi adayın gerçekten bir şehir olduğuna şehir arama
 * servisi karar verir.
 *
 * Adaylar en olası olandan başlayarak sıralanır:
 * 1. Kesme işaretli kelimenin kökü ("Ankara'da" → "Ankara"),
 * 2. Yan yana iki aday kelime ("New York"),
 * 3. Tek kelimeler,
 * 4. Küçük harfle, kesme işareti olmadan yazılmış ekli kelimelerin eksiz hali ("izmirde" → "izmir").
 */
object CityMention {

    /** En fazla bu kadar aday döndürülür; her aday bir ağ isteği demektir. */
    const val MAX_CANDIDATES = 4

    fun candidates(question: String): List<String> {
        val words = question
            .split(Regex("[^\\p{L}'’]+"))
            .filter { it.isNotBlank() }
            .map { Word(it) }
        val kept = words.filter { it.isCandidate }

        val result = mutableListOf<String>()
        // Kesme işareti kullanan kişi büyük ihtimalle bir özel ad yazıyordur.
        kept.filter { it.hadApostrophe }.forEach { result += it.base }
        // Ardışık iki aday kelime: "New York", "Buenos Aires".
        words.zipWithNext().forEach { (a, b) -> if (a.isCandidate && b.isCandidate && !a.hadApostrophe) result += "${a.base} ${b.base}" }
        kept.forEach { result += it.base }
        // Ekli yazım yalnızca küçük harfle yazılmış kelimelerde denenir; "Antalya"yı "Antal"a çevirmeyelim.
        kept.filter { !it.hadApostrophe && it.base.first().isLowerCase() }.forEach { word -> stripSuffix(word.base)?.let { result += it } }

        return result.distinctBy { WeatherAssistant.normalize(it) }.take(MAX_CANDIDATES)
    }

    /**
     * Servisin bulduğu şehir, aranan kelimeyle gerçekten aynı mı?
     *
     * Şehir araması benzer adları da döndürebilir; "para" kelimesi için "Pará" gibi.
     * Yalnızca adı aranan kelimeyle aynı olan ya da onunla başlayan şehirleri kabul ederiz.
     */
    fun matches(candidate: String, cityName: String): Boolean {
        val a = WeatherAssistant.normalize(candidate)
        val b = WeatherAssistant.normalize(cityName)
        return a == b || (a.length >= MIN_LENGTH && b.startsWith(a))
    }

    /** "izmirde" → "izmir", "ankaraya" → "ankara". Ek bulunamazsa null. */
    internal fun stripSuffix(word: String): String? {
        val folded = WeatherAssistant.normalize(word)
        val suffix = SUFFIXES.firstOrNull { folded.endsWith(it) && folded.length - it.length >= MIN_LENGTH } ?: return null
        return word.dropLast(suffix.length)
    }

    private class Word(raw: String) {
        /** Kesme işaretinden önceki kısım: "Ankara'da" → "Ankara". */
        val base: String = raw.split('\'', '’').first()
        val hadApostrophe: Boolean = '\'' in raw || '’' in raw
        private val folded = WeatherAssistant.normalize(base)

        /**
         * Şehir adı olabilir mi? Kısa kelimeler, bilinen soru/gün kelimeleri ve robotun
         * konu kelimeleri ("yağmur", "piknik"...) elenir. Büyük harfle başlayan bir kelime
         * konu kelimesine benzese de aday kalır: "Denizli" "deniz" ile başlar ama şehirdir.
         */
        val isCandidate: Boolean = folded.length >= MIN_LENGTH &&
            folded !in STOP_WORDS &&
            (base.first().isUpperCase() || WeatherAssistant.Topic.entries.none { topic -> topic.stems.any { folded.startsWith(it) } })
    }

    private const val MIN_LENGTH = 3

    /** Uzun ekler önce denenir: "istanbuldaki" önce "daki" ile, "da" ile değil. */
    private val SUFFIXES = listOf(
        "ndaki", "ndeki", "daki", "deki", "taki", "teki",
        "nda", "nde", "dan", "den", "tan", "ten", "nin", "nun", "yla", "yle",
        "da", "de", "ta", "te", "ya", "ye", "yi", "yu", "in", "un", "a", "e", "i", "u"
    )

    /** Sadeleştirilmiş halleriyle (bkz. [WeatherAssistant.normalize]) soru ve gün kelimeleri. */
    private val STOP_WORDS = setOf(
        // Türkçe
        "hava", "havasi", "havalar", "nasil", "nasildir", "nasilsin", "ne", "mi", "mu", "misin", "miyim", "var", "yok",
        "icin", "ki", "bir", "bu", "su", "sehir", "sehri", "sehrinde", "olacak", "olur", "acaba", "lutfen",
        "bana", "soyle", "soyler", "bakar", "bak", "ben", "sen", "biz", "gun", "gunu", "gunler", "hafta", "haftasonu",
        "sonu", "sonra", "sabah", "aksam", "gece", "ogle", "saat", "kac", "durum", "durumu", "tahmin", "tahmini",
        "gidebilir", "gidecegim", "gideyim", "gitmek", "cikabilir", "cikmak", "iyi", "guzel", "uygun", "hangi",
        "hangisi", "ile", "veya", "cok", "daha", "simdi", "bugun", "yarin", "obur", "ertesi", "istiyorum",
        "ogrenmek", "merak", "ediyorum", "mesela", "olsun", "olur", "olabilir", "mudur", "midir", "nedir",
        "pazartesi", "sali", "carsamba", "persembe", "cuma", "cumartesi", "pazar", "merhaba", "selam", "tesekkur",
        "tesekkurler", "sagol", "nerede", "neresi", "dersin", "dusunuyorum", "planliyorum",
        // İngilizce
        "the", "and", "for", "what", "whats", "how", "hows", "weather", "forecast", "like", "today", "tomorrow",
        "tonight", "now", "there", "any", "should", "will", "would", "could", "can", "does", "you", "your", "please",
        "tell", "about", "this", "week", "weekend", "day", "days", "best", "good", "going", "out", "with", "next",
        "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "morning", "evening",
        "night", "afternoon", "need", "chance", "degrees", "rainy", "sunny", "cloudy", "windy", "hello", "thanks",
        "thank", "want", "know", "plan", "planning", "today's", "tomorrow's", "great", "nice", "okay"
    )
}
