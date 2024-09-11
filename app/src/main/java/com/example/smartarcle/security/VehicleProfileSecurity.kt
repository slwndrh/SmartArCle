package com.example.smartarcle.security

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartarcle.R
import com.example.smartarcle.data.LocaleLanguage
import com.example.smartarcle.data.VehicleAdapter
import com.example.smartarcle.data.VehicleData
import com.example.smartarcle.databinding.ActivityVehicleProfileSecurityBinding
import com.google.firebase.firestore.FirebaseFirestore

class VehicleProfileSecurity : AppCompatActivity() {
    private lateinit var binding : ActivityVehicleProfileSecurityBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var vehicleAdapter: VehicleAdapter
    private var vehicleList: MutableList<VehicleData> = mutableListOf()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleLanguage.setLocale(this, LocaleLanguage.getCurrentLanguage(this))
        binding = ActivityVehicleProfileSecurityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.vehicle_profile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setRecyclerview()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setRecyclerview() {
        recyclerView = findViewById(R.id.rv_vehicle)
        recyclerView.layoutManager = LinearLayoutManager(this)
        vehicleAdapter = VehicleAdapter(vehicleList)
        recyclerView.adapter = vehicleAdapter

        db.collection("vehicle_data")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val vehicle = document.toObject(VehicleData::class.java)
                    vehicleList.add(vehicle)
                }
                vehicleAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
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
        val intent = Intent(this, SettingSecurity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "VehicleProfileSecurity"
    }
}