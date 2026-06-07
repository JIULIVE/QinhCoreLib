package com.qinhuai.corelib.command

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.bootstrap.EcosystemStartupProbe
import com.qinhuai.corelib.customgui.CustomGuiManager
import com.qinhuai.corelib.database.DatabaseManager
import com.qinhuai.corelib.debug.ApiBoundaryDiagnostics
import com.qinhuai.corelib.debug.ApiJarDiagnostics
import com.qinhuai.corelib.debug.ApiJarFilter
import com.qinhuai.corelib.debug.ApiJarManifest
import com.qinhuai.corelib.debug.ConfigDiagnostics
import com.qinhuai.corelib.debug.StartupDiagnostics
import com.qinhuai.corelib.item.ItemSourceManager
import com.qinhuai.corelib.placeholder.PapiBridge
import com.qinhuai.corelib.pdc.PdcServiceManager
import com.qinhuai.corelib.script.QinhScriptBridge
import com.qinhuai.corelib.script.ScriptContext
import com.qinhuai.corelib.util.TextUtil
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.description.Description
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.incendo.cloud.parser.standard.StringParser.stringParser

object QCLCommands {

    fun register(manager: LegacyPaperCommandManager<CommandSender>) {
        val root = manager.commandBuilder("qcl", "qinhcorelib")

        manager.command(
            root.handler { ctx: CommandContext<CommandSender> ->
                sendHelp(ctx.sender())
            },
        )

        manager.command(
            root.literal("status", Description.of("查看生态状态"), "probe")
                .permission("qcl.status")
                .handler { ctx: CommandContext<CommandSender> ->
                    sendStatus(ctx.sender(), "summary")
                },
        )

        manager.command(
            root.literal("status")
                .literal("detail", Description.of("查看详细状态"), "full")
                .permission("qcl.status")
                .handler { ctx: CommandContext<CommandSender> ->
                    sendStatus(ctx.sender(), "full")
                },
        )

        manager.command(
            root.literal("reload", Description.of("重新加载"), "rl")
                .permission("qcl.admin")
                .handler { ctx: CommandContext<CommandSender> ->
                    val sender = ctx.sender()
                    TextUtil.sendColored(sender, "§6[QinhCoreLib] §e正在重新加载…")
                    QinhCoreLib.instance.reloadPluginConfig()
                    TextUtil.sendColored(sender, "§6[QinhCoreLib] §a重载完成")
                },
        )

        manager.command(
            root.literal("gui")
                .permission("qcl.gui")
                .required("guiId", stringParser())
                .handler { ctx: CommandContext<CommandSender> ->
                    val sender = ctx.sender()
                    if (sender !is Player) {
                        TextUtil.sendColored(sender, "§c只有玩家可以使用此命令！")
                        return@handler
                    }
                    val guiId = ctx.get<String>("guiId")
                    val gui = CustomGuiManager.openGui(sender, guiId)
                    if (gui == null) {
                        TextUtil.sendColored(sender, "§c[QinhCoreLib] 找不到GUI: $guiId")
                    }
                },
        )
    }

    private fun sendHelp(sender: CommandSender) {
        TextUtil.sendColored(sender, "§6§m----------------------------------")
        TextUtil.sendColored(sender, "§6[QinhCoreLib] §e秦淮核心库")
        TextUtil.sendColored(sender, "§6/qcl status §7- 查看生态与桥接状态")
        TextUtil.sendColored(sender, "§6/qcl reload §7- 重新加载配置文件")
        TextUtil.sendColored(sender, "§6/qcl gui <id> §7- 打开自定义GUI")
        TextUtil.sendColored(sender, "§6§m----------------------------------")
    }

