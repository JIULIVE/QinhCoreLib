package com.qinhuai.corelib.item

import com.qinhuai.corelib.neigeitems.NeigeItemsManager
import org.bukkit.inventory.ItemStack

object NeigeItemsItemSource : ItemSource {
    override val id: String = "neigeitems"

    override fun getItem(id: String, amount: Int): ItemStack? {
        if (!isAvailable()) return null

        return try {
            val itemStack = NeigeItemsManager.getItemStack(id) ?: return null
            val cloned = itemStack.clone()
            cloned.amount = amount.coerceAtLeast(1)
            cloned
        } catch (e: Exception) {
            null
        }
    }

    override fun isAvailable(): Boolean = NeigeItemsManager.isAvailable()

    override fun identify(stack: ItemStack): String? = NeigeItemsManager.getItemId(stack)
}
