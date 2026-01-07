package com.resqnav.app.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Centralized Permission Manager for handling runtime permissions
 */
object PermissionManager {

    // Permission request codes
    const val REQUEST_MICROPHONE = 1001
    const val REQUEST_CAMERA = 1002
    const val REQUEST_LOCATION = 1003
    const val REQUEST_NOTIFICATION = 1005
    const val REQUEST_ALL_PERMISSIONS = 1004

    /**
     * Check if microphone permission is granted
     */
    fun hasMicrophonePermission(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if camera permission is granted
     */
    fun hasCameraPermission(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if notification permission is granted (Android 13+)
     */
    fun hasNotificationPermission(activity: Activity): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Notifications are allowed by default on Android 12 and below
            true
        }
    }

    /**
     * Request microphone permission with explanation dialog
     */
    fun requestMicrophonePermission(activity: Activity, onGranted: () -> Unit = {}, onDenied: () -> Unit = {}) {
        if (hasMicrophonePermission(activity)) {
            onGranted()
            return
        }

        // Show rationale dialog if needed
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)) {
            MaterialAlertDialogBuilder(activity)
                .setTitle("🎤 Microphone Permission Required")
                .setMessage("Voice-activated SOS requires microphone access to:\n\n" +
                        "• Listen for emergency voice commands\n" +
                        "• Enable hands-free emergency triggers\n" +
                        "• Recognize 'SOS' and other safety keywords\n\n" +
                        "This permission is essential for voice-based emergency features.")
                .setPositiveButton("Allow") { _, _ ->
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        REQUEST_MICROPHONE
                    )
                }
                .setNegativeButton("Deny") { dialog, _ ->
                    dialog.dismiss()
                    onDenied()
                }
                .setCancelable(false)
                .show()
        } else {
            // Request permission directly
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_MICROPHONE
            )
        }
    }

    /**
     * Request camera permission with explanation dialog
     */
    fun requestCameraPermission(activity: Activity, onGranted: () -> Unit = {}, onDenied: () -> Unit = {}) {
        if (hasCameraPermission(activity)) {
            onGranted()
            return
        }

        // Show rationale dialog if needed
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
            MaterialAlertDialogBuilder(activity)
                .setTitle("📸 Camera Permission Required")
                .setMessage("Damage Assessment requires camera access to:\n\n" +
                        "• Take photos of disaster damage\n" +
                        "• Analyze structural damage using AI\n" +
                        "• Document emergency situations\n" +
                        "• Help assess safety risks\n\n" +
                        "This permission is essential for AI-powered damage analysis.")
                .setPositiveButton("Allow") { _, _ ->
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.CAMERA),
                        REQUEST_CAMERA
                    )
                }
                .setNegativeButton("Deny") { dialog, _ ->
                    dialog.dismiss()
                    onDenied()
                }
                .setCancelable(false)
                .show()
        } else {
            // Request permission directly
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA
            )
        }
    }

    /**
     * Request location permission with explanation dialog
     */
    fun requestLocationPermission(activity: Activity, onGranted: () -> Unit = {}, onDenied: () -> Unit = {}) {
        if (hasLocationPermission(activity)) {
            onGranted()
            return
        }

        // Show rationale dialog if needed
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            MaterialAlertDialogBuilder(activity)
                .setTitle("📍 Location Permission Required")
                .setMessage("This app requires location access to:\n\n" +
                        "• Show your position on the map\n" +
                        "• Find nearest emergency shelters\n" +
                        "• Send your location in SOS alerts\n" +
                        "• Navigate to safe evacuation routes\n\n" +
                        "This permission is essential for emergency navigation.")
                .setPositiveButton("Allow") { _, _ ->
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                        REQUEST_LOCATION
                    )
                }
                .setNegativeButton("Deny") { dialog, _ ->
                    dialog.dismiss()
                    onDenied()
                }
                .setCancelable(false)
                .show()
        } else {
            // Request permission directly
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION
            )
        }
    }

    /**
     * Request notification permission (Android 13+)
     */
    fun requestNotificationPermission(activity: Activity, onGranted: () -> Unit = {}, onDenied: () -> Unit = {}) {
        if (hasNotificationPermission(activity)) {
            onGranted()
            return
        }

        // Only needed for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Show rationale dialog if needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)) {
                MaterialAlertDialogBuilder(activity)
                    .setTitle("🔔 Notification Permission Required")
                    .setMessage("This app needs notification access to:\n\n" +
                            "• Alert you about nearby disasters\n" +
                            "• Send critical emergency warnings\n" +
                            "• Notify you when entering danger zones\n" +
                            "• Provide real-time safety updates\n\n" +
                            "This permission is CRITICAL for your safety!")
                    .setPositiveButton("Allow") { _, _ ->
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            REQUEST_NOTIFICATION
                        )
                    }
                    .setNegativeButton("Deny") { dialog, _ ->
                        dialog.dismiss()
                        onDenied()
                    }
                    .setCancelable(false)
                    .show()
            } else {
                // Request permission directly
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION
                )
            }
        } else {
            // Android 12 and below don't need runtime permission
            onGranted()
        }
    }

    /**
     * Request all necessary permissions at once
     */
    fun requestAllPermissions(activity: Activity) {
        val permissionsNeeded = mutableListOf<String>()

        if (!hasMicrophonePermission(activity)) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
        }
        if (!hasCameraPermission(activity)) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }
        if (!hasLocationPermission(activity)) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (!hasNotificationPermission(activity) && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsNeeded.isNotEmpty()) {
            MaterialAlertDialogBuilder(activity)
                .setTitle("⚠️ Permissions Required")
                .setMessage("ResQNav needs the following permissions to function properly:\n\n" +
                        "🔔 Notifications - Critical disaster alerts\n" +
                        "🎤 Microphone - Voice-activated emergency commands\n" +
                        "📸 Camera - AI damage assessment\n" +
                        "📍 Location - Emergency navigation and SOS alerts\n\n" +
                        "These permissions are essential for your safety!")
                .setPositiveButton("Grant Permissions") { _, _ ->
                    ActivityCompat.requestPermissions(
                        activity,
                        permissionsNeeded.toTypedArray(),
                        REQUEST_ALL_PERMISSIONS
                    )
                }
                .setNegativeButton("Later") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    /**
     * Handle permission request result
     */
    fun onPermissionResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        onGranted: () -> Unit = {},
        onDenied: () -> Unit = {}
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onGranted()
        } else {
            onDenied()
        }
    }
}

