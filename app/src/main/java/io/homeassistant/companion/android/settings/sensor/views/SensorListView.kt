package io.homeassistant.companion.android.settings.sensor.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.homeassistant.companion.android.common.sensors.SensorManager
import io.homeassistant.companion.android.database.sensor.Sensor
import io.homeassistant.companion.android.settings.sensor.SensorSettingViewModel
import io.homeassistant.companion.android.common.R as commonR

@Composable
fun SensorListView(
    viewModel: SensorSettingViewModel,
    managers: List<SensorManager>,
    onSensorClicked: (SensorManager, String) -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        managers.forEachIndexed { index, manager ->
            if (manager.hasSensor(context)) {
                val basicSensors = manager.getAvailableSensors(context)
                val currentSensors = basicSensors.filter { basicSensor ->
                    viewModel.sensors.any { basicSensor.id == it.id }
                }
                if (currentSensors.any()) {
                    item {
                        if (index != 0) Divider()
                        Row(
                            modifier = Modifier
                                .height(48.dp)
                                .padding(start = 72.dp, end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(manager.name),
                                style = MaterialTheme.typography.body2,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.primary
                            )
                        }
                    }
                }
                items(currentSensors) { basicSensor ->
                    SensorRow(
                        manager = manager,
                        basicSensor = basicSensor,
                        dbSensor = viewModel.sensors.firstOrNull { it.id == basicSensor.id },
                        onSensorClicked = onSensorClicked
                    )
                }
            }
        }
    }
}

@Composable
fun SensorRow(
    manager: SensorManager,
    basicSensor: SensorManager.BasicSensor,
    dbSensor: Sensor?,
    onSensorClicked: (SensorManager, String) -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onSensorClicked(manager, basicSensor.id) }
            .height(72.dp)
            .padding(start = 72.dp, end = 16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,

    ) {
        Text(
            text = stringResource(basicSensor.name),
            style = MaterialTheme.typography.body1
        )
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                text = if (dbSensor?.enabled == true) {
                    if (dbSensor.state.isBlank()) {
                        stringResource(commonR.string.enabled)
                    } else {
                        if (basicSensor.unitOfMeasurement.isNullOrBlank()) dbSensor.state
                        else "${dbSensor.state} ${basicSensor.unitOfMeasurement}"
                    }
                } else {
                    stringResource(commonR.string.disabled)
                },
                style = MaterialTheme.typography.body2
            )
        }
    }
}
