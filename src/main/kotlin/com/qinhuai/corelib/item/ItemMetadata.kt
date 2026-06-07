package com.qinhuai.corelib.item

import com.qinhuai.corelib.QinhCoreLib
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

class ItemMetadata(private val namespace: String) {
    
    fun createKey(key: String): NamespacedKey {
        return NamespacedKey(QinhCoreLib.instance, "${namespace}_$key")
    }
    
    fun <T : Any, Z : Any> set(item: ItemStack, key: String, type: PersistentDataType<T, Z>, value: Z) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        container.set(createKey(key), type, value)
        item.itemMeta = meta
    }

    fun <T : Any, Z : Any> get(item: ItemStack, key: String, type: PersistentDataType<T, Z>): Z? {
        val meta = item.itemMeta ?: return null
        val container = meta.persistentDataContainer
        return container.get(createKey(key), type)
    }
    
    fun has(item: ItemStack, key: String): Boolean {
        val meta = item.itemMeta ?: return false
        val container = meta.persistentDataContainer
        return container.has(createKey(key))
    }
    
    fun remove(item: ItemStack, key: String) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        container.remove(createKey(key))
        item.itemMeta = meta
    }
    
    fun setString(item: ItemStack, key: String, value: String) {
        set(item, key, PersistentDataType.STRING, value)
    }
    
    fun getString(item: ItemStack, key: String): String? {
        return get(item, key, PersistentDataType.STRING)
    }
    
    fun setInt(item: ItemStack, key: String, value: Int) {
        set(item, key, PersistentDataType.INTEGER, value)
    }
    
    fun getInt(item: ItemStack, key: String): Int? {
        return get(item, key, PersistentDataType.INTEGER)
    }
    
    fun getIntOrDefault(item: ItemStack, key: String, default: Int): Int {
        return getInt(item, key) ?: default
    }
    
    fun setLong(item: ItemStack, key: String, value: Long) {
        set(item, key, PersistentDataType.LONG, value)
    }
    
    fun getLong(item: ItemStack, key: String): Long? {
        return get(item, key, PersistentDataType.LONG)
    }
    
    fun setDouble(item: ItemStack, key: String, value: Double) {
        set(item, key, PersistentDataType.DOUBLE, value)
    }
    
    fun getDouble(item: ItemStack, key: String): Double? {
        return get(item, key, PersistentDataType.DOUBLE)
    }
    
    fun setBoolean(item: ItemStack, key: String, value: Boolean) {
        setInt(item, key, if (value) 1 else 0)
    }
    
    fun getBoolean(item: ItemStack, key: String): Boolean? {
        return getInt(item, key)?.let { it == 1 }
    }
    
    fun getBooleanOrDefault(item: ItemStack, key: String, default: Boolean): Boolean {
        return getBoolean(item, key) ?: default
    }
    
    fun copyTo(from: ItemStack, to: ItemStack, keys: List<String>) {
        val fromMeta = from.itemMeta ?: return
        val toMeta = to.itemMeta ?: return
        val fromContainer = fromMeta.persistentDataContainer
        val toContainer = toMeta.persistentDataContainer
        
        for (key in keys) {
            val namespacedKey = createKey(key)
            if (fromContainer.has(namespacedKey)) {
                val value = fromContainer.get(namespacedKey, PersistentDataType.STRING)
                value?.let {
                    toContainer.set(namespacedKey, PersistentDataType.STRING, it)
                }
            }
        }
        to.itemMeta = toMeta
    }
}

object ItemMetadataManager {
    private val metadataInstances = mutableMapOf<String, ItemMetadata>()
    
    fun get(namespace: String): ItemMetadata {
        return metadataInstances.getOrPut(namespace) { ItemMetadata(namespace) }
    }
}

object TypeManager {
    private val itemTypeKey = ItemMetadataManager.get("qcl").createKey("type")
    
    fun setType(item: ItemStack, type: String) {
        val meta = item.itemMeta ?: return
        meta.persistentDataContainer.set(itemTypeKey, PersistentDataType.STRING, type)
        item.itemMeta = meta
    }
    
    fun getType(item: ItemStack): String? {
        val meta = item.itemMeta ?: return null
        return meta.persistentDataContainer.get(itemTypeKey, PersistentDataType.STRING)
    }
    
    fun isType(item: ItemStack, type: String): Boolean {
        return getType(item) == type
    }
}
