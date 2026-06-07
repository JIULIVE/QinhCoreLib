package com.qinhuai.corelib.economy

import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * 通过反射对接 ExcellentEconomy，避免编译期强依赖其 jar。
 */
class ExcellentEconomyProvider(
    private val plugin: JavaPlugin,
) : EconomyProvider {

    override val id: String = "excellenteconomy"

    override fun isAvailable(): Boolean {
        val api = api() ?: return false
        return try {
            api.javaClass.getMethod("canPerformOperations").invoke(api) as? Boolean ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun getBalance(player: OfflinePlayer, currencyId: String?): Double {
        val online = player.player ?: return 0.0
        val api = api() ?: return 0.0
        val currency = resolveCurrency(api, currencyId) ?: return 0.0
        return invokeDouble(api, "getBalance", online, currency) ?: 0.0
    }

    override fun has(player: OfflinePlayer, amount: Double, currencyId: String?): Boolean {
        return getBalance(player, currencyId) >= amount
    }

    override fun deposit(player: OfflinePlayer, amount: Double, currencyId: String?): EconomyTransactionResult {
        val online = player.player
            ?: return EconomyTransactionResult.fail("ExcellentEconomy 需要玩家在线", suggestion = "仅在线玩家可进行该操作", provider = id)
        val api = api() ?: return EconomyTransactionResult.fail("ExcellentEconomy 不可用", suggestion = "确认 ExcellentEconomy 已安装并启用", provider = id)
        val currency = resolveCurrency(api, currencyId)
            ?: return EconomyTransactionResult.fail("货币不存在: ${currencyId ?: "(未指定)"}", code = "CURRENCY_NOT_FOUND", suggestion = "检查 currencyId 配置", provider = id)
        return if (invokeTransaction(api, "deposit", online, currency, amount)) {
            EconomyTransactionResult.ok(id)
        } else {
            EconomyTransactionResult.fail("ExcellentEconomy 存款失败", suggestion = "检查货币状态与玩家余额", provider = id)
        }
    }

    override fun withdraw(player: OfflinePlayer, amount: Double, currencyId: String?): EconomyTransactionResult {
        val online = player.player
            ?: return EconomyTransactionResult.fail("ExcellentEconomy 需要玩家在线", suggestion = "仅在线玩家可进行该操作", provider = id)
        val api = api() ?: return EconomyTransactionResult.fail("ExcellentEconomy 不可用", suggestion = "确认 ExcellentEconomy 已安装并启用", provider = id)
        val currency = resolveCurrency(api, currencyId)
            ?: return EconomyTransactionResult.fail("货币不存在: ${currencyId ?: "(未指定)"}", code = "CURRENCY_NOT_FOUND", suggestion = "检查 currencyId 配置", provider = id)
        if (getBalance(online, currencyId) < amount) {
            return EconomyTransactionResult.fail("余额不足", code = "INSUFFICIENT_FUNDS", suggestion = "减少消耗或补充余额", provider = id)
        }
        return if (invokeTransaction(api, "withdraw", online, currency, amount)) {
            EconomyTransactionResult.ok(id)
        } else {
            EconomyTransactionResult.fail("ExcellentEconomy 扣款失败", suggestion = "检查货币状态与玩家余额", provider = id)
        }
    }

    override fun setBalance(player: OfflinePlayer, amount: Double, currencyId: String?): EconomyTransactionResult {
        val online = player.player
            ?: return EconomyTransactionResult.fail("ExcellentEconomy 需要玩家在线", suggestion = "仅在线玩家可进行该操作", provider = id)
        val api = api() ?: return EconomyTransactionResult.fail("ExcellentEconomy 不可用", suggestion = "确认 ExcellentEconomy 已安装并启用", provider = id)
        val currency = resolveCurrency(api, currencyId)
            ?: return EconomyTransactionResult.fail("货币不存在: ${currencyId ?: "(未指定)"}", code = "CURRENCY_NOT_FOUND", suggestion = "检查 currencyId 配置", provider = id)
        return if (invokeTransaction(api, "setBalance", online, currency, amount)) {
            EconomyTransactionResult.ok(id)
        } else {
            EconomyTransactionResult.fail("ExcellentEconomy 设置余额失败", suggestion = "检查货币状态与玩家余额", provider = id)
        }
    }

    private fun api(): Any? {
        if (plugin.server.pluginManager.getPlugin("ExcellentEconomy") == null) {
            return null
        }
        return try {
            val apiClass = Class.forName("su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI")
            plugin.server.servicesManager.getRegistration(apiClass)?.provider
        } catch (_: Throwable) {
            null
        }
    }

    private fun resolveCurrency(api: Any, currencyId: String?): Any? {
        if (currencyId.isNullOrBlank()) {
            return null
        }
        return try {
            api.javaClass.getMethod("getCurrency", String::class.java).invoke(api, currencyId)
        } catch (_: Throwable) {
            null
        }
    }

    private fun invokeDouble(target: Any, method: String, vararg args: Any?): Double? {
        return try {
            val m = target.javaClass.methods.firstOrNull {
                it.name == method && it.parameterCount == args.size
            } ?: return null
            when (val result = m.invoke(target, *args)) {
                is Double -> result
                is Number -> result.toDouble()
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun invokeTransaction(api: Any, method: String, player: Player, currency: Any, amount: Double): Boolean {
        return try {
            val m = api.javaClass.methods.firstOrNull {
                it.name == method &&
                    it.parameterCount == 3 &&
                    Player::class.java.isAssignableFrom(it.parameterTypes[0])
            } ?: return false
            when (val result = m.invoke(api, player, currency, amount)) {
                is Boolean -> result
                else -> isOperationSuccess(result)
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun isOperationSuccess(result: Any?): Boolean {
        if (result == null) return false
        if (result is Boolean) return result
        for (name in listOf("isSuccess", "success", "isSuccessful")) {
            try {
                val v = result.javaClass.getMethod(name).invoke(result)
                if (v is Boolean) return v
            } catch (_: Throwable) {
            }
        }
        return true
    }
}
