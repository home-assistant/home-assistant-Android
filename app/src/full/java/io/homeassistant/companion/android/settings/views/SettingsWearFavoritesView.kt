package io.homeassistant.companion.android.settings.views

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.util.previewEntityList
import io.homeassistant.companion.android.util.previewFavoritesList

const val WEAR_DOCS_LINK = "https://companion.home-assistant.io/docs/wear-os/wear-os"
val supportedDomains = listOf(
    "input_boolean", "light", "switch", "script", "scene"
)

@Composable
fun LoadWearFavoritesSettings(
    entities: Map<String, Entity<*>>,
    favoritesList: List<String>,
    onEntitySelected: (Boolean, String) -> Unit
) {
    val context = LocalContext.current

    val validEntities = entities.filter { it.key.split(".")[0] in supportedDomains }.values.sortedBy { it.entityId }.toList()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wear_favorite_entities)) },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WEAR_DOCS_LINK))
                        context.startActivity(intent)
                    }) {
                        Icon(
                            Icons.Filled.HelpOutline,
                            contentDescription = stringResource(id = R.string.help)
                        )
                    }
                }
            )
        }
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 10.dp, start = 20.dp, end = 20.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.wear_set_favorites),
                    fontWeight = FontWeight.Bold
                )
            }
            items(favoritesList.size) { index ->
                Row(
                    modifier = Modifier
                        .padding(15.dp)
                        .clickable {
                            onEntitySelected(
                                false,
                                favoritesList[index]
                            )
                        }
                ) {
                    Checkbox(
                        checked = favoritesList.contains(favoritesList[index]),
                        onCheckedChange = {
                            onEntitySelected(it, favoritesList[index])
                        },
                        modifier = Modifier.padding(end = 5.dp)
                    )
                    Text(
                        text = favoritesList[index].replace("[", "").replace("]", ""),
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
            if (!validEntities.isNullOrEmpty()) {
                items(validEntities.size) { index ->
                    val item = validEntities[index]
                    if (!favoritesList.contains(item.entityId)) {
                        Row(
                            modifier = Modifier
                                .padding(15.dp)
                                .clickable {
                                    onEntitySelected(true, item.entityId)
                                }
                        ) {
                            Checkbox(
                                checked = false,
                                onCheckedChange = {
                                    onEntitySelected(it, item.entityId)
                                },
                                modifier = Modifier.padding(end = 5.dp)
                            )
                            Text(
                                text = item.entityId,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewLoadWearFavoritesSettings() {
    LoadWearFavoritesSettings(
        entities = previewEntityList,
        favoritesList = previewFavoritesList,
        onEntitySelected = { _, _ -> }
    )
}
