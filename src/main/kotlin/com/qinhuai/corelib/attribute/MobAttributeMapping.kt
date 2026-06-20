package com.qinhuai.corelib.attribute

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.lang.Lang
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object MobAttributeMapping {

    private val map = HashMap<String, Map<String, Double>>()

    fun get(mobName: String): Map<String, Double>? = map[mobName]

    fun load() {
        map.clear()
        val inline = scanMythicInline()
        val external = loadMobsYml()
        val total = map.size
        if (total > 0) {
            QinhCoreLib.instance.logger.info(Lang.get("mob-attribute-mapping.loaded", "total" to total, "inline" to inline, "external" to external))
        }
    }

    private fun scanMythicInline(): Int {
        val mm = Bukkit.getPluginManager().getPlugin("MythicMobs") ?: return 0
        val mobsDir = File(mm.dataFolder, "Mobs")
        if (!mobsDir.isDirectory) return 0
        var count = 0
        mobsDir.walkTopDown().filter { it.isFile && it.extension.equals("yml", ignoreCase = true) }.forEach { f ->
            val yaml = runCatching { YamlConfiguration.loadConfiguration(f) }.getOrNull() ?: return@forEach
            for (mobName in yaml.getKeys(false)) {
                val mobSec = yaml.getConfigurationSection(mobName) ?: continue
                val attrSec = mobSec.getConfigurationSection("QinhAttributes")
                    ?: mobSec.getConfigurationSection("QinhCoreLib")
                    ?: continue
                val attrs = LinkedHashMap<String, Double>()
                for (key in attrSec.getKeys(false)) attrs[key.trim()] = attrSec.getDouble(key)
                if (attrs.isNotEmpty()) {
                    map[mobName] = attrs
                    count++
                }
            }
        }
        return count
    }

    private fun loadMobsYml(): Int {
        val plugin = QinhCoreLib.instance
        val file = File(plugin.dataFolder, "mobs.yml")
        if (!file.exists()) {
            file.parentFile.mkdirs()
            plugin.getResource("mobs.yml")?.use { input -> file.outputStream().use { out -> input.copyTo(out) } }
        }
        if (!file.exists()) return 0
        val yaml = YamlConfiguration.loadConfiguration(file)
        val section = yaml.getConfigurationSection("mobs") ?: return 0
        var count = 0
        for (name in section.getKeys(false)) {
            val node = section.getConfigurationSection(name) ?: continue
            val attrs = LinkedHashMap<String, Double>()
            for (key in node.getKeys(false)) attrs[key.trim()] = node.getDouble(key)
            if (attrs.isNotEmpty()) {
                map[name] = attrs
                count++
            }
        }
        return count
    }
}
