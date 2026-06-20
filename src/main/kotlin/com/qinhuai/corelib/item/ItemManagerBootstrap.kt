package com.qinhuai.corelib.item

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.api.item.ItemManagerAPI
import com.qinhuai.corelib.lang.Lang
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

object GroovyItemModuleLoader {

    private var groovyClassLoader: Any? = null
    private val loadedClasses = mutableListOf<Class<*>>()

    fun isGroovyAvailable(): Boolean = try {
        Class.forName("groovy.lang.GroovyClassLoader")
        true
    } catch (_: ClassNotFoundException) {
        false
    }

    fun ensureFolder(plugin: JavaPlugin): File {
        val folder = File(plugin.dataFolder, "item-modules")
        if (!folder.exists()) {
            folder.mkdirs()
            listOf(
                "item-modules/examples/RPGItemsModule.groovy.example",
                "item-modules/examples/OraxenModule.groovy.example",
            ).forEach { path ->
                try {
                    plugin.saveResource(path, false)
                } catch (_: IllegalArgumentException) {
                }
            }
        }
        return folder
    }

    fun reload(plugin: JavaPlugin): Int {
        unload()
        if (!isGroovyAvailable()) {
            plugin.logger.info(Lang.get("item-manager-bootstrap.groovy-not-ready"))
            return 0
        }

        val folder = ensureFolder(plugin)
        val scripts = folder.listFiles { file -> file.isFile && file.name.endsWith(".groovy", ignoreCase = true) }
            ?: return 0

        if (scripts.isEmpty()) return 0

        val loaderClass = Class.forName("groovy.lang.GroovyClassLoader")
        val gcl = loaderClass.getConstructor(ClassLoader::class.java)
            .newInstance(plugin.javaClass.classLoader)
        groovyClassLoader = gcl
        val parseClass = loaderClass.getMethod("parseClass", File::class.java)

        var count = 0
        for (script in scripts.sortedBy { it.name }) {
            try {
                val clazz = parseClass.invoke(gcl, script) as Class<*>
                clazz.getMethod("onGroovyRegister").invoke(null)
                loadedClasses += clazz
                count++
                plugin.logger.info(Lang.get("item-manager-bootstrap.module-loaded", "name" to script.name))
            } catch (ex: Exception) {
                plugin.logger.warning(Lang.get("item-manager-bootstrap.script-load-failed", "name" to script.name, "error" to ex.message))
            }
        }
        return count
    }

    fun unload() {
        for (clazz in loadedClasses.asReversed()) {
            try {
                clazz.getMethod("onGroovyUnregister").invoke(null)
            } catch (_: Exception) {
            }
        }
        loadedClasses.clear()
        groovyClassLoader?.let { loader ->
            try {
                loader.javaClass.getMethod("clearCache").invoke(loader)
            } catch (_: Exception) {
            }
        }
        groovyClassLoader = null
    }
}

object ItemManagerBootstrap {

    fun onItemSourcesReady() {
        ItemManagerAPI.instance.registerBuiltinSources()
    }

    fun reloadExternalModules(plugin: JavaPlugin = QinhCoreLib.instance) {
        GroovyItemModuleLoader.reload(plugin)
    }

    fun unloadAll() {
        GroovyItemModuleLoader.unload()
        ItemManagerAPI.instance.clear()
    }
}
