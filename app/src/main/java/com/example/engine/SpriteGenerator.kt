package com.example.engine

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Generates high-detail 128x128 shaded realistic ARGB billboard sprites for all 18 Backrooms
 * artifacts in BOTH Normal Baseline and Anomaly variants — using cylindrical/spherical shading,
 * wood grain, porcelain subsurface gloss, metallic highlights, glass reflections, cosmos nebula
 * shaders, foliage layering, and soft ambient occlusion shadows.
 */
object SpriteGenerator {

    const val SPRITE_SIZE = 128

    private val spriteCache = HashMap<String, IntArray>()
    private val previewCache = HashMap<String, ImageBitmap>()

    fun getSpritePixels(anomalyId: String, isAnomaly: Boolean): IntArray {
        val key = "${anomalyId}_${if (isAnomaly) "anom" else "norm"}"
        return spriteCache.getOrPut(key) { buildSprite(anomalyId, isAnomaly) }
    }

    fun getPreviewImage(anomalyId: String, isAnomaly: Boolean): ImageBitmap {
        val key = "${anomalyId}_${if (isAnomaly) "anom" else "norm"}"
        return previewCache.getOrPut(key) {
            val pixels = getSpritePixels(anomalyId, isAnomaly)
            val bmp = Bitmap.createBitmap(SPRITE_SIZE, SPRITE_SIZE, Bitmap.Config.ARGB_8888)
            bmp.setPixels(pixels, 0, SPRITE_SIZE, 0, 0, SPRITE_SIZE, SPRITE_SIZE)
            bmp.asImageBitmap()
        }
    }

    private fun argb(r: Int, g: Int, b: Int, a: Int = 255): Int =
        (a.coerceIn(0, 255) shl 24) or
            (r.coerceIn(0, 255) shl 16) or
            (g.coerceIn(0, 255) shl 8) or
            b.coerceIn(0, 255)

    private fun noise(x: Int, y: Int, seed: Int = 17): Int {
        var n = x * 374761393 + y * 668265263 + seed * 1442695041
        n = (n xor (n ushr 13)) * 1274126177
        return ((n xor (n ushr 16)) and 0xFF) - 128
    }

    private class RealisticCanvas {
        val buf = IntArray(SPRITE_SIZE * SPRITE_SIZE)

        fun blend(x: Int, y: Int, r: Int, g: Int, b: Int, a: Int = 255) {
            if (x !in 0 until SPRITE_SIZE || y !in 0 until SPRITE_SIZE || a <= 0) return
            if (a >= 250) {
                buf[y * SPRITE_SIZE + x] = argb(r, g, b, 255)
                return
            }
            val dst = buf[y * SPRITE_SIZE + x]
            val da = (dst ushr 24) and 0xFF
            if (da == 0) {
                buf[y * SPRITE_SIZE + x] = argb(r, g, b, a)
                return
            }
            val inv = 255 - a
            val nr = (r * a + ((dst ushr 16) and 0xFF) * inv) / 255
            val ng = (g * a + ((dst ushr 8) and 0xFF) * inv) / 255
            val nb = (b * a + (dst and 0xFF) * inv) / 255
            val na = (a + (da * inv) / 255).coerceAtMost(255)
            buf[y * SPRITE_SIZE + x] = argb(nr, ng, nb, na)
        }

        fun set(x: Int, y: Int, color: Int) {
            if (x in 0 until SPRITE_SIZE && y in 0 until SPRITE_SIZE) {
                buf[y * SPRITE_SIZE + x] = color
            }
        }

        /** Soft grounded shadow beneath props for realistic spatial anchoring */
        fun groundShadow(cx: Int, cy: Int, rx: Float, ry: Float, maxAlpha: Int = 150) {
            for (y in (cy - ry.toInt() - 2)..(cy + ry.toInt() + 2)) {
                for (x in (cx - rx.toInt() - 2)..(cx + rx.toInt() + 2)) {
                    val dx = (x - cx) / rx
                    val dy = (y - cy) / ry
                    val d = dx * dx + dy * dy
                    if (d <= 1f) {
                        val a = ((1f - d) * maxAlpha).toInt()
                        blend(x, y, 22, 19, 12, a)
                    }
                }
            }
        }

        /** Shaded 3D cylinder/plank with realistic directional lighting + optional wood grain */
        fun shadedRect(
            x0: Int,
            y0: Int,
            x1: Int,
            y1: Int,
            baseR: Int,
            baseG: Int,
            baseB: Int,
            grain: Boolean = false,
            verticalShade: Boolean = false,
            alpha: Int = 255
        ) {
            val w = (x1 - x0).coerceAtLeast(1).toFloat()
            val h = (y1 - y0).coerceAtLeast(1).toFloat()
            for (y in y0..y1) {
                val ty = (y - y0) / h
                for (x in x0..x1) {
                    val tx = (x - x0) / w
                    // Left-to-right cylindrical highlight (light from upper-left)
                    val light = if (verticalShade) {
                        1.18f - ty * 0.42f
                    } else {
                        0.76f + (1f - abs(tx - 0.28f) * 1.15f).coerceIn(0f, 0.46f)
                    }
                    val gNoise = if (grain) {
                        val streak = sin((x * 0.9 + sin(y * 0.14) * 2.5)).toFloat() * 12f
                        streak.toInt() + noise(x, y, 41) / 24
                    } else {
                        noise(x, y, 23) / 36
                    }
                    blend(
                        x, y,
                        (baseR * light + gNoise).toInt().coerceIn(0, 255),
                        (baseG * light + gNoise).toInt().coerceIn(0, 255),
                        (baseB * light + gNoise / 2).toInt().coerceIn(0, 255),
                        alpha
                    )
                }
            }
        }

