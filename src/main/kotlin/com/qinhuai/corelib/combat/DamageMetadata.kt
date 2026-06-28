package com.qinhuai.corelib.combat

import com.qinhuai.corelib.attribute.DamageType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageEvent

class DamageMetadata(
    val attacker: LivingEntity?,
    val victim: LivingEntity,
    val cause: EntityDamageEvent.DamageCause,
    val damageType: DamageType,
    val baseDamage: Double,
    var finalDamage: Double,
    val projectile: Boolean,
    var crit: Boolean,
    val pvp: Boolean,
    val penetration: Double,
    val trace: List<String>,
    val tags: MutableSet<String> = mutableSetOf(),
    val packets: MutableList<DamagePacket> = mutableListOf(),
) {

    @JvmOverloads
    fun addPacket(
        value: Double,
        damageType: DamageType,
        element: String? = null,
        crit: Boolean = false,
        source: String? = null,
    ): DamagePacket {
        val packet = DamagePacket(value, damageType, element, crit, source)
        packets.add(packet)
        if (value.isFinite()) finalDamage = (finalDamage + value).coerceAtLeast(0.0)
        return packet
    }

    fun recomputeFromPackets() {
        val sum = packets.sumOf { it.value }
        if (sum.isFinite()) finalDamage = sum.coerceAtLeast(0.0)
    }

    fun scaleAll(factor: Double) {
        if (!factor.isFinite() || factor < 0.0) return
        for (p in packets) p.value *= factor
        finalDamage = (finalDamage * factor).coerceAtLeast(0.0)
    }

    fun scaleType(damageType: DamageType, factor: Double) =
        scaleMatching(factor) { it.damageType == damageType }

    fun scaleElement(elementId: String, factor: Double) =
        scaleMatching(factor) { it.element.equals(elementId, ignoreCase = true) }

    private inline fun scaleMatching(factor: Double, predicate: (DamagePacket) -> Boolean) {
        if (!factor.isFinite() || factor < 0.0) return
        var delta = 0.0
        for (p in packets) {
            if (!predicate(p)) continue
            val before = p.value
            p.value *= factor
            delta += p.value - before
        }
        if (delta.isFinite()) finalDamage = (finalDamage + delta).coerceAtLeast(0.0)
    }

    fun damageOf(damageType: DamageType): Double =
        packets.filter { it.damageType == damageType }.sumOf { it.value }

    fun damageOfElement(elementId: String): Double =
        packets.filter { it.element.equals(elementId, ignoreCase = true) }.sumOf { it.value }

    fun elementPackets(): List<DamagePacket> = packets.filter { it.element != null }
}
