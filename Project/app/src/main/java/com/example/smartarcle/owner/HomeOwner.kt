package com.example.smartarcle.owner

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.example.smartarcle.Account
import com.example.smartarcle.R
import com.example.smartarcle.data.NotificationService
import com.example.smartarcle.data.UserData
import com.example.smartarcle.databinding.ActivityHomeOwnerBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.database.ValueEventListener
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class HomeOwner : AppCompatActivity() {
    private lateinit var binding: ActivityHomeOwnerBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var image : ImageView
    private lateinit var userData: UserData
    private val firestore = FirebaseFirestore.getInstance()
    private var db = Firebase.firestore
    private var securityToken: String = ""

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeOwnerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        val ownerType = intent.getStringExtra("owner")
        Log.d("owner", "User type: $ownerType")

        FirebaseApp.initializeApp(this)
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()
        userData = UserData(this)

        saveEmailToPreferences()
        startService(Intent(this, NotificationService::class.java))

        checkToken()
        getToken()
        setUser()
        setAccount()
        setParking()
        setDrive()
        setReportMissing()
        setReportPolice()
        setVehicleLoc()
        setSetting()
    }

    private fun saveEmailToPreferences() {
        val email = auth.currentUser?.email ?: return
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("email", email)
            apply()
        }
    }

    private fun checkToken() {
        val token = userData.getToken()
        if (token == null) {
            logoutUser()
        } else {
            Log.d("SQLite", "Valid owner token retrieved: $token")
        }
    }

    private fun logoutUser() {
        userData.deleteToken()
        Log.d("SQLite", "Token expired. Logging out the user.")

        FirebaseAuth.getInstance().signOut()

        val intent = Intent(this, Account::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun getToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("HomeOwner", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val fcmToken = task.result!!
            Log.d("HomeOwner", "FCM Token: $fcmToken")

            val expiryTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
            userData.setToken("owner", fcmToken, expiryTime)

            saveTokenToFirestore(fcmToken)
        }
    }


    private fun saveTokenToFirestore(token: String?) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        val tokenMap = mapOf("token" to token)

        firestore.collection("users").document(email)
            .set(tokenMap)
            .addOnSuccessListener {
                Log.d("HomeOwner", "FCM token successfully saved to Firestore")
            }
            .addOnFailureListener {
                Log.e("HomeOwner", "Error saving FCM token to Firestore")
            }
    }

    private fun setUser() {
        val email = auth.currentUser?.email ?: ""
        if (email.isEmpty()) {
            Log.e(TAG, "User is not logged in")
            return
        }
        val refProf = db.collection("owner").document(email)
            .collection("profile").document("profile_data")
        val refVehi = db.collection("vehicle_data").document(email)
        image = findViewById(R.id.profile_home_own)

        refProf.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val imageUrl = document.getString("url")
                imageUrl?.let {
                    Glide.with(this)
                        .load(it)
                        .into(image)
                }

                val name = document.getString("name")
                binding.usernameHomeOwn.text = name
            } else {
                Log.d(TAG, "No such document")
            }
        }.addOnFailureListener { e ->
            showToast(this, "getString(R.string.toast_failed_load) ${e.message}")
            Log.e(TAG, "Failed to load user data", e)
        }

        refVehi.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val plate = document.getString("plate")
                binding.plateHomeOwn.text = plate
            } else {
                Log.d(TAG, "No such document")
            }
        }.addOnFailureListener { e ->
            showToast(this, "getString(R.string.toast_failed_load) ${e.message}")
            Log.e(TAG, "Failed to load user data", e)
        }
    }

    private fun setAccount() {
        binding.profileHomeOwn.setOnClickListener {
            val profile = Intent (this, ProfileOwner::class.java)
            startActivity(profile)
            finish()
        }
    }

    private fun setParking() {
        binding.cvParkingHomeOwn.setOnClickListener {
            setRelayState("1")
        }
    }

    private fun setDrive() {
        binding.cvDriveHomeOwn.setOnClickListener {
            setRelayState("0")
        }
    }

    private fun setRelayState(state: String) {
        val email = auth.currentUser?.email?.replace(".", "_") ?: ""
        val buttonRef = database.child("Motor").child(email).child("Button")
        buttonRef.setValue(state).addOnSuccessListener {
            Log.d(TAG, "Relay state set to $state successfully.")
            setToastRelay()
        }.addOnFailureListener {
            Log.e(TAG, "Failed to set relay state to $state.")
            showToast(this, getString(R.string.toast_failed_relay))
        }
    }

    private fun setToastRelay() {
        val email = auth.currentUser?.email?.replace(".", "_") ?: ""
        val buttonRef = database.child("Motor").child(email).child("Button")
        buttonRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val state = dataSnapshot.getValue(String::class.java)
                Log.d(TAG, "Current button state: $state")
                if (state == "1") {
                    showToast(this@HomeOwner, getString(R.string.toast_parking))
                } else if (state == "0") {
                    showToast(this@HomeOwner, getString(R.string.toast_drive))
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "Failed to read button state: ${databaseError.message}")
                showToast(this@HomeOwner, getString(R.string.toast_failed_relay))
            }
        })
    }

    private fun setReportMissing() {
        val motorRef = database.child("Motor")
        val email = auth.currentUser?.email?.replace(".", "_") ?: ""

        val user = motorRef.child(email)
        user.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val alarm = snapshot.child("Alarm").getValue(String::class.java) ?: "0"
                val button = snapshot.child("Button").getValue(String::class.java) ?: "0"

                val shouldEnableButton = alarm == "1" && button == "1"
                Log.d(TAG, "Should enable button: $shouldEnableButton")

                binding.cvReportHomeOwn.setOnClickListener {
                    if (shouldEnableButton) {
                        reportMissing()
                    } else {
                        Log.d(TAG, "Conditions not met to report missing")
                        showToast(this@HomeOwner, getString(R.string.toast_no_theft))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching data from Realtime Database: ${error.message}")
            }
        })
    }

    private fun reportMissing() {
        val email = auth.currentUser?.email ?: ""
        val refVehi = db.collection("vehicle_data").document(email)
        Log.d(TAG, "Fetching vehicle data for email: $email")
        refVehi.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val plate = document.getString("plate")
                if (plate != null) {
                    fetchSecurityToken(
                        onSuccess = { fetchLocationAndSendNotification(plate) },
                        onFailure = { e -> showToast(
                            this,
                            getString(R.string.toast_failed_relay) + " ${e.message}"
                        )
                        })
                } else {
                    Log.d(TAG, "Plate number not found")
                    showToast(this, getString(R.string.toast_plate))
                }
            } else {
                Log.d(TAG, "No such document")
                showToast(this, "No such document in vehicle data")
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error fetching vehicle data", e)
            showToast(this, getString(R.string.toast_error_fetch) + " ${e.message}")
        }
    }

    private fun fetchSecurityToken(onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val securityEmail = "security@security.com"
        firestore.collection("security")
            .document(securityEmail)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val token = document.getString("token")
                    if (token != null) {
                        securityToken = token
                        Log.d("HomeOwner", "Security token: $securityToken")
                        onSuccess(token)
                    } else {
                        Log.e("HomeOwner", "Security token not found")
                        onFailure(Exception("Security token not found"))
                    }
                } else {
                    Log.e("HomeOwner", "No such document")
                    onFailure(Exception("No such document"))
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to retrieve security token", exception)
                showToast(this, "getString(R.string.toast_failed_retrieve), exception")
                onFailure(exception)
            }
    }

    private fun fetchLocationAndSendNotification(plateNumber: String) {
        val email = auth.currentUser?.email?.replace(".", "_") ?: ""
        val locationRef = database.child("Motor").child(email)

        locationRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val latitude = snapshot.child("Latitude").getValue(String::class.java)
                val longitude = snapshot.child("Longitude").getValue(String::class.java)

                if (latitude != null && longitude != null) {
                    val location = "$latitude,$longitude"
                    sendNotificationReport(plateNumber, location)
                } else {
                    showToast(this@HomeOwner, getString(R.string.toast_failed_loc))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast(
                    this@HomeOwner,
                    getString(R.string.toast_failed_loc) + ": ${error.message}"
                )
            }
        })
    }

    private fun fetchOwnerToken(ownerEmail: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        if (ownerEmail.isEmpty()) {
            onFailure(Exception("Owner email is missing"))
            return
        }

        val url = "http://147.139.214.76:3000/get-token/$ownerEmail"
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "Failed to fetch owner token: ${e.message}")
                onFailure(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body.string()
                Log.d(TAG, "Response from /get-token: $responseBody")
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch owner token: ${response.code} - $responseBody")
                    onFailure(IOException("Failed to fetch owner token"))
                } else {
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val ownerToken = jsonResponse.getString("token")
                        onSuccess(ownerToken)
                    } catch (e: JSONException) {
                        Log.e(TAG, "Failed to parse owner token: ${e.message}")
                        onFailure(e)
                    }
                }
            }
        })
    }

    private fun sendNotificationReport(plateNumber: String, location: String) {
        val ownerName = auth.currentUser?.displayName ?: "Owner"
        val ownerEmail = auth.currentUser?.email ?: ""
        val securityEmail = "security@security.com"

        if (plateNumber.isEmpty() || location.isEmpty() || ownerName.isEmpty() || ownerEmail.isEmpty() || securityEmail.isEmpty()) {
            Log.e(TAG, "Missing required fields")
            showToast(this, "Missing required fields")
            return
        }

        fetchOwnerToken(ownerEmail, { ownerToken ->
            val requestBody = JSONObject().apply {
                put("ownerName", ownerName)
                put("plateNumber", plateNumber)
                put("location", location)
                put("securityEmail", securityEmail)
                put("ownerEmail", ownerEmail)
                put("ownerToken", ownerToken)
            }.toString()

            Log.d(TAG, "Request Body: $requestBody")

            val url = "http://147.139.214.76:3000/report-missing"
            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(url)
                .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    Log.e(TAG, "Failed to send report: ${e.message}")
                    runOnUiThread {
                        showToast(this@HomeOwner, "Failed to send report: ${e.message}")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body.string()
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Failed to send report: ${response.code} - ${responseBody}")
                        runOnUiThread {
                            showToast(
                                this@HomeOwner,
                                "Failed to send report: ${response.code} - ${responseBody}"
                            )
                        }
                    } else {
                        Log.d(TAG, "Report has been sent to security")
                        runOnUiThread {
                            showToast(this@HomeOwner, "Report has been sent to security")
                        }
                    }
                }
            })
        }, { exception ->
            Log.e(TAG, "Error fetching owner token: ${exception.message}")
            runOnUiThread {
                showToast(this, "Error fetching owner token")
            }
        })
    }


    private fun setReportPolice() {
        binding.cvReportPoliceHomeOwn.setOnClickListener {
            val phoneNumber = "0341364211"
            val dialPhoneIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            startActivity(dialPhoneIntent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setVehicleLoc() {
        binding.cvVehicleLocHomeOwn.setOnClickListener {
            val setting = Intent(this, VehicleLocOwner::class.java)
            startActivity(setting)
        }
    }

    private fun setSetting() {
        binding.cvSettingHomeOwn.setOnClickListener {
            val setting = Intent(this, SettingOwner::class.java)
            startActivity(setting)
            finish()
        }
    }

    private fun showToast(text1: HomeOwner, text: String) {
        Toast.makeText(this@HomeOwner, text, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        checkToken()
    }
}