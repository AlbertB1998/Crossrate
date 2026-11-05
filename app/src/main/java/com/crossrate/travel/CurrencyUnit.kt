package com.crossrate.travel

enum class CurrencyUnit(
    val country: String,
    val fullName: String,
    val flag: String,
    val symbol: String
) {
    USD("United States", "US Dollar", "🇺🇸", "US$"),
    EUR("Germany / Eurozone", "Euro", "🇩🇪", "€"),
    MXN("Mexico", "Mexican Peso", "🇲🇽", "MX$"),
    ARS("Argentina", "Argentine Peso", "🇦🇷", "AR$")
}
