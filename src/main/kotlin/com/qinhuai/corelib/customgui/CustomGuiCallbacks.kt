package com.qinhuai.corelib.customgui

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

fun interface CustomGuiPlaceholderProvider {
    fun resolve(key: String): String?
}

fun interface CustomGuiActionHandler {
    fun onAction(action: String, player: Player, clickType: ClickType)
}
