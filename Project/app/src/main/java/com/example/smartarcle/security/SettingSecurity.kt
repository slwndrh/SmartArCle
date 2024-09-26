package com.example.smartarcle.security

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import com.example.smartarcle.Account
import com.example.smartarcle.R
import com.example.smartarcle.data.LocaleLanguage
import com.example.smartarcle.data.UserData
import com.example.smartarcle.databinding.ActivitySettingSecurityBinding
import com.google.firebase.auth.FirebaseAuth

class SettingSecurity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingSecurityBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var userData: UserData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleLanguage.setLocale(this, LocaleLanguage.getCurrentLanguage(this))
        binding = ActivitySettingSecurityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.setting)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        firebaseAuth = FirebaseAuth.getInstance()
        userData = UserData(this)

        setVehicle()
        setChangeLanguage()
        updateLanguageText()
        setLogOut()
    }

    private fun setChangeLanguage() {
        binding.cvLanguageSec.setOnClickListener {
            setLanguage()
        }
    }

    private fun setLanguage() {
        val currentLanguage = LocaleLanguage.getCurrentLanguage(this)
        val newLanguage = if (currentLanguage == "en") "id" else "en"
        LocaleLanguage.setLocale(this, newLanguage)
        recreate()
    }

    private fun updateLanguageText() {
        val tvLanguage = findViewById<TextView>(R.id.tv_language_sec)

        val currentLanguage = LocaleLanguage.getCurrentLanguage(this)
        if (currentLanguage == "en") {
            tvLanguage.text = getString(R.string.change_to_ind)
        } else {
            tvLanguage.text = getString(R.string.change_to_eng)
        }
    }

    private fun setVehicle() {
        binding.cvVehicleProfileSec.setOnClickListener {
            val vehicle = Intent(this, VehicleProfileSecurity::class.java)
            startActivity(vehicle)
            finish()
        }
    }

    private fun setLogOut() {
        binding.cvLogoutSec.setOnClickListener {
            userData.deleteToken()
            Log.d("SQLite", "Token expired. Logging out the user.")

            val auth = FirebaseAuth.getInstance()
            auth.signOut()
            clearUserType(this)

            val logout = Intent(this, Account::class.java)
            logout.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(logout)
            showToast(getString(R.string.logout))
            finish()
        }
    }

    private fun clearUserType(context: Context) {
        val sharedPrefs = context.getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.remove("userType")
        editor.apply()
    }

    private fun showToast(text: String) {
        Toast.makeText(this@SettingSecurity, text, Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, HomeSecurity::class.java)
        startActivity(intent)
        finish()
    }
}