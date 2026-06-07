package com.qinhuai.corelib.customgui

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

/** 跨插件 GUI 占位符 — fun interface 避免 Kotlin Function1 类加载器冲突。 */
fun interface CustomGuiPlaceholderProvider {
    fun resolve(key: String): String?
}

/** 跨插件 GUI 动作 — fun interface 避免 Kotlin Function3 类加载器冲突。 */
fun interface CustomGuiActionHandler {
    fun onAction(action: String, player: Player, clickType: ClickType)
}
