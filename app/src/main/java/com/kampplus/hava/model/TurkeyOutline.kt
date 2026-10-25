package com.kampplus.hava.model

/**
 * Türkiye'nin sadeleştirilmiş sınırı: haritada illerin arkasına çizilen kara parçası.
 *
 * Gerçek sınır binlerce noktadan oluşur; burada kıyıların ve kara sınırlarının belirgin
 * noktalarından (burunlar, körfezler, sınır kapıları) oluşan yaklaşık bir çizim vardır.
 * Amaç ölçüm değil, illerin Türkiye'nin şekli içinde görünmesidir.
 *
 * Her nokta `(enlem, boylam)` çiftidir; noktalar sırayla birleştirilip kapatılır.
 */
object TurkeyOutline {

    /** Anadolu: İstanbul Boğazı'nın Anadolu yakasından başlayıp saat yönünde. */
    val anatolia: List<Pair<Double, Double>> = listOf(
        // Karadeniz kıyısı, batıdan doğuya
        41.23 to 29.15, 41.18 to 29.60, 41.18 to 30.20, 41.10 to 30.65, 41.09 to 31.10, 41.28 to 31.42,
        41.46 to 31.79, 41.75 to 32.39, 41.90 to 33.00, 41.98 to 33.77, 42.10 to 34.98, 42.05 to 35.22,
        41.80 to 35.24, 41.73 to 35.95, 41.29 to 36.33, 41.13 to 37.28, 40.98 to 37.88, 40.92 to 38.39,
        41.04 to 39.73, 41.05 to 40.52, 41.39 to 41.43, 41.52 to 41.55,
        // Gürcistan ve Ermenistan sınırı
        41.45 to 42.50, 41.50 to 42.85, 41.12 to 43.45, 40.75 to 43.60, 40.45 to 43.70, 40.10 to 43.70,
        39.98 to 44.40, 39.70 to 44.80,
        // İran sınırı
        39.40 to 44.60, 39.00 to 44.40, 38.40 to 44.30, 37.75 to 44.20, 37.30 to 44.80, 37.15 to 44.79,
        // Irak ve Suriye sınırı
        37.05 to 44.30, 37.30 to 43.40, 37.33 to 42.80, 37.11 to 42.36, 37.07 to 41.20, 37.10 to 40.70,
        36.70 to 38.95, 36.83 to 38.00, 36.65 to 37.10, 36.83 to 36.70,
        // Hatay
        36.40 to 36.67, 36.22 to 36.55, 36.15 to 36.38, 35.82 to 36.15, 35.92 to 35.92,
        // Akdeniz kıyısı, doğudan batıya
        36.08 to 35.95, 36.40 to 35.90, 36.58 to 36.17, 36.85 to 36.20, 36.77 to 35.79, 36.57 to 35.38,
        36.80 to 34.64, 36.60 to 34.30, 36.30 to 33.88, 36.02 to 32.80, 36.54 to 32.00, 36.78 to 31.45,
        36.88 to 30.70, 36.60 to 30.56, 36.20 to 30.40, 36.20 to 29.64, 36.62 to 29.10, 36.70 to 28.80,
        36.85 to 28.27, 36.68 to 27.40,
        // Ege kıyısı, güneyden kuzeye
        37.03 to 27.43, 37.38 to 27.27, 37.86 to 27.25, 38.32 to 26.30, 38.65 to 26.50, 38.75 to 26.90,
        39.07 to 26.88, 39.32 to 26.69, 39.58 to 27.00, 39.48 to 26.07, 39.80 to 26.15, 40.00 to 26.20,
        40.12 to 26.36,
        // Çanakkale Boğazı ve Marmara'nın güney kıyısı
        40.25 to 26.50, 40.40 to 26.70, 40.40 to 27.30, 40.50 to 27.85, 40.35 to 27.97, 40.37 to 28.88, 40.43 to 29.10,
        40.52 to 28.85, 40.68 to 29.10, 40.68 to 29.28, 40.70 to 29.60, 40.76 to 29.92,
        // İzmit Körfezi'nin kuzeyi ve İstanbul'un Anadolu yakası
        40.77 to 29.60, 40.80 to 29.40, 40.82 to 29.30, 40.87 to 29.25, 40.90 to 29.18, 40.95 to 29.10,
        40.98 to 29.02, 41.03 to 29.02, 41.10 to 29.06, 41.20 to 29.10
    )

    /** Trakya: İstanbul Boğazı'nın Avrupa yakasından başlayıp saat yönünün tersine. */
    val thrace: List<Pair<Double, Double>> = listOf(
        41.25 to 29.05, 41.33 to 28.60, 41.63 to 28.10, 41.88 to 27.98, 41.98 to 28.00,
        // Bulgaristan ve Yunanistan sınırı
        41.95 to 27.55, 42.07 to 27.00, 41.90 to 26.60, 41.71 to 26.36, 41.30 to 26.33, 40.95 to 26.32,
        40.75 to 26.05,
        // Saros Körfezi ve Gelibolu Yarımadası
        40.72 to 26.08, 40.60 to 26.60, 40.32 to 26.18, 40.05 to 26.18, 40.20 to 26.40, 40.42 to 26.70,
        // Marmara'nın kuzey kıyısı
        40.60 to 27.10, 40.97 to 27.50, 40.97 to 27.95, 41.07 to 28.25, 41.00 to 28.58, 41.00 to 28.95,
        41.10 to 29.03
    )

    /**
     * Göller: (merkez enlem, merkez boylam, enlem yarıçapı, boylam yarıçapı), elips olarak çizilir.
     * Van Gölü ve Tuz Gölü haritada denizle aynı renkte görünür.
     */
    val lakes: List<Lake> = listOf(
        Lake(latitude = 38.65, longitude = 42.85, latitudeRadius = 0.28, longitudeRadius = 0.55),
        Lake(latitude = 38.80, longitude = 33.40, latitudeRadius = 0.30, longitudeRadius = 0.16)
    )

    data class Lake(val latitude: Double, val longitude: Double, val latitudeRadius: Double, val longitudeRadius: Double)

    /**
     * Nokta çokgenin içinde mi? Işın atma yöntemi: noktadan sağa çekilen bir çizgi çokgenin
     * kenarlarını tek sayıda keserse nokta içeridedir. Testlerde illerin kara içinde
     * kaldığını doğrulamak için kullanılır.
     */
    fun contains(polygon: List<Pair<Double, Double>>, latitude: Double, longitude: Double): Boolean {
        var inside = false
        var j = polygon.lastIndex
        for (i in polygon.indices) {
            val (latI, lonI) = polygon[i]
            val (latJ, lonJ) = polygon[j]
            val crosses = (latI > latitude) != (latJ > latitude) &&
                longitude < (lonJ - lonI) * (latitude - latI) / (latJ - latI) + lonI
            if (crosses) inside = !inside
            j = i
        }
        return inside
    }
}
