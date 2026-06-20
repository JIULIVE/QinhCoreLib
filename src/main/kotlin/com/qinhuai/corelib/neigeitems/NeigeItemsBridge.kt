package com.qinhuai.corelib.neigeitems

import com.qinhuai.corelib.debug.BridgeDiagnostics
import com.qinhuai.corelib.debug.BridgeStatus
import com.qinhuai.corelib.debug.BridgeStatusRegistry
import com.qinhuai.corelib.debug.DiagnosticResult
import com.qinhuai.corelib.lang.Lang
import org.bukkit.inventory.ItemStack

object NeigeItemsBridge {
    private var available: Boolean? = null
    private var itemManagerClass: Class<*>? = null
    private var itemManagerInstance: Any? = null
    private var getItemStackMethod: java.lang.reflect.Method? = null
    private var getItemIdMethod: java.lang.reflect.Method? = null
    
    fun isAvailable(): Boolean {
        if (available != null) return available!!
        return try {
            itemManagerClass = Class.forName("pers.neige.neigeitems.manager.ItemManager")
            val instanceField = itemManagerClass!!.getField("INSTANCE")
            itemManagerInstance = instanceField.get(null)

            getItemStackMethod = itemManagerClass!!.getMethod("getItemStack", String::class.java)
            getItemIdMethod = itemManagerClass!!.getMethod("getItemId", ItemStack::class.java)

            available = true
            BridgeStatusRegistry.register(status())
            true
        } catch (e: Exception) {
            available = false
            BridgeStatusRegistry.register(status())
            false
        }
    }
    
    fun getItemStack(identifier: String): ItemStack? {
        if (!isAvailable()) return null
        return try {
            getItemStackMethod!!.invoke(itemManagerInstance, identifier) as? ItemStack
        } catch (e: Exception) {
            null
        }
    }
    
    fun getItemId(itemStack: ItemStack): String? {
        if (!isAvailable()) return null
        return try {
            getItemIdMethod!!.invoke(itemManagerInstance, itemStack) as? String
        } catch (e: Exception) {
            null
        }
    }

    fun status(): BridgeStatus = if (available == true) {
        BridgeDiagnostics.available("NeigeItems", source = "NeigeItems", hint = Lang.get("neige-items-bridge.reflect-bound"))
    } else {
        BridgeDiagnostics.unavailable("NeigeItems", source = "NeigeItems", hint = Lang.get("neige-items-bridge.not-detected"))
    }

    fun diagnose(): DiagnosticResult<BridgeStatus> = BridgeDiagnostics.diagnose("NeigeItems", isAvailable(), source = "NeigeItems", hint = "itemManager=${itemManagerInstance != null}")
}

object NeigeItemsManager {
    fun isAvailable(): Boolean = NeigeItemsBridge.isAvailable()
    
    fun getItemStack(identifier: String): ItemStack? = NeigeItemsBridge.getItemStack(identifier)
    
    fun getItemId(itemStack: ItemStack): String? = NeigeItemsBridge.getItemId(itemStack)
    
    fun clear() {
    }
}
