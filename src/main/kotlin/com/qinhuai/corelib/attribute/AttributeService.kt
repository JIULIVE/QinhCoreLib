package com.qinhuai.corelib.attribute

import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object AttributeService {

    private val backends = linkedMapOf<String, AttributeBackend>()
    private var configuredId: String = "native"
    private val lastEquipActive = ConcurrentHashMap<UUID, Set<String>>()

    fun init() {
        register(NoopAttributeBackend)
        register(NativeAttributeBackend)
    }

    fun register(backend: AttributeBackend) {
        backends[backend.id] = backend
    }

    fun setConfiguredBackend(id: String) {
        configuredId = id.trim().lowercase().ifEmpty { "native" }
    }

    fun configuredBackend(): String = configuredId

    fun activeId(): String = active().id

    fun active(): AttributeBackend = when (configuredId) {
        "auto" -> backends.values.firstOrNull { it.id != "noop" && it.id != "native" && it.isAvailable() }
            ?: NativeAttributeBackend
        else -> backends[configuredId]?.takeIf { it.isAvailable() } ?: NativeAttributeBackend
    }

    fun isNativeActive(): Boolean = active().id == NativeAttributeBackend.id

    fun hasExternalBackend(): Boolean =
        backends.values.any { it.id != NoopAttributeBackend.id && it.id != NativeAttributeBackend.id && it.isAvailable() }

    fun externalBackendIds(): List<String> =
        backends.values.filter { it.id != NoopAttributeBackend.id && it.id != NativeAttributeBackend.id && it.isAvailable() }.map { it.id }

    fun apply(entity: LivingEntity, source: String, entries: List<AttributeEntry>): Boolean =
        active().apply(entity, source, entries)

    fun remove(entity: LivingEntity, source: String): Boolean =
        active().remove(entity, source)

    fun total(entity: LivingEntity, key: String): Double =
        active().total(entity, key)

    fun totals(entity: LivingEntity): Map<String, Double> =
        active().totals(entity)

    fun combatPower(player: Player): Double {
        var sum = 0.0
        for ((key, value) in totals(player)) {
            sum += value * (AttributeRegistry.resolve(key)?.combatPower ?: 0.0)
        }
        return sum
    }

    fun refreshEquipHooks(player: Player) {
        if (!isNativeActive()) return
        val uuid = player.uniqueId
        val now = LinkedHashSet<String>()
        for (key in AttributeStatStore.activeAttrs(uuid)) {
            val def = AttributeRegistry.resolve(key) ?: continue
            if (def.hook(AttributeHooks.ON_EQUIP) != null || def.hook(AttributeHooks.ON_UNEQUIP) != null) {
                now.add(key)
            }
        }
        val prev = lastEquipActive[uuid] ?: emptySet()
        for (key in now - prev) fireEquip(player, key, AttributeHooks.ON_EQUIP)
        for (key in prev - now) fireEquip(player, key, AttributeHooks.ON_UNEQUIP)
        if (now.isEmpty()) lastEquipActive.remove(uuid) else lastEquipActive[uuid] = now
    }

    fun clearPlayer(player: UUID) {
        AttributeStatStore.clear(player)
        lastEquipActive.remove(player)
    }

    private fun fireEquip(player: Player, attrKey: String, hook: String) {
        val def = AttributeRegistry.resolve(attrKey) ?: return
        val ref = def.hook(hook) ?: return
        val totals = AttributeStatStore.totals(player.uniqueId)
        val vars = HashMap<String, Any>()
        for ((k, v) in totals) vars[k] = v
        vars["attribute"] = attrKey
        vars["value"] = totals[attrKey] ?: 0.0
        AttributeScriptRunner.runEffectHook(ref, player, vars)
    }
}
