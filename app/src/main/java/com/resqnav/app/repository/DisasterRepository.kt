package com.resqnav.app.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.resqnav.app.api.MultiDisasterApiClient
import com.resqnav.app.database.DisasterDao
import com.resqnav.app.model.DisasterAlert
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

class DisasterRepository(private val disasterDao: DisasterDao) {

    val allActiveAlerts: LiveData<List<DisasterAlert>> = disasterDao.getAllActiveAlerts()
    val allAlerts: LiveData<List<DisasterAlert>> = disasterDao.getAllAlerts()

    suspend fun insert(alert: DisasterAlert) {
        disasterDao.insert(alert)
    }

    suspend fun insertAll(alerts: List<DisasterAlert>) {
        disasterDao.insertAll(alerts)
    }

    suspend fun update(alert: DisasterAlert) {
        disasterDao.update(alert)
    }

    suspend fun delete(alert: DisasterAlert) {
        disasterDao.delete(alert)
    }

    suspend fun deleteOldInactiveAlerts(timestamp: Long) {
        disasterDao.deleteOldInactiveAlerts(timestamp)
    }

    fun getAlertsBySeverity(severity: String): LiveData<List<DisasterAlert>> {
        return disasterDao.getAlertsBySeverity(severity)
    }

    /**
     * Fetch all disaster types in parallel from multiple APIs
     */
    suspend fun fetchAllDisasters(): List<DisasterAlert> = coroutineScope {
        Log.d("DisasterRepository", "Starting to fetch all disaster types...")

        val earthquakes = async { fetchEarthquakes() }
        val floods = async { fetchFloods() }
        val wildfires = async { fetchWildfires() }
        val storms = async { fetchStorms() }
        val volcanoes = async { fetchVolcanoes() }

        val allDisasters = mutableListOf<DisasterAlert>()

        // Fetch earthquakes
        try {
            val earthquakeList = earthquakes.await()
            allDisasters.addAll(earthquakeList)
            Log.d("DisasterRepository", "✅ Fetched ${earthquakeList.size} earthquakes")
        } catch (e: Exception) {
            Log.e("DisasterRepository", "❌ Error fetching earthquakes: ${e.message}", e)
        }

        // Fetch floods
        try {
            val floodList = floods.await()
            allDisasters.addAll(floodList)
            Log.d("DisasterRepository", "✅ Fetched ${floodList.size} floods")
        } catch (e: Exception) {
            Log.e("DisasterRepository", "❌ Error fetching floods: ${e.message}", e)
        }

        // Fetch wildfires
        try {
            val wildfireList = wildfires.await()
            allDisasters.addAll(wildfireList)
            Log.d("DisasterRepository", "✅ Fetched ${wildfireList.size} wildfires")
        } catch (e: Exception) {
            Log.e("DisasterRepository", "❌ Error fetching wildfires: ${e.message}", e)
        }

        // Fetch storms
        try {
            val stormList = storms.await()
            allDisasters.addAll(stormList)
            Log.d("DisasterRepository", "✅ Fetched ${stormList.size} storms")
        } catch (e: Exception) {
            Log.e("DisasterRepository", "❌ Error fetching storms: ${e.message}", e)
        }

        // Fetch volcanoes
        try {
            val volcanoList = volcanoes.await()
            allDisasters.addAll(volcanoList)
            Log.d("DisasterRepository", "✅ Fetched ${volcanoList.size} volcanoes")
        } catch (e: Exception) {
            Log.e("DisasterRepository", "❌ Error fetching volcanoes: ${e.message}", e)
        }

        Log.d("DisasterRepository", "🎉 Total disasters fetched: ${allDisasters.size}")
        allDisasters
    }

