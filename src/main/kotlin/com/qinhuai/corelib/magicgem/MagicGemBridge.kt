package com.qinhuai.corelib.magicgem

import com.qinhuai.corelib.debug.BridgeDiagnostics
import com.qinhuai.corelib.debug.BridgeStatus
import com.qinhuai.corelib.debug.BridgeStatusRegistry
import com.qinhuai.corelib.debug.DiagnosticResult
import org.bukkit.inventory.ItemStack

/** 反射桥接 MagicGem（pku.yim.magicgem）— 无编译期依赖 */
object MagicGemBridge {

    private var available: Boolean? = null
    private var gemManagerClass: Class<*>? = null
    private var getGemByNameMethod: java.lang.reflect.Method? = null
    fun isAvailable(): Boolean {
        if (available != null) return available!!
        return try {
            gemManagerClass = Class.forName("pku.yim.magicgem.gem.GemManager")
            getGemByNameMethod = gemManagerClass!!.getMethod("getGemByName", String::class.java)
            available = true
            BridgeStatusRegistry.register(status())
            true
        } catch (_: Throwable) {
            available = false
            BridgeStatusRegistry.register(status())
            false
        }
    }

    fun getGemItem(id: String): ItemStack? {
        if (!isAvailable()) return null
        return try {
            val gem = getGemByNameMethod!!.invoke(null, id) ?: return null
            realGemFrom(gem)
        } catch (_: Throwable) {
            null
        }
    }

    private fun realGemFrom(gem: Any): ItemStack? {
        try {
            return (gem.javaClass.getMethod("getRealGem").invoke(gem) as? ItemStack)?.clone()
        } catch (_: Throwable) {
        }
        try {
            return (gem.javaClass.getField("realGem").get(gem) as? ItemStack)?.clone()
        } catch (_: Throwable) {
            return null
        }
    }

    fun hasGem(id: String): Boolean = getGemItem(id) != null

    fun status(): BridgeStatus = if (available == true) {
        BridgeDiagnostics.available("MagicGem", source = "MagicGem", hint = "反射绑定成功")
    } else {
        BridgeDiagnostics.unavailable("MagicGem", source = "MagicGem", hint = "未检测到 MagicGem 或反射失败")
    }

    fun diagnose(): DiagnosticResult<BridgeStatus> = BridgeDiagnostics.diagnose("MagicGem", isAvailable(), source = "MagicGem", hint = "gemManager=${gemManagerClass != null}")

    fun clear() {
        available = null
        gemManagerClass = null
        getGemByNameMethod = null
    }
}
