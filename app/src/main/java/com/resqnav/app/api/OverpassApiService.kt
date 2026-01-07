package com.resqnav.app.api


import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// OpenStreetMap Overpass API for finding emergency facilities
interface OverpassApiService {

    @FormUrlEncoded
    @POST("interpreter")
    suspend fun queryEmergencyFacilities(@Field("data") query: String): Response<OverpassResponse>

    companion object {
        private const val BASE_URL = "https://overpass-api.de/api/"

        fun create(): OverpassApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OverpassApiService::class.java)
        }

        fun buildQuery(lat: Double, lon: Double, radiusKm: Int = 20): String {
            val radiusMeters = radiusKm * 1000
            return """
                [out:json][timeout:25];
                (
                  node["amenity"="hospital"](around:$radiusMeters,$lat,$lon);
                  node["amenity"="clinic"](around:$radiusMeters,$lat,$lon);
                  node["amenity"="fire_station"](around:$radiusMeters,$lat,$lon);
                  node["amenity"="police"](around:$radiusMeters,$lat,$lon);
                  node["amenity"="community_centre"](around:$radiusMeters,$lat,$lon);
                  node["amenity"="social_facility"](around:$radiusMeters,$lat,$lon);
                  node["building"="government"](around:$radiusMeters,$lat,$lon);
                  way["amenity"="hospital"](around:$radiusMeters,$lat,$lon);
                  way["amenity"="clinic"](around:$radiusMeters,$lat,$lon);
                  way["amenity"="fire_station"](around:$radiusMeters,$lat,$lon);
                  way["amenity"="police"](around:$radiusMeters,$lat,$lon);
                  way["amenity"="community_centre"](around:$radiusMeters,$lat,$lon);
                  way["amenity"="social_facility"](around:$radiusMeters,$lat,$lon);
                );
                out center;
            """.trimIndent()
        }
    }
}

data class OverpassResponse(
    val elements: List<OverpassElement>
)

data class OverpassElement(
    val type: String,
    val id: Long,
    val lat: Double?,
    val lon: Double?,
    val center: OverpassCenter?,
    val tags: Map<String, String>?
)

data class OverpassCenter(
    val lat: Double,
    val lon: Double
)

