package com.qinhuai.corelib.assembly

import com.qinhuai.corelib.util.TextUtil
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

interface DisplayLayer {
    val priority: Int
    fun apply(meta: ItemMeta, context: AssemblyContext)
}

data class AssemblyContext(
    val variables: MutableMap<String, Any> = mutableMapOf()
)

class ItemAssembly {
    private val layers = mutableListOf<DisplayLayer>()
    
    fun addLayer(layer: DisplayLayer) {
        layers.add(layer)
    }
    
    fun apply(item: ItemStack, context: AssemblyContext = AssemblyContext()): ItemStack {
        val meta = item.itemMeta ?: return item
        layers.sortedBy { it.priority }.forEach { layer ->
            layer.apply(meta, context)
        }
        item.itemMeta = meta
        return item
    }
}

class NameLayer(
    override val priority: Int,
    private val nameProvider: (AssemblyContext) -> String
) : DisplayLayer {
    override fun apply(meta: ItemMeta, context: AssemblyContext) {
        meta.displayName(TextUtil.toComponent(nameProvider(context)))
    }
}

class LoreLayer(
    override val priority: Int,
    private val loreProvider: (AssemblyContext) -> List<String>
) : DisplayLayer {
    override fun apply(meta: ItemMeta, context: AssemblyContext) {
        val existingLore = meta.lore()?.toMutableList() ?: mutableListOf()
        val newLore = loreProvider(context).map { TextUtil.toComponent(it) }
        meta.lore(existingLore + newLore)
    }
}
