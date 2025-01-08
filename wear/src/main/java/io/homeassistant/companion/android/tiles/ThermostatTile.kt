package io.homeassistant.companion.android.tiles

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material.Button
import androidx.wear.protolayout.material.ButtonColors
import androidx.wear.tiles.EventBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.common.data.prefs.WearPrefsRepository
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.database.AppDatabase
import io.homeassistant.companion.android.database.wear.ThermostatTile
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class ThermostatTile : TileService() {

    companion object {
        private const val TAG = "ThermostatTile"
        const val DEFAULT_REFRESH_INTERVAL = 600L
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    @Inject
    lateinit var serverManager: ServerManager

    @Inject
    lateinit var wearPrefsRepository: WearPrefsRepository

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<Tile> = serviceScope.future {
        val tileId = requestParams.tileId
        val thermostatTileDao = AppDatabase.getInstance(this@ThermostatTile).thermostatTileDao()
        val tileConfig = thermostatTileDao.get(tileId)

        val entity = tileConfig?.entityId?.let {
            serverManager.integrationRepository().getEntity(it)
        }

        if (requestParams.currentState.lastClickableId.isNotEmpty()) {
            if (wearPrefsRepository.getWearHapticFeedback()) hapticClick(applicationContext)
        }

        val lastId = requestParams.currentState.lastClickableId

        var targetTemp = tileConfig?.targetTemperature.toString()
        if (targetTemp == "null") targetTemp = entity?.attributes?.get("temperature").toString()

        if (lastId == "Up" || lastId == "Down") {
            val entityStr = entity?.entityId.toString()
            val stepSize = entity?.attributes?.get("target_temp_step").toString().toFloat()
            val updatedTargetTemp = targetTemp.toFloat() + if (lastId == "Up") +stepSize else -stepSize

            serverManager.integrationRepository().callAction(
                entityStr.split(".")[0],
                "set_temperature",
                hashMapOf(
                    "entity_id" to entityStr,
                    "temperature" to updatedTargetTemp
                )
            )
            val updated = tileConfig?.copy(targetTemperature = updatedTargetTemp) ?: ThermostatTile(id = tileId, targetTemperature = updatedTargetTemp)
            thermostatTileDao.add(updated)
            targetTemp = updatedTargetTemp.toString()
        } else {
            val updated = tileConfig?.copy(targetTemperature = null) ?: ThermostatTile(id = tileId, targetTemperature = null)
            thermostatTileDao.add(updated)
        }

        val freshness = when {
            (tileConfig?.refreshInterval != null && tileConfig.refreshInterval!! <= 1) -> 0
            tileConfig?.refreshInterval != null -> tileConfig.refreshInterval!!
            else -> DEFAULT_REFRESH_INTERVAL
        }

        Tile.Builder()
            .setResourcesVersion("$TAG$tileId.${System.currentTimeMillis()}")
            .setFreshnessIntervalMillis(TimeUnit.SECONDS.toMillis(freshness))
            .setTileTimeline(
                if (serverManager.isRegistered()) {
                    timeline(
                        tileConfig,
                        targetTemp
                    )
                } else {
                    loggedOutTimeline(
                        this@ThermostatTile,
                        requestParams,
                        R.string.thermostat,
                        R.string.thermostat_tile_log_in
                    )
                }
            )
            .build()
    }

    override fun onTileResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> = serviceScope.future {
        Resources.Builder()
            .setVersion(requestParams.version)
            .addIdToImageMapping(
                RESOURCE_REFRESH,
                ResourceBuilders.ImageResource.Builder()
                    .setAndroidResourceByResId(
                        ResourceBuilders.AndroidImageResourceByResId.Builder()
                            .setResourceId(io.homeassistant.companion.android.R.drawable.ic_refresh)
                            .build()
                    ).build()
            )
            .build()
    }

    override fun onTileAddEvent(requestParams: EventBuilders.TileAddEvent) = runBlocking {
        withContext(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(this@ThermostatTile).thermostatTileDao()
            if (dao.get(requestParams.tileId) == null) {
                dao.add(ThermostatTile(id = requestParams.tileId))
            } // else already existing, don't overwrite existing tile data
        }
    }

    override fun onTileRemoveEvent(requestParams: EventBuilders.TileRemoveEvent) = runBlocking {
        withContext(Dispatchers.IO) {
            AppDatabase.getInstance(this@ThermostatTile)
                .thermostatTileDao()
                .delete(requestParams.tileId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private suspend fun timeline(tileConfig: ThermostatTile?, targetTemperature: String): Timeline = Timeline.fromLayoutElement(
        LayoutElementBuilders.Box.Builder().apply {
            val entity = tileConfig?.entityId?.let {
                serverManager.integrationRepository().getEntity(it)
            }

            val currentTemperature = entity?.attributes?.get("current_temperature").toString()
            val hvacAction = entity?.attributes?.get("hvac_action").toString().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val friendlyName = entity?.attributes?.get("friendly_name").toString()
            val config = serverManager.webSocketRepository().getConfig()
            val temperatureUnit = config?.unitSystem?.getValue("temperature").toString()

            val hvacActionColor = when (hvacAction) {
                "Idle" -> getColor(R.color.colorWidgetButtonLabelWhite)
                "Heating" -> getColor(R.color.colorDeviceControlsThermostatHeat)
                "Cooling" -> getColor(R.color.colorDeviceControlsDefaultOn)
                else -> getColor(R.color.colorWidgetButtonLabelWhite)
            }

            if (tileConfig?.entityId.isNullOrBlank()) {
                addContent(
                    LayoutElementBuilders.Text.Builder()
                        .setText(getString(R.string.thermostat_tile_no_entity_yet))
                        .setMaxLines(10)
                        .build()
                )
            } else {
                addContent(
                    LayoutElementBuilders.Column.Builder()
                        .addContent(
                            LayoutElementBuilders.Text.Builder()
                                .setText(hvacAction)
                                .setFontStyle(LayoutElementBuilders.FontStyle.Builder().setColor(ColorBuilders.argb(hvacActionColor)).build())
                                .build()
                        )
                        .addContent(
                            LayoutElementBuilders.Text.Builder()
                                .setText(if (hvacAction == "Off") "--.- $temperatureUnit" else "$targetTemperature $temperatureUnit")
                                .setFontStyle(
                                    LayoutElementBuilders.FontStyle.Builder().setSize(
                                        DimensionBuilders.sp(30f)
                                    ).build()
                                )
                                .build()
                        )
                        .addContent(
                            LayoutElementBuilders.Text.Builder()
                                .setText("$currentTemperature $temperatureUnit")
                                .setFontStyle(
                                    LayoutElementBuilders.FontStyle.Builder()
                                        .setColor(ColorBuilders.argb(hvacActionColor))
                                        .build()
                                )
                                .build()
                        )
                        .addContent(
                            LayoutElementBuilders.Spacer.Builder()
                                .setHeight(DimensionBuilders.dp(10f)).build()
                        )
                        .addContent(
                            if (hvacAction != "Off") {
                                LayoutElementBuilders.Row.Builder()
                                    .addContent(getTempDownButton())
                                    .addContent(
                                        LayoutElementBuilders.Spacer.Builder()
                                            .setWidth(DimensionBuilders.dp(20f)).build()
                                    )
                                    .addContent(getTempUpButton())
                                    .build()
                            } else {
                                LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.dp(0f)).build()
                            }
                        )
                        .build()
                )
            }
            if (tileConfig?.showEntityName == true) {
                addContent(
                    LayoutElementBuilders.Arc.Builder()
                        .setAnchorAngle(
                            DimensionBuilders.DegreesProp.Builder(0f).build()
                        )
                        .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_CENTER)
                        .addContent(
                            LayoutElementBuilders.ArcText.Builder()
                                .setText(friendlyName)
                                .build()
                        )
                        .build()
                )
            }
            // Refresh button
            addContent(getRefreshButton())
            setModifiers(getRefreshModifiers())
        }.build()
    )

    private fun getTempUpButton(): LayoutElement {
        val clickable = Clickable.Builder()
            .setOnClick(ActionBuilders.LoadAction.Builder().build())
            .setId("Up")
            .build()

        return Button.Builder(this, clickable)
            .setTextContent("+")
            .setButtonColors(
                ButtonColors(
                    ColorBuilders.argb(getColor(R.color.colorPrimary)),
                    ColorBuilders.argb(getColor(R.color.colorWidgetButtonLabelBlack))
                )
            )
            .build()
    }

    private fun getTempDownButton(): LayoutElement {
        val clickable = Clickable.Builder()
            .setOnClick(ActionBuilders.LoadAction.Builder().build())
            .setId("Down")
            .build()

        return Button.Builder(this, clickable)
            .setTextContent("—")
            .setButtonColors(
                ButtonColors(
                    ColorBuilders.argb(getColor(R.color.colorPrimary)),
                    ColorBuilders.argb(getColor(R.color.colorWidgetButtonLabelBlack))
                )
            )
            .build()
    }
}