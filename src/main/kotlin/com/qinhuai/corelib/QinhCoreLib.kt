package com.qinhuai.corelib

import com.qinhuai.corelib.bootstrap.ModuleManager
import com.qinhuai.corelib.bootstrap.StartupReporter
import com.qinhuai.corelib.command.QCLCommands
import com.qinhuai.corelib.command.cloud.QinhCloud
import com.qinhuai.corelib.customgui.CustomGuiManager
import com.qinhuai.corelib.item.ItemManagerBootstrap
import com.qinhuai.corelib.item.ItemSourceBootstrap
import com.qinhuai.corelib.util.ServerCompat
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.paper.LegacyPaperCommandManager

class QinhCoreLib : JavaPlugin() {

    companion object {
        lateinit var instance: QinhCoreLib
            private set

        lateinit var moduleManager: ModuleManager
            private set

        lateinit var commandManager: LegacyPaperCommandManager<CommandSender>
            private set

        private fun unloadModulesIfInitialized() {
            if (::moduleManager.isInitialized) {
                moduleManager.unloadAll()
            }
        }
    }

    override fun onLoad() {
        instance = this
    }

    override fun onEnable() {
        ServerCompat.validateServer(logger)?.let { reason ->
            logger.severe("[QinhCoreLib] $reason")
            server.pluginManager.disablePlugin(this)
            return
        }

        StartupReporter.reset()
        logger.info("§6[QinhCoreLib] §e秦淮核心库 v${pluginMeta.version} 启用中…")

        saveDefaultConfig()

        com.qinhuai.corelib.economy.EconomyBridge.init(this)
        com.qinhuai.corelib.script.QinhScriptBridge.init(this)

        commandManager = QinhCloud.create(this)
        QCLCommands.register(commandManager)

        CustomGuiManager.init(this)
        StartupReporter.setGuiCount(CustomGuiManager.loadedGuiCount())

        moduleManager = ModuleManager()
        CoreModules.registerAll(moduleManager)
        moduleManager.loadAll()

        server.scheduler.runTaskLater(this, Runnable {
            ItemSourceBootstrap.reportAvailable()
            ItemManagerBootstrap.reloadExternalModules(this)
            StartupReporter.printSummary(this)
        }, 1L)
    }

    override fun onDisable() {
        unloadModulesIfInitialized()
        logger.info("[QinhCoreLib] 已卸载")
    }

    fun reloadPluginConfig() {
        reloadConfig()
        com.qinhuai.corelib.economy.EconomyBridge.init(this)
        com.qinhuai.corelib.script.QinhScriptBridge.reload()
        ItemManagerBootstrap.reloadExternalModules(this)
        CustomGuiManager.loadAllGuis()
        StartupReporter.setGuiCount(CustomGuiManager.loadedGuiCount())
        logger.info("[QinhCoreLib] 配置已重载")
    }
}
