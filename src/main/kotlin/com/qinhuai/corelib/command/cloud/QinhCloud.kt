package com.qinhuai.corelib.command.cloud

import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.bukkit.CloudBukkitCapabilities
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.LegacyPaperCommandManager

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
