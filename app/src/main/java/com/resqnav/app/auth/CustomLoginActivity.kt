package com.resqnav.app.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.resqnav.app.MainActivity
import com.resqnav.app.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class CustomLoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnSignup: MaterialButton
    private lateinit var btnPhone: MaterialButton
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        try {
            installSplashScreen()
        } catch (e: Exception) {
            // Continue without splash screen if it fails
        }

        super.onCreate(savedInstanceState)

        // Check if user is already logged in
        try {
            auth = FirebaseAuth.getInstance()
            if (auth.currentUser != null) {
                navigateToMain()
                return
            }
        } catch (e: Exception) {
            // If Firebase isn't initialized, show error and continue
            Toast.makeText(this, "Initializing app...", Toast.LENGTH_SHORT).show()
        }

        setContentView(R.layout.activity_login_custom)

        // Initialize views with null safety
        try {
            emailInput = findViewById(R.id.email_input)
            passwordInput = findViewById(R.id.password_input)
            btnLogin = findViewById(R.id.btn_login)
            btnSignup = findViewById(R.id.btn_signup)
            btnPhone = findViewById(R.id.btn_phone)
            progressBar = findViewById(R.id.progress_bar)

            // Set up click listeners
            btnLogin.setOnClickListener { signIn() }
            btnSignup.setOnClickListener { signUp() }
            btnPhone.setOnClickListener { phoneSignIn() }
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing login screen", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun signIn() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (email.isEmpty()) {
            emailInput.error = "Email is required"
            return
        }

        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            return
        }

        if (password.length < 6) {
            passwordInput.error = "Password must be at least 6 characters"
            return
        }

        showLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Toast.makeText(
                        this,
                        "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun signUp() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (email.isEmpty()) {
            emailInput.error = "Email is required"
            return
        }

        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            return
        }

        if (password.length < 6) {
            passwordInput.error = "Password must be at least 6 characters"
            return
        }

        showLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Toast.makeText(
                        this,
                        "Sign up failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun phoneSignIn() {
        Toast.makeText(this, "Phone sign-in coming soon!", Toast.LENGTH_SHORT).show()
        // TODO: Implement phone authentication
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
        btnSignup.isEnabled = !show
        btnPhone.isEnabled = !show
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

