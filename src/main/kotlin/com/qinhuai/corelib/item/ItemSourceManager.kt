package com.qinhuai.corelib.item

import org.bukkit.inventory.ItemStack

interface ItemSource {
    val id: String
    fun getItem(id: String, amount: Int): ItemStack?
    fun isAvailable(): Boolean
}

object ItemSourceManager {
    private val sources = linkedMapOf<String, ItemSource>()

    fun register(source: ItemSource) {
        sources[source.id.lowercase()] = source
    }

    fun unregister(id: String) {
        sources.remove(id.lowercase())
    }

    fun registeredSources(): List<ItemSource> = sources.values.toList()

    fun getSource(id: String): ItemSource? = sources[id.lowercase()]

    fun getItem(sourceId: String, itemId: String, amount: Int = 1): ItemStack? {
        val type = ItemSourceType.fromId(sourceId)
        val resolvedId = type?.id ?: sourceId
        return getSource(resolvedId)?.getItem(itemId, amount)
    }

    /** 解析 {@code mm-xxx} / {@code mi-SWORD-Blade} / {@code qinhitems:id} 等统一引用 */
    fun parseItemReference(ref: String): ItemStack? =
        com.qinhuai.corelib.api.item.ItemManagerAPI.instance.getHookItem(ref, null, 1)

    fun diagnoseItemReference(ref: String): com.qinhuai.corelib.debug.DiagnosticResult<ItemStack> =
        com.qinhuai.corelib.api.item.ItemManagerAPI.instance.diagnose(ref)
}
