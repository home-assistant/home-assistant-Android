package io.homeassistant.companion.android.common.data.authentication

import io.homeassistant.companion.android.common.data.authentication.impl.Session
import java.net.URL

interface AuthenticationRepository {

    suspend fun registerAuthorizationCode(authorizationCode: String)

    suspend fun retrieveExternalAuthentication(forceRefresh: Boolean): String

    suspend fun revokeSession()

    suspend fun getSessionState(): SessionState

    suspend fun buildAuthenticationUrl(callbackUrl: String): URL

    suspend fun buildBearerToken(): String

    suspend fun ensureValidSession(forceRefresh: Boolean = false): Session

    suspend fun setLockEnabled(enabled: Boolean)
    suspend fun isLockEnabled(): Boolean
}
