package com.kampplus.hava.model

/**
 * Türkiye'nin 81 ili: plaka numarası, adı ve il merkezinin koordinatı.
 *
 * Koordinatlar Open-Meteo şehir arama servisinden alınmış il merkezleridir (örneğin
 * Kocaeli için İzmit, Hatay için Antakya). Harita bu noktaları çizer; sıcaklık da
 * bu noktalar için istenir.
 *
 * @param plate Plaka numarası (1–81); ilin kimliği olarak da kullanılır.
 */
data class Province(val plate: Int, val name: String, val latitude: Double, val longitude: Double) {
    /** Tahmin ekranına gitmek için şehir modeline çevirir: "İstanbul, Türkiye". */
    fun toCity(): City = City(
        id = plate.toLong(),
        name = name,
        latitude = latitude,
        longitude = longitude,
        countryCode = "TR",
        country = "Türkiye",
        admin1 = name
    )
}

/** Plaka sırasıyla 81 il. */
val TurkishProvinces: List<Province> = listOf(
    Province(1, "Adana", 36.98615, 35.32531),
    Province(2, "Adıyaman", 37.76441, 38.27629),
    Province(3, "Afyonkarahisar", 38.75667, 30.54333),
    Province(4, "Ağrı", 39.71467, 43.04015),
    Province(5, "Amasya", 40.65333, 35.83306),
    Province(6, "Ankara", 39.91987, 32.85427),
    Province(7, "Antalya", 36.90812, 30.69556),
    Province(8, "Artvin", 41.18161, 41.82172),
    Province(9, "Aydın", 37.84501, 27.83963),
    Province(10, "Balıkesir", 39.64917, 27.88611),
    Province(11, "Bilecik", 40.14192, 29.97932),
    Province(12, "Bingöl", 38.88472, 40.49389),
    Province(13, "Bitlis", 38.40115, 42.10784),
    Province(14, "Bolu", 40.73583, 31.60611),
    Province(15, "Burdur", 37.72028, 30.29083),
    Province(16, "Bursa", 40.19559, 29.06013),
    Province(17, "Çanakkale", 40.15552, 26.41271),
    Province(18, "Çankırı", 40.59995, 33.6153),
    Province(19, "Çorum", 40.54889, 34.95333),
    Province(20, "Denizli", 37.77417, 29.0875),
    Province(21, "Diyarbakır", 37.91363, 40.21721),
    Province(22, "Edirne", 41.67719, 26.55597),
    Province(23, "Elazığ", 38.67431, 39.22321),
    Province(24, "Erzincan", 39.73919, 39.49015),
    Province(25, "Erzurum", 39.90861, 41.27694),
    Province(26, "Eskişehir", 39.77667, 30.52056),
    Province(27, "Gaziantep", 37.05944, 37.3825),
    Province(28, "Giresun", 40.91698, 38.38741),
    Province(29, "Gümüşhane", 40.46001, 39.47176),
    Province(30, "Hakkari", 37.57444, 43.74083),
    Province(31, "Hatay", 36.20655, 36.15722),
    Province(32, "Isparta", 37.76444, 30.55222),
    Province(33, "Mersin", 36.81196, 34.63886),
    Province(34, "İstanbul", 41.01384, 28.94966),
    Province(35, "İzmir", 38.41273, 27.13838),
    Province(36, "Kars", 40.59825, 43.08548),
    Province(37, "Kastamonu", 41.37805, 33.77528),
    Province(38, "Kayseri", 38.73222, 35.48528),
    Province(39, "Kırklareli", 41.73508, 27.22521),
    Province(40, "Kırşehir", 39.14583, 34.16389),
    Province(41, "Kocaeli", 40.76499, 29.92928),
    Province(42, "Konya", 37.87135, 32.48464),
    Province(43, "Kütahya", 39.42417, 29.98333),
    Province(44, "Malatya", 38.35018, 38.31667),
    Province(45, "Manisa", 38.61202, 27.42647),
    Province(46, "Kahramanmaraş", 37.5847, 36.92641),
    Province(47, "Mardin", 37.31309, 40.74357),
    Province(48, "Muğla", 37.21807, 28.3665),
    Province(49, "Muş", 38.73163, 41.48482),
    Province(50, "Nevşehir", 38.625, 34.71222),
    Province(51, "Niğde", 37.96583, 34.67935),
    Province(52, "Ordu", 40.97782, 37.89047),
    Province(53, "Rize", 41.02083, 40.52194),
    Province(54, "Sakarya", 40.78056, 30.40333),
    Province(55, "Samsun", 41.27976, 36.3361),
    Province(56, "Siirt", 37.9293, 41.94134),
    Province(57, "Sinop", 42.02683, 35.16253),
    Province(58, "Sivas", 39.74833, 37.01611),
    Province(59, "Tekirdağ", 40.9781, 27.51101),
    Province(60, "Tokat", 40.31389, 36.55444),
    Province(61, "Trabzon", 41.005, 39.72694),
    Province(62, "Tunceli", 39.09921, 39.54351),
    Province(63, "Şanlıurfa", 37.16708, 38.79392),
    Province(64, "Uşak", 38.67351, 29.4058),
    Province(65, "Van", 38.49457, 43.38323),
    Province(66, "Yozgat", 39.82, 34.80444),
    Province(67, "Zonguldak", 41.45139, 31.79305),
    Province(68, "Aksaray", 38.37255, 34.02537),
    Province(69, "Bayburt", 40.25631, 40.22289),
    Province(70, "Karaman", 37.18111, 33.215),
    Province(71, "Kırıkkale", 39.84528, 33.50639),
    Province(72, "Batman", 37.88738, 41.13221),
    Province(73, "Şırnak", 37.51393, 42.45432),
    Province(74, "Bartın", 41.63583, 32.3375),
    Province(75, "Ardahan", 41.10871, 42.70222),
    Province(76, "Iğdır", 39.92371, 44.045),
    Province(77, "Yalova", 40.65501, 29.27693),
    Province(78, "Karabük", 41.20488, 32.62768),
    Province(79, "Kilis", 36.71611, 37.115),
    Province(80, "Osmaniye", 37.07417, 36.24778),
    Province(81, "Düzce", 40.83889, 31.16389)
)

/**
 * Bir ilin bugünkü ortalama sıcaklığı (gece ve gündüzün ortalaması).
 * Servis değer göndermediyse [meanC] null'dır.
 */
data class ProvinceTemperature(val province: Province, val meanC: Double?, val weatherCode: Int?)
