package com.abilas.connecthub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class UbisoftLinksTest {

    @Test
    fun germanyUsesGermanSiteAndGermanStore() {
        val links = UbisoftLinks(Locale.GERMANY)
        assertEquals("de-de", links.siteLocale)
        assertEquals("https://www.ubisoft.com/de-de/account", links.account)
        assertEquals("https://news.ubisoft.com/de-de/", links.news)
        assertEquals("https://store.ubisoft.com/de/home", links.storeHome)
        assertEquals("https://www.ubisoft.com/de-de/help/connectivity-and-performance/server-status", links.serverStatus)
    }

    @Test
    fun austriaHasItsOwnStore() {
        val links = UbisoftLinks(Locale.forLanguageTag("de-AT"))
        assertEquals("de-de", links.siteLocale)
        assertEquals("https://store.ubisoft.com/at/units-discount", links.unitsDiscount)
    }

    @Test
    fun countryWithoutStorePathLetsUbisoftPickByLocation() {
        val links = UbisoftLinks(Locale.forLanguageTag("de-CH"))
        assertEquals("de-de", links.siteLocale)
        assertNull(links.storeRegion)
        assertEquals("https://store.ubisoft.com/home", links.storeHome)
        assertEquals("https://store.ubisoft.com/free-games", links.freeGames)
    }

    @Test
    fun exactSiteLocalesAreKept() {
        assertEquals("en-us", UbisoftLinks.siteLocaleFor(Locale.US))
        assertEquals("en-gb", UbisoftLinks.siteLocaleFor(Locale.UK))
        assertEquals("fr-ca", UbisoftLinks.siteLocaleFor(Locale.CANADA_FRENCH))
        assertEquals("ja-jp", UbisoftLinks.siteLocaleFor(Locale.JAPAN))
    }

    @Test
    fun regionalVariantsFallBackToTheClosestSiteLocale() {
        assertEquals("es-mx", UbisoftLinks.siteLocaleFor(Locale.forLanguageTag("es-AR")))
        assertEquals("es-mx", UbisoftLinks.siteLocaleFor(Locale.forLanguageTag("es-US")))
        assertEquals("es-es", UbisoftLinks.siteLocaleFor(Locale.forLanguageTag("es-ES")))
        assertEquals("zh-tw", UbisoftLinks.siteLocaleFor(Locale.forLanguageTag("zh-HK")))
        assertEquals("zh-tw", UbisoftLinks.siteLocaleFor(Locale.forLanguageTag("zh-Hant")))
        assertEquals("zh-cn", UbisoftLinks.siteLocaleFor(Locale.forLanguageTag("zh-SG")))
        assertEquals("fr-fr", UbisoftLinks.siteLocaleFor(Locale.forLanguageTag("fr-BE")))
        assertEquals("pt-br", UbisoftLinks.siteLocaleFor(Locale.forLanguageTag("pt-PT")))
        assertEquals("en-gb", UbisoftLinks.siteLocaleFor(Locale.forLanguageTag("en-IN")))
        assertEquals("en-us", UbisoftLinks.siteLocaleFor(Locale.ENGLISH))
    }

    @Test
    fun unsupportedLanguagesUseEnglish() {
        assertEquals("en-gb", UbisoftLinks.siteLocaleFor(Locale.forLanguageTag("sv-SE")))
        assertEquals("en-gb", UbisoftLinks.siteLocaleFor(Locale.forLanguageTag("nl-NL")))
        assertEquals("en-us", UbisoftLinks.siteLocaleFor(Locale.forLanguageTag("vi")))
    }

    @Test
    fun storeRegions() {
        assertEquals("uk", UbisoftLinks.storeRegionFor("GB"))
        assertEquals("anz", UbisoftLinks.storeRegionFor("NZ"))
        assertEquals("de", UbisoftLinks.storeRegionFor("de"))
        assertNull(UbisoftLinks.storeRegionFor(""))
        assertNull(UbisoftLinks.storeRegionFor("BR"))
    }
}
