package com.resqnav.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shelters")
data class Shelter(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val capacity: Int,
    val currentOccupancy: Int = 0,
    val contactNumber: String,
    val facilities: String, // comma-separated: medical, food, water, electricity
    val type: String, // government, private, community
    val isOperational: Boolean = true
)

