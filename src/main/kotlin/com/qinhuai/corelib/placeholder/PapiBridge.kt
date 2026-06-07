package com.qinhuai.corelib.placeholder

import com.qinhuai.corelib.debug.BridgeStatus
import com.qinhuai.corelib.debug.BridgeStatusRegistry
import com.qinhuai.corelib.debug.DiagnosticResult
import me.clip.placeholderapi.PlaceholderAPI
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import com.qinhuai.corelib.util.ServerCompat

/**
 * PlaceholderAPI 桥接：秦淮系列插件统一经 QinhCoreLib 使用 PAPI。
 * 服务端未安装 PlaceholderAPI 时所有方法安全降级。
 */
object PapiBridge {

    private val enabled: Boolean by lazy {
        Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null
    }

    private val registered = mutableMapOf<Plugin, MutableList<PlaceholderExpansion>>()

    fun isEnabled(): Boolean = enabled

    fun bridgeStatus(): BridgeStatus = BridgeStatus(
        name = "PlaceholderAPI",
        available = enabled,
        enabled = enabled,
        source = "PAPI",
        message = if (enabled) "占位符桥可用" else "占位符桥不可用",
        recoverable = true,
    )

    fun diagnose(): DiagnosticResult<BridgeStatus> = DiagnosticResult.ok(bridgeStatus(), source = "papi")

    fun apply(player: Player?, text: String): String {
        if (!enabled || player == null || !text.contains('%')) return text
        return PlaceholderAPI.setPlaceholders(player, text)
    }

    fun apply(offline: OfflinePlayer?, text: String): String {
        if (!enabled || offline == null || !text.contains('%')) return text
        return PlaceholderAPI.setPlaceholders(offline, text)
    }

    fun register(plugin: Plugin, provider: QinhPlaceholderProvider): Boolean {
        if (!enabled) return false
        val expansion = object : PlaceholderExpansion() {
            override fun getIdentifier(): String = provider.identifier
            override fun getAuthor(): String = "Qinhuai"
            override fun getVersion(): String = ServerCompat.pluginVersion(plugin)
            override fun persist(): Boolean = true
            override fun canRegister(): Boolean = true
            override fun onRequest(player: OfflinePlayer?, params: String): String? =
                provider.onRequest(player, params)
        }
        if (!expansion.register()) return false
        registered.getOrPut(plugin) { mutableListOf() }.add(expansion)
        BridgeStatusRegistry.register(bridgeStatus())
        return true
    }

    fun unregister(plugin: Plugin) {
        registered.remove(plugin)?.forEach { it.unregister() }
    }
}
