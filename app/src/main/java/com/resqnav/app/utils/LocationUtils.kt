package com.resqnav.app.utils


import android.location.Location
import kotlin.math.*

object LocationUtils {

    /**
     * Calculate distance between two coordinates in kilometers
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadius = 6371.0 // Earth radius in kilometers

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * Format distance to human-readable string
     */
    fun formatDistance(distanceKm: Double): String {
        return when {
            distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()} m"
            distanceKm < 10.0 -> String.format("%.1f km", distanceKm)
            else -> String.format("%.0f km", distanceKm)
        }
    }

    /**
     * Check if a location is within a certain radius of another location
     */
    fun isWithinRadius(
        centerLat: Double,
        centerLon: Double,
        targetLat: Double,
        targetLon: Double,
        radiusKm: Double
    ): Boolean {
        val distance = calculateDistance(centerLat, centerLon, targetLat, targetLon)
        return distance <= radiusKm
    }

    /**
     * Get nearest shelter from a list based on current location
     */
    fun findNearestShelter(
        currentLat: Double,
        currentLon: Double,
        shelters: List<com.resqnav.app.model.Shelter>
    ): com.resqnav.app.model.Shelter? {
        return shelters.minByOrNull { shelter ->
            calculateDistance(currentLat, currentLon, shelter.latitude, shelter.longitude)
        }
    }

    /**
     * Sort shelters by distance from current location
     */
    fun sortSheltersByDistance(
        currentLat: Double,
        currentLon: Double,
        shelters: List<com.resqnav.app.model.Shelter>
    ): List<Pair<com.resqnav.app.model.Shelter, Double>> {
        return shelters.map { shelter ->
            val distance = calculateDistance(currentLat, currentLon, shelter.latitude, shelter.longitude)
            Pair(shelter, distance)
        }.sortedBy { it.second }
    }
}

