package com.abilas.connecthub

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.isVisible
import com.abilas.connecthub.databinding.ViewBrowserTabBinding

/** One bottom navigation destination: a web view with pull-to-refresh and an offline screen. */
class BrowserTab(
    context: Context,
    val section: Section,
    private val host: WebHost,
) {
    private val binding = ViewBrowserTabBinding.inflate(LayoutInflater.from(context))

    val view: View get() = binding.root

    var webView: WebView = createWebView(context)
        private set

    var progress = 100
        private set

    var isDesktopMode = false
        private set

    private var lastRequestedUrl: String? = null

    val url: String? get() = webView.url ?: lastRequestedUrl

    /** The page title, or null while the web view only knows the URL. */
    val title: String? get() = webView.title?.takeUnless { it.isBlank() || it == webView.url }

    init {
        binding.swipeRefresh.setOnRefreshListener { reload() }
        // The web view is not the refresh layout's direct child, so tell it when the page can scroll up.
        binding.swipeRefresh.setOnChildScrollUpCallback { _, _ ->
            !binding.errorView.isVisible && webView.canScrollVertically(-1)
        }
        binding.errorRetry.setOnClickListener { reload() }
    }

    fun load(url: String) {
        lastRequestedUrl = url
        hideError()
        webView.loadUrl(url)
    }

    fun reload() {
        hideError()
        if (webView.url == null) lastRequestedUrl?.let(webView::loadUrl) else webView.reload()
    }

    fun canGoBack(): Boolean = webView.canGoBack()

    fun goBack() {
        hideError()
        webView.goBack()
    }

    fun setDesktopMode(enabled: Boolean) {
        isDesktopMode = enabled
        applyUserAgent(webView)
        reload()
    }

    fun onPause() = webView.onPause()

    fun onResume() = webView.onResume()

    fun destroy() {
        binding.webContainer.removeView(webView)
        webView.destroy()
    }

    private fun createWebView(context: Context): WebView =
        WebView(context).apply {
            applyDefaultSettings(allowPopups = true)
            webViewClient = TabClient()
            webChromeClient = TabChromeClient()
            setDownloadListener { url, _, _, _, _ -> host.openExternally(url) }
            binding.webContainer.addView(
                this,
                0,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
        }

    private fun applyUserAgent(view: WebView) {
        view.settings.userAgentString =
            if (isDesktopMode) UserAgents.desktop(WebSettings.getDefaultUserAgent(view.context)) else null
    }

    private fun showError(description: CharSequence) {
        binding.errorMessage.text = description
        binding.errorView.isVisible = true
        binding.swipeRefresh.isRefreshing = false
    }

    private fun hideError() {
        binding.errorView.isVisible = false
    }

    private inner class TabClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!request.isForMainFrame) return false
            val url = request.url.toString()
            if (UrlPolicy.targetFor(url) == UrlPolicy.Target.IN_APP) return false
            host.openExternally(url)
            return true
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            hideError()
            host.onTabChanged(this@BrowserTab)
        }

        override fun onPageFinished(view: WebView, url: String?) {
            binding.swipeRefresh.isRefreshing = false
            host.onTabChanged(this@BrowserTab)
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            host.onTabChanged(this@BrowserTab)
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (request.isForMainFrame) showError(error.description)
        }

        // A crashed or killed renderer would otherwise take the whole app down.
        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            if (view !== webView) return true
            val lastUrl = view.url ?: lastRequestedUrl
            binding.webContainer.removeView(view)
            view.destroy()
            webView = createWebView(binding.root.context).also(::applyUserAgent)
            lastUrl?.let(::load)
            return true
        }
    }

    private inner class TabChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            progress = newProgress
            host.onTabChanged(this@BrowserTab)
        }

        override fun onReceivedTitle(view: WebView, title: String?) {
            host.onTabChanged(this@BrowserTab)
        }

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams,
        ): Boolean = host.showFileChooser(filePathCallback, fileChooserParams)

        override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean =
            host.openPopup(resultMsg)
    }
}
