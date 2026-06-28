package com.qinhuai.corelib.attribute

object AttributeHooks {
    const val ON_DAMAGE_DEALT = "on_damage_dealt"
    /** 造成法术/技能伤害时（与 on_damage_dealt 对称，但走法术管线：纯技能伤害的 MAGIC 事件 + 近战附带元素伤害）。 */
    const val ON_MAGIC_DAMAGE_DEALT = "on_magic_damage_dealt"
    const val ON_DAMAGE_TAKEN = "on_damage_taken"
    const val ON_EQUIP = "on_equip"
    const val ON_UNEQUIP = "on_unequip"
    const val ON_TICK = "on_tick"
    const val ON_KILL = "on_kill"
}
