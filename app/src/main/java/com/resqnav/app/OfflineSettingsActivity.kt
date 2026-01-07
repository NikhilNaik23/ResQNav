package com.resqnav.app

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.resqnav.app.offline.OfflineDataManager
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class OfflineSettingsActivity : AppCompatActivity() {

    private lateinit var offlineDataManager: OfflineDataManager
    private lateinit var switchOfflineMode: SwitchMaterial
    private lateinit var btnDownloadData: Button
    private lateinit var btnClearCache: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvLastSync: TextView
    private lateinit var tvCacheSize: TextView
    private lateinit var tvNetworkStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_settings)

        // Initialize offline data manager
        offlineDataManager = OfflineDataManager(this)

        // Setup toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Offline Mode Settings"

        // Initialize views
        switchOfflineMode = findViewById(R.id.switch_offline_mode)
        btnDownloadData = findViewById(R.id.btn_download_data)
        btnClearCache = findViewById(R.id.btn_clear_cache)
        progressBar = findViewById(R.id.progress_download)
        tvLastSync = findViewById(R.id.tv_last_sync)
        tvCacheSize = findViewById(R.id.tv_cache_size)
        tvNetworkStatus = findViewById(R.id.tv_network_status)

        setupUI()
        updateStatus()
    }

    private fun setupUI() {
        // Offline mode switch
        switchOfflineMode.isChecked = offlineDataManager.isOfflineModeEnabled()
        switchOfflineMode.setOnCheckedChangeListener { _, isChecked ->
            offlineDataManager.setOfflineMode(isChecked)
            Toast.makeText(
                this,
                if (isChecked) "Offline mode enabled" else "Offline mode disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Download data button
        btnDownloadData.setOnClickListener {
            downloadOfflineData()
        }

        // Clear cache button
        btnClearCache.setOnClickListener {
            clearCache()
        }
    }

    private fun updateStatus() {
        // Network status
        val isOnline = offlineDataManager.isNetworkAvailable()
        tvNetworkStatus.text = if (isOnline) "✅ Online" else "📵 Offline"
        tvNetworkStatus.setTextColor(
            if (isOnline) getColor(android.R.color.holo_green_dark)
            else getColor(android.R.color.holo_red_dark)
        )

        // Last sync time
        val lastSync = offlineDataManager.getLastSyncTime()
        if (lastSync > 0) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            tvLastSync.text = "Last synced: ${dateFormat.format(Date(lastSync))}"
        } else {
            tvLastSync.text = "Never synced"
        }

        // Cache size
        val cacheSize = offlineDataManager.getCacheSize()
        tvCacheSize.text = "Cache size: ${"%.2f".format(cacheSize)} MB"

        // Enable/disable download button based on network
        btnDownloadData.isEnabled = isOnline
    }

    private fun downloadOfflineData() {
        progressBar.visibility = View.VISIBLE
        btnDownloadData.isEnabled = false

        lifecycleScope.launch {
            offlineDataManager.downloadOfflineData(
                onProgress = { progress ->
                    runOnUiThread {
                        progressBar.progress = progress
                    }
                },
                onComplete = { success ->
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        btnDownloadData.isEnabled = true

                        if (success) {
                            Toast.makeText(
                                this@OfflineSettingsActivity,
                                "✅ Offline data downloaded successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            updateStatus()
                        } else {
                            Toast.makeText(
                                this@OfflineSettingsActivity,
                                "❌ Failed to download offline data",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }
    }

    private fun clearCache() {
        lifecycleScope.launch {
            offlineDataManager.clearCache()
            runOnUiThread {
                Toast.makeText(
                    this@OfflineSettingsActivity,
                    "Cache cleared successfully",
                    Toast.LENGTH_SHORT
                ).show()
                updateStatus()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

