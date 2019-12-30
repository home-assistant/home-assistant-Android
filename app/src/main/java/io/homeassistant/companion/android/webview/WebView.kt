package io.homeassistant.companion.android.webview

interface WebView {

    fun loadUrl(url: String)

    fun setExternalAuth(script: String)

    fun openOnBoarding()

    fun getCurrentSsid(): String

    fun showError()
}
