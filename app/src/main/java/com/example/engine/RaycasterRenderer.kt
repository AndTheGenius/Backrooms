package com.example.engine

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

data class AimTarget(
    val prop: WorldProp,
    val distance: Float
)

/**
 * Pure-Kotlin 2.5D DDA Raycaster + Multi-Biome Textured Floor/Ceiling + Z-Buffered 128x128
 * Realistic Billboard Sprite Renderer. Smoothly renders transitions across all 8 Backrooms
 * environments, animated Poolrooms water ripples, flickering Camera Room monitors, and
 * dynamic player-tracking wallpaper eyes.
 */
class RaycasterRenderer(
    val width: Int = 240,
    val height: Int = 150
) {
    private val pixels = IntArray(width * height)
    private val zBuffer = FloatArray(width)
    private val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val imageBitmap: ImageBitmap = bitmap.asImageBitmap()

    private var frameSeed = 1337

    fun render(
        world: BackroomsWorld,
        fovPlane: Float,
        timeSeconds: Float,
        distortionIntensity: Float,
        distortionTier: Int,
        staticBurst: Float,
        scanActive: Boolean
    ): AimTarget? {
        frameSeed = (frameSeed * 1664525 + 1013904223)

        val posX = world.playerX
        val posY = world.playerY
        val dirX = cos(world.playerAngle)
        val dirY = sin(world.playerAngle)
        val planeX = -dirY * fovPlane
        val planeY = dirX * fovPlane
        val playerBiome = world.getPlayerBiome()

        val walkBob = sin(world.bobPhase).toFloat() * 3.6f
        val breatheDrift = if (distortionTier >= 2) {
            sin(timeSeconds * (1.1f + distortionIntensity * 1.4f)) * (2.4f * distortionIntensity)
        } else 0f
        val horizonBase = (height / 2f + walkBob + breatheDrift).toInt().coerceIn(height / 3, (height * 2) / 3)

        val flickerWave = sin(timeSeconds * 19f) * sin(timeSeconds * 7.3f)
        val ballastDim = when {
            distortionTier >= 4 && flickerWave > 0.72f -> 0.44f
            distortionTier >= 3 && flickerWave > 0.80f -> 0.64f
            distortionTier >= 2 && flickerWave > 0.86f -> 0.78f
            else -> 1.0f
        }

        // Biome-specific atmospheric fog tinted gently by distortion tier
        val fogR = (playerBiome.fogR + distortionIntensity * 10f).toInt().coerceIn(0, 255)
        val fogG = (playerBiome.fogG + distortionIntensity * 4f - (if (distortionTier >= 5) 6 else 0)).toInt().coerceIn(0, 255)
        val fogB = (playerBiome.fogB + distortionIntensity * 12f).toInt().coerceIn(0, 255)

        val texMask = TextureAtlas.TEX_SIZE - 1
        val rippleU = (sin(timeSeconds * 2.2f) * 3.0f).toInt()
        val rippleV = (cos(timeSeconds * 1.7f) * 3.0f).toInt()

        // 1. MULTI-BIOME FLOOR & CEILING TEXTURED RAYCASTING
        val rayDirX0 = dirX - planeX
        val rayDirY0 = dirY - planeY
        val rayDirX1 = dirX + planeX
        val rayDirY1 = dirY + planeY
        val posZ = 0.5f * height

        for (y in 0 until height) {
            val isFloor = y >= horizonBase
            val p = if (isFloor) (y - horizonBase) else (horizonBase - y)
            val rowDist = if (p == 0) 20f else posZ / p.toFloat()

            val floorStepX = rowDist * (rayDirX1 - rayDirX0) / width
            val floorStepY = rowDist * (rayDirY1 - rayDirY0) / width
            var fx = posX + rowDist * rayDirX0
            var fy = posY + rowDist * rayDirY0

            val fogFactor = (1f - (rowDist / (12.2f - distortionIntensity * 2.6f))).coerceIn(0.06f, 1f) * ballastDim
            val rowOffset = y * width

            for (x in 0 until width) {
                val cellX = floor(fx).toInt()
                val cellY = floor(fy).toInt()
                val cellBiome = world.getBiome(cellX, cellY)

                var tx = (floor(fx * TextureAtlas.TEX_SIZE).toInt()) and texMask
                var ty = (floor(fy * TextureAtlas.TEX_SIZE).toInt()) and texMask
                fx += floorStepX
                fy += floorStepY

                if (cellBiome == BackroomsBiome.POOLROOMS && isFloor) {
                    tx = (tx + rippleU + ((sin(fy * 6.0 + timeSeconds * 3.1) * 2.0).toInt())) and texMask
                    ty = (ty + rippleV) and texMask
                }

                val tex = if (isFloor) {
                    TextureAtlas.floorTextureFor(cellBiome)
                } else {
                    TextureAtlas.ceilingTextureFor(cellBiome)
                }

                val sample = tex[ty * TextureAtlas.TEX_SIZE + tx]
                var r = (sample ushr 16) and 0xFF
                var g = (sample ushr 8) and 0xFF
                var b = sample and 0xFF

                val isLightPanel = !isFloor && ((r > 225 && g > 215) || (g > 215 && b > 140))
                val lit = if (isLightPanel) (fogFactor * 1.45f).coerceAtMost(1.1f) else fogFactor

                r = (r * lit + fogR * (1f - lit.coerceAtMost(1f))).toInt().coerceIn(0, 255)
                g = (g * lit + fogG * (1f - lit.coerceAtMost(1f))).toInt().coerceIn(0, 255)
                b = (b * lit + fogB * (1f - lit.coerceAtMost(1f))).toInt().coerceIn(0, 255)

                pixels[rowOffset + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        // 2. DDA VERTICAL WALL STRIPES
        val wallTextures = TextureAtlas.wallTextures
        val eyeFrames = TextureAtlas.eyeTrackingFrames
        val defaultWall = wallTextures[TextureAtlas.WALL_YELLOW]!!
        val cctvPulse = ((sin(timeSeconds * 11f) * 14f).toInt())

        for (x in 0 until width) {
            val cameraX = 2.0 * x / width - 1.0
            val rayDirX = dirX + planeX * cameraX
            val rayDirY = dirY + planeY * cameraX

            var mapX = floor(posX).toInt()
            var mapY = floor(posY).toInt()

            val deltaDistX = if (rayDirX == 0.0) 1e30 else abs(1.0 / rayDirX)
            val deltaDistY = if (rayDirY == 0.0) 1e30 else abs(1.0 / rayDirY)

            val stepX: Int
            var sideDistX: Double
            if (rayDirX < 0) {
                stepX = -1
                sideDistX = (posX - mapX) * deltaDistX
            } else {
                stepX = 1
                sideDistX = (mapX + 1.0 - posX) * deltaDistX
            }

            val stepY: Int
            var sideDistY: Double
            if (rayDirY < 0) {
                stepY = -1
                sideDistY = (posY - mapY) * deltaDistY
            } else {
                stepY = 1
                sideDistY = (mapY + 1.0 - posY) * deltaDistY
            }

            var hitWall = 0
            var side = 0
            var steps = 0
            while (hitWall == 0 && steps < 22) {
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX
                    mapX += stepX
                    side = 0
                } else {
                    sideDistY += deltaDistY
                    mapY += stepY
                    side = 1
                }
                hitWall = world.getCell(mapX, mapY)
                steps++
            }

            val perpWallDist = (if (side == 0) sideDistX - deltaDistX else sideDistY - deltaDistY)
                .coerceAtLeast(0.08)
                .toFloat()
            zBuffer[x] = perpWallDist

            if (hitWall == 0) continue

            val warpMultiplier = if (distortionTier >= 3) {
                val wave = sin(x * 0.06f + timeSeconds * (1.8f + distortionIntensity * 2.2f))
                1f + wave * (0.04f + (distortionTier - 3) * 0.055f)
            } else 1f

            val rollShift = if (distortionTier >= 3) {
                ((x - width / 2f) * sin(timeSeconds * 0.9f) * 0.035f * distortionIntensity).toInt()
            } else 0

            val lineHeight = ((height / perpWallDist) * warpMultiplier).toInt().coerceAtLeast(1)
            val colHorizon = horizonBase + rollShift
            val drawStart = (-lineHeight / 2 + colHorizon).coerceAtLeast(0)
            val drawEnd = (lineHeight / 2 + colHorizon).coerceAtMost(height - 1)

            val wallX = if (side == 0) posY + perpWallDist * rayDirY else posX + perpWallDist * rayDirX
            val fracWallX = wallX - floor(wallX)
            var texX = (fracWallX * TextureAtlas.TEX_SIZE).toInt().coerceIn(0, TextureAtlas.TEX_SIZE - 1)
            val flipped = (side == 0 && rayDirX > 0) || (side == 1 && rayDirY < 0)
            if (flipped) {
                texX = TextureAtlas.TEX_SIZE - texX - 1
            }

            val tex = if (hitWall == TextureAtlas.WALL_EYES) {
                val toPlayerX = posX - (mapX + 0.5)
                val toPlayerY = posY - (mapY + 0.5)
                val tangentDot = if (side == 0) {
                    if (rayDirX > 0) -toPlayerY else toPlayerY
                } else {
                    if (rayDirY < 0) -toPlayerX else toPlayerX
                }
                val normalDot = (if (side == 0) abs(toPlayerX) else abs(toPlayerY)).coerceAtLeast(0.4)
                val gazeNorm = (tangentDot / normalDot).coerceIn(-1.2, 1.2)
                val frameIdx = when {
                    gazeNorm < -0.65 -> 0
                    gazeNorm < -0.22 -> 1
                    gazeNorm <= 0.22 -> 2
                    gazeNorm <= 0.65 -> 3
                    else -> 4
                }
                eyeFrames[frameIdx]
            } else {
                wallTextures[hitWall] ?: defaultWall
            }

            val step = TextureAtlas.TEX_SIZE.toFloat() / lineHeight.toFloat()
            var texPos = (drawStart - colHorizon + lineHeight / 2f) * step

            val sideShade = if (side == 1) 0.80f else 1.0f
            val fogFactor = (1f - (perpWallDist / (13.0f - distortionIntensity * 2.8f)))
                .coerceIn(0.06f, 1f) * sideShade * ballastDim

            val isEmissiveWall = (hitWall == TextureAtlas.WALL_CAMERA_MONITORS || hitWall == TextureAtlas.WALL_PHARMACY_CROSS || hitWall == TextureAtlas.WALL_CARNIVAL_BOOTH)

            for (y in drawStart..drawEnd) {
                val texY = texPos.toInt() and texMask
                texPos += step
                val sample = tex[texY * TextureAtlas.TEX_SIZE + texX]
                var sr = (sample ushr 16) and 0xFF
                var sg = (sample ushr 8) and 0xFF
                var sb = sample and 0xFF

                // Subtle phosphor glow boost on CCTV monitors, Pharmacy cross, & carnival bulbs
                val localFog = if (isEmissiveWall && (sg > 180 || sr > 225)) {
                    if (hitWall == TextureAtlas.WALL_CAMERA_MONITORS) {
                        sg = (sg + cctvPulse).coerceIn(0, 255)
                    }
                    (fogFactor * 1.45f).coerceAtMost(1.0f)
                } else fogFactor

                val r = (sr * localFog + fogR * (1f - localFog)).toInt().coerceIn(0, 255)
                val g = (sg * localFog + fogG * (1f - localFog)).toInt().coerceIn(0, 255)
                val b = (sb * localFog + fogB * (1f - localFog)).toInt().coerceIn(0, 255)
                pixels[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        // 3. Z-BUFFERED 128x128 REALISTIC BILLBOARD SPRITES
        val sortedProps = world.props.sortedByDescending { prop ->
            val dx = prop.x - posX
            val dy = prop.y - posY
            dx * dx + dy * dy
        }

        val invDet = 1.0 / (planeX * dirY - dirX * planeY)
        val centerX = width / 2
        var bestAimTarget: AimTarget? = null
        var bestAimDist = 6.8f
        val spriteSize = SpriteGenerator.SPRITE_SIZE

        for (prop in sortedProps) {
            val spriteX = prop.x - posX
            val spriteY = prop.y - posY
            val transformX = invDet * (dirY * spriteX - dirX * spriteY)
            val transformY = invDet * (-planeY * spriteX + planeX * spriteY)
            if (transformY <= 0.25) continue

            val depth = transformY.toFloat()
            if (depth > 14.0f) continue

            val spriteScreenX = ((width / 2.0) * (1.0 + transformX / transformY)).toInt()
            val scale = prop.spec.scale
            val spriteHeight = abs((height / transformY) * scale).toInt().coerceAtLeast(1)
            val spriteWidth = spriteHeight
            val vMoveScreen = ((prop.spec.verticalOffset * height) / transformY).toInt()

            val drawStartY = (-spriteHeight / 2 + horizonBase + vMoveScreen).coerceAtLeast(0)
            val drawEndY = (spriteHeight / 2 + horizonBase + vMoveScreen).coerceAtMost(height - 1)
            val drawStartX = (-spriteWidth / 2 + spriteScreenX).coerceAtLeast(0)
            val drawEndX = (spriteWidth / 2 + spriteScreenX).coerceAtMost(width - 1)

            if (!prop.scanned &&
                depth < bestAimDist &&
                centerX in (drawStartX - 6)..(drawEndX + 6) &&
                depth < zBuffer[centerX] + 0.15f
            ) {
                bestAimDist = depth
                bestAimTarget = AimTarget(prop, depth)
            }

            val spriteTex = SpriteGenerator.getSpritePixels(prop.spec.id, prop.isAnomaly)
            val fogFactor = (1f - (depth / (12.5f - distortionIntensity * 2.5f)))
                .coerceIn(0.12f, 1f) * ballastDim

            for (stripe in drawStartX..drawEndX) {
                if (depth >= zBuffer[stripe]) continue
                val texX = ((stripe - (-spriteWidth / 2 + spriteScreenX)) * spriteSize / spriteWidth)
                    .coerceIn(0, spriteSize - 1)

                for (y in drawStartY..drawEndY) {
                    val d = (y - vMoveScreen - horizonBase) + spriteHeight / 2
                    val texY = ((d * spriteSize) / spriteHeight)
                        .coerceIn(0, spriteSize - 1)

                    val color = spriteTex[texY * spriteSize + texX]
                    val alpha = (color ushr 24) and 0xFF
                    if (alpha < 64) continue

                    if (prop.scanned) {
                        val noise = ((stripe * 73 + y * 31 + frameSeed) and 0xFF) / 255f
                        if (noise < prop.dissolveProgress) continue
                    }

                    val sr = (color ushr 16) and 0xFF
                    var sg = (color ushr 8) and 0xFF
                    val sb = color and 0xFF

                    if (prop.scanned) {
                        sg = (sg + 120).coerceAtMost(255)
                    } else if (scanActive && bestAimTarget?.prop?.instanceId == prop.instanceId) {
                        val pulse = (sin(timeSeconds * 28f) * 24f + 26f).toInt()
                        sg = (sg + pulse).coerceAtMost(255)
                    }

                    val r = (sr * fogFactor + fogR * (1f - fogFactor)).toInt().coerceIn(0, 255)
                    val g = (sg * fogFactor + fogG * (1f - fogFactor)).toInt().coerceIn(0, 255)
                    val b = (sb * fogFactor + fogB * (1f - fogFactor)).toInt().coerceIn(0, 255)
                    pixels[y * width + stripe] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
        }

        applyVhsPostProcess(timeSeconds, distortionIntensity, distortionTier, staticBurst)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bestAimTarget
    }

    private fun applyVhsPostProcess(
        timeSeconds: Float,
        distortionIntensity: Float,
        distortionTier: Int,
        staticBurst: Float
    ) {
        val chromaShift = when {
            staticBurst > 0.15f -> 3
            distortionTier >= 5 -> 3
            distortionTier >= 4 -> 2
            distortionTier >= 2 -> 1
            else -> 0
        }

        val tearY = (((timeSeconds * (28f + distortionIntensity * 45f)) % height).toInt() + height) % height
        val tearBandHalf = if (staticBurst > 0.1f) 9 else if (distortionTier >= 4) 4 else 2
        val grainAmount = (7 + distortionIntensity * 24 + staticBurst * 85).toInt()

        for (y in 0 until height) {
            val rowStart = y * width
            val scanlineDark = (y and 1) == 1
            val inTear = (distortionTier >= 3 || staticBurst > 0.1f) && abs(y - tearY) <= tearBandHalf
            val hOffset = if (inTear) ((sin(y * 1.9f + timeSeconds * 30f) * (3 + distortionTier * 2)).toInt()) else 0

            for (x in 0 until width) {
                val srcX = (x + hOffset).coerceIn(0, width - 1)
                val base = pixels[rowStart + srcX]
                var r = (base ushr 16) and 0xFF
                var g = (base ushr 8) and 0xFF
                var b = base and 0xFF

                if (chromaShift > 0) {
                    val rx = (srcX + chromaShift).coerceAtMost(width - 1)
                    val bx = (srcX - chromaShift).coerceAtLeast(0)
                    r = (pixels[rowStart + rx] ushr 16) and 0xFF
                    b = pixels[rowStart + bx] and 0xFF
                }

                frameSeed = frameSeed * 1664525 + 1013904223
                val noise = (((frameSeed ushr 24) and 0xFF) - 128) * grainAmount / 128

                r = (r + noise).coerceIn(0, 255)
                g = (g + noise).coerceIn(0, 255)
                b = (b + noise).coerceIn(0, 255)

                if (scanlineDark) {
                    r = (r * 228) ushr 8
                    g = (g * 228) ushr 8
                    b = (b * 228) ushr 8
                }
                if (inTear && ((frameSeed ushr 16) and 3) == 0) {
                    val snow = 170 + ((frameSeed ushr 8) and 63)
                    r = snow; g = snow; b = snow
                }

                pixels[rowStart + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
    }
}
