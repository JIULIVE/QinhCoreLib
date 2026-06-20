package com.qinhuai.corelib.attribute

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.lang.Lang
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.EventExecutor
import java.lang.reflect.Method

internal object MythicMobAttributeHook : Listener {

    private var registered = false

    fun tryRegister() {
        if (registered) return
        val eventClass = runCatching {
            Class.forName("io.lumine.mythic.bukkit.events.MythicMobSpawnEvent")
        }.getOrNull() ?: return
        val getEntity = runCatching { eventClass.getMethod("getEntity") }.getOrNull() ?: return
        val getMobType = runCatching { eventClass.getMethod("getMobType") }.getOrNull() ?: return

        val executor = EventExecutor { _, event -> handle(event, getEntity, getMobType) }

        @Suppress("UNCHECKED_CAST")
        Bukkit.getPluginManager().registerEvent(
            eventClass as Class<out Event>,
            this,
            EventPriority.MONITOR,
            executor,
            QinhCoreLib.instance,
            true,
        )
        registered = true
        QinhCoreLib.instance.logger.info(Lang.get("mythic-mob-attribute-hook.mapping-mounted"))
    }

    private fun handle(event: Event, getEntity: Method, getMobType: Method) {
        if (!AttributeService.isNativeActive()) return
        runCatching {
            val entity = getEntity.invoke(event) as? LivingEntity ?: return
            val name = internalName(event, getMobType) ?: return
            val attrs = MobAttributeMapping.get(name) ?: return
            QinhCoreLib.instance.server.scheduler.runTaskLater(QinhCoreLib.instance, Runnable {
                if (!entity.isValid) return@Runnable
                MobAttributeService.applyMapped(entity, attrs)
                MobAttributeService.fullHealToMax(entity)
            }, 1L)
        }
    }

    private fun internalName(event: Event, getMobType: Method): String? {
        val mobType = runCatching { getMobType.invoke(event) }.getOrNull() ?: return null
        return runCatching { mobType.javaClass.getMethod("getInternalName").invoke(mobType) as? String }.getOrNull()
    }
}
