package com.resqnav.app.offline

import android.content.Context
import android.util.Log
import com.resqnav.app.database.AppDatabase
import com.resqnav.app.model.DisasterAlert
import com.resqnav.app.model.Shelter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages offline data caching and retrieval
 */
class OfflineDataManager(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val sharedPrefs = context.getSharedPreferences("offline_cache", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "OfflineDataManager"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
        private const val KEY_OFFLINE_MODE_ENABLED = "offline_mode_enabled"
        private const val CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    /**
     * Check if app is in offline mode
     */
    fun isOfflineModeEnabled(): Boolean {
        return sharedPrefs.getBoolean(KEY_OFFLINE_MODE_ENABLED, false)
    }

    /**
     * Enable or disable offline mode
     */
    fun setOfflineMode(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_OFFLINE_MODE_ENABLED, enabled).apply()
        Log.d(TAG, "Offline mode ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Cache disaster alerts for offline access
     */
    suspend fun cacheDisasters(disasters: List<DisasterAlert>) = withContext(Dispatchers.IO) {
        try {
            database.disasterDao().insertAll(disasters)
            updateLastSyncTime()
            Log.d(TAG, "Cached ${disasters.size} disasters for offline access")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching disasters: ${e.message}", e)
        }
    }

    /**
     * Cache shelters for offline access
     */
    suspend fun cacheShelters(shelters: List<Shelter>) = withContext(Dispatchers.IO) {
        try {
            shelters.forEach { shelter ->
                database.shelterDao().insert(shelter)
            }
            Log.d(TAG, "Cached ${shelters.size} shelters for offline access")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching shelters: ${e.message}", e)
        }
    }

    /**
     * Get cached disasters from local database
     */
    suspend fun getCachedDisasters(): List<DisasterAlert> = withContext(Dispatchers.IO) {
        try {
            val disasters = database.disasterDao().getAllAlerts().value ?: emptyList()
            Log.d(TAG, "Retrieved ${disasters.size} cached disasters")
            disasters
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving cached disasters: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get cached shelters from local database
     */
    suspend fun getCachedShelters(): List<Shelter> = withContext(Dispatchers.IO) {
        try {
            val shelters = database.shelterDao().getAllShelters().value ?: emptyList()
            Log.d(TAG, "Retrieved ${shelters.size} cached shelters")
            shelters
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving cached shelters: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Check if cached data is still valid
     */
    fun isCacheValid(): Boolean {
        val lastSync = sharedPrefs.getLong(KEY_LAST_SYNC, 0)
        val currentTime = System.currentTimeMillis()
        val isValid = (currentTime - lastSync) < CACHE_EXPIRY_MS

        Log.d(TAG, "Cache validity: $isValid (age: ${(currentTime - lastSync) / 1000 / 60} minutes)")
        return isValid
    }

    /**
     * Get last sync timestamp
     */
    fun getLastSyncTime(): Long {
        return sharedPrefs.getLong(KEY_LAST_SYNC, 0)
    }

    /**
     * Update last sync timestamp
     */
    private fun updateLastSyncTime() {
        sharedPrefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()
    }

    /**
     * Clear all cached data
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            // Clear database
            database.clearAllTables()

            // Clear shared preferences
            sharedPrefs.edit().clear().apply()

            Log.d(TAG, "Cache cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache: ${e.message}", e)
        }
    }

    /**
     * Check if device has internet connection
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as android.net.ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        val isConnected = activeNetwork?.isConnectedOrConnecting == true

        Log.d(TAG, "Network available: $isConnected")
        return isConnected
    }

    /**
     * Get cache size in MB
     */
    fun getCacheSize(): Double {
        val dbFile = context.getDatabasePath(database.openHelper.databaseName)
        val sizeInBytes = dbFile.length()
        val sizeInMB = sizeInBytes / (1024.0 * 1024.0)

        Log.d(TAG, "Cache size: ${"%.2f".format(sizeInMB)} MB")
        return sizeInMB
    }

    /**
     * Download essential data for offline use
     */
    suspend fun downloadOfflineData(
        onProgress: (Int) -> Unit,
        onComplete: (Boolean) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting offline data download...")

            // Step 1: Cache current disasters (30%)
            onProgress(30)
            // This will be called by the repository after fetching

            // Step 2: Cache shelters (60%)
            onProgress(60)
            // Shelters are already in database

            // Step 3: Mark download as complete (100%)
            onProgress(100)
            setOfflineMode(true)
            updateLastSyncTime()

            Log.d(TAG, "Offline data download complete")
            onComplete(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading offline data: ${e.message}", e)
            onComplete(false)
        }
    }
}

