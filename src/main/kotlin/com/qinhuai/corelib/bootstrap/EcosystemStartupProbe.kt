package com.qinhuai.corelib.bootstrap

import com.qinhuai.corelib.debug.BridgeStatus
import com.qinhuai.corelib.debug.BridgeStatusRegistry
import com.qinhuai.corelib.debug.HealthReport
import com.qinhuai.corelib.debug.ModuleStatus
import com.qinhuai.corelib.debug.PlatformStatus
import com.qinhuai.corelib.debug.TraceContext
import com.qinhuai.corelib.economy.EconomyBridge
import com.qinhuai.corelib.item.ItemSourceManager
import com.qinhuai.corelib.script.QinhScriptBridge
import org.bukkit.Bukkit

/**
 * 卫星插件（QI / QSt / QF）启动时探测已接入的软依赖。
 */
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
        plug("QinhItems", "物品库")
        plug("QinhSkills", "技能")
        plug("QinhForge", "锻造")
        plug("QinhStrengthen", "强化")
        plug("QCR", "作物机器人")
        plug("AttributePlus", "属性")
        plug("LegendCore", "Legendinlay 核心")
        plug("Legendinlay", "宝石·传奇")
        plug("MagicGem", "宝石·MG")
        plug("MMOItems", "MMOItems")
        plug("NeigeItems", "NeigeItems")
        plug("MythicMobs", "MythicMobs")
        plug("PlaceholderAPI", "占位符")
        plug("Vault", "经济 Vault")
        return out
    }

    fun buildPlatformStatus(name: String = "QCL", version: String = "unknown"): PlatformStatus {
        val scriptBridge = BridgeStatus(
            name = "GraalJS",
            available = QinhScriptBridge.isAvailable(),
            enabled = QinhScriptBridge.isAvailable(),
            source = "ScriptBridge",
            message = if (QinhScriptBridge.isAvailable()) "脚本桥可用" else "脚本桥不可用",
            recoverable = true,
        )
        val bridges = probePluginHooks().map {
            BridgeStatus(
                name = it.name,
                available = true,
                enabled = true,
                source = it.role,
                message = "已探测到软依赖",
                recoverable = true,
            )
        } + BridgeStatusRegistry.all() + scriptBridge
        val modules = availableItemSources().map {
            ModuleStatus(
                name = it,
                enabled = true,
                available = true,
                message = "物品源可用",
            )
        } + listOf(
            ModuleStatus("QinhSkills", Bukkit.getPluginManager().getPlugin("QinhSkills")?.isEnabled == true, Bukkit.getPluginManager().getPlugin("QinhSkills")?.isEnabled == true, "技能模块"),
            ModuleStatus("QinhForge", Bukkit.getPluginManager().getPlugin("QinhForge")?.isEnabled == true, Bukkit.getPluginManager().getPlugin("QinhForge")?.isEnabled == true, "锻造模块"),
            ModuleStatus("QinhStrengthen", Bukkit.getPluginManager().getPlugin("QinhStrengthen")?.isEnabled == true, Bukkit.getPluginManager().getPlugin("QinhStrengthen")?.isEnabled == true, "强化模块"),
            ModuleStatus("QCR", Bukkit.getPluginManager().getPlugin("QCR")?.isEnabled == true, Bukkit.getPluginManager().getPlugin("QCR")?.isEnabled == true, "作物机器人模块"),
        )
        val economyDiagnose = EconomyBridge.diagnose()
        val economyBridges = EconomyBridge.bridgeStatuses() + availableEconomies().map {
            BridgeStatus(
                name = it,
                available = true,
                enabled = true,
                source = "Economy",
                message = "经济提供器可用",
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
                bridges.isNotEmpty() || modules.isNotEmpty() -> HealthReport.healthy("平台探测完成")
                else -> HealthReport.degraded("NO_HOOK", "未发现可用软依赖", "确认相关插件是否已安装并启用")
            },
            trace = TraceContext(traceId = "qcl-status", module = name, bridge = "startup-probe", result = if (economyDiagnose.success) "ok" else economyDiagnose.code, hint = "用于 /qcl status"),
        )
    }

    fun formatSummary(prefix: String, extraLines: List<String> = emptyList()): List<String> {
        val sources = availableItemSources()
        val economies = availableEconomies()
        val hooks = probePluginHooks()
        val lines = mutableListOf<String>()
        lines += "§6[$prefix] §a软依赖探测"
        if (sources.isNotEmpty()) {
            lines += "§7  物品源 §f${sources.size}§7 个: §f${sources.joinToString("§7, §f")}"
        } else {
            lines += "§7  物品源: §c无可用源"
        }
        if (economies.isNotEmpty()) {
            lines += "§7  经济: §f${economies.joinToString("§7, §f")}"
        }
        val attr = hooks.filter { it.role.contains("属性") }
        if (attr.isNotEmpty()) {
            lines += "§7  属性插件: §f${attr.joinToString { it.name }}"
        }
        val others = hooks.filter { !it.role.contains("属性") && it.name != "Vault" }
        if (others.isNotEmpty()) {
            lines += "§7  已接入: §f${others.joinToString { "${it.name}(${it.role})" }}"
        }
        extraLines.forEach { lines += it }
        return lines
    }
}
