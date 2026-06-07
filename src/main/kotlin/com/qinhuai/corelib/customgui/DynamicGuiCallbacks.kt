package com.qinhuai.corelib.customgui

import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory

/** 跨插件 GUI 渲染 — fun interface 避免 Kotlin Function2 类加载器冲突。 */
fun interface DynamicGuiRenderer {
    fun render(gui: DynamicGui, inventory: Inventory)
}

/** 跨插件 GUI 点击 — fun interface 避免 Kotlin Function1 类加载器冲突。 */
fun interface DynamicGuiClickHandler {
    fun onClick(clickType: ClickType): Boolean
}
