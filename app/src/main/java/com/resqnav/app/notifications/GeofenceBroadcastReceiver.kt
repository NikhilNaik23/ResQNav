package com.resqnav.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val TAG = "GeofenceReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        // Get the transition type
        val geofenceTransition = geofencingEvent.geofenceTransition

        // Test that the reported transition was of interest
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {

            // Get the geofences that were triggered
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            triggeringGeofences?.forEach { geofence ->
                Log.d(TAG, "Geofence triggered: ${geofence.requestId}")
                handleGeofenceTransition(context, geofence, geofenceTransition)
            }
        } else {
            Log.e(TAG, "Invalid transition type: $geofenceTransition")
        }
    }

    private fun handleGeofenceTransition(context: Context, geofence: Geofence, transitionType: Int) {
        val transitionString = when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTERED"
            Geofence.GEOFENCE_TRANSITION_DWELL -> "DWELLING IN"
            else -> "UNKNOWN"
        }

        // Send a high-priority notification
        val notificationHelper = NotificationHelper(context)
        notificationHelper.sendGeofenceNotification(
            geofenceId = geofence.requestId,
            transitionType = transitionString
        )

        Log.i(TAG, "User $transitionString disaster zone: ${geofence.requestId}")
    }
}

