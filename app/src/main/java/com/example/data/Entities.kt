package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anomaly_discoveries")
data class AnomalyDiscoveryEntity(
    @PrimaryKey val anomalyId: String,
    val timesSpotted: Int = 1,
    val firstDiscoveredAt: Long = System.currentTimeMillis(),
    val bestDistortionTierSeen: Int = 1
)

@Entity(tableName = "run_records")
data class RunRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val distanceMeters: Float,
    val anomaliesFound: Int,
    val falsePositives: Int,
    val peakDistortionTier: Int,
    val durationSeconds: Int
)
