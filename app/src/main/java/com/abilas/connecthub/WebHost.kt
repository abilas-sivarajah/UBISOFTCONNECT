package com.abilas.connecthub

import android.net.Uri
import android.os.Message
import android.webkit.ValueCallback
import android.webkit.WebChromeClient

/** What a web view needs from the activity that hosts it. */
interface WebHost {
    fun onTabChanged(tab: BrowserTab)
    fun openExternally(url: String)
    fun showFileChooser(callback: ValueCallback<Array<Uri>>, params: WebChromeClient.FileChooserParams): Boolean
    fun openPopup(resultMsg: Message): Boolean
}