        /** Softly shaded 3D sphere/oval with upper-left specular lighting & rim shadow */
        fun shadedEllipse(
            cx: Float,
            cy: Float,
            rx: Float,
            ry: Float,
            baseR: Int,
            baseG: Int,
            baseB: Int,
            gloss: Float = 0.25f,
            alpha: Int = 255
        ) {
            for (y in (cy - ry - 1).toInt()..(cy + ry + 1).toInt()) {
                for (x in (cx - rx - 1).toInt()..(cx + rx + 1).toInt()) {
                    val nx = (x - cx) / rx
                    val ny = (y - cy) / ry
                    val d2 = nx * nx + ny * ny
                    if (d2 <= 1f) {
                        val nz = sqrt((1f - d2).coerceAtLeast(0f))
                        val diffuse = (0.45f + (nx * -0.35f + ny * -0.45f + nz * 0.75f).coerceAtLeast(0f) * 0.72f)
                        val spec = ((nx + 0.35f) * (nx + 0.35f) + (ny + 0.38f) * (ny + 0.38f))
                        val highlight = if (spec < 0.16f) (1f - spec / 0.16f) * gloss * 140f else 0f
                        val n = noise(x, y, 67) / 42
                        blend(
                            x, y,
                            (baseR * diffuse + highlight + n).toInt().coerceIn(0, 255),
                            (baseG * diffuse + highlight + n).toInt().coerceIn(0, 255),
                            (baseB * diffuse + highlight + n).toInt().coerceIn(0, 255),
                            alpha
                        )
                    }
                }
            }
        }

        fun line(x0: Int, y0: Int, x1: Int, y1: Int, r: Int, g: Int, b: Int, thickness: Int = 2, alpha: Int = 255) {
            val steps = maxOf(abs(x1 - x0), abs(y1 - y0), 1)
            val rad = thickness / 2
            for (i in 0..steps) {
                val x = x0 + ((x1 - x0) * i) / steps
                val y = y0 + ((y1 - y0) * i) / steps
                for (dy in -rad..rad) {
                    for (dx in -rad..rad) {
                        blend(x + dx, y + dy, r, g, b, alpha)
                    }
                }
            }
        }
    }

    private fun buildSprite(id: String, isAnomaly: Boolean): IntArray {
        val c = RealisticCanvas()
        when (id) {
            "three_legged_chair" -> drawChair(c, isAnomaly)
            "five_eyed_doll" -> drawDoll(c, isAnomaly)
            "cosmic_mirror" -> drawCosmicMirror(c, isAnomaly)
            "slenderman_doll" -> drawSlendermanDoll(c, isAnomaly)
            "old_vinyl_player" -> drawVinylPlayer(c, isAnomaly)
            "dementia_pills" -> drawDementiaPills(c, isAnomaly)
            "indoor_tree" -> drawIndoorTree(c, isAnomaly)
            "indoor_grass" -> drawIndoorGrass(c, isAnomaly)
            "backwards_exit_sign" -> drawExitSign(c, isAnomaly)
            "thirteen_hour_clock" -> drawClock(c, isAnomaly)
            "inverted_floor_lamp" -> drawLamp(c, isAnomaly)
            "six_finger_bucket" -> drawBucket(c, isAnomaly)
            "void_water_cooler" -> drawWaterCooler(c, isAnomaly)
            "watching_mannequin" -> drawMannequin(c, isAnomaly)
            "bleeding_payphone" -> drawPayphone(c, isAnomaly)
            "recursive_painting" -> drawPainting(c, isAnomaly)
            "floating_crt" -> drawCrt(c, isAnomaly)
            "stretched_doorway" -> drawDoorway(c, isAnomaly)
            else -> drawChair(c, isAnomaly)
        }
        return c.buf
    }

    // 1. REALISTIC OAK CHAIR (4 legs normal vs 3 legs hovering over air)
    private fun drawChair(c: RealisticCanvas, isAnomaly: Boolean) {
        c.groundShadow(64, 120, 38f, 6f, 155)
        // Rear backrest uprights with wood grain
        c.shadedRect(35, 18, 42, 74, 134, 82, 44, grain = true)
        c.shadedRect(86, 18, 93, 74, 128, 76, 40, grain = true)
        // Curved ergonomic top rail & slats
        c.shadedRect(34, 17, 94, 27, 154, 98, 54, grain = true, verticalShade = true)
        c.shadedRect(42, 34, 86, 41, 138, 86, 46, grain = true, verticalShade = true)
        c.shadedRect(42, 49, 86, 56, 134, 82, 44, grain = true, verticalShade = true)
        // Vertical spindles
        c.shadedRect(54, 27, 58, 72, 124, 74, 38, grain = true)
        c.shadedRect(70, 27, 74, 72, 124, 74, 38, grain = true)

        // Beveled 3/4 oak seat with front lip shadow
        c.shadedRect(30, 71, 98, 78, 166, 108, 60, grain = true, verticalShade = true)
        c.shadedRect(29, 77, 99, 83, 102, 60, 30, grain = true)

        // Leg 1: Front-left leg
        c.shadedRect(31, 83, 38, 120, 146, 90, 48, grain = true)
        // Leg 2: Rear-left leg (slightly darker depth)
        c.shadedRect(48, 83, 54, 114, 108, 64, 34, grain = true)
        // Stretcher bar between left legs
        c.shadedRect(38, 102, 49, 106, 98, 58, 30, grain = true)

        // Leg 3: Rear-right leg — PRESENT if normal, CLEANLY MISSING on 3-legged anomaly!
        if (!isAnomaly) {
            c.shadedRect(74, 83, 80, 114, 108, 64, 34, grain = true)
            c.shadedRect(80, 102, 90, 106, 98, 58, 30, grain = true)
        } else {
            // Jagged splintered stub under the seat — stands unnaturally on 3 legs!
            c.shadedRect(74, 83, 80, 88, 92, 54, 28, grain = true)
            c.line(75, 88, 79, 90, 152, 106, 62, 1)
        }

        // Leg 4: Front-right leg
        c.shadedRect(90, 83, 97, 120, 146, 90, 48, grain = true)
    }

