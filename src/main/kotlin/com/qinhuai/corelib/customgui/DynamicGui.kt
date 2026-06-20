package com.qinhuai.corelib.customgui

import com.qinhuai.corelib.util.TextUtil
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class DynamicGui(
    val player: Player,
    val rows: Int,
    val title: String,
    private val openSound: String? = null,
    private val closeSound: String? = null,
    private val renderer: DynamicGuiRenderer,
) {
    private var inventory: Inventory? = null
    private val slotHandlers = mutableMapOf<Int, DynamicGuiClickHandler>()

    fun open() {
        inventory = Bukkit.createInventory(null, rows * 9, TextUtil.toComponent(title))
        refresh()
        player.openInventory(inventory!!)
        CustomGuiListener.register(this)
        openSound?.let { playSound(it) }
    }

    fun refresh() {
        val inv = inventory ?: return
        inv.clear()
        slotHandlers.clear()
        renderer.render(this, inv)
    }

    fun onPlayerClose() {
        closeSound?.let { playSound(it) }
        CustomGuiListener.unregister(this)
        inventory = null
    }

    fun close() {
        val inv = inventory
        onPlayerClose()
        if (inv != null && player.openInventory.topInventory == inv) {
            player.closeInventory()
        }
    }

    fun setItem(slot: Int, item: ItemStack, onClick: DynamicGuiClickHandler? = null) {
        val inv = inventory ?: return
        if (slot < 0 || slot >= inv.size) return
        inv.setItem(slot, item)
        if (onClick != null) {
            slotHandlers[slot] = onClick
        }
    }

    fun handleClick(slot: Int, clickType: ClickType): Boolean {
        return slotHandlers[slot]?.onClick(clickType) ?: false
    }

    fun getInventory(): Inventory? = inventory

    private fun playSound(soundStr: String) {
        val parts = soundStr.split(",")
        val soundName = parts.getOrNull(0) ?: "ENTITY_PLAYER_LEVELUP"
        val volume = parts.getOrNull(1)?.toFloatOrNull() ?: 1.0f
        val pitch = parts.getOrNull(2)?.toFloatOrNull() ?: 1.0f
        com.qinhuai.corelib.util.ServerCompat.resolveSound(soundName)?.let { sound ->
            player.playSound(player.location, sound, volume, pitch)
        }
    }

    companion object {
        fun applyDisplay(item: ItemStack, name: String?, lore: List<String>) {
            val meta = item.itemMeta ?: return
            TextUtil.applyItemDisplay(meta, name, lore)
            item.itemMeta = meta
        }
    }
}
