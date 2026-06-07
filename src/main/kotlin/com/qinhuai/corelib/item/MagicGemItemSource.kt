package com.qinhuai.corelib.item

import com.qinhuai.corelib.magicgem.MagicGemBridge
import org.bukkit.inventory.ItemStack

object MagicGemItemSource : ItemSource {
    override val id: String = "magicgem"

    override fun getItem(id: String, amount: Int): ItemStack? {
        if (!isAvailable()) return null
        val stack = MagicGemBridge.getGemItem(id) ?: return null
        stack.amount = amount.coerceAtLeast(1)
        return stack
    }

    override fun isAvailable(): Boolean = MagicGemBridge.isAvailable()
}
