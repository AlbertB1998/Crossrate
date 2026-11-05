package com.crossrate.travel

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Pure conversion code, independent of Android and unit-testable. */
object FxMath {
    private val amountPattern = Regex("^[0-9]+([.,][0-9]{0,8})?$")

    /** Accept '.' or ',' as a decimal separator; forbid ambiguous grouping. */
    fun parseAmount(input: String): BigDecimal? {
        val value = input.trim()
        if (value.length > 24 || !amountPattern.matches(value)) return null
        return try {
            BigDecimal(value.replace(',', '.'))
        } catch (_: NumberFormatException) {
            null
        }
    }

    /** All rates represent units of currency for 1 USD. */
    fun convert(
        amount: BigDecimal,
        from: String,
        to: String,
        unitsPerUsd: Map<String, BigDecimal>
    ): BigDecimal? {
        val fromRate = unitsPerUsd[from] ?: return null
        val toRate = unitsPerUsd[to] ?: return null
        if (fromRate.signum() <= 0 || toRate.signum() <= 0) return null
        return amount.multiply(toRate, MathContext.DECIMAL128)
            .divide(fromRate, 16, RoundingMode.HALF_UP)
    }

    fun formatAmount(value: BigDecimal): String =
        DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US)).apply {
            roundingMode = RoundingMode.HALF_UP
        }.format(value)

    fun formatRate(value: BigDecimal): String =
        value.round(MathContext(7, RoundingMode.HALF_UP))
            .stripTrailingZeros().toPlainString()

    fun asInput(value: BigDecimal): String =
        value.setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros().toPlainString()
}
