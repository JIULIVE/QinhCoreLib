package com.qinhuai.corelib.bootstrap

import com.qinhuai.corelib.debug.BridgeStatus
import com.qinhuai.corelib.debug.BridgeStatusRegistry
import com.qinhuai.corelib.debug.HealthReport
import com.qinhuai.corelib.debug.ModuleStatus
import com.qinhuai.corelib.debug.PlatformStatus
import com.qinhuai.corelib.debug.TraceContext
import com.qinhuai.corelib.economy.EconomyBridge
import com.qinhuai.corelib.item.ItemSourceManager
import com.qinhuai.corelib.lang.Lang
import com.qinhuai.corelib.script.QinhScriptBridge
import org.bukkit.Bukkit

object EcosystemStartupProbe {

    data class Hook(val name: String, val role: String)

    private val ITEM_SOURCE_NAMES = mapOf(
        "vanilla" to "Minecraft",
        "qinhitems" to "QinhItems",
        "mmoitems" to "MMOItems",
        "neigeitems" to "NeigeItems",
        "mythicmobs" to "MythicMobs",
        "craftengine" to "CraftEngine",
        "customfishing" to "CustomFishing",
        "magicgem" to "MagicGem",
    )

    fun availableItemSources(): List<String> =
        ItemSourceManager.registeredSources()
            .filter { it.isAvailable() }
            .map { ITEM_SOURCE_NAMES[it.id.lowercase()] ?: it.id }
            .sorted()

    fun availableEconomies(): List<String> =
        EconomyBridge.availableProviderIds().map { id ->
            when (id.lowercase()) {
                "vault" -> "Vault"
                "excellenteconomy" -> "ExcellentEconomy"
                "playerpoints" -> "PlayerPoints"
                else -> id
            }
        }

    fun probePluginHooks(): List<Hook> {
        val pm = Bukkit.getPluginManager()
        val out = mutableListOf<Hook>()
        fun plug(name: String, role: String) {
            val p = pm.getPlugin(name)
            if (p != null && p.isEnabled) out.add(Hook(name, role))
        }
        plug("QinhItems", Lang.get("ecosystem-startup-probe.role-item-library"))
        plug("QinhSkills", Lang.get("ecosystem-startup-probe.role-skill"))
        plug("QinhForge", Lang.get("ecosystem-startup-probe.role-forge"))
        plug("QinhStrengthen", Lang.get("ecosystem-startup-probe.role-strengthen"))
        plug("QCR", Lang.get("ecosystem-startup-probe.role-crop-robot"))
        plug("AttributePlus", Lang.get("ecosystem-startup-probe.role-attribute"))
        plug("LegendCore", Lang.get("ecosystem-startup-probe.role-legend-core"))
        plug("Legendinlay", Lang.get("ecosystem-startup-probe.role-gem-legend"))
        plug("MagicGem", Lang.get("ecosystem-startup-probe.role-gem-mg"))
        plug("MMOItems", "MMOItems")
        plug("NeigeItems", "NeigeItems")
        plug("MythicMobs", "MythicMobs")
        plug("PlaceholderAPI", Lang.get("ecosystem-startup-probe.role-placeholder"))
        plug("Vault", Lang.get("ecosystem-startup-probe.role-economy-vault"))
        return out
    }

