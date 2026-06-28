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

    fun apply(entity: LivingEntity, source: String, entries: List<AttributeEntry>, slot: ModifierSlot): Boolean {
        val ok = active().apply(entity, source, entries)
        if (isNativeActive()) AttributeStatStore.tagSlot(entity.uniqueId, source, slot)
        return ok
    }

    fun totalsForHand(entity: LivingEntity, hand: ActionHand): Map<String, Double> {
        val base = AttributeStatStore.totals(entity.uniqueId, hand)
        if (!AttributeModifierStore.has(entity.uniqueId)) return base
        return AttributeModifierStore.foldTotals(entity.uniqueId, base, hand)
    }

    fun remove(entity: LivingEntity, source: String): Boolean =
        active().remove(entity, source)

    fun total(entity: LivingEntity, key: String): Double {
        val base = active().total(entity, key)
        if (!AttributeModifierStore.has(entity.uniqueId)) return base
        val def = AttributeRegistry.resolve(key)
        if (vanillaBacked(def)) return base
        val combined = AttributeModifierStore.combine(entity.uniqueId, key, base)
        if (combined == base) return base
        return def?.clamp(combined) ?: combined
    }

    fun totals(entity: LivingEntity): Map<String, Double> {
        val base = active().totals(entity)
        if (!AttributeModifierStore.has(entity.uniqueId)) return base
        val uuid = entity.uniqueId
        val out = LinkedHashMap<String, Double>(base)
        for (key in AttributeModifierStore.activeKeys(uuid)) out.putIfAbsent(key, 0.0)
        val result = LinkedHashMap<String, Double>(out.size)
        for ((key, value) in out) {
            val def = AttributeRegistry.resolve(key)
            if (vanillaBacked(def)) {
                result[key] = value
                continue
            }
            val combined = AttributeModifierStore.combine(uuid, key, value)
            result[key] = if (combined == value) value else (def?.clamp(combined) ?: combined)
        }
        return result
    }

    private fun vanillaBacked(def: com.qinhuai.corelib.attribute.AttributeDef?): Boolean =
        def != null && isNativeActive() && NativeAttributeBackend.isVanillaMapped(def)

    private fun syncVanilla(entity: LivingEntity) {
        if (isNativeActive()) NativeAttributeBackend.resyncBuffs(entity)
    }

    fun addModifier(entity: LivingEntity, modifier: StatModifier): String {
        val id = AttributeModifierStore.add(entity.uniqueId, modifier)
        syncVanilla(entity)
        return id
    }

    fun buff(
        entity: LivingEntity,
        key: String,
        amount: Double,
        source: String,
        durationTicks: Long = 0L,
        operation: ModifierOp = ModifierOp.FLAT,
    ): String {
        val id = AttributeModifierStore.addTimed(entity.uniqueId, key, amount, operation, source, durationTicks)
        syncVanilla(entity)
        return id
    }

    fun refreshBuff(
        entity: LivingEntity,
        key: String,
        amount: Double,
        source: String,
        durationTicks: Long = 0L,
        operation: ModifierOp = ModifierOp.FLAT,
    ): String {
        AttributeModifierStore.removeSourceKey(entity.uniqueId, source, key)
        val id = AttributeModifierStore.addTimed(entity.uniqueId, key, amount, operation, source, durationTicks)
        syncVanilla(entity)
        return id
    }

    fun removeModifier(entity: LivingEntity, id: String): Boolean {
        val removed = AttributeModifierStore.remove(entity.uniqueId, id)
        if (removed) syncVanilla(entity)
        return removed
    }

    fun removeModifierSource(entity: LivingEntity, source: String): Boolean {
        val removed = AttributeModifierStore.removeSource(entity.uniqueId, source)
        if (removed) syncVanilla(entity)
        return removed
    }

    fun activeModifiers(entity: LivingEntity): List<StatModifier> =
        AttributeModifierStore.active(entity.uniqueId)

    fun displayValue(entity: LivingEntity, def: AttributeDef): Double {
        if (isNativeActive()) {
            NativeAttributeBackend.vanillaEffective(entity, def)?.let { return it }
        }
        return total(entity, def.key)
    }

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
        AttributeModifierStore.clear(player)
        lastEquipActive.remove(player)
        if (isNativeActive()) org.bukkit.Bukkit.getPlayer(player)?.let { NativeAttributeBackend.resyncBuffs(it) }
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
