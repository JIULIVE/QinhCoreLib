package com.qinhuai.corelib.economy

import com.qinhuai.corelib.util.TextUtil
import org.bukkit.entity.Player

object EconomyGuiActions {

    fun giveMoney(player: Player, rawValue: String) {
        val spec = EconomyActionParser.parse(rawValue)
        if (spec == null) {
            notify(player, null, "&c经济动作格式错误")
            return
        }
        if (!EconomyBridge.isAvailable()) {
            notify(player, spec.failMessage, "&c经济系统不可用")
            return
        }
        val result = EconomyBridge.deposit(player, spec.amount, spec.providerId, spec.currencyId)
        if (!result.success) {
            notify(player, spec.failMessage, result.message ?: "&c发放货币失败")
        }
    }

    fun takeMoney(player: Player, rawValue: String) {
        val spec = EconomyActionParser.parse(rawValue)
        if (spec == null) {
            notify(player, null, "&c经济动作格式错误")
            return
        }
        if (!EconomyBridge.isAvailable()) {
            notify(player, spec.failMessage, "&c经济系统不可用")
            return
        }
        if (!EconomyBridge.has(player, spec.amount, spec.providerId, spec.currencyId)) {
            notify(player, spec.failMessage, "&c余额不足")
            return
        }
        val result = EconomyBridge.withdraw(player, spec.amount, spec.providerId, spec.currencyId)
        if (!result.success) {
            notify(player, spec.failMessage, result.message ?: "&c扣除货币失败")
        }
    }

    fun setMoney(player: Player, rawValue: String) {
        val spec = EconomyActionParser.parse(rawValue)
        if (spec == null) {
            notify(player, null, "&c经济动作格式错误")
            return
        }
        if (!EconomyBridge.isAvailable()) {
            notify(player, spec.failMessage, "&c经济系统不可用")
            return
        }
        val result = EconomyBridge.setBalance(player, spec.amount, spec.providerId, spec.currencyId)
        if (!result.success) {
            notify(player, spec.failMessage, result.message ?: "&c设置余额失败")
        }
    }

    private fun notify(player: Player, custom: String?, fallback: String) {
        TextUtil.sendColored(player, custom ?: fallback)
    }
}
