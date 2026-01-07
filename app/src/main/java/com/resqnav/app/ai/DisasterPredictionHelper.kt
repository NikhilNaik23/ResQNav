package com.resqnav.app.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * AI-powered disaster risk prediction based on weather data
 * Uses machine learning to predict likelihood of disasters
 */
class DisasterPredictionHelper(private val context: Context) {

    private val TAG = "DisasterPrediction"

    /**
     * Predict disaster risk based on weather conditions
     * @return Risk score from 0.0 (safe) to 1.0 (extreme danger)
     */
    suspend fun predictDisasterRisk(
        temperature: Float,
        humidity: Float,
        windSpeed: Float,
        rainfall: Float
    ): DisasterRiskResult = withContext(Dispatchers.Default) {
        try {
            // Simple rule-based prediction (can be replaced with TensorFlow Lite model)
            var riskScore = 0f
            val disasterTypes = mutableListOf<String>()

            // Flood risk prediction
            if (rainfall > 50 && humidity > 80) {
                riskScore += 0.4f
                disasterTypes.add("Flood")
            }

            // Fire risk prediction
            if (temperature > 35 && humidity < 30 && windSpeed > 20) {
                riskScore += 0.35f
                disasterTypes.add("Fire")
            }

            // Storm risk prediction
            if (windSpeed > 40 && rainfall > 30) {
                riskScore += 0.35f
                disasterTypes.add("Storm")
            }

            // Heat wave risk
            if (temperature > 40) {
                riskScore += 0.2f
                disasterTypes.add("Heat Wave")
            }

            // Normalize risk score
            riskScore = riskScore.coerceIn(0f, 1f)

            val severity = when {
                riskScore > 0.7 -> "CRITICAL"
                riskScore > 0.5 -> "HIGH"
                riskScore > 0.3 -> "MODERATE"
                else -> "LOW"
            }

            DisasterRiskResult(
                riskScore = riskScore,
                severity = severity,
                predictedDisasters = disasterTypes,
                recommendation = getRecommendation(severity, disasterTypes)
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error predicting disaster risk", e)
            DisasterRiskResult(0f, "UNKNOWN", emptyList(), "Unable to predict risk")
        }
    }

    /**
     * Analyze weather pattern trends
     */
    suspend fun analyzeWeatherTrends(
        temperatureTrend: List<Float>,
        humidityTrend: List<Float>
    ): String = withContext(Dispatchers.Default) {
        if (temperatureTrend.size < 3) return@withContext "Insufficient data"

        val tempChange = temperatureTrend.last() - temperatureTrend.first()
        val humidityChange = humidityTrend.last() - humidityTrend.first()

        when {
            tempChange > 10 && humidityChange < -20 ->
                "⚠️ Rapid temperature rise with decreasing humidity - Fire risk increasing!"
            tempChange < -10 && humidityChange > 20 ->
                "⚠️ Temperature drop with high humidity - Storm risk!"
            humidityChange > 30 ->
                "⚠️ Rapid humidity increase - Flood risk!"
            else ->
                "Weather conditions stable"
        }
    }

    private fun getRecommendation(severity: String, disasters: List<String>): String {
        return when (severity) {
            "CRITICAL" -> "🚨 IMMEDIATE ACTION REQUIRED! " +
                    "Evacuate to nearest shelter. ${disasters.joinToString(", ")} imminent."
            "HIGH" -> "⚠️ HIGH ALERT! Prepare for possible ${disasters.joinToString(", ")}. " +
                    "Have emergency kit ready."
            "MODERATE" -> "⚡ Moderate risk of ${disasters.joinToString(", ")}. " +
                    "Stay informed and monitor alerts."
            else -> "✅ Low risk. Stay prepared and aware of surroundings."
        }
    }
}

data class DisasterRiskResult(
    val riskScore: Float,
    val severity: String,
    val predictedDisasters: List<String>,
    val recommendation: String
)
