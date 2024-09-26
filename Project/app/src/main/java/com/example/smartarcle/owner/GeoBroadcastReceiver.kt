package com.example.smartarcle.owner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.smartarcle.R
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class GeoBroadcastReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_GEOFENCE_EVENT) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
            if (geofencingEvent.hasError()) {
                val errorMessage =
                    GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
                Log.e(TAG, errorMessage)
                sendNotification(context, errorMessage)
                return
            }
//            val geofenceTransition = geofencingEvent.geofenceTransition
//            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
//            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
//            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
//            val triggeringGeofences = geofencingEvent.triggeringGeofences
//                triggeringGeofences?.forEach { geofence ->
//                    val geofenceRequestId = geofence.requestId
//                    val geofenceTransitionDetails = when (geofenceTransition) {
//                        Geofence.GEOFENCE_TRANSITION_ENTER -> "Entered $geofenceRequestId"
//                        Geofence.GEOFENCE_TRANSITION_EXIT -> "Exited from $geofenceRequestId"
//                        else -> "Unknown geofence transition"
//                    }
//                    Log.i(TAG, geofenceTransitionDetails)
//            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
//                val triggeringGeofences = geofencingEvent.triggeringGeofences
//                if (triggeringGeofences != null && triggeringGeofences.isNotEmpty()) {
//                    val geofenceTransitionDetails = triggeringGeofences.joinToString(", ") { it.requestId }
//                    checkButtonStateAndFetchData(context, geofenceTransitionDetails)
//                }
            val geofenceTransition = geofencingEvent.geofenceTransition
            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                val triggeringGeofences = geofencingEvent.triggeringGeofences
                triggeringGeofences?.forEach { geofence ->
                    val geofenceTransitionDetails = geofence.requestId
                    Log.i(TAG, geofenceTransitionDetails)
                    checkButtonStateAndFetchData(context, geofenceTransitionDetails)
//                    sendNotification(context, geofenceTransitionDetails)
                }
            } else {
                val errorMessage = "Invalid transition type: $geofenceTransition"
                Log.e(TAG, errorMessage)
                sendNotification(context, errorMessage)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkButtonStateAndFetchData(context: Context, geofenceTransitionDetails: String) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
        val emailUser = email.replace(".", "_")
        val db = FirebaseDatabase.getInstance().reference

        // Reference to the button state
        val buttonRef = db.child(emailUser).child("Alarm")

        buttonRef.get().addOnSuccessListener { buttonSnapshot ->
            val buttonState = buttonSnapshot.getValue(String::class.java)
            if (buttonState == "1") {
                // Button is ON, proceed to fetch user data and send notification
                fetchUserDataAndSendNotification(context, geofenceTransitionDetails)
            } else {
                // Button is OFF, no notification should be sent
                Log.i(TAG, "Button state is OFF, no notification will be sent.")
            }
        }.addOnFailureListener { e ->
            Log.e("FirestoreError", "Error getting button state", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun fetchUserDataAndSendNotification(context: Context, geofenceTransitionDetails: String) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
        val db = FirebaseFirestore.getInstance()

        val profile = db.collection("owner").document(email).collection("profile").document("profile_data")
        val vehicle = db.collection("vehicle_data").document(email)

        profile.get().addOnSuccessListener { profileSnapshot ->
            val ownerName = profileSnapshot.getString("name") ?: ""

            vehicle.get().addOnSuccessListener { vehicleSnapshot ->
                val plateNumber = vehicleSnapshot.getString("plate") ?: ""
                val notificationMessage = "$geofenceTransitionDetails: The $ownerName's vehicle with License Plate Number $plateNumber has exited the geofencing area. Immediately report to security!"

                sendNotification(context,  notificationMessage)
            }.addOnFailureListener { e ->
                Log.e("FirestoreError", "Error getting vehicle data", e)
                sendNotification(context, "Error getting vehicle data")
            }
        }.addOnFailureListener { e ->
            Log.e("FirestoreError", "Error getting profile data", e)
            sendNotification(context, "Error getting profile data")
        }
    }

//    @RequiresApi(Build.VERSION_CODES.S)
//    private fun sendNotification(context: Context, message: String) {
//        val intent = Intent(context, VehicleLocOwner::class.java)
//        intent.action = "GEOFENCE_NOTIFICATION"
//        intent.putExtra("MESSAGE", message)
//        context.sendBroadcast(intent)
//    }

    private fun sendNotification(context: Context, message: String) {
        val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val bigTextStyle = NotificationCompat.BigTextStyle().bigText(message)
        val mBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.vehicle)
            .setContentTitle("Geofence Alert")
            .setContentText(message)
            .setStyle(bigTextStyle)
            .setVibrate(longArrayOf(0, 500, 500, 500))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 500, 500)
                setSound(null, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(
                    AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            }

            mBuilder.setChannelId(CHANNEL_ID)
            mNotificationManager.createNotificationChannel(channel)
        }

        val notification = mBuilder.build()
        mNotificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "GeofenceBroadcast"
        const val ACTION_GEOFENCE_EVENT = "GeofenceEvent"
        private const val CHANNEL_ID = "Geofence Channel"
        private const val CHANNEL_NAME = "Geofence Notification"
        private const val NOTIFICATION_ID = 2
    }
}
