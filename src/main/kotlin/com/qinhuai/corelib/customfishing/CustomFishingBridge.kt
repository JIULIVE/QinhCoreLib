package com.qinhuai.corelib.customfishing

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.reflection.ReflectionBridge
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack

object CustomFishingBridge {

    private var available: Boolean? = null
    private var itemManager: Any? = null
    private var buildMethod: java.lang.reflect.Method? = null

    fun isAvailable(): Boolean {
        if (available != null) return available!!
        if (Bukkit.getPluginManager().getPlugin("CustomFishing") == null) {
            available = false
            return false
        }
        return try {
            bindItemManager()
            available = buildMethod != null && itemManager != null
            available!!
        } catch (_: Throwable) {
            available = false
            false
        }
    }

    fun buildItem(itemId: String, amount: Int): ItemStack? {
        if (!isAvailable()) return null
        val manager = itemManager ?: return null
        val method = buildMethod ?: return null
        return try {
            val stack = when (method.parameterCount) {
                1 -> method.invoke(manager, itemId)
                2 -> {
                    val ctx = createBuildContext() ?: return null
                    method.invoke(manager, ctx, itemId)
                }
                3 -> {
                    val ctx = createBuildContext() ?: return null
                    method.invoke(manager, ctx, itemId, amount)
                }
                else -> method.invoke(manager, itemId)
            } as? ItemStack
            stack?.clone()?.apply { this.amount = amount.coerceAtLeast(1) }
        } catch (_: Throwable) {
            null
        }
    }

    private fun bindItemManager(): Boolean {
        val plugin = resolvePluginInstance() ?: return false
        val manager = plugin.javaClass.methods
            .firstOrNull { it.name == "getItemManager" && it.parameterCount == 0 }
            ?.invoke(plugin) ?: return false
        itemManager = manager
        val managerClass = manager.javaClass
        buildMethod = listOf("buildAnyPluginItemByID", "build", "buildInternal").firstNotNullOfOrNull { name ->
            managerClass.methods.firstOrNull { method ->
                method.name == name &&
                    method.parameterCount in 1..3 &&
                    method.parameterTypes.last() == String::class.java
            }
        }
        return buildMethod != null
    }

    private fun resolvePluginInstance(): Any? {
        val pluginClasses = listOf(
            "net.momirealms.customfishing.api.CustomFishingPlugin",
            "net.momirealms.customfishing.bukkit.BukkitCustomFishingPlugin",
            "net.momirealms.customfishing.api.BukkitCustomFishingPlugin",
        )
        for (className in pluginClasses) {
            if (!ReflectionBridge.isClassAvailable(className)) continue
            try {
                val clazz = Class.forName(className)
                val instance = clazz.getMethod("getInstance").invoke(null)
                if (instance != null) return instance
            } catch (_: Throwable) {
            }
        }
        return QinhCoreLib.instance.server.pluginManager.getPlugin("CustomFishing")
    }

    private fun createBuildContext(): Any? {
        val contextClasses = listOf(
            "net.momirealms.customfishing.api.context.Context",
            "net.momirealms.customfishing.api.mechanic.context.Context",
        )
        for (className in contextClasses) {
            if (!ReflectionBridge.isClassAvailable(className)) continue
            try {
                val clazz = Class.forName(className)
                clazz.methods.firstOrNull { it.name == "empty" && it.parameterCount == 0 }?.let {
                    return it.invoke(null)
                }
                val builderMethod = clazz.methods.firstOrNull { it.name == "builder" && it.parameterCount == 0 }
                if (builderMethod != null) {
                    val b = builderMethod.invoke(null)
                    if (b != null) {
                        val build = b.javaClass.methods.firstOrNull { it.name == "build" && it.parameterCount == 0 }
                        if (build != null) {
                            return build.invoke(b)
                        }
                    }
                }
                return clazz.getConstructor().newInstance()
            } catch (_: Throwable) {
            }
        }
        return null
    }

    fun clear() {
        available = null
        itemManager = null
        buildMethod = null
    }
}

object CustomFishingManager {
    fun isAvailable(): Boolean = CustomFishingBridge.isAvailable()

    fun buildItem(itemId: String, amount: Int = 1): ItemStack? =
        CustomFishingBridge.buildItem(itemId, amount)

    fun clear() = CustomFishingBridge.clear()
}
