package com.qinhuai.corelib.nexo

import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack

object NexoBridge {
    private var nexoItemsClass: Class<*>? = null
    private var itemFromIdMethod: java.lang.reflect.Method? = null
    private var buildMethod: java.lang.reflect.Method? = null
    private var idFromItemMethod: java.lang.reflect.Method? = null

    fun isAvailable(): Boolean {
        return try {
            val plugin = Bukkit.getPluginManager().getPlugin("Nexo")
            if (plugin == null || !plugin.isEnabled) return false
            if (itemFromIdMethod == null) {
                val classLoader = plugin.javaClass.classLoader
                nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems", true, classLoader)
                itemFromIdMethod = nexoItemsClass!!.getMethod("itemFromId", String::class.java)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun idFromItem(stack: ItemStack): String? {
        if (!isAvailable()) return null
        return try {
            if (idFromItemMethod == null) {
                idFromItemMethod = nexoItemsClass!!.getMethod("idFromItem", ItemStack::class.java)
            }
            idFromItemMethod!!.invoke(null, stack) as? String
        } catch (e: Exception) {
            null
        }
    }

    fun buildItemStack(id: String, amount: Int): ItemStack? {
        if (!isAvailable()) return null
        return try {
            val builder = itemFromIdMethod!!.invoke(null, id) ?: return null
            if (buildMethod == null) buildMethod = builder.javaClass.getMethod("build")
            val item = buildMethod!!.invoke(builder) as? ItemStack ?: return null
            item.clone().apply { this.amount = amount.coerceAtLeast(1) }
        } catch (e: Exception) {
            null
        }
    }
}
