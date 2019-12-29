package io.homeassistant.companion.android.domain.authentication

import java.net.URL
import javax.inject.Inject

class AuthenticationUseCaseImpl @Inject constructor(
    private val authenticationRepository: AuthenticationRepository
) : AuthenticationUseCase {

    override suspend fun registerAuthorizationCode(authorizationCode: String) {
        authenticationRepository.registerAuthorizationCode(authorizationCode)
    }

    override suspend fun retrieveExternalAuthentication(): String {
        return authenticationRepository.retrieveExternalAuthentication()
    }

    override suspend fun revokeSession() {
        authenticationRepository.revokeSession()
    }

    override suspend fun getSessionState(): SessionState {
        return authenticationRepository.getSessionState()
    }

    override suspend fun buildAuthenticationUrl(isInternal: Boolean, callbackUrl: String): URL {
        return authenticationRepository.buildAuthenticationUrl(isInternal, callbackUrl)
    }
}
