package com.resqnav.app.ai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import java.util.Locale

/**
 * Voice-activated emergency SOS system
 * Supports hands-free emergency triggers
 */
class VoiceSOSHelper(private val context: Context) {

    private val TAG = "VoiceSOSHelper"
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    interface VoiceCommandListener {
        fun onSOSTriggered()
        fun onFindShelter()
        fun onShowRoute()
        fun onCallEmergency()
        fun onUnknownCommand(spokenText: String)
        fun onError(error: String)
    }

    /**
     * Start voice recognition using Activity Result Launcher
     */
    fun startVoiceRecognition(launcher: ActivityResultLauncher<Intent>) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "⚠️ Speech recognition not available on this device", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "🎤 Say: 'SOS', 'Emergency', 'Find shelter', or 'Call ambulance'")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Improve accuracy
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
        }

        try {
            launcher.launch(intent)
            Log.d(TAG, "Voice recognition started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start voice recognition", e)
            Toast.makeText(context, "❌ Voice recognition failed to start. Please try again.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Start continuous voice recognition
     */
    fun startContinuousListening(listener: VoiceCommandListener) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onError("Speech recognition not available on this device")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    Log.d(TAG, "Ready for speech")
                }

                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "Speech started")
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    isListening = false
                    Log.d(TAG, "Speech ended")
                }

                override fun onError(error: Int) {
                    isListening = false
                    val errorMessage = getErrorMessage(error)
                    Log.e(TAG, "Speech recognition error: $errorMessage")
                    listener.onError(errorMessage)
                }

                override fun onResults(results: Bundle?) {
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                        if (matches.isNotEmpty()) {
                            val spokenText = matches[0]
                            handleVoiceCommand(spokenText, listener)
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        speechRecognizer?.startListening(intent)
    }

    /**
     * Handle spoken text from Activity Result
     */
    fun handleVoiceResult(data: Intent?, listener: VoiceCommandListener) {
        data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.let { results ->
            if (results.isNotEmpty()) {
                val spokenText = results[0]
                handleVoiceCommand(spokenText, listener)
            }
        }
    }

    /**
     * Process voice command and trigger appropriate action
     */
    private fun handleVoiceCommand(spokenText: String, listener: VoiceCommandListener) {

        Log.d(TAG, "Voice command received: $spokenText")

        val lowerText = spokenText.lowercase(Locale.getDefault())

        when {
            // SOS and emergency triggers - expanded keywords
            lowerText.contains("sos") ||
            lowerText.contains("emergency") ||
            lowerText.contains("help me") ||
            lowerText.contains("help") ||
            lowerText.contains("danger") ||
            lowerText.contains("urgent") ||
            lowerText.contains("crisis") -> {
                Log.d(TAG, "SOS triggered")
                listener.onSOSTriggered()
            }

            // Shelter finding - expanded keywords
            lowerText.contains("shelter") ||
            lowerText.contains("safe place") ||
            lowerText.contains("safe location") ||
            lowerText.contains("evacuation center") ||
            lowerText.contains("refuge") -> {
                Log.d(TAG, "Find shelter triggered")
                listener.onFindShelter()
            }

            // Route/navigation - expanded keywords
            lowerText.contains("route") ||
            lowerText.contains("navigate") ||
            lowerText.contains("direction") ||
            lowerText.contains("show map") ||
            lowerText.contains("how do i get") ||
            lowerText.contains("take me to") -> {
                Log.d(TAG, "Show route triggered")
                listener.onShowRoute()
            }

            // Emergency call - expanded keywords
            (lowerText.contains("call") || lowerText.contains("dial") || lowerText.contains("phone")) &&
            (lowerText.contains("ambulance") ||
             lowerText.contains("police") ||
             lowerText.contains("fire") ||
             lowerText.contains("100") ||
             lowerText.contains("emergency") ||
             lowerText.contains("help")) -> {
                Log.d(TAG, "Call emergency triggered")
                listener.onCallEmergency()
            }

            else -> {
                Log.d(TAG, "Unknown command: $spokenText")
                listener.onUnknownCommand(spokenText)
            }
        }
    }

    /**
     * Stop listening
     */
    fun stopListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }

    private fun getErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }
    }
}

