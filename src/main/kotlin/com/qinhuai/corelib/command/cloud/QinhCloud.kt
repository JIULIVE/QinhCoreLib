package com.qinhuai.corelib.command.cloud

import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.bukkit.CloudBukkitCapabilities
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.LegacyPaperCommandManager

/**
 * 秦淮系统一 Cloud 命令入口 — 替代 ACF，避免 Paper Timings 弃用警告。
 *
 * 使用 [LegacyPaperCommandManager]：兼容 Paper / Purpur / Spigot 1.21.11+。
 * Cloud 由 QinhCoreLib shade；子插件 compile 用 provided，运行时走 CoreLib。
 */
object QinhCloud {

    fun create(plugin: JavaPlugin): LegacyPaperCommandManager<CommandSender> {
        val coordinator = ExecutionCoordinator.simpleCoordinator<CommandSender>()
        val manager = LegacyPaperCommandManager.createNative(plugin, coordinator)
        if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            manager.registerBrigadier()
        }
        return manager
    }
}
