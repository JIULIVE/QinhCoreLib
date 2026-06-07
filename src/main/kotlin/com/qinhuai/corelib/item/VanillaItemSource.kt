package com.qinhuai.corelib.item

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object VanillaItemSource : ItemSource {
    override val id: String = "vanilla"

    override fun getItem(id: String, amount: Int): ItemStack? {
        return try {
            val material = Material.valueOf(id.uppercase())
            ItemStack(material, amount)
        } catch (e: Exception) {
            null
        }
    }

    override fun isAvailable(): Boolean {
        return true
    }
}
