package com.qinhuai.corelib.action.skill

import org.bukkit.inventory.ItemStack

/** QI 透传的只读原始上下文 — QS 不据此做业务分支，仅审计/trace */
data class RawSkillContext(
    val itemId: String? = null,
    val item: ItemStack? = null,
    val sneak: Boolean = false,
    val source: String = "qi",
)
