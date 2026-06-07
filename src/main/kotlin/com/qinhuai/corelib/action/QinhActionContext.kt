package com.qinhuai.corelib.action

import com.qinhuai.corelib.action.skill.RawSkillContext
import com.qinhuai.corelib.action.skill.TriggerType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** dispatch 上下文 — 只读 ICVM 产物 + opaque payload */
data class QinhActionContext(
    val trigger: String,
    val player: Player,
    val item: ItemStack,
    val itemId: String,
    val handlerId: String,
    /** opaque — QI passes through unchanged */
    val payload: String,
    val compileEpoch: Long? = null,
    val providerSnapshot: Any? = null,
    val triggerType: TriggerType? = null,
    val rawContext: RawSkillContext? = null,
)
