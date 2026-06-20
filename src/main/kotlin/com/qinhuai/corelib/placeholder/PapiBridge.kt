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
import com.qinhuai.corelib.lang.Lang

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
        message = if (enabled) Lang.get("papi-bridge.status-available") else Lang.get("papi-bridge.status-unavailable"),
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
