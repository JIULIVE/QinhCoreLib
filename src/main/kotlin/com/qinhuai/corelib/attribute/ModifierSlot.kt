package com.qinhuai.corelib.attribute

enum class ModifierSlot {
    NONE,
    MAIN_HAND,
    OFF_HAND,
    ARMOR,
    ACCESSORY,
    ;

    companion object {
        fun parse(raw: String?): ModifierSlot = when (raw?.trim()?.lowercase()) {
            "main_hand", "mainhand", "main", "hand" -> MAIN_HAND
            "off_hand", "offhand", "off" -> OFF_HAND
            "armor", "armour" -> ARMOR
            "accessory", "trinket", "ornament" -> ACCESSORY
            else -> NONE
        }
    }
}

enum class ActionHand {
    MAIN,
    OFF,
    ANY,
    ;

    fun accepts(slot: ModifierSlot): Boolean = when (this) {
        ANY -> true
        MAIN -> slot != ModifierSlot.OFF_HAND
        OFF -> slot != ModifierSlot.MAIN_HAND
    }
}
