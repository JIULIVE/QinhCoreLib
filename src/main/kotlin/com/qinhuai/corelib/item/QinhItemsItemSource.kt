package com.qinhuai.corelib.item

import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack

/**
 * 软依赖 QinhItems；插件未加载时不可用。
 * QI 启用后也可自行 register 覆盖本实现。
 */
object QinhItemsItemSource : ItemSource {
    override val id: String = "qinhitems"

    override fun isAvailable(): Boolean {
        val plugin = Bukkit.getPluginManager().getPlugin("QinhItems") ?: return false
        return plugin.isEnabled
    }

    override fun getItem(id: String, amount: Int): ItemStack? {
        if (!isAvailable()) return null
        return try {
            val registry = Class.forName("com.qinhuai.items.item.QinhItemRegistry")
            registry.getMethod("create", String::class.java, Int::class.javaPrimitiveType)
                .invoke(null, id, amount) as? ItemStack
        } catch (_: Throwable) {
            null
        }
    }
}
