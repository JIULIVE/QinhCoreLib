package com.qinhuai.corelib.command

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.attribute.AttributeDebug
import com.qinhuai.corelib.attribute.AttributeRegistry
import com.qinhuai.corelib.attribute.AttributeService
import com.qinhuai.corelib.attribute.NativeAttributeBackend
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
import com.qinhuai.corelib.lang.Lang
import com.qinhuai.corelib.placeholder.PapiBridge
import com.qinhuai.corelib.pdc.PdcServiceManager
import com.qinhuai.corelib.script.QinhScriptBridge
import com.qinhuai.corelib.script.ScriptContext
import com.qinhuai.corelib.util.TextUtil
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.description.Description
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.suggestion.BlockingSuggestionProvider

object QCLCommands {

    fun register(manager: LegacyPaperCommandManager<CommandSender>) {
        val root = manager.commandBuilder("qcl", "qinhcorelib")

        manager.command(
            root.handler { ctx: CommandContext<CommandSender> ->
                sendHelp(ctx.sender())
            },
        )

        manager.command(
            root.literal("status", Description.of(Lang.get("qcl-commands.desc-status")), "probe")
                .permission("qcl.status")
                .handler { ctx: CommandContext<CommandSender> ->
                    sendStatus(ctx.sender(), "summary")
                },
        )

        manager.command(
            root.literal("status")
                .literal("detail", Description.of(Lang.get("qcl-commands.desc-status-detail")), "full")
                .permission("qcl.status")
                .handler { ctx: CommandContext<CommandSender> ->
                    sendStatus(ctx.sender(), "full")
                },
        )

        manager.command(
            root.literal("reload", Description.of(Lang.get("qcl-commands.desc-reload")), "rl")
                .permission("qcl.admin")
                .handler { ctx: CommandContext<CommandSender> ->
                    val sender = ctx.sender()
                    Lang.send(sender, "qcl-commands.reloading")
                    QinhCoreLib.instance.reloadPluginConfig()
                    Lang.send(sender, "qcl-commands.reload-done")
                },
        )

        manager.command(
            root.literal("gui")
                .permission("qcl.gui")
                .required("guiId", stringParser())
                .handler { ctx: CommandContext<CommandSender> ->
                    val sender = ctx.sender()
                    if (sender !is Player) {
                        Lang.send(sender, "qcl-commands.only-player")
                        return@handler
                    }
                    val guiId = ctx.get<String>("guiId")
                    val gui = CustomGuiManager.openGui(sender, guiId)
                    if (gui == null) {
                        Lang.send(sender, "qcl-commands.gui-not-found", "id" to guiId)
                    }
                },
        )

        manager.command(
            root.literal("attr", Description.of(Lang.get("qcl-commands.desc-attr")), "attribute")
                .permission("qcl.admin")
                .handler { ctx: CommandContext<CommandSender> ->
                    sendAttrOverview(ctx.sender())
                },
        )

        manager.command(
            root.literal("attr")
                .literal("list", Description.of(Lang.get("qcl-commands.desc-attr-list")))
                .permission("qcl.admin")
                .handler { ctx: CommandContext<CommandSender> ->
                    sendAttrList(ctx.sender())
                },
        )

        manager.command(
            root.literal("attr")
                .literal("show", Description.of(Lang.get("qcl-commands.desc-attr-show")))
                .permission("qcl.admin")
                .handler { ctx: CommandContext<CommandSender> ->
                    sendAttrShow(ctx.sender(), null)
                },
        )

        manager.command(
            root.literal("attr")
                .literal("show")
                .required("player", stringParser())
                .permission("qcl.admin")
                .handler { ctx: CommandContext<CommandSender> ->
                    sendAttrShow(ctx.sender(), ctx.get<String>("player"))
                },
        )

        manager.command(
            root.literal("attr")
                .literal("debug", Description.of(Lang.get("qcl-commands.desc-attr-debug")))
                .permission("qcl.admin")
                .handler { ctx: CommandContext<CommandSender> ->
                    toggleAttrDebug(ctx.sender())
                },
        )

        manager.command(
            root.literal("attr")
                .literal("book", Description.of(Lang.get("qcl-commands.desc-attr-book")))
                .handler { ctx: CommandContext<CommandSender> ->
                    val sender = ctx.sender()
                    if (sender is Player) {
                        com.qinhuai.corelib.attribute.AttributeBook.open(sender)
                    } else {
                        Lang.send(sender, "qcl-commands.only-player-book")
                    }
                },
        )

        manager.command(
            root.literal("mobattr", Description.of(Lang.get("qcl-commands.desc-mobattr")))
                .permission("qcl.admin")
                .required("key", stringParser())
                .optional("value", stringParser())
                .handler { ctx: CommandContext<CommandSender> -> mobAttr(ctx) },
        )

        manager.command(
            root.literal("buff")
                .permission("qcl.admin")
                .required("player", stringParser(), playerSuggestions)
                .required("attr", stringParser(), attrSuggestions)
                .required("amount", stringParser())
                .optional("op", stringParser(), opSuggestions)
                .optional("seconds", stringParser())
                .handler { ctx: CommandContext<CommandSender> -> addBuff(ctx) },
        )

        manager.command(
            root.literal("buffs")
                .permission("qcl.admin")
                .required("player", stringParser(), playerSuggestions)
                .handler { ctx: CommandContext<CommandSender> -> listBuffs(ctx) },
        )

        manager.command(
            root.literal("unbuff")
                .permission("qcl.admin")
                .required("player", stringParser(), playerSuggestions)
                .optional("source", stringParser(), buffSourceSuggestions)
                .handler { ctx: CommandContext<CommandSender> -> clearBuff(ctx) },
        )
    }

