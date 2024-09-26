package com.example.smartarcle.security

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.smartarcle.Account
import com.example.smartarcle.R
import com.example.smartarcle.data.LocaleLanguage
import com.example.smartarcle.data.UserData
import com.example.smartarcle.databinding.ActivityHomeSecurityBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class HomeSecurity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeSecurityBinding
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth
    private lateinit var userData: UserData
    private var tokenOwner: String? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleLanguage.setLocale(this, LocaleLanguage.getCurrentLanguage(this))
        binding = ActivityHomeSecurityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        val secType = intent.getStringExtra("security")
        Log.d("security", "User type: $secType")

        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        userData = UserData(this)
        tokenOwner = userData.getToken()

        val ownerEmail = intent.getStringExtra("ownerEmail") ?: ""

        checkToken()
        getToken()
        setUser()
        setVehicleFound(ownerEmail)
        setLocVehicle()
        setSetting()
    }

    private fun checkToken() {
        val token = userData.getToken()
        if (token == null) {
            logoutUser()
        } else {
            Log.d("SQLite", "Valid security token retrieved: $token")
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
                Log.w("HomeSecurity", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val fcmToken = task.result!!
            Log.d("HomeSecurity", "FCM Token: $fcmToken")

            val expiryTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
            userData.setToken("security", fcmToken, expiryTime)

            saveTokenToFirestore(fcmToken)
        }
    }


    private fun saveTokenToFirestore(token: String?) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        val tokenMap = mapOf("token" to token)

        firestore.collection("security").document(email).set(tokenMap).addOnSuccessListener {
            Log.d("HomeSecurity", "FCM token successfully saved to Firestore")
        }.addOnFailureListener {
            Log.e("HomeSecurity", "Error saving FCM token to Firestore")
        }
    }

    private fun setUser() {
        binding.usernameHomeSec.text
        binding.profileHomeSec.setImageResource(R.drawable.security_acc)
    }

    private fun setVehicleFound(ownerEmail: String) {
        binding.cvVehicleFoundHomeSec.setOnClickListener {
            val securityEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
            Log.d(TAG, "Security email: $securityEmail")

            if (securityEmail.isNotEmpty()) {
                if (ownerEmail.isEmpty()) {
                    showToast(this, getString(R.string.toast_no_missing))
                } else {
                    Log.d(TAG, "Fetching owner token for: $ownerEmail")
                    fetchOwnerToken(ownerEmail, { ownerToken ->
                        Log.d(TAG, "Owner token fetched: $ownerToken")
                        setNotification(securityEmail, ownerToken)
                    }, { exception ->
                        Log.e(TAG, "Error fetching owner token: ${exception.message}")
                        showToast(this, getString(R.string.toast_no_missing))
                    })
                }
            } else {
                Log.e(TAG, "Security email is missing")
            }
        }
    }

    private fun fetchOwnerToken(ownerEmail: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        if (ownerEmail.isEmpty()) {
            Log.e(TAG, "Owner email is missing in fetchOwnerToken")
            onFailure(Exception("Owner email is missing"))
            return
        }

        val url = "http://147.139.214.76:3000/get-token/$ownerEmail"
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        Log.d(TAG, "Fetching owner token from: $url")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "Failed to fetch owner token: ${e.message}")
                onFailure(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body.string() ?: ""
                Log.d(TAG, "Response from /get-token: $responseBody")

                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch owner token: ${response.code} - $responseBody")
                    onFailure(IOException("Failed to fetch owner token"))
                } else {
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val ownerToken = jsonResponse.getString("token")
                        Log.d(TAG, "Fetched owner token successfully: $ownerToken")
                        onSuccess(ownerToken)
                    } catch (e: JSONException) {
                        Log.e(TAG, "Failed to parse owner token: ${e.message}")
                        onFailure(e)
                    }
                }
            }
        })
    }

    private fun setNotification(securityEmail: String, ownerToken: String) {
        val url = "http://147.139.214.76:3000/vehicle-found"
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val requestBody = JSONObject().apply {
            put("ownerToken", ownerToken)
            put("securityEmail", securityEmail)
            put("message", "Your vehicle has been found!")
        }.toString()

        Log.d(TAG, "Sending notification with body: $requestBody")

        val request = Request.Builder()
            .url(url)
            .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "Failed to send notification to owners: ${e.message}")
                runOnUiThread {
                    showToast(this@HomeSecurity, "Failed to send notification")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body.string() ?: ""
                Log.d(TAG, "Response from /vehicle-found: $responseBody")

                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to send notification to owners: ${response.message} - $responseBody")
                    runOnUiThread {
                        showToast(this@HomeSecurity, "Failed to send notification")
                    }
                    throw IOException("Unexpected code $response")
                }
                Log.d(TAG, "Notification sent to owners successfully")
                runOnUiThread {
                    showToast(this@HomeSecurity, "Notification sent successfully")
                }
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setLocVehicle() {
        binding.cvLocHomeSec.setOnClickListener {
            val intent = Intent(this, VehicleLocSecurity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setSetting() {
        binding.cvSettingHomeSec.setOnClickListener {
            val setting = Intent(this, SettingSecurity::class.java)
            startActivity(setting)
            finish()
        }
    }

    private fun showToast(text1: HomeSecurity, text: String) {
        Toast.makeText(this@HomeSecurity, text, Toast.LENGTH_SHORT).show()
    }
}