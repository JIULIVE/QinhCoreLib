package com.qinhuai.corelib.attribute

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.bootstrap.AbstractModule
import org.bukkit.event.HandlerList
import java.io.File

object AttributeModule : AbstractModule("Attribute") {

    override val priority: Int = 45

    private val builtinScripts = listOf(
        "defense.js", "dodge.js", "block.js", "lifesteal.js", "thorns.js",
        "damage_reduction.js", "parry.js", "reflection.js", "spell_vampirism.js",
    )

    override fun load() {
        extractTemplate()
        extractElements()
        extractLang()
        AttributeRegistry.reset()
        ElementRegistry.load()
        AttributeRegistry.loadCustom()
        AttributeLang.load()
        MobAttributeMapping.load()
        AttributeService.init()
        applyBackendConfig()
        DamageIndicators.reload()
        AttributeCombatListener.reload()
    }

    fun reloadConfig() {
        AttributeRegistry.reset()
        ElementRegistry.load()
        AttributeRegistry.loadCustom()
        AttributeLang.load()
        MobAttributeMapping.load()
        applyBackendConfig()
        DamageIndicators.reload()
        AttributeCombatListener.reload()
    }

    private fun applyBackendConfig() {
        val id = QinhCoreLib.instance.config.getConfigurationSection("attribute")
            ?.getString("backend", "native")?.trim()?.lowercase()
            ?: "native"
        AttributeService.setConfiguredBackend(id)
    }

    override fun enable() {
        extractBuiltinScripts()
        QinhCoreLib.instance.server.pluginManager.registerEvents(AttributeCombatListener, QinhCoreLib.instance)
        QinhCoreLib.instance.server.pluginManager.registerEvents(DamageIndicators, QinhCoreLib.instance)
        org.bukkit.Bukkit.getScheduler().runTaskLater(QinhCoreLib.instance, Runnable { DamageIndicators.clearOrphans() }, 20L)
        AttributeTickRunner.start()
        MythicMobAttributeHook.tryRegister()
        com.qinhuai.corelib.placeholder.PapiBridge.register(
            QinhCoreLib.instance,
            com.qinhuai.corelib.placeholder.AttributePlaceholders,
        )
    }

    override fun disable() {
        AttributeTickRunner.stop()
        HandlerList.unregisterAll(AttributeCombatListener)
        HandlerList.unregisterAll(DamageIndicators)
        DamageIndicators.clearOrphans()
    }

    private fun extractBuiltinScripts() {
        val plugin = QinhCoreLib.instance
        for (name in builtinScripts) {
            val target = File(plugin.dataFolder, "scripts/attributes/$name")
            if (target.exists()) continue
            target.parentFile.mkdirs()
            plugin.getResource("scripts/attributes/$name")?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    private fun extractTemplate() {
        val plugin = QinhCoreLib.instance
        val target = File(plugin.dataFolder, "attributes.yml")
        if (target.exists()) return
        target.parentFile.mkdirs()
        plugin.getResource("attributes.yml")?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
    }

    private fun extractElements() {
        val plugin = QinhCoreLib.instance
        val target = File(plugin.dataFolder, "elements.yml")
        if (target.exists()) return
        target.parentFile.mkdirs()
        plugin.getResource("elements.yml")?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
    }

    private fun extractLang() {
        val plugin = QinhCoreLib.instance
        val target = File(plugin.dataFolder, "lang/attributes.yml")
        if (target.exists()) return
        target.parentFile.mkdirs()
        plugin.getResource("lang/attributes.yml")?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
    }
}
