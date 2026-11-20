package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlin.math.max

class BackroomsRepository(private val dao: BackroomsDao) {

    val discoveries: Flow<List<AnomalyDiscoveryEntity>> = dao.observeDiscoveries()
    val runsByDistance: Flow<List<RunRecordEntity>> = dao.observeRunsByBestDistance()
    val runsByRecent: Flow<List<RunRecordEntity>> = dao.observeRunsByRecent()

    /**
     * Records an anomaly sighting and returns true if this is the player's first-ever
     * discovery of this anomaly type.
     */
    suspend fun recordAnomalySighting(anomalyId: String, distortionTier: Int): Boolean {
        val existing = dao.getDiscoveryById(anomalyId)
        return if (existing == null) {
            dao.upsertDiscovery(
                AnomalyDiscoveryEntity(
                    anomalyId = anomalyId,
                    timesSpotted = 1,
                    firstDiscoveredAt = System.currentTimeMillis(),
                    bestDistortionTierSeen = distortionTier
                )
            )
            true
        } else {
            dao.upsertDiscovery(
                existing.copy(
                    timesSpotted = existing.timesSpotted + 1,
                    bestDistortionTierSeen = max(existing.bestDistortionTierSeen, distortionTier)
                )
            )
            false
        }
    }

    suspend fun saveRun(record: RunRecordEntity) {
        dao.insertRun(record)
    }

    suspend fun clearRunArchive() {
        dao.clearRuns()
    }
}
