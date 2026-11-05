# Crossrate for Android

An Android application for converting an amount entered in **USD, EUR, MXN or ARS** into the other three currencies at the same time.

## Features

- Choose any of the four currencies and enter an amount.
- See the other three converted values, plus their per-unit exchange rates.
- Tap any result to make it the source currency.
- Supports either `.` or `,` decimal notation; **do not add thousands separators**.
- Fetches daily mid-market reference rates from **Frankfurter v2**, without an API key.
- Remembers the last successfully downloaded rate table for use offline.
- Tells you which dates the rate data represents and when it was last checked.
- Clearly distinguishes ARS reference prices from Argentine blue-dollar and MEP quotations.
- No account, analytics, ads, geolocation or special permissions. Only internet access.

## Important distinction for Argentina

Frankfurter delivers *blended mid-market reference rates* once per day, not local informal cash rates (dólar blue), MEP brokerage rates, or your card issuer's settlement rate. All displayed conversions are estimates. Rates can be dated differently for different currencies, so the application reports the date range when appropriate. Rates do not update tick by tick.

## How rates are calculated

The app fetches the values of EUR, MXN and ARS **per one USD**. Conversion from currency A to B is:

`amount_in_B = amount_in_A * (units_of_B_per_USD / units_of_A_per_USD)`

No downloaded rate means no displayed conversion. A failed refresh leaves the last good cached data untouched.

## Technical details

- Kotlin, Jetpack Compose, Material 3
- Min Android: API 26 (Android 8)
- Target Android: API 36
- Gradle 8.13 / Android Gradle Plugin 8.13.2 / Kotlin 2.3.21
- Dependency versions are declared in `app/build.gradle.kts`.
- API: `https://api.frankfurter.dev/v2/rates?base=USD&quotes=EUR,MXN,ARS`
- Reference documentation: https://frankfurter.dev/
- Source code: `app/src/main/java/com/crossrate/travel/`
- Tests: `app/src/test/java/com/crossrate/travel/FxMathTest.kt`
- CI: `.github/workflows/android-apk.yml`
