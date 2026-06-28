package com.qinhuai.corelib.combat

import com.qinhuai.corelib.attribute.DamageType
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * QinhCoreLib 战斗事件总线 —— 在 CoreLib 攻防管线算出最终伤害后、写回原版事件前触发。
 *
 * 消费者用法：
 *  - 读：`event.metadata`（攻击者/受害者/类型/暴击/明细…）做飘字、on-hit 触发、统计。
 *  - 改：`event.damage = x` 调整最终伤害（最后裁决权）。
 *  - 取消：`event.isCancelled = true` → 本次伤害归零（原版事件被取消）。
 *
 * 仅对 CoreLib 接管的战斗伤害（近战/横扫/弹射物等物理战斗）触发；
 * 环境伤害（坠落/火/毒等）不进此总线。
 */
class QinhDamageEvent(val metadata: DamageMetadata) : Event(), Cancellable {

    private var cancelled = false

    var damage: Double
        get() = metadata.finalDamage
        set(value) {
            metadata.finalDamage = value
        }

    /** 追加伤害分量并同步累加进最终伤害（如附魔火伤、技能附带伤害）。见 [DamageMetadata.addPacket]。 */
    @JvmOverloads
    fun addPacket(value: Double, damageType: DamageType, element: String? = null, crit: Boolean = false, source: String? = null) =
        metadata.addPacket(value, damageType, element, crit, source)

    /** 按系数缩放全部分量与最终伤害。见 [DamageMetadata.scaleAll]。 */
    fun scaleAll(factor: Double) = metadata.scaleAll(factor)

    /** 只缩放指定伤害类型的分量。见 [DamageMetadata.scaleType]。 */
    fun scaleType(damageType: DamageType, factor: Double) = metadata.scaleType(damageType, factor)

    /** 只缩放指定元素的分量。见 [DamageMetadata.scaleElement]。 */
    fun scaleElement(elementId: String, factor: Double) = metadata.scaleElement(elementId, factor)

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        @JvmStatic
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}
