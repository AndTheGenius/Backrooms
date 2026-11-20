package com.example.engine

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

data class WorldProp(
    val instanceId: Int,
    val spec: AnomalySpec,
    val isAnomaly: Boolean,
    var x: Double,
    var y: Double,
    var scanned: Boolean = false,
    var dissolveProgress: Float = 0f
)

/**
 * Infinite multi-biome Backrooms procedural world seamlessly interconnecting all 8 environments:
 * Level 0 Yellow Halls, Poolrooms, Amusement Park, Gym, Garden, Camera Room, Pharmacy, and Antique Shop.
 * Supports starting in any chosen biome while keeping all other biomes immediately reachable as adjacent wings!
 */
class BackroomsWorld(
    seed: Long = System.currentTimeMillis(),
    val startingBiome: BackroomsBiome = BackroomsBiome.YELLOW_HALLS
) {

    private val random = Random(seed)
    private val cellOverrides = HashMap<Long, Int>()
    private val spawnedRoomKeys = HashSet<Long>()
    val props = mutableListOf<WorldProp>()

    private var nextPropId = 1
    private var shiftCooldown = 6.5f
    private var totalShifts = 0

    // Ordered ring of the other 7 biomes around the starting sector so every biome is close by!
    private val neighboringBiomes: List<BackroomsBiome> =
        BackroomsBiome.entries.filter { it != startingBiome }

    var playerX: Double = 2.5
    var playerY: Double = 2.5
    var playerAngle: Double = 0.0
    var bobPhase: Double = 0.0
    var distanceWalkedMeters: Float = 0f

    init {
        initializeStartZone()
    }

    private fun pack(gx: Int, gy: Int): Long = (gx.toLong() shl 32) or (gy.toLong() and 0xFFFFFFFFL)

    /**
     * Determines which Backrooms environment owns grid cell (gx, gy).
     * Uses spacious 12x12 macro-sectors:
     * - Sector (0, 0) is always [startingBiome].
     * - The 8 surrounding sectors immediately expose all 7 other biomes so players walk naturally
     *   into Amusement Park, Gym, Garden, Camera Room, Pharmacy, Antique Shop, and Poolrooms!
     * - Deeper sectors hash infinitely across all 8 biomes.
     */
    fun getBiome(gx: Int, gy: Int): BackroomsBiome {
        val macroX = Math.floorDiv(gx, 12)
        val macroY = Math.floorDiv(gy, 12)
        if (macroX == 0 && macroY == 0) return startingBiome

        // Deterministic surrounding ring of all 7 other biomes around the starting sector
        if (macroX in -1..1 && macroY in -1..1) {
            val ringIndex = when (macroX to macroY) {
                1 to 0 -> 0
                0 to 1 -> 1
                1 to 1 -> 2
                0 to -1 -> 3
                -1 to 0 -> 4
                -1 to 1 -> 5
                1 to -1 -> 6
                else -> 0
            }
            return neighboringBiomes[ringIndex % neighboringBiomes.size]
        }

        val all = BackroomsBiome.entries
        val idx = abs(hash(macroX * 19 + 7, macroY * 37 + 13)) % all.size
        return all[idx]
    }

    fun getPlayerBiome(): BackroomsBiome = getBiome(floor(playerX).toInt(), floor(playerY).toInt())

    private fun initializeStartZone() {
        // Spacious starting hall inside macro-sector (0, 0) using [startingBiome] architecture
        for (gx in 1..11) {
            for (gy in 1..10) {
                cellOverrides[pack(gx, gy)] = 0
            }
        }
        // Frame outer border of the starting hall with wide 3-tile doorways into neighboring biomes!
        for (gx in 0..11) {
            cellOverrides[pack(gx, 0)] = if (gx in 4..6) 0 else primaryWallFor(startingBiome, gx, 0, 1)
            cellOverrides[pack(gx, 11)] = if (gx in 5..7) 0 else primaryWallFor(startingBiome, gx, 11, 1)
        }
        for (gy in 1..10) {
            cellOverrides[pack(0, gy)] = if (gy in 5..7) 0 else primaryWallFor(startingBiome, 0, gy, 1)
            // Wide doorway straight ahead at +X (x=11, y=2..4) opening directly into the next biome!
            cellOverrides[pack(11, gy)] = if (gy in 2..4 || gy in 7..8) 0 else primaryWallFor(startingBiome, 11, gy, 1)
        }

        // Signature architectural pillars & accent walls inside the opening room
        cellOverrides[pack(5, 4)] = accentWallFor(startingBiome)
        cellOverrides[pack(8, 7)] = accentWallFor(startingBiome)
        cellOverrides[pack(5, 8)] = primaryWallFor(startingBiome, 5, 8, 1)

        // Rare player-tracking wall eye on one outer wall if starting in Yellow Halls
        if (startingBiome == BackroomsBiome.YELLOW_HALLS) {
            cellOverrides[pack(9, 0)] = TextureAtlas.WALL_EYES
        }

        spawnedRoomKeys.add(pack(0, 0))
        spawnedRoomKeys.add(pack(1, 0))
        spawnedRoomKeys.add(pack(0, 1))
        spawnedRoomKeys.add(pack(1, 1))

        // Spawn thematic anomalies + a normal decoy right in the opening sector so players can explore immediately
        val thematic = startingBiome.preferredAnomalyIds.ifEmpty { listOf("three_legged_chair", "five_eyed_doll") }
        val spots = listOf(
            Triple(6.4, 2.6, true),
            Triple(9.4, 3.6, true),
            Triple(7.8, 9.2, false), // Normal baseline decoy
            Triple(3.2, 7.6, true)
        )
        spots.forEachIndexed { idx, (sx, sy, isAnom) ->
            val id = thematic[idx % thematic.size]
            val spec = AnomalyCatalog.getById(id) ?: return@forEachIndexed
            props.add(WorldProp(nextPropId++, spec, isAnom, sx, sy))
        }
    }

    fun getCell(gx: Int, gy: Int): Int {
        val key = pack(gx, gy)
        cellOverrides[key]?.let { return it }

        val cell = generateProceduralCell(gx, gy)
        cellOverrides[key] = cell
        return cell
    }

    private fun generateProceduralCell(gx: Int, gy: Int): Int {
        val roomX = Math.floorDiv(gx, 6)
        val roomY = Math.floorDiv(gy, 6)
        val lx = Math.floorMod(gx, 6)
        val ly = Math.floorMod(gy, 6)

        val roomHash = hash(roomX, roomY)
        val distTier = AnomalyCatalog.stageForDistance(distanceWalkedMeters).tier
        val biome = getBiome(gx, gy)

        if (lx == 0 || ly == 0) {
            val isDoorwayX = (lx == 0 && (ly == 2 || ly == 3))
            val isDoorwayY = (ly == 0 && (lx == 2 || lx == 3))
            if (isDoorwayX || isDoorwayY) {
                val closeWall = ((roomHash and 7) == 0) && isDoorwayY && ((roomX + roomY) and 1 == 0)
                if (!closeWall) return 0
            }
            return pickWallTexture(gx, gy, biome, roomHash, distTier)
        }

        if (lx == 3 && ly == 3 && (roomHash and 3) != 0) {
            return if (biome == BackroomsBiome.YELLOW_HALLS && distTier >= 4 && (roomHash and 4) != 0) {
                TextureAtlas.WALL_GLITCH
            } else {
                accentWallFor(biome)
            }
        }
        if (lx == 3 && ly == 2 && (roomHash and 15) == 5) {
            return pickWallTexture(gx, gy, biome, roomHash, distTier)
        }
        return 0
    }

    private fun primaryWallFor(biome: BackroomsBiome, gx: Int, gy: Int, tier: Int): Int {
        val h = abs(hash(gx * 3 + 7, gy * 5 + 11)) % 100
        return when (biome) {
            BackroomsBiome.YELLOW_HALLS -> when {
                tier >= 5 && h < 24 -> TextureAtlas.WALL_GLITCH
                tier >= 4 && h < 14 -> TextureAtlas.WALL_GLITCH
                tier >= 3 && h in 26..30 -> TextureAtlas.WALL_EYES
                h in 32..36 -> TextureAtlas.WALL_DOOR
                tier >= 2 && h < 55 -> TextureAtlas.WALL_STAINED
                else -> TextureAtlas.WALL_YELLOW
            }
            BackroomsBiome.POOLROOMS -> if (h < 18) TextureAtlas.WALL_POOL_LADDER else TextureAtlas.WALL_POOL_TILE
            BackroomsBiome.AMUSEMENT_PARK -> if (h < 34) TextureAtlas.WALL_CARNIVAL_BOOTH else TextureAtlas.WALL_CARNIVAL_STRIPE
            BackroomsBiome.GYM -> if (h < 32) TextureAtlas.WALL_GYM_LOCKER else TextureAtlas.WALL_GYM_BRICK
            BackroomsBiome.GARDEN -> if (h < 40) TextureAtlas.WALL_GARDEN_TRELLIS else TextureAtlas.WALL_GARDEN_BRICK
            BackroomsBiome.CAMERA_ROOM -> if (h < 52) TextureAtlas.WALL_CAMERA_MONITORS else TextureAtlas.WALL_CAMERA_RACK
            BackroomsBiome.PHARMACY -> if (h < 22) TextureAtlas.WALL_PHARMACY_CROSS else TextureAtlas.WALL_PHARMACY_SHELF
            BackroomsBiome.ANTIQUE_SHOP -> if (h < 42) TextureAtlas.WALL_ANTIQUE_SHELF else TextureAtlas.WALL_ANTIQUE_DAMASK
        }
    }

    private fun accentWallFor(biome: BackroomsBiome): Int = when (biome) {
        BackroomsBiome.YELLOW_HALLS -> TextureAtlas.WALL_PILLAR
        BackroomsBiome.POOLROOMS -> TextureAtlas.WALL_POOL_LADDER
        BackroomsBiome.AMUSEMENT_PARK -> TextureAtlas.WALL_CARNIVAL_BOOTH
        BackroomsBiome.GYM -> TextureAtlas.WALL_GYM_LOCKER
        BackroomsBiome.GARDEN -> TextureAtlas.WALL_GARDEN_TRELLIS
        BackroomsBiome.CAMERA_ROOM -> TextureAtlas.WALL_CAMERA_MONITORS
        BackroomsBiome.PHARMACY -> TextureAtlas.WALL_PHARMACY_CROSS
        BackroomsBiome.ANTIQUE_SHOP -> TextureAtlas.WALL_ANTIQUE_SHELF
    }

    private fun pickWallTexture(gx: Int, gy: Int, biome: BackroomsBiome, roomHash: Int, tier: Int): Int {
        val base = primaryWallFor(biome, gx, gy, tier)
        // Rare watching eyes can very occasionally appear in Yellow Halls or Antique Shop at Tier 3+
        if (tier >= 3 && (biome == BackroomsBiome.YELLOW_HALLS || biome == BackroomsBiome.ANTIQUE_SHOP)) {
            if (abs(hash(gx * 11, gy * 17)) % 100 < 5) return TextureAtlas.WALL_EYES
        }
        if (biome == BackroomsBiome.YELLOW_HALLS && (roomHash and 3) == 0 && base == TextureAtlas.WALL_YELLOW) {
            return TextureAtlas.WALL_STAINED
        }
        return base
    }

    private fun hash(x: Int, y: Int): Int {
        var h = x * 374761393 + y * 668265263 + 1013904223
        h = (h xor (h ushr 13)) * 1274126177
        return h xor (h ushr 16)
    }

    fun isWalkable(x: Double, y: Double, radius: Double = 0.28): Boolean {
        val minX = floor(x - radius).toInt()
        val maxX = floor(x + radius).toInt()
        val minY = floor(y - radius).toInt()
        val maxY = floor(y + radius).toInt()
        for (gx in minX..maxX) {
            for (gy in minY..maxY) {
                if (getCell(gx, gy) != 0) return false
            }
        }
        return true
    }

    fun update(
        dt: Float,
        forwardInput: Float,
        strafeInput: Float,
        turnDelta: Float
    ): Boolean {
        playerAngle = (playerAngle + turnDelta) % (Math.PI * 2.0)

        val cosA = cos(playerAngle)
        val sinA = sin(playerAngle)
        val speed = getPlayerBiome().walkSpeed

        val moveX = (cosA * forwardInput - sinA * strafeInput) * speed * dt
        val moveY = (sinA * forwardInput + cosA * strafeInput) * speed * dt

        val oldX = playerX
        val oldY = playerY

        if (isWalkable(playerX + moveX, playerY)) playerX += moveX
        if (isWalkable(playerX, playerY + moveY)) playerY += moveY

        val stepDist = hypot(playerX - oldX, playerY - oldY)
        if (stepDist > 0.0005) {
            distanceWalkedMeters += (stepDist * 1.65).toFloat()
            bobPhase += stepDist * 4.4
        }

        val iterator = props.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            if (p.scanned) {
                p.dissolveProgress += dt * 1.9f
                if (p.dissolveProgress >= 1f) iterator.remove()
            } else if (hypot(p.x - playerX, p.y - playerY) > 30.0) {
                iterator.remove()
            }
        }

        ensureNearbyRoomsPopulated()

        val tier = AnomalyCatalog.stageForDistance(distanceWalkedMeters).tier
        if (tier >= 2) {
            shiftCooldown -= dt * (0.7f + tier * 0.25f)
            if (shiftCooldown <= 0f) {
                shiftCooldown = (9.5f - tier * 1.2f).coerceAtLeast(3.5f)
                return mutateUnseenGeometryBehindPlayer(tier)
            }
        }
        return false
    }

    private fun ensureNearbyRoomsPopulated() {
        val pRoomX = Math.floorDiv(floor(playerX).toInt(), 6)
        val pRoomY = Math.floorDiv(floor(playerY).toInt(), 6)
        val tier = AnomalyCatalog.stageForDistance(distanceWalkedMeters).tier

        for (rx in (pRoomX - 2)..(pRoomX + 2)) {
            for (ry in (pRoomY - 2)..(pRoomY + 2)) {
                val rKey = pack(rx, ry)
                if (spawnedRoomKeys.contains(rKey)) continue
                spawnedRoomKeys.add(rKey)

                if (props.size >= 28) continue
                if (random.nextFloat() > 0.78f) continue

                val candidates = listOf(
                    (rx * 6 + 2.5) to (ry * 6 + 2.5),
                    (rx * 6 + 4.4) to (ry * 6 + 4.4),
                    (rx * 6 + 4.2) to (ry * 6 + 1.8),
                    (rx * 6 + 1.8) to (ry * 6 + 4.2)
                ).shuffled(random)

                val spot = candidates.firstOrNull { (sx, sy) ->
                    isWalkable(sx, sy, 0.42) &&
                        hypot(sx - playerX, sy - playerY) > 4.0 &&
                        props.none { hypot(it.x - sx, it.y - sy) < 3.2 }
                } ?: continue

                val spotBiome = getBiome(floor(spot.first).toInt(), floor(spot.second).toInt())
                val eligibleSpecs = AnomalyCatalog.all.filter { it.minDistortionTier <= tier }

                // 65% chance to pick a biome-thematic artifact (e.g. pills in Pharmacy, tree/grass in Garden!)
                val spec = if (random.nextFloat() < 0.65f) {
                    val preferredId = spotBiome.preferredAnomalyIds.random(random)
                    AnomalyCatalog.getById(preferredId) ?: eligibleSpecs.random(random)
                } else {
                    eligibleSpecs.random(random)
                }

                val anomalyChance = (0.58f + (tier - 1) * 0.04f).coerceAtMost(0.76f)
                val isAnomaly = random.nextFloat() < anomalyChance

                props.add(
                    WorldProp(
                        instanceId = nextPropId++,
                        spec = spec,
                        isAnomaly = isAnomaly,
                        x = spot.first,
                        y = spot.second
                    )
                )
            }
        }
    }

    private fun mutateUnseenGeometryBehindPlayer(tier: Int): Boolean {
        val px = floor(playerX).toInt()
        val py = floor(playerY).toInt()
        var mutatedCount = 0

        for (attempt in 0 until 24) {
            val gx = px + random.nextInt(-10, 11)
            val gy = py + random.nextInt(-10, 11)
            val dx = (gx + 0.5) - playerX
            val dy = (gy + 0.5) - playerY
            val dist = hypot(dx, dy)
            if (dist < 4.5 || dist > 11.5) continue

            val angleToCell = atan2(dy, dx)
            var angleDiff = (angleToCell - playerAngle) % (Math.PI * 2.0)
            if (angleDiff > Math.PI) angleDiff -= Math.PI * 2.0
            if (angleDiff < -Math.PI) angleDiff += Math.PI * 2.0
            if (abs(angleDiff) < 1.75) continue

            val current = getCell(gx, gy)
            val biome = getBiome(gx, gy)
            if (current != 0) {
                val newWall = when {
                    tier >= 3 && random.nextFloat() < 0.22f -> 0
                    biome == BackroomsBiome.YELLOW_HALLS && tier >= 4 && random.nextFloat() < 0.40f -> TextureAtlas.WALL_GLITCH
                    biome == BackroomsBiome.YELLOW_HALLS && tier >= 3 && random.nextFloat() < 0.06f -> TextureAtlas.WALL_EYES
                    else -> primaryWallFor(biome, gx, gy, tier)
                }
                cellOverrides[pack(gx, gy)] = newWall
                mutatedCount++
            } else if (tier >= 3 && random.nextFloat() < 0.14f) {
                val blockedProp = props.any { hypot(it.x - (gx + 0.5), it.y - (gy + 0.5)) < 1.2 }
                if (!blockedProp) {
                    cellOverrides[pack(gx, gy)] = accentWallFor(biome)
                    mutatedCount++
                }
            }
            if (mutatedCount >= 4) break
        }

        if (tier >= 2) {
            for (prop in props) {
                if (prop.scanned || !prop.isAnomaly) continue
                val dx = prop.x - playerX
                val dy = prop.y - playerY
                val dist = hypot(dx, dy)
                if (dist in 4.0..11.0) {
                    val angleToProp = atan2(dy, dx)
                    var diff = (angleToProp - playerAngle) % (Math.PI * 2.0)
                    if (diff > Math.PI) diff -= Math.PI * 2.0
                    if (diff < -Math.PI) diff += Math.PI * 2.0
                    if (abs(diff) > 1.8) {
                        val nx = prop.x - (dx / dist) * 0.45
                        val ny = prop.y - (dy / dist) * 0.45
                        if (isWalkable(nx, ny, 0.35)) {
                            prop.x = nx
                            prop.y = ny
                            mutatedCount++
                            break
                        }
                    }
                }
            }
        }

        if (mutatedCount > 0) {
            totalShifts++
            return totalShifts % 2 == 1
        }
        return false
    }
}
