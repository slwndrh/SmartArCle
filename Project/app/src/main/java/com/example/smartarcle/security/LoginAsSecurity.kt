package com.example.smartarcle.security

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.smartarcle.Account
import com.example.smartarcle.R
import com.example.smartarcle.data.LocaleLanguage
import com.example.smartarcle.data.UserData
import com.example.smartarcle.databinding.ActivityLoginAsSecurityBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class LoginAsSecurity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginAsSecurityBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var userData: UserData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleLanguage.setLocale(this, LocaleLanguage.getCurrentLanguage(this))
        binding = ActivityLoginAsSecurityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        userData = UserData(this)

        setLogin()
        setForgotPassword()
    }

    private fun setLogin() {
        binding.btnLoginSec.setOnClickListener {
            val email = binding.edLoginEmailSec.text.toString().trim()
            val pw = binding.edLoginPasswordSec.text.toString().trim()

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
                                    saveUserTypeAndToken("security", token, tokenExpiryTime)
                                    Log.d(TAG, "Security Token: $token")

                                    updateToken(token)

                                    val login = Intent(this, HomeSecurity::class.java)
                                    login.putExtra("security", "security")
                                    startActivity(login)
                                    finish()
                                } else {
                                    Log.e(TAG, "Failed to get Security token", tokenTask.exception)
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

    private fun updateToken(token: String) {
        val firestore = FirebaseFirestore.getInstance()
        val user = auth.currentUser
        user?.let {
            val email = user.email
            if (email != null) {
                val tokenData = hashMapOf("token" to token)

                firestore.collection("security").document(email).set(tokenData).addOnSuccessListener {
                    Log.d(TAG, "Security token updated successfully in Firestore")
                }.addOnFailureListener {
                    Log.e(TAG, "Failed to update security token in Firestore", it)
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
        editor.putString("securityToken", token)
        editor.putLong("tokenExpiryTime", expiryTime)
        editor.apply()
        Log.d("SharedPreferences", "securityToken: $token")
    }

    private fun setForgotPassword() {
        binding.tvForgotpwSec.setOnClickListener {
            val resetPassword = Intent(this, ForgetPwSecurity::class.java)
            startActivity(resetPassword)
        }
    }

    private fun showToast(text: String) {
        Toast.makeText(this@LoginAsSecurity, text, Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, Account::class.java)
        startActivity(intent)
        finish()
    }
}