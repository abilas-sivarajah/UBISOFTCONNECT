package com.abilas.connecthub

object UserAgents {

    private val CHROME_VERSION = Regex("""Chrome/([\d.]+)""")

    /**
     * A desktop Chrome user agent matching the Chrome version of the device's web view,
     * so sites that hide features from phones serve their full layout.
     */
    fun desktop(mobileUserAgent: String): String {
        val version = CHROME_VERSION.find(mobileUserAgent)?.groupValues?.get(1) ?: "130.0.0.0"
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/$version Safari/537.36"
    }
}
