package io.homeassistant.companion.android.home.views

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.mikepenz.iconics.compose.Image
import io.homeassistant.companion.android.data.SimplifiedEntity
import io.homeassistant.companion.android.theme.WearAppTheme
import io.homeassistant.companion.android.theme.wearColorScheme
import io.homeassistant.companion.android.util.getIcon
import io.homeassistant.companion.android.util.simplifiedEntity
import io.homeassistant.companion.android.views.ListHeader
import io.homeassistant.companion.android.views.ThemeLazyColumn
import io.homeassistant.companion.android.common.R as commonR

@Composable
fun SetShortcutsTileView(
    shortcutEntities: List<SimplifiedEntity>,
    onShortcutEntitySelectionChange: (Int) -> Unit
) {
    WearAppTheme {
        ThemeLazyColumn {
            item {
                ListHeader(id = commonR.string.shortcuts_choose)
            }
            items(shortcutEntities.size) { index ->

                val iconBitmap = getIcon(
                    shortcutEntities[index].icon,
                    shortcutEntities[index].domain,
                    LocalContext.current
                )

                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    icon = {
                        Image(
                            iconBitmap,
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(commonR.string.shortcut_n, index + 1)
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = shortcutEntities[index].friendlyName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = { onShortcutEntitySelectionChange(index) },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = wearColorScheme.outlineVariant)
                )
            }
            if (shortcutEntities.size < 7) {
                item {
                    Button(
                        modifier = Modifier.padding(top = 16.dp),
                        onClick = { onShortcutEntitySelectionChange(shortcutEntities.size) },
                        colors = ButtonDefaults.buttonColors(),
                        icon = {
                            Icon(Icons.Filled.Add, stringResource(id = commonR.string.add_shortcut))
                        }
                    ) { }
                }
            }
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun PreviewSetTileShortcutsView() {
    SetShortcutsTileView(
        shortcutEntities = mutableListOf(simplifiedEntity),
        onShortcutEntitySelectionChange = {}
    )
}
