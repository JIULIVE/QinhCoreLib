package com.qinhuai.corelib.util

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

object ItemUtils {
    
    fun isEmpty(item: ItemStack?): Boolean {
        return item == null || item.type == Material.AIR || item.amount <= 0
    }
    
    fun isNotEmpty(item: ItemStack?): Boolean {
        return !isEmpty(item)
    }
    
    fun createItem(
        material: Material,
        amount: Int = 1,
        name: String? = null,
        lore: List<String> = emptyList(),
        customModelData: Int? = null
    ): ItemStack {
        val item = ItemStack(material, amount)
        val meta = item.itemMeta ?: return item
        
        name?.let { meta.displayName(TextUtil.toComponent(it)) }
        if (lore.isNotEmpty()) {
            meta.lore(lore.map { TextUtil.toComponent(it) })
        }
        customModelData?.let { meta.setCustomModelData(it) }
        
        item.itemMeta = meta
        return item
    }
    
    fun setDisplayName(item: ItemStack, name: String) {
        val meta = item.itemMeta ?: return
        meta.displayName(TextUtil.toComponent(name))
        item.itemMeta = meta
    }
    
    fun getDisplayName(item: ItemStack): String? {
        val meta = item.itemMeta ?: return null
        return meta.displayName()?.let {
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(it)
        }
    }
    
    fun setLore(item: ItemStack, lore: List<String>) {
        val meta = item.itemMeta ?: return
        meta.lore(lore.map { TextUtil.toComponent(it) })
        item.itemMeta = meta
    }
    
    fun getLore(item: ItemStack): List<String> {
        val meta = item.itemMeta ?: return emptyList()
        return meta.lore()?.map {
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(it)
        } ?: emptyList()
    }
    
    fun addLoreLine(item: ItemStack, line: String) {
        val meta = item.itemMeta ?: return
        val lore = meta.lore()?.toMutableList() ?: mutableListOf()
        lore.add(TextUtil.toComponent(line))
        meta.lore(lore)
        item.itemMeta = meta
    }
    
    fun setCustomModelData(item: ItemStack, data: Int) {
        val meta = item.itemMeta ?: return
        meta.setCustomModelData(data)
        item.itemMeta = meta
    }
    
    fun getCustomModelData(item: ItemStack): Int? {
        val meta = item.itemMeta ?: return null
        return if (meta.hasCustomModelData()) meta.customModelData else null
    }
    
    fun hasCustomModelData(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        return meta.hasCustomModelData()
    }
    
    fun isSimilar(item1: ItemStack?, item2: ItemStack?): Boolean {
        if (item1 == null || item2 == null) return false
        if (item1.type != item2.type) return false
        
        val meta1 = item1.itemMeta
        val meta2 = item2.itemMeta
        
        if (meta1?.hasCustomModelData() != meta2?.hasCustomModelData()) return false
        if (meta1?.customModelData != meta2?.customModelData) return false
        
        return true
    }
    
    fun clone(item: ItemStack, amount: Int? = null): ItemStack {
        val clone = item.clone()
        amount?.let { clone.amount = it }
        return clone
    }
    
    fun takeItem(item: ItemStack, amount: Int = 1): ItemStack {
        val result = clone(item, amount.coerceAtMost(item.amount))
        item.amount = (item.amount - amount).coerceAtLeast(0)
        return result
    }
    
    fun compareWithNbt(item1: ItemStack, item2: ItemStack): Boolean {
        if (item1.type != item2.type) return false
        if (item1.amount != item2.amount) return false
        
        val meta1 = item1.itemMeta
        val meta2 = item2.itemMeta
        
        return meta1 == meta2
    }
}
