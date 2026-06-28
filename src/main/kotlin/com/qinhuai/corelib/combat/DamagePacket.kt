package com.qinhuai.corelib.combat

import com.qinhuai.corelib.attribute.DamageType

data class DamagePacket(
    var value: Double,
    val damageType: DamageType,
    val element: String? = null,
    var crit: Boolean = false,
    val source: String? = null,
) {
    val styleKey: String
        get() = element ?: when (damageType) {
            DamageType.PHYSICAL -> "physical"
            DamageType.MAGIC, DamageType.SKILL -> "magic"
            DamageType.TRUE -> "true"
        }
}
