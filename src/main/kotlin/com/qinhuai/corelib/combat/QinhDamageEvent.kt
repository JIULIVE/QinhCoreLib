package com.qinhuai.corelib.combat

import com.qinhuai.corelib.attribute.DamageType
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class QinhDamageEvent(val metadata: DamageMetadata) : Event(), Cancellable {

    private var cancelled = false

    var damage: Double
        get() = metadata.finalDamage
        set(value) {
            metadata.finalDamage = value
        }

    @JvmOverloads
    fun addPacket(value: Double, damageType: DamageType, element: String? = null, crit: Boolean = false, source: String? = null) =
        metadata.addPacket(value, damageType, element, crit, source)

    fun scaleAll(factor: Double) = metadata.scaleAll(factor)

    fun scaleType(damageType: DamageType, factor: Double) = metadata.scaleType(damageType, factor)

    fun scaleElement(elementId: String, factor: Double) = metadata.scaleElement(elementId, factor)

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        @JvmStatic
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
