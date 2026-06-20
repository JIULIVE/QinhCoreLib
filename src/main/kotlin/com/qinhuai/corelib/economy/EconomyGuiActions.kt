package com.qinhuai.corelib.economy

import com.qinhuai.corelib.lang.Lang
import com.qinhuai.corelib.util.TextUtil
import org.bukkit.entity.Player

object EconomyGuiActions {

    fun giveMoney(player: Player, rawValue: String) {
        val spec = EconomyActionParser.parse(rawValue)
        if (spec == null) {
            notify(player, null, Lang.get("economy-gui-actions.format-error"))
            return
        }
        if (!EconomyBridge.isAvailable()) {
            notify(player, spec.failMessage, Lang.get("economy-gui-actions.unavailable"))
            return
        }
        val result = EconomyBridge.deposit(player, spec.amount, spec.providerId, spec.currencyId)
        if (!result.success) {
            notify(player, spec.failMessage, result.message ?: Lang.get("economy-gui-actions.deposit-failed"))
        }
    }

    fun takeMoney(player: Player, rawValue: String) {
        val spec = EconomyActionParser.parse(rawValue)
        if (spec == null) {
            notify(player, null, Lang.get("economy-gui-actions.format-error"))
            return
        }
        if (!EconomyBridge.isAvailable()) {
            notify(player, spec.failMessage, Lang.get("economy-gui-actions.unavailable"))
            return
        }
        if (!EconomyBridge.has(player, spec.amount, spec.providerId, spec.currencyId)) {
            notify(player, spec.failMessage, Lang.get("economy-gui-actions.insufficient-balance"))
            return
        }
        val result = EconomyBridge.withdraw(player, spec.amount, spec.providerId, spec.currencyId)
        if (!result.success) {
            notify(player, spec.failMessage, result.message ?: Lang.get("economy-gui-actions.withdraw-failed"))
        }
    }

    fun setMoney(player: Player, rawValue: String) {
        val spec = EconomyActionParser.parse(rawValue)
        if (spec == null) {
            notify(player, null, Lang.get("economy-gui-actions.format-error"))
            return
        }
        if (!EconomyBridge.isAvailable()) {
            notify(player, spec.failMessage, Lang.get("economy-gui-actions.unavailable"))
            return
        }
        val result = EconomyBridge.setBalance(player, spec.amount, spec.providerId, spec.currencyId)
        if (!result.success) {
            notify(player, spec.failMessage, result.message ?: Lang.get("economy-gui-actions.set-balance-failed"))
        }
    }

    private fun notify(player: Player, custom: String?, fallback: String) {
        TextUtil.sendColored(player, custom ?: fallback)
    }
}
