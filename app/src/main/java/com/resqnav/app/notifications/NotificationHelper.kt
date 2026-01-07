package com.resqnav.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.resqnav.app.MainActivity
import com.resqnav.app.R

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID_GEOFENCE = "geofence_alerts"
        private const val GEOFENCE_NOTIFICATION_ID = 1001
    }

    fun sendGeofenceNotification(geofenceId: String, transitionType: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_alerts", true)
            putExtra("geofence_id", geofenceId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_GEOFENCE)
            .setSmallIcon(R.drawable.ic_disaster)
            .setContentTitle("⚠️ You've $transitionType a Disaster Zone!")
            .setContentText("A disaster has been detected near your location. Stay alert and check for safety information.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("A disaster has been detected near your location. Tap to view details and find nearby shelters. Stay safe!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_shelter, "Find Shelter", pendingIntent)
            .addAction(R.drawable.ic_map, "View Map", pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_GEOFENCE,
                "Geofence Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when you enter a disaster zone"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(GEOFENCE_NOTIFICATION_ID, notificationBuilder.build())
    }
}

