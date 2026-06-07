package com.qinhuai.corelib.customblock

import org.bukkit.Location
import org.bukkit.block.Block

interface CustomBlockProvider {
    val id: String
    fun isAvailable(): Boolean
    fun isCustomBlock(block: Block): Boolean
    fun getCustomBlockId(block: Block): String?
    fun getCustomBlockAt(location: Location): String?
}

object CustomBlockBridge {
    private val providers = mutableListOf<CustomBlockProvider>()
    
    fun registerProvider(provider: CustomBlockProvider) {
        providers.add(provider)
    }
    
    fun isCustomBlock(block: Block): Boolean {
        return providers.any { it.isAvailable() && it.isCustomBlock(block) }
    }
    
    fun getCustomBlockId(block: Block): String? {
        return providers.firstOrNull { it.isAvailable() }?.getCustomBlockId(block)
    }
    
    fun getCustomBlockAt(location: Location): String? {
        return providers.firstOrNull { it.isAvailable() }?.getCustomBlockAt(location)
    }
}
