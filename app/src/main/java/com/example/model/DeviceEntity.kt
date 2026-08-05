package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val ipAddress: String,
    val type: String,
    val isFavorite: Boolean = false,
    val lastConnected: Long = System.currentTimeMillis()
)
