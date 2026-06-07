package com.qinhuai.corelib

import com.qinhuai.corelib.bootstrap.AbstractModule
import com.qinhuai.corelib.bootstrap.ModuleManager
import com.qinhuai.corelib.bootstrap.StartupReporter
import com.qinhuai.corelib.item.ItemManagerBootstrap
import com.qinhuai.corelib.item.ItemSourceBootstrap

object CoreModules {
    fun registerAll(moduleManager: ModuleManager) {
        moduleManager.register(ConfigModule)
        moduleManager.register(DatabaseModule)
        moduleManager.register(ReflectionModule)
        moduleManager.register(ModelEngineModule)
        moduleManager.register(CustomCropsModule)
        moduleManager.register(CraftEngineModule)
        moduleManager.register(MythicMobsModule)
        moduleManager.register(NeigeItemsModule)
        moduleManager.register(MMOItemsModule)
        moduleManager.register(GuiModule)
        moduleManager.register(ActionModule)
        moduleManager.register(PdcModule)
        moduleManager.register(ItemModule)
        moduleManager.register(HologramModule)
        moduleManager.register(SchedulerModule)
        moduleManager.register(ConditionModule)
        moduleManager.register(ExpressionModule)
        moduleManager.register(EconomyModule)
        moduleManager.register(ScriptModule)
        moduleManager.register(CustomBlockModule)
        moduleManager.register(AssemblyModule)
    }
}

object ConfigModule : AbstractModule("Config")

object GuiModule : AbstractModule("Gui") {
    override val priority: Int = 20
    override fun load() {
        if (com.qinhuai.corelib.placeholder.PapiBridge.isEnabled()) {
            StartupReporter.hookedBridge("PlaceholderAPI", "占位符")
        }
    }
}

object ActionModule : AbstractModule("Action") {
    override val priority: Int = 30
}

object PdcModule : AbstractModule("Pdc") {
    override val priority: Int = 40
}

object ItemModule : AbstractModule("Item") {
    override val priority: Int = 50
    override fun load() {
        ItemSourceBootstrap.registerAll()
        ItemManagerBootstrap.onItemSourcesReady()
    }

    override fun unload() {
        ItemManagerBootstrap.unloadAll()
        ItemSourceBootstrap.unregisterAll()
        com.qinhuai.corelib.magicgem.MagicGemBridge.clear()
        com.qinhuai.corelib.customfishing.CustomFishingManager.clear()
        com.qinhuai.corelib.mythicmobs.MythicMobsManager.clear()
    }
}

object ConditionModule : AbstractModule("Condition") {
    override val priority: Int = 60
}

object ExpressionModule : AbstractModule("Expression") {
    override val priority: Int = 70
}

object ScriptModule : AbstractModule("Script") {
    override val priority: Int = 75
    override fun load() {
        com.qinhuai.corelib.script.QinhScriptBridge.init(QinhCoreLib.instance)
        if (com.qinhuai.corelib.script.QinhScriptBridge.isAvailable()) {
            StartupReporter.hookedBridge("GraalJS", "脚本")
        }
    }

    override fun unload() {
        com.qinhuai.corelib.script.QinhScriptBridge.shutdown()
    }
}

object EconomyModule : AbstractModule("Economy") {
    override val priority: Int = 80
    override fun load() {
        com.qinhuai.corelib.economy.EconomyBridge.init(QinhCoreLib.instance)
    }

    override fun unload() {
        com.qinhuai.corelib.economy.EconomyBridge.clear()
    }
}

object CustomBlockModule : AbstractModule("CustomBlock") {
    override val priority: Int = 90
    override fun load() {
        val provider = com.qinhuai.corelib.customblock.CraftEngineBlockProvider
        com.qinhuai.corelib.customblock.CustomBlockBridge.registerProvider(provider)
        if (provider.isAvailable()) {
            StartupReporter.hookedBridge("CraftEngine", "自定义方块")
        }
    }
}

object DatabaseModule : AbstractModule("Database") {
    override val priority: Int = 5
}

object ReflectionModule : AbstractModule("Reflection") {
    override val priority: Int = 8
}

object ModelEngineModule : AbstractModule("ModelEngine") {
    override val priority: Int = 9
    override fun load() {
        if (com.qinhuai.corelib.modelengine.ModelEngineBridge.isAvailable()) {
            StartupReporter.hookedBridge("ModelEngine", "模型")
        }
    }

    override fun unload() {
        com.qinhuai.corelib.modelengine.ModelEngineManager.clear()
    }
}

object CustomCropsModule : AbstractModule("CustomCrops") {
    override val priority: Int = 10
    override fun load() {
        if (com.qinhuai.corelib.customcrops.CustomCropsBridge.isAvailable() &&
            com.qinhuai.corelib.customcrops.CustomCropsManager.init()
        ) {
            StartupReporter.hookedBridge("CustomCrops", "作物")
        }
    }

    override fun unload() {
        com.qinhuai.corelib.customcrops.CustomCropsManager.clear()
    }
}

object CraftEngineModule : AbstractModule("CraftEngine") {
    override val priority: Int = 11
    override fun unload() {
        com.qinhuai.corelib.craftengine.CraftEngineManager.clear()
    }
}

object MythicMobsModule : AbstractModule("MythicMobs") {
    override val priority: Int = 12
    override fun unload() {
        com.qinhuai.corelib.mythicmobs.MythicMobsManager.clear()
    }
}

object NeigeItemsModule : AbstractModule("NeigeItems") {
    override val priority: Int = 13
    override fun unload() {
        com.qinhuai.corelib.neigeitems.NeigeItemsManager.clear()
    }
}

object MMOItemsModule : AbstractModule("MMOItems") {
    override val priority: Int = 14
    override fun unload() {
        com.qinhuai.corelib.mmoitems.MMOItemsManager.clear()
    }
}

object HologramModule : AbstractModule("Hologram") {
    override val priority: Int = 55
    override fun unload() {
        com.qinhuai.corelib.hologram.HologramManager.removeAll()
    }
}

object SchedulerModule : AbstractModule("Scheduler") {
    override val priority: Int = 58
}

object AssemblyModule : AbstractModule("Assembly") {
    override val priority: Int = 100
}
