package com.qinhuai.corelib.attribute

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.lang.Lang
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object ElementRegistry {

    private val elements = linkedMapOf<String, Element>()

    var restraintBonus: Double = 1.5
        private set

    fun all(): Collection<Element> = elements.values

    fun get(id: String): Element? = elements[id.trim().lowercase()]

    fun load() {
        elements.clear()
        val file = File(QinhCoreLib.instance.dataFolder, "elements.yml")
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)
        restraintBonus = yaml.getDouble("restraint-bonus", 1.5).coerceAtLeast(1.0)
        val section = yaml.getConfigurationSection("elements") ?: return
        var count = 0
        for (key in section.getKeys(false)) {
            val node = section.getConfigurationSection(key) ?: continue
            val id = key.trim().lowercase()
            val name = node.getString("name", key) ?: key
            val color = node.getString("color", "&f") ?: "&f"
            val restrains = node.getStringList("restrains").map { it.trim().lowercase() }
            val elem = Element(id, name, color, restrains)
            elements[id] = elem
            AttributeRegistry.register(AttributeDef(elem.damageKey, "${name}伤害", category = "元素", combatPower = 5.0))
            AttributeRegistry.register(AttributeDef(elem.bonusKey, "${name}增伤", category = "元素", combatPower = 50.0))
            AttributeRegistry.register(AttributeDef(elem.resistKey, "${name}抗性", category = "元素抗性", max = 1.0))
            count++
        }
        if (count > 0) QinhCoreLib.instance.logger.info(Lang.get("element-registry.loaded", "count" to count))
    }

    fun dominantElement(totals: Map<String, Double>): Element? =
        elements.values.maxByOrNull { totals[it.resistKey] ?: 0.0 }
            ?.takeIf { (totals[it.resistKey] ?: 0.0) > 0.0 }

    fun restraintMultiplier(attacker: Element, target: Element?): Double {
        if (target == null || attacker.id == target.id) return 1.0
        if (attacker.restrains.contains(target.id)) return restraintBonus
        if (target.restrains.contains(attacker.id)) return (2.0 - restraintBonus).coerceAtLeast(0.0)
        return 1.0
    }
}
