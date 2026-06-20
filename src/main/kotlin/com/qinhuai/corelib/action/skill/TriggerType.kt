package com.qinhuai.corelib.action.skill

enum class TriggerType {
    RIGHT_CLICK,
    LEFT_CLICK,

    SHIFT_RIGHT_CLICK,
    SHIFT_LEFT_CLICK,

    SHIFT_TOGGLE,
    DOUBLE_SHIFT_TOGGLE,

    DOUBLE_RIGHT_CLICK,
    DOUBLE_LEFT_CLICK,

    HOLD_RIGHT_CLICK,
    HOLD_LEFT_CLICK,

    CI_TEST,
    COMMAND,
    API,
    PASSIVE,
    UNKNOWN,
    ;

    fun legacyActionKey(): String = when (this) {
        RIGHT_CLICK -> "right_click"
        LEFT_CLICK -> "left_click"
        SHIFT_RIGHT_CLICK -> "shift_right_click"
        SHIFT_LEFT_CLICK -> "shift_left_click"
        SHIFT_TOGGLE -> "shift_toggle"
        DOUBLE_SHIFT_TOGGLE -> "double_shift_toggle"
        DOUBLE_RIGHT_CLICK -> "double_right_click"
        DOUBLE_LEFT_CLICK -> "double_left_click"
        HOLD_RIGHT_CLICK -> "hold_right_click"
        HOLD_LEFT_CLICK -> "hold_left_click"
        CI_TEST -> "ci_test"
        COMMAND -> "command"
        API -> "api"
        PASSIVE -> "passive"
        UNKNOWN -> "unknown"
    }

    companion object {
        fun fromLegacy(trigger: String?): TriggerType {
            if (trigger.isNullOrBlank()) return UNKNOWN
            val key = trigger.trim().lowercase()
            return entries.firstOrNull { it.legacyActionKey() == key }
                ?: entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
                ?: when (key) {
                    "right_click_air", "right_click_block" -> RIGHT_CLICK
                    "left_click_air", "left_click_block" -> LEFT_CLICK
                    else -> UNKNOWN
                }
        }
    }
}
