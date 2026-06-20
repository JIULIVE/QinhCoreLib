package com.qinhuai.corelib.bootstrap

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.lang.Lang
import com.qinhuai.corelib.util.TextUtil
import org.bukkit.plugin.java.JavaPlugin

object StartupReporter {

    private val itemSources = mutableListOf<String>()
    private val bridges = mutableListOf<String>()
    private val economies = mutableListOf<String>()
    private var guiCount = 0

    fun reset() {
        itemSources.clear()
        bridges.clear()
        economies.clear()
        guiCount = 0
    }

    fun setGuiCount(count: Int) {
        guiCount = count.coerceAtLeast(0)
    }

    fun hookedItemSource(pluginDisplayName: String) {
        if (itemSources.contains(pluginDisplayName)) return
        itemSources.add(pluginDisplayName)
        logHook(pluginDisplayName, Lang.get("startup-reporter.role-item-source"))
    }

    fun hookedBridge(pluginDisplayName: String, role: String) {
        bridges.add(pluginDisplayName)
        logHook(pluginDisplayName, role)
    }

    fun hookedEconomy(providerDisplayName: String) {
        economies.add(providerDisplayName)
        logHook(providerDisplayName, Lang.get("startup-reporter.role-economy"))
    }

    fun printSummary(plugin: JavaPlugin) {
        val allHooked = buildList {
            addAll(itemSources)
            addAll(bridges)
            addAll(economies)
        }
        if (allHooked.isEmpty()) {
            TextUtil.logColored(plugin, Lang.get("startup-reporter.no-soft-deps"))
            return
        }
        val bridgeTotal = bridges.size + economies.size
        plugin.logger.info(
            Lang.get(
                "startup-reporter.summary-counts",
                "items" to itemSources.size,
                "gui" to guiCount,
                "bridges" to bridgeTotal,
            ),
        )
        plugin.logger.info(
            Lang.get("startup-reporter.summary-hooked", "list" to allHooked.distinct().joinToString(", ")),
        )
    }

    private fun logHook(name: String, role: String) {
        TextUtil.logColored(QinhCoreLib.instance, Lang.get("startup-reporter.hooked", "name" to name, "role" to role))
    }
}
