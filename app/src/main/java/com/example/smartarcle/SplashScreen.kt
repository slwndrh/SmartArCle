package com.example.smartarcle

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.smartarcle.data.LocaleLanguage
import com.example.smartarcle.data.UserData
import com.example.smartarcle.owner.HomeOwner
import com.example.smartarcle.security.HomeSecurity
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashScreen : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authListener: FirebaseAuth.AuthStateListener
    private lateinit var userData: UserData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleLanguage.setLocale(this, LocaleLanguage.getCurrentLanguage(this))
        setContentView(R.layout.activity_splash_screen)

        firebaseAuth = FirebaseAuth.getInstance()
        userData = UserData(this)

        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                currentUser.getIdToken(false).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val tokenResult = task.result
                        val token = tokenResult?.token
                        if (token != null) {
                            updateTokenExpiryTime(token)
                            val user = userData.getUserType()
                            if (user != null && user.token == token && !userData.isTokenExpired()) {
                                checkUserTypeAndRedirect(user.userType)
                            } else {
                                userData.deleteToken()
                                logoutUser()
                            }
                        } else {
                            logoutUser()
                        }
                    } else {
                        logoutUser()
                    }
                }
            } else {
                startActivity(Intent(this, Account::class.java))
                finish()
            }
        }
        firebaseAuth.addAuthStateListener(authListener)
    }

    override fun onResume() {
        super.onResume()
        firebaseAuth.addAuthStateListener(authListener)
    }

    override fun onStop() {
        super.onStop()
        firebaseAuth.removeAuthStateListener(authListener)
    }

    private fun checkUserTypeAndRedirect(userType: String) {
        when (userType) {
            "owner" -> {
                val intent = Intent(this, HomeOwner::class.java)
                startActivity(intent)
            }
            "security" -> {
                val intent = Intent(this, HomeSecurity::class.java)
                startActivity(intent)
            }
        }
        finish()
    }

    private fun updateTokenExpiryTime(token: String) {
        val oneHourInMillis = 3600_000L
        val currentTime = System.currentTimeMillis()
        val tokenExpiryTime = currentTime + oneHourInMillis // Set the token to expire in 1 hour
        val userType = firebaseAuth.currentUser?.let { userData.getUserType()?.userType } ?: ""
        userData.setUserType(userType, token, tokenExpiryTime)
    }

    private fun logoutUser() {
        userData.deleteToken()
        firebaseAuth.signOut()
        startActivity(Intent(this, Account::class.java))
        finish()
    }
}
