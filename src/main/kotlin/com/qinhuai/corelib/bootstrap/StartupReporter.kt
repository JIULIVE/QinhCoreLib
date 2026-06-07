package com.qinhuai.corelib.bootstrap

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.util.TextUtil
import org.bukkit.plugin.java.JavaPlugin

/**
 * 启动期软依赖挂钩日志：仅在实际可用时输出单行，最后汇总统计。
 */
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
        logHook(pluginDisplayName, "物品源")
    }

    fun hookedBridge(pluginDisplayName: String, role: String) {
        bridges.add(pluginDisplayName)
        logHook(pluginDisplayName, role)
    }

    fun hookedEconomy(providerDisplayName: String) {
        economies.add(providerDisplayName)
        logHook(providerDisplayName, "经济")
    }

    fun printSummary(plugin: JavaPlugin) {
        val allHooked = buildList {
            addAll(itemSources)
            addAll(bridges)
            addAll(economies)
        }
        if (allHooked.isEmpty()) {
            TextUtil.logColored(plugin, "§6[QinhCoreLib] §e未挂钩任何软依赖")
            return
        }
        val bridgeTotal = bridges.size + economies.size
        plugin.logger.info(
            "  物品源 : ${itemSources.size}    GUI : $guiCount    桥接 : $bridgeTotal",
        )
        plugin.logger.info("  已挂钩 : ${allHooked.distinct().joinToString(", ")}")
    }

    private fun logHook(name: String, role: String) {
        TextUtil.logColored(QinhCoreLib.instance, "§a已挂钩软依赖 §f$name §a作为§e$role")
    }
}
