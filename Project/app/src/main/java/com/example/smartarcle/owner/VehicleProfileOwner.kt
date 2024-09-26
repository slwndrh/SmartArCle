package com.example.smartarcle.owner

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import com.example.smartarcle.R
import com.example.smartarcle.data.LocaleLanguage
import com.example.smartarcle.databinding.ActivityVehicleProfileOwnerBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class VehicleProfileOwner : AppCompatActivity() {
    private lateinit var binding : ActivityVehicleProfileOwnerBinding
    private var db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleLanguage.setLocale(this, LocaleLanguage.getCurrentLanguage(this))
        binding = ActivityVehicleProfileOwnerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.vehicle_profile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        getData()
        setUpload()
    }

    private fun setUpload() {
        if (isNetworkAvailable()) {
            binding.btnVehicleSaveOwn.setOnClickListener {
                val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
                val edPlate = binding.edVehiclePlateOwn.text.toString().trim()
                val edName = binding.edVehicleNameOwn.text.toString().trim()
                val edAddress = binding.edVehicleAddressOwn.text.toString().trim()
                val edBrand = binding.edVehicleBrandOwn.text.toString().trim()
                val edColour = binding.edVehicleColourOwn.text.toString().trim()
                val updates = hashMapOf(
                    "plate" to edPlate,
                    "name" to edName,
                    "address" to edAddress,
                    "brand" to edBrand,
                    "colour" to edColour
                )

                val databaseRef = db.collection("vehicle_data").document(email)
                databaseRef.set(updates as Map<String, Any>).addOnSuccessListener {
                    showToast(getString(R.string.toast_success_data))
                }.addOnFailureListener {
                    showToast(getString(R.string.toast_failed_data))
                }
            }
        } else {
            showToast(getString(R.string.toast_not_internet))
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun getData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val email = user.email ?: ""
            val userRef = db.collection("vehicle_data").document(email)

            userRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val plate = document.getString("plate")
                    val name = document.getString("name")
                    val address = document.getString("address")
                    val brand = document.getString("brand")
                    val colour = document.getString("colour")

                    binding.edVehiclePlateOwn.setText(plate)
                    binding.edVehicleNameOwn.setText(name)
                    binding.edVehicleAddressOwn.setText(address)
                    binding.edVehicleBrandOwn.setText(brand)
                    binding.edVehicleColourOwn.setText(colour)
                } else {
                    Log.d(TAG, "Document not found")
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error getting document: ", e)
            }
        }
    }

    private fun showToast(text: String) {
        Toast.makeText(this@VehicleProfileOwner, text, Toast.LENGTH_SHORT).show()
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
        val intent = Intent(this, SettingOwner::class.java)
        startActivity(intent)
        finish()
    }
}