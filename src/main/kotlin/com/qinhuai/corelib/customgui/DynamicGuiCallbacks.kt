package com.qinhuai.corelib.customgui

import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory

fun interface DynamicGuiRenderer {
    fun render(gui: DynamicGui, inventory: Inventory)
}

fun interface DynamicGuiClickHandler {
    fun onClick(clickType: ClickType): Boolean
}
