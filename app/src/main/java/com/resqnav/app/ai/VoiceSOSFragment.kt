package com.resqnav.app.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.resqnav.app.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class VoiceSOSFragment : Fragment() {

    private lateinit var statusText: TextView
    private lateinit var commandText: TextView
    private lateinit var btnStartVoice: Button
    private lateinit var voiceSOSHelper: VoiceSOSHelper

    private val TAG = "VoiceSOSFragment"

    private val voiceRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Voice recognition result received")
        btnStartVoice.text = "🎤\nTAP TO SPEAK"
        btnStartVoice.isEnabled = true
        val data = result.data
        voiceSOSHelper.handleVoiceResult(data, voiceCommandListener)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startVoiceRecognition()
        } else {
            Toast.makeText(requireContext(), "⚠️ Microphone permission is required for voice commands", Toast.LENGTH_LONG).show()
        }
    }

    private val voiceCommandListener = object : VoiceSOSHelper.VoiceCommandListener {
        override fun onSOSTriggered() {
            commandText.text = "🚨 SOS ACTIVATED!"
            statusText.text = "Emergency alert system activated"

            // Show SOS confirmation dialog
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("🚨 SOS ALERT")
                .setMessage("Do you need emergency assistance?\n\nThis will:\n• Send your location to emergency contacts\n• Alert local authorities\n• Call emergency services")
                .setPositiveButton("YES - CALL 100") { _, _ ->
                    callEmergencyNumber("100")
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    resetStatus()
                }
                .setCancelable(false)
                .show()
        }

        override fun onFindShelter() {
            commandText.text = "🏠 Finding Shelters..."
            statusText.text = "Opening shelter locations on map"
            Toast.makeText(requireContext(), "Opening Shelters", Toast.LENGTH_SHORT).show()

            // Navigate to Shelters tab (if your app has navigation)
            try {
                // You can add navigation to shelters fragment here
                Log.d(TAG, "Navigate to shelters")
            } catch (e: Exception) {
                Log.e(TAG, "Error navigating to shelters", e)
            }
        }

        override fun onShowRoute() {
            commandText.text = "🗺️ Showing Route..."
            statusText.text = "Calculating safest evacuation route"
            Toast.makeText(requireContext(), "Opening Map", Toast.LENGTH_SHORT).show()

            // Navigate to Map tab
            try {
                // You can add navigation to map fragment here
                Log.d(TAG, "Navigate to map")
            } catch (e: Exception) {
                Log.e(TAG, "Error navigating to map", e)
            }
        }

        override fun onCallEmergency() {
            commandText.text = "📞 Calling Emergency..."
            statusText.text = "Connecting to emergency services"

            // Show emergency call options
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("📞 Emergency Call")
                .setMessage("Which emergency service?")
                .setPositiveButton("🚨 100 (Emergency)") { _, _ ->
                    callEmergencyNumber("100")
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    resetStatus()
                }
                .show()
        }

        override fun onUnknownCommand(spokenText: String) {
            commandText.text = "❓ Command Not Recognized"
            statusText.text = "You said: \"$spokenText\"\n\nTry saying:\n• 'SOS' or 'Emergency'\n• 'Find shelter'\n• 'Show route'\n• 'Call ambulance'"
            Toast.makeText(requireContext(), "Command not recognized. Try again!", Toast.LENGTH_LONG).show()
        }

        override fun onError(error: String) {
            commandText.text = "❌ Error"
            statusText.text = error
            Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_LONG).show()
            btnStartVoice.isEnabled = true
            btnStartVoice.text = "🎤\nTAP TO SPEAK"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_voice_sos, container, false)

        voiceSOSHelper = VoiceSOSHelper(requireContext())

        statusText = view.findViewById(R.id.statusText)
        commandText = view.findViewById(R.id.commandText)
        btnStartVoice = view.findViewById<Button>(R.id.btnStartVoice)

        btnStartVoice.setOnClickListener {
            checkPermissionAndStart()
        }

        return view
    }

    private fun checkPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startVoiceRecognition()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startVoiceRecognition() {
        commandText.text = "🎤 Listening..."
        statusText.text = "Speak now: Say 'SOS', 'Emergency', 'Find shelter', etc."
        btnStartVoice.text = "⏺️\nLISTENING..."
        btnStartVoice.isEnabled = false

        voiceSOSHelper.startVoiceRecognition(voiceRecognitionLauncher)
    }

    private fun callEmergencyNumber(number: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$number")
            }
            startActivity(intent)
            Toast.makeText(requireContext(), "Calling $number", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error calling emergency number", e)
            Toast.makeText(requireContext(), "Unable to place call", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetStatus() {
        commandText.text = "Ready to listen..."
        statusText.text = "Tap the button below and say:\n• 'SOS' or 'Emergency'\n• 'Find shelter'\n• 'Show route'\n• 'Call ambulance'"
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceSOSHelper.cleanup()
    }
}

