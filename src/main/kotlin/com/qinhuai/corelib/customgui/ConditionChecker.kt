package com.qinhuai.corelib.customgui

import com.qinhuai.corelib.economy.EconomyBridge
import com.qinhuai.corelib.script.QinhScriptBridge
import org.bukkit.entity.Player

object ConditionChecker {
    fun check(player: Player, requirement: ViewRequirement): Boolean {
        val result = try {
            when (requirement.type.lowercase()) {
                "permission" -> checkPermission(player, requirement.value)
                "has_item" -> checkHasItem(player, requirement.value)
                "level" -> checkLevel(player, requirement.value)
                "money" -> checkMoney(player, requirement.value)
                "exp" -> checkExp(player, requirement.value)
                "health" -> checkHealth(player, requirement.value)
                "food" -> checkFood(player, requirement.value)
                "world" -> checkWorld(player, requirement.value)
                "gamemode" -> checkGamemode(player, requirement.value)
                "javascript", "js", "script" -> checkJavaScript(player, requirement.value)
                else -> true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            true
        }
        
        return if (requirement.negate) !result else result
    }
    
    fun checkAll(player: Player, requirements: List<ViewRequirement>): Boolean {
        return requirements.all { check(player, it) }
    }
    
    private fun checkPermission(player: Player, value: String): Boolean {
        return player.hasPermission(value)
    }
    
    private fun checkHasItem(player: Player, value: String): Boolean {
        val parts = value.split(":", limit = 2)
        val materialName = parts.getOrNull(0) ?: return false
        val amount = parts.getOrNull(1)?.toIntOrNull() ?: 1
        
        val material = org.bukkit.Material.matchMaterial(materialName) ?: return false
        
        var count = 0
        for (item in player.inventory.contents) {
            if (item != null && item.type == material) {
                count += item.amount
            }
        }
        
        return count >= amount
    }
    
    private fun checkLevel(player: Player, value: String): Boolean {
        return checkNumeric(player.level, value)
    }
    
    private fun checkHealth(player: Player, value: String): Boolean {
        return checkNumeric(player.health.toInt(), value)
    }
    
    private fun checkFood(player: Player, value: String): Boolean {
        return checkNumeric(player.foodLevel, value)
    }
    
    private fun checkNumeric(current: Int, value: String): Boolean {
        val operator = when {
            value.startsWith(">=") -> ">="
            value.startsWith("<=") -> "<="
            value.startsWith(">") -> ">"
            value.startsWith("<") -> "<"
            value.startsWith("==") -> "=="
            else -> "=="
        }
        
        val target = value.replace(Regex("^[<>]=?"), "").toIntOrNull() ?: return true
        
        return when (operator) {
            ">=" -> current >= target
            "<=" -> current <= target
            ">" -> current > target
            "<" -> current < target
            "==" -> current == target
            else -> true
        }
    }
    
    private fun checkWorld(player: Player, value: String): Boolean {
        return player.world.name.equals(value, ignoreCase = true)
    }
    
    private fun checkGamemode(player: Player, value: String): Boolean {
        return player.gameMode.name.equals(value, ignoreCase = true)
    }
    
    private fun checkMoney(player: Player, value: String): Boolean {
        val requirement = EconomyBridge.parseMoneyRequirement(value) ?: return false
        if (!EconomyBridge.isAvailable()) {
            return false
        }
        val balance = EconomyBridge.getBalance(player, requirement.providerId, requirement.currencyId)
        return checkNumericDouble(balance, requirement.operator, requirement.amount)
    }

    private fun checkNumericDouble(current: Double, operator: String, target: Double): Boolean {
        return when (operator) {
            ">=" -> current >= target
            "<=" -> current <= target
            ">" -> current > target
            "<" -> current < target
            "==" -> current == target
            else -> current >= target
        }
    }
    
    private fun checkJavaScript(player: Player, value: String): Boolean {
        if (!QinhScriptBridge.isAvailable()) {
            return false
        }
        return QinhScriptBridge.evaluateBoolean(value, player)
    }

    private fun checkExp(player: Player, value: String): Boolean {
        return true
    }
}
