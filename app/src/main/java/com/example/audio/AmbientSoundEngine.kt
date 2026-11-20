package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.engine.BackroomsBiome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Real-time procedural AudioTrack soundscape adapting dynamically across all 8 Backrooms environments:
 * Level 0 fluorescent hum, Poolrooms echoing water, Amusement Park distant detuned calliope drone,
 * Gym hollow acoustics, Garden gentle twilight breeze, Camera Room CRT/server whine, Pharmacy clinical buzz,
 * and Antique Shop dusty room tone.
 */
class AmbientSoundEngine {

    private val sampleRate = 22050
    private var audioJob: Job? = null
    private var track: AudioTrack? = null

    @Volatile var isMuted: Boolean = false
    @Volatile var distortionIntensity: Float = 0f
    @Volatile var currentBiome: BackroomsBiome = BackroomsBiome.YELLOW_HALLS

    @Volatile private var footstepSamples = 0
    @Volatile private var zoomSamples = 0
    @Volatile private var confirmSamples = 0
    @Volatile private var errorSamples = 0
    @Volatile private var shiftSamples = 0
    @Volatile var isScanning: Boolean = false

    fun start(scope: CoroutineScope) {
        if (audioJob?.isActive == true) return
        audioJob = scope.launch(Dispatchers.Default) {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            val audioTrack = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (_: Throwable) {
                return@launch
            }

            track = audioTrack
            try {
                audioTrack.play()
            } catch (_: Throwable) {
                audioTrack.release()
                track = null
                return@launch
            }

            val chunk = ShortArray(1024)
            var phaseMain = 0.0
            var phaseHarmonic = 0.0
            var phaseDetune = 0.0
            var phaseBiome = 0.0
            var phaseSfx = 0.0
            var rng = 987654321
            var lpNoise = 0.0

            val twoPi = 2.0 * PI

            while (isActive) {
                val muted = isMuted
                val dist = distortionIntensity.toDouble()
                val scanning = isScanning
                val biome = currentBiome

                val baseFreq = when (biome) {
                    BackroomsBiome.YELLOW_HALLS -> 60.0
                    BackroomsBiome.POOLROOMS -> 142.0
                    BackroomsBiome.AMUSEMENT_PARK -> 196.0
                    BackroomsBiome.GYM -> 68.0
                    BackroomsBiome.GARDEN -> 94.0
                    BackroomsBiome.CAMERA_ROOM -> 112.0
                    BackroomsBiome.PHARMACY -> 64.0
                    BackroomsBiome.ANTIQUE_SHOP -> 52.0
                }
                val biomeVoiceFreq = when (biome) {
                    BackroomsBiome.AMUSEMENT_PARK -> 247.0 // Subtle eerie minor-third carnival chord
                    BackroomsBiome.CAMERA_ROOM -> 1820.0   // Subtle CRT flyback whine
                    BackroomsBiome.POOLROOMS -> 212.0      // Water cavern resonance
                    else -> baseFreq * 2.0
                }

                val stepMain = twoPi * (baseFreq - dist * 4.0) / sampleRate
                val stepHarm = twoPi * (baseFreq * 2.0 + dist * 8.0) / sampleRate
                val stepDetune = twoPi * (56.5 - dist * 11.0) / sampleRate
                val stepBiome = twoPi * biomeVoiceFreq / sampleRate

                for (i in chunk.indices) {
                    if (muted) {
                        chunk[i] = 0
                        continue
                    }

                    val ambient = when (biome) {
                        BackroomsBiome.GARDEN -> sin(phaseMain) * 0.025 + sin(phaseDetune) * (dist * 0.06)
                        BackroomsBiome.CAMERA_ROOM -> sin(phaseMain) * 0.06 + sin(phaseBiome) * 0.014 + sin(phaseDetune) * (dist * 0.07)
                        BackroomsBiome.AMUSEMENT_PARK -> sin(phaseMain) * 0.035 + sin(phaseBiome) * 0.024 + sin(phaseDetune) * (dist * 0.07)
                        BackroomsBiome.ANTIQUE_SHOP -> sin(phaseMain) * 0.03 + sin(phaseDetune) * (dist * 0.06)
                        else -> sin(phaseMain) * 0.075 + sin(phaseHarmonic) * (0.036 + dist * 0.02) + sin(phaseDetune) * (dist * 0.08)
                    }

                    phaseMain = (phaseMain + stepMain) % twoPi
                    phaseHarmonic = (phaseHarmonic + stepHarm) % twoPi
                    phaseDetune = (phaseDetune + stepDetune) % twoPi
                    phaseBiome = (phaseBiome + stepBiome) % twoPi

                    rng = rng * 1664525 + 1013904223
                    val white = ((rng ushr 16) and 0xFFFF) / 32768.0 - 1.0
                    lpNoise = lpNoise * 0.86 + white * 0.14
                    val hissGain = when (biome) {
                        BackroomsBiome.GARDEN -> 0.095
                        BackroomsBiome.POOLROOMS -> 0.075
                        BackroomsBiome.CAMERA_ROOM -> 0.07
                        else -> 0.045
                    }
                    val hiss = lpNoise * (hissGain + dist * 0.06)

                    var sfx = 0.0
                    if (scanning) {
                        phaseSfx = (phaseSfx + twoPi * 740.0 / sampleRate) % twoPi
                        sfx += sin(phaseSfx) * 0.09
                    }
                    if (footstepSamples > 0) {
                        val env = footstepSamples / 2200.0
                        sfx += when (biome) {
                            BackroomsBiome.POOLROOMS -> (white * 0.16 + lpNoise * 0.22) * env
                            BackroomsBiome.GYM, BackroomsBiome.PHARMACY -> (sin(footstepSamples * 0.12) * 0.14 + lpNoise * 0.20) * env
                            else -> lpNoise * env * 0.26
                        }
                        footstepSamples--
                    }
                    if (zoomSamples > 0) {
                        val env = zoomSamples / 1800.0
                        sfx += sin(zoomSamples * 0.24) * env * 0.14
                        zoomSamples--
                    }
                    if (confirmSamples > 0) {
                        val freq = if (confirmSamples > 2600) 880.0 else 1320.0
                        val env = (confirmSamples / 5200.0).coerceAtMost(1.0)
                        sfx += sin(confirmSamples * twoPi * freq / sampleRate) * env * 0.22
                        confirmSamples--
                    }
                    if (errorSamples > 0) {
                        val env = errorSamples / 5500.0
                        sfx += (white * 0.25 + sin(errorSamples * 0.09) * 0.20) * env
                        errorSamples--
                    }
                    if (shiftSamples > 0) {
                        val env = shiftSamples / 9000.0
                        sfx += sin(shiftSamples * 0.018) * env * 0.24
                        shiftSamples--
                    }

                    val mixed = (ambient + hiss + sfx).coerceIn(-0.95, 0.95)
                    chunk[i] = (mixed * 32767.0).toInt().toShort()
                }

                try {
                    audioTrack.write(chunk, 0, chunk.size)
                } catch (_: Throwable) {
                    break
                }
            }

            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Throwable) {
                // Ignore teardown race
            }
            track = null
        }
    }

    fun playFootstep() {
        footstepSamples = if (currentBiome == BackroomsBiome.POOLROOMS) 2200 else 1500
    }

    fun playZoomClick() { zoomSamples = 1800 }
    fun playAnomalyConfirm() { confirmSamples = 5200 }
    fun playFalsePositive() { errorSamples = 5500 }
    fun playGeometryShift() { shiftSamples = 9000 }

    fun stop() {
        audioJob?.cancel()
        audioJob = null
    }
}
