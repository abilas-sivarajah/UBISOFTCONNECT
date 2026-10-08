package com.abilas.connecthub

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.GridLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.abilas.connecthub.databinding.ActivityMainBinding
import com.abilas.connecthub.databinding.ItemShortcutBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.net.URISyntaxException
import java.util.EnumMap
import java.util.Locale

class MainActivity : AppCompatActivity(), WebHost {

    private lateinit var binding: ActivityMainBinding
    private lateinit var links: UbisoftLinks
    private lateinit var popup: PopupController

    private val tabs = EnumMap<Section, BrowserTab>(Section::class.java)
    private var current = Section.START

    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private val fileChooser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        fileCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
        fileCallback = null
    }

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val tab = tabs[current]
            when {
                popup.handleBack() -> Unit
                tab != null && tab.canGoBack() -> tab.goBack()
                current != Section.START -> binding.bottomNav.selectedItemId = Section.START.menuId
                else -> finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        applyWindowInsets()

        links = UbisoftLinks(resources.configuration.locales[0] ?: Locale.getDefault())
        popup = PopupController(binding.popup, this)
        buildDashboard()

        savedInstanceState?.let(::restoreTabs)
        binding.bottomNav.selectedItemId = current.menuId
        binding.bottomNav.setOnItemSelectedListener { item ->
            show(Section.forMenuId(item.itemId))
            true
        }
        binding.bottomNav.setOnItemReselectedListener { item ->
            val section = Section.forMenuId(item.itemId)
            val home = section.homeUrl(links)
            if (home == null) binding.dashboard.root.smoothScrollTo(0, 0) else tabs[section]?.load(home)
        }
        onBackPressedDispatcher.addCallback(this, backCallback)
        show(current)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Only the URLs: full web view state can exceed the binder transaction limit.
        outState.putString(STATE_SECTION, current.name)
        tabs.forEach { (section, tab) -> tab.url?.let { outState.putString(STATE_URL + section.name, it) } }
    }

    private fun restoreTabs(state: Bundle) {
        current = state.getString(STATE_SECTION)?.let(Section::valueOf) ?: Section.START
        Section.entries.forEach { section ->
            state.getString(STATE_URL + section.name)?.let { createTab(section, it) }
        }
    }

    override fun onResume() {
        super.onResume()
        currentTab()?.onResume()
    }

    override fun onPause() {
        tabs.values.forEach(BrowserTab::onPause)
        CookieManager.getInstance().flush()
        super.onPause()
    }

    override fun onDestroy() {
        popup.close()
        tabs.values.forEach(BrowserTab::destroy)
        tabs.clear()
        super.onDestroy()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { root, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val keyboardOpen = insets.isVisible(WindowInsetsCompat.Type.ime())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            // Edge-to-edge is mandatory from Android 15, so make room for bars and keyboard ourselves.
            root.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = if (keyboardOpen) keyboard.bottom else 0,
            )
            binding.bottomNav.isVisible = !keyboardOpen
            binding.bottomNav.updatePadding(bottom = bars.bottom)
            binding.popup.root.updatePadding(bottom = if (keyboardOpen) 0 else bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun buildDashboard() {
        binding.dashboard.signInButton.setOnClickListener { openIn(Section.ACCOUNT, links.account) }
        val margin = resources.getDimensionPixelSize(R.dimen.shortcut_margin)
        shortcuts().forEach { shortcut ->
            val item = ItemShortcutBinding.inflate(layoutInflater, binding.dashboard.shortcutGrid, false)
            item.icon.setImageResource(shortcut.icon)
            item.title.setText(shortcut.title)
            item.subtitle.setText(shortcut.subtitle)
            item.root.setOnClickListener { openIn(shortcut.section, shortcut.url) }
            item.root.layoutParams = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL),
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f),
            ).apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                setMargins(margin, margin, margin, margin)
            }
            binding.dashboard.shortcutGrid.addView(item.root)
        }
    }

    private fun shortcuts() = listOf(
        Shortcut(R.drawable.ic_account, R.string.shortcut_account, R.string.shortcut_account_sub, Section.ACCOUNT, links.account),
        Shortcut(R.drawable.ic_units, R.string.shortcut_units, R.string.shortcut_units_sub, Section.STORE, links.unitsDiscount),
        Shortcut(R.drawable.ic_membership, R.string.shortcut_ubisoft_plus, R.string.shortcut_ubisoft_plus_sub, Section.STORE, links.ubisoftPlus),
        Shortcut(R.drawable.ic_redeem, R.string.shortcut_free_games, R.string.shortcut_free_games_sub, Section.STORE, links.freeGames),
        Shortcut(R.drawable.ic_games, R.string.shortcut_games, R.string.shortcut_games_sub, Section.DISCOVER, links.games),
        Shortcut(R.drawable.ic_leaderboard, R.string.shortcut_siege_stats, R.string.shortcut_siege_stats_sub, Section.DISCOVER, links.siegeStats),
        Shortcut(R.drawable.ic_dns, R.string.shortcut_server_status, R.string.shortcut_server_status_sub, Section.HELP, links.serverStatus),
        Shortcut(R.drawable.ic_support, R.string.shortcut_support, R.string.shortcut_support_sub, Section.HELP, links.contactSupport),
        Shortcut(R.drawable.ic_desktop, R.string.shortcut_pc_app, R.string.shortcut_pc_app_sub, Section.DISCOVER, links.pcClient),
    )

    private fun openIn(section: Section, url: String) {
        val tab = tabs[section]
        if (tab == null) createTab(section, url) else tab.load(url)
        if (current != section) binding.bottomNav.selectedItemId = section.menuId
    }

    private fun createTab(section: Section, url: String): BrowserTab {
        val tab = BrowserTab(this, section, this)
        tab.view.isVisible = section == current
        binding.webContainer.addView(tab.view)
        tabs[section] = tab
        tab.load(url)
        return tab
    }

    private fun show(section: Section) {
        current = section
        val home = section.homeUrl(links)
        if (home != null && section !in tabs) createTab(section, home)
        binding.dashboard.root.isVisible = section == Section.START
        // Hidden tabs keep their page but stop running scripts and timers.
        tabs.forEach { (s, tab) ->
            tab.view.isVisible = s == section
            if (s == section) tab.onResume() else tab.onPause()
        }
        updateChrome()
        invalidateOptionsMenu()
    }

    private fun currentTab(): BrowserTab? = tabs[current]

    private fun updateChrome() {
        val tab = currentTab()
        binding.toolbar.title = getString(if (tab == null) R.string.app_name else current.label)
        binding.toolbar.subtitle = tab?.let { it.title ?: it.url?.toUri()?.host }
        val loading = tab != null && tab.progress < 100
        binding.progress.isVisible = loading
        if (loading) binding.progress.setProgressCompat(tab.progress, true)
    }

    override fun onTabChanged(tab: BrowserTab) {
        if (tab.section == current) updateChrome()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val tab = currentTab()
        WEB_ACTIONS.forEach { menu.findItem(it).isVisible = tab != null }
        menu.findItem(R.id.action_desktop).isChecked = tab?.isDesktopMode == true
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val tab = currentTab()
        when (item.itemId) {
            R.id.action_reload -> tab?.reload()
            R.id.action_open_browser -> tab?.url?.let(::openInBrowser)
            R.id.action_share -> tab?.url?.let(::shareLink)
            R.id.action_desktop -> {
                tab?.setDesktopMode(!tab.isDesktopMode)
                invalidateOptionsMenu()
            }
            R.id.action_sign_out -> confirmSignOut()
            R.id.action_about -> showAbout()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun openExternally(url: String) {
        val uri = url.toUri()
        when (uri.scheme?.lowercase(Locale.ROOT)) {
            "http", "https" -> launch(Intent(Intent.ACTION_VIEW, uri)) {
                CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(this, uri)
            }
            "intent" -> openIntentUri(url)
            else -> launch(Intent(Intent.ACTION_VIEW, uri)) { startActivity(it) }
        }
    }

    // intent:// links (used by app install and "open in app" buttons) carry their own fallback page.
    private fun openIntentUri(url: String) {
        val intent = try {
            Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                component = null
                selector = null
            }
        } catch (_: URISyntaxException) {
            return toast(R.string.no_app_found)
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            val fallback = intent.getStringExtra("browser_fallback_url")
            val tab = currentTab()
            if (fallback != null && tab != null && UrlPolicy.targetFor(fallback) == UrlPolicy.Target.IN_APP) {
                tab.load(fallback)
            } else {
                toast(R.string.no_app_found)
            }
        }
    }

    private fun openInBrowser(url: String) {
        launch(Intent(Intent.ACTION_VIEW, url.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)) { startActivity(it) }
    }

    private fun shareLink(url: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
        startActivity(Intent.createChooser(send, null))
    }

    private inline fun launch(intent: Intent, start: (Intent) -> Unit) {
        try {
            start(intent)
        } catch (_: ActivityNotFoundException) {
            toast(R.string.no_app_found)
        }
    }

    override fun showFileChooser(
        callback: ValueCallback<Array<Uri>>,
        params: WebChromeClient.FileChooserParams,
    ): Boolean {
        fileCallback?.onReceiveValue(null)
        fileCallback = callback
        return try {
            fileChooser.launch(params.createIntent())
            true
        } catch (_: ActivityNotFoundException) {
            fileCallback = null
            false
        }
    }

    override fun openPopup(resultMsg: Message): Boolean = popup.open(resultMsg)

    private fun confirmSignOut() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sign_out_title)
            .setMessage(R.string.sign_out_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.sign_out_confirm) { _, _ -> signOut() }
            .show()
    }

    private fun signOut() {
        popup.close()
        tabs.values.forEach {
            binding.webContainer.removeView(it.view)
            it.destroy()
        }
        tabs.clear()
        // The HTTP cache is shared by all web views of the app; any instance can clear it.
        WebView(this).apply {
            clearCache(true)
            destroy()
        }
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            if (!isDestroyed) {
                show(current)
                toast(R.string.signed_out)
            }
        }
    }

    private fun showAbout() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.about_title, getString(R.string.app_name), BuildConfig.VERSION_NAME))
            .setMessage(R.string.about_message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun toast(@StringRes message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private class Shortcut(
        @DrawableRes val icon: Int,
        @StringRes val title: Int,
        @StringRes val subtitle: Int,
        val section: Section,
        val url: String,
    )

    private companion object {
        const val STATE_SECTION = "section"
        const val STATE_URL = "url_"
        val WEB_ACTIONS = listOf(R.id.action_reload, R.id.action_open_browser, R.id.action_share, R.id.action_desktop)
    }
}
