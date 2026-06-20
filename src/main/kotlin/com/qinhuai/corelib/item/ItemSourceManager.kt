package com.qinhuai.corelib.item

import org.bukkit.inventory.ItemStack

interface ItemSource {
    val id: String
    fun getItem(id: String, amount: Int): ItemStack?
    fun isAvailable(): Boolean

    fun identify(stack: ItemStack): String? = null

    fun matches(stack: ItemStack, id: String): Boolean = identify(stack) == id
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

    fun parseItemReference(ref: String): ItemStack? =
        com.qinhuai.corelib.api.item.ItemManagerAPI.instance.getHookItem(ref, null, 1)

    fun identify(stack: ItemStack): Pair<String, String>? {
        if (stack.type.isAir) return null
        for (source in sources.values) {
            if (source.id == ItemSourceType.VANILLA.id) continue
            if (!source.isAvailable()) continue
            val itemId = runCatching { source.identify(stack) }.getOrNull() ?: continue
            return source.id to itemId
        }
        return null
    }

    fun matchesReference(stack: ItemStack, ref: String): Boolean {
        if (stack.type.isAir) return false
        val parsed = ItemReferenceParser.parse(ref) ?: return false
        val sourceType = ItemSourceType.fromId(parsed.alias) ?: return false
        if (sourceType == ItemSourceType.VANILLA) {
            return stack.type.name.equals(parsed.itemId.substringBefore(':'), ignoreCase = true)
        }
        val source = getSource(sourceType.id)?.takeIf { it.isAvailable() } ?: return false
        return runCatching { source.matches(stack, parsed.itemId) }.getOrDefault(false)
    }

    fun diagnoseItemReference(ref: String): com.qinhuai.corelib.debug.DiagnosticResult<ItemStack> =
        com.qinhuai.corelib.api.item.ItemManagerAPI.instance.diagnose(ref)
}
