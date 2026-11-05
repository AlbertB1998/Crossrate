package com.crossrate.travel

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class FxMathTest {
    private val quotes = mapOf(
        "USD" to BigDecimal.ONE,
        "EUR" to BigDecimal("0.9"),
        "MXN" to BigDecimal("20"),
        "ARS" to BigDecimal("1000")
    )

    @Test fun usdIntoThreeOtherCurrencies() {
        assertEquals("90.00", FxMath.formatAmount(FxMath.convert(BigDecimal("100"), "USD", "EUR", quotes)!!))
        assertEquals("2,000.00", FxMath.formatAmount(FxMath.convert(BigDecimal("100"), "USD", "MXN", quotes)!!))
        assertEquals("100,000.00", FxMath.formatAmount(FxMath.convert(BigDecimal("100"), "USD", "ARS", quotes)!!))
    }

    @Test fun crossRatesDoNotDependOnSourceSelection() {
        assertEquals("45.00", FxMath.formatAmount(FxMath.convert(BigDecimal("1000"), "MXN", "EUR", quotes)!!))
        assertEquals("1.00", FxMath.formatAmount(FxMath.convert(BigDecimal("1000"), "ARS", "USD", quotes)!!))
    }

    @Test fun europeanDecimalSeparatorsWorkWithoutGuessingThousands() {
        assertEquals(BigDecimal("12.50"), FxMath.parseAmount("12,50"))
        assertNull(FxMath.parseAmount("1,234.50"))
        assertNull(FxMath.parseAmount("1.234,50"))
        assertNull(FxMath.parseAmount(""))
        assertNull(FxMath.parseAmount("-10"))
    }

    @Test fun missingRateNeverSilentlyDefaultsToOne() {
        assertNull(FxMath.convert(BigDecimal.ONE, "USD", "CHF", quotes))
        assertNull(FxMath.convert(BigDecimal.ONE, "XYZ", "USD", quotes))
    }
}
