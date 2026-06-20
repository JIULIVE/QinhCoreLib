package com.qinhuai.corelib.customgui

import org.bukkit.inventory.ItemStack

data class GuiPaginationEntry(
    val placeholders: Map<String, String> = emptyMap(),
    val displayItem: ItemStack? = null,
    val leftAction: String? = null,
    val rightAction: String? = null,
    val action: String? = null
)
