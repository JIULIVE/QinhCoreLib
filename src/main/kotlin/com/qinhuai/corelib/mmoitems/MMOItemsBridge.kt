package com.qinhuai.corelib.mmoitems

import com.qinhuai.corelib.debug.BridgeDiagnostics
import com.qinhuai.corelib.debug.BridgeStatus
import com.qinhuai.corelib.debug.BridgeStatusRegistry
import com.qinhuai.corelib.debug.DiagnosticResult
import org.bukkit.inventory.ItemStack

object MMOItemsBridge {
    private var available: Boolean? = null
    private var mmoItemsClass: Class<*>? = null
    private var mmoItemsInstance: Any? = null
    private var pluginField: java.lang.reflect.Field? = null
    private var getTypesMethod: java.lang.reflect.Method? = null
    private var typesGetMethod: java.lang.reflect.Method? = null
    private var getItemMethod: java.lang.reflect.Method? = null
    private var staticGetTypeMethod: java.lang.reflect.Method? = null
    private var staticGetIDMethod: java.lang.reflect.Method? = null
    
    fun isAvailable(): Boolean {
        if (available != null) return available!!
        return try {
            mmoItemsClass = Class.forName("net.Indyuce.mmoitems.MMOItems")
            pluginField = mmoItemsClass!!.getField("plugin")
            mmoItemsInstance = pluginField!!.get(null)

            val typesClass = Class.forName("net.Indyuce.mmoitems.api.Type")

            getTypesMethod = mmoItemsClass!!.getMethod("getTypes")
            val types = getTypesMethod!!.invoke(mmoItemsInstance)
            val typesListClass = types!!.javaClass
            typesGetMethod = typesListClass.getMethod("get", String::class.java)

            getItemMethod = mmoItemsClass!!.getMethod("getItem", typesClass, String::class.java)

            staticGetTypeMethod = mmoItemsClass!!.getMethod("getType", ItemStack::class.java)
            staticGetIDMethod = mmoItemsClass!!.getMethod("getID", ItemStack::class.java)

            available = true
            BridgeStatusRegistry.register(status())
            true
        } catch (e: Exception) {
            available = false
            BridgeStatusRegistry.register(status())
            false
        }
    }
    
    fun getItem(typeId: String, itemId: String): ItemStack? {
        if (!isAvailable()) return null
        return try {
            val types = getTypesMethod!!.invoke(mmoItemsInstance)
            val type = typesGetMethod!!.invoke(types, typeId)
            getItemMethod!!.invoke(mmoItemsInstance, type, itemId) as? ItemStack
        } catch (e: Exception) {
            null
        }
    }
    
    fun getType(itemStack: ItemStack): String? {
        if (!isAvailable()) return null
        return try {
            val type = staticGetTypeMethod!!.invoke(null, itemStack)
            if (type != null) {
                val getIdMethod = type.javaClass.getMethod("getId")
                getIdMethod.invoke(type) as? String
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun getID(itemStack: ItemStack): String? {
        if (!isAvailable()) return null
        return try {
            staticGetIDMethod!!.invoke(null, itemStack) as? String
        } catch (e: Exception) {
            null
        }
    }

    fun status(): BridgeStatus = if (available == true) {
        BridgeDiagnostics.available("MMOItems", source = "MMOItems", hint = "反射绑定成功")
    } else {
        BridgeDiagnostics.unavailable("MMOItems", source = "MMOItems", hint = "未检测到 MMOItems 或反射失败")
    }

    fun diagnose(): DiagnosticResult<BridgeStatus> = BridgeDiagnostics.diagnose("MMOItems", isAvailable(), source = "MMOItems", hint = "type API bound=${getTypesMethod != null}")
}

object MMOItemsManager {
    fun isAvailable(): Boolean = MMOItemsBridge.isAvailable()
    
    fun getItem(typeId: String, itemId: String): ItemStack? = MMOItemsBridge.getItem(typeId, itemId)
    
    fun getType(itemStack: ItemStack): String? = MMOItemsBridge.getType(itemStack)
    
    fun getID(itemStack: ItemStack): String? = MMOItemsBridge.getID(itemStack)
    
    fun clear() {
    }
}
