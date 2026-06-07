package com.qinhuai.corelib.modelengine

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.reflection.ReflectionBridge
import com.qinhuai.corelib.reflection.ReflectionCache
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import java.util.*

object ModelEngineBridge {
    private var available: Boolean? = null
    private val apiCache: ReflectionCache = ReflectionCache("com.ticxo.modelengine.api.ModelEngineAPI")
    private val modeledEntityCache: ReflectionCache? = null
    
    fun isAvailable(): Boolean {
        if (available != null) return available!!
        return try {
            ReflectionBridge.isClassAvailable("com.ticxo.modelengine.api.ModelEngineAPI")
            available = true
            true
        } catch (e: Exception) {
            available = false
            false
        }
    }
    
    fun createModeledEntity(entity: Entity): Any? {
        if (!isAvailable()) return null
        return try {
            val method = apiCache.getMethod("createModeledEntity", Entity::class.java) ?: return null
            apiCache.invokeStatic(method, entity)
        } catch (e: Throwable) {
            null
        }
    }
    
    fun removeModeledEntity(uuid: UUID): Boolean {
        if (!isAvailable()) return false
        return try {
            val method = apiCache.getMethod("removeModeledEntity", UUID::class.java) ?: return false
            apiCache.invokeStatic(method, uuid)
            true
        } catch (e: Throwable) {
            false
        }
    }
    
    fun removeModeledEntity(entity: Entity): Boolean {
        return removeModeledEntity(entity.uniqueId)
    }
    
    fun getModeledEntity(entity: Entity): Any? {
        if (!isAvailable()) return null
        return try {
            val method = apiCache.getMethod("getModeledEntity", Entity::class.java) ?: return null
            apiCache.invokeStatic(method, entity)
        } catch (e: Throwable) {
            null
        }
    }
    
    fun isModeledEntity(uuid: UUID): Boolean {
        if (!isAvailable()) return false
        return try {
            val method = apiCache.getMethod("isModeledEntity", UUID::class.java) ?: return false
            apiCache.invokeStatic(method, uuid) as? Boolean ?: false
        } catch (e: Throwable) {
            false
        }
    }
    
    fun createActiveModel(modelId: String): Any? {
        if (!isAvailable()) return null
        return try {
            val method = apiCache.getMethod("createActiveModel", String::class.java) ?: return null
            apiCache.invokeStatic(method, modelId)
        } catch (e: Throwable) {
            null
        }
    }
}

class ModeledEntityWrapper(private val modeledEntity: Any) {
    private val cache: ReflectionCache = ReflectionCache(modeledEntity.javaClass.name)
    
    fun addModel(activeModel: Any, hitbox: Boolean = true): Boolean {
        return try {
            val method = cache.findMethodByName("addModel") ?: return false
            cache.invoke(method, modeledEntity, activeModel, hitbox)
            true
        } catch (e: Throwable) {
            false
        }
    }
    
    fun removeModel(modelId: String): Boolean {
        return try {
            val method = cache.findMethodByName("removeModel") ?: return false
            cache.invoke(method, modeledEntity, modelId)
            true
        } catch (e: Throwable) {
            false
        }
    }
    
    fun setBaseEntityVisible(visible: Boolean): Boolean {
        return try {
            val method = cache.findMethodByName("setBaseEntityVisible") ?: return false
            cache.invoke(method, modeledEntity, visible)
            true
        } catch (e: Throwable) {
            false
        }
    }
    
    fun destroy(): Boolean {
        return try {
            val method = cache.findMethodByName("destroy") ?: return false
            cache.invoke(method, modeledEntity)
            true
        } catch (e: Throwable) {
            false
        }
    }
    
    fun getModels(): Collection<*>? {
        return try {
            val field = cache.findFieldInHierarchy("models") ?: cache.findFieldInHierarchy("cachedModels")
            field?.let { cache.get(it, modeledEntity) as? Collection<*> }
        } catch (e: Throwable) {
            null
        }
    }
}

class ActiveModelWrapper(private val activeModel: Any) {
    private val cache: ReflectionCache = ReflectionCache(activeModel.javaClass.name)
    
    fun playAnimation(animationName: String, speed: Double = 1.0, force: Boolean = true): Boolean {
        return try {
            val handler = getAnimationHandler() ?: return false
            val handlerCache = ReflectionCache(handler.javaClass.name)
            
            var played = false
            handlerCache.javaClass.declaredMethods.forEach { method ->
                if (method.name == "playAnimation" && !played) {
                    try {
                        when (method.parameterCount) {
                            1 -> method.invoke(handler, animationName)
                            2 -> method.invoke(handler, animationName, speed)
                            3 -> method.invoke(handler, animationName, 0.3, 0.3)
                            4 -> method.invoke(handler, animationName, 0.3, 0.3, speed)
                            5 -> method.invoke(handler, animationName, 0.3, 0.3, speed, force)
                        }
                        played = true
                    } catch (e: Exception) {
                    }
                }
            }
            played
        } catch (e: Throwable) {
            false
        }
    }
    
