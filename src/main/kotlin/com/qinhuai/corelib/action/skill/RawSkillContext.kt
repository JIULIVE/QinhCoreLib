package com.qinhuai.corelib.action.skill

import org.bukkit.inventory.ItemStack

data class RawSkillContext(
    val itemId: String? = null,
    val item: ItemStack? = null,
    val sneak: Boolean = false,
    val source: String = "qi",
)
