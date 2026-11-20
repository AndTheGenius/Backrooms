package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AnomalyDiscoveryEntity
import com.example.data.BackroomsRepository
import com.example.data.RunRecordEntity
import com.example.engine.AimTarget
import com.example.engine.AnomalyCatalog
import com.example.engine.BackroomsBiome
import com.example.engine.BackroomsWorld
import com.example.engine.DistortionStage
import com.example.engine.WorldProp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RunPhase {
    PLAYING,
    PAUSED,
    SUMMARY
}

enum class BannerTone {
    CONFIRM,
    ERROR,
    WARNING,
    INFO
}

data class HudBanner(
    val title: String,
    val detail: String,
    val tone: BannerTone,
    val id: Long = System.nanoTime()
)

data class CareerStats(
    val bestDistanceMeters: Float = 0f,
    val totalRuns: Int = 0,
    val discoveredCount: Int = 0,
    val totalCatalogCount: Int = AnomalyCatalog.all.size,
    val highestTierReached: Int = 1
)

data class PlayHudState(
    val phase: RunPhase = RunPhase.PLAYING,
    val distanceMeters: Float = 0f,
    val elapsedSeconds: Float = 0f,
    val distortionStage: DistortionStage = AnomalyCatalog.stages.first(),
    val distortionIntensity: Float = 0f,
    val currentBiome: BackroomsBiome = BackroomsBiome.YELLOW_HALLS,
    val isZoomed: Boolean = false,
    val isMuted: Boolean = false,
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val lockedTargetDistance: Float? = null,
    val hasTargetInRange: Boolean = false,
    val anomaliesFound: Int = 0,
    val falsePositives: Int = 0,
    val peakTierReached: Int = 1,
    val newlyUnlockedIds: List<String> = emptyList(),
    val activeBanner: HudBanner? = null,
    val staticBurst: Float = 0f,
    val shiftCount: Int = 0
)

enum class HapticEvent {
    CONFIRM,
    ERROR,
    SHIFT
}

