package com.qinhuai.corelib.attribute

import com.qinhuai.corelib.QinhCoreLib
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object AttributeLang {

    data class Display(val displayName: String?, val prefix: String, val suffix: String)

    private val map = linkedMapOf<String, Display>()

    fun load() {
        map.clear()
        val folder = QinhCoreLib.instance.dataFolder
        mergeFile(File(folder, "lang/attributes.yml"))
        mergeFile(File(folder, "lang/en_US/attributes.yml"))
        val locale = com.qinhuai.corelib.lang.Lang.activeLocale()
        if (locale != "en_US") mergeFile(File(folder, "lang/$locale/attributes.yml"))
    }

    private fun mergeFile(file: File) {
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)
        for (key in yaml.getKeys(false)) {
            val sec = yaml.getConfigurationSection(key) ?: continue
            val norm = normalize(key)
            val prev = map[norm]
            map[norm] = Display(
                displayName = firstString(sec, "display_name", "显示名", "display") ?: prev?.displayName,
                prefix = firstString(sec, "prefix", "前缀") ?: prev?.prefix ?: "",
                suffix = firstString(sec, "suffix", "后缀") ?: prev?.suffix ?: "",
            )
        }
    }

    fun display(key: String): Display? = map[normalize(key)]

    private fun firstString(sec: ConfigurationSection, vararg keys: String): String? {
        for (k in keys) {
            val v = sec.getString(k)
            if (v != null) return v
        }
        return null
    }

    private fun normalize(key: String): String = key.trim().lowercase().replace('-', '_')
}
