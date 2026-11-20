package com.example.engine

import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Generates crisp 64x64 procedural ARGB textures for all 8 Backrooms environments:
 * Level 0 Yellow Halls, Poolrooms, Amusement Park, Gym, Garden, Camera Room, Pharmacy, and Antique Shop,
 * plus 5-frame player-tracking realistic wall eyes.
 */
object TextureAtlas {

    const val TEX_SIZE = 64

    // Wall IDs
    const val WALL_YELLOW = 1
    const val WALL_STAINED = 2
    const val WALL_PILLAR = 3
    const val WALL_EYES = 4
    const val WALL_GLITCH = 5
    const val WALL_DOOR = 6
    const val WALL_POOL_TILE = 7
    const val WALL_POOL_LADDER = 8
    const val WALL_CARNIVAL_STRIPE = 9
    const val WALL_CARNIVAL_BOOTH = 10
    const val WALL_GYM_BRICK = 11
    const val WALL_GYM_LOCKER = 12
    const val WALL_GARDEN_BRICK = 13
    const val WALL_GARDEN_TRELLIS = 14
    const val WALL_CAMERA_MONITORS = 15
    const val WALL_CAMERA_RACK = 16
    const val WALL_PHARMACY_SHELF = 17
    const val WALL_PHARMACY_CROSS = 18
    const val WALL_ANTIQUE_DAMASK = 19
    const val WALL_ANTIQUE_SHELF = 20

    val wallTextures: Map<Int, IntArray> by lazy {
        mapOf(
            WALL_YELLOW to generateYellowWallpaper(stained = false),
            WALL_STAINED to generateYellowWallpaper(stained = true),
            WALL_PILLAR to generateConcretePillar(),
            WALL_EYES to eyeTrackingFrames[2],
            WALL_GLITCH to generateGlitchWallpaper(),
            WALL_DOOR to generateUtilityDoorWall(),
            WALL_POOL_TILE to generatePoolTileWall(withLadder = false),
            WALL_POOL_LADDER to generatePoolTileWall(withLadder = true),
            WALL_CARNIVAL_STRIPE to generateCarnivalStripeWall(),
            WALL_CARNIVAL_BOOTH to generateCarnivalBoothWall(),
            WALL_GYM_BRICK to generateGymBrickWall(),
            WALL_GYM_LOCKER to generateGymLockerWall(),
            WALL_GARDEN_BRICK to generateGardenBrickWall(),
            WALL_GARDEN_TRELLIS to generateGardenTrellisWall(),
            WALL_CAMERA_MONITORS to generateCameraMonitorWall(),
            WALL_CAMERA_RACK to generateCameraRackWall(),
            WALL_PHARMACY_SHELF to generatePharmacyShelfWall(),
            WALL_PHARMACY_CROSS to generatePharmacyCrossWall(),
            WALL_ANTIQUE_DAMASK to generateAntiqueDamaskWall(),
            WALL_ANTIQUE_SHELF to generateAntiqueShelfWall()
        )
    }

    val eyeTrackingFrames: List<IntArray> by lazy {
        listOf(-4.2f, -2.1f, 0f, 2.1f, 4.2f).map { gazeShift ->
            generateTrackingEyeWallpaper(gazeShift)
        }
    }

    private val floorTextures: Map<BackroomsBiome, IntArray> by lazy {
        mapOf(
            BackroomsBiome.YELLOW_HALLS to generateCarpetTexture(),
            BackroomsBiome.POOLROOMS to generatePoolWaterFloorTexture(),
            BackroomsBiome.AMUSEMENT_PARK to generateCarnivalFloorTexture(),
            BackroomsBiome.GYM to generateGymHardwoodFloor(),
            BackroomsBiome.GARDEN to generateGardenGrassFloor(),
            BackroomsBiome.CAMERA_ROOM to generateCameraGridFloor(),
            BackroomsBiome.PHARMACY to generatePharmacyLinoleumFloor(),
            BackroomsBiome.ANTIQUE_SHOP to generateAntiqueWoodFloor()
        )
    }

    private val ceilingTextures: Map<BackroomsBiome, IntArray> by lazy {
        mapOf(
            BackroomsBiome.YELLOW_HALLS to generateCeilingTileTexture(),
            BackroomsBiome.POOLROOMS to generatePoolCeilingTexture(),
            BackroomsBiome.AMUSEMENT_PARK to generateCarnivalCeilingTexture(),
            BackroomsBiome.GYM to generateGymCeilingTexture(),
            BackroomsBiome.GARDEN to generateGardenSkyCeiling(),
            BackroomsBiome.CAMERA_ROOM to generateCameraCeilingTexture(),
            BackroomsBiome.PHARMACY to generatePharmacyCeilingTexture(),
            BackroomsBiome.ANTIQUE_SHOP to generateAntiqueTinCeiling()
        )
    }

