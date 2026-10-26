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
        // Baş kısmı tutması yalnızca uzun adaylarda kabul edilir: "nsıl" gibi bir yazım
        // hatası "Nsilambanda" gibi uzak bir yeri getirmesin.
        return a == b || (a.length >= MIN_PREFIX_LENGTH && b.startsWith(a))
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
         * Şehir adı olabilir mi? 81 ilin adı her zaman adaydır ("Mersin", "ne dersin"deki
         * "dersin"e tek harf uzak olsa da). Diğer kelimelerden elenenler:
         * - kısa kelimeler ve bilinen soru kelimeleri ("hava", "nasıl"),
         * - gün kelimeleri, ekli halleriyle ve büyük harfle yazılsa bile ("Bugün", "yarınki",
         *   "bugünkü", "akşamki"): "Bugün" adında bir köy bulunsa da kimse onu sormaz,
         * - bir soru kelimesinin tek harf hatalı yazımı ("nsıl", "hva"),
         * - robotun konu kelimeleri ("yağmur", "piknik"). Büyük harfle başlayan kelime konu
         *   kelimesine benzese de aday kalır: "Denizli" "deniz" ile başlar ama şehirdir.
         */
        val isCandidate: Boolean = folded in PROVINCE_NAMES || folded.length >= MIN_LENGTH &&
            folded !in STOP_WORDS &&
            DAY_STEMS.none { folded.startsWith(it) } &&
            !looksLikeStopWord(folded) &&
            (base.first().isUpperCase() || WeatherAssistant.Topic.entries.none { topic -> topic.stems.any { folded.startsWith(it) } })
    }

    private const val MIN_LENGTH = 3

    /** 81 ilin sadeleştirilmiş adı; aşağıdaki eleme kurallarından muaftır. */
    private val PROVINCE_NAMES: Set<String> by lazy { TurkishProvinces.map { WeatherAssistant.normalize(it.name) }.toSet() }
    private const val MIN_PREFIX_LENGTH = 5

    /** Gün ve zaman kelimelerinin kökleri; ekli halleri de ("bugünkü", "yarına") yakalanır. */
    private val DAY_STEMS = listOf(
        "bugun", "yarin", "dun", "simdi", "haftaya", "haftasonu", "sabah", "aksam", "gece", "ogle",
        "pazartesi", "sali", "carsamba", "persembe", "cuma", "pazar",
        "today", "tomorrow", "tonight", "yesterday", "monday", "tuesday", "wednesday", "thursday", "friday",
        "saturday", "sunday", "weekend"
    )

    /**
     * Kelime bir soru kelimesinin tek harf eksik, fazla ya da yanlış yazılmış hali mi?
     * ("nsıl" → "nasıl", "hva" → "hava"). Yalnızca 4 harf ve üstü soru kelimelerine bakılır;
     * kısa kelimelerde tek harf farkı gerçek şehir adlarını da yakalardı.
     */
    private fun looksLikeStopWord(word: String): Boolean =
        STOP_WORDS.any { it.length >= 4 && it != word && editDistanceAtMostOne(it, word) }

    /** İki kelime arasında en fazla bir harf ekleme, silme ya da değiştirme farkı var mı? */
    private fun editDistanceAtMostOne(a: String, b: String): Boolean {
        if (kotlin.math.abs(a.length - b.length) > 1) return false
        var i = 0
        var j = 0
        var edits = 0
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) {
                i++
                j++
                continue
            }
            if (++edits > 1) return false
            when {
                a.length > b.length -> i++
                a.length < b.length -> j++
                else -> {
                    i++
                    j++
                }
            }
        }
        return edits + (a.length - i) + (b.length - j) <= 1
    }

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
