package com.abilas.connecthub

import java.util.Locale

/**
 * Entry points into Ubisoft's own web services, localized for the device.
 *
 * Ubisoft picks the language of www.ubisoft.com and the store from the URL path
 * (and from the visitor's IP when the path has none), not from Accept-Language,
 * so the path has to be chosen here.
 */
class UbisoftLinks(locale: Locale) {

    /** Path segment for www.ubisoft.com and news.ubisoft.com, e.g. "de-de". */
    val siteLocale: String = siteLocaleFor(locale)

    /** Path segment for store.ubisoft.com, or null to let Ubisoft pick by location. */
    val storeRegion: String? = storeRegionFor(locale.country)

    val account = "https://www.ubisoft.com/$siteLocale/account"
    val news = "https://news.ubisoft.com/$siteLocale/"
    val games = "https://www.ubisoft.com/$siteLocale/games"
    val siegeStats = "https://www.ubisoft.com/$siteLocale/game/rainbow-six/siege/stats"
    val pcClient = "https://www.ubisoft.com/$siteLocale/ubisoft-connect/download"
    val help = "https://www.ubisoft.com/$siteLocale/help"
    val contactSupport = "$help/contact"
    val serverStatus = "$help/connectivity-and-performance/server-status"

    val storeHome = store("home")
    val unitsDiscount = store("units-discount")
    val ubisoftPlus = store("ubisoftplus")
    val freeGames = store("free-games")

    private fun store(page: String): String =
        if (storeRegion == null) "https://store.ubisoft.com/$page"
        else "https://store.ubisoft.com/$storeRegion/$page"

    companion object {
        // Locales www.ubisoft.com serves itself; anything else falls back to en-gb there.
        private val SITE_LOCALES = setOf(
            "en-us", "en-gb", "en-au", "en-ca", "de-de", "fr-fr", "fr-ca", "es-es", "es-mx",
            "it-it", "pl-pl", "pt-br", "ja-jp", "ko-kr", "zh-cn", "zh-tw",
        )

        private val LANGUAGE_DEFAULTS = mapOf(
            "de" to "de-de", "fr" to "fr-fr", "es" to "es-es", "it" to "it-it", "pl" to "pl-pl",
            "pt" to "pt-br", "ja" to "ja-jp", "ko" to "ko-kr", "zh" to "zh-cn", "en" to "en-gb",
        )

        // Countries served by Ubisoft's Latin American Spanish (es-mx) rather than es-es.
        private val AMERICAS = setOf(
            "AR", "BO", "CL", "CO", "CR", "CU", "DO", "EC", "GT", "HN", "MX", "NI", "PA", "PE",
            "PR", "PY", "SV", "US", "UY", "VE",
        )

        private val TRADITIONAL_CHINESE = setOf("TW", "HK", "MO")

        // Countries with their own store.ubisoft.com path. Others are left to Ubisoft's
        // location-based redirect, which knows their currency better than we do.
        private val STORE_REGIONS = mapOf(
            "US" to "us", "CA" to "ca", "GB" to "uk", "IE" to "ie", "DE" to "de", "AT" to "at",
            "FR" to "fr", "ES" to "es", "IT" to "it", "NL" to "nl", "JP" to "jp", "KR" to "kr",
            "TR" to "tr", "AU" to "anz", "NZ" to "anz",
        )

        fun siteLocaleFor(locale: Locale): String {
            val language = locale.language.lowercase(Locale.ROOT)
            val country = locale.country.uppercase(Locale.ROOT)
            val exact = "$language-${country.lowercase(Locale.ROOT)}"
            return when {
                exact in SITE_LOCALES -> exact
                language == "es" && country in AMERICAS -> "es-mx"
                language == "zh" && (country in TRADITIONAL_CHINESE || locale.script == "Hant") -> "zh-tw"
                language == "en" && country.isEmpty() -> "en-us"
                language in LANGUAGE_DEFAULTS -> LANGUAGE_DEFAULTS.getValue(language)
                country.isEmpty() || country == "US" -> "en-us"
                else -> "en-gb"
            }
        }

        fun storeRegionFor(country: String): String? = STORE_REGIONS[country.uppercase(Locale.ROOT)]
    }
}