    // 2. REALISTIC PORCELAIN DOLL (2 eyes normal vs 5 eerie glossy eyes)
    private fun drawDoll(c: RealisticCanvas, isAnomaly: Boolean) {
        c.groundShadow(64, 120, 32f, 6f, 160)
        // Porcelain legs & glossy Victorian Mary-Jane shoes
        c.shadedRect(42, 104, 54, 117, 214, 200, 186)
        c.shadedRect(74, 104, 86, 117, 214, 200, 186)
        c.shadedEllipse(47f, 118f, 9f, 5f, 32, 28, 30, gloss = 0.5f)
        c.shadedEllipse(81f, 118f, 9f, 5f, 32, 28, 30, gloss = 0.5f)

        // Pleated dusty-blue Victorian fabric dress with vertical fold shadows
        for (y in 62..108) {
            val t = (y - 62) / 46f
            val half = (16 + t * 22).toInt()
            for (x in (64 - half)..(64 + half)) {
                val pleat = sin((x - 64) * 0.42).toFloat() * 18f
                val shade = 0.82f + (1f - abs(x - 56) / (half * 1.4f)).coerceIn(0f, 0.35f)
                c.blend(
                    x, y,
                    ((84 + pleat) * shade).toInt().coerceIn(0, 255),
                    ((112 + pleat) * shade).toInt().coerceIn(0, 255),
                    ((142 + pleat) * shade).toInt().coerceIn(0, 255)
                )
            }
        }
        // White antique lace trim & collar
        for (x in 30..98 step 3) {
            c.shadedEllipse(x.toFloat(), 108f, 2.6f, 2.6f, 232, 224, 206, gloss = 0.05f)
        }
        c.shadedEllipse(64f, 66f, 17f, 6f, 236, 228, 212, gloss = 0.05f)

        // Sleeves & porcelain hands
        c.line(48, 70, 30, 94, 92, 118, 146, 9)
        c.line(80, 70, 98, 94, 82, 108, 134, 9)
        c.shadedEllipse(29f, 96f, 5.5f, 5.5f, 234, 222, 208, gloss = 0.25f)
        c.shadedEllipse(99f, 96f, 5.5f, 5.5f, 234, 222, 208, gloss = 0.25f)

        // Curled chestnut hair halo
        c.shadedEllipse(64f, 40f, 25f, 24f, 72, 46, 28, gloss = 0.22f)
        c.shadedRect(39, 40, 89, 64, 62, 38, 22, grain = true)

        // Glossy pale porcelain oval face with subtle blush cheeks
        c.shadedEllipse(64f, 43f, 20f, 21f, 238, 228, 214, gloss = 0.38f)
        c.shadedEllipse(51f, 49f, 4.5f, 3f, 216, 152, 152, gloss = 0f, alpha = 90)
        c.shadedEllipse(77f, 49f, 4.5f, 3f, 216, 152, 152, gloss = 0f, alpha = 90)
        // Bangs across top of forehead
        c.shadedRect(46, 22, 82, 29, 68, 44, 26, grain = true)

        fun realisticDollEye(ex: Float, ey: Float, r: Float) {
            c.shadedEllipse(ex, ey, r + 1.1f, r + 0.8f, 185, 174, 164, gloss = 0f)
            c.shadedEllipse(ex, ey, r, r, 16, 14, 18, gloss = 0.75f)
            c.blend((ex - r * 0.35f).toInt(), (ey - r * 0.35f).toInt(), 250, 250, 255)
            c.blend((ex - r * 0.35f).toInt() + 1, (ey - r * 0.35f).toInt(), 240, 245, 255)
        }

        if (!isAnomaly) {
            realisticDollEye(54f, 42f, 4.0f)
            realisticDollEye(74f, 42f, 4.0f)
            c.shadedEllipse(64f, 55f, 4.5f, 2.0f, 174, 68, 76, gloss = 0.2f)
        } else {
            // FIVE uncanny glossy dark eyes across face & forehead
            realisticDollEye(53f, 42f, 4.0f)
            realisticDollEye(75f, 42f, 4.0f)
            realisticDollEye(64f, 33f, 3.6f)
            realisticDollEye(55f, 52f, 3.1f)
            realisticDollEye(73f, 52f, 3.1f)
            c.line(59, 59, 69, 59, 108, 42, 46, 2)
        }
    }

