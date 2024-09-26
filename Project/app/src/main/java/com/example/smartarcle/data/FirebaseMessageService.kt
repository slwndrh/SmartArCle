package com.example.smartarcle.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.smartarcle.R
import com.example.smartarcle.security.VehicleLocSecurity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.*

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class FirebaseMessageService : FirebaseMessagingService() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        when (data["notificationType"] ?: "default") {
            "VehicleReportedMissing" -> setVehicleReportedMissing(data)
            "ToastOwner" -> setToastOwner(data)
            "ResponseToOwner" -> setVehicleFound(data)
            "ToastSecurity" -> setToastSecurity(data)
            else -> Log.e("FirebaseMessageService", "Unknown notification type")
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setVehicleReportedMissing(data: Map<String, String>) {
        val ownerName = data["ownerName"] ?: ""
        val plateNumber = data["plateNumber"] ?: ""
        val location = data["location"] ?: ""
        val ownerEmail = data["ownerEmail"] ?: ""
        val latitude = data["latitude"] ?: ""
        val longitude = data["longitude"] ?: ""
        val ownerToken = data["ownerToken"] ?: ""

        Log.d("FirebaseMessageService", "Received data: $data")
        Log.d("FirebaseMessageService", "Extracted location: $location")

        val channelId = "vehicle_reported_missing"
        val channelName = "Vehicle Reported Missing"

        val ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Vehicle Reported Missing!"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(ringtone, Notification.AUDIO_ATTRIBUTES_DEFAULT)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, VehicleLocSecurity::class.java).apply {
            putExtra("location", location)
            putExtra("latitude", latitude)
            putExtra("longitude", longitude)
            putExtra("plateNumber", plateNumber)
            putExtra("ownerEmail", ownerEmail)
            putExtra("ownerToken", ownerToken)
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val bigTextStyle = NotificationCompat
            .BigTextStyle()
            .bigText("The $ownerName's vehicle with License Plate Number $plateNumber has been stolen. Immediately track the vehicle by clicking view vehicle location.")

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.vehicle)
            .setContentTitle("Vehicle Reported Missing")
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "View Vehicle Location", pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setSound(ringtone)
            .setFullScreenIntent(pendingIntent, true)
            .build()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            with(NotificationManagerCompat.from(this)) {
                notify(3, notification)
            }
        } else {
            Log.e(TAG, "Permission Denied")
        }
    }

    private fun setVehicleFound(data: Map<String, String>) {
        val message = data["message"] ?: "Vehicle has been found, immediately go to the security to pick up the vehicle."

        val channelId = "vehicle_found"
        val channelName = "Vehicle Found"

        val ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Vehicle Found Notification"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(ringtone, Notification.AUDIO_ATTRIBUTES_DEFAULT)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(message)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.vehicle)
            .setContentTitle("Vehicle Found")
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setSound(ringtone)
            .build()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            with(NotificationManagerCompat.from(this)) {
                notify(4, notification)
            }
        } else {
            Log.e(TAG, "Permission Denied for POST_NOTIFICATIONS")
        }
    }

    private fun setToastOwner(data: Map<String, String>) {
        val message = data["body"] ?: getString(R.string.toast_success_report)
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun setToastSecurity(data: Map<String, String>) {
        val message = data["body"] ?: getString(R.string.toast_success_found)
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val TAG = "FirebaseMessageService"
    }
}