package io.homeassistant.companion.android.widgets.grid

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.widget.RemoteViewsCompat
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.IconicsSize
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.padding
import com.mikepenz.iconics.utils.size
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.database.widget.GridWidgetEntity
import io.homeassistant.companion.android.database.widget.GridWidgetItemEntity
import io.homeassistant.companion.android.database.widget.GridWidgetWithItemsEntity
import io.homeassistant.companion.android.util.icondialog.getIconByMdiName
import io.homeassistant.companion.android.widgets.grid.GridWidget.Companion.CALL_SERVICE
import io.homeassistant.companion.android.widgets.grid.GridWidget.Companion.CALL_SERVICE_AUTH
import io.homeassistant.companion.android.widgets.grid.GridWidget.Companion.EXTRA_ACTION_ID
import io.homeassistant.companion.android.widgets.grid.config.GridConfiguration
import io.homeassistant.companion.android.widgets.grid.config.GridItem

fun GridConfiguration?.asRemoteViews(context: Context, widgetId: Int): RemoteViews {
    val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        R.layout.widget_grid_wrapper_dynamiccolor
    } else {
        R.layout.widget_grid_wrapper_default
    }
    val remoteViews = RemoteViews(context.packageName, layout)

    if (this != null) {
        remoteViews.apply {
            if (label.isNullOrEmpty()) {
                setViewVisibility(R.id.widgetLabel, View.GONE)
            } else {
                setViewVisibility(R.id.widgetLabel, View.VISIBLE)
                setTextViewText(R.id.widgetLabel, label)
            }

            val intent = Intent(context, GridWidget::class.java).apply {
                action = if (requireAuthentication) CALL_SERVICE_AUTH else CALL_SERVICE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            setPendingIntentTemplate(
                R.id.widgetGrid,
                PendingIntent.getBroadcast(
                    context,
                    widgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            )

            RemoteViewsCompat.setRemoteAdapter(
                context = context,
                remoteViews = this,
                appWidgetId = widgetId,
                viewId = R.id.widgetGrid,
                items = items.asRemoteCollection(context)
            )
        }
    }
    return remoteViews
}

fun List<GridItem>.asRemoteCollection(context: Context) =
    RemoteViewsCompat.RemoteCollectionItems.Builder().apply {
        setHasStableIds(true)
        forEach { addItem(context, it) }
    }.build()

private fun RemoteViewsCompat.RemoteCollectionItems.Builder.addItem(context: Context, item: GridItem) {
    addItem(item.id.toLong(), item.asRemoteViews(context))
}

private fun GridItem.asRemoteViews(context: Context) =
    RemoteViews(context.packageName, R.layout.widget_grid_button).apply {
        val icon = CommunityMaterial.getIconByMdiName(icon)
        icon?.let {
            val iconDrawable = DrawableCompat.wrap(
                IconicsDrawable(context, icon).apply {
                    padding = IconicsSize.dp(2)
                    size = IconicsSize.dp(24)
                }
            )

            setImageViewBitmap(R.id.widgetImageButton, iconDrawable.toBitmap())
        }
        setTextViewText(
            R.id.widgetLabel,
            label
        )

        val fillInIntent = Intent().apply {
            Bundle().also { extras ->
                extras.putInt(EXTRA_ACTION_ID, id)
                putExtras(extras)
            }
        }
        setOnClickFillInIntent(R.id.gridButtonLayout, fillInIntent)
    }

fun GridConfiguration.asDbEntity(widgetId: Int) =
    GridWidgetWithItemsEntity(
        gridWidget = GridWidgetEntity(
            id = widgetId,
            serverId = serverId ?: 0,
            label = label,
            requireAuthentication = requireAuthentication
        ),
        items = items.map { it.asDbEntity(widgetId) }
    )

fun GridItem.asDbEntity(widgetId: Int) =
    GridWidgetItemEntity(
        id = id,
        gridId = widgetId,
        domain = domain,
        service = service,
        serviceData = serviceData,
        label = label,
        iconName = icon
    )

fun GridWidgetWithItemsEntity.asGridConfiguration() =
    GridConfiguration(
        serverId = gridWidget.serverId,
        label = gridWidget.label,
        requireAuthentication = gridWidget.requireAuthentication,
        items = items.map(GridWidgetItemEntity::asGridItem)
    )

fun GridWidgetItemEntity.asGridItem() =
    GridItem(
        id = id,
        label = label.orEmpty(),
        icon = iconName,
        domain = domain,
        service = service,
        serviceData = serviceData
    )
