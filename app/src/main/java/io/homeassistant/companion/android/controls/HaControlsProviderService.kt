package io.homeassistant.companion.android.controls

import android.os.Build
import android.service.controls.Control
import android.service.controls.ControlsProviderService
import android.service.controls.actions.ControlAction
import android.util.Log
import androidx.annotation.RequiresApi
import dagger.hilt.android.AndroidEntryPoint
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.IntegrationRepository
import io.homeassistant.companion.android.common.data.websocket.WebSocketRepository
import io.homeassistant.companion.android.common.data.websocket.impl.entities.AreaRegistryResponse
import io.homeassistant.companion.android.common.data.websocket.impl.entities.DeviceRegistryResponse
import io.homeassistant.companion.android.common.data.websocket.impl.entities.EntityRegistryResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import java.util.concurrent.Flow
import java.util.function.Consumer
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.R)
@AndroidEntryPoint
class HaControlsProviderService : ControlsProviderService() {

    companion object {
        private const val TAG = "HaConProService"
    }

    @Inject
    lateinit var integrationRepository: IntegrationRepository

    @Inject
    lateinit var webSocketRepository: WebSocketRepository

    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    private val domainToHaControl = mapOf(
        "automation" to DefaultSwitchControl,
        "camera" to null,
        "climate" to ClimateControl,
        "cover" to CoverControl,
        "fan" to FanControl,
        "input_boolean" to DefaultSwitchControl,
        "input_number" to DefaultSliderControl,
        "light" to LightControl,
        "lock" to LockControl,
        "media_player" to null,
        "remote" to null,
        "scene" to SceneControl,
        "script" to SceneControl,
        "switch" to DefaultSwitchControl,
        "vacuum" to VacuumControl
    )

    override fun createPublisherForAllAvailable(): Flow.Publisher<Control> {
        return Flow.Publisher { subscriber ->
            ioScope.launch {
                try {
                    val areaRegistry = webSocketRepository.getAreaRegistry()
                    val deviceRegistry = webSocketRepository.getDeviceRegistry()
                    val entityRegistry = webSocketRepository.getEntityRegistry()

                    integrationRepository
                        .getEntities()
                        .mapNotNull {
                            val domain = it.entityId.split(".")[0]
                            domainToHaControl[domain]?.createControl(
                                applicationContext,
                                it as Entity<Map<String, Any>>,
                                getAreaForEntity(it.entityId, areaRegistry, deviceRegistry, entityRegistry)
                            )
                        }
                        .forEach {
                            subscriber.onNext(it)
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting list of entities", e)
                }
                subscriber.onComplete()
            }
        }
    }

    override fun createPublisherFor(controlIds: MutableList<String>): Flow.Publisher<Control> {
        Log.d(TAG, "publisherFor $controlIds")
        return Flow.Publisher { subscriber ->
            subscriber.onSubscribe(object : Flow.Subscription {
                var running = true
                override fun request(n: Long) {
                    Log.d(TAG, "request $n")
                    ioScope.launch {
                        val entityFlow = integrationRepository.getEntityUpdates()
                        // Load up initial values
                        // This should use the cached values that we should store in the DB.
                        // For now we'll use the rest API
                        val areaRegistry = webSocketRepository.getAreaRegistry()
                        val deviceRegistry = webSocketRepository.getDeviceRegistry()
                        val entityRegistry = webSocketRepository.getEntityRegistry()
                        controlIds.forEach {
                            val entity = integrationRepository.getEntity(it)
                            val domain = it.split(".")[0]
                            val control = domainToHaControl[domain]?.createControl(
                                applicationContext,
                                entity,
                                getAreaForEntity(it, areaRegistry, deviceRegistry, entityRegistry)
                            )
                            subscriber.onNext(control)
                        }

                        // Listen for the state changed events.
                        entityFlow.takeWhile { running }.collect {
                            if (controlIds.contains(it.entityId)) {
                                val domain = it.entityId.split(".")[0]
                                val control = domainToHaControl[domain]?.createControl(
                                    applicationContext,
                                    it as Entity<Map<String, Any>>,
                                    getAreaForEntity(it.entityId, areaRegistry, deviceRegistry, entityRegistry)
                                )
                                subscriber.onNext(control)
                            }
                        }
                    }
                }

                override fun cancel() {
                    Log.d(TAG, "cancel")
                    running = false
                }
            })
        }
    }

    override fun performControlAction(
        controlId: String,
        action: ControlAction,
        consumer: Consumer<Int>
    ) {
        Log.d(TAG, "Control: $controlId, action: $action")
        val domain = controlId.split(".")[0]
        val haControl = domainToHaControl[domain]

        var actionSuccess = false
        if (haControl != null) {
            try {
                actionSuccess = haControl.performAction(integrationRepository, action)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to control or get entity information", e)
            }
        }
        if (actionSuccess) {
            consumer.accept(ControlAction.RESPONSE_OK)
        } else {
            consumer.accept(ControlAction.RESPONSE_UNKNOWN)
        }
    }

    private fun getAreaForEntity(
        entityId: String,
        areaRegistry: List<AreaRegistryResponse>,
        deviceRegistry: List<DeviceRegistryResponse>,
        entityRegistry: List<EntityRegistryResponse>
    ): AreaRegistryResponse? {
        val rEntity = entityRegistry.firstOrNull { it.entityId == entityId }
        if (rEntity != null) {
            if (rEntity.areaId != null) {
                return areaRegistry.firstOrNull { it.areaId == rEntity.entityId }
            } else if (rEntity.deviceId != null) {
                val rDevice = deviceRegistry.firstOrNull { it.id == rEntity.deviceId }
                if (rDevice != null) {
                    return areaRegistry.firstOrNull { it.areaId == rDevice.areaId }
                }
            }
        }
        return null
    }
}
