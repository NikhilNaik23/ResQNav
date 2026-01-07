package com.resqnav.app.api

import com.resqnav.app.utils.GroqConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// Request/Response data classes for Groq (OpenAI-compatible)
data class GroqChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val max_tokens: Int? = null,
    val temperature: Double? = null
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class GroqChatResponse(
    val id: String,
    val choices: List<ChatChoice>,
    val usage: GroqUsage? = null
)

data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    val finish_reason: String
)

data class GroqUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

// Retrofit API Interface
interface GroqApiService {
    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun createChatCompletion(@Body request: GroqChatRequest): GroqChatResponse
}

// API Client
object GroqApiClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Enable full logging to debug
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${GroqConfig.GROQ_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(GroqConfig.BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: GroqApiService = retrofit.create(GroqApiService::class.java)
}

