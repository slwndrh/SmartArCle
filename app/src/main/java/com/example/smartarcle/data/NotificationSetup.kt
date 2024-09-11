package com.example.smartarcle.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.smartarcle.R
import com.example.smartarcle.owner.HomeOwner

class NotificationSetup (private val ctx: Context) {
    private var CHANNEL_ID = "channel_id"
    private val ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

    fun sendTheftNotification() {
        val intent = Intent(ctx, HomeOwner::class.java)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(ctx, 0, intent, flags)
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val bigTextStyle = NotificationCompat
            .BigTextStyle()
            .bigText("Detected vehicle theft immediately contact the security with the report missing feature button!")

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.vehicle)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setSound(ringtone)
            .setContentTitle("Vehicle Theft Detected!")
            .setStyle(bigTextStyle)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(pendingIntent, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID, "theft_notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.enableVibration(true)
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            notificationChannel.setSound(ringtone, Notification.AUDIO_ATTRIBUTES_DEFAULT)

            manager.createNotificationChannel(notificationChannel)
        }
        manager.notify(1, notification.build())
    }
}