package com.example.smartarcle.security

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartarcle.R
import com.example.smartarcle.data.LocaleLanguage
import com.example.smartarcle.databinding.ActivityForgetPwSecurityBinding
import com.google.firebase.auth.FirebaseAuth

class ForgetPwSecurity : AppCompatActivity() {
    private lateinit var binding: ActivityForgetPwSecurityBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleLanguage.setLocale(this, LocaleLanguage.getCurrentLanguage(this))
        binding = ActivityForgetPwSecurityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        firebaseAuth = FirebaseAuth.getInstance()

        setChangePassword()
    }

    private fun setChangePassword() {
        binding.btnChangeForgetSec.setOnClickListener {
            val email = binding.edEmailForgetSec.text.toString().trim()
            val newPassword = binding.edNewForgetSec.text.toString().trim()
            val confirmPassword = binding.edConfirmForgetSec.text.toString().trim()

            if (email.isEmpty()) {
                binding.edEmailForgetSec.error = getString(R.string.fill_email)
                return@setOnClickListener
            }

            if (newPassword.isEmpty()) {
                binding.edNewForgetSec.error = getString(R.string.fill_pw)
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                binding.edConfirmForgetSec.error = getString(R.string.pw_not_match)
                return@setOnClickListener
            }

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
        Toast.makeText(this@ForgetPwSecurity, text, Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, LoginAsSecurity::class.java)
        startActivity(intent)
        finish()
    }
}