    private fun sendStatus(sender: CommandSender, mode: String) {
        TextUtil.sendColored(sender, "§6§m----------------------------------")
        val status = EcosystemStartupProbe.buildPlatformStatus("QCL", QinhCoreLib.instance.pluginMeta.version)
        val moduleStatuses = QinhCoreLib.moduleManager.statuses()
        val publicApiClasses = ApiBoundaryDiagnostics.publicApiClasses()
        val exportableApiClasses = ApiJarFilter.exportableClasses(publicApiClasses)
        val exposedPackages = ApiJarManifest.exposedPackages()
        val internalPackages = ApiJarManifest.internalPackages()
        val lines = mutableListOf<String>()
        lines += EcosystemStartupProbe.formatSummary("QCL")
        lines += "§6[QCL] §e生态诊断"
        lines += "§6[QCL] §a平台状态 §7= §f${if (status.health.ok) "正常" else "异常"}"
        lines += "§6[QCL] §a健康码 §7= §f${status.health.code}"
        lines += "§6[QCL] §a健康建议 §7= §f${status.health.suggestion.ifBlank { "无" }}"
        lines += "§6[QCL] §a模块健康 §7= §f${QinhCoreLib.moduleManager.healthReport().code}"
        lines += "§6[QCL] §a脚本桥接 §7= §f${if (QinhScriptBridge.isAvailable()) "可用" else "不可用"}"
        lines += "§6[QCL] §a已加载脚本 §7= §f${QinhScriptBridge.loadedScripts().size}"
        lines += "§6[QCL] §a经济桥健康 §7= §f${com.qinhuai.corelib.economy.EconomyBridge.diagnose().code}"
        lines += "§6[QCL] §aPAPI桥健康 §7= §f${PapiBridge.diagnose().code}"
        lines += "§6[QCL] §a数据库健康 §7= §f${DatabaseManager.diagnose().code}"
        lines += "§6[QCL] §aPDC健康 §7= §f${PdcServiceManager.diagnoseAll().code}"
        lines += "§6[QCL] §aQI状态 §7= §f${if (QinhCoreLib.instance.server.pluginManager.getPlugin("QinhItems")?.isEnabled == true) "可用" else "不可用"}"
        lines += "§6[QCL] §aQI桥数量 §7= §f${QinhCoreLib.instance.server.pluginManager.getPlugin("QinhItems")?.let { 1 } ?: 0}"
        lines += "§6[QCL] §aQS状态 §7= §f${if (QinhCoreLib.instance.server.pluginManager.getPlugin("QinhSkills")?.isEnabled == true) "可用" else "不可用"}"
        lines += "§6[QCL] §aQS协议数 §7= §f${if (QinhCoreLib.instance.server.pluginManager.getPlugin("QinhSkills")?.isEnabled == true) 1 else 0}"
        lines += "§6[QCL] §aQF状态 §7= §f${if (QinhCoreLib.instance.server.pluginManager.getPlugin("QinhForge")?.isEnabled == true) "可用" else "不可用"}"
        lines += "§6[QCL] §aQSt状态 §7= §f${if (QinhCoreLib.instance.server.pluginManager.getPlugin("QinhStrengthen")?.isEnabled == true) "可用" else "不可用"}"
        lines += "§6[QCL] §a配置诊断 §7= §f${ConfigDiagnostics.diagnose().code}"
        lines += "§6[QCL] §aAPI边界 §7= §f${ApiBoundaryDiagnostics.diagnose().code}"
        lines += "§6[QCL] §a公开API数 §7= §f${publicApiClasses.size}"
        lines += "§6[QCL] §aapiJar包数 §7= §f${exposedPackages.size}"
        lines += "§6[QCL] §a内部包数 §7= §f${internalPackages.size}"
        lines += "§6[QCL] §aapiJar诊断 §7= §f${ApiJarDiagnostics.diagnose().code}"
        lines += "§6[QCL] §a可导出API类数 §7= §f${exportableApiClasses.size}"
        lines += "§6[QCL] §a启动诊断 §7= §f${StartupDiagnostics.diagnose().code}"
        lines += "§6[QCL] §a物品引用诊断 §7= §f${ItemSourceManager.diagnoseItemReference("vanilla:stone").code}"
        if (mode == "full") {
            lines += "§6[QCL] §a桥接数 §7= §f${status.bridges.size}"
            lines += "§6[QCL] §a模块数 §7= §f${status.modules.size} / ${moduleStatuses.size}"
            lines += "§6[QCL] §aTrace §7= §f${status.trace.traceId}"
            moduleStatuses.forEach { m ->
                lines += "§7  - §f${m.name}§7 : ${if (m.enabled) "§a启用" else "§c未启用"} §7(${if (m.available) "可用" else "不可用"}) §7${m.message}"
            }
            lines += "§7  桥状态:"
            status.bridges.forEach { b ->
                lines += "§7  - §f${b.name}§7 : ${if (b.enabled) "§a启用" else "§c未启用"} §7(${if (b.available) "可用" else "不可用"}) §7${b.source} §7${b.message}"
            }
            val jsResult = QinhScriptBridge.execute("global:qcl_status.js:formatStatus", ScriptContext(plugin = QinhCoreLib.instance, player = null, silent = true))
            (jsResult.value as? List<*>)?.forEach { line ->
                if (line != null) lines += line.toString()
            }
            lines += "§7  说明: 复杂诊断逻辑可放入脚本，后续可按 namespace 扩展"
        }
        lines.forEach { TextUtil.sendColored(sender, it) }
        TextUtil.sendColored(sender, "§6§m----------------------------------")
    }
}
