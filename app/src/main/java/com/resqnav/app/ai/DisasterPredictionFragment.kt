package com.resqnav.app.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.resqnav.app.R
import kotlinx.coroutines.launch

class DisasterPredictionFragment : Fragment() {

    private lateinit var predictionHelper: DisasterPredictionHelper
    private lateinit var resultText: TextView
    private lateinit var riskScoreText: TextView
    private lateinit var recommendationText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_disaster_prediction, container, false)

        predictionHelper = DisasterPredictionHelper(requireContext())

        resultText = view.findViewById(R.id.resultText)
        riskScoreText = view.findViewById(R.id.riskScoreText)
        recommendationText = view.findViewById(R.id.recommendationText)

        val tempInput = view.findViewById<EditText>(R.id.tempInput)
        val humidityInput = view.findViewById<EditText>(R.id.humidityInput)
        val windInput = view.findViewById<EditText>(R.id.windInput)
        val rainInput = view.findViewById<EditText>(R.id.rainInput)

        view.findViewById<Button>(R.id.btnPredict).setOnClickListener {
            val temp = tempInput.text.toString().toFloatOrNull() ?: 25f
            val humidity = humidityInput.text.toString().toFloatOrNull() ?: 50f
            val wind = windInput.text.toString().toFloatOrNull() ?: 10f
            val rain = rainInput.text.toString().toFloatOrNull() ?: 0f

            predictRisk(temp, humidity, wind, rain)
        }

        // Set sample data
        tempInput.setText("38")
        humidityInput.setText("25")
        windInput.setText("25")
        rainInput.setText("5")

        return view
    }

    private fun predictRisk(temp: Float, humidity: Float, wind: Float, rain: Float) {
        lifecycleScope.launch {
            val result = predictionHelper.predictDisasterRisk(temp, humidity, wind, rain)

            val riskPercentage = (result.riskScore * 100).toInt()
            riskScoreText.text = "Risk Level: $riskPercentage%"

            val scoreColor = when (result.severity) {
                "CRITICAL" -> android.graphics.Color.RED
                "HIGH" -> android.graphics.Color.parseColor("#FF6B00")
                "MODERATE" -> android.graphics.Color.parseColor("#FFA500")
                else -> android.graphics.Color.GREEN
            }
            riskScoreText.setTextColor(scoreColor)

            resultText.text = if (result.predictedDisasters.isNotEmpty()) {
                "⚠️ ${result.severity} RISK\n\nPredicted Disasters:\n• ${result.predictedDisasters.joinToString("\n• ")}"
            } else {
                "✅ ${result.severity} RISK\n\nNo immediate disaster threats detected"
            }

            recommendationText.text = result.recommendation
        }
    }
}

