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
        val stored = AttributeStatStore.total(entity.uniqueId, key)
        val vanillaKey = AttributeRegistry.resolve(key)?.vanillaKey ?: return stored
        val vanilla = resolveVanilla(vanillaKey) ?: return stored
        val instance = entity.getAttribute(vanilla) ?: return stored
        val fromVanilla = instance.modifiers.filter { it.key.namespace == "qinh" }.sumOf { it.amount }
        return stored + fromVanilla
    }

    override fun totals(entity: LivingEntity): Map<String, Double> {
        val out = LinkedHashMap<String, Double>()
        for ((k, v) in AttributeStatStore.totals(entity.uniqueId)) if (v != 0.0) out[k] = v
        for (def in AttributeRegistry.all()) {
            val vanilla = def.vanillaKey?.let { resolveVanilla(it) } ?: continue
            val instance = entity.getAttribute(vanilla) ?: continue
            val sum = instance.modifiers.filter { it.key.namespace == "qinh" }.sumOf { it.amount }
            if (sum != 0.0) out[def.key] = (out[def.key] ?: 0.0) + sum
        }
        return out
    }

    private fun resolveVanilla(vanillaKey: String): Attribute? =
        Registry.ATTRIBUTE.get(NamespacedKey.minecraft(vanillaKey))

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
            val mods = instance.modifiers.filter { it.key.namespace == "qinh" }
            if (mods.isEmpty()) continue
            val sum = mods.sumOf { it.amount }
            out.add(Lang.get("native-attribute-backend.debug-entry", "name" to def.displayName, "key" to def.key, "sum" to sum, "count" to mods.size, "base" to instance.baseValue, "final" to instance.value))
        }
        return out
    }
}
