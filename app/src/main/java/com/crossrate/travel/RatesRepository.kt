package com.crossrate.travel

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Fetches daily blended mid-market reference quotes from Frankfurter.
 * Rates are cached on the device; no login, location, or server is involved.
 */
class RatesRepository(context: Context) {
    private val preferences = context.getSharedPreferences("fx_cache", Context.MODE_PRIVATE)
    private val endpoint = "https://api.frankfurter.dev/v2/rates?base=USD&quotes=EUR,MXN,ARS"
    private val requiredQuotes = setOf("EUR", "MXN", "ARS")

    fun cached(): FxSnapshot? {
        return try {
        val raw = preferences.getString("last_good_snapshot", null) ?: return null
        val json = JSONObject(raw)
        val ratesObj = json.getJSONObject("rates")
        val datesObj = json.getJSONObject("dates")
        val rates = mutableMapOf("USD" to BigDecimal.ONE)
        val dates = mutableMapOf<String, String>()
        for (code in requiredQuotes) {
            val rate = BigDecimal(ratesObj.getString(code))
            if (rate.signum() <= 0) return null
            rates[code] = rate
            dates[code] = datesObj.getString(code)
        }
        FxSnapshot(rates, dates, json.getLong("savedAtMillis"))
        } catch (_: Exception) {
            null // Never show a partial or corrupted cached rate table.
        }
    }

    /** Call on a background thread; throws on network/API/validation failure. */
    fun fetchAndCache(): FxSnapshot {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "Crossrate-Android/1.0")
        try {
            if (connection.responseCode !in 200..299) {
                throw IOException("Exchange-rate service responded ${connection.responseCode}")
            }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val quotes = JSONArray(body)
            val rates = mutableMapOf("USD" to BigDecimal.ONE)
            val dates = mutableMapOf<String, String>()
            for (i in 0 until quotes.length()) {
                val item = quotes.getJSONObject(i)
                if (!item.getString("base").equals("USD", ignoreCase = true)) continue
                val quote = item.getString("quote").uppercase(Locale.ROOT)
                if (quote !in requiredQuotes) continue
                val rate = BigDecimal(item.getString("rate"))
                val date = item.getString("date")
                if (rate.signum() <= 0 || !date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                    throw IOException("Exchange-rate service returned invalid data")
                }
                rates[quote] = rate
                dates[quote] = date
            }
            if (!requiredQuotes.all { it in rates && it in dates }) {
                throw IOException("Exchange-rate service returned incomplete data")
            }
            val result = FxSnapshot(rates, dates, System.currentTimeMillis())
            val rateJson = JSONObject()
            val dateJson = JSONObject()
            for (code in requiredQuotes) {
                rateJson.put(code, requireNotNull(rates[code]).toPlainString())
                dateJson.put(code, dates[code])
            }
            val savedJson = JSONObject()
                .put("rates", rateJson)
                .put("dates", dateJson)
                .put("savedAtMillis", result.savedAtMillis)
            preferences.edit().putString("last_good_snapshot", savedJson.toString()).apply()
            return result
        } finally {
            connection.disconnect()
        }
    }
}

data class FxSnapshot(
    val unitsPerUsd: Map<String, BigDecimal>,
    val quoteDates: Map<String, String>,
    val savedAtMillis: Long
) {
    fun dateLabel(): String {
        val dates = quoteDates.values.sorted()
        return when {
            dates.isEmpty() -> "Unknown"
            dates.first() == dates.last() -> dates.first()
            else -> "${dates.first()} to ${dates.last()}"
        }
    }
}
