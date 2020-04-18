package io.homeassistant.companion.android.domain.url

import java.net.URL

interface UrlUseCase {

    suspend fun getApiUrls(): Array<URL>

    suspend fun getBaseApiUrls(): Map<String, String>

    suspend fun saveRegistrationUrls(
        cloudHookUrl: String?, remoteUiUrl: String?, webhookId: String, localUrl: String? = null
    )

    suspend fun getUrl(isInternal: Boolean? = null): URL?

    suspend fun saveUrl(url: String, isInternal: Boolean? = null)

    suspend fun getHomeWifiSsids(): Set<String>

    suspend fun saveHomeWifiSsids(ssid: Set<String>)
}
