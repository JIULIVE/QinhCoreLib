package com.qinhuai.corelib.customgui

import com.qinhuai.corelib.economy.EconomyGuiActions
import com.qinhuai.corelib.script.QinhScriptBridge
import com.qinhuai.corelib.script.ScriptContext
import com.qinhuai.corelib.util.TextUtil
import com.qinhuai.corelib.util.maxHealthValue
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

object ActionExecutor {
    fun execute(
        player: Player,
        gui: CustomGui,
        actions: List<ClickAction>,
        clickType: ClickType,
        isShift: Boolean
    ) {
        for (action in actions) {
            val matchesClickType = action.clickTypes.any { configType ->
                when (configType.lowercase()) {
                    "left" -> clickType.isLeftClick
                    "right" -> clickType.isRightClick
                    "middle" -> clickType == ClickType.MIDDLE
                    "all", "any" -> true
                    else -> configType.equals(clickType.name, ignoreCase = true)
                }
            }
            val matchesShift = !action.shift || isShift
            
            if (matchesClickType && matchesShift) {
                executeSingle(player, gui, action)
            }
        }
    }
    
    private fun executeSingle(player: Player, gui: CustomGui, action: ClickAction) {
        val value = PlaceholderManager.replace(action.value, player, gui)
        
        try {
            when (action.type.lowercase()) {
                "command", "cmd" -> executeCommand(player, value)
                "console_command", "console_cmd" -> executeConsoleCommand(value)
                "message", "msg" -> sendMessage(player, value)
                "sound" -> playSound(player, value)
                "open_gui", "gui" -> openGui(player, value)
                "close" -> closeGui(player)
                "refresh" -> refreshGui(gui)
                "player_command", "player_cmd" -> executePlayerCommand(player, value)
                "broadcast" -> broadcastMessage(value)
                "teleport" -> teleportPlayer(player, value)
                "give_item" -> giveItem(player, value)
                "take_item" -> takeItem(player, value)
                "clear_inventory" -> clearInventory(player)
                "heal" -> healPlayer(player, value)
                "feed" -> feedPlayer(player, value)
                "gamemode" -> setGamemode(player, value)
                "fly" -> toggleFly(player, value)
                "effect" -> giveEffect(player, value)
                "give_money", "givemoney" -> EconomyGuiActions.giveMoney(player, value)
                "take_money", "takemoney", "remove_money" -> EconomyGuiActions.takeMoney(player, value)
                "set_money", "setmoney" -> EconomyGuiActions.setMoney(player, value)
                "javascript", "js", "run_script", "script" -> runJavaScript(player, gui, value)
                else -> {
                    Bukkit.getLogger().warning("[QinhCoreLib] Unknown action type: ${action.type}")
                }
            }
        } catch (e: Exception) {
            Bukkit.getLogger().severe("[QinhCoreLib] Error executing action ${action.type}: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun runJavaScript(player: Player, gui: CustomGui, value: String) {
        val result = QinhScriptBridge.execute(
            value,
            ScriptContext(
                plugin = com.qinhuai.corelib.QinhCoreLib.instance,
                player = player,
                variables = mutableMapOf("guiId" to gui.config.id),
            ),
        )
        if (!result.success && result.message.isNotBlank()) {
            TextUtil.sendColored(player, result.message)
        }
    }

    private fun takeItem(player: Player, value: String) {
        val parts = value.split(":", limit = 2)
        val materialName = parts.getOrNull(0) ?: return
        val amount = parts.getOrNull(1)?.toIntOrNull() ?: 1
        
        val material = org.bukkit.Material.matchMaterial(materialName) ?: return
        
        var remaining = amount
        for (item in player.inventory.contents) {
            if (item != null && item.type == material && remaining > 0) {
                val toTake = Math.min(item.amount, remaining)
                item.amount -= toTake
                remaining -= toTake
            }
        }
    }
    
    private fun clearInventory(player: Player) {
        player.inventory.clear()
    }
    
    private fun healPlayer(player: Player, value: String) {
        val max = player.maxHealthValue()
        val amount = value.toDoubleOrNull() ?: max
        player.health = Math.min(player.health + amount, max)
    }
    
    private fun feedPlayer(player: Player, value: String) {
        val amount = value.toIntOrNull() ?: 20
        player.foodLevel = Math.min(player.foodLevel + amount, 20)
    }
    
    private fun setGamemode(player: Player, value: String) {
        val gamemode = try {
            org.bukkit.GameMode.valueOf(value.uppercase())
        } catch (e: Exception) {
            null
        }
        gamemode?.let { player.gameMode = it }
    }
    
    private fun toggleFly(player: Player, value: String) {
        val shouldFly = value.lowercase() == "true" || value.isEmpty() && !player.allowFlight
        player.allowFlight = shouldFly
        player.isFlying = shouldFly
    }
    
    private fun giveEffect(player: Player, value: String) {
        val parts = value.split(",")
        if (parts.size < 2) return
        
        val effectName = parts[0]
        val duration = parts.getOrNull(1)?.toIntOrNull() ?: 60
        val amplifier = parts.getOrNull(2)?.toIntOrNull() ?: 0
        
        val effect = com.qinhuai.corelib.util.ServerCompat.resolvePotionEffectType(effectName) ?: return
        val potionEffect = org.bukkit.potion.PotionEffect(effect, duration * 20, amplifier)
        player.addPotionEffect(potionEffect)
    }
    
    private fun executeCommand(player: Player, command: String) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
    }
    
    private fun executeConsoleCommand(command: String) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
    }
    