    fun buildPlatformStatus(name: String = "QCL", version: String = "unknown"): PlatformStatus {
        val scriptBridge = BridgeStatus(
            name = "GraalJS",
            available = QinhScriptBridge.isAvailable(),
            enabled = QinhScriptBridge.isAvailable(),
            source = "ScriptBridge",
            message = if (QinhScriptBridge.isAvailable()) Lang.get("ecosystem-startup-probe.script-bridge-available") else Lang.get("ecosystem-startup-probe.script-bridge-unavailable"),
            recoverable = true,
        )
        val bridges = probePluginHooks().map {
            BridgeStatus(
                name = it.name,
                available = true,
                enabled = true,
                source = it.role,
                message = Lang.get("ecosystem-startup-probe.soft-dep-detected"),
                recoverable = true,
            )
        } + BridgeStatusRegistry.all() + scriptBridge
        val modules = availableItemSources().map {
            ModuleStatus(
                name = it,
                enabled = true,
                available = true,
                message = Lang.get("ecosystem-startup-probe.item-source-available"),
            )
        } + listOf(
            ModuleStatus("QinhSkills", Bukkit.getPluginManager().getPlugin("QinhSkills")?.isEnabled == true, Bukkit.getPluginManager().getPlugin("QinhSkills")?.isEnabled == true, Lang.get("ecosystem-startup-probe.module-skill")),
            ModuleStatus("QinhForge", Bukkit.getPluginManager().getPlugin("QinhForge")?.isEnabled == true, Bukkit.getPluginManager().getPlugin("QinhForge")?.isEnabled == true, Lang.get("ecosystem-startup-probe.module-forge")),
            ModuleStatus("QinhStrengthen", Bukkit.getPluginManager().getPlugin("QinhStrengthen")?.isEnabled == true, Bukkit.getPluginManager().getPlugin("QinhStrengthen")?.isEnabled == true, Lang.get("ecosystem-startup-probe.module-strengthen")),
            ModuleStatus("QCR", Bukkit.getPluginManager().getPlugin("QCR")?.isEnabled == true, Bukkit.getPluginManager().getPlugin("QCR")?.isEnabled == true, Lang.get("ecosystem-startup-probe.module-crop-robot")),
        )
        val economyDiagnose = EconomyBridge.diagnose()
        val economyBridges = EconomyBridge.bridgeStatuses() + availableEconomies().map {
            BridgeStatus(
                name = it,
                available = true,
                enabled = true,
                source = "Economy",
                message = Lang.get("ecosystem-startup-probe.economy-provider-available"),
                recoverable = true,
            )
        }
        return PlatformStatus(
            name = name,
            version = version,
            enabled = true,
            modules = modules,
            bridges = bridges + economyBridges,
            health = when {
                !economyDiagnose.success -> HealthReport.degraded(economyDiagnose.code, economyDiagnose.message, economyDiagnose.suggestion)
                bridges.isNotEmpty() || modules.isNotEmpty() -> HealthReport.healthy(Lang.get("ecosystem-startup-probe.platform-probe-done"))
                else -> HealthReport.degraded("NO_HOOK", Lang.get("ecosystem-startup-probe.no-soft-dep"), Lang.get("ecosystem-startup-probe.no-soft-dep-hint"))
            },
            trace = TraceContext(traceId = "qcl-status", module = name, bridge = "startup-probe", result = if (economyDiagnose.success) "ok" else economyDiagnose.code, hint = Lang.get("ecosystem-startup-probe.trace-hint")),
        )
    }

    fun formatSummary(prefix: String, extraLines: List<String> = emptyList()): List<String> {
        val sources = availableItemSources()
        val economies = availableEconomies()
        val hooks = probePluginHooks()
        val lines = mutableListOf<String>()
        lines += Lang.get("ecosystem-startup-probe.summary-header", "prefix" to prefix)
        if (sources.isNotEmpty()) {
            lines += Lang.get("ecosystem-startup-probe.summary-item-sources", "count" to sources.size, "list" to sources.joinToString("§7, §f"))
        } else {
            lines += Lang.get("ecosystem-startup-probe.summary-no-item-source")
        }
        if (economies.isNotEmpty()) {
            lines += Lang.get("ecosystem-startup-probe.summary-economy", "list" to economies.joinToString("§7, §f"))
        }
        val attrRole = Lang.get("ecosystem-startup-probe.role-attribute")
        val attr = hooks.filter { it.role.contains(attrRole) }
        if (attr.isNotEmpty()) {
            lines += Lang.get("ecosystem-startup-probe.summary-attribute-plugins", "list" to attr.joinToString { it.name })
        }
        val others = hooks.filter { !it.role.contains(attrRole) && it.name != "Vault" }
        if (others.isNotEmpty()) {
            lines += Lang.get("ecosystem-startup-probe.summary-hooked", "list" to others.joinToString { "${it.name}(${it.role})" })
        }
        extraLines.forEach { lines += it }
        return lines
    }
}
