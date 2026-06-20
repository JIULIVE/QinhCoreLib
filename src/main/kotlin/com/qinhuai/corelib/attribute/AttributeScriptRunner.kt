package com.qinhuai.corelib.attribute

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.script.QinhScriptBridge
import com.qinhuai.corelib.script.ScriptContext
import org.bukkit.entity.LivingEntity

object AttributeScriptRunner {

    fun runDamageHook(ref: String, entity: LivingEntity, variables: Map<String, Any>): Double? {
        val context = ScriptContext(
            plugin = QinhCoreLib.instance,
            player = entity as? org.bukkit.entity.Player,
            variables = variables.toMutableMap(),
            silent = true,
        )
        val result = runCatching { QinhScriptBridge.execute(ref, context) }.getOrNull() ?: return null
        val returned = result.value
        if (returned is Number) return returned.toDouble()
        val back = context.variables["damage"]
        if (back is Number) return back.toDouble()
        return null
    }

    fun runEffectHook(ref: String, entity: LivingEntity, variables: Map<String, Any>) {
        val context = ScriptContext(
            plugin = QinhCoreLib.instance,
            player = entity as? org.bukkit.entity.Player,
            variables = variables.toMutableMap(),
            silent = true,
        )
        runCatching { QinhScriptBridge.execute(ref, context) }
    }
}
