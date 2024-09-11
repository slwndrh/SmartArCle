package com.example.smartarcle.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.smartarcle.R
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GeofenceService : Service() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_GEOFENCE_EVENT) {
            handleGeofenceEvent(intent)
        }
        return START_NOT_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleGeofenceEvent(intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e("GeofenceService", errorMessage)
            sendNotification("Error: $errorMessage")
            return
        }
        val geofenceTransition = geofencingEvent.geofenceTransition
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            triggeringGeofences?.forEach { geofence ->
                val geofenceTransitionDetails = geofence.requestId
                Log.i("GeofenceService", geofenceTransitionDetails)
                // Fetch user data and send notification
                fetchUserDataAndSendNotification(geofenceTransitionDetails)
            }
        } else {
            val errorMessage = "Invalid transition type: $geofenceTransition"
            Log.e("GeofenceService", errorMessage)
            sendNotification(errorMessage)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchUserDataAndSendNotification(geofenceTransitionDetails: String) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
        val db = FirebaseFirestore.getInstance()

        val profile = db.collection("owner").document(email).collection("profile").document("profile_data")
        val vehicle = db.collection("vehicle_data").document(email)

        profile.get().addOnSuccessListener { profileSnapshot ->
            val ownerName = profileSnapshot.getString("name") ?: ""
            vehicle.get().addOnSuccessListener { vehicleSnapshot ->
                val plateNumber = vehicleSnapshot.getString("plate") ?: ""
                val notificationMessage = "$geofenceTransitionDetails: The $ownerName's vehicle with License Plate Number $plateNumber has exited the geofencing area. Immediately report to security!"
                sendNotification(notificationMessage)
            }.addOnFailureListener { e ->
                Log.e("FirestoreError", "Error getting vehicle data", e)
                sendNotification("Error getting vehicle data")
            }
        }.addOnFailureListener { e ->
            Log.e("FirestoreError", "Error getting profile data", e)
            sendNotification("Error getting profile data")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendNotification(message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val bigTextStyle = NotificationCompat.BigTextStyle().bigText(message)
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.vehicle)
            .setContentTitle("Geofence Alert")
            .setStyle(bigTextStyle)
            .setVibrate(longArrayOf(0, 500, 500, 500))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 500, 500)
                setSound(null, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            }
            notificationBuilder.setChannelId(CHANNEL_ID)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = notificationBuilder.build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val ACTION_GEOFENCE_EVENT = "GeofenceEvent"
        const val CHANNEL_ID = "Geofence Channel"
        const val CHANNEL_NAME = "Geofence Notification"
        const val NOTIFICATION_ID = 2
    }
}