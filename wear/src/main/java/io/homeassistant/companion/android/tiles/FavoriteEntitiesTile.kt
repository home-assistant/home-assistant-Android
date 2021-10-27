package io.homeassistant.companion.android.tiles

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.wear.tiles.ActionBuilders
import androidx.wear.tiles.ColorBuilders.argb
import androidx.wear.tiles.DimensionBuilders.dp
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.LayoutElementBuilders.Box
import androidx.wear.tiles.LayoutElementBuilders.Column
import androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.tiles.LayoutElementBuilders.Row
import androidx.wear.tiles.LayoutElementBuilders.Spacer
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement
import androidx.wear.tiles.LayoutElementBuilders.Layout
import androidx.wear.tiles.ModifiersBuilders
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.ResourceBuilders.Resources
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders.Timeline
import androidx.wear.tiles.TimelineBuilders.TimelineEntry
import com.google.common.util.concurrent.ListenableFuture
import com.mikepenz.iconics.IconicsColor
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.backgroundColor
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.common.dagger.GraphComponentAccessor
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.IntegrationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.guava.future
import java.nio.ByteBuffer
import javax.inject.Inject
import kotlin.math.roundToInt

private const val RESOURCES_VERSION = "1"

// Dimensions (dp)
private const val CIRCLE_SIZE = 56f
private const val ICON_SIZE = 48f*0.7071f // square that fits in 48dp circle
private const val SPACING = 8f

class FavoriteEntitiesTile : TileService() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    @Inject
    lateinit var integrationUseCase: IntegrationRepository

    override fun onTileRequest(requestParams: TileRequest): ListenableFuture<Tile> =
        serviceScope.future {
            val entities = getEntities()

            Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setTimeline(
                    Timeline.Builder().addTimelineEntry(
                        TimelineEntry.Builder().setLayout(
                            Layout.Builder().setRoot(
                                layout(entities)
                            ).build()
                        ).build()
                    ).build()
                ).build()
        }

    override fun onResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> =
        serviceScope.future {
            val density = requestParams.deviceParameters!!.screenDensity
            val iconSizePx = (ICON_SIZE * density).roundToInt()
            val entities = getEntities()

            Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .apply {
                    entities.map { entity ->
                        val entityAttributes = entity.attributes as Map<String, String>
                        val iconName: String = if (entityAttributes["icon"]?.startsWith("mdi") == true) {
                            entityAttributes["icon"]!!.split(":")[1]
                        } else {
                            "palette" // Default scene icon
                        }

                        val iconBitmap = IconicsDrawable(this@FavoriteEntitiesTile, "cmd-$iconName").apply {
                            colorInt = Color.WHITE
                            sizeDp = ICON_SIZE.roundToInt()
                            backgroundColor = IconicsColor.colorRes(R.color.colorOverlay)
                        }.toBitmap(iconSizePx, iconSizePx, Bitmap.Config.RGB_565)

                        val bitmapData = ByteBuffer.allocate(iconBitmap.byteCount).apply {
                            iconBitmap.copyPixelsToBuffer(this)
                        }.array()

                        // link the entity id to the drawable
                        entity.entityId to ResourceBuilders.ImageResource.Builder()
                            .setInlineResource(
                                ResourceBuilders.InlineImageResource.Builder()
                                    .setData(bitmapData)
                                    .setWidthPx(iconSizePx)
                                    .setHeightPx(iconSizePx)
                                    .setFormat(ResourceBuilders.IMAGE_FORMAT_RGB_565)
                                    .build()
                            )
                            .build()
                    }.forEach { (id, imageResource) ->
                        addIdToImageMapping(id, imageResource)
                    }
                }
                .build()
        }

    override fun onDestroy() {
        super.onDestroy()
        // Cleans up the coroutine
        serviceJob.cancel()
    }

    private suspend fun getEntities(): List<Entity<Any>> {
        //TODO this should actually be a list specified by the user in settings
        DaggerTilesComponent.builder()
            .appComponent((applicationContext as GraphComponentAccessor).appComponent)
            .build()
            .inject(this@FavoriteEntitiesTile)

        return integrationUseCase.getEntities()
            .sortedBy { it.entityId }
            .filter { it.entityId.split(".")[0] == "scene" }
    }

    fun layout(entities: List<Entity<Any>>): LayoutElement = Column.Builder()
        .addContent(
            Row.Builder()
                .addContent(iconLayout(entities[0]))
                .addContent(Spacer.Builder().setWidth(dp(SPACING)).build())
                .addContent(iconLayout(entities[1]))
                .build()
        )
        .addContent(
            Row.Builder()
                .addContent(iconLayout(entities[2]))
                .addContent(Spacer.Builder().setWidth(dp(SPACING)).build())
                .addContent(iconLayout(entities[3]))
                .addContent(Spacer.Builder().setWidth(dp(SPACING)).build())
                .addContent(iconLayout(entities[4]))
                .build()
        )
        .addContent(
            Row.Builder()
                .addContent(iconLayout(entities[5]))
                .addContent(Spacer.Builder().setWidth(dp(SPACING)).build())
                .addContent(iconLayout(entities[6]))
                .build()
        )
        .build()

    private fun iconLayout(entity: Entity<Any>): LayoutElement = Box.Builder().apply {
        setWidth(dp(CIRCLE_SIZE))
        setHeight(dp(CIRCLE_SIZE))
        setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
        setModifiers(ModifiersBuilders.Modifiers.Builder()
            .setBackground(
                ModifiersBuilders.Background.Builder()
                    .setColor(argb(ContextCompat.getColor(baseContext, R.color.colorOverlay)))
                    .setCorner(
                        ModifiersBuilders.Corner.Builder()
                            .setRadius(dp(CIRCLE_SIZE / 2))
                            .build()
                    )
                    .build()
            )
            .setClickable(ModifiersBuilders.Clickable.Builder()
                .setOnClick(
                    ActionBuilders.LaunchAction.Builder()
                        .setAndroidActivity(
                            ActionBuilders.AndroidActivity.Builder()
                                .setClassName(TileActionActivity::class.java.name)
                                .setPackageName(this@FavoriteEntitiesTile.packageName)
                                .addKeyToExtraMapping("entity_id", ActionBuilders.stringExtra(entity.entityId))
                                .build()
                        )
                        .build()
                )
                .build()
            )
            .build()
        )
        addContent(
            LayoutElementBuilders.Image.Builder()
                .setResourceId(entity.entityId)
                .setWidth(dp(ICON_SIZE))
                .setHeight(dp(ICON_SIZE))
                .build()
        )
    }
        .build()
}