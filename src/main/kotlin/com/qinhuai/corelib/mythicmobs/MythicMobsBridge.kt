package com.qinhuai.corelib.mythicmobs

import com.qinhuai.corelib.debug.BridgeDiagnostics
import com.qinhuai.corelib.debug.BridgeStatus
import com.qinhuai.corelib.debug.BridgeStatusRegistry
import com.qinhuai.corelib.debug.DiagnosticResult
import com.qinhuai.corelib.lang.Lang
import com.qinhuai.corelib.reflection.ReflectionBridge
import org.bukkit.inventory.ItemStack

object MythicMobsBridge {

    private var available: Boolean? = null
    private var bindMode: String = ""
    private var getItemStackMethod: java.lang.reflect.Method? = null
    private var itemManagerInstance: Any? = null

    fun isAvailable(): Boolean {
        if (available != null) return available!!
        return try {
            if (bindModern()) {
                bindMode = "modern"
                available = true
                BridgeStatusRegistry.register(status())
                return true
            }
            if (bindLegacy()) {
                bindMode = "legacy"
                available = true
                BridgeStatusRegistry.register(status())
                return true
            }
            available = false
            BridgeStatusRegistry.register(status())
            false
        } catch (_: Throwable) {
            available = false
            BridgeStatusRegistry.register(status())
            false
        }
    }

    fun getItemStack(itemId: String, amount: Int): ItemStack? {
        if (!isAvailable()) return null
        val method = getItemStackMethod ?: return null
        val manager = itemManagerInstance ?: return null
        return try {
            val stack = when (method.parameterCount) {
                1 -> method.invoke(manager, itemId)
                2 -> method.invoke(manager, itemId, amount)
                else -> method.invoke(manager, itemId)
            } as? ItemStack
            stack?.clone()?.apply { this.amount = amount.coerceAtLeast(1) }
        } catch (_: Throwable) {
            null
        }
    }

    fun status(): BridgeStatus = if (available == true) {
        BridgeDiagnostics.available("MythicMobs", source = "MM", hint = Lang.get("mythic-mobs-bridge.hint-bind-mode", "mode" to bindMode))
    } else {
        BridgeDiagnostics.unavailable("MythicMobs", source = "MM", hint = Lang.get("mythic-mobs-bridge.hint-unbound"), recoverable = true)
    }

    fun diagnose(): DiagnosticResult<BridgeStatus> = BridgeDiagnostics.diagnose("MythicMobs", isAvailable(), source = "MM", hint = "mode=$bindMode")

    private fun bindModern(): Boolean {
        if (!ReflectionBridge.isClassAvailable("io.lumine.mythic.bukkit.MythicBukkit")) {
            return false
        }
        val mythic = Class.forName("io.lumine.mythic.bukkit.MythicBukkit")
            .getMethod("inst")
            .invoke(null) ?: return false
        val itemManager = mythic.javaClass.getMethod("getItemManager").invoke(mythic) ?: return false
        itemManagerInstance = itemManager
        getItemStackMethod = itemManager.javaClass.methods.firstOrNull {
            it.name == "getItemStack" &&
                it.parameterCount == 1 &&
                it.parameterTypes[0] == String::class.java
        } ?: itemManager.javaClass.methods.firstOrNull {
            it.name == "getItemStack" && it.parameterCount >= 1
        }
        return getItemStackMethod != null
    }

    private fun bindLegacy(): Boolean {
        if (!ReflectionBridge.isClassAvailable("io.lumine.xikage.mythicmobs.MythicMobs")) {
            return false
        }
        val mythic = Class.forName("io.lumine.xikage.mythicmobs.MythicMobs")
            .getMethod("inst")
            .invoke(null) ?: return false
        val itemManager = mythic.javaClass.getMethod("getItemManager").invoke(mythic) ?: return false
        itemManagerInstance = itemManager
        getItemStackMethod = itemManager.javaClass.methods.firstOrNull {
            it.name == "getItemStack" && it.parameterCount == 1
        }
        return getItemStackMethod != null
    }

    fun clear() {
        available = null
        getItemStackMethod = null
        itemManagerInstance = null
    }
}

object MythicMobsManager {
    fun isAvailable(): Boolean = MythicMobsBridge.isAvailable()

    fun getItemStack(itemId: String, amount: Int = 1): ItemStack? =
        MythicMobsBridge.getItemStack(itemId, amount)

    fun clear() = MythicMobsBridge.clear()
}
