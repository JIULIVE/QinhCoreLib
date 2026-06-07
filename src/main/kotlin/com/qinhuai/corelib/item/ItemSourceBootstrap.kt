package com.qinhuai.corelib.item

import com.qinhuai.corelib.bootstrap.StartupReporter

object ItemSourceBootstrap {

    private val displayNames = mapOf(
        "vanilla" to "Minecraft",
        "craftengine" to "CraftEngine",
        "neigeitems" to "NeigeItems",
        "mmoitems" to "MMOItems",
        "qinhitems" to "QinhItems",
        "mythicmobs" to "MythicMobs",
        "customfishing" to "CustomFishing",
        "magicgem" to "MagicGem",
    )

    private val allSources: List<ItemSource> = listOf(
        VanillaItemSource,
        CraftEngineItemSource,
        NeigeItemsItemSource,
        MMOItemsItemSource,
        QinhItemsItemSource,
        MythicMobsItemSource,
        CustomFishingItemSource,
        MagicGemItemSource,
    )

    fun registerAll() {
        allSources.forEach { ItemSourceManager.register(it) }
    }

    fun unregisterAll() {
        allSources.forEach { ItemSourceManager.unregister(it.id) }
    }

    /** 在所有插件 enable 后调用，汇报实际可用的物品源。 */
    fun reportAvailable() {
        ItemSourceManager.registeredSources()
            .filter { it.isAvailable() }
            .forEach { source ->
                val name = displayNames[source.id.lowercase()] ?: source.id
                StartupReporter.hookedItemSource(name)
            }
    }
}
