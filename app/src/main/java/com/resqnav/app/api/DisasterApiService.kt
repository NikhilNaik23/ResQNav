package com.resqnav.app.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// API Models for disaster data
data class EarthquakeResponse(
    val features: List<EarthquakeFeature>
)

data class EarthquakeFeature(
    val id: String, // Unique earthquake ID from USGS
    val properties: EarthquakeProperties,
    val geometry: EarthquakeGeometry
)

data class EarthquakeProperties(
    val mag: Double,
    val place: String,
    val time: Long,
    val title: String,
    val alert: String?
)

data class EarthquakeGeometry(
    val coordinates: List<Double> // [longitude, latitude, depth]
)

// NASA EONET API Models (for floods, fires, cyclones, etc.)
data class EonetResponse(
    val events: List<EonetEvent>
)

data class EonetEvent(
    val id: String,
    val title: String,
    val description: String?,
    val categories: List<EonetCategory>,
    val geometries: List<EonetGeometry>,
    val closed: String? // null if event is still active
)

data class EonetCategory(
    val id: String,
    val title: String
)

data class EonetGeometry(
    val date: String,
    val type: String,
    val coordinates: List<Double> // [longitude, latitude] or [longitude, latitude, 0]
)

// GDACS (Global Disaster Alert and Coordination System) API Models
data class GdacsResponse(
    val features: List<GdacsFeature>
)

data class GdacsFeature(
    val properties: GdacsProperties,
    val geometry: GdacsGeometry
)

data class GdacsProperties(
    val eventid: String,
    val name: String,
    val description: String?,
    val eventtype: String, // TC (Tropical Cyclone), FL (Flood), EQ (Earthquake), etc.
    val alertlevel: String?, // Green, Orange, Red
    val severity: String?,
    val fromdate: String,
    val todate: String?,
    val country: String?
)

data class GdacsGeometry(
    val type: String,
    val coordinates: List<Double> // [longitude, latitude]
)

// USGS Earthquake API Service
interface DisasterApiService {

    @GET("fdsnws/event/1/query")
    suspend fun getEarthquakes(
        @Query("format") format: String = "geojson",
        @Query("starttime") startTime: String,
        @Query("minmagnitude") minMagnitude: Double = 4.0,
        @Query("limit") limit: Int = 50
    ): Response<EarthquakeResponse>
}

// NASA EONET API Service for other disasters
interface EonetApiService {

    @GET("api/v3/events")
    suspend fun getEvents(
        @Query("status") status: String = "open",
        @Query("limit") limit: Int = 100,
        @Query("days") days: Int = 30
    ): Response<EonetResponse>
}

// GDACS API Service for floods, cyclones, etc.
interface GdacsApiService {

    @GET("gdacsapi/api/events/geteventlist/SEARCH")
    suspend fun getDisasters(
        @Query("fromDate") fromDate: String = "",
        @Query("toDate") toDate: String = "",
        @Query("alertlevel") alertLevel: String = "" // Green, Orange, Red
    ): Response<GdacsResponse>
}

// ReliefWeb API Models (UN OCHA Humanitarian Data)
data class ReliefWebResponse(
    val data: List<ReliefWebDisaster>
)

data class ReliefWebDisaster(
    val id: String,
    val fields: ReliefWebFields
)

data class ReliefWebFields(
    val id: Int?,
    val name: String?,
    val status: String?,
    val glide: String?,
    val description: String?,
    val primary_country: ReliefWebPrimaryCountry?,
    val primary_type: ReliefWebPrimaryType?
)

data class ReliefWebPrimaryCountry(
    val id: Int?,
    val name: String?,
    val shortname: String?,
    val iso3: String?,
    val location: ReliefWebLocation?
)

data class ReliefWebLocation(
    val lat: Double?,
    val lon: Double?
)

data class ReliefWebPrimaryType(
    val id: Int?,
    val name: String?,
    val code: String?
)

// Single disaster detail response
data class ReliefWebDetailResponse(
    val data: List<ReliefWebDisaster>
)

// ReliefWeb API Service (v2)
interface ReliefWebApiService {

    @GET("v2/disasters")
    suspend fun getDisasters(
        @Query("appname") appName: String = "Nikhil-ResQNav-J7y67q1D",
        @Query("profile") profile: String = "list",
        @Query("preset") preset: String = "latest",
        @Query("slim") slim: Int = 1,
        @Query("limit") limit: Int = 50,
        @Query("filter[type.name]") disasterType: String = ""
    ): Response<ReliefWebResponse>

    @GET("v2/disasters/{id}")
    suspend fun getDisasterDetail(
        @retrofit2.http.Path("id") id: String,
        @Query("appname") appName: String = "Nikhil-ResQNav-J7y67q1D",
        @Query("profile") profile: String = "full"
    ): Response<ReliefWebDetailResponse>
}

object DisasterApi {
    private const val BASE_URL = "https://earthquake.usgs.gov/"

    val service: DisasterApiService by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .client(
                okhttp3.OkHttpClient.Builder()
                    .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
                        level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
                    })
                    .build()
            )
            .build()
            .create(DisasterApiService::class.java)
    }
}

object EonetApi {
    private const val BASE_URL = "https://eonet.gsfc.nasa.gov/"

    val service: EonetApiService by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .client(
                okhttp3.OkHttpClient.Builder()
                    .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
                        level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
                    })
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
            )
            .build()
            .create(EonetApiService::class.java)
    }
}

object GdacsApi {
    private const val BASE_URL = "https://www.gdacs.org/"

    val service: GdacsApiService by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .client(
                okhttp3.OkHttpClient.Builder()
                    .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
                        level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
                    })
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
            )
            .build()
            .create(GdacsApiService::class.java)
    }
}

object ReliefWebApi {
    private const val BASE_URL = "https://api.reliefweb.int/"

    val service: ReliefWebApiService by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .client(
                okhttp3.OkHttpClient.Builder()
                    .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
                        level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
                    })
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
            )
            .build()
            .create(ReliefWebApiService::class.java)
    }
}

