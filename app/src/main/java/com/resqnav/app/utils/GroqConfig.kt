package com.resqnav.app.utils

import android.util.Log

/**
 * Groq API Configuration
 * 100% FREE API with fast LLama models!
 * Get your free API key from: https://console.groq.com/keys
 *
 * SECURITY NOTE:
 * For open source projects, add your API key to local.properties:
 * GROQ_API_KEY=your_actual_key_here
 */
object GroqConfig {

    private const val TAG = "GroqConfig"

    // API key - Set this in your local.properties file:
    // GROQ_API_KEY=your_actual_key_here
    // DO NOT COMMIT YOUR ACTUAL API KEY TO GIT!
    const val GROQ_API_KEY = "YOUR_GROQ_API_KEY_HERE"  // Replace with your actual key from console.groq.com

    // Groq API endpoint
    const val BASE_URL = "https://api.groq.com/openai/v1/"

    // Using LLama 3.1 8B Instant - Fastest and most reliable free model
    // This model is guaranteed to work with Groq API
    const val MODEL_NAME = "llama-3.1-8b-instant"

    // Generation parameters
    const val MAX_TOKENS = 512
    const val TEMPERATURE = 0.5

    init {
        Log.d(TAG, "Groq Configuration Initialized")
        Log.d(TAG, "Model: $MODEL_NAME")
        Log.d(TAG, "API Key: ${if (GROQ_API_KEY.length > 20) GROQ_API_KEY.take(20) + "..." else "NOT SET"}")
    }

    fun isConfigured(): Boolean {
        return GROQ_API_KEY.isNotEmpty() &&
               GROQ_API_KEY != "YOUR_GROQ_API_KEY_HERE"
    }
}



