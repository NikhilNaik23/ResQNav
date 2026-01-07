package com.resqnav.app.ai

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.resqnav.app.R
import com.resqnav.app.model.DisasterAlert
import com.resqnav.app.model.Shelter
import com.resqnav.app.viewmodel.DisasterViewModel
import com.resqnav.app.viewmodel.ShelterViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

class SmartRouteFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var routeAnalysis: TextView
    private lateinit var safetyScore: TextView
    private lateinit var recommendation: TextView
    private lateinit var destinationInfo: TextView
    private val routeOptimizer = SmartRouteOptimizer()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var disasterViewModel: DisasterViewModel
    private lateinit var shelterViewModel: ShelterViewModel
    private var currentRoutePolyline: Polyline? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_smart_route, container, false)

        // Initialize osmdroid configuration
        Configuration.getInstance().load(
            requireContext(),
            android.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        Configuration.getInstance().userAgentValue = "ResQNav/1.0"

        mapView = view.findViewById(R.id.routeMapView)
        routeAnalysis = view.findViewById(R.id.routeAnalysis)
        safetyScore = view.findViewById(R.id.safetyScore)
        recommendation = view.findViewById(R.id.recommendation)
        destinationInfo = view.findViewById(R.id.destinationInfo)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        disasterViewModel = ViewModelProvider(this)[DisasterViewModel::class.java]
        shelterViewModel = ViewModelProvider(this)[ShelterViewModel::class.java]

        setupMap()

        view.findViewById<Button>(R.id.btnAnalyzeRoute).setOnClickListener {
            analyzeCurrentRoute()
        }

        return view
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        mapView.minZoomLevel = 4.0
        mapView.maxZoomLevel = 19.0

        // Set initial position and zoom (Delhi, India as default)
        mapView.controller.setZoom(12.0)
        val defaultLocation = GeoPoint(28.7041, 77.1025)
        mapView.controller.setCenter(defaultLocation)
    }

    @Suppress("MissingPermission")
    private fun analyzeCurrentRoute() {
        lifecycleScope.launch {
            // Get current location
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show()
                return@launch
            }

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val origin = GeoPoint(location.latitude, location.longitude)

                    // Get shelters and find nearest one
                    shelterViewModel.allOperationalShelters.observe(viewLifecycleOwner) { shelters ->
                        if (shelters.isNotEmpty()) {
                            val nearestShelter = findNearestShelter(origin, shelters)
                            val destination = GeoPoint(nearestShelter.latitude, nearestShelter.longitude)

                            destinationInfo.text = "📍 Routing to: ${nearestShelter.name}"

                            // Get active disasters
                            disasterViewModel.allActiveAlerts.observe(viewLifecycleOwner) { disasters ->
                                lifecycleScope.launch {
                                    calculateAndDrawRoute(origin, destination, disasters, nearestShelter.name)
                                }
                            }
                        } else {
                            Toast.makeText(requireContext(), "No shelters available", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Use sample data if location not available
                    val origin = GeoPoint(28.7041, 77.1025) // Delhi
                    val destination = GeoPoint(28.5355, 77.3910) // Noida (sample shelter)

                    destinationInfo.text = "📍 Routing to: Sample Emergency Shelter (Noida)"

                    disasterViewModel.allActiveAlerts.observe(viewLifecycleOwner) { disasters ->
                        lifecycleScope.launch {
                            calculateAndDrawRoute(origin, destination, disasters, "Sample Emergency Shelter")
                        }
                    }
                }
            }.addOnFailureListener {
                // Use sample data on failure
                val origin = GeoPoint(28.7041, 77.1025)
                val destination = GeoPoint(28.5355, 77.3910)

                destinationInfo.text = "📍 Routing to: Sample Emergency Shelter (Noida)"

                disasterViewModel.allActiveAlerts.observe(viewLifecycleOwner) { disasters ->
                    lifecycleScope.launch {
                        calculateAndDrawRoute(origin, destination, disasters, "Sample Emergency Shelter")
                    }
                }
            }
        }
    }

    private fun findNearestShelter(origin: GeoPoint, shelters: List<Shelter>): Shelter {
        return shelters.minByOrNull { shelter ->
            // Calculate simple Euclidean distance
            val latDiff = shelter.latitude - origin.latitude
            val lonDiff = shelter.longitude - origin.longitude
            sqrt(latDiff.pow(2) + lonDiff.pow(2))
        } ?: shelters.first()
    }

    private suspend fun calculateAndDrawRoute(
        origin: GeoPoint,
        destination: GeoPoint,
        disasters: List<DisasterAlert>,
        shelterName: String = "Destination"
    ) {
        val result = routeOptimizer.calculateSafestRoute(origin, destination, disasters)

        // Update UI
        safetyScore.text = "Safety Score: ${result.safetyScore.toInt()}/100"

        val scoreColor = when {
            result.safetyScore >= 80 -> Color.GREEN
            result.safetyScore >= 60 -> Color.parseColor("#FFA500")
            else -> Color.RED
        }
        safetyScore.setTextColor(scoreColor)

        recommendation.text = result.recommendation

        val hazardsText = if (result.hazardsNearRoute.isEmpty()) {
            "✅ No hazards detected on this route"
        } else {
            "⚠️ Hazards detected:\n" + result.hazardsNearRoute.joinToString("\n• ", "• ")
        }

        routeAnalysis.text = """
            📍 Distance: ${String.format("%.1f", result.estimatedDistance)} km
            🏥 Destination: $shelterName
            
            $hazardsText
            
            Route Points: ${result.recommendedRoute.size} waypoints
        """.trimIndent()

        // Draw the route on map
        drawRouteOnMap(result, origin, destination, disasters, shelterName)
    }

    private fun drawRouteOnMap(
        result: RouteRecommendation,
        origin: GeoPoint,
        destination: GeoPoint,
        disasters: List<DisasterAlert>,
        shelterName: String = "Destination"
    ) {
        // Clear previous route
        currentRoutePolyline?.let { mapView.overlays.remove(it) }

        // Remove old markers and circles
        mapView.overlays.removeAll { overlay ->
            overlay is Marker || overlay is Polygon
        }

        // Draw route polyline
        val polyline = Polyline(mapView)
        polyline.setPoints(result.recommendedRoute)

        // Color based on safety score
        polyline.outlinePaint.color = when {
            result.safetyScore >= 80 -> Color.GREEN
            result.safetyScore >= 60 -> Color.parseColor("#FFA500")
            else -> Color.RED
        }
        polyline.outlinePaint.strokeWidth = 10f

        mapView.overlays.add(polyline)
        currentRoutePolyline = polyline

        // Add origin marker
        val originMarker = Marker(mapView)
        originMarker.position = origin
        originMarker.title = "📍 Your Location"
        originMarker.snippet = "Starting point"
        originMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(originMarker)

        // Add destination marker
        val destMarker = Marker(mapView)
        destMarker.position = destination
        destMarker.title = "🏥 $shelterName"
        destMarker.snippet = "Safe shelter - ${String.format("%.1f", result.estimatedDistance)} km away"
        destMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(destMarker)

        // Add disaster circles
        disasters.forEach { disaster ->
            val disasterPoint = GeoPoint(disaster.latitude, disaster.longitude)

            // Add disaster marker
            val marker = Marker(mapView)
            marker.position = disasterPoint
            marker.title = "⚠️ ${disaster.type}"
            marker.snippet = "${disaster.severity} severity"
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(marker)

            // Add danger circle
            val circle = Polygon(mapView)
            circle.points = Polygon.pointsAsCircle(disasterPoint, disaster.radius * 1000.0)
            circle.fillPaint.color = Color.parseColor("#22FF0000") // Semi-transparent red
            circle.outlinePaint.color = Color.parseColor("#88FF0000")
            circle.outlinePaint.strokeWidth = 2f
            mapView.overlays.add(circle)
        }

        // Center map on route
        val boundingBox = org.osmdroid.util.BoundingBox.fromGeoPoints(result.recommendedRoute)
        mapView.zoomToBoundingBox(boundingBox, true, 100)

        mapView.invalidate()

        Toast.makeText(requireContext(), "✅ Route displayed on map!", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDetach()
    }
}
