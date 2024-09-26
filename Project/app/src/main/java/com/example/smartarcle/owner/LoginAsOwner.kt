package com.example.smartarcle.owner

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.smartarcle.Account
import com.example.smartarcle.R
import com.example.smartarcle.data.UserData
import com.example.smartarcle.databinding.ActivityLoginAsOwnerBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class LoginAsOwner : AppCompatActivity() {
    private lateinit var binding: ActivityLoginAsOwnerBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var userData: UserData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginAsOwnerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        userData = UserData(this)

        setLogin()
        setSignUp()
        setForgotPassword()
    }

    private fun setLogin() {
        binding.btnLoginOwn.setOnClickListener {
            val email = binding.edLoginEmailOwn.text.toString().trim()
            val pw = binding.edLoginPasswordOwn.text.toString().trim()

            if (email.isEmpty() || pw.isEmpty()) {
                showToast(getString(R.string.toast_fill))
            } else {
                auth.signInWithEmailAndPassword(email, pw).addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.let {
                            FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                                if (tokenTask.isSuccessful) {
                                    val token = tokenTask.result
                                    val tokenExpiryTime = System.currentTimeMillis() + 3600_000L
                                    saveUserTypeAndToken("owner", token, tokenExpiryTime)

                                    sendToken(email, token)
                                    updateToken(token)

                                    val login = Intent(this, HomeOwner::class.java)
                                    login.putExtra("owner", "owner")
                                    startActivity(login)
                                    finish()
                                } else {
                                    Log.e(ContentValues.TAG, "Failed to get Owner token", tokenTask.exception)
                                }
                            }
                        }
                    } else {
                        showToast(getString(R.string.toast_login_failed))
                        Log.e("AUTH", "Error login!", task.exception)
                    }
                }
            }
        }
    }

    private fun sendToken(email: String, token: String) {
        val requestBody = JSONObject().apply {
            put("email", email)
            put("token", token)
        }.toString()

        val url = "http://147.139.214.76:3000/post-token"
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
                Log.e("Backend", "Error updating token: ", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body.string().let {
                        Log.d("Backend", "Token updated successfully: $it")
                    }
                } else {
                    Log.e("Backend", "Error updating token: ${response.code}")
                }
            }
        })
    }

    private fun updateToken(token: String) {
        val firestore = FirebaseFirestore.getInstance()
        val user = auth.currentUser
        user?.let {
            val email = user.email
            if (email != null) {
                val tokenData = hashMapOf("token" to token)

                firestore.collection("users").document(email).set(tokenData).addOnSuccessListener {
                    Log.d(ContentValues.TAG, "Security token updated successfully in Firestore")
                }.addOnFailureListener {
                    Log.e(ContentValues.TAG, "Failed to update security token in Firestore", it)
                }
            }
        }
    }

    private fun saveUserTypeAndToken(userType: String, token: String, tokenExpiryTime: Long) {
        userData.setUserType(userType, token, tokenExpiryTime)
        saveToken(token)
        Log.d("Database", "User type and token saved: $userType")
    }

    private fun saveToken(token: String) {
        val sharedPrefs = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        val expiryTime = System.currentTimeMillis() + 3600_000L
        editor.putString("ownerToken", token)
        editor.putLong("tokenExpiryTime", expiryTime)
        editor.apply()
        Log.d("SharedPreferences", "ownerToken: $token")
    }

    private fun setSignUp() {
        binding.btnSignupOwn.setOnClickListener {
            val signup = Intent(this, SignUpAsOwner::class.java)
            startActivity(signup)
            finish()
        }
    }

    private fun setForgotPassword() {
        binding.tvForgotpwOwn.setOnClickListener {
            val resetPassword = Intent(this, ForgetPwOwner::class.java)
            startActivity(resetPassword)
        }
    }

    private fun showToast(text: String) {
        Toast.makeText(this@LoginAsOwner, text, Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, Account::class.java)
        startActivity(intent)
        finish()
    }
}