    /**
     * Fetch earthquakes from USGS API
     */
    private suspend fun fetchEarthquakes(): List<DisasterAlert> {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val thirtyDaysAgo = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -30)
            }.time
            val startTime = dateFormat.format(thirtyDaysAgo)

            val response = MultiDisasterApiClient.earthquakeService.getEarthquakes(
                startTime = startTime,
                minMagnitude = 4.0,
                limit = 100
            )

            if (response.isSuccessful && response.body() != null) {
                response.body()!!.features.map { feature ->
                    val coords = feature.geometry.coordinates
                    DisasterAlert(
                        externalId = "eq_${feature.id}",
                        type = "earthquake",
                        severity = calculateEarthquakeSeverity(feature.properties.mag),
                        title = feature.properties.title,
                        description = "Magnitude ${feature.properties.mag} earthquake at ${feature.properties.place}",
                        latitude = coords[1],
                        longitude = coords[0],
                        radius = calculateEarthquakeRadius(feature.properties.mag),
                        timestamp = feature.properties.time,
                        isActive = true
                    )
                }
            } else {
                Log.e("DisasterRepository", "Earthquake API error: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("DisasterRepository", "Exception fetching earthquakes: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Fetch floods from ReliefWeb API (UN OCHA)
     */
    private suspend fun fetchFloods(): List<DisasterAlert> {
        return try {
            Log.d("DisasterRepository", "🌊 Calling ReliefWeb API for floods...")
            val response = MultiDisasterApiClient.reliefwebService.getDisasters()

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Log.d("DisasterRepository", "📦 ReliefWeb returned ${body.data.size} total disasters")

                val floodDisasters = body.data.filter { disaster ->
                    val name = disaster.fields.name ?: ""
                    name.contains("flood", ignoreCase = true)
                }.take(10) // Limit to 10 floods to reduce API calls

                Log.d("DisasterRepository", "🌊 Found ${floodDisasters.size} flood-related disasters (limited to 10)")

                val alerts = floodDisasters.mapNotNull { disaster ->
                    try {
                        // Fetch detailed disaster info to get coordinates
                        val detailResponse = MultiDisasterApiClient.reliefwebService.getDisasterDetail(disaster.id)

                        if (detailResponse.isSuccessful && detailResponse.body() != null) {
                            val detail = detailResponse.body()!!.data.firstOrNull() ?: return@mapNotNull null
                            val location = detail.fields.primary_country?.location
                            val name = detail.fields.name ?: return@mapNotNull null

                            if (location?.lat == null || location.lon == null) {
                                Log.w("DisasterRepository", "⚠️ Skipping flood '$name' - no location data")
                                return@mapNotNull null
                            }

                            Log.d("DisasterRepository", "✅ Adding flood: $name at (${location.lat}, ${location.lon})")
                            DisasterAlert(
                                externalId = "reliefweb_flood_${disaster.id}",
                                type = "flood",
                                severity = "high",
                                title = name,
                                description = detail.fields.description?.take(200) ?: "Active flood disaster (GLIDE: ${detail.fields.glide ?: "N/A"})",
                                latitude = location.lat,
                                longitude = location.lon,
                                radius = 50.0,
                                timestamp = parseDateFromDisasterName(name),
                                isActive = detail.fields.status?.equals("ongoing", ignoreCase = true) == true
                            )
                        } else {
                            Log.w("DisasterRepository", "⚠️ Failed to fetch detail for disaster ${disaster.id}")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("DisasterRepository", "❌ Error fetching detail for disaster ${disaster.id}: ${e.message}")
                        null
                    }
                }
                Log.d("DisasterRepository", "✅ Successfully created ${alerts.size} flood alerts")
                alerts
            } else {
                Log.e("DisasterRepository", "❌ Floods API error: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("DisasterRepository", "❌ Exception fetching floods: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Fetch wildfires from ReliefWeb API
     */
    private suspend fun fetchWildfires(): List<DisasterAlert> {
        return try {
            Log.d("DisasterRepository", "🔥 Calling ReliefWeb API for fires...")
            val response = MultiDisasterApiClient.reliefwebService.getDisasters()

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val fireDisasters = body.data.filter { disaster ->
                    val name = disaster.fields.name ?: ""
                    name.contains("fire", ignoreCase = true) ||
                    name.contains("wildfire", ignoreCase = true)
                }.take(5) // Limit to 5 fires

                Log.d("DisasterRepository", "🔥 Found ${fireDisasters.size} fire-related disasters (limited to 5)")

                fireDisasters.mapNotNull { disaster ->
                    try {
                        val detailResponse = MultiDisasterApiClient.reliefwebService.getDisasterDetail(disaster.id)

                        if (detailResponse.isSuccessful && detailResponse.body() != null) {
                            val detail = detailResponse.body()!!.data.firstOrNull() ?: return@mapNotNull null
                            val location = detail.fields.primary_country?.location
                            val name = detail.fields.name ?: return@mapNotNull null

                            if (location?.lat == null || location.lon == null) {
                                Log.w("DisasterRepository", "⚠️ Skipping fire '$name' - no location data")
                                return@mapNotNull null
                            }

                            DisasterAlert(
                                externalId = "reliefweb_fire_${disaster.id}",
                                type = "fire",
                                severity = "high",
                                title = name,
                                description = detail.fields.description?.take(200) ?: "Active wildfire (GLIDE: ${detail.fields.glide ?: "N/A"})",
                                latitude = location.lat,
                                longitude = location.lon,
                                radius = 30.0,
                                timestamp = parseDateFromDisasterName(name),
                                isActive = detail.fields.status?.equals("ongoing", ignoreCase = true) == true
                            )
                        } else null
                    } catch (e: Exception) {
                        Log.e("DisasterRepository", "❌ Error fetching fire detail: ${e.message}")
                        null
                    }
                }
            } else {
                Log.e("DisasterRepository", "Wildfires API error: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("DisasterRepository", "Exception fetching wildfires: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Fetch storms/cyclones from ReliefWeb API
     */
    private suspend fun fetchStorms(): List<DisasterAlert> {
        return try {
            Log.d("DisasterRepository", "🌪️ Calling ReliefWeb API for storms...")
            val response = MultiDisasterApiClient.reliefwebService.getDisasters()

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val stormDisasters = body.data.filter { disaster ->
                    val name = disaster.fields.name ?: ""
                    name.contains("storm", ignoreCase = true) ||
                    name.contains("cyclone", ignoreCase = true) ||
                    name.contains("hurricane", ignoreCase = true) ||
                    name.contains("typhoon", ignoreCase = true)
                }.take(5) // Limit to 5 storms

                Log.d("DisasterRepository", "🌪️ Found ${stormDisasters.size} storm-related disasters (limited to 5)")

                stormDisasters.mapNotNull { disaster ->
                    try {
                        val detailResponse = MultiDisasterApiClient.reliefwebService.getDisasterDetail(disaster.id)

                        if (detailResponse.isSuccessful && detailResponse.body() != null) {
                            val detail = detailResponse.body()!!.data.firstOrNull() ?: return@mapNotNull null
                            val location = detail.fields.primary_country?.location
                            val name = detail.fields.name ?: return@mapNotNull null

                            if (location?.lat == null || location.lon == null) {
                                Log.w("DisasterRepository", "⚠️ Skipping storm '$name' - no location data")
                                return@mapNotNull null
                            }

                            DisasterAlert(
                                externalId = "reliefweb_cyclone_${disaster.id}",
                                type = "cyclone",
                                severity = "critical",
                                title = name,
                                description = detail.fields.description?.take(200) ?: "Active storm/cyclone (GLIDE: ${detail.fields.glide ?: "N/A"})",
                                latitude = location.lat,
                                longitude = location.lon,
                                radius = 200.0,
                                timestamp = parseDateFromDisasterName(name),
                                isActive = detail.fields.status?.equals("ongoing", ignoreCase = true) == true
                            )
                        } else null
                    } catch (e: Exception) {
                        Log.e("DisasterRepository", "❌ Error fetching storm detail: ${e.message}")
                        null
                    }
                }
            } else {
                Log.e("DisasterRepository", "Storms API error: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("DisasterRepository", "Exception fetching storms: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Fetch volcanoes from ReliefWeb API
     */
    private suspend fun fetchVolcanoes(): List<DisasterAlert> {
        return try {
            Log.d("DisasterRepository", "🌋 Calling ReliefWeb API for volcanoes...")
            val response = MultiDisasterApiClient.reliefwebService.getDisasters()

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val volcanoDisasters = body.data.filter { disaster ->
                    val name = disaster.fields.name ?: ""
                    name.contains("volcano", ignoreCase = true) ||
                    name.contains("volcanic", ignoreCase = true)
                }.take(3) // Limit to 3 volcanoes

                Log.d("DisasterRepository", "🌋 Found ${volcanoDisasters.size} volcano-related disasters (limited to 3)")

                volcanoDisasters.mapNotNull { disaster ->
                    try {
                        val detailResponse = MultiDisasterApiClient.reliefwebService.getDisasterDetail(disaster.id)

                        if (detailResponse.isSuccessful && detailResponse.body() != null) {
                            val detail = detailResponse.body()!!.data.firstOrNull() ?: return@mapNotNull null
                            val location = detail.fields.primary_country?.location
                            val name = detail.fields.name ?: return@mapNotNull null

                            if (location?.lat == null || location.lon == null) {
                                Log.w("DisasterRepository", "⚠️ Skipping volcano '$name' - no location data")
                                return@mapNotNull null
                            }

                            DisasterAlert(
                                externalId = "reliefweb_volcano_${disaster.id}",
                                type = "volcano",
                                severity = "high",
                                title = name,
                                description = detail.fields.description?.take(200) ?: "Active volcanic activity (GLIDE: ${detail.fields.glide ?: "N/A"})",
                                latitude = location.lat,
                                longitude = location.lon,
                                radius = 40.0,
                                timestamp = parseDateFromDisasterName(name),
                                isActive = detail.fields.status?.equals("ongoing", ignoreCase = true) == true
                            )
                        } else null
                    } catch (e: Exception) {
                        Log.e("DisasterRepository", "❌ Error fetching volcano detail: ${e.message}")
                        null
                    }
                }
            } else {
                Log.e("DisasterRepository", "Volcanoes API error: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("DisasterRepository", "Exception fetching volcanoes: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get approximate center coordinates for a country name
     */
    private fun getCountryCoordinates(countryName: String): Pair<Double, Double>? {
        val countryMap = mapOf(
            // Asia
            "Philippines" to Pair(12.8797, 121.7740),
            "India" to Pair(20.5937, 78.9629),
            "China" to Pair(35.8617, 104.1954),
            "Japan" to Pair(36.2048, 138.2529),
            "Indonesia" to Pair(-0.7893, 113.9213),
            "Pakistan" to Pair(30.3753, 69.3451),
            "Bangladesh" to Pair(23.6850, 90.3563),
            "Nepal" to Pair(28.3949, 84.1240),
            "Myanmar" to Pair(21.9162, 95.9560),
            "Thailand" to Pair(15.8700, 100.9925),
            "Vietnam" to Pair(14.0583, 108.2772),
            "Afghanistan" to Pair(33.9391, 67.7100),
            "Sri Lanka" to Pair(7.8731, 80.7718),
            "Malaysia" to Pair(4.2105, 101.9758),
            "South Korea" to Pair(35.9078, 127.7669),
            "North Korea" to Pair(40.3399, 127.5101),
            "Cambodia" to Pair(12.5657, 104.9910),
            "Laos" to Pair(19.8563, 102.4955),

            // Middle East
            "Turkey" to Pair(38.9637, 35.2433),
            "Iran" to Pair(32.4279, 53.6880),
            "Iraq" to Pair(33.2232, 43.6793),
            "Syria" to Pair(34.8021, 38.9968),
            "Yemen" to Pair(15.5527, 48.5164),
            "Saudi Arabia" to Pair(23.8859, 45.0792),
            "Lebanon" to Pair(33.8547, 35.8623),
            "Jordan" to Pair(30.5852, 36.2384),
            "Israel" to Pair(31.0461, 34.8516),
            "Palestine" to Pair(31.9522, 35.2332),

            // Africa
            "Nigeria" to Pair(9.0820, 8.6753),
            "Kenya" to Pair(-0.0236, 37.9062),
            "Ethiopia" to Pair(9.1450, 40.4897),
            "South Africa" to Pair(-30.5595, 22.9375),
            "Egypt" to Pair(26.8206, 30.8025),
            "Democratic Republic of the Congo" to Pair(-4.0383, 21.7587),
            "Congo" to Pair(-0.2280, 15.8277),
            "Somalia" to Pair(5.1521, 46.1996),
            "Sudan" to Pair(12.8628, 30.2176),
            "South Sudan" to Pair(6.8770, 31.3070),
            "Tanzania" to Pair(-6.3690, 34.8888),
            "Uganda" to Pair(1.3733, 32.2903),
            "Mozambique" to Pair(-18.6657, 35.5296),
            "Ghana" to Pair(7.9465, -1.0232),
            "Madagascar" to Pair(-18.7669, 46.8691),
            "Cameroon" to Pair(7.3697, 12.3547),
            "Mali" to Pair(17.5707, -3.9962),
            "Niger" to Pair(17.6078, 8.0817),
            "Chad" to Pair(15.4542, 18.7322),
            "Zimbabwe" to Pair(-19.0154, 29.1549),
            "Malawi" to Pair(-13.2543, 34.3015),
            "Zambia" to Pair(-13.1339, 27.8493),

            // Europe
            "Greece" to Pair(39.0742, 21.8243),
            "Italy" to Pair(41.8719, 12.5674),
            "Spain" to Pair(40.4637, -3.7492),
            "France" to Pair(46.6034, 1.8883),
            "Germany" to Pair(51.1657, 10.4515),
            "United Kingdom" to Pair(55.3781, -3.4360),
            "Poland" to Pair(51.9194, 19.1451),
            "Ukraine" to Pair(48.3794, 31.1656),
            "Romania" to Pair(45.9432, 24.9668),
            "Portugal" to Pair(39.3999, -8.2245),
            "Georgia" to Pair(42.3154, 43.3569),
            "Albania" to Pair(41.1533, 20.1683),
            "Bosnia and Herzegovina" to Pair(43.9159, 17.6791),
            "Serbia" to Pair(44.0165, 21.0059),
            "Croatia" to Pair(45.1000, 15.2000),

            // Americas
            "United States" to Pair(37.0902, -95.7129),
            "Mexico" to Pair(23.6345, -102.5528),
            "Brazil" to Pair(-14.2350, -51.9253),
            "Canada" to Pair(56.1304, -106.3468),
            "Peru" to Pair(-9.1900, -75.0152),
            "Colombia" to Pair(4.5709, -74.2973),
            "Venezuela" to Pair(6.4238, -66.5897),
            "Chile" to Pair(-35.6751, -71.5430),
            "Ecuador" to Pair(-1.8312, -78.1834),
            "Guatemala" to Pair(15.7835, -90.2308),
            "Haiti" to Pair(18.9712, -72.2852),
            "Honduras" to Pair(15.2000, -86.2419),
            "Nicaragua" to Pair(12.8654, -85.2072),
            "El Salvador" to Pair(13.7942, -88.8965),
            "Costa Rica" to Pair(9.7489, -83.7534),
            "Panama" to Pair(8.5380, -80.7821),
            "Bolivia" to Pair(-16.2902, -63.5887),
            "Paraguay" to Pair(-23.4425, -58.4438),
            "Argentina" to Pair(-38.4161, -63.6167),

            // Oceania
            "Australia" to Pair(-25.2744, 133.7751),
            "Papua New Guinea" to Pair(-6.3150, 143.9555),
            "Fiji" to Pair(-17.7134, 178.0650),
            "Vanuatu" to Pair(-15.3767, 166.9592),
            "Solomon Islands" to Pair(-9.6457, 160.1562),
            "New Zealand" to Pair(-40.9006, 174.8860)
        )

        return countryMap[countryName]
    }

    /**
     * Parse date from ReliefWeb disaster name format: "Country: Disaster - MMM YYYY"
     * Example: "Lao PDR: Floods - Jul 2025" -> timestamp for July 2025
     */
    private fun parseDateFromDisasterName(name: String): Long {
        return try {
            // Extract date string from format "Country: Disaster - MMM YYYY"
            val datePattern = Regex("""- (\w{3}) (\d{4})$""")
            val matchResult = datePattern.find(name)

            if (matchResult != null) {
                val (month, year) = matchResult.destructured
                val dateStr = "01 $month $year" // Use 1st day of month
                val format = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                format.timeZone = TimeZone.getTimeZone("UTC")
                val date = format.parse(dateStr)
                val timestamp = date?.time ?: System.currentTimeMillis()
                Log.d("DisasterRepository", "Parsed date from '$name': $dateStr -> $timestamp")
                return timestamp
            }

            Log.w("DisasterRepository", "No date pattern found in: $name")
            // If no date pattern found, return current time
            System.currentTimeMillis()
        } catch (e: Exception) {
            Log.w("DisasterRepository", "Failed to parse date from: $name", e)
            System.currentTimeMillis()
        }
    }

    private fun calculateEarthquakeSeverity(magnitude: Double): String {
        return when {
            magnitude >= 7.0 -> "critical"
            magnitude >= 6.0 -> "high"
            magnitude >= 5.0 -> "medium"
            else -> "low"
        }
    }

    private fun calculateEarthquakeRadius(magnitude: Double): Double {
        // Radius increases exponentially with magnitude
        return 10.0 * (1.5.pow(magnitude - 4.0))
    }

    private fun parseEonetDate(dateString: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}

