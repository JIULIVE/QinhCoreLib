package com.qinhuai.corelib.customblock

import com.qinhuai.corelib.craftengine.CraftEngineManager
import org.bukkit.Location
import org.bukkit.block.Block

object CraftEngineBlockProvider : CustomBlockProvider {
    override val id: String = "craftengine"
    
    override fun isAvailable(): Boolean {
        return CraftEngineManager.isAvailable()
    }
    
    override fun isCustomBlock(block: Block): Boolean {
        return CraftEngineManager.isCustomBlock(block)
    }
    
    override fun getCustomBlockId(block: Block): String? {
        return CraftEngineManager.getCustomBlockId(block)
    }
    
    override fun getCustomBlockAt(location: Location): String? {
        return getCustomBlockId(location.block)
    }
    
    fun placeBlock(location: Location, blockId: String, playSound: Boolean = true): Boolean {
        return CraftEngineManager.placeBlock(location, blockId, playSound)
    }
    
    fun removeBlock(block: Block): Boolean {
        return CraftEngineManager.removeBlock(block)
    }
}
