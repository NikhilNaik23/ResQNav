package com.resqnav.app.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.resqnav.app.R
import com.google.android.material.card.MaterialCardView

class AIHubFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ai_hub, container, false)

        // Set up click listeners for each AI feature card
        view.findViewById<MaterialCardView>(R.id.cardAIChatbot).setOnClickListener {
            navigateToFragment(AIChatbotFragment())
        }

        view.findViewById<MaterialCardView>(R.id.cardVoiceSOS).setOnClickListener {
            navigateToFragment(VoiceSOSFragment())
        }

        view.findViewById<MaterialCardView>(R.id.cardSmartRoute).setOnClickListener {
            navigateToFragment(SmartRouteFragment())
        }

        view.findViewById<MaterialCardView>(R.id.cardDisasterPrediction).setOnClickListener {
            navigateToFragment(DisasterPredictionFragment())
        }

        view.findViewById<MaterialCardView>(R.id.cardDamageAssessment).setOnClickListener {
            navigateToFragment(DamageAssessmentFragment())
        }

        return view
    }

    private fun navigateToFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}

