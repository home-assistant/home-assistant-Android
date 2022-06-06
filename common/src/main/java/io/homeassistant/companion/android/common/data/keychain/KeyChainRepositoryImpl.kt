package io.homeassistant.companion.android.common.data.keychain

import android.content.Context
import android.security.KeyChain
import io.homeassistant.companion.android.common.data.prefs.PrefsRepository
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class KeyChainRepositoryImpl @Inject constructor(
    private val prefsRepository: PrefsRepository
) : KeyChainRepository {

    private var alias: String? = null
    private var key: PrivateKey? = null
    private var chain: Array<X509Certificate>? = null
    private var isLoading: Boolean = false

    override suspend fun getAlias(): String? {
        return alias
    }

    override suspend fun load(context: Context, alias: String): Boolean {
        if (alias == null) return isLoading

        this.alias = alias
        prefsRepository.saveKeyAlias(alias)

        return load(context)
    }

    override suspend fun load(context: Context): Boolean {
        if (alias == null) {
            alias = prefsRepository.getKeyAlias()
        }

        if (alias != null && !isLoading && (key == null || chain == null)) {
            isLoading = true // TODO: need proper sync
            var executor = Executors.newSingleThreadExecutor()
            executor.execute {
                if (chain == null) {
                    chain = KeyChain.getCertificateChain(context, alias!!)
                }
                if (key == null) {
                    key = KeyChain.getPrivateKey(context, alias!!)
                }
                isLoading = false
            }
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)
        }

        return isLoading
    }

    override suspend fun getPrivateKey(): PrivateKey? {
        return key
    }

    override suspend fun getCertificateChain(): Array<X509Certificate>? {
        return chain
    }
}
