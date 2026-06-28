package com.qinhuai.corelib.attribute

import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import com.qinhuai.corelib.lang.Lang

object NativeAttributeBackend : AttributeBackend {

    override val id: String = "native"

    private const val BUFF_NAMESPACE = "qinhbuff"

    override fun isAvailable(): Boolean = true

    override fun apply(entity: LivingEntity, source: String, entries: List<AttributeEntry>): Boolean {
        remove(entity, source)
        var applied = false
        val tracked = HashMap<String, Double>()
        for (entry in entries) {
            val def = entry.def ?: continue
            def.vanillaKey?.let { vanillaKey ->
                val vanilla = resolveVanilla(vanillaKey)
                val instance = vanilla?.let { entity.getAttribute(it) }
                if (instance != null) {
                    instance.addModifier(
                        AttributeModifier(modifierKey(source, def), entry.scalar(), AttributeModifier.Operation.ADD_NUMBER),
                    )
                    applied = true
                }
            }
            if (def.isStored || def.itemAttribute != null) {
                tracked[def.key] = (tracked[def.key] ?: 0.0) + entry.scalar()
            }
        }
        AttributeStatStore.set(entity.uniqueId, source, tracked)
        if (tracked.isNotEmpty()) applied = true
        return applied
    }

    override fun remove(entity: LivingEntity, source: String): Boolean {
        var removed = false
        for (def in AttributeRegistry.all()) {
            val vanillaKey = def.vanillaKey ?: continue
            val vanilla = resolveVanilla(vanillaKey) ?: continue
            val instance = entity.getAttribute(vanilla) ?: continue
            val key = modifierKey(source, def)
            val existing = instance.modifiers.firstOrNull { it.key == key } ?: continue
            instance.removeModifier(existing)
            removed = true
        }
        AttributeStatStore.removeSource(entity.uniqueId, source)
        return removed
    }

    override fun total(entity: LivingEntity, key: String): Double {
        val def = AttributeRegistry.resolve(key)
        val stored = AttributeStatStore.total(entity.uniqueId, key)
        val vanilla = def?.let { vanillaAttributeOf(it) } ?: return stored
        val instance = entity.getAttribute(vanilla) ?: return stored
        return instance.value
    }

    override fun totals(entity: LivingEntity): Map<String, Double> {
        val out = LinkedHashMap<String, Double>()
        for ((k, v) in AttributeStatStore.totals(entity.uniqueId)) if (v != 0.0) out[k] = v
        for (def in AttributeRegistry.all()) {
            val vanilla = vanillaAttributeOf(def) ?: continue
            val instance = entity.getAttribute(vanilla) ?: continue
            val v = instance.value
            if (v != 0.0) out[def.key] = v else out.remove(def.key)
        }
        return out
    }

    fun isVanillaMapped(def: AttributeDef): Boolean = vanillaAttributeOf(def) != null

    private fun vanillaAttributeOf(def: AttributeDef): Attribute? {
        val key = def.vanillaKey ?: def.itemAttribute ?: return null
        return resolveVanilla(key)
    }

    fun resyncBuffs(entity: LivingEntity) {
        val active = AttributeModifierStore.active(entity.uniqueId)
        for (def in AttributeRegistry.all()) {
            val vanilla = vanillaAttributeOf(def) ?: continue
            val instance = entity.getAttribute(vanilla) ?: continue
            val desired = active.filter { it.key == def.key }
            val desiredKeys = desired.map { buffModifierKey(it).key }.toHashSet()
            val existing = instance.modifiers.filter { it.key.namespace == BUFF_NAMESPACE }
            val existingKeys = HashSet<String>()
            for (m in existing) {
                if (m.key.key !in desiredKeys) instance.removeModifier(m) else existingKeys.add(m.key.key)
            }
            for (mod in desired) {
                val key = buffModifierKey(mod)
                if (key.key in existingKeys) continue
                instance.addModifier(AttributeModifier(key, mod.amount, mod.operation.toBukkit()))
            }
        }
    }

    private fun buffModifierKey(mod: StatModifier): NamespacedKey =
        NamespacedKey(BUFF_NAMESPACE, "b_" + mod.id.lowercase().replace(Regex("[^a-z0-9_.-]"), "_"))

    fun vanillaEffective(entity: LivingEntity, def: AttributeDef): Double? {
        val vanilla = vanillaAttributeOf(def) ?: return null
        val instance = entity.getAttribute(vanilla) ?: return null
        return instance.value
    }

    private fun resolveVanilla(vanillaKey: String): Attribute? =
        runCatching { Registry.ATTRIBUTE.get(NamespacedKey.minecraft(vanillaKey)) }.getOrNull()

    private fun modifierKey(source: String, def: AttributeDef): NamespacedKey {
        val safe = "${source}_${def.key}".lowercase().replace(Regex("[^a-z0-9_.-]"), "_")
        return NamespacedKey("qinh", safe)
    }

    fun debugVanilla(player: Player): List<String> {
        val out = mutableListOf<String>()
        for (def in AttributeRegistry.all()) {
            val vanillaKey = def.vanillaKey ?: continue
            val vanilla = resolveVanilla(vanillaKey) ?: continue
            val instance = player.getAttribute(vanilla) ?: continue
            val mods = instance.modifiers.filter { it.key.namespace.startsWith("qinh") }
            if (mods.isEmpty()) continue
            val sum = mods.sumOf { it.amount }
            out.add(Lang.get("native-attribute-backend.debug-entry", "name" to def.displayName, "key" to def.key, "sum" to sum, "count" to mods.size, "base" to instance.baseValue, "final" to instance.value))
        }
        return out
    }
}
