package com.qinhuai.corelib.customgui

import org.bukkit.inventory.ItemStack

/**
 * 分页/动态槽位的一条数据。外观由 YAML 的 item-template 决定，数据由插件 Provider 填入占位符。
 */
data class GuiPaginationEntry(
    val placeholders: Map<String, String> = emptyMap(),
    /** 若提供则作为物品底图（可再套用 template 的 name/lore/cmd） */
    val displayItem: ItemStack? = null,
    val leftAction: String? = null,
    val rightAction: String? = null,
    val action: String? = null
)
