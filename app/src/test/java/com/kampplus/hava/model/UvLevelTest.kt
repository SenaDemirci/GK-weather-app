package com.kampplus.hava.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** UV indeksinin doğru seviyeye eşlendiğini doğrular. */
class UvLevelTest {

    @Test
    fun `seviye sinirlari dunya saglik orgutu olcegine uyar`() {
        assertEquals(UvLevel.LOW, UvLevel.of(0.0))
        assertEquals(UvLevel.LOW, UvLevel.of(2.0))
        assertEquals(UvLevel.MODERATE, UvLevel.of(3.0))
        assertEquals(UvLevel.MODERATE, UvLevel.of(5.0))
        assertEquals(UvLevel.HIGH, UvLevel.of(6.0))
        assertEquals(UvLevel.HIGH, UvLevel.of(7.0))
        assertEquals(UvLevel.VERY_HIGH, UvLevel.of(8.0))
        assertEquals(UvLevel.VERY_HIGH, UvLevel.of(10.0))
        assertEquals(UvLevel.EXTREME, UvLevel.of(11.0))
        assertEquals(UvLevel.EXTREME, UvLevel.of(14.0))
    }

    @Test
    fun `ondalikli deger once yuvarlanir`() {
        // 2.4 → 2 "Düşük"; 2.6 → 3 "Orta"; 5.6 → 6 "Yüksek".
        assertEquals(UvLevel.LOW, UvLevel.of(2.4))
        assertEquals(UvLevel.MODERATE, UvLevel.of(2.6))
        assertEquals(UvLevel.HIGH, UvLevel.of(5.6))
    }
}
