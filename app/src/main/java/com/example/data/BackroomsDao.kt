package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BackroomsDao {

    @Query("SELECT * FROM anomaly_discoveries ORDER BY firstDiscoveredAt DESC")
    fun observeDiscoveries(): Flow<List<AnomalyDiscoveryEntity>>

    @Query("SELECT * FROM anomaly_discoveries WHERE anomalyId = :anomalyId LIMIT 1")
    suspend fun getDiscoveryById(anomalyId: String): AnomalyDiscoveryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDiscovery(entity: AnomalyDiscoveryEntity)

    @Query("SELECT * FROM run_records ORDER BY distanceMeters DESC, anomaliesFound DESC")
    fun observeRunsByBestDistance(): Flow<List<RunRecordEntity>>

    @Query("SELECT * FROM run_records ORDER BY timestamp DESC")
    fun observeRunsByRecent(): Flow<List<RunRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(record: RunRecordEntity)

    @Query("DELETE FROM run_records")
    suspend fun clearRuns()
}
