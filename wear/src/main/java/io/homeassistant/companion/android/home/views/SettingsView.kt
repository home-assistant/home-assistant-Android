package io.homeassistant.companion.android.home.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyListState
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.rememberScalingLazyListState
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.theme.WearAppTheme
import io.homeassistant.companion.android.theme.wearColorPalette
import io.homeassistant.companion.android.util.LocalRotaryEventDispatcher
import io.homeassistant.companion.android.util.RotaryEventDispatcher
import io.homeassistant.companion.android.util.RotaryEventState
import io.homeassistant.companion.android.util.previewFavoritesList
import io.homeassistant.companion.android.common.R as commonR

@ExperimentalWearMaterialApi
@Composable
fun SettingsView(
    favorites: List<String>,
    onClickSetFavorites: () -> Unit,
    onClearFavorites: () -> Unit,
    onClickSetShortcuts: () -> Unit,
    onClickLogout: () -> Unit,
    isHapticEnabled: Boolean,
    isToastEnabled: Boolean,
    onHapticEnabled: (Boolean) -> Unit,
    onToastEnabled: (Boolean) -> Unit
) {
    val scalingLazyListState: ScalingLazyListState = rememberScalingLazyListState()
    RotaryEventState(scrollState = scalingLazyListState)

    WearAppTheme {
        Scaffold(
            positionIndicator = {
                if (scalingLazyListState.isScrollInProgress)
                    PositionIndicator(scalingLazyListState = scalingLazyListState)
            },
            timeText = {
                if (!scalingLazyListState.isScrollInProgress)
                    TimeText()
            }
        ) {
            ScalingLazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 24.dp,
                    start = 8.dp,
                    end = 8.dp,
                    bottom = 48.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                state = scalingLazyListState
            ) {
                item {
                    ListHeader(id = R.string.favorite_settings)
                }
                item {
                    Chip(
                        modifier = Modifier
                            .fillMaxWidth(),
                        icon = {
                            Image(
                                asset = CommunityMaterial.Icon3.cmd_star,
                                colorFilter = ColorFilter.tint(wearColorPalette.onSurface)
                            )
                        },
                        colors = ChipDefaults.secondaryChipColors(),
                        label = {
                            Text(
                                text = stringResource(id = R.string.favorite)
                            )
                        },
                        onClick = onClickSetFavorites
                    )
                }
                item {
                    Chip(
                        modifier = Modifier
                            .fillMaxWidth(),
                        icon = {
                            Image(
                                asset = CommunityMaterial.Icon.cmd_delete,
                                colorFilter = ColorFilter.tint(wearColorPalette.onSurface)
                            )
                        },
                        colors = ChipDefaults.secondaryChipColors(),
                        label = {
                            Text(
                                text = stringResource(id = R.string.clear_favorites),
                            )
                        },
                        onClick = onClearFavorites,
                        secondaryLabel = {
                            Text(
                                text = stringResource(id = R.string.irreverisble)
                            )
                        },
                        enabled = favorites.isNotEmpty()
                    )
                }
                item {
                    ListHeader(
                        id = R.string.feedback_settings
                    )
                }
                item {
                    val haptic = LocalHapticFeedback.current
                    ToggleChip(
                        modifier = Modifier
                            .fillMaxWidth(),
                        checked = isHapticEnabled,
                        onCheckedChange = {
                            onHapticEnabled(it)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        label = {
                            Text(stringResource(R.string.setting_haptic_label))
                        },
                        appIcon = {
                            Image(
                                asset =
                                if (isHapticEnabled)
                                    CommunityMaterial.Icon3.cmd_watch_vibrate
                                else
                                    CommunityMaterial.Icon3.cmd_watch_vibrate_off,
                                colorFilter = ColorFilter.tint(wearColorPalette.onSurface)
                            )
                        }
                    )
                }
                item {
                    ToggleChip(
                        modifier = Modifier
                            .fillMaxWidth(),
                        checked = isToastEnabled,
                        onCheckedChange = {
                            onToastEnabled(it)
                        },
                        label = {
                            Text(stringResource(R.string.setting_toast_label))
                        },
                        appIcon = {
                            Image(
                                asset =
                                if (isToastEnabled)
                                    CommunityMaterial.Icon3.cmd_message
                                else
                                    CommunityMaterial.Icon3.cmd_message_off,
                                colorFilter = ColorFilter.tint(wearColorPalette.onSurface)
                            )
                        }
                    )
                }

                item {
                    ListHeader(
                        id = R.string.tile_settings
                    )
                }
                item {
                    Chip(
                        modifier = Modifier
                            .fillMaxWidth(),
                        icon = {
                            Image(
                                asset = CommunityMaterial.Icon3.cmd_star_circle_outline,
                                colorFilter = ColorFilter.tint(wearColorPalette.onSurface)
                            )
                        },
                        colors = ChipDefaults.secondaryChipColors(),
                        label = {
                            Text(
                                text = stringResource(id = R.string.shortcuts)
                            )
                        },
                        onClick = onClickSetShortcuts
                    )
                }

                item {
                    ListHeader(
                        id = R.string.account
                    )
                }
                item {
                    Chip(
                        modifier = Modifier
                            .fillMaxWidth(),
                        icon = {
                            Image(asset = CommunityMaterial.Icon.cmd_exit_run)
                        },
                        label = {
                            Text(
                                text = stringResource(id = R.string.logout)
                            )
                        },
                        onClick = onClickLogout,
                        colors = ChipDefaults.primaryChipColors(
                            backgroundColor = Color.Red,
                            contentColor = Color.Black
                        )
                    )
                }
            }
        }
    }
}

@ExperimentalWearMaterialApi
@Preview
@Composable
private fun PreviewSettingsView() {
    val rotaryEventDispatcher = RotaryEventDispatcher()

    CompositionLocalProvider(
        LocalRotaryEventDispatcher provides rotaryEventDispatcher
    ) {
        SettingsView(
            favorites = previewFavoritesList,
            onClickSetFavorites = { /*TODO*/ },
            onClearFavorites = {},
            onClickSetShortcuts = {},
            onClickLogout = {},
            isHapticEnabled = true,
            isToastEnabled = false,
            {},
            {}
        )
    }
}
