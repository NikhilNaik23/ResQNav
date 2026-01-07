package com.resqnav.app.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.resqnav.app.model.DisasterAlert

@Dao
interface DisasterDao {
    @Query("SELECT * FROM disaster_alerts WHERE isActive = 1 ORDER BY timestamp DESC")
    fun getAllActiveAlerts(): LiveData<List<DisasterAlert>>

    @Query("SELECT * FROM disaster_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): LiveData<List<DisasterAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: DisasterAlert)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alerts: List<DisasterAlert>)

    @Update
    suspend fun update(alert: DisasterAlert)

    @Delete
    suspend fun delete(alert: DisasterAlert)

    @Query("DELETE FROM disaster_alerts WHERE isActive = 0 AND timestamp < :timestamp")
    suspend fun deleteOldInactiveAlerts(timestamp: Long)

    @Query("SELECT * FROM disaster_alerts WHERE severity = :severity AND isActive = 1")
    fun getAlertsBySeverity(severity: String): LiveData<List<DisasterAlert>>
}

