package com.abilas.connecthub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class UserAgentsTest {

    @Test
    fun desktopUserAgentKeepsTheWebViewChromeVersion() {
        val mobile = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Build/AP2A; wv) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Version/4.0 Chrome/129.0.6668.100 Mobile Safari/537.36"
        val desktop = UserAgents.desktop(mobile)
        assertEquals(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/129.0.6668.100 Safari/537.36",
            desktop,
        )
        assertFalse(desktop.contains("Mobile"))
    }

    @Test
    fun unknownUserAgentStillProducesADesktopOne() {
        assertFalse(UserAgents.desktop("something else").contains("Android"))
    }
}
