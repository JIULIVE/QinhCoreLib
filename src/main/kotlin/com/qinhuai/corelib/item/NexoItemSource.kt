package com.qinhuai.corelib.item

import com.qinhuai.corelib.nexo.NexoBridge
import org.bukkit.inventory.ItemStack

object NexoItemSource : ItemSource {
    override val id: String = "nexo"

    override fun getItem(id: String, amount: Int): ItemStack? {
        val nexoId = id.substringAfterLast(':')
        return NexoBridge.buildItemStack(nexoId, amount)
    }

    override fun isAvailable(): Boolean = NexoBridge.isAvailable()
}
