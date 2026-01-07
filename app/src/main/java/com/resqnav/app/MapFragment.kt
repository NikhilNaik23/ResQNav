package com.resqnav.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.resqnav.app.model.DisasterAlert
import com.resqnav.app.model.Shelter
import com.resqnav.app.viewmodel.DisasterViewModel
import com.resqnav.app.viewmodel.ShelterViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var disasterViewModel: DisasterViewModel
    private lateinit var shelterViewModel: ShelterViewModel
    private lateinit var fabSos: FloatingActionButton
    private lateinit var fabMyLocation: FloatingActionButton
    private lateinit var fabRefresh: FloatingActionButton
    private lateinit var loadingProgress: android.widget.ProgressBar
    private var currentLocation: Location? = null
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private val disasterMarkers = mutableListOf<Marker>()
    private val shelterMarkers = mutableListOf<Marker>()
    private val disasterCircles = mutableListOf<Polygon>()

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                enableMyLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                enableMyLocation()
            }
            else -> {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        // Initialize views
        mapView = view.findViewById(R.id.map)
        fabSos = view.findViewById(R.id.fab_sos)
        fabMyLocation = view.findViewById(R.id.fab_my_location)
        fabRefresh = view.findViewById(R.id.fab_refresh)
        loadingProgress = view.findViewById(R.id.loading_progress)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupMap()
        setupViewModels()
        setupButtons()
        checkLocationPermission()

        // Check if shelter location is passed from SheltersFragment
        checkForShelterLocation()

        return view
    }

    private fun setupMap() {
        // Initialize osmdroid configuration
        val ctx = requireContext()
        Configuration.getInstance().load(
            ctx,
            android.preference.PreferenceManager.getDefaultSharedPreferences(ctx)
        )
        Configuration.getInstance().userAgentValue = "ResQNav/1.0"

        // Aggressive limits to prevent image decoding overload and crashes
        Configuration.getInstance().cacheMapTileCount = 9 // Reduce from default 16
        Configuration.getInstance().cacheMapTileOvershoot = 2 // Reduce tile overshoot
        Configuration.getInstance().tileDownloadThreads = 1 // Reduce from default 4
        Configuration.getInstance().tileFileSystemThreads = 1 // Reduce from default 4
        Configuration.getInstance().tileDownloadMaxQueueSize = 20 // Limit queue size
        Configuration.getInstance().setExpirationExtendedDuration(1000L * 60 * 60 * 24 * 7) // Cache for 7 days

        // Check internet connection
        val connectivityManager = ctx.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected = networkInfo?.isConnected == true

        if (!isConnected) {
            Toast.makeText(ctx, "⚠️ No internet connection. Map tiles won't load.", Toast.LENGTH_LONG).show()
        }

        // Important: Set tile source BEFORE any other configuration
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        // Use software rendering to prevent HWUI image decoding overload
        mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        // Set zoom limits to prevent multiple world maps
        mapView.minZoomLevel = 4.0  // Prevent zooming out too much
        mapView.maxZoomLevel = 19.0  // Allow detailed zoom

        // Disable scroll to prevent showing multiple worlds
        mapView.setScrollableAreaLimitDouble(
            org.osmdroid.util.BoundingBox(
                85.0, 180.0, -85.0, -180.0  // Limit to single world view
            )
        )

        // Set zoom controls
        mapView.setBuiltInZoomControls(false)
        mapView.setMultiTouchControls(true)

        // Set initial position and zoom
        mapView.controller.setZoom(12.0)

        // Set default location (Delhi, India)
        val defaultLocation = GeoPoint(28.7041, 77.1025)
        mapView.controller.setCenter(defaultLocation)

        // Add my location overlay
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), mapView)
        myLocationOverlay.enableMyLocation()
        mapView.overlays.add(myLocationOverlay)


        // Show loading message
        if (isConnected) {
            Toast.makeText(ctx, "📍 Loading map tiles...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupViewModels() {
        disasterViewModel = ViewModelProvider(this)[DisasterViewModel::class.java]
        shelterViewModel = ViewModelProvider(this)[ShelterViewModel::class.java]

        // Observe disaster alerts and add markers
        disasterViewModel.allActiveAlerts.observe(viewLifecycleOwner) { alerts ->
            addDisasterMarkers(alerts)
        }

        // Observe shelters and add markers
        shelterViewModel.allOperationalShelters.observe(viewLifecycleOwner) { shelters ->
            addShelterMarkers(shelters)
        }

        // Observe loading state
        disasterViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            loadingProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
            fabRefresh.isEnabled = !isLoading
        }

        // Observe error messages
        disasterViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        // Auto-fetch all disaster types on first load
        Toast.makeText(requireContext(), "Loading disasters from NASA EONET & USGS...", Toast.LENGTH_SHORT).show()
        disasterViewModel.refreshAllDisasters()

        // Cleanup old alerts
        disasterViewModel.cleanupOldAlerts()
    }

    private fun setupButtons() {
        fabSos.setOnClickListener {
            triggerSOS()
        }

        fabMyLocation.setOnClickListener {
            moveToCurrentLocation()
        }

        fabRefresh.setOnClickListener {
            Toast.makeText(requireContext(), "Refreshing all disaster types...", Toast.LENGTH_SHORT).show()
            disasterViewModel.refreshAllDisasters()
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @Suppress("MissingPermission")
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            myLocationOverlay.enableMyLocation()
            myLocationOverlay.enableFollowLocation()
            moveToCurrentLocation()
        }
    }

    @Suppress("MissingPermission")
    private fun moveToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    mapView.controller.animateTo(geoPoint)
                    mapView.controller.setZoom(15.0)
                } else {
                    Toast.makeText(requireContext(), "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addDisasterMarkers(alerts: List<DisasterAlert>) {
        // Clear existing disaster markers and circles
        disasterMarkers.forEach { mapView.overlays.remove(it) }
        disasterCircles.forEach { mapView.overlays.remove(it) }
        disasterMarkers.clear()
        disasterCircles.clear()

        alerts.forEach { alert ->
            val position = GeoPoint(alert.latitude, alert.longitude)

            // Add marker
            val marker = Marker(mapView)
            marker.position = position
            marker.title = alert.title
            marker.snippet = "${alert.type.uppercase()} - ${alert.severity.uppercase()}"
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            // Get colors based on disaster type
            val colors = getDisasterColors(alert.type)

            // Set marker icon - use default icons with different colors based on type
            marker.icon = when (alert.type.lowercase()) {
                "earthquake" -> ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_dialog_alert) // Red
                "flood" -> ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_dialog_info) // Blue
                "fire" -> ContextCompat.getDrawable(requireContext(), android.R.drawable.star_big_on) // Orange/Yellow
                "cyclone" -> ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_rotate) // Purple
                "volcano" -> ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_compass) // Brown
                else -> ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_info_details)
            }

            mapView.overlays.add(marker)
            disasterMarkers.add(marker)

            // Add circle to show affected area with disaster-specific colors
            val circle = Polygon(mapView)
            circle.points = Polygon.pointsAsCircle(position, alert.radius * 1000.0) // Convert km to meters
            circle.fillColor = colors.fillColor
            circle.strokeColor = colors.strokeColor
            circle.strokeWidth = 3f
            mapView.overlays.add(circle)
            disasterCircles.add(circle)
        }

        // Map will auto-refresh when overlays change - no need to manually invalidate
    }

    /**
     * Get disaster-specific colors for markers and circles
     */
    private fun getDisasterColors(disasterType: String): DisasterColors {
        return when (disasterType.lowercase()) {
            "earthquake" -> DisasterColors(
                markerColor = 0xFFDC143C.toInt(), // Crimson Red
                fillColor = 0x33DC143C, // Semi-transparent red
                strokeColor = 0x88DC143C.toInt() // More opaque red
            )
            "flood" -> DisasterColors(
                markerColor = 0xFF1E90FF.toInt(), // Dodger Blue
                fillColor = 0x331E90FF, // Semi-transparent blue
                strokeColor = 0x881E90FF.toInt() // More opaque blue
            )
            "fire" -> DisasterColors(
                markerColor = 0xFFFF8C00.toInt(), // Dark Orange
                fillColor = 0x33FF8C00, // Semi-transparent orange
                strokeColor = 0x88FF8C00.toInt() // More opaque orange
            )
            "cyclone" -> DisasterColors(
                markerColor = 0xFF9370DB.toInt(), // Medium Purple
                fillColor = 0x339370DB, // Semi-transparent purple
                strokeColor = 0x889370DB.toInt() // More opaque purple
            )
            "volcano" -> DisasterColors(
                markerColor = 0xFF8B4513.toInt(), // Saddle Brown
                fillColor = 0x338B4513, // Semi-transparent brown
                strokeColor = 0x888B4513.toInt() // More opaque brown
            )
            "tsunami" -> DisasterColors(
                markerColor = 0xFF00CED1.toInt(), // Dark Turquoise
                fillColor = 0x3300CED1, // Semi-transparent turquoise
                strokeColor = 0x8800CED1.toInt() // More opaque turquoise
            )
            else -> DisasterColors(
                markerColor = 0xFF808080.toInt(), // Gray
                fillColor = 0x33808080, // Semi-transparent gray
                strokeColor = 0x88808080.toInt() // More opaque gray
            )
        }
    }

    /**
     * Data class to hold disaster-specific colors
     */
    private data class DisasterColors(
        val markerColor: Int,
        val fillColor: Int,
        val strokeColor: Int
    )

    private fun addShelterMarkers(shelters: List<Shelter>) {
        // Clear existing shelter markers
        shelterMarkers.forEach { mapView.overlays.remove(it) }
        shelterMarkers.clear()

        shelters.forEach { shelter ->
            val position = GeoPoint(shelter.latitude, shelter.longitude)

            val marker = Marker(mapView)
            marker.position = position
            marker.title = shelter.name
            marker.snippet = "Capacity: ${shelter.currentOccupancy}/${shelter.capacity}"
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_compass)

            mapView.overlays.add(marker)
            shelterMarkers.add(marker)
        }

        // Map will auto-refresh when overlays change - no need to manually invalidate
    }

    @Suppress("MissingPermission")
    private fun triggerSOS() {
        if (currentLocation == null) {
            Toast.makeText(requireContext(), "Getting your location...", Toast.LENGTH_SHORT).show()

            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        currentLocation = location
                        sendSOSMessage(location)
                    } else {
                        Toast.makeText(requireContext(), "Unable to get location for SOS", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            sendSOSMessage(currentLocation!!)
        }
    }

    private fun sendSOSMessage(location: Location) {
        val message = "🚨 EMERGENCY SOS! 🚨\n\n" +
                "I need immediate help at my current location:\n\n" +
                "📍 Latitude: ${location.latitude}\n" +
                "📍 Longitude: ${location.longitude}\n\n" +
                "🗺️ View on map:\n" +
                "https://www.openstreetmap.org/?mlat=${location.latitude}&mlon=${location.longitude}&zoom=15\n\n" +
                "Google Maps:\n" +
                "https://maps.google.com/?q=${location.latitude},${location.longitude}"

        // Send SMS (requires SMS permission)
        val smsIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:")
            putExtra("sms_body", message)
        }

        try {
            startActivity(smsIntent)
            Toast.makeText(requireContext(), "SOS message prepared. Add emergency contacts and send.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to send SOS: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkForShelterLocation() {
        arguments?.let { bundle ->
            val shelterLat = bundle.getDouble("shelter_lat", 0.0)
            val shelterLon = bundle.getDouble("shelter_lon", 0.0)
            val shelterName = bundle.getString("shelter_name", "")

            if (shelterLat != 0.0 && shelterLon != 0.0) {
                // Show shelter on map
                val shelterLocation = GeoPoint(shelterLat, shelterLon)

                // Move map to shelter location
                mapView.controller.animateTo(shelterLocation)
                mapView.controller.setZoom(15.0)

                // Add shelter marker
                val marker = Marker(mapView)
                marker.position = shelterLocation
                marker.title = shelterName
                marker.snippet = "Tap for turn-by-turn navigation"
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_dialog_map)

                // Make marker clickable for navigation
                marker.setOnMarkerClickListener { clickedMarker, _ ->
                    // Show navigation options
                    showNavigationOptions(shelterLat, shelterLon, shelterName)
                    true
                }

                mapView.overlays.add(marker)
                mapView.invalidate()

                Toast.makeText(
                    requireContext(),
                    "📍 Showing: $shelterName\nTap marker for navigation",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showNavigationOptions(lat: Double, lon: Double, name: String) {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("🗺️ Navigate to $name")
        builder.setMessage("Choose navigation app:")

        // Google Maps option
        builder.setPositiveButton("Google Maps") { _, _ ->
            openGoogleMapsNavigation(lat, lon)
        }

        // Waze option
        builder.setNeutralButton("Waze") { _, _ ->
            openWazeNavigation(lat, lon)
        }

        // OpenStreetMap browser option
        builder.setNegativeButton("Browser") { _, _ ->
            openBrowserNavigation(lat, lon)
        }

        builder.show()
    }

    private fun openGoogleMapsNavigation(lat: Double, lon: Double) {
        try {
            // Try to open Google Maps app with navigation
            val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lon&mode=d")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
            Toast.makeText(requireContext(), "🗺️ Opening Google Maps navigation...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Fallback to browser if Google Maps not installed
            val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lon&travelmode=driving")
            val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
            startActivity(browserIntent)
        }
    }

    private fun openWazeNavigation(lat: Double, lon: Double) {
        try {
            val wazeUri = Uri.parse("waze://?ll=$lat,$lon&navigate=yes")
            val wazeIntent = Intent(Intent.ACTION_VIEW, wazeUri)
            wazeIntent.setPackage("com.waze")
            startActivity(wazeIntent)
            Toast.makeText(requireContext(), "🗺️ Opening Waze navigation...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "❌ Waze not installed. Opening browser...", Toast.LENGTH_SHORT).show()
            openBrowserNavigation(lat, lon)
        }
    }

    private fun openBrowserNavigation(lat: Double, lon: Double) {
        val osmUri = Uri.parse("https://www.openstreetmap.org/directions?from=&to=$lat,$lon&engine=fossgis_osrm_car")
        val browserIntent = Intent(Intent.ACTION_VIEW, osmUri)
        startActivity(browserIntent)
        Toast.makeText(requireContext(), "🗺️ Opening navigation in browser...", Toast.LENGTH_SHORT).show()
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
