package com.qinhuai.corelib.bootstrap

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.debug.ModuleStatus
import com.qinhuai.corelib.debug.HealthReport
import com.qinhuai.corelib.lang.Lang
import java.util.concurrent.ConcurrentHashMap

class ModuleManager {
    private val modules = ConcurrentHashMap<String, Module>()
    private val loadedModules = mutableListOf<Module>()
    private val loadStatus = linkedMapOf<String, ModuleStatus>()

    fun register(module: Module) {
        modules[module.name] = module
    }

    fun unregister(name: String) {
        modules.remove(name)
        loadStatus.remove(name)
    }

    fun getModule(name: String): Module? = modules[name]

    fun statuses(): List<ModuleStatus> = loadStatus.values.toList()

    fun healthReport(): HealthReport {
        val failed = loadStatus.values.firstOrNull { !it.available || !it.enabled }
            ?: return HealthReport.healthy(Lang.get("module-manager.all-healthy"))
        return HealthReport.degraded(
            code = if (!failed.available) "MODULE_UNAVAILABLE" else "MODULE_DISABLED",
            message = Lang.get("module-manager.module-abnormal", "name" to failed.name),
            suggestion = failed.message.ifBlank { Lang.get("module-manager.check-deps-logs") },
        )
    }

    fun loadAll() {
        loadStatus.clear()
        loadedModules.clear()
        val sortedModules = modules.values.sortedBy { it.priority }
        sortedModules.forEach { module ->
            try {
                module.load()
                loadedModules.add(module)
                loadStatus[module.name] = ModuleStatus(module.name, enabled = false, available = true, message = Lang.get("common.loaded"))
            } catch (e: Exception) {
                QinhCoreLib.instance.logger.severe(Lang.get("module-manager.load-failed", "name" to module.name))
                e.printStackTrace()
                loadStatus[module.name] = ModuleStatus(module.name, enabled = false, available = false, message = e.message ?: Lang.get("module-manager.load-failed-short"))
            }
        }
        loadedModules.forEach { module ->
            try {
                module.enable()
                loadStatus[module.name] = ModuleStatus(module.name, enabled = true, available = true, message = Lang.get("module-manager.enabled"))
            } catch (e: Exception) {
                QinhCoreLib.instance.logger.severe(Lang.get("module-manager.enable-failed", "name" to module.name))
                e.printStackTrace()
                loadStatus[module.name] = ModuleStatus(module.name, enabled = false, available = true, message = e.message ?: Lang.get("module-manager.enable-failed-short"))
            }
        }
    }

    fun unloadAll() {
        loadedModules.reversed().forEach { module ->
            try {
                module.disable()
                loadStatus[module.name] = ModuleStatus(module.name, enabled = false, available = true, message = Lang.get("module-manager.disabled"))
            } catch (e: Exception) {
                QinhCoreLib.instance.logger.severe(Lang.get("module-manager.disable-failed", "name" to module.name))
                e.printStackTrace()
                loadStatus[module.name] = ModuleStatus(module.name, enabled = false, available = true, message = e.message ?: Lang.get("module-manager.disable-failed-short"))
            }
        }
        loadedModules.reversed().forEach { module ->
            try {
                module.unload()
                loadStatus[module.name] = ModuleStatus(module.name, enabled = false, available = false, message = Lang.get("common.unloaded"))
            } catch (e: Exception) {
                QinhCoreLib.instance.logger.severe(Lang.get("module-manager.unload-failed", "name" to module.name))
                e.printStackTrace()
                loadStatus[module.name] = ModuleStatus(module.name, enabled = false, available = false, message = e.message ?: Lang.get("module-manager.unload-failed-short"))
            }
        }
        loadedModules.clear()
    }

    fun reloadAll() {
        unloadAll()
        loadAll()
    }
}
