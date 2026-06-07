package com.qinhuai.corelib.config

import com.qinhuai.corelib.QinhCoreLib
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object ConfigManager {
    private val configs = mutableMapOf<String, YamlConfiguration>()
    
    fun loadConfig(path: String, saveDefault: Boolean = true): YamlConfiguration {
        val file = File(QinhCoreLib.instance.dataFolder, path)
        if (!file.exists()) {
            if (saveDefault) {
                QinhCoreLib.instance.saveResource(path, false)
            } else {
                file.parentFile?.mkdirs()
                file.createNewFile()
            }
        }
        val config = YamlConfiguration.loadConfiguration(file)
        configs[path] = config
        com.qinhuai.corelib.debug.ConfigManagerSnapshot.remember(path)
        return config
    }
    
    fun getConfig(path: String): YamlConfiguration? = configs[path]
    
    fun saveConfig(path: String) {
        val config = configs[path] ?: return
        val file = File(QinhCoreLib.instance.dataFolder, path)
        config.save(file)
    }
    
    fun reloadConfig(path: String): YamlConfiguration {
        configs.remove(path)
        return loadConfig(path, false)
    }
}
