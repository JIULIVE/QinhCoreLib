package com.qinhuai.corelib.item

import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack

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

    override fun identify(stack: ItemStack): String? {
        if (!isAvailable()) return null
        return try {
            val tags = Class.forName("com.qinhuai.items.item.QinhItemTags")
            val instance = tags.getField("INSTANCE").get(null)
            tags.getMethod("getId", ItemStack::class.java).invoke(instance, stack) as? String
        } catch (_: Throwable) {
            null
        }
    }
}
