package io.homeassistant.companion.android

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import io.homeassistant.companion.android.common.dagger.AppComponent
import io.homeassistant.companion.android.common.dagger.Graph
import io.homeassistant.companion.android.common.dagger.GraphComponentAccessor
import io.homeassistant.companion.android.migrations.Migrations
import io.homeassistant.companion.android.sensors.SensorReceiver

open class HomeAssistantApplication : Application(), GraphComponentAccessor {

    lateinit var graph: Graph

    override fun onCreate() {
        super.onCreate()

        graph = Graph(this, 0)

        Migrations(this).migrate()

        val sensorReceiver = SensorReceiver()
        // This will cause the sensor to be updated every time the OS broadcasts that a cable was plugged/unplugged.
        // This should be nearly instantaneous allowing automations to fire immediately when a phone is plugged
        // in or unplugged. Updates will also be triggered when the system reports low battery and when it recovers.
        registerReceiver(
            sensorReceiver, IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_LOW)
                addAction(Intent.ACTION_BATTERY_OKAY)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
        )

        // This will trigger an update any time the wifi state has changed
        registerReceiver(
            sensorReceiver,
            IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        )

        // This will cause the phone state sensor to be updated every time the OS broadcasts that a call triggered.
        registerReceiver(
            sensorReceiver,
            IntentFilter().apply {
                addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            }
        )

        //Listen for bluetooth state changes
        registerReceiver(sensorReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )

        // Listen to changes to the audio input/output on the device
        registerReceiver(
            sensorReceiver,
            IntentFilter().apply {
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                addAction(AudioManager.ACTION_HEADSET_PLUG)
                addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            }
        )

        // Add extra audio receiver for devices that support it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            registerReceiver(
                sensorReceiver,
                IntentFilter().apply {
                    addAction(AudioManager.ACTION_MICROPHONE_MUTE_CHANGED)
                    addAction(AudioManager.ACTION_SPEAKERPHONE_STATE_CHANGED)
                }
            )
        }
    }

    override val appComponent: AppComponent
        get() = graph.appComponent
}
