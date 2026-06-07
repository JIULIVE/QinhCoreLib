package com.qinhuai.corelib.item

import com.qinhuai.corelib.mmoitems.MMOItemsManager
import org.bukkit.inventory.ItemStack

object MMOItemsItemSource : ItemSource {
    override val id: String = "mmoitems"
    
    override fun getItem(id: String, amount: Int): ItemStack? {
        if (!isAvailable()) return null
        
        return try {
            val parts = id.split(':', limit = 2)
            val typeId = parts.getOrNull(0) ?: return null
            val itemId = parts.getOrNull(1) ?: return null
            
            val itemStack = MMOItemsManager.getItem(typeId, itemId) ?: return null
            
            val cloned = itemStack.clone()
            cloned.amount = amount.coerceAtLeast(1)
            cloned
        } catch (e: Exception) {
            null
        }
    }
    
    override fun isAvailable(): Boolean = MMOItemsManager.isAvailable()
}
