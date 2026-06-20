package com.qinhuai.corelib.attribute

data class AttributeDef(
    val key: String,
    val displayName: String,
    val prefix: String? = null,
    val category: String = "杂项",
    val vanillaKey: String? = null,
    val itemAttribute: String? = null,
    val damageType: DamageType? = null,
    val order: Int = 0,
    val mitigation: Boolean = false,
    val combatPower: Double = 1.0,
    val min: Double? = null,
    val max: Double? = null,
    val message: String? = null,
    val hooks: Map<String, String> = emptyMap(),
) {
    val appliesVanilla: Boolean get() = vanillaKey != null
    val isItemAttribute: Boolean get() = itemAttribute != null
    val isScripted: Boolean get() = hooks.isNotEmpty()

    val isStored: Boolean get() = vanillaKey == null && itemAttribute == null

    fun hook(event: String): String? = hooks[event.trim().lowercase()]

    fun clamp(value: Double): Double {
        var v = value
        min?.let { v = maxOf(v, it) }
        max?.let { v = minOf(v, it) }
        return v
    }
}
