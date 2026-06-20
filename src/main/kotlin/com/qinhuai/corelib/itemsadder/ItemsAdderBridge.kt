package com.qinhuai.corelib.itemsadder

import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack

object ItemsAdderBridge {
    private var customStackClass: Class<*>? = null
    private var getInstanceMethod: java.lang.reflect.Method? = null
    private var getItemStackMethod: java.lang.reflect.Method? = null
    private var byItemStackMethod: java.lang.reflect.Method? = null
    private var getNamespacedIDMethod: java.lang.reflect.Method? = null

    fun isAvailable(): Boolean {
        return try {
            val plugin = Bukkit.getPluginManager().getPlugin("ItemsAdder")
            if (plugin == null || !plugin.isEnabled) return false
            if (getInstanceMethod == null) {
                val classLoader = plugin.javaClass.classLoader
                customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack", true, classLoader)
                getInstanceMethod = customStackClass!!.getMethod("getInstance", String::class.java)
                getItemStackMethod = customStackClass!!.getMethod("getItemStack")
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun idFromItem(stack: ItemStack): String? {
        if (!isAvailable()) return null
        return try {
            if (byItemStackMethod == null) {
                byItemStackMethod = customStackClass!!.getMethod("byItemStack", ItemStack::class.java)
            }
            val custom = byItemStackMethod!!.invoke(null, stack) ?: return null
            if (getNamespacedIDMethod == null) {
                getNamespacedIDMethod = custom.javaClass.getMethod("getNamespacedID")
            }
            getNamespacedIDMethod!!.invoke(custom) as? String
        } catch (e: Exception) {
            null
        }
    }

    fun buildItemStack(id: String, amount: Int): ItemStack? {
        if (!isAvailable()) return null
        return try {
            val custom = getInstanceMethod!!.invoke(null, id) ?: return null
            val item = getItemStackMethod!!.invoke(custom) as? ItemStack ?: return null
            item.clone().apply { this.amount = amount.coerceAtLeast(1) }
        } catch (e: Exception) {
            null
        }
    }
}
