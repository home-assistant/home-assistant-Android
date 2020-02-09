package io.homeassistant.companion.android.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.background.LocationBroadcastReceiver
import io.homeassistant.companion.android.common.dagger.GraphComponentAccessor
import io.homeassistant.companion.android.domain.integration.IntegrationUseCase
import io.homeassistant.companion.android.webview.WebViewActivity
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessagingService : FirebaseMessagingService() {
    companion object {
        const val TAG = "MessagingService"
        const val TITLE = "title"
        const val MESSAGE = "message"
        const val IMAGE_URL = "image"
    }

    @Inject
    lateinit var integrationUseCase: IntegrationUseCase

    private val mainScope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate() {
        super.onCreate()
        DaggerServiceComponent.builder()
            .appComponent((applicationContext as GraphComponentAccessor).appComponent)
            .build()
            .inject(this)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        remoteMessage.data.let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)

            if (it[MESSAGE] == "request_location_update") {
                Log.d(TAG, "Request location update")
                requestAccurateLocationUpdate()
            } else {
                mainScope.launch {
                    Log.d(TAG, "Creating notification with following data: $it")
                    sendNotification(it)
                }
            }
        }
    }

    private fun requestAccurateLocationUpdate() {
        val intent = Intent(this, LocationBroadcastReceiver::class.java)
        intent.action = LocationBroadcastReceiver.ACTION_REQUEST_ACCURATE_LOCATION_UPDATE

        sendBroadcast(intent)
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     */
    private suspend fun sendNotification(data: Map<String, String>) {

        val tag = data["tag"]
        val messageId = tag?.hashCode() ?: System.currentTimeMillis().toInt()

        val intent = Intent(this, WebViewActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT
        )

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = handleChannel(notificationManager)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_stat_ic_notification)
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))

        handleText(notificationBuilder, data)

        handleImage(notificationBuilder, data)

        handleActions(notificationBuilder, data, messageId)

        if (tag != null) {
            notificationManager.notify(tag, messageId, notificationBuilder.build())
        } else {
            notificationManager.notify(messageId, notificationBuilder.build())
        }
    }

    private fun handleText(
        builder: NotificationCompat.Builder,
        data: Map<String, String>
    ) {
        builder
            .setContentTitle(data[TITLE])
            .setContentText(data[MESSAGE])
    }

    private suspend fun handleImage(
        builder: NotificationCompat.Builder,
        data: Map<String, String>
    ) {
        val imageUrl = data[IMAGE_URL]
        if (imageUrl != null) {
            val bitmap = getImageBitmap(imageUrl)
            if (bitmap != null) {
                builder
                    .setLargeIcon(bitmap)
                    .setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .bigLargeIcon(null)
                    )
            }
        }
    }

    private suspend fun getImageBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        var image: Bitmap? = null
        try {
            image = BitmapFactory.decodeStream(URL(url).openStream())
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't download image for notification", e)
        }
        return@withContext image
    }

    private fun handleActions(
        builder: NotificationCompat.Builder,
        data: Map<String, String>,
        messageId: Int
    ) {
        for (i in 1..3) {
            if (data.containsKey("action_${i}_key")) {
                val notificationAction = NotificationAction(
                    data["action_${i}_key"].toString(),
                    data["action_${i}_title"].toString(),
                    data
                )
                val actionIntent = Intent(this, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.FIRE_EVENT
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, messageId)
                    putExtra(
                        NotificationActionReceiver.EXTRA_NOTIFICATION_ACTION,
                        notificationAction.key
                    )
                }
                val actionPendingIntent = PendingIntent.getBroadcast(
                    this,
                    0,
                    actionIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT
                )

                builder.addAction(0, notificationAction.title, actionPendingIntent)
            }
        }
    }

    private fun handleChannel(
        notificationManager: NotificationManager
    ): String {
        // TODO: implement channels
        val channelId = "default"
        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Default Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        return channelId
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        mainScope.launch {
            Log.d(TAG, "Refreshed token: $token")
            try {
                integrationUseCase.updateRegistration(
                    pushToken = token
                )
            } catch (e: Exception) {
                // TODO: Store for update later
                Log.e(TAG, "Issue updating token", e)
            }
        }
    }
}
