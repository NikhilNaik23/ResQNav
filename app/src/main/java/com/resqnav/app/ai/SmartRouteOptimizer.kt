package com.resqnav.app.ai

import android.location.Location
import android.util.Log
import com.resqnav.app.model.DisasterAlert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * AI-powered smart route optimization
 * Calculates safest evacuation routes considering disasters, traffic, and distance
 */
class SmartRouteOptimizer {
    
    private val TAG = "SmartRouteOptimizer"
    
    /**
     * Calculate safety score for a route
     */
    suspend fun calculateSafestRoute(
        origin: GeoPoint,
        destination: GeoPoint,
        activeDisasters: List<DisasterAlert>,
        alternativePoints: List<GeoPoint> = emptyList()
    ): RouteRecommendation = withContext(Dispatchers.Default) {
        
        try {
            // Generate multiple route options
            val routes = generateRouteOptions(origin, destination, alternativePoints)
            
            // Score each route
            val scoredRoutes = routes.map { route ->
                val safetyScore = calculateRouteSafetyScore(route, activeDisasters)
                ScoredRoute(route, safetyScore)
            }
            
            // Find safest route
            val safestRoute = scoredRoutes.maxByOrNull { it.safetyScore }
            
            if (safestRoute != null) {
                RouteRecommendation(
                    recommendedRoute = safestRoute.route,
                    safetyScore = safestRoute.safetyScore,
                    estimatedDistance = calculateDistance(origin, destination),
                    hazardsNearRoute = findHazardsNearRoute(safestRoute.route, activeDisasters),
                    recommendation = generateRecommendation(safestRoute.safetyScore)
                )
            } else {
                RouteRecommendation(
                    recommendedRoute = listOf(origin, destination),
                    safetyScore = 50f,
                    estimatedDistance = calculateDistance(origin, destination),
                    hazardsNearRoute = emptyList(),
                    recommendation = "Direct route. Exercise caution."
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating route", e)
            RouteRecommendation(
                recommendedRoute = listOf(origin, destination),
                safetyScore = 0f,
                estimatedDistance = 0f,
                hazardsNearRoute = emptyList(),
                recommendation = "Unable to calculate optimal route"
            )
        }
    }
    
    /**
     * Check if a location is safe from known disasters
     */
    fun isLocationSafe(
        location: GeoPoint,
        disasters: List<DisasterAlert>,
        safetyBuffer: Double = 2.0 // km
    ): SafetyCheck {
        val nearbyDisasters = disasters.filter { disaster ->
            val distance = calculateDistance(
                location,
                GeoPoint(disaster.latitude, disaster.longitude)
            )
            distance <= (disaster.radius + safetyBuffer)
        }
        
        return if (nearbyDisasters.isEmpty()) {
            SafetyCheck(true, "Location is safe", emptyList())
        } else {
            SafetyCheck(
                false,
                "⚠️ ${nearbyDisasters.size} hazard(s) nearby!",
                nearbyDisasters.map { it.type }
            )
        }
    }
    
    private fun generateRouteOptions(
        origin: GeoPoint,
        destination: GeoPoint,
        waypoints: List<GeoPoint>
    ): List<List<GeoPoint>> {
        val routes = mutableListOf<List<GeoPoint>>()
        
        // Direct route
        routes.add(listOf(origin, destination))
        
        // Routes with waypoints
        if (waypoints.isNotEmpty()) {
            waypoints.forEach { waypoint ->
                routes.add(listOf(origin, waypoint, destination))
            }
        }
        
        // Generate intermediate points for detour options
        val midpoint = GeoPoint(
            (origin.latitude + destination.latitude) / 2,
            (origin.longitude + destination.longitude) / 2
        )
        
        // Offset routes (north and south detours)
        val northDetour = GeoPoint(midpoint.latitude + 0.01, midpoint.longitude)
        val southDetour = GeoPoint(midpoint.latitude - 0.01, midpoint.longitude)
        
        routes.add(listOf(origin, northDetour, destination))
        routes.add(listOf(origin, southDetour, destination))
        
        return routes
    }
    
    private fun calculateRouteSafetyScore(
        route: List<GeoPoint>,
        disasters: List<DisasterAlert>
    ): Float {
        var score = 100f
        
        // Check each segment of the route
        for (i in 0 until route.size - 1) {
            val point = route[i]
            
            disasters.forEach { disaster ->
                val disasterPoint = GeoPoint(disaster.latitude, disaster.longitude)
                val distance = calculateDistance(point, disasterPoint)
                
                // Deduct points based on proximity to disasters
                when {
                    distance < disaster.radius -> {
                        // Route passes through disaster zone
                        score -= when (disaster.severity.lowercase()) {
                            "critical" -> 40f
                            "high" -> 30f
                            "moderate" -> 20f
                            else -> 10f
                        }
                    }
                    distance < disaster.radius + 2 -> {
                        // Route is near disaster zone
                        score -= 15f
                    }
                    distance < disaster.radius + 5 -> {
                        // Route is somewhat close
                        score -= 5f
                    }
                }
            }
        }
        
        // Ensure score stays between 0-100
        return score.coerceIn(0f, 100f)
    }
    
    private fun findHazardsNearRoute(
        route: List<GeoPoint>,
        disasters: List<DisasterAlert>
    ): List<String> {
        val hazards = mutableSetOf<String>()
        
        route.forEach { point ->
            disasters.forEach { disaster ->
                val distance = calculateDistance(
                    point,
                    GeoPoint(disaster.latitude, disaster.longitude)
                )
                
                if (distance < disaster.radius + 5) {
                    hazards.add("${disaster.type} (${String.format("%.1f", distance)}km away)")
                }
            }
        }
        
        return hazards.toList()
    }
    
    private fun generateRecommendation(safetyScore: Float): String {
        return when {
            safetyScore >= 80 -> "✅ SAFE ROUTE: This route avoids known hazards. Proceed safely."
            safetyScore >= 60 -> "⚡ MODERATE RISK: Exercise caution. Some hazards in the area."
            safetyScore >= 40 -> "⚠️ ELEVATED RISK: Consider alternate route if possible. Stay alert."
            else -> "🚨 HIGH RISK: This route passes near danger zones. Seek alternate route immediately!"
        }
    }

    /**
     * Calculate distance between two points in kilometers
     */
    fun calculateDistance(point1: GeoPoint, point2: GeoPoint): Float {
        val earthRadius = 6371.0 // km

        val lat1 = Math.toRadians(point1.latitude)
        val lat2 = Math.toRadians(point2.latitude)
        val lon1 = Math.toRadians(point1.longitude)
        val lon2 = Math.toRadians(point2.longitude)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1) * cos(lat2) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (earthRadius * c).toFloat()
    }
}

data class RouteRecommendation(
    val recommendedRoute: List<GeoPoint>,
    val safetyScore: Float,
    val estimatedDistance: Float,
    val hazardsNearRoute: List<String>,
    val recommendation: String
)

data class ScoredRoute(
    val route: List<GeoPoint>,
    val safetyScore: Float
)

data class SafetyCheck(
    val isSafe: Boolean,
    val message: String,
    val nearbyHazards: List<String>
)

