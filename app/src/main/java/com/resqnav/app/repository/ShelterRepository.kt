package com.resqnav.app.repository

import androidx.lifecycle.LiveData
import com.resqnav.app.database.ShelterDao
import com.resqnav.app.model.Shelter

class ShelterRepository(private val shelterDao: ShelterDao) {

    val allOperationalShelters: LiveData<List<Shelter>> = shelterDao.getAllOperationalShelters()
    val allShelters: LiveData<List<Shelter>> = shelterDao.getAllShelters()

    suspend fun insert(shelter: Shelter) {
        shelterDao.insert(shelter)
    }

    suspend fun insertAll(shelters: List<Shelter>) {
        shelterDao.insertAll(shelters)
    }

    suspend fun update(shelter: Shelter) {
        shelterDao.update(shelter)
    }

    suspend fun delete(shelter: Shelter) {
        shelterDao.delete(shelter)
    }

    fun getSheltersInArea(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): LiveData<List<Shelter>> {
        return shelterDao.getSheltersInArea(minLat, maxLat, minLon, maxLon)
    }

    suspend fun getShelterById(shelterId: Int): Shelter? {
        return shelterDao.getShelterById(shelterId)
    }
}

