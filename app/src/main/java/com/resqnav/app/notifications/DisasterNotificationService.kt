package com.resqnav.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.resqnav.app.MainActivity
import com.resqnav.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class DisasterNotificationService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "DisasterNotification"
        private const val CHANNEL_ID_CRITICAL = "disaster_critical"
        private const val CHANNEL_ID_HIGH = "disaster_high"
        private const val CHANNEL_ID_MEDIUM = "disaster_medium"
        private const val CHANNEL_ID_LOW = "disaster_low"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(
                title = it.title ?: "Disaster Alert",
                body = it.body ?: "",
                severity = remoteMessage.data["severity"] ?: "medium",
                disasterType = remoteMessage.data["type"] ?: "unknown"
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔥🔥🔥 REFRESHED FCM TOKEN: $token")
        Log.d("FCM_TOKEN", token) // Extra log with simple tag for easy filtering

        // Save token to SharedPreferences
        applicationContext.getSharedPreferences("disaster_app", Context.MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply()

        // Send token to your server or save it locally
        sendRegistrationToServer(token)
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val title = data["title"] ?: "Disaster Alert"
        val body = data["body"] ?: "A disaster has been detected near you"
        val severity = data["severity"] ?: "medium"
        val disasterType = data["type"] ?: "unknown"
        val distance = data["distance"] ?: "unknown"

        sendNotification(title, "$body\nDistance: $distance km", severity, disasterType)
    }

    private fun sendNotification(
        title: String,
        body: String,
        severity: String,
        disasterType: String
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("disaster_type", disasterType)
            putExtra("open_alerts", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = when (severity.lowercase()) {
            "critical" -> CHANNEL_ID_CRITICAL
            "high" -> CHANNEL_ID_HIGH
            "medium" -> CHANNEL_ID_MEDIUM
            else -> CHANNEL_ID_LOW
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(getNotificationIcon(disasterType))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(getPriority(severity))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(getNotificationColor(severity))

        // Add action buttons for critical alerts
        if (severity == "critical") {
            notificationBuilder
                .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
                .addAction(R.drawable.ic_shelter, "Find Shelter", pendingIntent)
                .addAction(R.drawable.ic_sos, "Send SOS", pendingIntent)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channels for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels(notificationManager)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun createNotificationChannels(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val criticalChannel = NotificationChannel(
                CHANNEL_ID_CRITICAL,
                "Critical Disasters",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical disaster alerts requiring immediate action"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            }

            val highChannel = NotificationChannel(
                CHANNEL_ID_HIGH,
                "High Priority Disasters",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority disaster alerts"
                enableVibration(true)
            }

            val mediumChannel = NotificationChannel(
                CHANNEL_ID_MEDIUM,
                "Medium Priority Disasters",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Medium priority disaster alerts"
            }

            val lowChannel = NotificationChannel(
                CHANNEL_ID_LOW,
                "Low Priority Disasters",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Low priority disaster information"
            }

            notificationManager.createNotificationChannels(
                listOf(criticalChannel, highChannel, mediumChannel, lowChannel)
            )
        }
    }

    private fun getNotificationIcon(disasterType: String): Int {
        return when (disasterType.lowercase()) {
            "earthquake" -> R.drawable.ic_earthquake
            "flood" -> R.drawable.ic_flood
            "fire" -> R.drawable.ic_fire
            "cyclone", "storm" -> R.drawable.ic_cyclone
            "tsunami" -> R.drawable.ic_tsunami
            else -> R.drawable.ic_disaster
        }
    }

    private fun getNotificationColor(severity: String): Int {
        return when (severity.lowercase()) {
            "critical" -> android.graphics.Color.RED
            "high" -> android.graphics.Color.rgb(255, 140, 0) // Orange
            "medium" -> android.graphics.Color.rgb(255, 215, 0) // Yellow
            else -> android.graphics.Color.GRAY
        }
    }

    private fun getPriority(severity: String): Int {
        return when (severity.lowercase()) {
            "critical", "high" -> NotificationCompat.PRIORITY_HIGH
            "medium" -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_LOW
        }
    }

    private fun sendRegistrationToServer(token: String) {
        // TODO: Send token to your backend server
        // For now, save it to SharedPreferences
        val prefs = getSharedPreferences("disaster_app", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
        Log.d(TAG, "FCM Token saved locally")
    }
}

