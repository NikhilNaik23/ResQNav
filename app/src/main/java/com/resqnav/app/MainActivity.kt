package com.resqnav.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.resqnav.app.notifications.GeofenceManager
import com.resqnav.app.utils.PermissionManager
import com.resqnav.app.viewmodel.DisasterViewModel
import com.firebase.ui.auth.AuthUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var geofenceManager: GeofenceManager
    private lateinit var viewModel: DisasterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is logged in
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            // User not logged in, redirect to login
            startActivity(Intent(this, com.resqnav.app.auth.CustomLoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        try {
            // Initialize geofencing with null safety
            geofenceManager = GeofenceManager(this)
            Log.d("MainActivity", "✅ GeofenceManager initialized")
        } catch (e: Exception) {
            Log.e("MainActivity", "⚠️ Geofence init error - app will continue without geofencing", e)
            // Create a dummy geofence manager to prevent null pointer exceptions
            geofenceManager = GeofenceManager(this)
        }

        try {
            // Initialize ViewModel with proper error handling
            viewModel = ViewModelProvider(this)[DisasterViewModel::class.java]
            Log.d("MainActivity", "✅ ViewModel initialized")
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ CRITICAL: ViewModel init error", e)
            // Show error to user and close app gracefully
            Toast.makeText(this, "Error initializing app. Please restart.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Set up the toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Remove the default title to avoid duplication with custom layout
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_map -> loadFragment(MapFragment())
                R.id.nav_alerts -> loadFragment(AlertsFragment())
                R.id.nav_shelters -> loadFragment(SheltersFragment())
                R.id.nav_ai_assistant -> loadFragment(com.resqnav.app.ai.AIHubFragment())
                else -> false
            }
        }

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(MapFragment())
        }

        // Request necessary permissions when app first launches
        try {
            requestAppPermissions()
        } catch (e: Exception) {
            Log.e("MainActivity", "Permission request error", e)
        }

        // Initialize FCM and get token
        try {
            initializeFirebaseMessaging()
        } catch (e: Exception) {
            Log.e("MainActivity", "FCM init error", e)
        }

        // Setup geofences for active disasters
        try {
            setupDisasterGeofences()
        } catch (e: Exception) {
            Log.e("MainActivity", "Geofence setup error", e)
        }

        // Handle notification intent
        try {
            handleNotificationIntent(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Intent handling error", e)
        }
    }

    private fun requestAppPermissions() {
        // Check if any critical permissions are missing
        if (!PermissionManager.hasNotificationPermission(this) ||
            !PermissionManager.hasMicrophonePermission(this) ||
            !PermissionManager.hasCameraPermission(this) ||
            !PermissionManager.hasLocationPermission(this)) {

            // Show explanation dialog and request permissions
            PermissionManager.requestAllPermissions(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionManager.REQUEST_ALL_PERMISSIONS,
            PermissionManager.REQUEST_MICROPHONE,
            PermissionManager.REQUEST_CAMERA,
            PermissionManager.REQUEST_LOCATION,
            PermissionManager.REQUEST_NOTIFICATION -> {
                PermissionManager.onPermissionResult(
                    requestCode,
                    permissions,
                    grantResults,
                    onGranted = {
                        Toast.makeText(this, "✅ Permissions granted! All features are now available.", Toast.LENGTH_SHORT).show()
                        if (requestCode == PermissionManager.REQUEST_NOTIFICATION) {
                            Toast.makeText(this, "🔔 You'll now receive disaster alerts!", Toast.LENGTH_LONG).show()
                        }
                    },
                    onDenied = {
                        Toast.makeText(this, "⚠️ Some features may be limited without permissions.", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                showProfile()
                true
            }
            R.id.action_offline_settings -> {
                showOfflineSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

//    private fun logout() {
//        AuthUI.getInstance()
//            .signOut(this)
//            .addOnCompleteListener {
//                // Navigate back to login
//                val intent = Intent(this, com.resqnav.app.auth.CustomLoginActivity::class.java)
//                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                startActivity(intent)
//                finish()
//            }
//    }

    private fun showProfile() {
        // Navigate to Profile Activity
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
    }

    private fun showOfflineSettings() {
        // Navigate to Offline Settings Activity
        val intent = Intent(this, OfflineSettingsActivity::class.java)
        startActivity(intent)
    }

    private fun loadFragment(fragment: androidx.fragment.app.Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        return true
    }

    private fun initializeFirebaseMessaging() {
        // Subscribe to topics for broadcast notifications
        subscribeToDisasterTopics()

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("MainActivity", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d("MainActivity", "🔥🔥🔥 FCM TOKEN: $token")
            Log.d("FCM_TOKEN", token) // Extra log with simple tag for easy filtering
            Toast.makeText(this, "✅ Push notifications enabled", Toast.LENGTH_SHORT).show()

            // Save token to SharedPreferences
            getSharedPreferences("disaster_app", MODE_PRIVATE)
                .edit()
                .putString("fcm_token", token)
                .apply()
        }
    }

    private fun subscribeToDisasterTopics() {
        // Subscribe to all disaster types
        val topics = listOf(
            "all_disasters",      // For broadcasting to everyone
            "earthquakes",
            "floods",
            "tsunamis",
            "cyclones",
            "fires",
            "critical_alerts"
        )

        topics.forEach { topic ->
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnSuccessListener {
                    Log.d("MainActivity", "✅ Subscribed to topic: $topic")
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "❌ Failed to subscribe to topic: $topic", e)
                }
        }

        Log.d("MainActivity", "📢 Subscribed to ${topics.size} disaster notification topics")
    }

    private fun setupDisasterGeofences() {
        try {
            // Check if we have necessary permissions before setting up geofences
            val hasFineLocation = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasFineLocation) {
                Log.w("MainActivity", "⚠️ Fine location permission not granted, skipping geofence setup")
                return
            }

            // Only observe if permissions are granted
            viewModel.allActiveAlerts.observe(this) { disasters ->
                try {
                    if (disasters != null && disasters.isNotEmpty()) {
                        Log.d("MainActivity", "Setting up geofences for ${disasters.size} disasters")
                        geofenceManager.addGeofences(
                            disasters,
                            onSuccess = {
                                Log.d("MainActivity", "✅ Geofences activated for all disasters")
                            },
                            onFailure = { exception ->
                                Log.e("MainActivity", "❌ Failed to setup geofences: ${exception.message}", exception)
                                // Silently fail - geofences are optional feature, don't crash the app
                            }
                        )
                    } else {
                        Log.d("MainActivity", "No active disasters, skipping geofence setup")
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error in geofence observer", e)
                    // Don't crash - geofences are optional
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up disaster geofences", e)
            // Don't crash - geofences are optional feature
        }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        intent?.let {
            if (it.getBooleanExtra("open_alerts", false)) {
                // Navigate to alerts fragment
                findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.nav_alerts
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }
}
