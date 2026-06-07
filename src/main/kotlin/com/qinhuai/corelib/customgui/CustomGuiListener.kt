package com.qinhuai.corelib.customgui

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent

object CustomGuiListener : Listener {
    private sealed interface ActiveGui {
        val player: Player
    }

    private class ActiveCustomGui(val gui: CustomGui) : ActiveGui {
        override val player: Player get() = gui.player
    }

    private class ActiveDynamicGui(val gui: DynamicGui) : ActiveGui {
        override val player: Player get() = gui.player
    }

    private val activeGuis = mutableMapOf<Player, ActiveGui>()
    private val lastClickTime = mutableMapOf<Player, Long>()

    fun register(gui: CustomGui) {
        activeGuis[gui.player] = ActiveCustomGui(gui)
    }

    fun register(gui: DynamicGui) {
        activeGuis[gui.player] = ActiveDynamicGui(gui)
    }

    fun unregister(player: Player) {
        activeGuis.remove(player)
        lastClickTime.remove(player)
    }

    fun unregister(gui: CustomGui) {
        if (activeGuis[gui.player] is ActiveCustomGui) {
            unregister(gui.player)
        }
    }

    fun unregister(gui: DynamicGui) {
        if (activeGuis[gui.player] is ActiveDynamicGui) {
            unregister(gui.player)
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        when (val active = activeGuis[player] ?: return) {
            is ActiveCustomGui -> handleCustomClick(event, active.gui)
            is ActiveDynamicGui -> handleDynamicClick(event, active.gui)
        }
    }

    private fun handleCustomClick(event: InventoryClickEvent, gui: CustomGui) {
        event.isCancelled = true
        if (!throttleClick(event.whoClicked as Player)) return

        val inv = gui.getInventory() ?: return
        val slot = event.rawSlot
        if (slot >= 0 && slot < inv.size) {
            gui.handleClick(slot, event.click, event.isShiftClick)
        }
    }

    private fun handleDynamicClick(event: InventoryClickEvent, gui: DynamicGui) {
        event.isCancelled = true
        val player = event.whoClicked as Player
        if (!throttleClick(player)) return

        val inv = gui.getInventory() ?: return
        val slot = event.rawSlot
        if (slot >= 0 && slot < inv.size) {
            if (gui.handleClick(slot, event.click)) {
                gui.refresh()
            }
        }
    }

    private fun throttleClick(player: Player): Boolean {
        val now = System.currentTimeMillis()
        val lastClick = lastClickTime[player] ?: 0L
        if (now - lastClick < 150) {
            return false
        }
        lastClickTime[player] = now
        return true
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val closedInv = event.view.topInventory
        when (val active = activeGuis[player] ?: return) {
            is ActiveCustomGui -> {
                if (active.gui.getInventory() == closedInv) {
                    active.gui.close()
                }
            }
            is ActiveDynamicGui -> {
                if (active.gui.getInventory() == closedInv) {
                    active.gui.onPlayerClose()
                }
            }
        }
    }
}