    fun getAnimationHandler(): Any? {
        return try {
            val method = cache.findMethodByName("getAnimationHandler") ?: return null
            cache.invoke(method, activeModel)
        } catch (e: Throwable) {
            null
        }
    }
    
    fun getBones(): Map<*, *>? {
        return try {
            val method = cache.findMethodByName("getBones") ?: return null
            cache.invoke(method, activeModel) as? Map<*, *>
        } catch (e: Throwable) {
            null
        }
    }
    
    fun setHeldItem(boneName: String, item: ItemStack): Boolean {
        return try {
            val bones = getBones() ?: return false
            val bone = bones[boneName] ?: return false
            val boneCache = ReflectionCache(bone.javaClass.name)
            
            val itemBehaviorTypeClazz = Class.forName("com.ticxo.modelengine.api.model.bone.BoneBehaviorTypes")
            val itemField = itemBehaviorTypeClazz.getField("ITEM")
            val itemBehaviorType = itemField.get(null)
            
            val getBoneBehaviorMethod = boneCache.javaClass.getMethod(
                "getBoneBehavior",
                Class.forName("com.ticxo.modelengine.api.model.bone.BoneBehaviorType")
            )
            val boneBehaviorOpt = getBoneBehaviorMethod.invoke(bone, itemBehaviorType)
            
            val isPresentMethod = Class.forName("java.util.Optional").getMethod("isPresent")
            val isPresent = isPresentMethod.invoke(boneBehaviorOpt) as? Boolean ?: return false
            if (!isPresent) return false
            
            val getMethod = Class.forName("java.util.Optional").getMethod("get")
            val heldItem = getMethod.invoke(boneBehaviorOpt)
            
            val staticItemStackSupplierClazz = Class.forName("com.ticxo.modelengine.api.model.bone.behavior.HeldItem\$StaticItemStackSupplier")
            val constructor = staticItemStackSupplierClazz.getConstructor(ItemStack::class.java)
            val supplier = constructor.newInstance(item)
            
            val setItemProviderMethod = heldItem.javaClass.getMethod(
                "setItemProvider",
                Class.forName("com.ticxo.modelengine.api.model.bone.behavior.HeldItem\$ItemStackSupplier")
            )
            setItemProviderMethod.invoke(heldItem, supplier)
            true
        } catch (e: Throwable) {
            false
        }
    }
}

object ModelEngineManager {
    private val modeledEntities = mutableMapOf<UUID, ModeledEntityWrapper>()
    private val activeModels = mutableMapOf<UUID, MutableMap<String, ActiveModelWrapper>>()
    
    fun spawnModel(
        entity: Entity,
        modelId: String,
        hideBaseEntity: Boolean = true
    ): Pair<ModeledEntityWrapper?, ActiveModelWrapper?> {
        if (!ModelEngineBridge.isAvailable()) {
            return Pair(null, null)
        }
        
        val modeledEntity = ModelEngineBridge.createModeledEntity(entity) ?: return Pair(null, null)
        val modeledWrapper = ModeledEntityWrapper(modeledEntity)
        
        val activeModel = ModelEngineBridge.createActiveModel(modelId) ?: return Pair(modeledWrapper, null)
        val activeWrapper = ActiveModelWrapper(activeModel)
        
        modeledWrapper.addModel(activeModel, true)
        
        if (hideBaseEntity) {
            modeledWrapper.setBaseEntityVisible(false)
        }
        
        modeledEntities[entity.uniqueId] = modeledWrapper
        activeModels.getOrPut(entity.uniqueId) { mutableMapOf() }[modelId] = activeWrapper
        
        return Pair(modeledWrapper, activeWrapper)
    }
    
    fun removeModel(entity: Entity) {
        ModelEngineBridge.removeModeledEntity(entity)
        modeledEntities.remove(entity.uniqueId)
        activeModels.remove(entity.uniqueId)
    }
    
    fun getModeledEntity(entity: Entity): ModeledEntityWrapper? {
        return modeledEntities[entity.uniqueId]
    }
    
    fun getActiveModel(entity: Entity, modelId: String): ActiveModelWrapper? {
        return activeModels[entity.uniqueId]?.get(modelId)
    }
    
    fun playAnimation(entity: Entity, modelId: String, animationName: String): Boolean {
        val activeModel = getActiveModel(entity, modelId) ?: return false
        return activeModel.playAnimation(animationName)
    }
    
    fun setHeldItem(entity: Entity, modelId: String, boneName: String, item: ItemStack): Boolean {
        val activeModel = getActiveModel(entity, modelId) ?: return false
        return activeModel.setHeldItem(boneName, item)
    }
    
    fun clear() {
        modeledEntities.clear()
        activeModels.clear()
    }
}
