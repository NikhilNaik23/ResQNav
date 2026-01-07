package com.resqnav.app.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.resqnav.app.R
import kotlinx.coroutines.launch

class DamageAssessmentFragment : Fragment() {

    private lateinit var damageHelper: DamageAssessmentHelper
    private lateinit var imagePreview: ImageView
    private lateinit var resultText: TextView
    private lateinit var severityText: TextView
    private lateinit var detailsText: TextView
    private var capturedImage: Bitmap? = null

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                capturedImage = imageBitmap
                imagePreview.setImageBitmap(imageBitmap)
                imagePreview.visibility = View.VISIBLE
                analyzeDamage(imageBitmap)
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_damage_assessment, container, false)

        damageHelper = DamageAssessmentHelper(requireContext())

        imagePreview = view.findViewById(R.id.imagePreview)
        resultText = view.findViewById(R.id.resultText)
        severityText = view.findViewById(R.id.severityText)
        detailsText = view.findViewById(R.id.detailsText)

        view.findViewById<Button>(R.id.btnTakePhoto).setOnClickListener {
            checkCameraPermission()
        }

        return view
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private fun analyzeDamage(bitmap: Bitmap) {
        lifecycleScope.launch {
            resultText.text = "🔍 Analyzing damage..."

            val result = damageHelper.assessDamageFromBitmap(bitmap)

            severityText.text = "Severity: ${result.severity}"

            val severityColor = when (result.severity) {
                "SEVERE" -> android.graphics.Color.RED
                "MODERATE" -> android.graphics.Color.parseColor("#FF6B00")
                "MINOR" -> android.graphics.Color.parseColor("#FFA500")
                "NO_DAMAGE" -> android.graphics.Color.GREEN
                else -> android.graphics.Color.GRAY
            }
            severityText.setTextColor(severityColor)

            resultText.text = result.description

            detailsText.text = if (result.detectedDamageTypes.isNotEmpty()) {
                "Detected: ${result.detectedDamageTypes.joinToString(", ")}\n" +
                "Confidence: ${(result.confidence * 100).toInt()}%"
            } else {
                "No damage indicators detected in image"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        damageHelper.cleanup()
    }
}