    fun floorTextureFor(biome: BackroomsBiome): IntArray = floorTextures[biome]!!
    fun ceilingTextureFor(biome: BackroomsBiome): IntArray = ceilingTextures[biome]!!

    private fun argb(r: Int, g: Int, b: Int, a: Int = 255): Int {
        return (a.coerceIn(0, 255) shl 24) or
            (r.coerceIn(0, 255) shl 16) or
            (g.coerceIn(0, 255) shl 8) or
            b.coerceIn(0, 255)
    }

    private fun hashNoise(x: Int, y: Int, seed: Int): Int {
        var n = x * 374761393 + y * 668265263 + seed * 1442695041
        n = (n xor (n ushr 13)) * 1274126177
        return ((n xor (n ushr 16)) and 0xFF) - 128
    }

    // --- LEVEL 0 YELLOW HALLS ---
    private fun generateYellowWallpaper(stained: Boolean): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val n = hashNoise(x, y, if (stained) 71 else 19) / 16
                val stripe = (x % 8 == 0 || x % 8 == 1)
                val chevron = ((x + y) % 16 == 0 || (x - y + 64) % 16 == 0) && (x % 8 in 3..6)
                var r = if (stripe) 174 else 196
                var g = if (stripe) 158 else 180
                var b = if (stripe) 84 else 101
                if (chevron) { r -= 14; g -= 13; b -= 8 }
                if (y < 3) { r = 128; g = 118; b = 76 }
                if (y >= TEX_SIZE - 6) {
                    val trim = if (y == TEX_SIZE - 6) 14 else 0
                    r = 92 + trim; g = 74 + trim; b = 46 + trim
                }
                if (stained) {
                    val stainWave = sin(x * 0.22) * 5.0 + sin(x * 0.55) * 3.0
                    if (y > TEX_SIZE - 22 + stainWave.toInt() && y < TEX_SIZE - 6) {
                        r = (r * 0.68f).toInt(); g = (g * 0.67f).toInt(); b = (b * 0.58f).toInt()
                    }
                    if (x in 24..27 && y in 4..44) {
                        r = (r * 0.72f).toInt(); g = (g * 0.70f).toInt(); b = (b * 0.62f).toInt()
                    }
                } else if (y in (TEX_SIZE - 12)..(TEX_SIZE - 7)) {
                    r = (r * 0.88f).toInt(); g = (g * 0.87f).toInt(); b = (b * 0.82f).toInt()
                }
                pixels[y * TEX_SIZE + x] = argb(r + n, g + n, b + n / 2)
            }
        }
        return pixels
    }

    private fun generateConcretePillar(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val n = hashNoise(x, y, 33) / 14
                val edge = (x < 3 || x >= TEX_SIZE - 3)
                var r = if (edge) 142 else 172
                var g = if (edge) 130 else 159
                var b = if (edge) 74 else 92
                if (x in 28..35 && y in 46..54) {
                    r = 210; g = 205; b = 186
                    if ((x == 30 || x == 33) && (y == 48 || y == 52)) { r = 35; g = 32; b = 28 }
                }
                if (y >= TEX_SIZE - 6) { r = 84; g = 68; b = 42 }
                pixels[y * TEX_SIZE + x] = argb(r + n, g + n, b + n / 2)
            }
        }
        return pixels
    }

    private fun generateTrackingEyeWallpaper(pupilOffsetX: Float): IntArray {
        val pixels = generateYellowWallpaper(stained = true)
        val cx = 32f
        val cy = 28f
        val rx = 12.5f
        val ry = 6.2f
        val pupilX = cx + pupilOffsetX
        val pupilY = cy + 0.4f

        for (y in 18..38) {
            for (x in 16..48) {
                val dx = (x - cx) / rx
                val dy = (y - cy) / ry
                val ellipseDist = dx * dx + dy * dy
                if (ellipseDist <= 1.25f) {
                    if (ellipseDist > 0.92f) {
                        val orig = pixels[y * TEX_SIZE + x]
                        pixels[y * TEX_SIZE + x] = argb(
                            ((orig ushr 16) and 0xFF) * 58 / 100,
                            ((orig ushr 8) and 0xFF) * 52 / 100,
                            (orig and 0xFF) * 44 / 100
                        )
                    } else {
                        val pdx = x - pupilX
                        val pdy = y - pupilY
                        val pDist = sqrt(pdx * pdx + pdy * pdy)
                        val lidShadow = if (y < cy - 2.6f) 0.68f else 1.0f
                        val glintDist = sqrt((x - (pupilX - 1.4f)) * (x - (pupilX - 1.4f)) + (y - (pupilY - 1.4f)) * (y - (pupilY - 1.4f)))
                        pixels[y * TEX_SIZE + x] = when {
                            glintDist < 1.25f -> argb(248, 248, 240)
                            pDist < 2.6f -> argb(14, 12, 14)
                            pDist < 4.8f -> {
                                val ring = if (pDist > 4.1f) 0.65f else 1.0f
                                argb((112 * ring * lidShadow).toInt(), (78 * ring * lidShadow).toInt(), (38 * ring * lidShadow).toInt())
                            }
                            else -> {
                                val vein = if (abs(y - cy) < 1.2f && abs(x - cx) > 6.5f) 18 else 0
                                val edgeShade = (1f - ellipseDist * 0.25f) * lidShadow
                                argb(((226 + vein) * edgeShade).toInt(), ((214 - vein) * edgeShade).toInt(), ((192 - vein) * edgeShade).toInt())
                            }
                        }
                    }
                }
            }
        }
        return pixels
    }

    private fun generateGlitchWallpaper(): IntArray {
        val pixels = generateYellowWallpaper(stained = true)
        for (y in 0 until TEX_SIZE) {
            val rowGlitch = (y % 11 == 0 || y % 17 == 3)
            for (x in 0 until TEX_SIZE) {
                val base = pixels[y * TEX_SIZE + x]
                var r = (base ushr 16) and 0xFF
                var g = (base ushr 8) and 0xFF
                var b = base and 0xFF
                if (rowGlitch) {
                    val shift = pixels[y * TEX_SIZE + ((x + 5) % TEX_SIZE)]
                    r = ((shift ushr 16) and 0xFF).coerceAtMost(240)
                    g = (g * 0.75f).toInt()
                    b = (b + 48).coerceAtMost(220)
                } else {
                    r = (r * 0.84f).toInt(); g = (g * 0.90f).toInt(); b = (b * 0.72f).toInt()
                }
                pixels[y * TEX_SIZE + x] = argb(r, g, b)
            }
        }
        return pixels
    }

    private fun generateUtilityDoorWall(): IntArray {
        val pixels = generateYellowWallpaper(stained = false)
        for (y in 8 until TEX_SIZE - 1) {
            for (x in 14 until 50) {
                val frame = (x in 14..16 || x in 47..49 || y in 8..10)
                val panel = (x in 21..42 && (y in 16..34 || y in 39..56))
                val n = hashNoise(x, y, 99) / 22
                var r = if (frame) 72 else 118
                var g = if (frame) 62 else 102
                var b = if (frame) 44 else 68
                if (panel && !frame) { r -= 12; g -= 10; b -= 8 }
                if (abs(x - 42) <= 2 && abs(y - 38) <= 2) { r = 198; g = 170; b = 82 }
                pixels[y * TEX_SIZE + x] = argb(r + n, g + n, b + n / 2)
            }
        }
        return pixels
    }

    // --- POOLROOMS ---
    private fun generatePoolTileWall(withLadder: Boolean): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val isGrout = (x % 8 == 0 || y % 8 == 0)
                val bevel = (x % 8 == 1 || y % 8 == 1)
                val n = hashNoise(x, y, 211) / 24
                val waterBounce = ((y - 38).coerceAtLeast(0) / 26f)
                var r = if (isGrout) 128 else if (bevel) 232 else 214
                var g = if (isGrout) 148 else if (bevel) 244 else 230
                var b = if (isGrout) 154 else if (bevel) 246 else 234
                r = (r * (1f - waterBounce * 0.38f)).toInt()
                g = (g * (1f - waterBounce * 0.06f)).toInt()
                b = (b + (waterBounce * 18f)).toInt().coerceAtMost(255)
                if (y in 16..19 && !isGrout) { r = 82; g = 176; b = 196 }
                if (withLadder) {
                    val leftRail = x in 20..22 && y >= 10
                    val rightRail = x in 41..43 && y >= 10
                    val rung = (y == 22 || y == 34 || y == 46 || y == 58) && x in 21..42
                    if (leftRail || rightRail || rung) {
                        val sheen = if (x == 21 || x == 42 || y % 2 == 0) 238 else 166
                        r = sheen; g = sheen + 4; b = sheen + 8
                    }
                }
                pixels[y * TEX_SIZE + x] = argb(r + n, g + n, b + n)
            }
        }
        return pixels
    }

    // --- AMUSEMENT PARK ---
    private fun generateCarnivalStripeWall(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val n = hashNoise(x, y, 401) / 18
                val isRedStripe = ((x / 8) % 2 == 0)
                var r = if (isRedStripe) 174 else 222
                var g = if (isRedStripe) 42 else 208
                var b = if (isRedStripe) 48 else 174

                // Gold bunting trim & warm marquee bulbs along the top valance
                if (y in 6..11) {
                    r = 186; g = 148; b = 52
                    val bulbDist = abs((x % 12) - 6) + abs(y - 8)
                    if (bulbDist <= 2) { r = 254; g = 242; b = 174 }
                }
                // Scalloped dark carnival valance shadow
                if (y in 12..14) { r = (r * 0.62f).toInt(); g = (g * 0.62f).toInt(); b = (b * 0.62f).toInt() }
                // Faded painted wood baseboard
                if (y >= 54) { r = 64; g = 88; b = 96 }
                pixels[y * TEX_SIZE + x] = argb(r + n, g + n, b + n)
            }
        }
        return pixels
    }

    private fun generateCarnivalBoothWall(): IntArray {
        val pixels = generateCarnivalStripeWall()
        // Recessed dark ticket booth window with glowing golden marquee frame & striped awning
        for (y in 14..48) {
            for (x in 10..54) {
                val border = (x in 10..13 || x in 51..54 || y in 14..17 || y in 45..48)
                val awning = y in 18..25
                when {
                    border -> {
                        val bulb = ((x + y) % 6 == 0)
                        pixels[y * TEX_SIZE + x] = if (bulb) argb(255, 240, 148) else argb(166, 128, 42)
                    }
                    awning -> {
                        val stripe = ((x / 5) % 2 == 0)
                        pixels[y * TEX_SIZE + x] = if (stripe) argb(192, 46, 56) else argb(232, 220, 188)
                    }
                    else -> {
                        // Dim interior ticket window with silhouette bars
                        val bar = (x % 8 == 0)
                        pixels[y * TEX_SIZE + x] = if (bar) argb(82, 70, 44) else argb(24, 20, 32)
                    }
                }
            }
        }
        return pixels
    }

    // --- EMPTY GYM ---
    private fun generateGymBrickWall(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            val row = y / 8
            val offset = if (row % 2 == 0) 0 else 8
            for (x in 0 until TEX_SIZE) {
                val mortar = (y % 8 == 0) || ((x + offset) % 16 == 0)
                val n = hashNoise(x, y, 512) / 22
                // Painted institutional cream cinderblocks with varsity navy & crimson gym stripes
                var r = if (mortar) 158 else 212
                var g = if (mortar) 156 else 208
                var b = if (mortar) 146 else 196
                if (!mortar && y in 24..31) { r = 34; g = 58; b = 104 } // Varsity navy stripe
                if (!mortar && y in 33..36) { r = 172; g = 42; b = 46 } // Crimson trim
                if (y >= 56) { r = 74; g = 76; b = 82 } // Rubber gym kickboard
                pixels[y * TEX_SIZE + x] = argb(r + n, g + n, b + n)
            }
        }
        return pixels
    }

    private fun generateGymLockerWall(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val n = hashNoise(x, y, 525) / 24
                val seam = (x % 16 == 0 || x % 16 == 15)
                val lx = x % 16
                var r = if (seam) 42 else 74
                var g = if (seam) 56 else 96
                var b = if (seam) 74 else 124
                if (y < 6) { r = 186; g = 184; b = 176 } // Wall above lockers
                if (y >= 6 && y <= 8) { r = 52; g = 68; b = 88 }
                // Locker louver vents near top & handle latch
                if (y in 13..22 && y % 2 == 0 && lx in 4..11) { r = 28; g = 34; b = 44 }
                if (y in 34..40 && lx in 11..13) { r = 198; g = 204; b = 212 }
                if (y >= 58) { r = 56; g = 58; b = 64 }
                pixels[y * TEX_SIZE + x] = argb(r + n, g + n, b + n)
            }
        }
        return pixels
    }

    // --- INDOOR GARDEN ---
    private fun generateGardenBrickWall(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            val row = y / 6
            val offset = if (row % 2 == 0) 0 else 6
            for (x in 0 until TEX_SIZE) {
                val mortar = (y % 6 == 0) || ((x + offset) % 12 == 0)
                val n = hashNoise(x, y, 601) / 14
                var r = if (mortar) 108 else 142
                var g = if (mortar) 106 else 72
                var b = if (mortar) 94 else 56
                // Moss & creeping ivy vines clinging down from top & rising from damp grass
                val vineWave = sin(x * 0.24) * 8.0 + sin(x * 0.62) * 4.0
                val hasMoss = y > 46 + (vineWave / 2).toInt() ||
                    (y < 22 + vineWave.toInt() && abs(hashNoise(x / 2, y / 2, 609)) > 35)
                if (hasMoss) {
                    r = 44 + n / 2; g = 104 + n; b = 46 + n / 2
                }
                pixels[y * TEX_SIZE + x] = argb(r + n, g + n, b + n)
            }
        }
        return pixels
    }

    private fun generateGardenTrellisWall(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val n = hashNoise(x, y, 633) / 18
                // Pale misty conservatory glass panes behind timber trellis lattice
                var r = 152 + (y / 4)
                var g = 186 + (y / 6)
                var b = 176 + (y / 6)
                val lattice = (x % 16 < 2 || y % 16 < 2 || (x + y) % 16 == 0 || (x - y + 64) % 16 == 0)
                if (lattice) { r = 74; g = 62; b = 46 }
                // Lush climbing ivy leaves weaving across trellis
                val ivy = ((x * 3 + y * 5) % 23 < 5) && abs(sin(x * 0.15 + y * 0.12)) > 0.48
                if (ivy) { r = 46; g = 118; b = 52 }
                if (y >= 54) { r = 82; g = 74; b = 62 }
                pixels[y * TEX_SIZE + x] = argb(r + n, g + n, b + n)
            }
        }
        return pixels
    }

    // --- CAMERA ROOM (SURVEILLANCE) ---
    private fun generateCameraMonitorWall(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val n = hashNoise(x, y, 701) / 24
                var r = 34 + n
                var g = 38 + n
                var b = 36 + n

                // 3x2 bank of CRT CCTV monitors embedded in the dark steel console wall
                val col = x / 21
                val row = (y - 6) / 22
                val lx = x % 21
                val ly = (y - 6) % 22
                if (col in 0..2 && row in 0..1 && y in 6..49 && lx in 2..18 && ly in 2..17) {
                    val bezel = (lx == 2 || lx == 18 || ly == 2 || ly == 17)
                    if (bezel) {
                        r = 58; g = 62; b = 60
                    } else {
                        // Phosphor-green security feed showing tiny hallway perspective + scanlines
                        val scan = if (ly % 2 == 0) 0.78f else 1.0f
                        val staticDot = if (abs(hashNoise(x, y, 719 + col * 7)) > 96) 50 else 0
                        val corridorWall = (lx < 6 || lx > 14)
                        val baseG = (if (corridorWall) 118 else 68) + staticDot
                        r = (baseG * 0.34f * scan).toInt()
                        g = (baseG * scan).toInt()
                        b = (baseG * 0.42f * scan).toInt()
                        // Tiny crimson REC LED in upper-left of each monitor
                        if (lx in 4..5 && ly in 4..5) { r = 236; g = 42; b = 48 }
                    }
                }
                // Bottom steel desk lip with blinking control buttons
                if (y >= 54) {
                    r = 46; g = 50; b = 52
                    if (y in 57..58 && x % 9 in 2..4) {
                        if ((x / 9) % 2 == 0) { r = 92; g = 228; b = 112 } else { r = 236; g = 166; b = 46 }
                    }
                }
                pixels[y * TEX_SIZE + x] = argb(r, g, b)
            }
        }
        return pixels
    }

    private fun generateCameraRackWall(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val n = hashNoise(x, y, 733) / 26
                val frame = (x < 6 || x > 57)
                var r = if (frame) 48 else 26
                var g = if (frame) 52 else 30
                var b = if (frame) 50 else 32
                if (!frame && y % 6 == 0) { r = 56; g = 60; b = 62 }
                // Blinking green/amber/red server tape drive LEDs
                if (!frame && y % 6 == 3 && x in 10..52) {
                    if (x % 7 == 0) {
                        val pick = (x + y) % 3
                        when (pick) {
                            0 -> { r = 96; g = 238; b = 122 }
                            1 -> { r = 242; g = 172; b = 48 }
                            else -> { r = 232; g = 54; b = 62 }
                        }
                    } else if (x in 18..46) {
                        r = 16; g = 18; b = 20 // VHS tape deck slot
                    }
                }
                pixels[y * TEX_SIZE + x] = argb(r + n, g + n, b + n)
            }
        }
        return pixels
    }

    // --- EMPTY PHARMACY ---
    private fun generatePharmacyShelfWall(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val n = hashNoise(x, y, 801) / 28
                // Bright clinical white/mint pharmacy backpanel
                var r = 222
                var g = 234
                var b = 228
                val shelfBar = (y == 16 || y == 30 || y == 44 || y == 57)
                if (shelfBar) {
                    r = 148; g = 164; b = 158
                } else if (y in 7..56) {
                    val slotY = (y - 3) % 14
                    val bottleX = x % 7
                    if (slotY in 5..12 && bottleX in 1..5) {
                        val bottleKind = ((x / 7) * 5 + (y / 14) * 3) % 4
                        val cap = (slotY in 5..6)
                        when {
                            cap -> { r = 242; g = 242; b = 238 }
                            bottleKind == 0 -> { r = 194; g = 104; b = 32 } // Amber Rx bottle
                            bottleKind == 1 -> { r = 64; g = 156; b = 142 } // Mint medicine box
                            bottleKind == 2 -> { r = 74; g = 126; b = 194 } // Blue pharmacy carton
                            else -> { r = 234; g = 228; b = 216 } // White pill bottle
                        }
                    }
                }
                if (y < 6) { r = 62; g = 152; b = 124 } // Mint pharmacy header valance
                pixels[y * TEX_SIZE + x] = argb(r + n, g + n, b + n)
            }
        }
        return pixels
    }

    private fun generatePharmacyCrossWall(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val grout = (x % 16 == 0 || y % 16 == 0)
                val n = hashNoise(x, y, 821) / 28
                var r = if (grout) 176 else 226
                var g = if (grout) 196 else 238
                var b = if (grout) 190 else 232
                // Glowing emerald-green clinical Pharmacy Cross (+)
                val inCross = (x in 27..37 && y in 14..44) || (x in 17..47 && y in 24..34)
                val inBorder = (x in 25..39 && y in 12..46) || (x in 15..49 && y in 22..36)
                when {
                    inCross -> { r = 66; g = 224; b = 134 }
                    inBorder -> { r = 34; g = 124; b = 82 }
                }
                if (y >= 56) { r = 78; g = 156; b = 132 }
                pixels[y * TEX_SIZE + x] = argb(r + n, g + n, b + n)
            }
        }
        return pixels
    }

    // --- ANTIQUE SHOP ---
    private fun generateAntiqueDamaskWall(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val n = hashNoise(x, y, 901) / 18
                if (y >= 42) {
                    // Rich carved mahogany wainscoting panels
                    val panel = (x % 16 in 3..12 && y in 46..58)
                    val grain = (sin(x * 0.7) * 8).toInt()
                    val r = (if (panel) 74 else 94) + grain
                    val g = (if (panel) 44 else 56) + grain / 2
                    val b = (if (panel) 26 else 34) + grain / 3
                    pixels[y * TEX_SIZE + x] = argb(r + n, g + n, b + n)
                } else {
                    // Vintage faded burgundy & tarnished-gold damask wallpaper
                    val motif = ((x % 16 - 8) * (x % 16 - 8) + (y % 16 - 8) * (y % 16 - 8)) in 12..24
                    val r = if (motif) 136 else 98
                    val g = if (motif) 88 else 36
                    val b = if (motif) 54 else 38
                    pixels[y * TEX_SIZE + x] = argb(r + n, g + n, b + n)
                }
            }
        }
        return pixels
    }

    private fun generateAntiqueShelfWall(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val n = hashNoise(x, y, 922) / 20
                val frame = (x < 5 || x > 58 || y < 5 || y == 22 || y == 40 || y >= 56)
                var r = if (frame) 86 else 36
                var g = if (frame) 52 else 22
                var b = if (frame) 30 else 18
                if (!frame) {
                    // Curio silhouettes on shelves: brass vase, antique books, framed portrait, pendulum clock
                    val isBooks = y in 9..21 && x in 8..26 && (x % 4 != 0)
                    val isClock = y in 8..21 && x in 36..52
                    val isVase = y in 26..39 && x in 14..24
                    val isFrame = y in 26..38 && x in 34..52
                    val isChest = y in 44..55 && x in 12..48
                    when {
                        isBooks -> { r = 128 + (x * 13) % 50; g = 62 + (x * 7) % 40; b = 42 }
                        isClock -> {
                            val dial = ((x - 44) * (x - 44) + (y - 14) * (y - 14)) < 18
                            if (dial) { r = 216; g = 198; b = 148 } else { r = 114; g = 74; b = 38 }
                        }
                        isVase -> { r = 186; g = 152; b = 74 }
                        isFrame -> {
                            val border = (x in 34..36 || x in 50..52 || y in 26..28 || y in 36..38)
                            if (border) { r = 172; g = 136; b = 62 } else { r = 74; g = 64; b = 52 }
                        }
                        isChest -> { r = 98; g = 62; b = 34 }
                    }
                }
                pixels[y * TEX_SIZE + x] = argb(r + n, g + n, b + n)
            }
        }
        return pixels
    }

    // --- FLOOR & CEILING TEXTURES ---
    private fun generateCarpetTexture(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val weave = if ((x + y) % 2 == 0) 6 else -6
                val n = hashNoise(x, y, 51) / 10
                val damp = if ((x / 16 + y / 16) % 2 == 0) -5 else 3
                pixels[y * TEX_SIZE + x] = argb(138 + weave + n + damp, 121 + weave + n + damp, 66 + weave / 2 + n / 2)
            }
        }
        return pixels
    }

    private fun generateCeilingTileTexture(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val gridLine = (x == 0 || x == 32 || y == 0 || y == 32)
                val isLightPanel = (x in 10..22 && y in 8..24) || (x in 42..54 && y in 40..56)
                val n = hashNoise(x, y, 88) / 18
                pixels[y * TEX_SIZE + x] = when {
                    isLightPanel -> argb(246, 245, 214)
                    gridLine -> argb(102, 98, 78)
                    else -> argb(166 + n, 158 + n, 122 + n / 2)
                }
            }
        }
        return pixels
    }

    private fun generatePoolWaterFloorTexture(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val isGrout = (x % 8 == 0 || y % 8 == 0)
                val caustic = sin(x * 0.25 + y * 0.15) * sin(y * 0.28 - x * 0.12)
                val boost = (caustic * 26.0).toInt()
                pixels[y * TEX_SIZE + x] = argb(
                    if (isGrout) 46 else 64 + boost,
                    if (isGrout) 144 else 188 + boost,
                    if (isGrout) 158 else 204 + boost
                )
            }
        }
        return pixels
    }

    private fun generatePoolCeilingTexture(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val isGrout = (x % 16 == 0 || y % 16 == 0)
                val skylight = (x in 20..44 && y in 20..44)
                val n = hashNoise(x, y, 307) / 26
                pixels[y * TEX_SIZE + x] = when {
                    skylight -> argb(232, 248, 250)
                    isGrout -> argb(122, 146, 152)
                    else -> argb(192 + n, 214 + n, 218 + n)
                }
            }
        }
        return pixels
    }

    private fun generateCarnivalFloorTexture(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val checker = ((x / 16 + y / 16) % 2 == 0)
                val n = hashNoise(x, y, 421) / 18
                pixels[y * TEX_SIZE + x] = if (checker) argb(184 + n, 166 + n, 134 + n) else argb(62 + n, 114 + n, 118 + n)
            }
        }
        return pixels
    }

    private fun generateCarnivalCeilingTexture(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val n = hashNoise(x, y, 439)
                val isBulb = ((x == 16 || x == 48) && (y == 16 || y == 48)) ||
                    (abs(x - 16) <= 1 && abs(y - 16) <= 1) || (abs(x - 48) <= 1 && abs(y - 48) <= 1)
                val isStar = n > 116
                pixels[y * TEX_SIZE + x] = when {
                    isBulb -> argb(255, 232, 146)
                    isStar -> argb(232, 224, 246)
                    else -> argb(26, 22, 48)
                }
            }
        }
        return pixels
    }

    private fun generateGymHardwoodFloor(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val plankSeam = (y % 8 == 0)
                val boardTone = ((y / 8 + (x / 24)) % 3) * 10
                val n = hashNoise(x, y, 541) / 22
                var r = 172 + boardTone + n
                var g = 128 + boardTone + n
                var b = 74 + boardTone / 2 + n / 2
                if (plankSeam) { r -= 34; g -= 28; b -= 18 }
                // Crisp painted court boundary stripe
                if (x in 29..31) { r = 232; g = 228; b = 216 }
                pixels[y * TEX_SIZE + x] = argb(r, g, b)
            }
        }
        return pixels
    }

    private fun generateGymCeilingTexture(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val truss = (x % 16 < 2 || y % 32 < 2)
                val lamp = (x in 22..42 && y in 22..42)
                pixels[y * TEX_SIZE + x] = when {
                    lamp -> argb(248, 248, 244)
                    truss -> argb(106, 112, 122)
                    else -> argb(164, 170, 178)
                }
            }
        }
        return pixels
    }

    private fun generateGardenGrassFloor(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val n = hashNoise(x, y, 651) / 8
                // Central mossy stepping stone flagstone path + surrounding lush damp grass
                val isStone = (x in 22..42 && ((y % 16) in 2..13))
                pixels[y * TEX_SIZE + x] = if (isStone) {
                    argb(114 + n / 2, 118 + n / 2, 104 + n / 2)
                } else {
                    argb(46 + n / 2, 108 + n, 44 + n / 2)
                }
            }
        }
        return pixels
    }

    private fun generateGardenSkyCeiling(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val frame = (x % 32 < 2 || y % 32 < 2)
                val n = hashNoise(x, y, 677) / 26
                pixels[y * TEX_SIZE + x] = if (frame) argb(62, 74, 68) else argb(176 + n, 206 + n, 198 + n)
            }
        }
        return pixels
    }

    private fun generateCameraGridFloor(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val seam = (x % 16 == 0 || y % 16 == 0)
                val dot = (x % 4 == 2 && y % 4 == 2)
                val base = if (seam) 24 else if (dot) 48 else 38
                pixels[y * TEX_SIZE + x] = argb(base, base + 4, base + 2)
            }
        }
        return pixels
    }

    private fun generateCameraCeilingTexture(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val strip = (x in 29..34 && y in 12..52)
                val duct = (y % 16 < 3)
                pixels[y * TEX_SIZE + x] = when {
                    strip -> argb(122, 224, 146)
                    duct -> argb(46, 52, 50)
                    else -> argb(28, 32, 30)
                }
            }
        }
        return pixels
    }

    private fun generatePharmacyLinoleumFloor(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val checker = ((x / 16 + y / 16) % 2 == 0)
                val n = hashNoise(x, y, 845) / 30
                pixels[y * TEX_SIZE + x] = if (checker) argb(218 + n, 232 + n, 224 + n) else argb(172 + n, 204 + n, 192 + n)
            }
        }
        return pixels
    }

    private fun generatePharmacyCeilingTexture(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val grid = (x % 32 == 0 || y % 32 == 0)
                val panel = (x in 6..26 && y in 8..24) || (x in 38..58 && y in 40..56)
                pixels[y * TEX_SIZE + x] = when {
                    panel -> argb(246, 254, 250)
                    grid -> argb(156, 174, 168)
                    else -> argb(206, 220, 214)
                }
            }
        }
        return pixels
    }

    private fun generateAntiqueWoodFloor(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val seam = (y % 8 == 0)
                val grain = (sin(x * 0.45 + y * 0.1) * 7).toInt()
                val n = hashNoise(x, y, 955) / 20
                var r = 88 + grain + n
                var g = 56 + grain / 2 + n / 2
                var b = 34 + grain / 3 + n / 3
                if (seam) { r -= 26; g -= 18; b -= 12 }
                pixels[y * TEX_SIZE + x] = argb(r, g, b)
            }
        }
        return pixels
    }

    private fun generateAntiqueTinCeiling(): IntArray {
        val pixels = IntArray(TEX_SIZE * TEX_SIZE)
        for (y in 0 until TEX_SIZE) {
            for (x in 0 until TEX_SIZE) {
                val border = (x % 16 == 0 || y % 16 == 0)
                val lamp = ((x - 32) * (x - 32) + (y - 32) * (y - 32)) < 80
                val emboss = ((x % 16 - 8) * (x % 16 - 8) + (y % 16 - 8) * (y % 16 - 8)) in 12..22
                pixels[y * TEX_SIZE + x] = when {
                    lamp -> argb(244, 212, 138)
                    border -> argb(72, 54, 38)
                    emboss -> argb(148, 122, 86)
                    else -> argb(118, 96, 68)
                }
            }
        }
        return pixels
    }
}
