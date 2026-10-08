package com.abilas.connecthub

import com.abilas.connecthub.UrlPolicy.Target.EXTERNAL
import com.abilas.connecthub.UrlPolicy.Target.IN_APP
import org.junit.Assert.assertEquals
import org.junit.Test

class UrlPolicyTest {

    @Test
    fun ubisoftPagesStayInApp() {
        assertEquals(IN_APP, UrlPolicy.targetFor("https://www.ubisoft.com/de-de/account"))
        assertEquals(IN_APP, UrlPolicy.targetFor("https://store.ubisoft.com/de/home"))
        assertEquals(IN_APP, UrlPolicy.targetFor("https://connect.ubisoft.com/login?appId=x"))
    }

    @Test
    fun signInAndPaymentPagesStayInApp() {
        assertEquals(IN_APP, UrlPolicy.targetFor("https://steamcommunity.com/openid/login?openid.mode=checkid_setup"))
        assertEquals(IN_APP, UrlPolicy.targetFor("https://login.live.com/oauth20_authorize.srf"))
        assertEquals(IN_APP, UrlPolicy.targetFor("https://www.paypal.com/checkoutnow?token=1"))
        assertEquals(IN_APP, UrlPolicy.targetFor("https://acs.some-bank.example/3ds"))
    }

    @Test
    fun socialAndAppStoreLinksOpenExternally() {
        assertEquals(EXTERNAL, UrlPolicy.targetFor("https://www.youtube.com/watch?v=abc"))
        assertEquals(EXTERNAL, UrlPolicy.targetFor("https://m.youtube.com/watch?v=abc"))
        assertEquals(EXTERNAL, UrlPolicy.targetFor("https://youtu.be/abc"))
        assertEquals(EXTERNAL, UrlPolicy.targetFor("https://X.com/Ubisoft"))
        assertEquals(EXTERNAL, UrlPolicy.targetFor("https://discord.gg/invite"))
        assertEquals(EXTERNAL, UrlPolicy.targetFor("https://play.google.com/store/apps/details?id=x"))
    }

    @Test
    fun lookalikeHostsAreNotTreatedAsExternalSites() {
        assertEquals(IN_APP, UrlPolicy.targetFor("https://notyoutube.com/"))
        assertEquals(IN_APP, UrlPolicy.targetFor("https://youtube.com.example.org/"))
    }

    @Test
    fun nonWebSchemesGoToOtherApps() {
        assertEquals(EXTERNAL, UrlPolicy.targetFor("mailto:support@example.com"))
        assertEquals(EXTERNAL, UrlPolicy.targetFor("tel:+491234"))
        assertEquals(EXTERNAL, UrlPolicy.targetFor("intent://scan/#Intent;scheme=zxing;end"))
        assertEquals(EXTERNAL, UrlPolicy.targetFor("market://details?id=x"))
        assertEquals(EXTERNAL, UrlPolicy.targetFor("steam://run/123"))
    }

    @Test
    fun webViewInternalSchemesStayInApp() {
        assertEquals(IN_APP, UrlPolicy.targetFor("about:blank"))
        assertEquals(IN_APP, UrlPolicy.targetFor("blob:https://www.ubisoft.com/1234"))
        assertEquals(IN_APP, UrlPolicy.targetFor("javascript:void(0)"))
    }

    @Test
    fun unparsableUrlsAreLeftToTheWebView() {
        assertEquals(IN_APP, UrlPolicy.targetFor("https://www.ubisoft.com/search?q=a b|c"))
    }
}
