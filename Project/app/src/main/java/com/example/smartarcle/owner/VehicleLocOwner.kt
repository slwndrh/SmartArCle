package com.example.smartarcle.owner

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.smartarcle.R
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.media.AudioAttributes
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.smartarcle.data.GeofenceService
import com.example.smartarcle.data.LocaleLanguage
import com.example.smartarcle.databinding.ActivityVehicleLocOwnerBinding
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import java.util.*

@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("UnspecifiedImmutableFlag")
//yg bisa muncul icon
class VehicleLocOwner : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private var mapReady = false
    private lateinit var binding: ActivityVehicleLocOwnerBinding
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private var geofenceIds = mutableSetOf<String>()

    private var isGeofenceActive = false

    private val ahLat = -7.947576939123812
    private val ahLng = 112.61505438055553
    private val aiLat = -7.945040586669759
    private val aiLng = 112.61514982433243
    private val aiRad = 35.0
    private val ahRad = 35.0
    private val vehicleRadius = 15.0

    private var vehicleMarker: Marker? = null

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeoBroadcastReceiver::class.java)
        intent.action = GeoBroadcastReceiver.ACTION_GEOFENCE_EVENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                showToast(getString(R.string.toast_permission_granted))
            } else {
                showToast(getString(R.string.toast_permission_reject))
            }
        }

    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            if (runningQOrLater) {
                requestBackgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                vehicleLocation()
            }
        }
    }

    private val requestBackgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            vehicleLocation()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleLanguage.setLocale(this, LocaleLanguage.getCurrentLanguage(this))
        binding = ActivityVehicleLocOwnerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.vehicle_loc)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_owner) as SupportMapFragment
        mapFragment.getMapAsync(this)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Motor")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)

        val email = firebaseAuth.currentUser?.email?.replace(".", "_") ?: return
        checkAndRequestPermissions()
        setGeofencingState(email)
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )

        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            Log.d("Geofencing", "All required permissions are granted")
            vehicleLocation()
        } else {
            permissions.forEach { permission ->
                requestLocationPermissionLauncher.launch(permission)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mapReady = true
        mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
        mMap.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isMapToolbarEnabled = true

            val parkingAI = LatLng(aiLat, aiLng)
            addMarker(MarkerOptions().position(parkingAI).title("Parking AI"))
            moveCamera(CameraUpdateFactory.newLatLngZoom(parkingAI, 18f))
            addCircle(
                CircleOptions()
                    .center(parkingAI)
                    .radius(aiRad)
                    .fillColor(0x22FF0000)
                    .strokeColor(Color.RED)
                    .strokeWidth(3f)
            )

            val parkingAH = LatLng(ahLat, ahLng)
            addMarker(MarkerOptions().position(parkingAH).title("Parking AH"))
            moveCamera(CameraUpdateFactory.newLatLngZoom(parkingAH, 18f))
            addCircle(
                CircleOptions()
                    .center(parkingAH)
                    .radius(ahRad)
                    .fillColor(0x22FF0000)
                    .strokeColor(Color.RED)
                    .strokeWidth(3f)
            )
            if (checkForegroundAndBackgroundLocationPermission()) {
                vehicleLocation()
            } else {
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun checkForegroundAndBackgroundLocationPermission(): Boolean {
        val foregroundLocationApproved = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    @SuppressLint("MissingPermission")
    private fun vehicleLocation() {
        if (mapReady) {
            val email = firebaseAuth.currentUser?.email?.replace(".", "_") ?: return
            val vehicleRef = database.child(email)

            vehicleRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val latitude = dataSnapshot.child("Latitude").getValue(String::class.java)?.toDouble() ?: 0.0
                    val longitude = dataSnapshot.child("Longitude").getValue(String::class.java)?.toDouble() ?: 0.0

                    Log.d("VehicleLocOwner", "Latitude: $latitude, Longitude: $longitude")

                    val latLng = LatLng(latitude, longitude)
                    vehicleMarker?.remove()

                    val markerOptions = MarkerOptions()
                        .position(latLng)
                        .title("Vehicle Location")
                        .icon(resizeBitmap(R.drawable.marker, 100, 100))
                    vehicleMarker = mMap.addMarker(markerOptions)

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 21f))

                    setGeofencingState(email)
                    checkGeofenceStatus()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("VehicleLocOwner", "Failed to read vehicle location", databaseError.toException())
                }
            })
        } else {
            Log.e("VehicleLocOwner", "Map is not ready yet")
        }
    }

    @SuppressLint("MissingPermission")
    private fun setGeofencingState(email: String) {
        val buttonRef = database.child(email).child("Alarm")

        buttonRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(buttonSnapshot: DataSnapshot) {
                val buttonState = buttonSnapshot.getValue(String::class.java) ?: "0"
                Log.d("VehicleLocOwner", "Button State: $buttonState")

                if (buttonState == "1") {
                    if (!isGeofenceActive) {
                        addGeofence("AI Geofence", aiLat, aiLng)
                        addGeofence("AH Geofence", ahLat, ahLng)
                        isGeofenceActive = true
                    }
                } else {
                    if (isGeofenceActive) {
                        removeGeofence()
                        isGeofenceActive = false
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("VehicleLocOwner", "Failed to read button state", databaseError.toException())
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(geofenceId: String, latitude: Double, longitude: Double) {
        val geofence = Geofence.Builder()
            .setRequestId(geofenceId)
            .setCircularRegion(
                latitude,
                longitude,
                vehicleRadius.toFloat()
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                geofenceIds.add(geofenceId)
                showToast(getString(R.string.toast_geofence_add))
                Log.i("Geofencing", "Geofencing added successfully for vehicle: $geofenceId")
            }
            addOnFailureListener { exception ->
                handleGeofencingFailure(exception)
            }
        }
    }

    private fun removeGeofence() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            geofencingClient.removeGeofences(geofencePendingIntent).run {
                addOnSuccessListener {
                    geofenceIds.clear()
                    showToast(getString(R.string.toast_geofence_remove))
                    Log.i("Geofencing", "Geofencing removed successfully")
                }
                addOnFailureListener { exception ->
                    handleGeofencingFailure(exception)
                }
            }
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }


    private fun handleGeofencingFailure(exception: Exception) {
        if (exception is ApiException) {
            when (exception.statusCode) {
                GeofenceStatusCodes.GEOFENCE_INSUFFICIENT_LOCATION_PERMISSION -> {
                    showToast(getString(R.string.toast_geofence_not_add_permission))
                    Log.e("Geofencing", "Geofencing not added: Insufficient location permission")
                }
                else -> {
                    showToast(getString(R.string.toast_geofence_not_add))
                    Log.e("Geofencing", "Geofencing not added: ${exception.statusCode}")
                }
            }
        } else {
            showToast(getString(R.string.toast_geofence_not_add))
            Log.e("Geofencing", "Geofencing not added: $exception")
        }
    }

    private fun isLocationInsideGeofence(location: LatLng, centerLat: Double, centerLng: Double, radius: Double): Boolean {
        val locationLat = location.latitude
        val locationLng = location.longitude
        val distance = FloatArray(1)
        Location.distanceBetween(locationLat, locationLng, centerLat, centerLng, distance)
        return distance[0] <= radius
    }

    private fun checkGeofenceStatus() {
        Log.d("GeofenceStatus", "Checking geofence status")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    val isOutsideAH = !isLocationInsideGeofence(currentLatLng, ahLat, ahLng, ahRad)
                    val isOutsideAI = !isLocationInsideGeofence(currentLatLng, aiLat, aiLng, aiRad)

                    Log.d("GeofenceStatus", "Current Location: $currentLatLng")
                    Log.d("GeofenceStatus", "Is Outside AH: $isOutsideAH, Is Outside AI: $isOutsideAI")
                    Log.d("GeofenceStatus", "Is Geofence Active: $isGeofenceActive")

                    if ((isOutsideAH || isOutsideAI) && isGeofenceActive) {
                        Log.d("GeofenceStatus", "Triggering notification")
                        fetchUserDataAndSendNotification()
                    }
                } ?: Log.d("GeofenceStatus", "No location data available")
            }
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun fetchUserDataAndSendNotification() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
        val db = FirebaseFirestore.getInstance()

        val profile = db.collection("owner").document(email).collection("profile").document("profile_data")
        val vehicle = db.collection("vehicle_data").document(email)

        profile.get().addOnSuccessListener { profileSnapshot ->
            val ownerName = profileSnapshot.getString("name") ?: ""
            vehicle.get().addOnSuccessListener { vehicleSnapshot ->
                val plateNumber = vehicleSnapshot.getString("plate") ?: ""
                val notificationMessage = "The $ownerName's vehicle with License Plate Number $plateNumber has exited the geofencing area. Immediately report to security!"
                sendNotification(notificationMessage)
            }.addOnFailureListener { e ->
                Log.e("FirestoreError", "Error getting vehicle data", e)
                sendNotification("Error getting vehicle data")
            }
        }.addOnFailureListener { e ->
            Log.e("FirestoreError", "Error getting profile data", e)
            sendNotification("Error getting profile data")
        }
    }

private fun sendNotification(message: String) {
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val bigTextStyle = NotificationCompat.BigTextStyle().bigText(message)
    val notificationBuilder = NotificationCompat.Builder(this, GeofenceService.CHANNEL_ID)
        .setSmallIcon(R.drawable.vehicle)
        .setContentTitle("Geofence Alert")
        .setStyle(bigTextStyle)
        .setVibrate(longArrayOf(0, 500, 500, 500))
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            GeofenceService.CHANNEL_ID,
            GeofenceService.CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 500, 500)
            setSound(null, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        }
        notificationBuilder.setChannelId(GeofenceService.CHANNEL_ID)
        notificationManager.createNotificationChannel(channel)
    }

    val notification = notificationBuilder.build()
    notificationManager.notify(GeofenceService.NOTIFICATION_ID, notification)
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

    private fun showToast(text: String) {
        Toast.makeText(this@VehicleLocOwner, text, Toast.LENGTH_SHORT).show()
    }

    private fun resizeBitmap(drawableRes: Int, width: Int, height: Int): BitmapDescriptor {
        val imageBitmap = BitmapFactory.decodeResource(resources, drawableRes)
        val resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false)
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap)
    }
}