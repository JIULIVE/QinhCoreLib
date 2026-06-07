package com.qinhuai.corelib.item

import com.qinhuai.corelib.mythicmobs.MythicMobsManager
import org.bukkit.inventory.ItemStack

object MythicMobsItemSource : ItemSource {
    override val id: String = "mythicmobs"

    override fun getItem(id: String, amount: Int): ItemStack? {
        if (!isAvailable()) return null
        return MythicMobsManager.getItemStack(id, amount)
    }

    override fun isAvailable(): Boolean = MythicMobsManager.isAvailable()
}
