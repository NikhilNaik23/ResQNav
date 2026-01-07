package com.resqnav.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.resqnav.app.MainActivity
import com.resqnav.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class DisasterNotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token: $token")
        // Send token to your server if needed
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "Message received from: ${message.from}")

        // Check if message contains data payload
        message.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: ${message.data}")
            handleDataPayload(message.data)
        }

        // Check if message contains notification payload
        message.notification?.let {
            Log.d(TAG, "Message Notification: ${it.title} - ${it.body}")
            showNotification(it.title ?: "Disaster Alert", it.body ?: "New alert received")
        }
    }

    private fun handleDataPayload(data: Map<String, String>) {
        val type = data["type"] ?: "alert"
        val title = data["title"] ?: "Emergency Alert"
        val body = data["body"] ?: "Please check the app for details"
        val severity = data["severity"] ?: "medium"

        showNotification(title, body, severity)
    }

    private fun showNotification(title: String, body: String, severity: String = "medium") {
        val channelId = when (severity.lowercase()) {
            "critical" -> CHANNEL_ID_CRITICAL
            "high" -> CHANNEL_ID_HIGH
            else -> CHANNEL_ID_DEFAULT
        }

        createNotificationChannels()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val priority = when (severity.lowercase()) {
            "critical" -> NotificationCompat.PRIORITY_MAX
            "high" -> NotificationCompat.PRIORITY_HIGH
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Critical alerts channel
            val criticalChannel = NotificationChannel(
                CHANNEL_ID_CRITICAL,
                "Critical Disaster Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical emergency disaster alerts"
                enableVibration(true)
                enableLights(true)
            }

            // High priority alerts channel
            val highChannel = NotificationChannel(
                CHANNEL_ID_HIGH,
                "High Priority Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority disaster alerts"
            }

            // Default alerts channel
            val defaultChannel = NotificationChannel(
                CHANNEL_ID_DEFAULT,
                "Disaster Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General disaster alerts and updates"
            }

            notificationManager.createNotificationChannel(criticalChannel)
            notificationManager.createNotificationChannel(highChannel)
            notificationManager.createNotificationChannel(defaultChannel)
        }
    }

    companion object {
        private const val TAG = "DisasterFCMService"
        private const val CHANNEL_ID_CRITICAL = "disaster_alerts_critical"
        private const val CHANNEL_ID_HIGH = "disaster_alerts_high"
        private const val CHANNEL_ID_DEFAULT = "disaster_alerts_default"
    }
}

