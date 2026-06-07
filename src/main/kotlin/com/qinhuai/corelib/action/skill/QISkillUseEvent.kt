package com.qinhuai.corelib.action.skill

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent
import org.bukkit.inventory.ItemStack

/**
 * QinhItems → QinhSkills 技能触发事件（主链路入口）。
 * 类型必须在 QinhCoreLib，供 QI/QS 共用同一 EventBus 契约。
 */
class QISkillUseEvent(
    player: Player,
    val payload: String,
    trigger: String,
    val item: ItemStack? = null,
    val itemId: String? = null,
    val triggerType: TriggerType = TriggerType.fromLegacy(trigger),
    val rawContext: RawSkillContext? = null,
) : PlayerEvent(player), Cancellable {

    /** 向后兼容字符串 trigger（QI action 表键 / 旧 API） */
    val trigger: String = trigger

    var skillHandled: Boolean = false
    var castResult: String? = null

    var castAttempted: Boolean = false
    var fallbackInvoked: Boolean = false
    var mythicInvoked: Boolean = false
    var primaryPipeline: Boolean = true

    private var cancelled: Boolean = false

    override fun getHandlers(): HandlerList = HANDLER_LIST

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    companion object {
        @JvmStatic
        val HANDLER_LIST = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLER_LIST
    }
}
