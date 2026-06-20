package com.qinhuai.corelib.attribute

enum class DamageType(val displayName: String, val hasCrit: Boolean = true) {
    PHYSICAL("物理伤害"),
    MAGIC("魔法伤害"),
    SKILL("技能伤害"),
    TRUE("真实伤害", hasCrit = false),
    ;

    val path: String get() = name.lowercase()
    val bonusKey: String get() = "${path}_damage"
    val critRateKey: String get() = "${path}_crit_rate"
    val critDamageKey: String get() = "${path}_crit_damage"

    companion object {
        fun fromConfig(token: String?): DamageType? = when (token?.trim()?.lowercase()) {
            "physical", "物理" -> PHYSICAL
            "magic", "法术", "魔法" -> MAGIC
            "skill", "技能" -> SKILL
            "true", "真实" -> TRUE
            else -> null
        }
    }
}
