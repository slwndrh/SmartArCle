package com.example.smartarcle.owner

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
import com.example.smartarcle.databinding.ActivitySettingOwnerBinding
import com.google.firebase.auth.FirebaseAuth

class SettingOwner : AppCompatActivity() {
    private lateinit var binding: ActivitySettingOwnerBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var userData: UserData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleLanguage.setLocale(this, LocaleLanguage.getCurrentLanguage(this))
        binding = ActivitySettingOwnerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.setting)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        firebaseAuth = FirebaseAuth.getInstance()
        userData = UserData(this)

        setProfile()
        setVehicle()
        setChangeLanguage()
        updateLanguageText()
        setLogOut()
    }

    private fun setChangeLanguage() {
        binding.cvLanguageOwn.setOnClickListener {
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
        val tvLanguage = findViewById<TextView>(R.id.tv_language_own)

        val currentLanguage = LocaleLanguage.getCurrentLanguage(this)
        if (currentLanguage == "en") {
            tvLanguage.text = getString(R.string.change_to_ind)
        } else {
            tvLanguage.text = getString(R.string.change_to_eng)
        }
    }

    private fun setLogOut() {
        binding.cvLogoutOwn.setOnClickListener {
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

    private fun setVehicle() {
        binding.cvVehicleProfileOwn.setOnClickListener {
            val veProf = Intent(this, VehicleProfileOwner::class.java)
            startActivity(veProf)
            finish()
        }
    }

    private fun setProfile() {
        binding.cvUserProfileOwn.setOnClickListener {
            val profile = Intent(this, ProfileOwner::class.java)
            startActivity(profile)
            finish()
        }
    }

    private fun showToast(text: String) {
        Toast.makeText(this@SettingOwner, text, Toast.LENGTH_SHORT).show()
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
        val intent = Intent(this, HomeOwner::class.java)
        startActivity(intent)
        finish()
    }
}