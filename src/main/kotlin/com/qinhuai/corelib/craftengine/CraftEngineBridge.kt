package com.qinhuai.corelib.craftengine

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack

object CraftEngineBridge {
    private var craftEngineItemsClass: Class<*>? = null
    private var craftEngineBlocksClass: Class<*>? = null
    private var craftEngineFurnitureClass: Class<*>? = null
    private var keyClass: Class<*>? = null
    private var byIdMethod: java.lang.reflect.Method? = null
    private var getCustomItemIdMethod: java.lang.reflect.Method? = null
    private var keyOfMethod: java.lang.reflect.Method? = null
    private var buildItemStackMethod: java.lang.reflect.Method? = null
    private var itemBuildContextClass: Class<*>? = null
    private var itemBuildContextEmptyMethod: java.lang.reflect.Method? = null
    
    private var placeBlockMethod: java.lang.reflect.Method? = null
    private var removeBlockMethod: java.lang.reflect.Method? = null
    private var isCustomBlockMethod: java.lang.reflect.Method? = null
    private var getCustomBlockStateMethod: java.lang.reflect.Method? = null
    
    private var placeFurnitureMethod: java.lang.reflect.Method? = null
    private var removeFurnitureMethod: java.lang.reflect.Method? = null
    private var isFurnitureMethod: java.lang.reflect.Method? = null
    private var getFurnitureIdMethod: java.lang.reflect.Method? = null
    
