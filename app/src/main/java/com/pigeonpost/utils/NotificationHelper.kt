package com.pigeonpost.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pigeonpost.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles creation and display of notifications with medieval-themed messages.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID_DELIVERY = "pigeon_delivery"
        const val CHANNEL_NAME_DELIVERY = "Pigeon Deliveries"
        const val CHANNEL_DESC_DELIVERY = "Notifications about your pigeon messenger deliveries"

        const val CHANNEL_ID_LOST = "pigeon_lost"
        const val CHANNEL_NAME_LOST = "Lost Pigeons"
        const val CHANNEL_DESC_LOST = "Notifications when a pigeon perishes during transit"

        private const val NOTIFICATION_ID_BASE = 1000
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val deliveryChannel = NotificationChannel(
                CHANNEL_ID_DELIVERY,
                CHANNEL_NAME_DELIVERY,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC_DELIVERY
            }

            val lostChannel = NotificationChannel(
                CHANNEL_ID_LOST,
                CHANNEL_NAME_LOST,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_LOST
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(deliveryChannel)
            notificationManager.createNotificationChannel(lostChannel)
        }
    }

    /**
     * Show notification when a pigeon successfully delivers a message.
     */
    fun showDeliveryNotification(senderName: String, messagePreview: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_DELIVERY)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(context.getString(R.string.notification_delivered_title))
            .setContentText(
                context.getString(R.string.notification_delivered_body, senderName)
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(messagePreview)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_BASE + System.currentTimeMillis().toInt() % 10000, notification)
        } catch (e: SecurityException) {
            // Permission not granted for notifications
        }
    }

    /**
     * Show notification when a pigeon is lost in transit.
     */
    fun showPigeonLostNotification(receiverName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_LOST)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.notification_lost_title))
            .setContentText(
                context.getString(R.string.notification_lost_body, receiverName)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_BASE + System.currentTimeMillis().toInt() % 10000, notification)
        } catch (e: SecurityException) {
            // Permission not granted for notifications
        }
    }
}
