package io.homeassistant.companion.android.widgets.grid

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dagger.hilt.android.AndroidEntryPoint
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.database.widget.GridWidgetDao
import io.homeassistant.companion.android.widgets.common.WidgetAuthenticationActivity
import java.util.regex.Pattern
import javax.inject.Inject
import kotlin.text.split
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GridWidget : AppWidgetProvider() {
    companion object {
        private const val TAG = "GridWidget"
        const val CALL_SERVICE =
            "io.homeassistant.companion.android.widgets.grid.GridWidget.CALL_SERVICE"
        const val CALL_SERVICE_AUTH =
            "io.homeassistant.companion.android.widgets.grid.GridWidget.CALL_SERVICE_AUTH"
        const val EXTRA_ACTION_ID =
            "io.homeassistant.companion.android.widgets.grid.GridWidget.EXTRA_ACTION_ID"
    }

    @Inject
    lateinit var serverManager: ServerManager

    @Inject
    lateinit var gridWidgetDao: GridWidgetDao

    private val mainScope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val actionId = intent.getIntExtra(EXTRA_ACTION_ID, -1)

        super.onReceive(context, intent)
        when (action) {
            CALL_SERVICE_AUTH -> authThenCallConfiguredAction(context, appWidgetId, actionId)
            CALL_SERVICE -> callConfiguredAction(appWidgetId, actionId)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val gridConfig = gridWidgetDao.get(appWidgetId)?.asGridConfiguration()
            appWidgetManager.updateAppWidget(appWidgetId, gridConfig.asRemoteViews(context, appWidgetId))
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetId: Int, newOptions: Bundle?) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
    }

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
    }

    override fun onRestored(context: Context?, oldWidgetIds: IntArray?, newWidgetIds: IntArray?) {
        super.onRestored(context, oldWidgetIds, newWidgetIds)
    }

    private fun authThenCallConfiguredAction(context: Context, appWidgetId: Int, actionId: Int) {
        Log.d(TAG, "Calling authentication, then configured action")

        val extras = Bundle().apply {
            putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putInt(EXTRA_ACTION_ID, actionId)
        }
        val intent = Intent(context, WidgetAuthenticationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
            putExtra(WidgetAuthenticationActivity.EXTRA_TARGET, GridWidget::class.java)
            putExtra(WidgetAuthenticationActivity.EXTRA_ACTION, CALL_SERVICE)
            putExtra(WidgetAuthenticationActivity.EXTRA_EXTRAS, extras)
        }
        context.startActivity(intent)
    }

    private fun callConfiguredAction(appWidgetId: Int, actionId: Int) {
        Log.d(TAG, "Calling widget action")

        val widget = gridWidgetDao.get(appWidgetId)
        val item = widget?.items?.find { it.id == actionId }

        mainScope.launch {
            // Load the action call data from Shared Preferences
            val domain = item?.domain
            val action = item?.service
            val actionDataJson = item?.serviceData

            Log.d(
                TAG,
                "Action Call Data loaded:" + System.lineSeparator() +
                    "domain: " + domain + System.lineSeparator() +
                    "action: " + action + System.lineSeparator() +
                    "action_data: " + actionDataJson
            )

            if (domain == null || action == null || actionDataJson == null) {
                Log.w(TAG, "Action Call Data incomplete.  Aborting action call")
            } else {
                // If everything loaded correctly, package the action data and attempt the call
                try {
                    // Convert JSON to HashMap
                    val actionDataMap: HashMap<String, Any> =
                        jacksonObjectMapper().readValue(actionDataJson)

                    if (actionDataMap["entity_id"] != null) {
                        val entityIdWithoutBrackets = Pattern.compile("\\[(.*?)\\]")
                            .matcher(actionDataMap["entity_id"].toString())
                        if (entityIdWithoutBrackets.find()) {
                            val value = entityIdWithoutBrackets.group(1)
                            if (value != null) {
                                if (value == "all" ||
                                    value.split(",").contains("all")
                                ) {
                                    actionDataMap["entity_id"] = "all"
                                }
                            }
                        }
                    }

                    Log.d(TAG, "Sending action call to Home Assistant")
                    serverManager.integrationRepository(widget.gridWidget.serverId).callAction(domain, action, actionDataMap)
                    Log.d(TAG, "Action call sent successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to call action", e)
                }
            }
        }
    }
}
