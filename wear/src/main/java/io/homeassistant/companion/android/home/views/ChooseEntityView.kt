package io.homeassistant.companion.android.home.views

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.items
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.domain
import io.homeassistant.companion.android.data.SimplifiedEntity
import io.homeassistant.companion.android.theme.WearAppTheme
import io.homeassistant.companion.android.util.getIcon
import io.homeassistant.companion.android.util.stringForDomain
import io.homeassistant.companion.android.common.R as commonR

@Composable
fun ChooseEntityView(
    context: Context,
    entitiesByDomainOrder: SnapshotStateList<String>,
    entitiesByDomain: SnapshotStateMap<String, SnapshotStateList<Entity<*>>>,
    onNoneClicked: () -> Unit,
    onEntitySelected: (entity: SimplifiedEntity) -> Unit,
    allowNone: Boolean = true
) {
    // Remember expanded state of each header
    val expandedStates = rememberExpandedStates(entitiesByDomainOrder)

    WearAppTheme {
        ThemeLazyColumn {
            item {
                ListHeader(id = commonR.string.shortcuts_choose)
            }
            if (allowNone) {
                item {
                    Chip(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        icon = { Image(asset = CommunityMaterial.Icon.cmd_delete) },
                        label = { Text(stringResource(id = commonR.string.none)) },
                        onClick = onNoneClicked,
                        colors = ChipDefaults.primaryChipColors(
                            contentColor = Color.Black
                        )
                    )
                }
            }
            for (domain in entitiesByDomainOrder) {
                val entities = entitiesByDomain[domain]
                if (!entities.isNullOrEmpty()) {
                    item {
                        ExpandableListHeader(
                            string = stringForDomain(domain, context) ?: domain,
                            key = domain,
                            expandedStates = expandedStates
                        )
                    }
                    if (expandedStates[domain] == true) {
                        items(entities, key = { it.entityId }) { entity ->
                            ChooseEntityChip(
                                entity = entity,
                                onEntitySelected = onEntitySelected
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChooseEntityChip(
    entity: Entity<*>,
    onEntitySelected: (entity: SimplifiedEntity) -> Unit
) {
    val attributes = entity.attributes as Map<*, *>
    val iconBitmap = getIcon(
        entity as Entity<Map<String, Any>>,
        entity.domain,
        LocalContext.current
    )
    Chip(
        modifier = Modifier
            .fillMaxWidth(),
        icon = {
            Image(
                asset = iconBitmap ?: CommunityMaterial.Icon.cmd_cellphone,
                colorFilter = ColorFilter.tint(Color.White)
            )
        },
        label = {
            Text(
                text = attributes["friendly_name"].toString(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        enabled = entity.state != "unavailable",
        onClick = {
            onEntitySelected(
                SimplifiedEntity(
                    entity.entityId,
                    attributes["friendly_name"] as String? ?: entity.entityId,
                    attributes["icon"] as String? ?: ""
                )
            )
        },
        colors = ChipDefaults.secondaryChipColors()
    )
}
