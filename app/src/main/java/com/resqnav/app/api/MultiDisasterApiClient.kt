package com.resqnav.app.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Centralized API client for multiple disaster data sources
 * Provides access to:
 * - USGS Earthquake API
 * - NASA EONET API (Floods, Wildfires, Storms, Volcanoes)
 * - GDACS API (Global Disaster Alert)
 */
object MultiDisasterApiClient {

    // USGS Earthquake API (✅ WORKING)
    private const val USGS_BASE_URL = "https://earthquake.usgs.gov/"

    private const val RELIEFWEB_BASE_URL = "https://api.reliefweb.int/"

    // GDACS RSS/JSON Feed (✅ WORKING - Global Disaster Alert)
    private const val GDACS_BASE_URL = "https://www.gdacs.org/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // User-Agent interceptor for ReliefWeb API compliance
    private val userAgentInterceptor = okhttp3.Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("User-Agent", "ResQNav/1.0 (Disaster Management App; contact@resqnav.app)")
            .addHeader("Accept", "application/json")
            .build()
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Separate client for ReliefWeb with User-Agent
    private val reliefwebOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(userAgentInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // USGS Earthquake Service (✅ WORKING)
    private val usgsRetrofit = Retrofit.Builder()
        .baseUrl(USGS_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val earthquakeService: DisasterApiService = usgsRetrofit.create(DisasterApiService::class.java)

    // ReliefWeb API Service (Floods, Droughts, Cyclones, etc.)
    private val reliefwebRetrofit = Retrofit.Builder()
        .baseUrl(RELIEFWEB_BASE_URL)
        .client(reliefwebOkHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val reliefwebService: ReliefWebApiService = reliefwebRetrofit.create(ReliefWebApiService::class.java)

    // GDACS Service (Backup for multiple disaster types)
    private val gdacsRetrofit = Retrofit.Builder()
        .baseUrl(GDACS_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val gdacsService: GdacsApiService = gdacsRetrofit.create(GdacsApiService::class.java)
}

