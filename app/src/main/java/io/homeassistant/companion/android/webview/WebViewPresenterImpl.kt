package io.homeassistant.companion.android.webview

import android.net.Uri
import android.util.Log
import io.homeassistant.companion.android.domain.authentication.AuthenticationUseCase
import io.homeassistant.companion.android.domain.authentication.SessionState
import io.homeassistant.companion.android.domain.integration.IntegrationUseCase
import io.homeassistant.companion.android.domain.url.UrlUseCase
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class WebViewPresenterImpl @Inject constructor(
    private val view: WebView,
    private val urlUseCase: UrlUseCase,
    private val integrationUseCase: IntegrationUseCase,
    private val authenticationUseCase: AuthenticationUseCase
) : WebViewPresenter {

    companion object {
        private const val TAG = "WebViewPresenterImpl"
    }

    private val mainScope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onViewReady() {
        mainScope.launch {
            val url = urlUseCase.getUrl()

            view.loadUrl(
                Uri.parse(url.toString())
                    .buildUpon()
                    .appendQueryParameter("external_auth", "1")
                    .build()
                    .toString()
            )
        }
    }

    override fun onGetExternalAuth(callback: String) {
        mainScope.launch {
            try {
                view.setExternalAuth("$callback(true, ${authenticationUseCase.retrieveExternalAuthentication()})")
            } catch (e: Exception) {
                Log.e(TAG, "Unable to retrieve external auth", e)
                view.setExternalAuth("$callback(false)")
                view.showError(authenticationUseCase.getSessionState() == SessionState.ANONYMOUS)
            }
        }
    }

    override fun onRevokeExternalAuth(callback: String) {
        mainScope.launch {
            try {
                authenticationUseCase.revokeSession()
                view.setExternalAuth("$callback(true)")
                view.openOnBoarding()
            } catch (e: Exception) {
                Log.e(TAG, "Unable to revoke session", e)
                view.setExternalAuth("$callback(false)")
            }
        }
    }

    override fun clearKnownUrls() {
        mainScope.launch {
            urlUseCase.saveUrl("", true)
            urlUseCase.saveUrl("", false)
        }
    }

    override fun isFS(key: String): Boolean {
        return runBlocking {
            return@runBlocking when (key) {
                "fullscreen" -> integrationUseCase.isFullScreenEnabled()
                else -> throw Exception()
            }
        }
    }

    override fun onFinish() {
        mainScope.cancel()
    }
}
