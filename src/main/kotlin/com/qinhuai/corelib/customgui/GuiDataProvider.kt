package com.qinhuai.corelib.customgui

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * 为 GUI 分页或动态槽位提供数据。插件注册后，在 YAML 里用 source-type / source-value 引用。
 *
 * 例：source-type: qcr  +  source-value: unlocked_crops
 */
fun interface GuiPaginationListProvider {
    fun loadEntries(player: Player, gui: CustomGui, sourceValue: String): List<GuiPaginationEntry>
}

fun interface GuiDynamicSlotProvider {
    fun loadItem(player: Player, gui: CustomGui, sourceValue: String): ItemStack?
}

object GuiDataProviderRegistry {
    private val listProviders = mutableMapOf<String, GuiPaginationListProvider>()
    private val slotProviders = mutableMapOf<String, GuiDynamicSlotProvider>()

    fun registerList(namespace: String, provider: GuiPaginationListProvider) {
        listProviders[namespace.lowercase()] = provider
    }

    fun registerSlot(namespace: String, provider: GuiDynamicSlotProvider) {
        slotProviders[namespace.lowercase()] = provider
    }

    fun getListProvider(sourceType: String): GuiPaginationListProvider? {
        return listProviders[sourceType.lowercase()]
    }

    fun getSlotProvider(sourceType: String): GuiDynamicSlotProvider? {
        return slotProviders[sourceType.lowercase()]
    }

    fun unregisterList(namespace: String) {
        listProviders.remove(namespace.lowercase())
    }

    fun unregisterSlot(namespace: String) {
        slotProviders.remove(namespace.lowercase())
    }

    fun clear() {
        listProviders.clear()
        slotProviders.clear()
    }
}
