package com.qinhuai.corelib.api.item.module

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

interface ItemModule {
    fun build(player: Player?, id: String): ItemStack?

    fun buildWithParams(player: Player?, id: String, paramsJson: String?): ItemStack? =
        build(player, id)
}
