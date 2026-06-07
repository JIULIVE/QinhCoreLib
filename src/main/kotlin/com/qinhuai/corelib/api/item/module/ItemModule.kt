package com.qinhuai.corelib.api.item.module

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * 物品库兼容模块 — 与传奇 LegendCore ItemModule 同形，Groovy / Java 均可实现。
 */
interface ItemModule {
    fun build(player: Player?, id: String): ItemStack?

    fun buildWithParams(player: Player?, id: String, paramsJson: String?): ItemStack? =
        build(player, id)
}
