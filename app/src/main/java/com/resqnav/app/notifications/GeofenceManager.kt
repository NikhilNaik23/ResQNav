package com.resqnav.app.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.resqnav.app.model.DisasterAlert
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    private val TAG = "GeofenceManager"

    companion object {
        const val GEOFENCE_RADIUS_CRITICAL = 10000f // 10km for critical disasters
        const val GEOFENCE_RADIUS_HIGH = 25000f // 25km for high severity
        const val GEOFENCE_RADIUS_MEDIUM = 50000f // 50km for medium severity
        const val GEOFENCE_EXPIRATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    /**
     * Add geofence for a disaster alert
     */
    fun addGeofence(disaster: DisasterAlert, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            onFailure(SecurityException("Location permission not granted"))
            return
        }

        val geofence = buildGeofence(disaster)
        val geofencingRequest = buildGeofencingRequest(geofence)
        val geofencePendingIntent = getGeofencePendingIntent()

        try {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "Geofence added successfully for ${disaster.title}")
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to add geofence: ${exception.message}", exception)
                    onFailure(exception)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
            onFailure(e)
        }
    }

    /**
     * Add multiple geofences at once
     */
    fun addGeofences(disasters: List<DisasterAlert>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        try {
            if (!hasLocationPermission()) {
                Log.w(TAG, "Location permission not granted - skipping geofence setup")
                // Don't throw exception, just log and return silently
                onFailure(SecurityException("Location permission not granted"))
                return
            }

            if (disasters.isEmpty()) {
                Log.d(TAG, "No disasters to add geofences for")
                onSuccess()
                return
            }

            val geofences = disasters.mapNotNull {
                try {
                    buildGeofence(it)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to build geofence for ${it.title}", e)
                    null
                }
            }

            if (geofences.isEmpty()) {
                Log.w(TAG, "No valid geofences to add")
                onSuccess()
                return
            }

            val geofencingRequest = buildGeofencingRequest(geofences)
            val geofencePendingIntent = getGeofencePendingIntent()

            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "${geofences.size} geofences added successfully")
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to add geofences: ${exception.message}", exception)
                    // Don't crash the app, just log the error
                    onFailure(exception)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
            onFailure(e)
        }
    }

    /**
     * Remove a specific geofence
     */
    fun removeGeofence(geofenceId: String) {
        geofencingClient.removeGeofences(listOf(geofenceId))
            .addOnSuccessListener {
                Log.d(TAG, "Geofence removed: $geofenceId")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to remove geofence: ${exception.message}")
            }
    }

    /**
     * Remove all geofences
     */
    fun removeAllGeofences() {
        geofencingClient.removeGeofences(getGeofencePendingIntent())
            .addOnSuccessListener {
                Log.d(TAG, "All geofences removed")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to remove all geofences: ${exception.message}")
            }
    }

    private fun buildGeofence(disaster: DisasterAlert): Geofence {
        val radius = when (disaster.severity.lowercase()) {
            "critical" -> GEOFENCE_RADIUS_CRITICAL
            "high" -> GEOFENCE_RADIUS_HIGH
            else -> GEOFENCE_RADIUS_MEDIUM
        }

        return Geofence.Builder()
            .setRequestId(disaster.externalId)
            .setCircularRegion(
                disaster.latitude,
                disaster.longitude,
                radius
            )
            .setExpirationDuration(GEOFENCE_EXPIRATION_MS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
            .setLoiteringDelay(300000) // 5 minutes dwell time
            .build()
    }

    private fun buildGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
    }

    private fun buildGeofencingRequest(geofences: List<Geofence>): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()
    }

    private fun getGeofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}