    private fun executePlayerCommand(player: Player, command: String) {
        player.performCommand(command)
    }
    
    private fun sendMessage(player: Player, message: String) {
        TextUtil.sendColored(player, message)
    }

    private fun broadcastMessage(message: String) {
        TextUtil.broadcastColored(message)
    }
    
    private fun playSound(player: Player, soundStr: String) {
        val parts = soundStr.split(",")
        val soundName = parts.getOrNull(0) ?: "ENTITY_PLAYER_LEVELUP"
        val volume = parts.getOrNull(1)?.toFloatOrNull() ?: 1.0f
        val pitch = parts.getOrNull(2)?.toFloatOrNull() ?: 1.0f
        
        val sound = com.qinhuai.corelib.util.ServerCompat.resolveSound(soundName)
        if (sound == null) {
            Bukkit.getLogger().warning("[QinhCoreLib] Invalid sound: $soundName")
            return
        }
        player.playSound(player.location, sound, volume, pitch)
    }
    
    private fun openGui(player: Player, guiId: String) {
        CustomGuiManager.openGui(player, guiId)
    }
    
    private fun closeGui(player: Player) {
        player.closeInventory()
    }
    
    private fun refreshGui(gui: CustomGui) {
        gui.refresh()
    }
    
    private fun teleportPlayer(player: Player, locationStr: String) {
        val parts = locationStr.split(",")
        if (parts.size >= 3) {
            val world = parts.getOrNull(3)?.let { Bukkit.getWorld(it) } ?: player.world
            val x = parts.getOrNull(0)?.toDoubleOrNull() ?: player.location.x
            val y = parts.getOrNull(1)?.toDoubleOrNull() ?: player.location.y
            val z = parts.getOrNull(2)?.toDoubleOrNull() ?: player.location.z
            
            player.teleport(org.bukkit.Location(world, x, y, z))
        }
    }
    
    private fun giveItem(player: Player, itemStr: String) {
        val parts = itemStr.split(":", limit = 2)
        val materialName = parts.getOrNull(0) ?: return
        val amount = parts.getOrNull(1)?.toIntOrNull() ?: 1
        
        val material = org.bukkit.Material.matchMaterial(materialName) ?: return
        val item = org.bukkit.inventory.ItemStack(material, amount)
        
        player.inventory.addItem(item)
    }
}
