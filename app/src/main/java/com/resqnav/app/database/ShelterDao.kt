package com.resqnav.app.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.resqnav.app.model.Shelter

@Dao
interface ShelterDao {
    @Query("SELECT * FROM shelters WHERE isOperational = 1 ORDER BY name ASC")
    fun getAllOperationalShelters(): LiveData<List<Shelter>>

    @Query("SELECT * FROM shelters ORDER BY name ASC")
    fun getAllShelters(): LiveData<List<Shelter>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shelter: Shelter)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shelters: List<Shelter>)

    @Update
    suspend fun update(shelter: Shelter)

    @Delete
    suspend fun delete(shelter: Shelter)

    @Query("SELECT * FROM shelters WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLon AND :maxLon AND isOperational = 1")
    fun getSheltersInArea(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): LiveData<List<Shelter>>

    @Query("SELECT * FROM shelters WHERE id = :shelterId")
    suspend fun getShelterById(shelterId: Int): Shelter?
}

