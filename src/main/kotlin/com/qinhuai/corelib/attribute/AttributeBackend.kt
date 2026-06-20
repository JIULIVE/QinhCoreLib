package com.qinhuai.corelib.attribute

import org.bukkit.entity.LivingEntity

interface AttributeBackend {
    val id: String
    fun isAvailable(): Boolean
    fun apply(entity: LivingEntity, source: String, entries: List<AttributeEntry>): Boolean
    fun remove(entity: LivingEntity, source: String): Boolean
    fun total(entity: LivingEntity, key: String): Double = 0.0
    fun totals(entity: LivingEntity): Map<String, Double> = emptyMap()
}
