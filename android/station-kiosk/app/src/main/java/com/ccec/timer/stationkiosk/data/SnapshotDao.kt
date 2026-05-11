package com.ccec.timer.stationkiosk.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: CachedSnapshot)

    @Query("SELECT * FROM snapshot_cache WHERE stationCode = :code LIMIT 1")
    suspend fun getByStation(code: String): CachedSnapshot?

    @Query("DELETE FROM snapshot_cache")
    suspend fun clearAll()
}
