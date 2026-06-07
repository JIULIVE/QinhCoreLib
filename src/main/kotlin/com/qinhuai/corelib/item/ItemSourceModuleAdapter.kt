package com.qinhuai.corelib.item

import com.qinhuai.corelib.api.item.module.ItemModule
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ItemSourceModuleAdapter(private val source: ItemSource) : ItemModule {
    override fun build(player: Player?, id: String): ItemStack? = source.getItem(id, 1)
}
