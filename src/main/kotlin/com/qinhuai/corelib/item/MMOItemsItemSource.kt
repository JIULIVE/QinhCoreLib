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

    override fun identify(stack: ItemStack): String? {
        if (!isAvailable()) return null
        val type = MMOItemsManager.getType(stack) ?: return null
        val itemId = MMOItemsManager.getID(stack) ?: return null
        return "$type:$itemId"
    }

    override fun matches(stack: ItemStack, id: String): Boolean {
        if (!isAvailable()) return false
        val type = MMOItemsManager.getType(stack) ?: return false
        val itemId = MMOItemsManager.getID(stack) ?: return false
        val parts = id.split(':', '-', limit = 2)
        val wantType = parts.getOrNull(0) ?: return false
        val wantId = parts.getOrNull(1) ?: return false
        return type.equals(wantType, ignoreCase = true) && itemId.equals(wantId, ignoreCase = true)
    }
}
