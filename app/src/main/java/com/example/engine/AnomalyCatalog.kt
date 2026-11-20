package com.example.engine

enum class AnomalyCategory(val label: String) {
    FURNITURE("FURNITURE"),
    TOY("ARTIFACT"),
    SIGNAGE("SIGNAGE"),
    APPLIANCE("APPLIANCE"),
    ARCHITECTURE("SPATIAL"),
    BOTANICAL("ORGANIC")
}

data class AnomalySpec(
    val id: String,
    val anomalyTitle: String,
    val normalTitle: String,
    val category: AnomalyCategory,
    val minDistortionTier: Int,
    val normalBaseline: String,
    val anomalyTell: String,
    val fieldNote: String,
    val scale: Float = 0.85f,
    val verticalOffset: Float = 0.08f
)

data class DistortionStage(
    val tier: Int,
    val code: String,
    val title: String,
    val minDistanceMeters: Float,
    val summary: String
)

object AnomalyCatalog {

    val stages: List<DistortionStage> = listOf(
        DistortionStage(
            tier = 1,
            code = "TIER I",
            title = "LIMINAL BASELINE",
            minDistanceMeters = 0f,
            summary = "Standard Level 0 damp mono-yellow corridors and tiled pool wings."
        ),
        DistortionStage(
            tier = 2,
            code = "TIER II",
            title = "UNCANNY DRIFT",
            minDistanceMeters = 55f,
            summary = "Ballasts flicker out of phase. Props shift subtly when outside camera FOV."
        ),
        DistortionStage(
            tier = 3,
            code = "TIER III",
            title = "SPATIAL SLIPPAGE",
            minDistanceMeters = 135f,
            summary = "Non-Euclidean geometry active. Corridors silently reconfigure behind you."
        ),
        DistortionStage(
            tier = 4,
            code = "TIER IV",
            title = "REALITY FRACTURE",
            minDistanceMeters = 240f,
            summary = "Wall proportions undulate. Watching wallpaper eyes and VHS tears manifest."
        ),
        DistortionStage(
            tier = 5,
            code = "TIER V",
            title = "DEEP COLLAPSE",
            minDistanceMeters = 360f,
            summary = "Severe chromatic shear, surreal geometry drift, and acoustic detuning."
        )
    )

