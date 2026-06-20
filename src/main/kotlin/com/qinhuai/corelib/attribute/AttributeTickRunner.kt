package com.qinhuai.corelib.attribute

import com.qinhuai.corelib.QinhCoreLib
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask

object AttributeTickRunner {

    private var task: BukkitTask? = null

    fun start(intervalTicks: Long = 20L) {
        stop()
        task = Bukkit.getScheduler().runTaskTimer(QinhCoreLib.instance, Runnable { tick() }, intervalTicks, intervalTicks)
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    private fun tick() {
        if (!AttributeService.isNativeActive()) return
        for (player in Bukkit.getOnlinePlayers()) {
            val totals = AttributeStatStore.totals(player.uniqueId)
            if (totals.isEmpty()) continue
            for ((attrKey, total) in totals) {
                if (total == 0.0) continue
                val def = AttributeRegistry.resolve(attrKey) ?: continue
                val ref = def.hook(AttributeHooks.ON_TICK) ?: continue
                val vars = HashMap<String, Any>()
                for ((k, v) in totals) vars[k] = v
                vars["attribute"] = attrKey
                vars["value"] = total
                AttributeScriptRunner.runEffectHook(ref, player, vars)
            }
        }
    }
}
