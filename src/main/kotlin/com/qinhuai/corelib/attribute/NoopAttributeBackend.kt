package com.qinhuai.corelib.attribute

import org.bukkit.entity.LivingEntity

object NoopAttributeBackend : AttributeBackend {
    override val id: String = "noop"
    override fun isAvailable(): Boolean = true
    override fun apply(entity: LivingEntity, source: String, entries: List<AttributeEntry>): Boolean = false
    override fun remove(entity: LivingEntity, source: String): Boolean = false
}
