package com.qinhuai.corelib.action.skill

import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.EquipmentSlot

object QiTriggerMapper {

    fun fromInteract(event: PlayerInteractEvent): TriggerType? {
        if (event.hand != EquipmentSlot.HAND) return null
        val sneak = event.player.isSneaking
        return when (event.action) {
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK ->
                if (sneak) TriggerType.SHIFT_RIGHT_CLICK else TriggerType.RIGHT_CLICK
            Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK ->
                if (sneak) TriggerType.SHIFT_LEFT_CLICK else TriggerType.LEFT_CLICK
            else -> null
        }
    }

    fun fromSneakToggle(event: PlayerToggleSneakEvent): TriggerType =
        TriggerType.SHIFT_TOGGLE

    fun legacyKey(type: TriggerType): String = type.legacyActionKey()
}
