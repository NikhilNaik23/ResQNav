package com.resqnav.app.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "disaster_alerts",
    indices = [Index(value = ["externalId"], unique = true)]
)
data class DisasterAlert(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val externalId: String = "", // Unique ID from external API (e.g., USGS earthquake ID)
    val type: String, // earthquake, flood, fire, cyclone, tsunami
    val severity: String, // low, medium, high, critical
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Double, // affected area radius in km
    val timestamp: Long,
    val isActive: Boolean = true
)

enum class DisasterType {
    EARTHQUAKE,
    FLOOD,
    FIRE,
    CYCLONE,
    TSUNAMI,
    LANDSLIDE,
    OTHER
}

enum class SeverityLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