    // 3. DIMENSIONAL COSMOS MIRROR (Normal hallway reflection vs swirling galaxy & dimensions!)
    private fun drawCosmicMirror(c: RealisticCanvas, isAnomaly: Boolean) {
        c.groundShadow(64, 122, 34f, 5f, 165)
        // Ornate antique walnut & tarnished brass standing mirror frame
        c.shadedRect(24, 10, 104, 118, 128, 98, 48, grain = true)
        c.shadedRect(28, 14, 100, 114, 86, 62, 28, grain = true)
        c.shadedRect(32, 18, 96, 110, 176, 142, 72)

        // Mirror glass interior (x = 35..93, y = 21..107)
        if (!isAnomaly) {
            // Normal baseline: reflects ordinary yellow Backrooms corridor + diagonal glass shine
            for (y in 21..107) {
                for (x in 35..93) {
                    val isCeil = y < 44
                    val isFloor = y > 84
                    var r = if (isCeil) 168 else if (isFloor) 132 else 186
                    var g = if (isCeil) 160 else if (isFloor) 118 else 170
                    var b = if (isCeil) 128 else if (isFloor) 68 else 96
                    if (!isCeil && !isFloor && x % 8 == 0) {
                        r -= 18; g -= 16; b -= 10
                    }
                    // Diagonal specular glass sheen
                    val sheen = abs((x - 35) + (y - 21) - 62)
                    if (sheen < 6) {
                        r = (r + 48).coerceAtMost(255)
                        g = (g + 48).coerceAtMost(255)
                        b = (b + 44).coerceAtMost(255)
                    }
                    c.blend(x, y, r, g, b)
                }
            }
        } else {
            // ANOMALY: Deep cosmos, swirling violet/indigo nebula, glowing planetary ring & dimensional rifts!
            val cx = 64f
            val cy = 62f
            for (y in 21..107) {
                for (x in 35..93) {
                    val dx = (x - cx) / 30f
                    val dy = (y - cy) / 42f
                    val dist = sqrt(dx * dx + dy * dy)
                    val angle = kotlin.math.atan2(dy.toDouble(), dx.toDouble()).toFloat()
                    val spiral = sin(angle * 3f + dist * 6.5f) * 0.5f + 0.5f
                    val neb = sin(x * 0.11f + cos(y * 0.09f) * 2.2f) * 0.5f + 0.5f

                    var r = (14 + spiral * 95 + neb * 72).toInt()
                    var g = (10 + spiral * 38 + (1f - dist).coerceAtLeast(0f) * 90).toInt()
                    var b = (36 + spiral * 145 + neb * 85).toInt()

                    // Bright dimensional core
                    if (dist < 0.36f) {
                        val core = 1f - dist / 0.36f
                        r = (r + core * 185).toInt()
                        g = (g + core * 210).toInt()
                        b = (b + core * 240).toInt()
                    }
                    // Distant dimension floating tilted ring
                    val ringD = abs(sqrt(((x - 64) / 22f) * ((x - 64) / 22f) + ((y - 62) / 7.5f) * ((y - 62) / 7.5f)) - 1.0f)
                    if (ringD < 0.09f) {
                        r = (r + 140).coerceAtMost(255)
                        g = (g + 210).coerceAtMost(255)
                        b = 255
                    }
                    // Crisp starfield pinpoints
                    val starHash = noise(x, y, 911)
                    if (starHash > 116) {
                        r = 250; g = 248; b = 255
                    }
                    c.blend(x, y, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
                }
            }
            // Faint glowing cyan-violet portal rim spill on inner gold frame
            c.line(35, 21, 93, 21, 146, 186, 255, 2, 160)
            c.line(35, 107, 93, 107, 176, 118, 255, 2, 160)
        }
        // Wooden standing base feet
        c.shadedRect(20, 116, 42, 122, 92, 66, 32, grain = true)
        c.shadedRect(86, 116, 108, 122, 92, 66, 32, grain = true)
    }

    // 4. SLENDERMAN DOLL (Normal collector figurine vs tall blank-faced suited doll with dark tendrils)
    private fun drawSlendermanDoll(c: RealisticCanvas, isAnomaly: Boolean) {
        c.groundShadow(64, 121, 28f, 5f, 165)

        if (isAnomaly) {
            // Uncanny black branching tendrils unfurling from behind the shoulders!
            val tendril = { sx: Int, sy: Int, ex: Int, ey: Int, bend: Int ->
                val steps = 36
                for (i in 0..steps) {
                    val t = i / steps.toFloat()
                    val px = (sx + (ex - sx) * t + sin(t * 3.14159) * bend).toInt()
                    val py = (sy + (ey - sy) * t - sin(t * 3.14159) * 8).toInt()
                    val rad = ((1f - t * 0.65f) * 3.2f).toInt().coerceAtLeast(1)
                    c.shadedEllipse(px.toFloat(), py.toFloat(), rad.toFloat(), rad.toFloat(), 16, 16, 18, gloss = 0f)
                }
            }
            tendril(54, 46, 14, 18, -10)
            tendril(52, 54, 10, 56, -8)
            tendril(54, 62, 18, 92, -12)
            tendril(74, 46, 114, 18, 10)
            tendril(76, 54, 118, 56, 8)
            tendril(74, 62, 110, 92, 12)
        }

        // Elongated tailored dark suit legs
        c.shadedRect(53, 76, 61, 119, 26, 28, 32)
        c.shadedRect(67, 76, 75, 119, 24, 26, 30)
        c.shadedEllipse(56f, 120f, 6f, 3.5f, 18, 18, 20, gloss = 0.4f)
        c.shadedEllipse(72f, 120f, 6f, 3.5f, 18, 18, 20, gloss = 0.4f)

        // Suit jacket torso
        c.shadedRect(48, 38, 80, 80, 28, 30, 34)
        // White dress shirt V-neck
        for (y in 38..58) {
            val spread = ((58 - y) * 7) / 20
            c.shadedRect(64 - spread, y, 64 + spread, y, 232, 230, 224)
        }
        // Signature crimson tie
        c.shadedRect(62, 41, 66, 68, 178, 28, 34)
        c.blend(64, 69, 148, 20, 26)

        // Unnaturally long slender arms hanging past knees on anomaly (shorter on normal)
        val armBottom = if (isAnomaly) 104 else 82
        c.line(48, 42, 39, armBottom, 26, 28, 32, 6)
        c.line(80, 42, 89, armBottom, 26, 28, 32, 6)
        c.shadedEllipse(39f, (armBottom + 4).toFloat(), 4f, 6f, 234, 230, 222, gloss = 0.1f)
        c.shadedEllipse(89f, (armBottom + 4).toFloat(), 4f, 6f, 234, 230, 222, gloss = 0.1f)

        // Pale oval head
        if (!isAnomaly) {
            // Normal collector figurine with hair, eyes, and gentle painted features
            c.shadedEllipse(64f, 24f, 11f, 13f, 224, 198, 176, gloss = 0.15f)
            c.shadedRect(53, 10, 75, 18, 68, 48, 34, grain = true)
            c.shadedEllipse(59f, 23f, 2f, 2f, 24, 24, 28)
            c.shadedEllipse(69f, 23f, 2f, 2f, 24, 24, 28)
            c.line(61, 30, 67, 30, 142, 86, 78, 2)
        } else {
            // Completely smooth, eerily blank stark-white elongated Slenderman head (NO face!)
            c.shadedEllipse(64f, 22f, 10.5f, 14.5f, 238, 236, 230, gloss = 0.28f)
        }
    }

    // 5. OLD VINYL PLAYER (Normal walnut turntable vs glowing crimson cracked spinning LP & floating needle)
    private fun drawVinylPlayer(c: RealisticCanvas, isAnomaly: Boolean) {
        c.groundShadow(64, 118, 46f, 7f, 165)
        // Small wooden side stand
        c.shadedRect(24, 88, 30, 117, 108, 66, 36, grain = true)
        c.shadedRect(98, 88, 104, 117, 108, 66, 36, grain = true)
        c.shadedRect(20, 82, 108, 90, 134, 84, 46, grain = true, verticalShade = true)

        // Vintage walnut turntable plinth
        c.shadedRect(22, 62, 106, 84, 146, 92, 48, grain = true)
        c.shadedRect(24, 60, 104, 66, 174, 116, 66, grain = true, verticalShade = true)
        // Front brass volume knobs & cloth speaker grille
        c.shadedRect(28, 70, 76, 80, 72, 62, 44)
        c.shadedEllipse(88f, 75f, 4f, 4f, 198, 166, 84, gloss = 0.6f)
        c.shadedEllipse(98f, 75f, 4f, 4f, 198, 166, 84, gloss = 0.6f)

        // Perspective vinyl platter & LP record resting on top
        c.shadedEllipse(58f, 58f, 28f, 11f, 30, 30, 34, gloss = 0.35f)
        // Grooves on LP
        for (r in listOf(24f, 19f, 14f)) {
            for (deg in 0 until 360 step 4) {
                val rad = deg * Math.PI / 180.0
                val gx = (58 + cos(rad) * r).toInt()
                val gy = (58 + sin(rad) * (r * 0.38)).toInt()
                if (isAnomaly) {
                    c.blend(gx, gy, 232, 54, 62, 210) // Glowing crimson grooves
                } else {
                    c.blend(gx, gy, 74, 76, 84, 160)
                }
            }
        }
        // Center record label
        c.shadedEllipse(58f, 58f, 9f, 3.5f, if (isAnomaly) 216 else 188, if (isAnomaly) 34 else 142, if (isAnomaly) 42 else 62)

        // Brass horn / tonearm
        c.shadedEllipse(94f, 55f, 5.5f, 4.5f, 196, 164, 82, gloss = 0.6f)
        if (!isAnomaly) {
            c.line(94, 55, 76, 58, 212, 184, 104, 3)
        } else {
            // Levitating tonearm hovering unnaturally high above the glowing vinyl + eerie crimson soundwaves
            c.line(94, 42, 68, 46, 224, 196, 114, 3)
            c.line(68, 46, 66, 54, 236, 56, 64, 2)
            for (ring in 1..3) {
                val rr = ring * 10f
                for (deg in 200..340 step 6) {
                    val rad = deg * Math.PI / 180.0
                    c.blend((58 + cos(rad) * rr).toInt(), (44 + sin(rad) * (rr * 0.5)).toInt(), 236, 88, 92, 160)
                }
            }
        }
    }

    // 6. DEMENTIA PILLS (Normal vitamin bottle vs eerie glowing Rx bottle with floating capsules)
    private fun drawDementiaPills(c: RealisticCanvas, isAnomaly: Boolean) {
        c.groundShadow(64, 118, 26f, 5f, 150)

        // Translucent amber pharmacy cylinder
        c.shadedRect(42, 54, 86, 114, 184, 96, 24, alpha = 240)
        // Bright amber glass vertical highlight
        c.shadedRect(48, 56, 53, 112, 244, 178, 84, alpha = 150)
        // White child-proof ribbed cap
        c.shadedRect(39, 40, 89, 54, 232, 228, 218)
        for (x in 42..86 step 4) {
            c.line(x, 41, x, 53, 184, 180, 170, 1)
        }
        // Paper prescription label wrapped around front
        c.shadedRect(43, 64, 85, 104, 238, 234, 220)
        // Red pharmacy stripe across top of label
        c.shadedRect(43, 64, 85, 70, if (isAnomaly) 208 else 56, if (isAnomaly) 34 else 142, if (isAnomaly) 42 else 98)

        if (!isAnomaly) {
            // Neat ordinary label text lines
            c.shadedRect(48, 76, 78, 79, 82, 86, 92)
            c.shadedRect(48, 84, 72, 86, 128, 130, 134)
            c.shadedRect(48, 90, 76, 92, 128, 130, 134)
        } else {
            // Scratched crimson cross-out marks + eerie "Rx" & hovering capsules floating in mid-air!
            c.line(47, 76, 81, 88, 196, 28, 36, 2)
            c.line(81, 76, 47, 88, 196, 28, 36, 2)
            c.shadedRect(49, 93, 79, 97, 34, 30, 32)
            // Levitating pale blue/white capsules floating above & beside the bottle
            val pills = listOf(Triple(30f, 36f, true), Triple(96f, 32f, false), Triple(64f, 22f, true), Triple(26f, 74f, false), Triple(102f, 70f, true))
            for ((px, py, flip) in pills) {
                c.shadedEllipse(px, py, 9f, 5f, 236, 236, 242, gloss = 0.3f, alpha = 75) // faint static aura
                c.shadedEllipse(px - 2.5f, py, 4.5f, 3.2f, if (flip) 232 else 112, if (flip) 232 else 184, if (flip) 226 else 224, gloss = 0.5f)
                c.shadedEllipse(px + 2.5f, py, 4.5f, 3.2f, if (flip) 216 else 232, if (flip) 58 else 232, if (flip) 64 else 226, gloss = 0.5f)
            }
        }
    }

    // 7. HALLWAY BIRCH TREE (Normal potted office ficus vs full gnarled tree bursting from torn carpet!)
    private fun drawIndoorTree(c: RealisticCanvas, isAnomaly: Boolean) {
        c.groundShadow(64, 121, 44f, 6f, 165)

        if (!isAnomaly) {
            // Ordinary potted indoor office ficus
            c.shadedRect(44, 88, 84, 118, 152, 86, 54)
            c.shadedRect(41, 84, 87, 91, 172, 102, 66)
            // Thin wooden stem
            c.shadedRect(61, 48, 67, 86, 112, 78, 48, grain = true)
            // Compact rounded green office foliage
            c.shadedEllipse(64f, 44f, 24f, 22f, 54, 118, 56, gloss = 0.1f)
            c.shadedEllipse(52f, 50f, 16f, 14f, 68, 138, 64, gloss = 0.1f)
            c.shadedEllipse(76f, 48f, 16f, 15f, 48, 106, 50, gloss = 0.1f)
            c.shadedEllipse(64f, 34f, 18f, 14f, 78, 152, 70, gloss = 0.12f)
        } else {
            // ANOMALY: Massive pale-bark birch/oak tree bursting straight through torn yellow carpet!
            // Exposed roots ripping up the carpet
            c.line(64, 106, 26, 120, 142, 132, 116, 6)
            c.line(64, 106, 102, 120, 136, 126, 110, 6)
            c.line(64, 108, 46, 122, 124, 116, 102, 5)
            c.line(64, 108, 84, 122, 124, 116, 102, 5)

            // Thick realistic pale birch trunk with dark horizontal lenticel bark marks
            for (y in 26..114) {
                val half = 11 + ((y - 26) * 5) / 88
                c.shadedRect(64 - half, y, 64 + half, y, 206, 198, 182, grain = true)
                if (y % 9 == 0) {
                    c.shadedRect(64 - half + 3, y, 64 + half - 5, y + 1, 58, 52, 46)
                }
            }
            // Spreading gnarled upper branches reaching to the ceiling
            c.line(62, 46, 22, 16, 184, 174, 158, 7)
            c.line(66, 44, 106, 14, 184, 174, 158, 7)
            c.line(58, 58, 16, 42, 164, 154, 138, 5)
            c.line(70, 56, 112, 38, 164, 154, 138, 5)
            // Dense realistic canopy leaves pressing against the Backrooms ceiling
            val leafClusters = listOf(
                Triple(32f, 22f, 20f), Triple(64f, 16f, 24f), Triple(96f, 22f, 20f),
                Triple(22f, 38f, 16f), Triple(106f, 38f, 16f), Triple(50f, 30f, 18f), Triple(78f, 30f, 18f)
            )
            for ((lx, ly, lr) in leafClusters) {
                c.shadedEllipse(lx, ly, lr, lr * 0.72f, 52, 116, 48, gloss = 0.08f)
                c.shadedEllipse(lx - 4f, ly - 3f, lr * 0.65f, lr * 0.45f, 88, 158, 68, gloss = 0.1f)
            }
        }
    }

    // 8. OVERGROWN CARPET GRASS (Normal green welcome mat vs lush wild meadow grass & flowers sprouting!)
    private fun drawIndoorGrass(c: RealisticCanvas, isAnomaly: Boolean) {
        c.groundShadow(64, 118, 48f, 8f, 140)
        if (!isAnomaly) {
            // Flat rectangular institutional green rubber welcome mat
            for (y in 94..116) {
                val inset = ((116 - y) * 14) / 22
                c.shadedRect(14 + inset, y, 114 - inset, y, 58, 102, 68)
            }
            c.line(28, 95, 100, 95, 36, 68, 44, 2)
            c.line(15, 115, 113, 115, 36, 68, 44, 2)
        } else {
            // Lush organic tuft of vibrant dew-lit grass blades & tiny pale wildflowers erupting from floor!
            c.shadedEllipse(64f, 114f, 44f, 9f, 42, 64, 30, gloss = 0f)
            for (i in 0 until 78) {
                val bx = 22 + (i * 84) / 78 + (noise(i, 7, 13) / 34)
                val height = 24 + abs(noise(i, 19, 31)) % 36
                val lean = (noise(i, 41, 73) / 9)
                val gTone = 128 + (i % 5) * 18
                val rTone = 48 + (i % 4) * 14
                c.line(bx, 116, bx + lean, 116 - height, rTone, gTone, 42, 2)
                // Dewy bright tip
                c.blend(bx + lean, 116 - height, 174, 232, 118)
            }
            // Tiny surreal white meadow wildflowers blooming indoors
            val flowers = listOf(42 to 76, 68 to 68, 86 to 80, 56 to 84, 78 to 72)
            for ((fx, fy) in flowers) {
                c.shadedEllipse(fx.toFloat(), fy.toFloat(), 3.6f, 3.6f, 246, 244, 234, gloss = 0.2f)
                c.shadedEllipse(fx.toFloat(), fy.toFloat(), 1.5f, 1.5f, 236, 196, 62)
            }
        }
    }

    // 9. EXIT SIGN (Normal green EXIT vs Crimson dripping backwards TIXE)
    private fun drawExitSign(c: RealisticCanvas, isAnomaly: Boolean) {
        c.shadedRect(38, 8, 43, 36, 138, 134, 124)
        c.shadedRect(85, 8, 90, 36, 138, 134, 124)
        c.shadedRect(14, 34, 114, 80, 214, 210, 196, verticalShade = true)
        c.shadedRect(18, 38, 110, 76, if (isAnomaly) 48 else 16, if (isAnomaly) 10 else 48, if (isAnomaly) 14 else 24)

        val gr = if (isAnomaly) 244 else 96
        val gg = if (isAnomaly) 54 else 238
        val gb = if (isAnomaly) 62 else 124

        fun drawE(x: Int, y: Int) {
            c.shadedRect(x, y, x + 4, y + 22, gr, gg, gb)
            c.shadedRect(x, y, x + 15, y + 3, gr, gg, gb)
            c.shadedRect(x, y + 9, x + 13, y + 12, gr, gg, gb)
            c.shadedRect(x, y + 19, x + 15, y + 22, gr, gg, gb)
        }
        fun drawX(x: Int, y: Int) {
            c.line(x + 2, y + 1, x + 14, y + 21, gr, gg, gb, 4)
            c.line(x + 14, y + 1, x + 2, y + 21, gr, gg, gb, 4)
        }
        fun drawI(x: Int, y: Int) {
            c.shadedRect(x, y, x + 11, y + 3, gr, gg, gb)
            c.shadedRect(x + 4, y, x + 7, y + 22, gr, gg, gb)
            c.shadedRect(x, y + 19, x + 11, y + 22, gr, gg, gb)
        }
        fun drawT(x: Int, y: Int) {
            c.shadedRect(x, y, x + 15, y + 3, gr, gg, gb)
            c.shadedRect(x + 6, y, x + 9, y + 22, gr, gg, gb)
        }

        if (!isAnomaly) {
            drawE(26, 46); drawX(46, 46); drawI(68, 46); drawT(84, 46)
        } else {
            drawT(26, 46); drawI(46, 46); drawX(62, 46); drawE(84, 46)
            // Dark glossy ink tendrils dripping downward
            c.shadedRect(30, 80, 35, 108, 22, 14, 18)
            c.shadedRect(56, 80, 61, 118, 22, 14, 18)
            c.shadedRect(90, 80, 94, 102, 22, 14, 18)
            c.shadedEllipse(58.5f, 119f, 3f, 3f, 232, 48, 56, gloss = 0.5f)
        }
    }

    // 10. 13-HOUR WALL CLOCK
    private fun drawClock(c: RealisticCanvas, isAnomaly: Boolean) {
        c.shadedEllipse(64f, 60f, 40f, 40f, 48, 48, 46, gloss = 0.35f)
        c.shadedEllipse(64f, 60f, 34f, 34f, if (isAnomaly) 226 else 238, if (isAnomaly) 216 else 236, if (isAnomaly) 152 else 226, gloss = 0.15f)
        for (i in 0 until 12) {
            val a = i * (Math.PI / 6.0)
            val tx = (64 + cos(a) * 28).toInt()
            val ty = (60 + sin(a) * 28).toInt()
            c.shadedEllipse(tx.toFloat(), ty.toFloat(), 2f, 2f, 36, 36, 34)
        }
        if (!isAnomaly) {
            c.line(64, 60, 64, 36, 32, 32, 30, 4)
            c.line(64, 60, 84, 60, 32, 32, 30, 3)
            c.shadedEllipse(64f, 60f, 4f, 4f, 44, 44, 42)
        } else {
            // Bold crimson "13"
            c.shadedRect(54, 30, 57, 44, 212, 32, 38)
            c.shadedRect(62, 30, 72, 33, 212, 32, 38)
            c.shadedRect(69, 30, 72, 44, 212, 32, 38)
            c.shadedRect(64, 36, 72, 38, 212, 32, 38)
            c.shadedRect(62, 41, 72, 44, 212, 32, 38)
            c.line(64, 60, 38, 72, 28, 28, 26, 4)
            c.line(64, 60, 88, 76, 28, 28, 26, 3)
            c.line(64, 60, 46, 44, 212, 32, 38, 2)
            c.shadedEllipse(64f, 60f, 4.5f, 4.5f, 212, 32, 38)
        }
    }

    // 11. CEILING-ROOTED LAMP
    private fun drawLamp(c: RealisticCanvas, isAnomaly: Boolean) {
        if (!isAnomaly) {
            c.groundShadow(64, 121, 24f, 4f, 150)
            c.shadedEllipse(64f, 119f, 20f, 5f, 102, 88, 64, gloss = 0.35f)
            c.shadedRect(60, 48, 68, 118, 138, 124, 96)
            for (y in 16..48) {
                val half = 14 + ((y - 16) * 14) / 32
                c.shadedRect(64 - half, y, 64 + half, y, 232, 220, 174)
            }
            c.shadedRect(46, 47, 82, 51, 250, 242, 198)
        } else {
            c.shadedEllipse(64f, 8f, 20f, 5f, 102, 88, 64, gloss = 0.35f)
            c.shadedRect(60, 10, 68, 78, 138, 124, 96)
            for (y in 78..110) {
                val half = 28 - ((y - 78) * 14) / 32
                c.shadedRect(64 - half, y, 64 + half, y, 224, 214, 168)
            }
            c.shadedRect(42, 74, 86, 79, 178, 240, 154)
        }
    }

    // 12. SIX-FINGERED GLOVE BUCKET
    private fun drawBucket(c: RealisticCanvas, isAnomaly: Boolean) {
        c.groundShadow(64, 120, 28f, 5f, 155)
        for (y in 72..116) {
            val half = 28 - ((y - 72) * 6) / 44
            c.shadedRect(64 - half, y, 64 + half, y, 218, 184, 42)
        }
        c.shadedEllipse(64f, 73f, 26f, 5f, 64, 74, 78)
        if (!isAnomaly) {
            c.line(60, 74, 84, 16, 152, 108, 62, 6)
        } else {
            // Stiff pale yellow latex glove rising from water with 6 unmistakable fingers!
            c.shadedRect(50, 48, 78, 74, 238, 224, 136)
            val tips = listOf(34 to 28, 45 to 18, 56 to 14, 68 to 14, 79 to 18, 92 to 28)
            for ((fx, fy) in tips) {
                c.line(fx, fy, 64 + (fx - 64) / 2, 52, 138, 118, 52, 7)
                c.line(fx, fy + 1, 64 + (fx - 64) / 2, 52, 240, 228, 144, 4)
            }
        }
    }

    // 13. BLACK-WATER COOLER
    private fun drawWaterCooler(c: RealisticCanvas, isAnomaly: Boolean) {
        c.groundShadow(64, 121, 26f, 4.5f, 160)
        c.shadedRect(40, 60, 88, 119, 216, 212, 196)
        c.shadedRect(50, 72, 78, 88, 76, 74, 68)
        c.shadedRect(55, 76, 60, 83, 192, 54, 56)
        c.shadedRect(68, 76, 73, 83, 56, 122, 204)

        if (!isAnomaly) {
            c.shadedRect(42, 18, 86, 59, 108, 184, 220, alpha = 225)
            c.shadedRect(50, 12, 78, 18, 108, 184, 220, alpha = 225)
            c.shadedRect(47, 22, 53, 54, 210, 240, 252, alpha = 160)
        } else {
            c.shadedRect(42, 18, 86, 59, 18, 18, 22)
            c.shadedRect(50, 12, 78, 18, 18, 18, 22)
            c.shadedEllipse(54f, 30f, 4.5f, 4.5f, 112, 240, 128, gloss = 0.6f)
            c.shadedEllipse(72f, 42f, 4f, 4f, 112, 240, 128, gloss = 0.6f)
            c.shadedEllipse(62f, 52f, 2.8f, 2.8f, 112, 240, 128, gloss = 0.6f)
        }
    }

    // 14. TURNING MANNEQUIN
    private fun drawMannequin(c: RealisticCanvas, isAnomaly: Boolean) {
        c.groundShadow(64, 121, 24f, 4.5f, 155)
        c.shadedEllipse(64f, 119f, 20f, 4.5f, 94, 78, 58, gloss = 0.2f)
        c.shadedRect(60, 86, 68, 118, 94, 78, 58, grain = true)
        c.shadedRect(42, 48, 86, 90, 196, 192, 182)
        c.shadedRect(58, 38, 70, 48, 168, 164, 154)
        if (!isAnomaly) {
            c.shadedEllipse(64f, 26f, 13f, 16f, 202, 198, 188, gloss = 0.25f)
        } else {
            c.shadedEllipse(70f, 26f, 13f, 16f, 202, 198, 188, gloss = 0.25f)
            c.shadedEllipse(66f, 24f, 3.6f, 4.2f, 14, 12, 14)
            c.shadedEllipse(76f, 26f, 3.6f, 4.2f, 14, 12, 14)
            c.line(56, 44, 72, 40, 26, 16, 18, 3)
        }
    }

    // 15. STATIC PAYPHONE
    private fun drawPayphone(c: RealisticCanvas, isAnomaly: Boolean) {
        c.shadedRect(44, 28, 92, 96, 146, 150, 146)
        c.shadedRect(48, 32, 88, 92, 46, 48, 52)
        c.shadedRect(58, 48, 82, 70, 212, 210, 198)
        if (!isAnomaly) {
            c.shadedRect(35, 34, 46, 78, 26, 26, 30)
        } else {
            c.shadedRect(14, 40, 25, 84, 22, 22, 26)
            for (i in 0..55) {
                val sx = 18 + (i * 11) % 30
                val sy = 84 + (i * 7) % 34
                val v = if (i % 2 == 0) 240 else 22
                c.shadedRect(sx, sy, sx + 1, sy + 1, v, v, v)
            }
        }
    }

    // 16. RECURSIVE CORRIDOR PAINTING
    private fun drawPainting(c: RealisticCanvas, isAnomaly: Boolean) {
        c.shadedRect(20, 24, 108, 96, 112, 76, 42, grain = true)
        c.shadedRect(25, 29, 103, 91, 154, 114, 66, grain = true)
        if (!isAnomaly) {
            c.shadedRect(30, 34, 98, 64, 126, 174, 212, verticalShade = true)
            c.shadedRect(30, 64, 98, 86, 86, 136, 72, verticalShade = true)
            c.shadedEllipse(84f, 46f, 8f, 8f, 248, 238, 174, gloss = 0.1f)
        } else {
            c.shadedRect(30, 34, 98, 86, 196, 178, 92)
            c.shadedRect(48, 44, 80, 76, 134, 120, 58)
            c.line(30, 34, 48, 44, 92, 82, 38, 2)
            c.line(98, 34, 80, 44, 92, 82, 38, 2)
            c.line(30, 86, 48, 76, 92, 82, 38, 2)
            c.line(98, 86, 80, 76, 92, 82, 38, 2)
            c.shadedEllipse(64f, 52f, 6f, 7f, 12, 12, 14)
            c.shadedRect(58, 58, 70, 82, 12, 12, 14)
            c.blend(62, 51, 242, 242, 232)
            c.blend(66, 51, 242, 242, 232)
        }
    }

    // 17. HOVERING CRT
    private fun drawCrt(c: RealisticCanvas, isAnomaly: Boolean) {
        val yOff = if (isAnomaly) -24 else 0
        if (!isAnomaly) {
            c.groundShadow(64, 120, 32f, 5f, 150)
            c.shadedRect(34, 72, 94, 80, 56, 58, 64)
            c.shadedRect(38, 80, 44, 116, 56, 58, 64)
            c.shadedRect(84, 80, 90, 116, 56, 58, 64)
            c.shadedRect(38, 102, 90, 108, 52, 54, 60)
        } else {
            c.groundShadow(64, 120, 22f, 3.5f, 100)
        }
        c.shadedRect(32, 32 + yOff, 96, 72 + yOff, 198, 192, 170)
        if (!isAnomaly) {
            c.shadedRect(40, 38 + yOff, 88, 64 + yOff, 34, 38, 36)
        } else {
            for (y in (38 + yOff)..(64 + yOff)) {
                for (x in 40..88) {
                    val v = abs(noise(x, y, 503))
                    when {
                        y in (50 + yOff)..(51 + yOff) -> c.blend(x, y, 236, 48, 62)
                        v > 80 -> c.blend(x, y, 234, 242, 234)
                        v > 35 -> c.blend(x, y, 96, 214, 142)
                        else -> c.blend(x, y, 20, 22, 24)
                    }
                }
            }
        }
    }

    // 18. NARROW BLACK DOORWAY
    private fun drawDoorway(c: RealisticCanvas, isAnomaly: Boolean) {
        if (!isAnomaly) {
            c.shadedRect(32, 12, 96, 122, 88, 72, 50, grain = true)
            c.shadedRect(38, 18, 90, 122, 148, 128, 92, grain = true)
            c.shadedRect(46, 28, 82, 62, 126, 108, 76, grain = true)
            c.shadedRect(46, 72, 82, 112, 126, 108, 76, grain = true)
            c.shadedEllipse(80f, 70f, 4f, 4f, 216, 188, 88, gloss = 0.6f)
        } else {
            c.shadedRect(54, 4, 57, 124, 236, 46, 62)
            c.shadedRect(71, 4, 74, 124, 62, 228, 218)
            c.shadedRect(58, 4, 70, 124, 8, 8, 10)
            for (y in 10..118 step 12) {
                c.blend(56, y, 246, 246, 238)
                c.blend(72, y + 5, 246, 246, 238)
            }
        }
    }
}
