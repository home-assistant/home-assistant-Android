package io.homeassistant.companion.android.shortcuts

import io.homeassistant.companion.android.domain.integration.IntegrationUseCase
import io.homeassistant.companion.android.domain.integration.Panel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class ShortcutsPresenterImpl @Inject constructor(
    private val integrationUseCase: IntegrationUseCase
) : ShortcutsPresenter {

    companion object {
        private const val TAG = "ShortcutsPresenter"
    }

    private val mainScope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate() {
        mainScope.launch {}
    }

    override fun onFinish() {
        mainScope.cancel()
    }

    override fun getPanels(): Array<Panel> {
        return runBlocking {
            integrationUseCase.getPanels()
        }
    }
}
