package com.example.smartarcle.owner

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartarcle.R
import com.example.smartarcle.databinding.ActivityForgetPwOwnerBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ForgetPwOwner : AppCompatActivity() {
    private lateinit var binding: ActivityForgetPwOwnerBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgetPwOwnerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setChangePassword()
    }

    private fun setChangePassword() {
        binding.btnChangeForgetOwn.setOnClickListener {
            val email = binding.edEmailForgetOwn.text.toString().trim()
//            val newPassword = binding.edNewForgetOwn.text.toString().trim()
//            val confirmPassword = binding.edConfirmForgetOwn.text.toString().trim()

            if (email.isEmpty()) {
                binding.edEmailForgetOwn.error = getString(R.string.fill_email)
                return@setOnClickListener
            }

//            if (newPassword.isEmpty()) {
//                binding.edNewForgetOwn.error = getString(R.string.fill_pw)
//                return@setOnClickListener
//            }
//
//            if (newPassword != confirmPassword) {
//                binding.edConfirmForgetOwn.error = getString(R.string.pw_not_match)
//                return@setOnClickListener
//            }
            changePassword(email)
        }
    }

    private fun changePassword(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                showToast(getString(R.string.toast_reset_email))
                finish()
            } else {
                showToast("getString(R.string.toast_reset_failed)}: ${task.exception?.message}")
            }
        }
    }

    private fun showToast(text: String) {
        Toast.makeText(this@ForgetPwOwner, text, Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, LoginAsOwner::class.java)
        startActivity(intent)
        finish()
    }
}