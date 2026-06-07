package com.qinhuai.corelib.customcrops

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.reflection.ReflectionBridge
import com.qinhuai.corelib.reflection.ReflectionCache
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

object CustomCropsBridge {
    private var available: Boolean? = null
    private val apiCache: ReflectionCache = ReflectionCache("net.momirealms.customcrops.api.CustomCropsAPI")
    private val cropCache: ReflectionCache? = null
    
    fun isAvailable(): Boolean {
        if (available != null) return available!!
        return try {
            ReflectionBridge.isClassAvailable("net.momirealms.customcrops.api.CustomCropsAPI")
            available = true
            true
        } catch (e: Exception) {
            available = false
            false
        }
    }
    
    fun getInstance(): Any? {
        if (!isAvailable()) return null
        return try {
            val method = apiCache.getMethod("getInstance") ?: return null
            apiCache.invokeStatic(method)
        } catch (e: Throwable) {
            null
        }
    }
    
    fun getCropManager(): Any? {
        val instance = getInstance() ?: return null
        return try {
            val method = apiCache.findMethodByName("getCropManager") ?: return null
            apiCache.invoke(method, instance)
        } catch (e: Throwable) {
            null
        }
    }
    
    fun getMechanicManager(): Any? {
        val instance = getInstance() ?: return null
        return try {
            val method = apiCache.findMethodByName("getMechanicManager") ?: return null
            apiCache.invoke(method, instance)
        } catch (e: Throwable) {
            null
        }
    }
}

class CropManagerWrapper(private val cropManager: Any) {
    private val cache: ReflectionCache = ReflectionCache(cropManager.javaClass.name)
    
    fun isCrop(block: Block): Boolean {
        return try {
            val method = cache.findMethodByName("isCrop") ?: return false
            cache.invoke(method, cropManager, block) as? Boolean ?: false
        } catch (e: Throwable) {
            false
        }
    }
    
    fun isCrop(location: Location): Boolean {
        val block = location.block
        return isCrop(block)
    }
    
    fun getCropId(block: Block): String? {
        return try {
            val method = cache.findMethodByName("getCropId") ?: return null
            cache.invoke(method, cropManager, block) as? String
        } catch (e: Throwable) {
            null
        }
    }
    
    fun getCropId(location: Location): String? {
        val block = location.block
        return getCropId(block)
    }
    
    fun tryHarvest(block: Block, player: Player?): Boolean {
        return try {
            val method = cache.findMethodByName("tryHarvest") ?: return false
            when {
                player != null -> cache.invoke(method, cropManager, block, player) as? Boolean ?: false
                else -> cache.invoke(method, cropManager, block) as? Boolean ?: false
            }
        } catch (e: Throwable) {
            false
        }
    }
    
    fun tryPlant(location: Location, player: Player, item: ItemStack, cropId: String): Boolean {
        return try {
            val method = cache.findMethodByName("tryPlant") ?: return false
            cache.invoke(method, cropManager, location, player, item, cropId) as? Boolean ?: false
        } catch (e: Throwable) {
            false
        }
    }
    
    fun tryGrow(location: Location, boneMeal: Boolean = false, multiplier: Double = 1.0): Boolean {
        return try {
            val method = cache.findMethodByName("tryGrow") ?: return false
            when (method.parameterCount) {
                1 -> cache.invoke(method, cropManager, location) as? Boolean ?: false
                2 -> cache.invoke(method, cropManager, location, boneMeal) as? Boolean ?: false
                3 -> cache.invoke(method, cropManager, location, boneMeal, multiplier) as? Boolean ?: false
                else -> false
            }
        } catch (e: Throwable) {
            false
        }
    }
}

class MechanicManagerWrapper(private val mechanicManager: Any) {
    private val cache: ReflectionCache = ReflectionCache(mechanicManager.javaClass.name)
    
    fun getBoneMealItem(): ItemStack? {
        return try {
            val method = cache.findMethodByName("getBoneMealItem") ?: return null
            cache.invoke(method, mechanicManager) as? ItemStack
        } catch (e: Throwable) {
            null
        }
    }
    
    fun isBoneMeal(item: ItemStack): Boolean {
        return try {
            val method = cache.findMethodByName("isBoneMeal") ?: return false
            cache.invoke(method, mechanicManager, item) as? Boolean ?: false
        } catch (e: Throwable) {
            false
        }
    }
}

object CustomCropsManager {
    private var cropManager: CropManagerWrapper? = null
    private var mechanicManager: MechanicManagerWrapper? = null
    
    fun init(): Boolean {
        if (!CustomCropsBridge.isAvailable()) {
            return false
        }
        
        val cm = CustomCropsBridge.getCropManager()
        if (cm != null) {
            cropManager = CropManagerWrapper(cm)
        }
        
        val mm = CustomCropsBridge.getMechanicManager()
        if (mm != null) {
            mechanicManager = MechanicManagerWrapper(mm)
        }
        
        return cropManager != null
    }
    
    fun getCropManager(): CropManagerWrapper? = cropManager
    fun getMechanicManager(): MechanicManagerWrapper? = mechanicManager
    
    fun isAvailable(): Boolean = CustomCropsBridge.isAvailable()
    
    fun isCrop(block: Block): Boolean {
        return cropManager?.isCrop(block) ?: false
    }
    
    fun isCrop(location: Location): Boolean {
        return cropManager?.isCrop(location) ?: false
    }
    
    fun getCropId(block: Block): String? {
        return cropManager?.getCropId(block)
    }
    
    fun getCropId(location: Location): String? {
        return cropManager?.getCropId(location)
    }
    
    fun tryHarvest(block: Block, player: Player? = null): Boolean {
        return cropManager?.tryHarvest(block, player) ?: false
    }
    
    fun tryPlant(location: Location, player: Player, item: ItemStack, cropId: String): Boolean {
        return cropManager?.tryPlant(location, player, item, cropId) ?: false
    }
    
    fun tryGrow(location: Location, boneMeal: Boolean = false, multiplier: Double = 1.0): Boolean {
        return cropManager?.tryGrow(location, boneMeal, multiplier) ?: false
    }
    
    fun isBoneMeal(item: ItemStack): Boolean {
        return mechanicManager?.isBoneMeal(item) ?: false
    }
    
    fun clear() {
        cropManager = null
        mechanicManager = null
    }
}
