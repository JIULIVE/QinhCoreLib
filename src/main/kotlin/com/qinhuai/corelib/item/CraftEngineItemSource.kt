package com.qinhuai.corelib.item

import com.qinhuai.corelib.craftengine.CraftEngineManager
import org.bukkit.inventory.ItemStack

object CraftEngineItemSource : ItemSource {
    override val id: String = "craftengine"
    
    override fun getItem(id: String, amount: Int): ItemStack? {
        val ceId = if (id.contains(':')) {
            id
        } else {
            id.replaceFirst("_", ":")
        }
        return CraftEngineManager.buildItemStack(ceId, amount)
    }
    
    override fun isAvailable(): Boolean {
        return CraftEngineManager.isAvailable()
    }
}
