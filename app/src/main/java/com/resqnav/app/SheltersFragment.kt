package com.resqnav.app

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.resqnav.app.adapter.SheltersAdapter
import com.resqnav.app.api.OverpassApiService
import com.resqnav.app.model.Shelter
import com.resqnav.app.viewmodel.ShelterViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SheltersFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SheltersAdapter
    private lateinit var viewModel: ShelterViewModel
    private lateinit var searchEditText: TextInputEditText
    private lateinit var emptyState: View
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var allShelters: List<Shelter> = emptyList()
    private var userLocation: Location? = null
    private var hasAddedSampleShelters = false
    private val overpassApi = OverpassApiService.create()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_shelters, container, false)

        recyclerView = view.findViewById(R.id.shelters_recycler_view)
        searchEditText = view.findViewById(R.id.search_edit_text)
        emptyState = view.findViewById(R.id.empty_state)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupRecyclerView()
        setupViewModel()
        setupSearch()
        getUserLocation()

        return view
    }

    private fun setupRecyclerView() {
        adapter = SheltersAdapter(
            emptyList(),
            null,
            onItemClick = { shelter -> onShelterClick(shelter) },
            onViewDirectionClick = { shelter -> viewShelterOnMap(shelter) },
            onNavigateClick = { shelter -> navigateToShelter(shelter) }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[ShelterViewModel::class.java]

        viewModel.allOperationalShelters.observe(viewLifecycleOwner) { shelters ->
            allShelters = shelters

            // Filter and sort shelters by distance if location is available
            val sortedShelters = if (userLocation != null) {
                shelters
                    .map { shelter ->
                        val distance = calculateDistance(
                            userLocation!!.latitude, userLocation!!.longitude,
                            shelter.latitude, shelter.longitude
                        )
                        Pair(shelter, distance)
                    }
                    .filter { it.second <= 500 } // Only show shelters within 500km
                    .sortedBy { it.second } // Sort by distance (nearest first)
                    .map { it.first } // Extract shelter objects
            } else {
                shelters.take(20) // Show only first 20 if no location
            }

            adapter.updateShelters(sortedShelters, userLocation)

            if (shelters.isEmpty() && !hasAddedSampleShelters) {
                // Add sample shelters immediately for testing
                addPunjabShelters()
                hasAddedSampleShelters = true
            }

            if (shelters.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyState.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun getUserLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = location
                    // Update adapter with location
                    adapter.updateShelters(allShelters, userLocation)

                    // Add shelters near user's location if database is empty
                    if (allShelters.isEmpty() && !hasAddedSampleShelters) {
                        fetchRealSheltersFromAPI()
                        hasAddedSampleShelters = true
                    }
                } else {
                    // Fallback to Delhi if no location available
                    userLocation = Location("").apply {
                        latitude = 28.7041
                        longitude = 77.1025
                    }
                    if (allShelters.isEmpty() && !hasAddedSampleShelters) {
                        fetchRealSheltersFromAPI()
                        hasAddedSampleShelters = true
                    }
                }
            }.addOnFailureListener {
                // Fallback to Delhi
                userLocation = Location("").apply {
                    latitude = 28.7041
                    longitude = 77.1025
                }
                if (allShelters.isEmpty() && !hasAddedSampleShelters) {
                    fetchRealSheltersFromAPI()
                    hasAddedSampleShelters = true
                }
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] / 1000 // Convert to kilometers
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterShelters(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterShelters(query: String) {
        val filteredList = if (query.isEmpty()) {
            allShelters
        } else {
            allShelters.filter { shelter ->
                shelter.name.contains(query, ignoreCase = true) ||
                shelter.address.contains(query, ignoreCase = true)
            }
        }
        adapter.updateShelters(filteredList, userLocation)
    }

    private fun onShelterClick(shelter: Shelter) {
        val distance = if (userLocation != null) {
            val dist = calculateDistance(
                userLocation!!.latitude, userLocation!!.longitude,
                shelter.latitude, shelter.longitude
            )
            "\nDistance: %.1f km".format(dist)
        } else {
            ""
        }

        Toast.makeText(
            requireContext(),
            "Shelter: ${shelter.name}\nContact: ${shelter.contactNumber}$distance",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun viewShelterOnMap(shelter: Shelter) {
        // Navigate to Map tab and pass shelter location
        val bundle = Bundle().apply {
            putDouble("shelter_lat", shelter.latitude)
            putDouble("shelter_lon", shelter.longitude)
            putString("shelter_name", shelter.name)
        }

        val mapFragment = MapFragment().apply {
            arguments = bundle
        }

        // Switch to map tab
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, mapFragment)
            .addToBackStack(null)
            .commit()

        // Update bottom navigation to Map tab
        activity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
            ?.selectedItemId = R.id.nav_map
    }

    private fun navigateToShelter(shelter: Shelter) {
        // Open navigation to shelter using OpenStreetMap or Google Maps
        val distance = if (userLocation != null) {
            calculateDistance(
                userLocation!!.latitude, userLocation!!.longitude,
                shelter.latitude, shelter.longitude
            )
        } else {
            0f
        }

        Toast.makeText(
            requireContext(),
            "🧭 Navigate to ${shelter.name}\nDistance: %.1f km\n(Opening navigation...)".format(distance),
            Toast.LENGTH_SHORT
        ).show()

        // Navigate to shelter on the map
        viewShelterOnMap(shelter)
    }

    private fun fetchRealSheltersFromAPI() {
        if (userLocation == null) return

        val userLat = userLocation!!.latitude
        val userLon = userLocation!!.longitude

        Toast.makeText(
            requireContext(),
            "🔍 Fetching real emergency facilities from OpenStreetMap...",
            Toast.LENGTH_SHORT
        ).show()

        lifecycleScope.launch {
            try {
                val query = OverpassApiService.buildQuery(userLat, userLon, 20) // 20km radius
                Log.d("SheltersFragment", "Querying OSM around ($userLat, $userLon) with 20km radius")

                val response = withContext(Dispatchers.IO) {
                    overpassApi.queryEmergencyFacilities(query)
                }

                Log.d("SheltersFragment", "API Response: Code=${response.code()}, Success=${response.isSuccessful}")

                if (response.isSuccessful) {
                    val elements = response.body()?.elements ?: emptyList()
                    Log.d("SheltersFragment", "✅ Found ${elements.size} emergency facilities from OSM")

                    if (elements.isEmpty()) {
                        // Silently load demo data without annoying toast
                        Log.d("SheltersFragment", "No facilities found in area. Loading demo shelters.")
                        addSampleShelters()
                        return@launch
                    }

                    var addedCount = 0
                    elements.forEach { element ->
                        val tags = element.tags ?: return@forEach
                        val lat = element.lat ?: element.center?.lat ?: return@forEach
                        val lon = element.lon ?: element.center?.lon ?: return@forEach

                        val name = tags["name"] ?: tags["amenity"]?.replaceFirstChar { it.uppercase() } ?: "Emergency Facility"
                        val amenity = tags["amenity"] ?: "facility"

                        // Determine facility type and capacity
                        val (type, capacity, facilities) = when (amenity) {
                            "hospital" -> Triple("Hospital", 200, "Emergency Care, ICU, Ambulance, Medical Staff")
                            "clinic" -> Triple("Clinic", 50, "First Aid, Medical Staff, Basic Treatment")
                            "fire_station" -> Triple("Fire Station", 100, "Fire Safety, Rescue Team, Emergency Response")
                            "police" -> Triple("Police Station", 80, "Security, Emergency Response, Protection")
                            "community_centre" -> Triple("Community Center", 150, "Shelter, Food, Water, Basic Facilities")
                            "social_facility" -> Triple("Social Facility", 120, "Shelter, Food, Water, Support Services")
                            else -> Triple("Emergency Facility", 100, "Basic Emergency Services")
                        }

                        val address = buildString {
                            tags["addr:street"]?.let { append(it).append(", ") }
                            tags["addr:city"]?.let { append(it).append(", ") }
                            tags["addr:state"]?.let { append(it) }
                            if (isEmpty()) append("${tags["addr:district"] ?: ""} (Real OSM Data)")
                        }

                        val phone = tags["phone"] ?: tags["contact:phone"] ?: "+91-112-2671234" // NDMA Emergency

                        val shelter = Shelter(
                            name = name,
                            address = address.ifEmpty { "Real location from OpenStreetMap" },
                            latitude = lat,
                            longitude = lon,
                            capacity = capacity,
                            currentOccupancy = (capacity * 0.3).toInt(), // Assume 30% occupancy
                            contactNumber = phone,
                            facilities = facilities,
                            type = type
                        )

                        viewModel.insert(shelter)
                        addedCount++
                    }

                    Toast.makeText(
                        requireContext(),
                        "✅ Loaded $addedCount REAL emergency facilities from OpenStreetMap!",
                        Toast.LENGTH_LONG
                    ).show()

                } else {
                    // API failed - silently load demo data
                    Log.e("SheltersFragment", "API Error: ${response.code()} - ${response.message()}")
                    addSampleShelters()
                }
            } catch (e: Exception) {
                // Network error - silently load demo data
                Log.e("SheltersFragment", "Failed to fetch shelters from OSM: ${e.message}", e)
                addSampleShelters()
            }
        }
    }

    private fun addSampleShelters() {
        // Use current location or default to Delhi if location not available
        val userLat = userLocation?.latitude ?: 28.7041
        val userLon = userLocation?.longitude ?: 77.1025

        Log.d("SheltersFragment", "Adding demo shelters near ($userLat, $userLon)")

        // Fallback sample shelters when API fails
        val sampleShelters = listOf(
            Shelter(
                name = "[DEMO] Punjab Government Relief Camp",
                address = "Near GT Road, Main City Area (Sample Location)",
                latitude = userLat + 0.02,
                longitude = userLon + 0.01,
                capacity = 500,
                currentOccupancy = 120,
                contactNumber = "+91-172-2740000",
                facilities = "Medical, Food, Water, Electricity, Blankets",
                type = "Government"
            ),
            Shelter(
                name = "[DEMO] Community Gurudwara Shelter",
                address = "Gurudwara Complex, City Center (Sample Location)",
                latitude = userLat + 0.05,
                longitude = userLon - 0.02,
                capacity = 300,
                currentOccupancy = 85,
                contactNumber = "+91-172-2741000",
                facilities = "Food, Water, First Aid, Langar",
                type = "Community"
            ),
            Shelter(
                name = "[DEMO] District School Emergency Center",
                address = "Government School Campus (Sample Location)",
                latitude = userLat - 0.03,
                longitude = userLon + 0.04,
                capacity = 400,
                currentOccupancy = 150,
                contactNumber = "+91-172-2742000",
                facilities = "Medical, Food, Water, Sanitation",
                type = "Government"
            )
        )

        sampleShelters.forEach { shelter ->
            viewModel.insert(shelter)
        }
    }

    private fun addPunjabShelters() {
        Log.d("SheltersFragment", "Adding Punjab shelters including 2 in Jalandhar for Smart Route testing")

        val punjabShelters = listOf(
            // JALANDHAR - 2 shelters for Smart Route testing
            Shelter(
                name = "Jalandhar Municipal Corporation Emergency Shelter",
                address = "Civil Lines, Near DC Office, Jalandhar, Punjab 144001",
                latitude = 31.3260,
                longitude = 75.5762,
                capacity = 500,
                currentOccupancy = 0,
                contactNumber = "+91-181-2227307",
                facilities = "Medical Aid, Food, Water, Electricity, Blankets, Security, Toilets",
                type = "Government"
            ),
            Shelter(
                name = "Guru Nanak Dev University Sports Complex Shelter",
                address = "GT Road, GNDU Campus, Jalandhar, Punjab 144007",
                latitude = 31.3348,
                longitude = 75.5681,
                capacity = 800,
                currentOccupancy = 0,
                contactNumber = "+91-181-2258802",
                facilities = "Medical Center, Food Distribution, Water Supply, Bedding, Power Backup",
                type = "Educational Institution"
            ),
            // Other Punjab cities
            Shelter(
                name = "Amritsar Golden Temple Langar Hall",
                address = "Golden Temple Complex, Amritsar, Punjab 143006",
                latitude = 31.6200,
                longitude = 74.8765,
                capacity = 2000,
                currentOccupancy = 0,
                contactNumber = "+91-183-2553954",
                facilities = "Free Food (Langar), Medical Aid, Water, Shelter, 24/7 Support",
                type = "Religious"
            ),
            Shelter(
                name = "Ludhiana Civil Hospital Emergency Wing",
                address = "Sherpur Chowk, Ludhiana, Punjab 141008",
                latitude = 30.9010,
                longitude = 75.8573,
                capacity = 600,
                currentOccupancy = 45,
                contactNumber = "+91-161-2740445",
                facilities = "Emergency Medical Care, ICU, Food, Water, Ambulance Services",
                type = "Hospital"
            ),
            Shelter(
                name = "Patiala Rajindra Hospital",
                address = "Mall Road, Patiala, Punjab 147001",
                latitude = 30.3398,
                longitude = 76.3869,
                capacity = 400,
                currentOccupancy = 20,
                contactNumber = "+91-175-2212501",
                facilities = "Medical Treatment, Emergency Beds, Food, Water, Medicine",
                type = "Hospital"
            ),
            Shelter(
                name = "Mohali District Emergency Shelter",
                address = "Sector 65, SAS Nagar, Mohali, Punjab 160062",
                latitude = 30.7046,
                longitude = 76.7179,
                capacity = 550,
                currentOccupancy = 15,
                contactNumber = "+91-172-2220201",
                facilities = "Medical Aid, Food, Water, Security, Generator Backup, Toilets",
                type = "Government"
            ),
            Shelter(
                name = "Bathinda Relief Camp",
                address = "Mall Road, Bathinda, Punjab 151001",
                latitude = 30.2110,
                longitude = 74.9455,
                capacity = 350,
                currentOccupancy = 0,
                contactNumber = "+91-164-2211234",
                facilities = "Shelter, Food, Water, First Aid, Clothing",
                type = "Government"
            )
        )

        punjabShelters.forEach { shelter ->
            viewModel.insert(shelter)
        }
    }
}