    fun isAvailable(): Boolean {
        try {
            val pluginManager = org.bukkit.Bukkit.getPluginManager()
            val craftEnginePlugin = pluginManager.getPlugin("CraftEngine")
            
            if (craftEnginePlugin == null || !craftEnginePlugin.isEnabled) {
                return false
            }
            
            val classLoader = craftEnginePlugin.javaClass.classLoader
            
            craftEngineItemsClass = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineItems", true, classLoader)
            craftEngineBlocksClass = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineBlocks", true, classLoader)
            
            try {
                craftEngineFurnitureClass = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineFurniture", true, classLoader)
            } catch (e: Exception) {
            }
            
            keyClass = Class.forName("net.momirealms.craftengine.core.util.Key", true, classLoader)
            itemBuildContextClass = Class.forName("net.momirealms.craftengine.core.item.ItemBuildContext", true, classLoader)
            
            byIdMethod = craftEngineItemsClass!!.getMethod("byId", keyClass)
            getCustomItemIdMethod = craftEngineItemsClass!!.getMethod("getCustomItemId", ItemStack::class.java)
            keyOfMethod = keyClass!!.getMethod("of", String::class.java)
            itemBuildContextEmptyMethod = itemBuildContextClass!!.getMethod("empty")
            
            placeBlockMethod = findMatchingMethod(craftEngineBlocksClass!!, "place", Location::class.java, keyClass!!)
            removeBlockMethod = findMatchingMethod(craftEngineBlocksClass!!, "remove", Block::class.java)
            isCustomBlockMethod = findMatchingMethod(craftEngineBlocksClass!!, "isCustomBlock", Block::class.java)
            getCustomBlockStateMethod = findMatchingMethod(craftEngineBlocksClass!!, "getCustomBlockState", Block::class.java)
            
            if (craftEngineFurnitureClass != null) {
                placeFurnitureMethod = findMatchingMethod(craftEngineFurnitureClass!!, "place", Location::class.java, keyClass!!)
                removeFurnitureMethod = findMatchingMethod(craftEngineFurnitureClass!!, "remove", Entity::class.java)
                isFurnitureMethod = findMatchingMethod(craftEngineFurnitureClass!!, "isFurniture", Entity::class.java)
                getFurnitureIdMethod = findMatchingMethod(craftEngineFurnitureClass!!, "getId", Entity::class.java)
            }
            
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    private fun findMatchingMethod(clazz: Class<*>, name: String, vararg paramTypes: Class<*>): java.lang.reflect.Method? {
        return try {
            clazz.getMethod(name, *paramTypes)
        } catch (e: NoSuchMethodException) {
            clazz.methods.find { m ->
                if (m.name != name) return@find false
                if (m.parameterTypes.size < paramTypes.size) return@find false
                paramTypes.withIndex().all { (i, type) ->
                    type.isAssignableFrom(m.parameterTypes[i])
                }
            }
        }
    }
    
    fun buildItemStack(identifier: String, amount: Int): ItemStack? {
        if (!isAvailable()) return null
        return try {
            val key = keyOfMethod!!.invoke(null, identifier)
            val customItem = byIdMethod!!.invoke(null, key)
            
            if (customItem != null) {
                val buildItemStackMethod = findMatchingMethod(customItem.javaClass, "buildBukkitItem", Int::class.javaPrimitiveType as Class<*>)
                    ?: findMatchingMethod(customItem.javaClass, "buildItem", Int::class.javaPrimitiveType as Class<*>)
                    ?: findMatchingMethod(customItem.javaClass, "buildBukkitItem")
                    ?: findMatchingMethod(customItem.javaClass, "buildItem")
                
                if (buildItemStackMethod == null) {
                    return null
                }
                
                val itemBuildContext = itemBuildContextEmptyMethod!!.invoke(null)
                
                val item = when {
                    buildItemStackMethod.parameterTypes.size == 2 && 
                        buildItemStackMethod.parameterTypes[0].name.contains("ItemBuildContext") && 
                        buildItemStackMethod.parameterTypes[1] == Int::class.javaPrimitiveType -> {
                        buildItemStackMethod.invoke(customItem, itemBuildContext, amount)
                    }
                    buildItemStackMethod.parameterTypes.size == 1 && 
                        buildItemStackMethod.parameterTypes[0].name.contains("ItemBuildContext") -> {
                        val result = buildItemStackMethod.invoke(customItem, itemBuildContext)
                        if (result is ItemStack && amount > 1) {
                            result.amount = amount
                        }
                        result
                    }
                    buildItemStackMethod.parameterTypes.size == 1 && 
                        buildItemStackMethod.parameterTypes[0] == Int::class.javaPrimitiveType -> {
                        buildItemStackMethod.invoke(customItem, amount)
                    }
                    else -> {
                        val result = buildItemStackMethod.invoke(customItem)
                        if (result is ItemStack && amount > 1) {
                            result.amount = amount
                        }
                        result
                    }
                }
                
                item as? ItemStack
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun getCustomItemId(itemStack: ItemStack): String? {
        if (!isAvailable()) return null
        return try {
            val key = getCustomItemIdMethod!!.invoke(null, itemStack)
            if (key != null) {
                val toStringMethod = key.javaClass.getMethod("toString")
                toStringMethod.invoke(key) as? String
            } else {
                null
            }
        } catch (e: Exception) {
            println("[QCL-CraftEngine] getCustomItemId error: $e")
            null
        }
    }
    
    fun placeBlock(location: Location, blockId: String, playSound: Boolean = true): Boolean {
        if (!isAvailable()) return false
        if (placeBlockMethod == null) return false
        return try {
            val key = keyOfMethod!!.invoke(null, blockId)
            val paramTypes = placeBlockMethod!!.parameterTypes
            println("[QCL-CraftEngine] placeBlock method has ${paramTypes.size} params: ${paramTypes.joinToString { it.simpleName }}")
            
            when {
                paramTypes.size == 4 -> {
                    placeBlockMethod!!.invoke(null, location, key, null, playSound) as Boolean
                }
                paramTypes.size >= 3 -> {
                    placeBlockMethod!!.invoke(null, location, key, playSound) as Boolean
                }
                else -> {
                    placeBlockMethod!!.invoke(null, location, key) as Boolean
                }
            }
        } catch (e: Exception) {
            println("[QCL-CraftEngine] placeBlock error: $e")
            e.printStackTrace()
            false
        }
    }
    
    fun removeBlock(block: Block): Boolean {
        if (!isAvailable()) return false
        if (removeBlockMethod == null) return false
        return try {
            removeBlockMethod!!.invoke(null, block) as Boolean
        } catch (e: Exception) {
            println("[QCL-CraftEngine] removeBlock error: $e")
            false
        }
    }
    
    fun isCustomBlock(block: Block): Boolean {
        if (!isAvailable()) return false
        if (isCustomBlockMethod == null) return false
        return try {
            isCustomBlockMethod!!.invoke(null, block) as Boolean
        } catch (e: Exception) {
            println("[QCL-CraftEngine] isCustomBlock error: $e")
            false
        }
    }
    
    fun getCustomBlockId(block: Block): String? {
        if (!isAvailable()) return null
        if (getCustomBlockStateMethod == null) return null
        return try {
            val state = getCustomBlockStateMethod!!.invoke(null, block)
            if (state != null) {
                val getOwnerMethod = state.javaClass.getMethod("getOwner")
                val owner = getOwnerMethod.invoke(state)
                if (owner != null) {
                    val getIdMethod = owner.javaClass.getMethod("getId")
                    val id = getIdMethod.invoke(owner)
                    if (id != null) {
                        val toStringMethod = id.javaClass.getMethod("toString")
                        return toStringMethod.invoke(id) as? String
                    }
                }
            }
            null
        } catch (e: Exception) {
            println("[QCL-CraftEngine] getCustomBlockId error: $e")
            null
        }
    }
    
    fun placeFurniture(location: Location, furnitureId: String): Entity? {
        if (!isAvailable()) return null
        if (placeFurnitureMethod == null) return null
        return try {
            val key = keyOfMethod!!.invoke(null, furnitureId)
            placeFurnitureMethod!!.invoke(null, location, key) as? Entity
        } catch (e: Exception) {
            println("[QCL-CraftEngine] placeFurniture error: $e")
            null
        }
    }
    
    fun removeFurniture(entity: Entity): Boolean {
        if (!isAvailable()) return false
        if (removeFurnitureMethod == null) return false
        return try {
            removeFurnitureMethod!!.invoke(null, entity) as Boolean
        } catch (e: Exception) {
            println("[QCL-CraftEngine] removeFurniture error: $e")
            false
        }
    }
    
    fun isFurniture(entity: Entity): Boolean {
        if (!isAvailable()) return false
        if (isFurnitureMethod == null) return false
        return try {
            isFurnitureMethod!!.invoke(null, entity) as Boolean
        } catch (e: Exception) {
            println("[QCL-CraftEngine] isFurniture error: $e")
            false
        }
    }
    
    fun getFurnitureId(entity: Entity): String? {
        if (!isAvailable()) return null
        
        var result: String? = null
        try {
            entity.javaClass.getMethod("getId")?.let { getIdMethod ->
                val id = getIdMethod.invoke(entity)
                if (id != null) {
                    val toStringMethod = id.javaClass.getMethod("toString")
                    result = toStringMethod.invoke(id) as? String
                }
            }
        } catch (e: Exception) {
            println("[QCL-CraftEngine] getFurnitureId (from entity) error: $e")
        }
        
        if (result != null) return result
        
        if (getFurnitureIdMethod != null) {
            try {
                val key = getFurnitureIdMethod!!.invoke(null, entity)
                if (key != null) {
                    val toStringMethod = key.javaClass.getMethod("toString")
                    return toStringMethod.invoke(key) as? String
                }
            } catch (e: Exception) {
                println("[QCL-CraftEngine] getFurnitureId error: $e")
            }
        }
        return null
    }
}

object CraftEngineManager {
    fun isAvailable(): Boolean = CraftEngineBridge.isAvailable()
    
    fun buildItemStack(identifier: String, amount: Int): ItemStack? = CraftEngineBridge.buildItemStack(identifier, amount)
    
    fun getCustomItemId(itemStack: ItemStack): String? = CraftEngineBridge.getCustomItemId(itemStack)
    
    fun placeBlock(location: Location, blockId: String, playSound: Boolean = true): Boolean = 
        CraftEngineBridge.placeBlock(location, blockId, playSound)
    
    fun removeBlock(block: Block): Boolean = CraftEngineBridge.removeBlock(block)
    
    fun isCustomBlock(block: Block): Boolean = CraftEngineBridge.isCustomBlock(block)
    
    fun getCustomBlockId(block: Block): String? = CraftEngineBridge.getCustomBlockId(block)
    
    fun placeFurniture(location: Location, furnitureId: String): org.bukkit.entity.Entity? = 
        CraftEngineBridge.placeFurniture(location, furnitureId)
    
    fun removeFurniture(entity: org.bukkit.entity.Entity): Boolean = 
        CraftEngineBridge.removeFurniture(entity)
    
    fun isFurniture(entity: org.bukkit.entity.Entity): Boolean = 
        CraftEngineBridge.isFurniture(entity)
    
    fun getFurnitureId(entity: org.bukkit.entity.Entity): String? = 
        CraftEngineBridge.getFurnitureId(entity)
    
    fun clear() {
    }
}
