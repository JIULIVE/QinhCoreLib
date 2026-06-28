package com.qinhuai.corelib.combat

import com.qinhuai.corelib.attribute.DamageType

/**
 * 单个伤害分量 —— [DamageMetadata] 的计算单元（而非只读快照）。
 *
 * value / crit 可变：监听器可在 [QinhDamageEvent] 中按分量缩放或追加新分量再重算
 * finalDamage（对齐 MythicLib DamageMetadata 的活对象模型）。
 * source 标记分量来源（如技能id、来源插件、"physical"/"true"/元素id），供消费者识别与飘字分色。
 */
data class DamagePacket(
    var value: Double,
    val damageType: DamageType,
    val element: String? = null,
    var crit: Boolean = false,
    val source: String? = null,
) {
    val styleKey: String
        get() = element ?: when (damageType) {
            DamageType.PHYSICAL -> "physical"
            DamageType.MAGIC, DamageType.SKILL -> "magic"
            DamageType.TRUE -> "true"
        }
}
