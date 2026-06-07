package com.qinhuai.corelib.bootstrap

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.debug.ModuleStatus
import com.qinhuai.corelib.debug.HealthReport
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
            ?: return HealthReport.healthy("模块均正常")
        return HealthReport.degraded(
            code = if (!failed.available) "MODULE_UNAVAILABLE" else "MODULE_DISABLED",
            message = "模块状态异常: ${failed.name}",
            suggestion = failed.message.ifBlank { "检查模块依赖与启动日志" },
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
                loadStatus[module.name] = ModuleStatus(module.name, enabled = false, available = true, message = "已加载")
            } catch (e: Exception) {
                QinhCoreLib.instance.logger.severe("[QinhCoreLib] 加载模块失败: ${module.name}")
                e.printStackTrace()
                loadStatus[module.name] = ModuleStatus(module.name, enabled = false, available = false, message = e.message ?: "加载失败")
            }
        }
        loadedModules.forEach { module ->
            try {
                module.enable()
                loadStatus[module.name] = ModuleStatus(module.name, enabled = true, available = true, message = "已启用")
            } catch (e: Exception) {
                QinhCoreLib.instance.logger.severe("[QinhCoreLib] 启用模块失败: ${module.name}")
                e.printStackTrace()
                loadStatus[module.name] = ModuleStatus(module.name, enabled = false, available = true, message = e.message ?: "启用失败")
            }
        }
    }

    fun unloadAll() {
        loadedModules.reversed().forEach { module ->
            try {
                module.disable()
                loadStatus[module.name] = ModuleStatus(module.name, enabled = false, available = true, message = "已禁用")
            } catch (e: Exception) {
                QinhCoreLib.instance.logger.severe("[QinhCoreLib] 禁用模块失败: ${module.name}")
                e.printStackTrace()
                loadStatus[module.name] = ModuleStatus(module.name, enabled = false, available = true, message = e.message ?: "禁用失败")
            }
        }
        loadedModules.reversed().forEach { module ->
            try {
                module.unload()
                loadStatus[module.name] = ModuleStatus(module.name, enabled = false, available = false, message = "已卸载")
            } catch (e: Exception) {
                QinhCoreLib.instance.logger.severe("[QinhCoreLib] 卸载模块失败: ${module.name}")
                e.printStackTrace()
                loadStatus[module.name] = ModuleStatus(module.name, enabled = false, available = false, message = e.message ?: "卸载失败")
            }
        }
        loadedModules.clear()
    }

    fun reloadAll() {
        unloadAll()
        loadAll()
    }
}
