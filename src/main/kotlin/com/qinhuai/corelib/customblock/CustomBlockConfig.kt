package com.qinhuai.corelib.customblock

import com.qinhuai.corelib.craftengine.CraftEngineManager
import com.qinhuai.corelib.item.ItemSourceManager
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack

data class CustomBlockConfig(
    val id: String,
    val displayName: String,
    val itemSources: List<String>,
    val fallbackMaterial: Material = Material.CHEST,
    val fallbackCustomModelData: Int? = null
) {
    fun getItem(amount: Int = 1): ItemStack? {
        for (source in itemSources) {
            val item = ItemSourceManager.parseItemReference(source)
            if (item != null) {
                item.amount = amount
                return item
            }
        }
        
        val item = ItemStack(fallbackMaterial, amount)
        if (fallbackCustomModelData != null) {
            val meta = item.itemMeta
            meta?.setCustomModelData(fallbackCustomModelData)
            item.itemMeta = meta
        }
        return item
    }
    
    fun place(location: Location, player: org.bukkit.entity.Player? = null, playSound: Boolean = true): PlaceResult {
        val rotatedLocation = location.clone()
        if (player != null) {
            rotatedLocation.yaw = player.location.yaw
            rotatedLocation.pitch = player.location.pitch
        }
        
        for (source in itemSources) {
            if (source.startsWith("craftengine-")) {
                val idPart = source.removePrefix("craftengine-")
                val identifier = idPart.replaceFirst("_", ":")
                
                // 优先使用家具
                val entity = CraftEngineManager.placeFurniture(rotatedLocation, identifier)
                if (entity != null) {
                    return PlaceResult.Furniture(entity)
                }
                
                // 家具失败后尝试方块
                val success = CraftEngineManager.placeBlock(location, identifier, playSound)
                if (success) {
                    return PlaceResult.Block(location.block)
                }
            }
        }
        location.block.type = fallbackMaterial
        return PlaceResult.Fallback(location.block)
    }
    
    @Deprecated("Use place() instead", ReplaceWith("place(location, null, playSound)"))
    fun placeBlock(location: Location, playSound: Boolean = true): Boolean {
        val result = place(location, null, playSound)
        return result !is PlaceResult.Fallback || location.block.type == fallbackMaterial
    }
    
    fun isThis(obj: Any): Boolean {
        when (obj) {
            is Block -> {
                if (CraftEngineManager.isCustomBlock(obj)) {
                    val blockId = CraftEngineManager.getCustomBlockId(obj)
                    if (blockId != null) {
                        for (source in itemSources) {
                            if (source.startsWith("craftengine-")) {
                                val sourceIdPart = source.removePrefix("craftengine-")
                                val sourceId = sourceIdPart.replaceFirst("_", ":")
                                if (blockId == sourceId) {
                                    return true
                                }
                            }
                        }
                    }
                }
                return obj.type == fallbackMaterial
            }
            is Entity -> {
                if (CraftEngineManager.isFurniture(obj)) {
                    val furnitureId = CraftEngineManager.getFurnitureId(obj)
                    if (furnitureId != null) {
                        for (source in itemSources) {
                            if (source.startsWith("craftengine-")) {
                                val sourceIdPart = source.removePrefix("craftengine-")
                                val sourceId = sourceIdPart.replaceFirst("_", ":")
                                if (furnitureId == sourceId) {
                                    return true
                                }
                            }
                        }
                    }
                }
                return false
            }
            else -> return false
        }
    }
    
    @Deprecated("Use isThis() instead", ReplaceWith("isThis(block)"))
    fun isThisBlock(block: Block): Boolean = isThis(block)
    
    fun remove(obj: Any, dropLoot: Boolean = true, playSound: Boolean = true): Boolean {
        when (obj) {
            is Block -> {
                if (CraftEngineManager.isCustomBlock(obj)) {
                    return CraftEngineManager.removeBlock(obj)
                }
                obj.type = Material.AIR
                return true
            }
            is Entity -> {
                if (CraftEngineManager.isFurniture(obj)) {
                    return CraftEngineManager.removeFurniture(obj)
                }
                return false
            }
            else -> return false
        }
    }
    
    @Deprecated("Use remove() instead", ReplaceWith("remove(block)"))
    fun removeBlock(block: Block): Boolean = remove(block)
    
    companion object {
        fun fromConfig(section: ConfigurationSection, fallbackMaterial: Material = Material.CHEST): CustomBlockConfig {
            val materialName = section.getString("material")
            val mat = if (materialName != null) {
                try {
                    Material.valueOf(materialName)
                } catch (e: Exception) {
                    fallbackMaterial
                }
            } else {
                fallbackMaterial
            }
            
            val cmd = section.getInt("custom-model-data", -1)
            
            return CustomBlockConfig(
                id = section.name,
                displayName = section.getString("displayname", section.name)!!,
                itemSources = section.getStringList("item_sources"),
                fallbackMaterial = mat,
                fallbackCustomModelData = if (cmd >= 0) cmd else null
            )
        }
    }
}

sealed class PlaceResult {
    data class Furniture(val entity: Entity) : PlaceResult()
    data class Block(val block: org.bukkit.block.Block) : PlaceResult()
    data class Fallback(val block: org.bukkit.block.Block) : PlaceResult()
}
