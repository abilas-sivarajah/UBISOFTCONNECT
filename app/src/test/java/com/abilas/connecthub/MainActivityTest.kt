package com.abilas.connecthub

import android.view.View
import android.webkit.WebView
import android.widget.GridLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "de-rDE")
class MainActivityTest {

    private val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    private val activity = controller.get()
    private val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottom_nav)
    private val dashboard = activity.findViewById<View>(R.id.dashboard)
    private val webContainer = activity.findViewById<android.view.ViewGroup>(R.id.web_container)

    private fun visibleWebView(): WebView {
        val tabs = (0 until webContainer.childCount).map(webContainer::getChildAt).filter { it.isVisible }
        assertEquals(1, tabs.size)
        return findWebView(tabs.single()) ?: error("no web view in visible tab")
    }

    private fun findWebView(view: View): WebView? = when (view) {
        is WebView -> view
        is android.view.ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { findWebView(view.getChildAt(it)) }
        else -> null
    }

    private fun toolbarTitle() = activity.findViewById<Toolbar>(R.id.toolbar).title.toString()

    @Test
    fun startsOnTheDashboardWithoutLoadingAnyPage() {
        assertTrue(dashboard.isVisible)
        assertEquals(0, webContainer.childCount)
        assertEquals("Connect Hub", toolbarTitle())
        assertEquals(9, activity.findViewById<GridLayout>(R.id.shortcut_grid).childCount)
    }

    @Test
    fun storeTabLoadsTheLocalStore() {
        bottomNav.selectedItemId = R.id.nav_store
        assertFalse(dashboard.isVisible)
        assertEquals("Store", toolbarTitle())
        assertEquals("https://store.ubisoft.com/de/home", shadowOf(visibleWebView()).lastLoadedUrl)
    }

    @Test
    fun signInButtonOpensTheAccountPage() {
        activity.findViewById<View>(R.id.sign_in_button).performClick()
        assertEquals(R.id.nav_account, bottomNav.selectedItemId)
        assertEquals("Konto", toolbarTitle())
        assertEquals("https://www.ubisoft.com/de-de/account", shadowOf(visibleWebView()).lastLoadedUrl)
    }

    @Test
    fun shortcutOpensItsPageInTheMatchingTab() {
        val grid = activity.findViewById<GridLayout>(R.id.shortcut_grid)
        grid.getChildAt(6).performClick() // Serverstatus
        assertEquals(R.id.nav_help, bottomNav.selectedItemId)
        assertEquals(
            "https://www.ubisoft.com/de-de/help/connectivity-and-performance/server-status",
            shadowOf(visibleWebView()).lastLoadedUrl,
        )
        // Only the help tab was created; the shortcut's URL replaced its home page.
        assertEquals(1, webContainer.childCount)
    }

    @Test
    fun tabsKeepTheirWebViewsWhenSwitching() {
        bottomNav.selectedItemId = R.id.nav_store
        val store = visibleWebView()
        bottomNav.selectedItemId = R.id.nav_discover
        assertEquals("https://news.ubisoft.com/de-de/", shadowOf(visibleWebView()).lastLoadedUrl)
        bottomNav.selectedItemId = R.id.nav_store
        assertTrue(store === visibleWebView())
        assertEquals(2, webContainer.childCount)
    }

    @Test
    fun backFromAWebTabReturnsToTheDashboard() {
        bottomNav.selectedItemId = R.id.nav_help
        activity.onBackPressedDispatcher.onBackPressed()
        assertEquals(R.id.nav_start, bottomNav.selectedItemId)
        assertTrue(dashboard.isVisible)
        assertFalse(activity.isFinishing)
        activity.onBackPressedDispatcher.onBackPressed()
        assertTrue(activity.isFinishing)
    }

    @Test
    fun recreationRestoresTheOpenTab() {
        bottomNav.selectedItemId = R.id.nav_account
        controller.recreate()
        val recreated = controller.get()
        assertEquals(R.id.nav_account, recreated.findViewById<BottomNavigationView>(R.id.bottom_nav).selectedItemId)
        assertFalse(recreated.findViewById<View>(R.id.dashboard).isVisible)
    }
}
