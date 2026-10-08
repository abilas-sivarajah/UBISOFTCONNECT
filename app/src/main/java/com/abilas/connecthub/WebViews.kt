package com.abilas.connecthub

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView

/** Settings shared by the tab web views and the popup web view. */
@SuppressLint("SetJavaScriptEnabled")
fun WebView.applyDefaultSettings(allowPopups: Boolean) {
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        setSupportMultipleWindows(allowPopups)
        javaScriptCanOpenWindowsAutomatically = allowPopups
        loadWithOverviewMode = true
        useWideViewPort = true
        builtInZoomControls = true
        displayZoomControls = false
        allowFileAccess = false
        allowContentAccess = false
    }
    // Ubisoft's single sign-on spans ubisoft.com, store.ubisoft.com and connect.ubisoft.com.
    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(this@applyDefaultSettings, true)
    }
}
