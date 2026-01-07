package com.resqnav.app.ai

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.resqnav.app.R
import com.resqnav.app.utils.GroqConfig
import com.resqnav.app.api.GroqApiClient
import com.resqnav.app.api.GroqChatRequest
import com.resqnav.app.api.ChatMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AIChatbotFragment : Fragment() {
    private lateinit var chatInput: EditText
    private lateinit var chatOutput: TextView
    private lateinit var sendButton: Button
    private lateinit var scrollView: ScrollView
    private lateinit var clearButton: Button

    private val TAG = "AIChatbot"

    private val chatHistory = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ai_chatbot, container, false)

        chatInput = view.findViewById(R.id.chatInput)
        chatOutput = view.findViewById(R.id.chatOutput)
        sendButton = view.findViewById(R.id.sendButton)
        scrollView = view.findViewById(R.id.chatScrollView)
        clearButton = view.findViewById(R.id.clearButton)

        // Initial welcome message
        addMessageToChat("🤖 AI Assistant", "Hello! I'm your emergency response assistant powered by Groq AI (LLama 3.1 70B - FREE & FAST). I can help you with:\n\n" +
                "• Emergency procedures\n" +
                "• First aid guidance\n" +
                "• Evacuation tips\n" +
                "• Safety information\n" +
                "• Disaster preparedness\n\n" +
                "Ask me anything about emergencies!")

        sendButton.setOnClickListener {
            val userMessage = chatInput.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                askAI(userMessage)
                chatInput.text.clear()
            }
        }

        clearButton.setOnClickListener {
            chatHistory.clear()
            chatOutput.text = ""
            addMessageToChat("🤖 AI Assistant", "Chat cleared. How can I help you?")
        }

        // Quick action buttons
        view.findViewById<Button>(R.id.btnFirstAid).setOnClickListener {
            askAI("What are basic first aid steps during an emergency?")
        }

        view.findViewById<Button>(R.id.btnEvacuation).setOnClickListener {
            askAI("What should I do during an evacuation?")
        }

        view.findViewById<Button>(R.id.btnEarthquake).setOnClickListener {
            askAI("What should I do during an earthquake?")
        }

        view.findViewById<Button>(R.id.btnFlood).setOnClickListener {
            askAI("How to stay safe during a flood?")
        }

        return view
    }

    private fun askAI(userMessage: String) {
        // Add user message to chat
        addMessageToChat("👤 You", userMessage)

        // Show loading
        sendButton.isEnabled = false
        sendButton.text = "Thinking..."

        lifecycleScope.launch {
            var lastException: Exception? = null
            var retryCount = 0
            val maxRetries = 2

            while (retryCount <= maxRetries) {
                try {
                    Log.d(TAG, "Sending request to Groq API (Attempt ${retryCount + 1}/${maxRetries + 1})...")
                    Log.d(TAG, "Using model: ${GroqConfig.MODEL_NAME}")

                    val request = GroqChatRequest(
                        model = GroqConfig.MODEL_NAME,
                        messages = listOf(
                            ChatMessage(
                                role = "system",
                                content = "You are an expert emergency response AI assistant. Provide clear, concise, and actionable safety advice for emergency situations. Keep responses brief (2-4 sentences) and focused on immediate safety steps."
                            ),
                            ChatMessage(
                                role = "user",
                                content = userMessage
                            )
                        ),
                        max_tokens = GroqConfig.MAX_TOKENS,
                        temperature = GroqConfig.TEMPERATURE
                    )

                    val response = withContext(Dispatchers.IO) {
                        GroqApiClient.apiService.createChatCompletion(request)
                    }

                    val aiResponse = response.choices.firstOrNull()?.message?.content?.trim()
                        ?: "Sorry, I couldn't generate a response."

                    Log.d(TAG, "Received response from Groq API")
                    Log.d(TAG, "Tokens used: ${response.usage?.total_tokens ?: 0}")

                    addMessageToChat("🤖 AI Assistant", aiResponse)

                    sendButton.isEnabled = true
                    sendButton.text = "Send"
                    return@launch // Success - exit

                } catch (e: Exception) {
                    Log.e(TAG, "Groq API Error (Attempt ${retryCount + 1}): ${e.message}", e)
                    lastException = e

                    retryCount++
                    if (retryCount <= maxRetries) {
                        Log.d(TAG, "Retrying in 2 seconds...")
                        delay(2000)
                    }
                }
            }

            // All retries failed - show error
            val errorMessage = lastException?.let { e ->
                when {
                    e.message?.contains("401") == true || e.message?.contains("Unauthorized") == true ->
                        "❌ Invalid API Key\n\nPlease add your FREE Groq API key in GroqConfig.kt\n\nGet it from: https://console.groq.com/keys\n\n(Sign up is 100% free!)"
                    e.message?.contains("429") == true ->
                        "❌ Rate Limited\n\nYou've exceeded the free tier rate limit. Wait a few minutes and try again."
                    e.message?.contains("400") == true ->
                        "❌ Bad Request\n\nThere was an issue with the request format. Error: ${e.message}"
                    e.message?.contains("timeout") == true ->
                        "❌ Request Timeout\n\nThe request took too long. Check your internet connection and try again."
                    e.message?.contains("UnknownHostException") == true ->
                        "❌ Network Error\n\nCannot connect to Groq API. Check your internet connection."
                    else ->
                        "❌ Error: ${e.javaClass.simpleName}\n\nDetails: ${e.message}\n\nMake sure your API key is set in GroqConfig.kt"
                }
            } ?: "❌ Unknown error occurred. Please try again."

            addMessageToChat("❌ Error", errorMessage)
            Log.e(TAG, "Failed to get AI response after $maxRetries retries", lastException)

            sendButton.isEnabled = true
            sendButton.text = "Send"
        }
    }

    private fun addMessageToChat(sender: String, message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val formattedMessage = "\n[$timestamp] $sender:\n$message\n${"-".repeat(50)}\n"

        chatHistory.add(formattedMessage)
        chatOutput.append(formattedMessage)

        // Auto-scroll to bottom
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }
}