    private val playerSuggestions = BlockingSuggestionProvider.Strings<CommandSender> { _, _ ->
        Bukkit.getOnlinePlayers().map { it.name }
    }

    private val attrSuggestions = BlockingSuggestionProvider.Strings<CommandSender> { _, _ ->
        AttributeRegistry.all().map { it.key }
    }

    private val opSuggestions = BlockingSuggestionProvider.Strings<CommandSender> { _, _ ->
        listOf("flat", "relative", "multiply")
    }

    private val buffSourceSuggestions = BlockingSuggestionProvider.Strings<CommandSender> { ctx, _ ->
        val target = Bukkit.getPlayerExact(runCatching { ctx.get<String>("player") }.getOrNull() ?: "")
        if (target == null) listOf(DEBUG_BUFF_SOURCE)
        else (AttributeService.activeModifiers(target).map { it.source }.distinct() + DEBUG_BUFF_SOURCE).distinct()
    }

    private const val DEBUG_BUFF_SOURCE = "qcl_debug"

    private fun addBuff(ctx: CommandContext<CommandSender>) {
        val sender = ctx.sender()
        val target = Bukkit.getPlayerExact(ctx.get<String>("player"))
        if (target == null) {
            Lang.send(sender, "qcl-commands.buff-player-not-found", "player" to ctx.get<String>("player"))
            return
        }
        val attr = ctx.get<String>("attr")
        val amount = ctx.get<String>("amount").toDoubleOrNull()
        if (amount == null) {
            Lang.send(sender, "qcl-commands.buff-usage")
            return
        }
        val op = com.qinhuai.corelib.attribute.ModifierOp.parse(runCatching { ctx.get<String>("op") }.getOrNull())
        val seconds = runCatching { ctx.get<String>("seconds") }.getOrNull()?.toLongOrNull() ?: 0L
        val ticks = seconds * 20L
        val id = AttributeService.buff(target, attr, amount, DEBUG_BUFF_SOURCE, ticks, op)
        val dur = if (seconds > 0) Lang.get("qcl-commands.buff-seconds", "seconds" to seconds) else Lang.get("qcl-commands.buff-permanent")
        Lang.send(
            sender,
            "qcl-commands.buff-applied",
            "name" to target.name,
            "attr" to attr,
            "op" to op.name.lowercase(),
            "amount" to fmtNum(amount),
            "dur" to dur,
            "id" to id.take(8),
        )
        Lang.send(sender, "qcl-commands.buff-total", "attr" to attr, "total" to fmtNum(AttributeService.total(target, attr)))
    }

    private fun listBuffs(ctx: CommandContext<CommandSender>) {
        val sender = ctx.sender()
        val target = Bukkit.getPlayerExact(ctx.get<String>("player"))
        if (target == null) {
            Lang.send(sender, "qcl-commands.buff-player-not-found", "player" to ctx.get<String>("player"))
            return
        }
        val mods = AttributeService.activeModifiers(target)
        TextUtil.sendColored(sender, "§6§m----------------------------------")
        Lang.send(sender, "qcl-commands.buffs-title", "name" to target.name, "count" to mods.size)
        if (mods.isEmpty()) {
            Lang.send(sender, "qcl-commands.buffs-empty")
        } else {
            val now = System.currentTimeMillis()
            for (m in mods) {
                val dur = if (m.isTemporary) Lang.get("qcl-commands.buffs-remaining", "sec" to (m.remainingMillis(now) / 1000)) else Lang.get("qcl-commands.buffs-permanent-tag")
                Lang.send(
                    sender,
                    "qcl-commands.buffs-line",
                    "key" to m.key,
                    "op" to m.operation.name.lowercase(),
                    "amount" to fmtNum(m.amount),
                    "source" to m.source,
                    "dur" to dur,
                )
            }
        }
        TextUtil.sendColored(sender, "§6§m----------------------------------")
    }

