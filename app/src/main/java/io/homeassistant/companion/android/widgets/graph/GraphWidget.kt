package io.homeassistant.companion.android.widgets.graph

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.AndroidEntryPoint
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.canSupportPrecision
import io.homeassistant.companion.android.common.data.integration.friendlyState
import io.homeassistant.companion.android.common.data.widgets.GraphWidgetRepository
import io.homeassistant.companion.android.database.widget.WidgetBackgroundType
import io.homeassistant.companion.android.database.widget.WidgetTapAction
import io.homeassistant.companion.android.database.widget.graph.GraphWidgetEntity
import io.homeassistant.companion.android.database.widget.graph.GraphWidgetHistoryEntity
import io.homeassistant.companion.android.database.widget.graph.GraphWidgetWithHistories
import io.homeassistant.companion.android.widgets.BaseWidgetProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GraphWidget : BaseWidgetProvider() {

    companion object {

        private const val TAG = "GraphWidget"

        internal const val EXTRA_SERVER_ID = "EXTRA_SERVER_ID"
        internal const val EXTRA_ENTITY_ID = "EXTRA_ENTITY_ID"
        internal const val EXTRA_LABEL = "EXTRA_LABEL"

        internal const val EXTRA_TIME_RANGE = "EXTRA_TIME_RANGE"

        private data class ResolvedText(val text: CharSequence?, val exception: Boolean = false)
    }

    @Inject
    lateinit var repository: GraphWidgetRepository

    override fun getWidgetProvider(context: Context): ComponentName =
        ComponentName(context, GraphWidget::class.java)

    override suspend fun getWidgetRemoteViews(context: Context, appWidgetId: Int, suggestedEntity: Entity<Map<String, Any>>?): RemoteViews {
        val historicData = repository.getGraphWidgetWithHistories(appWidgetId)
        val widget = historicData?.graphWidget

        val intent = Intent(context, GraphWidget::class.java).apply {
            action = UPDATE_VIEW
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)

        // Convert dp to pixels for width and height
        val width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            minWidth.toFloat(),
            context.resources.displayMetrics
        ).toInt()

        val height = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            minHeight.toFloat(),
            context.resources.displayMetrics
        ).toInt()

        val useDynamicColors = widget?.backgroundType == WidgetBackgroundType.DYNAMICCOLOR && DynamicColors.isDynamicColorAvailable()
        val views = RemoteViews(context.packageName, if (useDynamicColors) R.layout.widget_graph_wrapper_dynamiccolor else R.layout.widget_graph_wrapper_default)
            .apply {
                if (widget != null && (historicData.histories?.size ?: 0) >= 2) {
                    val serverId = widget.serverId
                    val entityId: String = widget.entityId
                    val label: String? = widget.label

                    // Content
                    setViewVisibility(
                        R.id.chartImageView,
                        View.VISIBLE
                    )
                    setViewVisibility(
                        R.id.widgetProgressBar,
                        View.GONE
                    )
                    setViewVisibility(
                        R.id.widgetStaticError,
                        View.GONE
                    )
                    val resolvedText = resolveTextToShow(
                        context,
                        serverId,
                        entityId,
                        suggestedEntity,
                        appWidgetId
                    )

                    setTextViewText(
                        R.id.widgetLabel,
                        label ?: entityId
                    )
                    setViewVisibility(
                        R.id.widgetStaticError,
                        if (resolvedText.exception) View.VISIBLE else View.GONE
                    )
                    setImageViewBitmap(
                        R.id.chartImageView,
                        createLineChart(
                            context = context,
                            label = label ?: entityId,
                            entries = createEntriesFromHistoricData(historicData = historicData),
                            width = width,
                            height = height,
                            timeRange = widget.timeRange.toString()
                        ).chartBitmap
                    )
                    setViewVisibility(
                        R.id.chartImageView,
                        if (!resolvedText.exception) View.VISIBLE else View.GONE
                    )

                    setOnClickPendingIntent(
                        R.id.widgetTextLayout,
                        PendingIntent.getBroadcast(
                            context,
                            appWidgetId,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                } else if (historicData?.histories?.isNotEmpty() == false) {
                    // Content
                    setViewVisibility(
                        R.id.chartImageView,
                        View.GONE
                    )
                    setViewVisibility(
                        R.id.widgetProgressBar,
                        View.VISIBLE
                    )
                    setViewVisibility(
                        R.id.widgetStaticError,
                        View.GONE
                    )
                }

                setOnClickPendingIntent(
                    R.id.widgetTextLayout,
                    PendingIntent.getBroadcast(
                        context,
                        appWidgetId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }

        return views
    }

    private fun createEntriesFromHistoricData(historicData: GraphWidgetWithHistories): List<Entry> {
        val entries = mutableListOf<Entry>()
        historicData.getOrderedHistories()?.forEachIndexed { index, history ->
            entries.add(Entry(index.toFloat() + 1, history.state.toFloat()))
        }
        return entries
    }

    private fun createLineChart(context: Context, label: String, timeRange: String, entries: List<Entry>, width: Int, height: Int): LineChart {
        val lineChart = LineChart(context).apply {
            val dynTextColor = ContextCompat.getColor(context, commonR.color.colorWidgetButtonLabel)
            setBackgroundResource(commonR.color.colorWidgetButtonBackground)
            setDrawBorders(false)

            xAxis.apply {
                setDrawGridLines(true)
                position = XAxis.XAxisPosition.BOTTOM
                textColor = dynTextColor
                textSize = 12F
                granularity = 2F
                setAvoidFirstLastClipping(true)
                isAutoScaleMinMaxEnabled = true
            }

            axisLeft.apply {
                setDrawGridLines(true)
                textColor = dynTextColor
                textSize = 12F
            }

            axisRight.apply {
                setDrawGridLines(false)
                setDrawLabels(false)
            }

            legend.apply {
                isEnabled = true
                textColor = dynTextColor
                textSize = 12F
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }

            description = Description().apply {
                text = "$timeRange h"
            }

            legend.isEnabled = true
            description.isEnabled = true
        }

        val mainGraphColor = ContextCompat.getColor(context, commonR.color.colorPrimary)

        val dataSet = LineDataSet(entries, label).apply {
            color = mainGraphColor
            lineWidth = 2F
            circleRadius = 1F
            setDrawCircleHole(false)
            setCircleColor(mainGraphColor)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawCircles(true)
            setDrawValues(false)
        }

        lineChart.data = LineData(dataSet)

        lineChart.layout(0, 0, width, height)

        return lineChart
    }

    override suspend fun getAllWidgetIdsWithEntities(context: Context): Map<Int, Pair<Int, List<String>>> =
        repository.getAllFlow()
            .first()
            .associate { it.id to (it.serverId to listOf(it.entityId)) }

    private suspend fun resolveTextToShow(
        context: Context,
        serverId: Int,
        entityId: String?,
        suggestedEntity: Entity<Map<String, Any>>?,
        appWidgetId: Int
    ): ResolvedText {
        var entity: Entity<Map<String, Any>>? = null
        var entityCaughtException = false
        try {
            entity = if (suggestedEntity != null && suggestedEntity.entityId == entityId) {
                suggestedEntity
            } else {
                entityId?.let { serverManager.integrationRepository(serverId).getEntity(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to fetch entity", e)
            entityCaughtException = true
        }
        val entityOptions = if (
            entity?.canSupportPrecision() == true &&
            serverManager.getServer(serverId)?.version?.isAtLeast(2023, 3) == true
        ) {
            serverManager.webSocketRepository(serverId).getEntityRegistryFor(entity.entityId)?.options
        } else {
            null
        }
        repository.updateWidgetLastUpdate(
            appWidgetId,
            entity?.friendlyState(context, entityOptions) ?: repository.get(appWidgetId)?.lastUpdate ?: ""
        )
        return ResolvedText(repository.get(appWidgetId)?.lastUpdate, entityCaughtException)
    }

    override fun saveEntityConfiguration(context: Context, extras: Bundle?, appWidgetId: Int) {
        if (extras == null) return

        val serverId = extras.getInt(EXTRA_SERVER_ID)

        val entitySelection: String? = extras.getString(EXTRA_ENTITY_ID)
        val labelSelection: String? = extras.getString(EXTRA_LABEL)

        val timeRange = extras.getInt(EXTRA_TIME_RANGE)

        if (entitySelection == null) {
            Log.e(TAG, "Missing entitySelection. Expected entity ($labelSelection) data but received null. Time range: $timeRange")
            return
        }

        widgetScope?.launch {
            Log.d(
                TAG,
                "Saving entity state config data:" + System.lineSeparator() +
                    "entity id: " + entitySelection + System.lineSeparator()
            )
            repository.add(
                GraphWidgetEntity(
                    id = appWidgetId,
                    serverId = serverId,
                    entityId = entitySelection,
                    label = labelSelection,
                    timeRange = timeRange,
                    tapAction = WidgetTapAction.REFRESH,
                    lastUpdate = repository.get(appWidgetId)?.lastUpdate ?: ""
                )
            )

            onUpdate(context, AppWidgetManager.getInstance(context), intArrayOf(appWidgetId))
        }
    }

    override suspend fun onEntityStateChanged(context: Context, appWidgetId: Int, entity: Entity<*>) {
        widgetScope?.launch {
            val graphEntity = repository.get(appWidgetId)

            if (graphEntity != null) {
                val currentTimeMillis = System.currentTimeMillis()

                repository.deleteEntriesOlderThan(appWidgetId, graphEntity.timeRange, currentTimeMillis)

                repository.insertGraphWidgetHistory(
                    GraphWidgetHistoryEntity(
                        entityId = entity.entityId,
                        graphWidgetId = appWidgetId,
                        state = entity.friendlyState(context),
                        sentState = currentTimeMillis
                    )
                )
            }

            val views = getWidgetRemoteViews(context, appWidgetId, entity as Entity<Map<String, Any>>)
            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        widgetScope?.launch {
            repository.deleteAll(appWidgetIds)
            appWidgetIds.forEach { removeSubscription(it) }
        }
    }
}
