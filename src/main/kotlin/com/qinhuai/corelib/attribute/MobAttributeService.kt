package com.qinhuai.corelib.attribute

import com.qinhuai.corelib.util.ServerCompat
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

object MobAttributeService {

    private const val MYTHIC_SOURCE = "qcl:mob:mythic"

    fun applyMapped(entity: LivingEntity, attrs: Map<String, Double>) {
        if (attrs.isEmpty()) {
            AttributeService.remove(entity, MYTHIC_SOURCE)
            return
        }
        AttributeService.apply(entity, MYTHIC_SOURCE, attrs.map { (k, v) -> AttributeEntry.scalar(k, v) })
    }

    fun setAttributes(entity: LivingEntity, attrs: Map<String, Double>) {
        if (attrs.isEmpty()) {
            AttributeService.remove(entity, "qcl:mob:api")
            return
        }
        AttributeService.apply(entity, "qcl:mob:api", attrs.map { (k, v) -> AttributeEntry.scalar(k, v) })
    }

    fun setSingle(entity: LivingEntity, key: String, value: Double) {
        AttributeService.apply(entity, "qcl:mob:api:${key.trim().lowercase()}", listOf(AttributeEntry.scalar(key, value)))
    }

    fun clear(entity: LivingEntity) {
        AttributeService.clearPlayer(entity.uniqueId)
    }

    fun fullHealToMax(entity: LivingEntity) {
        if (entity is Player) return
        val max = entity.getAttribute(ServerCompat.ATTR_MAX_HEALTH)?.value ?: return
        if (max > 0.0 && entity.health < max) entity.health = max
    }
}