    private fun clearBuff(ctx: CommandContext<CommandSender>) {
        val sender = ctx.sender()
        val target = Bukkit.getPlayerExact(ctx.get<String>("player"))
        if (target == null) {
            Lang.send(sender, "qcl-commands.buff-player-not-found", "player" to ctx.get<String>("player"))
            return
        }
        val source = runCatching { ctx.get<String>("source") }.getOrNull()
        if (source == null) {
            AttributeService.removeModifierSource(target, DEBUG_BUFF_SOURCE)
            Lang.send(sender, "qcl-commands.unbuff-debug", "name" to target.name, "source" to DEBUG_BUFF_SOURCE)
        } else {
            AttributeService.removeModifierSource(target, source)
            Lang.send(sender, "qcl-commands.unbuff-source", "name" to target.name, "source" to source)
        }
    }

    private fun fmtNum(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.2f", v)

    private fun mobAttr(ctx: CommandContext<CommandSender>) {
        val sender = ctx.sender()
        if (sender !is Player) {
            Lang.send(sender, "qcl-commands.mobattr-only-player")
            return
        }
        val target = sender.getTargetEntity(12) as? org.bukkit.entity.LivingEntity
        if (target == null || target is Player) {
            Lang.send(sender, "qcl-commands.mobattr-look-at-mob")
            return
        }
        val key = ctx.get<String>("key")
        if (key.equals("clear", ignoreCase = true)) {
            com.qinhuai.corelib.attribute.MobAttributeService.clear(target)
            Lang.send(sender, "qcl-commands.mobattr-cleared", "type" to target.type)
            return
        }
        val value = runCatching { ctx.get<String>("value") }.getOrNull()?.toDoubleOrNull()
        if (value == null) {
            Lang.send(sender, "qcl-commands.mobattr-usage")
            return
        }
        com.qinhuai.corelib.attribute.MobAttributeService.setSingle(target, key, value)
        Lang.send(sender, "qcl-commands.mobattr-set", "type" to target.type, "key" to key, "value" to value)
    }

    private fun sendHelp(sender: CommandSender) {
        TextUtil.sendColored(sender, "§6§m----------------------------------")
        Lang.send(sender, "qcl-commands.help-title")
        Lang.send(sender, "qcl-commands.help-status")
        Lang.send(sender, "qcl-commands.help-reload")
        Lang.send(sender, "qcl-commands.help-gui")
        Lang.send(sender, "qcl-commands.help-attr")
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
        lines += Lang.get("qcl-commands.status-eco-diag")
        lines += Lang.get("qcl-commands.status-platform", "state" to (if (status.health.ok) Lang.get("common.normal") else Lang.get("common.abnormal")))
        lines += Lang.get("qcl-commands.status-health-code", "code" to status.health.code)
        lines += Lang.get("qcl-commands.status-health-suggestion", "suggestion" to status.health.suggestion.ifBlank { Lang.get("common.none") })
        lines += Lang.get("qcl-commands.status-module-health", "code" to QinhCoreLib.moduleManager.healthReport().code)
        lines += Lang.get("qcl-commands.status-script-bridge", "state" to (if (QinhScriptBridge.isAvailable()) Lang.get("common.available") else Lang.get("common.unavailable")))
        lines += Lang.get("qcl-commands.status-loaded-scripts", "count" to QinhScriptBridge.loadedScripts().size)
        lines += Lang.get("qcl-commands.status-economy-health", "code" to com.qinhuai.corelib.economy.EconomyBridge.diagnose().code)
        lines += Lang.get("qcl-commands.status-papi-health", "code" to PapiBridge.diagnose().code)
        lines += Lang.get("qcl-commands.status-database-health", "code" to DatabaseManager.diagnose().code)
        lines += Lang.get("qcl-commands.status-pdc-health", "code" to PdcServiceManager.diagnoseAll().code)
        lines += Lang.get("qcl-commands.status-qi", "state" to (if (QinhCoreLib.instance.server.pluginManager.getPlugin("QinhItems")?.isEnabled == true) Lang.get("common.available") else Lang.get("common.unavailable")))
        lines += Lang.get("qcl-commands.status-qi-bridge-count", "count" to (QinhCoreLib.instance.server.pluginManager.getPlugin("QinhItems")?.let { 1 } ?: 0))
        lines += Lang.get("qcl-commands.status-qs", "state" to (if (QinhCoreLib.instance.server.pluginManager.getPlugin("QinhSkills")?.isEnabled == true) Lang.get("common.available") else Lang.get("common.unavailable")))
        lines += Lang.get("qcl-commands.status-qs-protocol-count", "count" to (if (QinhCoreLib.instance.server.pluginManager.getPlugin("QinhSkills")?.isEnabled == true) 1 else 0))
        lines += Lang.get("qcl-commands.status-qf", "state" to (if (QinhCoreLib.instance.server.pluginManager.getPlugin("QinhForge")?.isEnabled == true) Lang.get("common.available") else Lang.get("common.unavailable")))
        lines += Lang.get("qcl-commands.status-qst", "state" to (if (QinhCoreLib.instance.server.pluginManager.getPlugin("QinhStrengthen")?.isEnabled == true) Lang.get("common.available") else Lang.get("common.unavailable")))
        lines += Lang.get("qcl-commands.status-config-diag", "code" to ConfigDiagnostics.diagnose().code)
        lines += Lang.get("qcl-commands.status-api-boundary", "code" to ApiBoundaryDiagnostics.diagnose().code)
        lines += Lang.get("qcl-commands.status-public-api-count", "count" to publicApiClasses.size)
        lines += Lang.get("qcl-commands.status-apijar-package-count", "count" to exposedPackages.size)
        lines += Lang.get("qcl-commands.status-internal-package-count", "count" to internalPackages.size)
        lines += Lang.get("qcl-commands.status-apijar-diag", "code" to ApiJarDiagnostics.diagnose().code)
        lines += Lang.get("qcl-commands.status-exportable-api-count", "count" to exportableApiClasses.size)
        lines += Lang.get("qcl-commands.status-startup-diag", "code" to StartupDiagnostics.diagnose().code)
        lines += Lang.get("qcl-commands.status-item-ref-diag", "code" to ItemSourceManager.diagnoseItemReference("vanilla:stone").code)
        if (mode == "full") {
            lines += Lang.get("qcl-commands.status-bridge-count", "count" to status.bridges.size)
            lines += Lang.get("qcl-commands.status-module-count", "count" to status.modules.size, "total" to moduleStatuses.size)
            lines += Lang.get("qcl-commands.status-trace", "trace" to status.trace.traceId)
            moduleStatuses.forEach { m ->
                lines += Lang.get(
                    "qcl-commands.status-module-line",
                    "name" to m.name,
                    "enabled" to (if (m.enabled) "§a" + Lang.get("common.enabled") else "§c" + Lang.get("common.disabled")),
                    "available" to (if (m.available) Lang.get("common.available") else Lang.get("common.unavailable")),
                    "message" to m.message,
                )
            }
            lines += Lang.get("qcl-commands.status-bridge-header")
            status.bridges.forEach { b ->
                lines += Lang.get(
                    "qcl-commands.status-bridge-line",
                    "name" to b.name,
                    "enabled" to (if (b.enabled) "§a" + Lang.get("common.enabled") else "§c" + Lang.get("common.disabled")),
                    "available" to (if (b.available) Lang.get("common.available") else Lang.get("common.unavailable")),
                    "source" to b.source,
                    "message" to b.message,
                )
            }
            val jsResult = QinhScriptBridge.execute("global:qcl_status.js:formatStatus", ScriptContext(plugin = QinhCoreLib.instance, player = null, silent = true))
            (jsResult.value as? List<*>)?.forEach { line ->
                if (line != null) lines += line.toString()
            }
            lines += Lang.get("qcl-commands.status-note")
        }
        lines.forEach { TextUtil.sendColored(sender, it) }
        TextUtil.sendColored(sender, "§6§m----------------------------------")
    }

    private fun sendAttrOverview(sender: CommandSender) {
        val defs = AttributeRegistry.all()
        val configured = AttributeService.configuredBackend()
        val active = AttributeService.activeId()
        TextUtil.sendColored(sender, "§6§m----------------------------------")
        Lang.send(sender, "qcl-commands.attr-overview-title")
        Lang.send(sender, "qcl-commands.attr-configured-backend", "backend" to configured)
        Lang.send(sender, "qcl-commands.attr-active-backend", "backend" to active)
        if (configured != active && configured != "auto") {
            Lang.send(sender, "qcl-commands.attr-backend-fallback", "configured" to configured, "active" to active)
        }
        TextUtil.sendColored(
            sender,
            Lang.get("qcl-commands.attr-graaljs", "state" to (if (QinhScriptBridge.isAvailable()) Lang.get("common.available") else "§c" + Lang.get("qcl-commands.attr-graaljs-unavailable"))),
        )
        Lang.send(sender, "qcl-commands.attr-registered-count", "count" to defs.size)
        Lang.send(sender, "qcl-commands.attr-help-list")
        Lang.send(sender, "qcl-commands.attr-help-show")
        Lang.send(sender, "qcl-commands.attr-help-debug")
        TextUtil.sendColored(sender, "§6§m----------------------------------")
    }

    private fun sendAttrList(sender: CommandSender) {
        val all = AttributeRegistry.all()
        TextUtil.sendColored(sender, Lang.get("qcl-commands.attr-list-header", "count" to all.size, "backend" to AttributeService.activeId()))
        for ((cat, defs) in all.groupBy { it.category }) {
            TextUtil.sendColored(sender, "§6▶ §e$cat")
            for (def in defs) {
                val kind = when {
                    def.vanillaKey != null -> "§a" + Lang.get("qcl-commands.attr-kind-vanilla")
                    def.itemAttribute != null -> "§d" + Lang.get("qcl-commands.attr-kind-item")
                    else -> "§b" + Lang.get("qcl-commands.attr-kind-storage")
                }
                val tags = mutableListOf<String>()
                def.damageType?.let { tags.add("§7type=§f${it.name.lowercase()}") }
                if (def.hooks.isNotEmpty()) tags.add("§a" + Lang.get("qcl-commands.attr-tag-effect"))
                val suffix = if (tags.isEmpty()) "" else " §8" + tags.joinToString(" ")
                TextUtil.sendColored(sender, "  $kind §f${def.displayName} §8${def.key}$suffix")
            }
        }
    }

    private fun sendAttrShow(sender: CommandSender, name: String?) {
        val target = if (name != null) Bukkit.getPlayerExact(name) else sender as? Player
        if (target == null) {
            val detail = if (name != null) ": $name" else Lang.get("qcl-commands.attr-show-console-hint")
            TextUtil.sendColored(sender, Lang.get("qcl-commands.attr-show-player-not-found", "detail" to detail))
            return
        }
        TextUtil.sendColored(sender, "§6§m----------------------------------")
        TextUtil.sendColored(sender, Lang.get("qcl-commands.attr-show-title", "name" to target.name, "backend" to AttributeService.activeId()))

        val totals = AttributeService.totals(target)
        val shown = LinkedHashSet<String>()
        var any = false
        for (def in AttributeRegistry.all()) {
            val v = AttributeService.displayValue(target, def).takeIf { it != 0.0 } ?: continue
            shown.add(def.key)
            any = true
            val c = when {
                def.vanillaKey != null -> "§a"
                def.itemAttribute != null -> "§d"
                else -> "§b"
            }
            TextUtil.sendColored(sender, "  $c${def.displayName} §8(${def.key}) §7= §f${fmtAttr(v)}")
        }
        for ((key, v) in totals) {
            if (key in shown || v == 0.0) continue
            any = true
            TextUtil.sendColored(sender, "  §7$key §7= §f${fmtAttr(v)}")
        }
        if (!any) {
            Lang.send(sender, "qcl-commands.attr-show-no-attr")
        }

        val vanilla = NativeAttributeBackend.debugVanilla(target)
        if (vanilla.isNotEmpty()) {
            Lang.send(sender, "qcl-commands.attr-show-vanilla-header")
            vanilla.forEach { TextUtil.sendColored(sender, "§8  - $it") }
        }

        if (AttributeService.activeId() != "native") {
            Lang.send(sender, "qcl-commands.attr-show-non-native-note")
        }
        TextUtil.sendColored(sender, "§6§m----------------------------------")
    }

    private fun fmtAttr(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.2f", v)

    private fun toggleAttrDebug(sender: CommandSender) {
        if (sender !is Player) {
            Lang.send(sender, "qcl-commands.debug-only-player")
            return
        }
        val on = AttributeDebug.toggle(sender.uniqueId)
        Lang.send(sender, "qcl-commands.debug-toggled", "state" to (if (on) "§a" + Lang.get("common.state-on") else "§c" + Lang.get("common.state-off")))
        if (on) Lang.send(sender, "qcl-commands.debug-on-hint")
    }
}
