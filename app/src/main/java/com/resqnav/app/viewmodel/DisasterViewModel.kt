package com.resqnav.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.resqnav.app.database.AppDatabase
import com.resqnav.app.model.DisasterAlert
import com.resqnav.app.repository.DisasterRepository
import kotlinx.coroutines.launch

class DisasterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DisasterRepository
    val allActiveAlerts: LiveData<List<DisasterAlert>>

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        val disasterDao = AppDatabase.getDatabase(application).disasterDao()
        repository = DisasterRepository(disasterDao)
        allActiveAlerts = repository.allActiveAlerts
    }

    fun insert(alert: DisasterAlert) = viewModelScope.launch {
        repository.insert(alert)
    }

    fun update(alert: DisasterAlert) = viewModelScope.launch {
        repository.update(alert)
    }

    fun delete(alert: DisasterAlert) = viewModelScope.launch {
        repository.delete(alert)
    }

    fun getAlertsBySeverity(severity: String): LiveData<List<DisasterAlert>> {
        return repository.getAlertsBySeverity(severity)
    }

    /**
     * Fetch all disaster types from multiple APIs and save to database
     */
    fun refreshAllDisasters() = viewModelScope.launch {
        _isLoading.value = true
        _errorMessage.value = null

        try {
            Log.d("DisasterViewModel", "Starting to fetch all disaster types...")
            val disasters = repository.fetchAllDisasters()

            if (disasters.isNotEmpty()) {
                Log.d("DisasterViewModel", "Fetched ${disasters.size} total disasters")

                // Group by type for logging
                val grouped = disasters.groupBy { it.type }
                grouped.forEach { (type, alerts) ->
                    Log.d("DisasterViewModel", "  - ${type}: ${alerts.size} events")
                }

                // Save all disasters to database
                repository.insertAll(disasters)
                Log.d("DisasterViewModel", "Successfully saved disasters to database")
            } else {
                _errorMessage.value = "No disasters found from APIs"
                Log.w("DisasterViewModel", "No disasters returned from APIs")
            }
        } catch (e: Exception) {
            _errorMessage.value = "Error fetching disasters: ${e.message}"
            Log.e("DisasterViewModel", "Error refreshing disasters", e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Clear old inactive alerts (older than 7 days)
     */
    fun cleanupOldAlerts() = viewModelScope.launch {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        repository.deleteOldInactiveAlerts(sevenDaysAgo)
    }
}

