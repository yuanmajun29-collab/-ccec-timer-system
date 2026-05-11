package com.ccec.timer.stationkiosk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snapshot_cache")
data class CachedSnapshot(
    @PrimaryKey val stationCode: String,
    val jsonPayload: String,
    val updatedAtMillis: Long
)
