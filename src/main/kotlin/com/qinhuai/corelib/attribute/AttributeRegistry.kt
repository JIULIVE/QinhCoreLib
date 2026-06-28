package com.qinhuai.corelib.attribute

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.lang.Lang
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.attribute.Attribute
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object AttributeRegistry {

    private val byKey = linkedMapOf<String, AttributeDef>()
    private val byDisplay = linkedMapOf<String, AttributeDef>()
    private val byDamageType = linkedMapOf<DamageType, AttributeDef>()
    private val byAlias = linkedMapOf<String, AttributeDef>()

    fun reset() {
        byKey.clear()
        byDisplay.clear()
        byDamageType.clear()
        byAlias.clear()
        BuiltinAttributes.all().forEach { register(it) }
        AttributeStatStore.invalidateAll()
    }

    fun register(def: AttributeDef) {
        byKey[def.key.trim().lowercase()] = def
        byDisplay[def.displayName.trim()] = def
        def.damageType?.let { byDamageType.putIfAbsent(it, def) }
        def.vanillaKey?.trim()?.lowercase()?.let { if (it != def.key.trim().lowercase()) byAlias.putIfAbsent(it, def) }
        def.itemAttribute?.trim()?.lowercase()?.let { if (it != def.key.trim().lowercase()) byAlias.putIfAbsent(it, def) }
    }

    fun resolve(token: String): AttributeDef? {
        val t = token.trim()
        if (t.isEmpty()) return null
        return byKey[t.lowercase()] ?: byDisplay[t] ?: byAlias[t.lowercase()]
    }

    fun all(): Collection<AttributeDef> = byKey.values

    fun baseAttrFor(type: DamageType): AttributeDef? = byDamageType[type]

    fun loadCustom() {
        val file = File(QinhCoreLib.instance.dataFolder, "attributes.yml")
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)
        val section = yaml.getConfigurationSection("attributes") ?: return
        val logger = QinhCoreLib.instance.logger
        var count = 0
        for (key in section.getKeys(false)) {
            val node = section.getConfigurationSection(key) ?: continue
            val label = "attributes.$key"
            val base = byKey[key.trim().lowercase()]
            val display = node.getString("display") ?: base?.displayName ?: key
            val prefix = node.getString("prefix")?.trim()?.takeIf { it.isNotEmpty() } ?: base?.prefix

            var vanilla = node.getString("vanilla")?.trim()?.takeIf { it.isNotEmpty() } ?: base?.vanillaKey
            if (vanilla != null && resolveVanillaKey(vanilla) == null) {
                logger.warning(Lang.get("attribute-registry.vanilla-key-not-found", "label" to label, "vanilla" to vanilla))
                vanilla = null
            }

            val typeToken = node.getString("type")?.trim()?.takeIf { it.isNotEmpty() }
            val damageType = DamageType.fromConfig(typeToken) ?: base?.damageType
            if (typeToken != null && DamageType.fromConfig(typeToken) == null) {
                logger.warning(Lang.get("attribute-registry.unknown-damage-type", "label" to label, "type" to typeToken))
            }

            val order = node.getInt("order", base?.order ?: 0)
            val hooks = linkedMapOf<String, String>()
            node.getConfigurationSection("hooks")?.let { h ->
                for (event in h.getKeys(false)) {
                    val normEvent = event.trim().lowercase()
                    if (normEvent !in VALID_HOOKS) {
                        logger.warning(Lang.get("attribute-registry.unknown-hook-event", "label" to label, "event" to event, "valid" to VALID_HOOKS.joinToString("/")))
                        continue
                    }
                    val ref = h.getString(event)?.trim()?.takeIf { it.isNotEmpty() }
                    if (ref == null) {
                        logger.warning(Lang.get("attribute-registry.hook-ref-empty", "label" to label, "event" to event))
                        continue
                    }
                    if (!ref.contains(':')) {
                        logger.warning(Lang.get("attribute-registry.hook-ref-missing-namespace", "label" to label, "event" to event, "ref" to ref))
                        continue
                    }
                    hooks[normEvent] = ref
                }
            }
            val category = node.getString("category") ?: base?.category ?: Lang.get("attribute-registry.default-category")
            val mitigation = if (node.contains("mitigation")) node.getBoolean("mitigation") else base?.mitigation ?: false
            val combatPower = if (node.contains("combat-power")) node.getDouble("combat-power") else base?.combatPower ?: 1.0
            val min = if (node.contains("min")) node.getDouble("min") else base?.min
            val max = if (node.contains("max")) node.getDouble("max") else base?.max
            val message = node.getString("message")?.trim()?.takeIf { it.isNotEmpty() } ?: base?.message
            val mergedHooks = if (hooks.isNotEmpty()) hooks else base?.hooks ?: emptyMap()
            register(
                AttributeDef(
                    key = key.trim(),
                    displayName = display,
                    prefix = prefix,
                    category = category,
                    vanillaKey = vanilla,
                    itemAttribute = base?.itemAttribute,
                    damageType = damageType,
                    order = order,
                    mitigation = mitigation,
                    combatPower = combatPower,
                    min = min,
                    max = max,
                    message = message,
                    hooks = mergedHooks,
                ),
            )
            count++
        }
        if (count > 0) logger.info(Lang.get("attribute-registry.custom-loaded", "count" to count))
    }

    private val VALID_HOOKS = setOf(
        AttributeHooks.ON_DAMAGE_DEALT,
        AttributeHooks.ON_MAGIC_DAMAGE_DEALT,
        AttributeHooks.ON_DAMAGE_TAKEN,
        AttributeHooks.ON_EQUIP,
        AttributeHooks.ON_UNEQUIP,
        AttributeHooks.ON_TICK,
        AttributeHooks.ON_KILL,
    )

    private fun resolveVanillaKey(vanillaKey: String): Attribute? =
        runCatching { Registry.ATTRIBUTE.get(NamespacedKey.minecraft(vanillaKey)) }.getOrNull()
}
