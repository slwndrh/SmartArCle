package com.example.smartarcle

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.smartarcle.data.LocaleLanguage
import com.example.smartarcle.data.UserData
import com.example.smartarcle.databinding.ActivityAccountBinding
import com.example.smartarcle.owner.LoginAsOwner
import com.example.smartarcle.security.LoginAsSecurity

class Account : AppCompatActivity() {
    private lateinit var binding: ActivityAccountBinding
    private lateinit var userData: UserData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleLanguage.setLocale(this, LocaleLanguage.getCurrentLanguage(this))
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        userData = UserData(this)

        setOwner()
        setSecurity()
    }

    private fun setSecurity() {
        binding.cvSecurityAcc.setOnClickListener {
            val security = Intent(this, LoginAsSecurity::class.java)
            security.putExtra("userType", "security")
            saveUserType("security")
            startActivity(security)
            finish()
        }
    }

    private fun setOwner() {
        binding.cvOwnerAcc.setOnClickListener {
            val owner = Intent(this, LoginAsOwner::class.java)
            owner.putExtra("userType", "owner")
            saveUserType("owner")
            startActivity(owner)
            finish()
        }
    }


    private fun saveUserType(userType: String) {
        val token = ""
        val tokenExpiryTime = System.currentTimeMillis() + 3600_000L
        userData.setUserType(userType, token, tokenExpiryTime)
        Log.d("Database", "User type saved: $userType")
    }
}