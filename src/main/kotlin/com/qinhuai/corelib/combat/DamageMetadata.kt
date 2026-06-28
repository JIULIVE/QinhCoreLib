package com.qinhuai.corelib.combat

import com.qinhuai.corelib.attribute.DamageType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageEvent

/**
 * 结构化伤害元数据 —— QinhCoreLib 战斗管线算完一次伤害后的完整快照 + 可继续重算的计算底座。
 * 经 [QinhDamageEvent] 总线对外暴露：飘字 / QI on-hit / QS 内联 buff 等消费者据此读取或改写，
 * 不必再各自 hook 原版事件重算（解耦"硬调用"）。
 *
 * 计算底座（对齐 MythicLib DamageMetadata 活对象）：
 *  - [packets] 是可变分量列表，[finalDamage] 始终等于各分量之和（由本类的修改器维护）。
 *  - 监听器用 [addPacket] 追加新分量（如附魔火伤）、[scaleAll]/[scaleType]/[scaleElement] 按分量缩放，
 *    这些方法会同步更新 finalDamage；改完即生效，无需手动求和。
 *  - 若直接写 [finalDamage]（标量裁决），分量不再与之精确对应，飘字会按比例回退缩放（见 DamageIndicators）。
 *
 * 可变字段（finalDamage / crit / tags / packets）允许监听器在事件中调整；其余为只读快照。
 */
class DamageMetadata(
    val attacker: LivingEntity?,
    val victim: LivingEntity,
    val cause: EntityDamageEvent.DamageCause,
    val damageType: DamageType,
    /** 进入 CoreLib 管线前的原始伤害（event.damage 入口值）。 */
    val baseDamage: Double,
    /** 管线计算后的最终伤害；监听器可改写，事件结束后写回原版事件。 */
    var finalDamage: Double,
    val projectile: Boolean,
    /** 本次是否暴击（由攻防管线判定）；监听器可改写（影响飘字样式）。 */
    var crit: Boolean,
    val pvp: Boolean,
    /** 真实/元素穿透部分（不经常规减伤的分量）。 */
    val penetration: Double,
    /** 逐步计算明细（调试用，与 /attr debug 同源）。 */
    val trace: List<String>,
    /** 自由标签，供消费者打标/识别（如技能id、来源插件）。 */
    val tags: MutableSet<String> = mutableSetOf(),
    /** 伤害分量（物理/各元素/真实）。可变：追加或缩放后 finalDamage 同步更新。 */
    val packets: MutableList<DamagePacket> = mutableListOf(),
) {

    /** 追加一个伤害分量，并同步累加进 finalDamage。返回新建的分量便于后续引用。 */
    @JvmOverloads
    fun addPacket(
        value: Double,
        damageType: DamageType,
        element: String? = null,
        crit: Boolean = false,
        source: String? = null,
    ): DamagePacket {
        val packet = DamagePacket(value, damageType, element, crit, source)
        packets.add(packet)
        if (value.isFinite()) finalDamage = (finalDamage + value).coerceAtLeast(0.0)
        return packet
    }

    /** 把 finalDamage 重置为当前各分量之和。手动改过 packets 内部值后调用。 */
    fun recomputeFromPackets() {
        val sum = packets.sumOf { it.value }
        if (sum.isFinite()) finalDamage = sum.coerceAtLeast(0.0)
    }

    /** 按系数缩放全部分量与 finalDamage（factor 必须有限且非负，否则忽略）。 */
    fun scaleAll(factor: Double) {
        if (!factor.isFinite() || factor < 0.0) return
        for (p in packets) p.value *= factor
        finalDamage = (finalDamage * factor).coerceAtLeast(0.0)
    }

    /** 只缩放指定伤害类型的分量，finalDamage 同步增减。 */
    fun scaleType(damageType: DamageType, factor: Double) =
        scaleMatching(factor) { it.damageType == damageType }

    /** 只缩放指定元素的分量，finalDamage 同步增减。 */
    fun scaleElement(elementId: String, factor: Double) =
        scaleMatching(factor) { it.element.equals(elementId, ignoreCase = true) }

    private inline fun scaleMatching(factor: Double, predicate: (DamagePacket) -> Boolean) {
        if (!factor.isFinite() || factor < 0.0) return
        var delta = 0.0
        for (p in packets) {
            if (!predicate(p)) continue
            val before = p.value
            p.value *= factor
            delta += p.value - before
        }
        if (delta.isFinite()) finalDamage = (finalDamage + delta).coerceAtLeast(0.0)
    }

    /** 指定类型的全部分量之和（如「本次魔法伤害总量」）。 */
    fun damageOf(damageType: DamageType): Double =
        packets.filter { it.damageType == damageType }.sumOf { it.value }

    /** 指定元素的全部分量之和。 */
    fun damageOfElement(elementId: String): Double =
        packets.filter { it.element.equals(elementId, ignoreCase = true) }.sumOf { it.value }

    /** 带元素标记的分量（飘字分色用）。 */
    fun elementPackets(): List<DamagePacket> = packets.filter { it.element != null }
}
