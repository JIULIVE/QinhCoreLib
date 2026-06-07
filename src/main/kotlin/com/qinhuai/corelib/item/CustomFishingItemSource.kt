package com.qinhuai.corelib.item

import com.qinhuai.corelib.customfishing.CustomFishingManager
import org.bukkit.inventory.ItemStack

object CustomFishingItemSource : ItemSource {
    override val id: String = "customfishing"

    override fun getItem(id: String, amount: Int): ItemStack? {
        if (!isAvailable()) return null
        return CustomFishingManager.buildItem(id, amount)
    }

    override fun isAvailable(): Boolean = CustomFishingManager.isAvailable()
}