    val all: List<AnomalySpec> = listOf(
        AnomalySpec(
            id = "three_legged_chair",
            anomalyTitle = "Three-Legged Chair",
            normalTitle = "Wooden Office Chair",
            category = AnomalyCategory.FURNITURE,
            minDistortionTier = 1,
            normalBaseline = "Four-legged oak office chair standing evenly on the damp carpet.",
            anomalyTell = "Rear-right leg is completely severed; the chair balances upright over empty air.",
            fieldNote = "Tape 04 operator tried tipping it over. It stayed anchored to thin air.",
            scale = 0.84f,
            verticalOffset = 0.12f
        ),
        AnomalySpec(
            id = "five_eyed_doll",
            anomalyTitle = "Five-Eyed Doll",
            normalTitle = "Porcelain Doll",
            category = AnomalyCategory.TOY,
            minDistortionTier = 1,
            normalBaseline = "Vintage porcelain doll in a blue dress with two symmetrical dark eyes.",
            anomalyTell = "Five glossy pitch-black eyes clustered across its pale porcelain face.",
            fieldNote = "Zooming to 2.0x causes all five pupils to track the camcorder lens.",
            scale = 0.72f,
            verticalOffset = 0.22f
        ),
        AnomalySpec(
            id = "cosmic_mirror",
            anomalyTitle = "Dimensional Mirror",
            normalTitle = "Standing Hallway Mirror",
            category = AnomalyCategory.ARCHITECTURE,
            minDistortionTier = 1,
            normalBaseline = "Brass-framed full-length mirror reflecting ordinary damp yellow walls.",
            anomalyTell = "Mirror glass opens into swirling deep cosmos, violet nebulas, and distant dimensions.",
            fieldNote = "Thermal scans read absolute zero inside the mirror frame.",
            scale = 1.02f,
            verticalOffset = 0.02f
        ),
        AnomalySpec(
            id = "slenderman_doll",
            anomalyTitle = "Slenderman Doll",
            normalTitle = "Collector Suit Figurine",
            category = AnomalyCategory.TOY,
            minDistortionTier = 1,
            normalBaseline = "Small dressed figurine in a neat charcoal jacket with painted facial features.",
            anomalyTell = "Elongated, completely blank faceless doll in a black suit with crimson tie and dark tendrils.",
            fieldNote = "Tape tracking destabilizes whenever the figure fills more than half the frame.",
            scale = 0.86f,
            verticalOffset = 0.12f
        ),
        AnomalySpec(
            id = "old_vinyl_player",
            anomalyTitle = "Old Vinyl Player",
            normalTitle = "Antique Turntable",
            category = AnomalyCategory.APPLIANCE,
            minDistortionTier = 1,
            normalBaseline = "Walnut vintage record player with a still black LP and resting brass tonearm.",
            anomalyTell = "Spinning cracked vinyl glowing crimson while the brass needle hovers mid-air without power.",
            fieldNote = "Plays faint slowed 1930s ballroom static audible even with no power cord attached.",
            scale = 0.76f,
            verticalOffset = 0.18f
        ),
        AnomalySpec(
            id = "dementia_pills",
            anomalyTitle = "Dementia Pills",
            normalTitle = "Vitamin Bottle",
            category = AnomalyCategory.TOY,
            minDistortionTier = 1,
            normalBaseline = "Standard amber pharmacy bottle with a clean white label resting on the floor.",
            anomalyTell = "Scratched prescription bottle labeled 'FORGET' with pale capsules hovering slightly above it.",
            fieldNote = "Operators who read the dosage label repeatedly forget which hallway they entered from.",
            scale = 0.62f,
            verticalOffset = 0.28f
        ),
        AnomalySpec(
            id = "indoor_tree",
            anomalyTitle = "Hallway Birch Tree",
            normalTitle = "Potted Office Ficus",
            category = AnomalyCategory.BOTANICAL,
            minDistortionTier = 1,
            normalBaseline = "Dusty potted office ficus plant sitting in a plastic planter.",
            anomalyTell = "Full-sized gnarled birch tree erupting straight out of torn carpet, roots gripping the floor.",
            fieldNote = "No soil exists beneath the carpet fibers; the bark is warm and slightly damp.",
            scale = 1.12f,
            verticalOffset = 0.0f
        ),
        AnomalySpec(
            id = "indoor_grass",
            anomalyTitle = "Overgrown Carpet Grass",
            normalTitle = "Green Welcome Mat",
            category = AnomalyCategory.BOTANICAL,
            minDistortionTier = 1,
            normalBaseline = "Flat rectangular green institutional rubber mat lying neatly on the floor.",
            anomalyTell = "Thick patch of dew-covered wild meadow grass and tiny white blossoms growing out of carpet.",
            fieldNote = "Smells faintly of summer rain despite miles of windowless fluorescent halls.",
            scale = 0.84f,
            verticalOffset = 0.24f
        ),
        AnomalySpec(
            id = "backwards_exit_sign",
            anomalyTitle = "Backwards Exit Sign",
            normalTitle = "Hallway Exit Sign",
            category = AnomalyCategory.SIGNAGE,
            minDistortionTier = 1,
            normalBaseline = "Standard illuminated green EXIT placard mounted straight.",
            anomalyTell = "Glowing crimson sign spelling TIXE backwards while dripping dark static ink.",
            fieldNote = "Following a TIXE placard only loops back to deeper rooms.",
            scale = 0.78f,
            verticalOffset = -0.18f
        ),
        AnomalySpec(
            id = "thirteen_hour_clock",
            anomalyTitle = "13-Hour Wall Clock",
            normalTitle = "Office Wall Clock",
            category = AnomalyCategory.APPLIANCE,
            minDistortionTier = 1,
            normalBaseline = "Clean round office clock with 12 hour markers reading 3:00.",
            anomalyTell = "Warped yellowed dial displaying 13 at the crown with three bent hands.",
            fieldNote = "Audio logs capture 61 ticks per minute within two meters of the dial.",
            scale = 0.72f,
            verticalOffset = -0.12f
        ),
        AnomalySpec(
            id = "inverted_floor_lamp",
            anomalyTitle = "Ceiling-Rooted Lamp",
            normalTitle = "Standing Floor Lamp",
            category = AnomalyCategory.FURNITURE,
            minDistortionTier = 1,
            normalBaseline = "Beige standing lamp resting upright on a circular floor base.",
            anomalyTell = "Mounted completely upside-down from the ceiling tiles, shining upward.",
            fieldNote = "No wiring penetrates the acoustic tile; the lampshade stays ice cold.",
            scale = 0.96f,
            verticalOffset = -0.05f
        ),
        AnomalySpec(
            id = "six_finger_bucket",
            anomalyTitle = "Six-Fingered Glove",
            normalTitle = "Janitor Mop Bucket",
            category = AnomalyCategory.TOY,
            minDistortionTier = 2,
            normalBaseline = "Yellow caution mop bucket holding an ordinary wooden mop handle.",
            anomalyTell = "Stiff yellow rubber glove with six splayed fingers reaching out of the bucket.",
            fieldNote = "No custodial crew has ever been logged inside Level 0.",
            scale = 0.76f,
            verticalOffset = 0.16f
        ),
        AnomalySpec(
            id = "void_water_cooler",
            anomalyTitle = "Black-Water Cooler",
            normalTitle = "Office Water Cooler",
            category = AnomalyCategory.APPLIANCE,
            minDistortionTier = 2,
            normalBaseline = "Beige dispenser topped with a clear blue office water jug.",
            anomalyTell = "Jug filled with pitch-black static fluid and glowing phosphor-green bubbles.",
            fieldNote = "Bubbles sink slowly toward the tap instead of rising.",
            scale = 0.92f,
            verticalOffset = 0.06f
        ),
        AnomalySpec(
            id = "watching_mannequin",
            anomalyTitle = "Turning Mannequin",
            normalTitle = "Display Mannequin",
            category = AnomalyCategory.FURNITURE,
            minDistortionTier = 2,
            normalBaseline = "Blank grey retail mannequin standing straight on a wooden stand.",
            anomalyTell = "Tilted head with hollow black eye sockets and a fractured collar seam.",
            fieldNote = "Orientation shifts 15 degrees closer whenever the operator looks away.",
            scale = 0.98f,
            verticalOffset = 0.03f
        ),
        AnomalySpec(
            id = "bleeding_payphone",
            anomalyTitle = "Static Payphone",
            normalTitle = "Corridor Payphone",
            category = AnomalyCategory.APPLIANCE,
            minDistortionTier = 2,
            normalBaseline = "Institutional wall payphone with receiver resting on the metal hook.",
            anomalyTell = "Handset levitates beside the box while monochrome snow spills from the speaker.",
            fieldNote = "Dial tone matches the exact 60Hz hum of the fluorescent ballasts.",
            scale = 0.78f,
            verticalOffset = -0.04f
        ),
        AnomalySpec(
            id = "recursive_painting",
            anomalyTitle = "Corridor Painting",
            normalTitle = "Framed Landscape",
            category = AnomalyCategory.SIGNAGE,
            minDistortionTier = 2,
            normalBaseline = "Muted corporate oil painting depicting blue skies over rolling green hills.",
            anomalyTell = "Canvas depicts this exact yellow hallway with a dark silhouette watching you.",
            fieldNote = "Each time the painting reappears deeper in Level 0, the silhouette is closer.",
            scale = 0.84f,
            verticalOffset = -0.06f
        ),
        AnomalySpec(
            id = "floating_crt",
            anomalyTitle = "Hovering CRT",
            normalTitle = "Rolling AV Monitor",
            category = AnomalyCategory.APPLIANCE,
            minDistortionTier = 3,
            normalBaseline = "Unplugged beige 1990s CRT monitor sitting squarely on a rolling metal cart.",
            anomalyTell = "Suspended mid-air with no cart, radiating harsh VHS snow and crimson tear bars.",
            fieldNote = "Occasionally flashes the operator's own live timecode.",
            scale = 0.88f,
            verticalOffset = 0.0f
        ),
        AnomalySpec(
            id = "stretched_doorway",
            anomalyTitle = "Narrow Black Door",
            normalTitle = "Utility Door",
            category = AnomalyCategory.ARCHITECTURE,
            minDistortionTier = 3,
            normalBaseline = "Standard-width beige office utility door with a round brass handle.",
            anomalyTell = "Impossibly narrow ceiling-high black void slit framed in jagged RGB static.",
            fieldNote = "Laser rangefinders return infinite distance through the open slit.",
            scale = 1.04f,
            verticalOffset = 0.0f
        )
    )

    private val byId: Map<String, AnomalySpec> = all.associateBy { it.id }

    fun getById(id: String): AnomalySpec? = byId[id]

    fun stageForDistance(distanceMeters: Float): DistortionStage {
        return stages.lastOrNull { distanceMeters >= it.minDistanceMeters } ?: stages.first()
    }

    fun distortionIntensity(distanceMeters: Float): Float {
        return (distanceMeters / 420f).coerceIn(0f, 1f)
    }
}
