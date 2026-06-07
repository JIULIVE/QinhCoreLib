package com.qinhuai.corelib.customgui

import com.qinhuai.corelib.placeholder.PapiBridge
import com.qinhuai.corelib.util.maxHealthValue
import org.bukkit.entity.Player
import java.text.SimpleDateFormat
import java.util.*

object PlaceholderManager {
    private val placeholders = mutableMapOf<String, (Player) -> String>()
    private val guiPlaceholders = mutableMapOf<String, (Player, CustomGui) -> String>()
    private val itemPlaceholders = mutableMapOf<String, (Player, CustomGui, Any, Int) -> String>()
    
    init {
        registerDefaultPlaceholders()
    }
    
    fun registerPlaceholder(key: String, provider: (Player) -> String) {
        placeholders[key.lowercase()] = provider
    }
    
    fun registerGuiPlaceholder(key: String, provider: (Player, CustomGui) -> String) {
        guiPlaceholders[key.lowercase()] = provider
    }
    
    fun registerItemPlaceholder(key: String, provider: (Player, CustomGui, Any, Int) -> String) {
        itemPlaceholders[key.lowercase()] = provider
    }
    
    fun replace(text: String, player: Player, gui: CustomGui? = null, itemData: Any? = null, itemIndex: Int = 0): String {
        var result = text
        
        for ((key, provider) in placeholders) {
            val placeholder = "{$key}"
            if (result.contains(placeholder)) {
                try {
                    result = result.replace(placeholder, provider(player))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        if (gui != null) {
            for ((key, provider) in guiPlaceholders) {
                val placeholder = "{$key}"
                if (result.contains(placeholder)) {
                    try {
                        result = result.replace(placeholder, provider(player, gui))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        
        if (gui != null && itemData != null) {
            if (itemData is GuiPaginationEntry) {
                itemData.placeholders.forEach { (k, v) ->
                    result = result.replace("{$k}", v)
                }
            }

            for ((key, provider) in itemPlaceholders) {
                val placeholder = "{$key}"
                if (result.contains(placeholder)) {
                    try {
                        result = result.replace(placeholder, provider(player, gui, itemData, itemIndex))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            when (itemData) {
                is Player -> {
                    result = result.replace("{item_name}", itemData.name)
                    result = result.replace("{item_uuid}", itemData.uniqueId.toString())
                }
                is GuiPaginationEntry -> {
                    result = result.replace("{item_name}", itemData.placeholders["crop_name"] ?: "")
                }
                else -> result = result.replace("{item_name}", itemData.toString())
            }
            result = result.replace("{item_index}", (itemIndex + 1).toString())
        }
        
        if (result.contains('%')) {
            result = PapiBridge.apply(player, result)
        }
        
        return result
    }
    
    fun replaceList(list: List<String>, player: Player, gui: CustomGui? = null, itemData: Any? = null, itemIndex: Int = 0): List<String> {
        return list.map { replace(it, player, gui, itemData, itemIndex) }
    }
    
    private fun registerDefaultPlaceholders() {
        registerPlaceholder("player") { player -> player.name }
        registerPlaceholder("uuid") { player -> player.uniqueId.toString() }
        registerPlaceholder("world") { player -> player.world.name }
        registerPlaceholder("x") { player -> player.location.blockX.toString() }
        registerPlaceholder("y") { player -> player.location.blockY.toString() }
        registerPlaceholder("z") { player -> player.location.blockZ.toString() }
        registerPlaceholder("health") { player -> player.health.toInt().toString() }
        registerPlaceholder("max_health") { player -> player.maxHealthValue().toInt().toString() }
        registerPlaceholder("food") { player -> player.foodLevel.toString() }
        registerPlaceholder("level") { player -> player.level.toString() }
        registerPlaceholder("exp") { player -> player.exp.toString() }
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val timeFormat = SimpleDateFormat("HH:mm:ss")
        registerPlaceholder("date") { _ -> dateFormat.format(Date()) }
        registerPlaceholder("time") { _ -> timeFormat.format(Date()) }
        
        registerPlaceholder("online") { _ -> 
            org.bukkit.Bukkit.getOnlinePlayers().size.toString()
        }
        registerPlaceholder("max_players") { _ -> 
            org.bukkit.Bukkit.getMaxPlayers().toString()
        }
        
        registerGuiPlaceholder("page") { _, gui -> (gui.getCurrentPage() + 1).toString() }
        registerGuiPlaceholder("max_page") { _, gui -> gui.getMaxPage().toString() }
        registerGuiPlaceholder("has_previous") { _, gui -> if (gui.getCurrentPage() > 0) "true" else "false" }
        registerGuiPlaceholder("has_next") { _, gui -> if (gui.getCurrentPage() < gui.getMaxPage() - 1) "true" else "false" }
    }
}
