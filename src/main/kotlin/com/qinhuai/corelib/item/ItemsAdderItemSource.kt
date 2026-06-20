package com.qinhuai.corelib.item

import com.qinhuai.corelib.itemsadder.ItemsAdderBridge
import org.bukkit.inventory.ItemStack

object ItemsAdderItemSource : ItemSource {
    override val id: String = "itemsadder"

    override fun getItem(id: String, amount: Int): ItemStack? {
        val iaId = if (id.contains(':')) id else id.replaceFirst("_", ":")
        return ItemsAdderBridge.buildItemStack(iaId, amount)
    }

    override fun isAvailable(): Boolean = ItemsAdderBridge.isAvailable()

    override fun identify(stack: ItemStack): String? = ItemsAdderBridge.idFromItem(stack)

    override fun matches(stack: ItemStack, id: String): Boolean {
        val normalized = if (id.contains(':')) id else id.replaceFirst("_", ":")
        return identify(stack) == normalized
    }
}