class BackroomsViewModel(
    private val repository: BackroomsRepository
) : ViewModel() {

    val discoveriesMap: StateFlow<Map<String, AnomalyDiscoveryEntity>> =
        repository.discoveries
            .map { list -> list.associateBy { it.anomalyId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val runsByDistance: StateFlow<List<RunRecordEntity>> =
        repository.runsByDistance
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val runsByRecent: StateFlow<List<RunRecordEntity>> =
        repository.runsByRecent
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val careerStats: StateFlow<CareerStats> = combine(
        repository.discoveries,
        repository.runsByDistance
    ) { discoveries, runs ->
        CareerStats(
            bestDistanceMeters = runs.maxOfOrNull { it.distanceMeters } ?: 0f,
            totalRuns = runs.size,
            discoveredCount = discoveries.size,
            totalCatalogCount = AnomalyCatalog.all.size,
            highestTierReached = runs.maxOfOrNull { it.peakDistortionTier } ?: 1
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CareerStats())

    private val _selectedBiome = MutableStateFlow(BackroomsBiome.YELLOW_HALLS)
    val selectedBiome: StateFlow<BackroomsBiome> = _selectedBiome.asStateFlow()

    private val _hudState = MutableStateFlow(PlayHudState())
    val hudState: StateFlow<PlayHudState> = _hudState.asStateFlow()

    var world: BackroomsWorld = BackroomsWorld()
        private set

    private var scanTargetId: Int? = null
    private var bannerTimer: Float = 0f
    private var runSaved: Boolean = false
    private var lastFootstepPhase: Int = 0

    fun selectStartingBiome(biome: BackroomsBiome) {
        _selectedBiome.value = biome
    }

    fun startNewRun(startingBiome: BackroomsBiome = _selectedBiome.value) {
        val currentMuted = _hudState.value.isMuted
        world = BackroomsWorld(System.currentTimeMillis(), startingBiome)
        scanTargetId = null
        bannerTimer = 4.5f
        runSaved = false
        lastFootstepPhase = 0
        _hudState.value = PlayHudState(
            phase = RunPhase.PLAYING,
            currentBiome = startingBiome,
            isMuted = currentMuted,
            activeBanner = HudBanner(
                title = startingBiome.entryTitle,
                detail = "${startingBiome.entryDetail} Walk through outer archways to cross into other environments.",
                tone = BannerTone.INFO
            )
        )
    }

    fun toggleZoom() {
        _hudState.update { it.copy(isZoomed = !it.isZoomed) }
    }

    fun toggleMute() {
        _hudState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun pauseRun() {
        _hudState.update {
            if (it.phase == RunPhase.PLAYING) it.copy(phase = RunPhase.PAUSED, isScanning = false, scanProgress = 0f)
            else it
        }
    }

    fun resumeRun() {
        _hudState.update {
            if (it.phase == RunPhase.PAUSED) it.copy(phase = RunPhase.PLAYING)
            else it
        }
    }

    fun ejectTape() {
        finishRun()
    }

    fun startScan(currentAim: AimTarget?) {
        val state = _hudState.value
        if (state.phase != RunPhase.PLAYING || state.isScanning) return
        if (currentAim == null) {
            showBanner(
                title = "NO OBJECT IN VIEWFINDER",
                detail = "Move closer and center an object inside the reticle.",
                tone = BannerTone.INFO,
                duration = 2.0f
            )
            return
        }
        scanTargetId = currentAim.prop.instanceId
        _hudState.update { it.copy(isScanning = true, scanProgress = 0.05f) }
    }

    fun cancelScan() {
        scanTargetId = null
        _hudState.update { it.copy(isScanning = false, scanProgress = 0f) }
    }

    /**
     * Advances simulation state one frame. Players can walk infinitely without any death or
     * signal drain — reality simply keeps growing weirder the further they go.
     */
    fun stepFrame(
        dt: Float,
        forwardInput: Float,
        strafeInput: Float,
        turnDelta: Float,
        currentAim: AimTarget?,
        onFootstep: () -> Unit,
        onHaptic: (HapticEvent) -> Unit
    ) {
        val state = _hudState.value
        if (state.phase != RunPhase.PLAYING) return

        val clampedDt = dt.coerceIn(0.001f, 0.05f)
        val geometryShifted = world.update(clampedDt, forwardInput, strafeInput, turnDelta)

        val footCycle = (world.bobPhase / Math.PI).toInt()
        if (footCycle != lastFootstepPhase) {
            lastFootstepPhase = footCycle
            onFootstep()
        }

        val distance = world.distanceWalkedMeters
        val newStage = AnomalyCatalog.stageForDistance(distance)
        val intensity = AnomalyCatalog.distortionIntensity(distance)
        val nowBiome = world.getPlayerBiome()

        var nextStatic = (state.staticBurst - clampedDt * 1.6f).coerceAtLeast(0f)
        var nextBanner = state.activeBanner
        var nextShiftCount = state.shiftCount

        if (bannerTimer > 0f) {
            bannerTimer -= clampedDt
            if (bannerTimer <= 0f) {
                nextBanner = null
            }
        }

        // Biome transition banner whenever crossing into any of the 8 environments
        if (nowBiome != state.currentBiome) {
            bannerTimer = 3.6f
            nextBanner = HudBanner(
                title = nowBiome.entryTitle,
                detail = nowBiome.entryDetail,
                tone = BannerTone.INFO
            )
        } else if (newStage.tier > state.distortionStage.tier) {
            nextStatic = 0.55f
            bannerTimer = 4.0f
            nextBanner = HudBanner(
                title = "${newStage.code} — ${newStage.title}",
                detail = newStage.summary,
                tone = BannerTone.WARNING
            )
            onHaptic(HapticEvent.SHIFT)
        } else if (geometryShifted) {
            nextShiftCount++
            nextStatic = maxOf(nextStatic, 0.30f)
            if (nextBanner == null) {
                bannerTimer = 2.8f
                nextBanner = HudBanner(
                    title = "SPATIAL ANOMALY DETECTED",
                    detail = "Corridor layout shifted behind camera.",
                    tone = BannerTone.WARNING
                )
            }
            onHaptic(HapticEvent.SHIFT)
        }

        var scanning = state.isScanning
        var scanProg = state.scanProgress
        if (scanning) {
            val aimProp = currentAim?.prop
            if (aimProp == null || aimProp.instanceId != scanTargetId) {
                scanning = false
                scanProg = 0f
                scanTargetId = null
            } else {
                scanProg += clampedDt / 0.85f
                if (scanProg >= 1f) {
                    scanning = false
                    scanProg = 0f
                    scanTargetId = null
                    val outcome = resolveScan(aimProp, newStage.tier)
                    nextStatic = maxOf(nextStatic, outcome.staticSpike)
                    nextBanner = outcome.banner
                    bannerTimer = 3.6f
                    onHaptic(outcome.haptic)
                }
            }
        }

        _hudState.update {
            it.copy(
                distanceMeters = distance,
                elapsedSeconds = it.elapsedSeconds + clampedDt,
                distortionStage = newStage,
                distortionIntensity = intensity,
                currentBiome = nowBiome,
                peakTierReached = maxOf(it.peakTierReached, newStage.tier),
                isScanning = scanning,
                scanProgress = scanProg,
                hasTargetInRange = currentAim != null,
                lockedTargetDistance = currentAim?.distance,
                activeBanner = nextBanner,
                staticBurst = nextStatic,
                shiftCount = nextShiftCount
            )
        }
    }

    private data class ScanOutcome(
        val staticSpike: Float,
        val banner: HudBanner,
        val haptic: HapticEvent
    )

    private fun resolveScan(prop: WorldProp, currentTier: Int): ScanOutcome {
        return if (prop.isAnomaly) {
            prop.scanned = true
            _hudState.update { it.copy(anomaliesFound = it.anomaliesFound + 1) }

            viewModelScope.launch {
                val isFirstDiscovery = repository.recordAnomalySighting(prop.spec.id, currentTier)
                if (isFirstDiscovery) {
                    _hudState.update { st ->
                        if (prop.spec.id in st.newlyUnlockedIds) st
                        else st.copy(newlyUnlockedIds = st.newlyUnlockedIds + prop.spec.id)
                    }
                }
            }

            ScanOutcome(
                staticSpike = 0.22f,
                banner = HudBanner(
                    title = "ANOMALY LOGGED: ${prop.spec.anomalyTitle.uppercase()}",
                    detail = prop.spec.anomalyTell,
                    tone = BannerTone.CONFIRM
                ),
                haptic = HapticEvent.CONFIRM
            )
        } else {
            prop.scanned = true
            _hudState.update { it.copy(falsePositives = it.falsePositives + 1) }
            ScanOutcome(
                staticSpike = 0.65f,
                banner = HudBanner(
                    title = "NORMAL OBJECT — ${prop.spec.normalTitle.uppercase()}",
                    detail = "No anomaly detected on this object. Logged as false positive.",
                    tone = BannerTone.ERROR
                ),
                haptic = HapticEvent.ERROR
            )
        }
    }

    private fun showBanner(title: String, detail: String, tone: BannerTone, duration: Float) {
        bannerTimer = duration
        _hudState.update {
            it.copy(activeBanner = HudBanner(title = title, detail = detail, tone = tone))
        }
    }

    private fun finishRun() {
        val st = _hudState.value
        if (st.phase == RunPhase.SUMMARY) return
        _hudState.update {
            it.copy(
                phase = RunPhase.SUMMARY,
                isScanning = false,
                scanProgress = 0f
            )
        }

        if (!runSaved && (st.distanceMeters >= 1f || st.anomaliesFound > 0 || st.elapsedSeconds >= 3f)) {
            runSaved = true
            viewModelScope.launch {
                repository.saveRun(
                    RunRecordEntity(
                        distanceMeters = st.distanceMeters,
                        anomaliesFound = st.anomaliesFound,
                        falsePositives = st.falsePositives,
                        peakDistortionTier = st.peakTierReached,
                        durationSeconds = st.elapsedSeconds.toInt().coerceAtLeast(1)
                    )
                )
            }
        }
    }

    fun clearArchive() {
        viewModelScope.launch {
            repository.clearRunArchive()
        }
    }

    class Factory(private val repository: BackroomsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BackroomsViewModel(repository) as T
        }
    }
}
