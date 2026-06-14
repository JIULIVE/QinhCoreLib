package com.qinhuai.corelib.item

enum class ItemSourceType(val id: String, val aliases: List<String>) {
    VANILLA("vanilla", listOf("vanilla", "minecraft", "mc", "v", "material", "type")),
    CRAFTENGINE("craftengine", listOf("craftengine", "ce", "craft-engine")),
    MMOITEMS("mmoitems", listOf("mmoitems", "mi")),
    NEIGEITEMS("neigeitems", listOf("neigeitems", "ni")),
    QINHITEMS("qinhitems", listOf("qinhitems", "qi")),
    MYTHICMOBS("mythicmobs", listOf("mythicmobs", "mm", "mythic")),
    CUSTOMFISHING("customfishing", listOf("customfishing", "cf")),
    MAGICGEM("magicgem", listOf("magicgem", "mg", "magic-gem")),
    ITEMSADDER("itemsadder", listOf("itemsadder", "ia")),
    NEXO("nexo", listOf("nexo", "nx")),
    ;

    companion object {
        fun fromId(id: String): ItemSourceType? {
            val lower = id.lowercase()
            return values().find { it.id == lower || it.aliases.contains(lower) }
        }
    }
}
