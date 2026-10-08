package com.abilas.connecthub

import android.net.Uri
import android.os.Message
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.abilas.connecthub.databinding.ViewPopupBinding

/**
 * Shows windows a page opens with window.open or target="_blank" in a full-screen layer.
 *
 * Sign-in with Steam, PlayStation, Xbox and similar providers runs in such a window and
 * reports back to the page that opened it, so the window must stay in this app.
 */
class PopupController(
    private val binding: ViewPopupBinding,
    private val host: WebHost,
) {
    private var webView: WebView? = null

    val isOpen: Boolean get() = webView != null

    init {
        binding.popupToolbar.setNavigationOnClickListener { close() }
    }

    fun open(resultMsg: Message): Boolean {
        close()
        val view = WebView(binding.root.context).apply {
            applyDefaultSettings(allowPopups = false)
            webViewClient = PopupClient()
            webChromeClient = PopupChromeClient()
        }
        binding.popupContainer.addView(
            view,
            0,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        webView = view
        binding.popupToolbar.title = null
        binding.popupToolbar.subtitle = null
        binding.root.isVisible = true

        val transport = resultMsg.obj as WebView.WebViewTransport
        transport.webView = view
        resultMsg.sendToTarget()
        return true
    }

    fun close() {
        val view = webView ?: return
        webView = null
        binding.root.isVisible = false
        binding.popupProgress.isVisible = false
        binding.popupContainer.removeView(view)
        view.destroy()
    }

    /** Returns true if the popup consumed the back press. */
    fun handleBack(): Boolean {
        val view = webView ?: return false
        if (view.canGoBack()) view.goBack() else close()
        return true
    }

    // Web view callbacks must not destroy the web view that is calling them.
    private fun closeLater() {
        val target = webView ?: return
        binding.root.post { if (webView === target) close() }
    }

    private inner class PopupClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!request.isForMainFrame) return false
            val url = request.url.toString()
            if (UrlPolicy.targetFor(url) == UrlPolicy.Target.IN_APP) return false
            host.openExternally(url)
            // A window opened only to show an external link would otherwise stay blank.
            if (view.url.isNullOrEmpty() || view.url == "about:blank") closeLater()
            return true
        }

        override fun onPageFinished(view: WebView, url: String?) {
            binding.popupToolbar.subtitle = url?.toUri()?.host
        }
    }

    private inner class PopupChromeClient : WebChromeClient() {
        override fun onReceivedTitle(view: WebView, title: String?) {
            binding.popupToolbar.title = title
        }

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            binding.popupProgress.isVisible = newProgress < 100
            binding.popupProgress.setProgressCompat(newProgress, true)
        }

        override fun onCloseWindow(window: WebView) {
            if (window === webView) closeLater()
        }

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams,
        ): Boolean = host.showFileChooser(filePathCallback, fileChooserParams)
    }
}
