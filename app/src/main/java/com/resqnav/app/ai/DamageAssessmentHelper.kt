package com.resqnav.app.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await
import java.io.IOException

/**
 * AI-powered damage assessment using ML Kit image recognition
 */
class DamageAssessmentHelper(private val context: Context) {

    private val TAG = "DamageAssessment"
    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    // Keywords that indicate disaster damage
    private val damageKeywords = listOf(
        "fire", "flame", "smoke", "burnt", "burning",
        "flood", "water", "flooded", "inundation",
        "collapsed", "debris", "rubble", "destruction", "damaged",
        "broken", "cracked", "fallen", "ruins"
    )

    /**
     * Assess damage from image URI
     */
    suspend fun assessDamageFromUri(imageUri: Uri): DamageAssessmentResult {
        return try {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            assessDamageFromBitmap(bitmap)
        } catch (e: IOException) {
            Log.e(TAG, "Error loading image", e)
            DamageAssessmentResult("ERROR", 0f, emptyList(), "Failed to load image")
        }
    }

    /**
     * Assess damage from bitmap image
     */
    suspend fun assessDamageFromBitmap(bitmap: Bitmap): DamageAssessmentResult {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val labels = labeler.process(image).await()

            // Analyze labels for damage indicators
            val damageLabels = labels.filter { label ->
                damageKeywords.any { keyword ->
                    label.text.lowercase().contains(keyword)
                }
            }

            // Calculate severity based on confidence and number of damage indicators
            val maxConfidence = damageLabels.maxOfOrNull { it.confidence } ?: 0f
            val severityScore = maxConfidence * (1 + damageLabels.size * 0.2f)

            val severity = when {
                severityScore > 0.7 -> "SEVERE"
                severityScore > 0.5 -> "MODERATE"
                severityScore > 0.3 -> "MINOR"
                else -> "NO_DAMAGE"
            }

            val detectedTypes = damageLabels.map { it.text }
            val description = generateDescription(severity, detectedTypes)

            DamageAssessmentResult(
                severity = severity,
                confidence = maxConfidence,
                detectedDamageTypes = detectedTypes,
                description = description
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error assessing damage", e)
            DamageAssessmentResult("ERROR", 0f, emptyList(), "Analysis failed: ${e.message}")
        }
    }

    /**
     * Quick damage check (yes/no)
     */
    suspend fun quickDamageCheck(bitmap: Bitmap): Boolean {
        return try {
            val result = assessDamageFromBitmap(bitmap)
            result.severity in listOf("SEVERE", "MODERATE", "MINOR")
        } catch (e: Exception) {
            false
        }
    }

    private fun generateDescription(severity: String, types: List<String>): String {
        return when (severity) {
            "SEVERE" -> "🔴 SEVERE DAMAGE DETECTED!\n" +
                    "Identified: ${types.joinToString(", ")}\n" +
                    "⚠️ This area is likely unsafe. Report to authorities immediately."
            "MODERATE" -> "🟠 Moderate damage detected.\n" +
                    "Identified: ${types.joinToString(", ")}\n" +
                    "⚡ Exercise caution in this area."
            "MINOR" -> "🟡 Minor damage indicators found.\n" +
                    "Identified: ${types.joinToString(", ")}\n" +
                    "✓ Proceed with caution."
            "NO_DAMAGE" -> "✅ No significant damage detected in image."
            else -> "Unable to assess damage from image."
        }
    }

    fun cleanup() {
        labeler.close()
    }
}

data class DamageAssessmentResult(
    val severity: String,
    val confidence: Float,
    val detectedDamageTypes: List<String>,
    val description: String
)
