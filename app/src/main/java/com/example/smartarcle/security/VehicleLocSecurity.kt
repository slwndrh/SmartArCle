package com.example.smartarcle.security

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.smartarcle.R
import com.example.smartarcle.data.LocaleLanguage
import com.example.smartarcle.databinding.ActivityVehicleLocSecurityBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnspecifiedImmutableFlag")
class VehicleLocSecurity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityVehicleLocSecurityBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var mMap: GoogleMap
    private var mapReady = false
    private var ownerEmail: String = ""
    private var allOwnerEmail: String? = null
    private val emails = listOf(
        "krisnarchmdni@gmail_com",
        "slwndrh2518@gmail_com"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleLanguage.setLocale(this, LocaleLanguage.getCurrentLanguage(this))
        binding = ActivityVehicleLocSecurityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.vehicle_loc)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val secType = intent.getStringExtra("securityType")
        Log.d("securityType", "User type: $secType")

        ownerEmail = intent.getStringExtra("ownerEmail") ?: ""

        val latitude = intent.getStringExtra("latitude")?.toDoubleOrNull()
        val longitude = intent.getStringExtra("longitude")?.toDoubleOrNull()
        if (latitude == null || longitude == null) {
            if (ownerEmail.isNotEmpty()) {
                fetchVehicleLocation(ownerEmail)
            } else {
                Log.e("VehicleLocSecurity", "Owner email is missing")
            }
        } else {
            val userEmail = ownerEmail
            val latLng = LatLng(latitude, longitude)
            updateMapLocation(latLng, userEmail)
        }

        auth = FirebaseAuth.getInstance()
        allOwnerEmail = intent.getStringExtra("ownerEmail")

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_security) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        sendOwnerEmailToHomeSecurity()
        return true
    }

    private fun sendOwnerEmailToHomeSecurity() {
        val intent = Intent(this, HomeSecurity::class.java)
        intent.putExtra("ownerEmail", ownerEmail)
        startActivity(intent)
        finish()
    }

    override fun onMapReady(map: GoogleMap) {
        mMap = map
        mapReady = true
        mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN

        if (allOwnerEmail != null) {
            fetchVehicleLocation(allOwnerEmail!!)
        } else {
            fetchAllVehicleLocations()
        }
    }

    private fun fetchAllVehicleLocations() {
        val database = FirebaseDatabase.getInstance("https://smartarcle-default-rtdb.firebaseio.com").reference
        emails.forEach { email ->
            database.child("Motor").child(email).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
//                    val latitude = snapshot.child("Latitude").getValue(Double::class.java)
//                    val longitude = snapshot.child("Longitude").getValue(Double::class.java)

                    val latitude = snapshot.child("Latitude").getValue(String::class.java) ?: "0.0"
                    val longitude = snapshot.child("Longitude").getValue(String::class.java) ?: "0.0"

                    Log.d("VehicleLocOwner", "Latitude: $latitude, Longitude: $longitude")

                    val lat = latitude.toDoubleOrNull()
                    val long = longitude.toDoubleOrNull()

                    if (lat != null && long != null) {
                        val location = LatLng(lat, long)
                        fetchUserName(email) { name ->
                            addMarker(location, name ?: email)
                        }
                    } else {
                        Log.e("VehicleLocSecurity", "Failed to get location for $email: Latitude or Longitude is null or invalid")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("VehicleLocSecurity", "Failed to get location for $email: ${error.message}")
                }
            })
        }
    }

    private fun fetchUserName(email: String, callback: (String?) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val formattedEmail = email.replace("_", ".")
        val docRef = firestore.collection("owner").document(formattedEmail).collection("profile").document("profile_data")

        docRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val userName = document.getString("name")
                callback(userName)
            } else {
                callback(null)
            }
        }.addOnFailureListener { exception ->
            Log.e("VehicleLocSecurity", "Error fetching user name: ${exception.message}")
            callback(null)
        }
    }

    private fun addMarker(location: LatLng, name: String) {
        if (::mMap.isInitialized) {
            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title(name)
                    .icon(resizeBitmap(R.drawable.vehicle, 120, 120))
            )
            marker?.showInfoWindow()
            if (mMap.cameraPosition.zoom < 10f) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
            }
        } else {
            Log.e("VehicleLocSecurity", "Google Map not initialized")
        }
    }

    private fun encodeEmail(email: String): String {
        return email.replace(".", "_")
    }

    private fun fetchVehicleLocation(ownerEmail: String) {
        val encodedEmail = encodeEmail(ownerEmail)
        val database = FirebaseDatabase.getInstance("https://smartarcle-default-rtdb.firebaseio.com").reference
        database.child("Motor").child(encodedEmail).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val latitude = snapshot.child("Latitude").getValue(String::class.java)?.toDouble() ?: 0.0
                val longitude = snapshot.child("Longitude").getValue(String::class.java)?.toDouble() ?: 0.0

                val location = LatLng(latitude, longitude)
                mMap.clear()
                fetchUserName(ownerEmail) { name ->
                    updateMapLocation(location, name ?: ownerEmail)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("VehicleLocSecurity", "Failed to get location for $ownerEmail: ${error.message}")
            }
        })
    }

    private fun updateMapLocation(location: LatLng, name: String) {
        if (::mMap.isInitialized) {
            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title(name)
                    .icon(resizeBitmap(R.drawable.vehicle, 120, 120))
            )
            marker?.showInfoWindow()
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 21f))
        } else {
            Log.e("VehicleLocSecurity", "Google Map not initialized")
        }
    }

    private fun resizeBitmap(drawableRes: Int, width: Int, height: Int): BitmapDescriptor {
        val imageBitmap = BitmapFactory.decodeResource(resources, drawableRes)
        val resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false)
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap)
    }

    override fun onResume() {
        super.onResume()
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_security) as SupportMapFragment
        mapFragment.onResume()
    }

    override fun onPause() {
        super.onPause()
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_security) as SupportMapFragment
        mapFragment.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_security) as? SupportMapFragment
        mapFragment?.let {
            supportFragmentManager.beginTransaction().remove(it).commit()
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