package com.resqnav.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private val TAG = "ProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Set up the toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Profile"

        val user = FirebaseAuth.getInstance().currentUser

        // Display user information
        findViewById<TextView>(R.id.tv_user_name).text = user?.displayName ?: "Not provided"
        findViewById<TextView>(R.id.tv_user_email).text = user?.email ?: "Not provided"
        findViewById<TextView>(R.id.tv_user_phone).text = user?.phoneNumber ?: "Not provided"
        findViewById<TextView>(R.id.tv_user_id).text = user?.uid ?: "Unknown"

        // Logout button
        findViewById<Button>(R.id.btn_logout).setOnClickListener {
            logout()
        }

        // Delete account button
        findViewById<Button>(R.id.btn_delete_account).setOnClickListener {
            showDeleteAccountDialog()
        }

        // Check if user just logged in to complete account deletion
        checkPendingDeletion()
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("⚠️ This will permanently delete your Firebase account.\n\n" +
                    "Note: Your local data (alerts, shelters, preferences) will remain on your device. " +
                    "You can clear app data from your device settings if needed.\n\n" +
                    "This action CANNOT be undone!")
            .setPositiveButton("Delete Account") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun deleteAccount() {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Show progress
        Toast.makeText(this, "Deleting your account...", Toast.LENGTH_SHORT).show()

        // Delete Firebase Auth account
        user.delete()
            .addOnSuccessListener {
                Log.d(TAG, "✅ Firebase account deleted successfully")
                Toast.makeText(this, "✅ Account deleted successfully", Toast.LENGTH_LONG).show()

                // Navigate to login screen
                navigateToLogin()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to delete Firebase account: ${e.message}", e)

                // Check if it's a re-authentication error
                if (e.message?.contains("requires recent authentication") == true) {
                    // Re-authenticate and try again
                    Toast.makeText(this, "Please log in again to delete your account", Toast.LENGTH_LONG).show()
                    reauthenticateAndDelete()
                } else {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun reauthenticateAndDelete() {
        // Store deletion intent in SharedPreferences
        val prefs = getSharedPreferences("ResQNavPrefs", MODE_PRIVATE)
        prefs.edit().putBoolean("pending_account_deletion", true).apply()

        // Sign out and go to login
        FirebaseAuth.getInstance().signOut()
        navigateToLogin()
    }

    private fun checkPendingDeletion() {
        val prefs = getSharedPreferences("ResQNavPrefs", MODE_PRIVATE)
        val pendingDeletion = prefs.getBoolean("pending_account_deletion", false)

        if (pendingDeletion) {
            // Clear the flag
            prefs.edit().putBoolean("pending_account_deletion", false).apply()

            // Show confirmation dialog
            AlertDialog.Builder(this)
                .setTitle("Confirm Account Deletion")
                .setMessage("Are you sure you want to delete your Firebase account?")
                .setPositiveButton("Yes, Delete Account") { _, _ ->
                    deleteAccount()
                }
                .setNegativeButton("Cancel", null)
                .setCancelable(false)
                .show()
        }
    }

    private fun logout() {
        try {
            AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener {
                    navigateToLogin()
                }
                .addOnFailureListener { e ->
                    // Even if signout fails, navigate to login
                    Log.e("ProfileActivity", "Logout error", e)
                    navigateToLogin()
                }
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Logout exception", e)
            // Force logout by navigating to login anyway
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        try {
            val intent = Intent(this, com.resqnav.app.auth.CustomLoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Navigate error", e)
            // Last resort - just finish
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

