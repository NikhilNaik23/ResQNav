package com.resqnav.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.resqnav.app.database.AppDatabase
import com.resqnav.app.model.Shelter
import com.resqnav.app.repository.ShelterRepository
import kotlinx.coroutines.launch

class ShelterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ShelterRepository
    val allOperationalShelters: LiveData<List<Shelter>>

    init {
        val shelterDao = AppDatabase.getDatabase(application).shelterDao()
        repository = ShelterRepository(shelterDao)
        allOperationalShelters = repository.allOperationalShelters
    }

    fun insert(shelter: Shelter) = viewModelScope.launch {
        repository.insert(shelter)
    }

    fun update(shelter: Shelter) = viewModelScope.launch {
        repository.update(shelter)
    }

    fun delete(shelter: Shelter) = viewModelScope.launch {
        repository.delete(shelter)
    }

    fun getSheltersInArea(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): LiveData<List<Shelter>> {
        return repository.getSheltersInArea(minLat, maxLat, minLon, maxLon)
    }
}

