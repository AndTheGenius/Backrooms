package com.example.engine

/**
 * All 8 playable interconnected Backrooms environments, each with distinct procedural wall,
 * floor, and ceiling architecture, atmospheric fog, acoustic ambience, and thematic artifacts.
 */
enum class BackroomsBiome(
    val shortLabel: String,
    val hudLocation: String,
    val entryTitle: String,
    val entryDetail: String,
    val fogR: Int,
    val fogG: Int,
    val fogB: Int,
    val walkSpeed: Double,
    val preferredAnomalyIds: List<String>
) {
    YELLOW_HALLS(
        shortLabel = "LEVEL 0",
        hudLocation = "LOC: LEVEL 0 // YELLOW HALLS",
        entryTitle = "LEVEL 0 — YELLOW CORRIDORS",
        entryDetail = "Damp mono-yellow wallpaper and endless humming fluorescents.",
        fogR = 20, fogG = 18, fogB = 10,
        walkSpeed = 2.75,
        preferredAnomalyIds = listOf(
            "three_legged_chair", "five_eyed_doll", "backwards_exit_sign",
            "inverted_floor_lamp", "stretched_doorway", "recursive_painting"
        )
    ),
    POOLROOMS(
        shortLabel = "POOLROOMS",
        hudLocation = "LOC: LEVEL 37 // POOLROOMS",
        entryTitle = "ENTERED LEVEL 37 — THE POOLROOMS",
        entryDetail = "Glossy white ceramic halls flooded with warm rippling aqua water.",
        fogR = 16, fogG = 48, fogB = 56,
        walkSpeed = 2.35,
        preferredAnomalyIds = listOf(
            "cosmic_mirror", "void_water_cooler", "six_finger_bucket",
            "slenderman_doll", "indoor_tree", "stretched_doorway"
        )
    ),
    AMUSEMENT_PARK(
        shortLabel = "CARNIVAL",
        hudLocation = "LOC: FUN SECTOR // AMUSEMENT PARK",
        entryTitle = "ENTERED FUN SECTOR — AMUSEMENT PARK",
        entryDetail = "Striped carnival booths beneath a dim painted indoor night sky.",
        fogR = 32, fogG = 16, fogB = 34,
        walkSpeed = 2.75,
        preferredAnomalyIds = listOf(
            "five_eyed_doll", "slenderman_doll", "cosmic_mirror",
            "backwards_exit_sign", "thirteen_hour_clock", "watching_mannequin"
        )
    ),
    GYM(
        shortLabel = "GYM",
        hudLocation = "LOC: LEVEL 14 // EMPTY GYM",
        entryTitle = "ENTERED LEVEL 14 — EMPTY GYMNASIUM",
        entryDetail = "Echoing maple hardwood courts, metal lockers, and harsh high-bay lamps.",
        fogR = 22, fogG = 24, fogB = 28,
        walkSpeed = 2.85,
        preferredAnomalyIds = listOf(
            "void_water_cooler", "watching_mannequin", "thirteen_hour_clock",
            "bleeding_payphone", "six_finger_bucket", "three_legged_chair"
        )
    ),
    GARDEN(
        shortLabel = "GARDEN",
        hudLocation = "LOC: OVERGROWN // INDOOR GARDEN",
        entryTitle = "ENTERED CONSERVATORY — INDOOR GARDEN",
        entryDetail = "Moss-covered brick walls, climbing ivy trellises, and damp twilight grass.",
        fogR = 14, fogG = 30, fogB = 22,
        walkSpeed = 2.65,
        preferredAnomalyIds = listOf(
            "indoor_tree", "indoor_grass", "cosmic_mirror",
            "slenderman_doll", "inverted_floor_lamp", "five_eyed_doll"
        )
    ),
    CAMERA_ROOM(
        shortLabel = "CAMERA ROOM",
        hudLocation = "LOC: SURVEILLANCE // CAMERA ROOM",
        entryTitle = "ENTERED SURVEILLANCE — CAMERA ROOM",
        entryDetail = "Banks of buzzing phosphor CCTV monitors watching empty corridors.",
        fogR = 10, fogG = 24, fogB = 16,
        walkSpeed = 2.70,
        preferredAnomalyIds = listOf(
            "floating_crt", "bleeding_payphone", "backwards_exit_sign",
            "recursive_painting", "stretched_doorway", "thirteen_hour_clock"
        )
    ),
    PHARMACY(
        shortLabel = "PHARMACY",
        hudLocation = "LOC: SECTOR RX // EMPTY PHARMACY",
        entryTitle = "ENTERED SECTOR RX — EMPTY PHARMACY",
        entryDetail = "Sterile linoleum aisles lined with unlabeled prescription bottles.",
        fogR = 22, fogG = 34, fogB = 30,
        walkSpeed = 2.75,
        preferredAnomalyIds = listOf(
            "dementia_pills", "void_water_cooler", "thirteen_hour_clock",
            "watching_mannequin", "six_finger_bucket", "inverted_floor_lamp"
        )
    ),
    ANTIQUE_SHOP(
        shortLabel = "ANTIQUE SHOP",
        hudLocation = "LOC: CURIO WING // ANTIQUE SHOP",
        entryTitle = "ENTERED CURIO WING — ANTIQUE SHOP",
        entryDetail = "Mahogany shelves, faded burgundy damask walls, and dusty relics.",
        fogR = 28, fogG = 18, fogB = 12,
        walkSpeed = 2.65,
        preferredAnomalyIds = listOf(
            "old_vinyl_player", "cosmic_mirror", "five_eyed_doll",
            "three_legged_chair", "recursive_painting", "thirteen_hour_clock"
        )
    )
}
