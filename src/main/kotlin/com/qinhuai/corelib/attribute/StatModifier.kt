package com.qinhuai.corelib.attribute

import org.bukkit.attribute.AttributeModifier
import java.util.UUID

enum class ModifierOp {
    FLAT,
    RELATIVE,
    MULTIPLY,
    ;

    fun toBukkit(): AttributeModifier.Operation = when (this) {
        FLAT -> AttributeModifier.Operation.ADD_NUMBER
        RELATIVE -> AttributeModifier.Operation.ADD_SCALAR
        MULTIPLY -> AttributeModifier.Operation.MULTIPLY_SCALAR_1
    }

    companion object {
        fun parse(raw: String?): ModifierOp = when (raw?.trim()?.lowercase()) {
            "flat", "add", "+" -> FLAT
            "relative", "percent", "pct", "%" -> RELATIVE
            "multiply", "mul", "compound", "x", "*" -> MULTIPLY
            else -> FLAT
        }
    }
}

data class StatModifier(
    val key: String,
    val amount: Double,
    val operation: ModifierOp = ModifierOp.FLAT,
    val source: String = "unknown",
    val expireAtMillis: Long? = null,
    val slot: ModifierSlot = ModifierSlot.NONE,
    val id: String = UUID.randomUUID().toString(),
) {
    val isTemporary: Boolean get() = expireAtMillis != null

    fun isExpired(now: Long): Boolean = expireAtMillis != null && expireAtMillis <= now

    fun remainingMillis(now: Long): Long = expireAtMillis?.let { maxOf(0L, it - now) } ?: -1L
}
