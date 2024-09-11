package com.example.smartarcle.data

import android.app.Service
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.smartarcle.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class NotificationService : Service() {
    private val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    private lateinit var notificationSetup: NotificationSetup

    override fun onCreate() {
        super.onCreate()
        notificationSetup = NotificationSetup(this)
        theftNotify()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun theftNotify() {
        val reference = database.getReference("Motor")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { motorSnapshot ->
                    val email = motorSnapshot.key.toString()
                    val alarm = motorSnapshot.child("Alarm").getValue(String::class.java) ?: "0"
                    val button = motorSnapshot.child("Button").getValue(String::class.java) ?: "0"

                    Log.d(TAG, "Alarm: $alarm, Button: $button, Email: $email")

                    if (alarm == "1" && button == "1") {
                        Log.d(TAG, "Condition met: Sending notification")
                        notificationSetup.sendTheftNotification()
                    } else {
                        Log.d(TAG, "Condition not met: No notification")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, R.string.load_post_onCancelled.toString(), error.toException())
            }
        })
    }
}