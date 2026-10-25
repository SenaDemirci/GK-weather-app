package com.kampplus.hava.model

import androidx.annotation.StringRes
import com.kampplus.hava.R
import java.time.LocalDateTime

/**
 * Saatlik tahmindeki filtre seçenekleri: günün hangi dilimine bakılacağı.
 *
 * Aralıklar başta dahil, sonda hariçtir: "Sabah" 06:00, 07:00 ... 11:00 saatlerini kapsar;
 * 12:00 "Öğle"ye aittir. Böylece her saat tam olarak bir dilime düşer.
 *
 * Enum olduğu için `rememberSaveable` ile doğrudan saklanabilir; ekran dönünce
 * seçili filtre kaybolmaz.
 *
 * @param startHour Dilimin ilk saati (0–23). [ALL] için kullanılmaz.
 * @param endHour Dilimin bittiği saat (hariç, 1–24).
 */
enum class HourRange(@StringRes val labelRes: Int, val startHour: Int, val endHour: Int) {
    ALL(R.string.hour_range_all, 0, 24),
    MORNING(R.string.hour_range_morning, 6, 12),
    NOON(R.string.hour_range_noon, 12, 18),
    EVENING(R.string.hour_range_evening, 18, 24),
    NIGHT(R.string.hour_range_night, 0, 6);

    /** Çipte etiketin altında gösterilen aralık: "06–12". [ALL] için null. */
    val span: String?
        get() = if (this == ALL) null else "%02d–%02d".format(startHour, endHour)

    operator fun contains(time: LocalDateTime): Boolean = time.hour in startHour until endHour

    /** Listeden yalnızca bu dilime düşen saatleri bırakır. */
    fun filter(hours: List<HourlyForecast>): List<HourlyForecast> = hours.filter { it.time in this }
}
