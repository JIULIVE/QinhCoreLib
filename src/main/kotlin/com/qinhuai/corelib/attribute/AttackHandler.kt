package com.qinhuai.corelib.attribute

import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageEvent
import java.util.concurrent.CopyOnWriteArrayList

interface AttackHandler {
    val id: String

    fun handles(event: EntityDamageEvent): Boolean

    fun resolveAttacker(event: EntityDamageEvent): LivingEntity?

    fun isProjectile(event: EntityDamageEvent): Boolean = false
}

object AttackHandlerRegistry {

    private val handlers = CopyOnWriteArrayList<AttackHandler>()

    fun register(handler: AttackHandler) {
        handlers.removeIf { it.id == handler.id }
        handlers.add(handler)
    }

    fun unregister(id: String): Boolean = handlers.removeIf { it.id == id }

    fun isEmpty(): Boolean = handlers.isEmpty()

    fun match(event: EntityDamageEvent): AttackHandler? {
        if (handlers.isEmpty()) return null
        return handlers.firstOrNull { runCatching { it.handles(event) }.getOrDefault(false) }
    }
}
