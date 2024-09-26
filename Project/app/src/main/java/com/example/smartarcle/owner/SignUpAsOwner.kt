package com.example.smartarcle.owner

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.smartarcle.R
import com.example.smartarcle.databinding.ActivitySignUpAsOwnerBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

class SignUpAsOwner : AppCompatActivity() {
    private lateinit var binding : ActivitySignUpAsOwnerBinding
    private lateinit var auth: FirebaseAuth
    private var db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpAsOwnerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        auth = FirebaseAuth.getInstance()

        setSignUp()
    }

    private fun setSignUp() {
        binding.btnSignupOwn.setOnClickListener {
            val edName = binding.edSignupNameOwn.text.toString().trim()
            val edEmail = binding.edSignupEmailOwn.text.toString().trim()
            val edPw = binding.edSignupPasswordOwn.text.toString().trim()

            if (edName.isEmpty() || edEmail.isEmpty() || edPw.isEmpty()) {
                showToast(getString(R.string.toast_fill))
            } else {
                auth.createUserWithEmailAndPassword(edEmail, edPw).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        getUserIdAndToken()
                        val user = auth.currentUser?.email ?: ""

                        val userMap = hashMapOf("name" to edName, "email" to edEmail)
                        if (user.isNotEmpty()) {
                            db.collection("owner").document(user).collection("profile").document("profile_data").set(userMap).addOnCompleteListener { firestore ->
                                if (firestore.isSuccessful) {
                                    showToast(getString(R.string.toast_signup))
                                    binding.edSignupNameOwn.text?.clear()
                                    binding.edSignupEmailOwn.text?.clear()
                                    binding.edSignupPasswordOwn.text?.clear()
                                    binding.edSignupConfirmOwn.text?.clear()

                                    val nama = auth.currentUser
                                    val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(edName).build()
                                    nama?.updateProfile(profileUpdates)

                                    FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                                        if (tokenTask.isSuccessful) {
                                            val fcmToken = tokenTask.result
                                            Log.d("FCM", "FCM Token: $fcmToken")

                                            val userData = hashMapOf("fcmToken" to fcmToken, "userType" to "ownerType")
                                            db.collection("users").document(user).set(userData, SetOptions.merge()).addOnSuccessListener {
                                                Log.d("Firestore", "FCM token successfully written!")

                                                saveUserType(this, "ownerType")
                                                val userType = getUserType(this)
                                                val loginIntent = Intent(this, LoginAsOwner::class.java)
                                                loginIntent.putExtra(userType, "ownerType")
                                                startActivity(loginIntent)
                                                finish()
                                            }.addOnFailureListener { e ->
                                                Log.w("Firestore", "Error writing document", e)
                                            }
                                        } else {
                                            Log.w("FCM", "Fetching FCM registration token failed", tokenTask.exception)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        showToast(getString(R.string.toast_failed_signup))
                        Log.e("AUTH", "Sign Up Failed!", task.exception)
                    }
                }
            }
        }
    }

    private fun getUserIdAndToken() {
        val user = auth.currentUser?.email ?: ""
        user.let {
            Log.d("AUTH", "User ID: $it")

            FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                if (tokenTask.isSuccessful) {
                    val fcmToken = tokenTask.result
                    Log.d("FCM", "FCM Token: $fcmToken")
                } else {
                    Log.w("FCM", "Fetching FCM registration token failed", tokenTask.exception)
                }
            }
        }
    }

    private fun saveUserType(context: Context, userType: String) {
        val sharedPreferences = context.getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("userType", userType)
        editor.apply()
    }

    private fun getUserType(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val userType = sharedPreferences.getString("userType", null)
        Log.d("SharedPreferences", "User type retrieved: $userType")
        return userType
    }

    private fun showToast(text: String) {
        Toast.makeText(this@SignUpAsOwner, text, Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, LoginAsOwner::class.java)
        startActivity(intent)
        finish()
    }
}