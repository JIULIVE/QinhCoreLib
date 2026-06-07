package com.qinhuai.corelib.action.skill

import com.qinhuai.corelib.action.ActionDispatchResult
import com.qinhuai.corelib.action.QinhActionContext
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

object QISkillBridge {

    const val HANDLER_ID: String = "qinhskills:cast"

    private val currentDispatch = ThreadLocal<QISkillUseEvent?>()

    fun peekCurrentDispatch(): QISkillUseEvent? = currentDispatch.get()

    fun dispatchViaEvent(context: QinhActionContext): ActionDispatchResult {
        traceQi("payload built handler=$HANDLER_ID payload=${context.payload} trigger=${context.trigger}")

        val event = QISkillUseEvent(
            player = context.player,
            payload = context.payload,
            trigger = context.trigger,
            item = context.item,
            itemId = context.itemId,
            triggerType = context.triggerType ?: TriggerType.fromLegacy(context.trigger),
            rawContext = context.rawContext,
        )
        currentDispatch.set(event)
        traceQi("QISkillUseEvent firing")
        Bukkit.getPluginManager().callEvent(event)
        traceQi(
            "event completed handled=${event.skillHandled} castAttempted=${event.castAttempted} " +
                "mythicInvoked=${event.mythicInvoked} fallback=${event.fallbackInvoked}",
        )
        if (event.isCancelled) {
            clearDispatch()
            return ActionDispatchResult.NOT_HANDLED
        }
        return if (event.skillHandled) {
            clearDispatch()
            ActionDispatchResult.HANDLED
        } else {
            ActionDispatchResult.NOT_HANDLED
        }
    }

    fun clearDispatch() {
        currentDispatch.remove()
    }

    private fun traceQi(message: String) {
        val plugin = resolveTracePlugin() ?: return
        if (!plugin.config.getBoolean("debug", false)) return
        plugin.logger.info("[QinhSkills][QI] $message")
    }

    internal fun traceFallbackBlocked(reason: String) {
        val plugin = resolveTracePlugin() ?: return
        plugin.logger.log(Level.WARNING, "[QinhSkills][FALLBACK] $reason")
    }

    private fun resolveTracePlugin(): JavaPlugin? {
        val names = listOf("QinhSkills", "QinhItems")
        for (name in names) {
            val p = Bukkit.getPluginManager().getPlugin(name)
            if (p is JavaPlugin && p.isEnabled) return p
        }
        return null
    }
}
