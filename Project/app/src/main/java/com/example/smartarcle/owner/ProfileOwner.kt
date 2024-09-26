package com.example.smartarcle.owner

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.example.smartarcle.R
import com.example.smartarcle.databinding.ActivityProfileOwnerBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import de.hdodenhof.circleimageview.CircleImageView

class ProfileOwner : AppCompatActivity() {
    private lateinit var binding :  ActivityProfileOwnerBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var image: CircleImageView
    private lateinit var btnBrowse: FloatingActionButton
    private lateinit var uri: Uri
    private var db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileOwnerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.user_profile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        auth = FirebaseAuth.getInstance()

        getData()
        setPhoto()
    }

    private fun loadImageFromStorage() {
        val user = auth.currentUser?.email ?: ""
//        val user = FirebaseAuth.getInstance().currentUser?.email?.replace(".", "_") ?: return
        val databaseRef = db.collection("owner").document(user).collection("profile").document("profile_data")

        databaseRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val imageUrl = document.getString("url")

                imageUrl?.let {
                    Glide.with(this)
                        .load(imageUrl)
                        .into(image)
                }
            } else {
                Log.d(TAG, "No document found for user $user")
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error getting document for user $user", e)
        }
    }

    private fun setUpload(){
        if (isNetworkAvailable()) {
            if (::uri.isInitialized) {
                val user = auth.currentUser?.email ?: ""
//                val user = FirebaseAuth.getInstance().currentUser?.email?.replace(".", "_") ?: ""
                val storageRef = Firebase.storage.reference
                val imageRef = storageRef.child("images/$user/${System.currentTimeMillis()}.jpg")
                imageRef.putFile(uri).addOnSuccessListener { task ->
                    task.metadata!!.reference!!.downloadUrl.addOnSuccessListener {imageUrl ->
                        val databaseRef = db.collection("owner").document(user).collection("profile").document("profile_data")
                        val edName = binding.edProfileNameOwn.text.toString().trim()
                        val edEmail = binding.edProfileEmailOwn.text.toString().trim()
                        val edPhone = binding.edProfilePhoneOwn.text.toString().trim()
                        val edAddress = binding.edProfileAddressOwn.text.toString().trim()
                        val updates = hashMapOf(
                            "name" to edName,
                            "email" to edEmail,
                            "phone" to edPhone,
                            "address" to edAddress,
                            "url" to imageUrl.toString() // Simpan URL gambar ke Firestore
                        )
                        databaseRef.set(updates as Map<String, Any>).addOnSuccessListener {
                            showToast(getString(R.string.toast_success_image))
                            loadImageFromStorage()
                        }.addOnFailureListener {
                            showToast(getString(R.string.toast_failed_image))
                        }
                    }
                }.addOnFailureListener {
                    showToast(getString(R.string.toast_failed_image))
                }
            } else {
                showToast(getString(R.string.toast_select))
                Log.e(TAG, "Please select an image first!")
            }
        } else {
            showToast(getString(R.string.toast_not_internet))
        }
    }

    private fun setPhoto(){
        image = findViewById(R.id.iv_profile_own)
        btnBrowse = findViewById(R.id.iv_profile_browse_own)

        val gallery = registerForActivityResult(ActivityResultContracts.GetContent()) {
            image.setImageURI(it)
            if (it != null) {
                uri = it
            }
        }

        btnBrowse.setOnClickListener {
            gallery.launch("image/*")
        }

        binding.btnSaveOwn.setOnClickListener {
            setUpload()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun getData() {
        val currentUser = auth.currentUser?.email ?: ""
//        val currentUser = FirebaseAuth.getInstance().currentUser?.email?.replace(".", "_") ?: ""

        if (currentUser.isNotEmpty()) {
            val userRef = db.collection("owner").document(currentUser).collection("profile").document("profile_data")
            userRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("name")
                    val email = document.getString("email")
                    val phone = document.getString("phone")
                    val address = document.getString("address")
                    val photoUrl = document.getString("url")

                    binding.edProfileNameOwn.setText(name)
                    binding.edProfileEmailOwn.setText(email)
                    binding.edProfilePhoneOwn.setText(phone)
                    binding.edProfileAddressOwn.setText(address)

                    photoUrl?.let {
                        Glide.with(this)
                            .load(photoUrl)
                            .into(binding.ivProfileOwn)
                    }
                } else {
                    Log.d(TAG, "Document not found")
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error getting document: ", e)
            }
        } else {
            Log.e(TAG, "User email is empty")
            showToast(getString(R.string.toast_login_again))
        }
    }

    private fun showToast(text: String) {
        Toast.makeText(this@ProfileOwner, text, Toast.LENGTH_SHORT).show()
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