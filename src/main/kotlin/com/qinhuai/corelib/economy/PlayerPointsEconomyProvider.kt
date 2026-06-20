package com.qinhuai.corelib.economy

import com.qinhuai.corelib.lang.Lang
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

class PlayerPointsEconomyProvider(
    private val plugin: JavaPlugin,
) : EconomyProvider {

    override val id: String = "playerpoints"

    override fun isAvailable(): Boolean = api() != null

    override fun getBalance(player: OfflinePlayer, currencyId: String?): Double {
        val api = api() ?: return 0.0
        return invokeInt(api, "look", player.uniqueId)?.toDouble() ?: 0.0
    }

    override fun has(player: OfflinePlayer, amount: Double, currencyId: String?): Boolean {
        return getBalance(player, currencyId) >= amount
    }

    override fun deposit(player: OfflinePlayer, amount: Double, currencyId: String?): EconomyTransactionResult {
        val api = api() ?: return EconomyTransactionResult.fail(Lang.get("player-points-economy-provider.unavailable"), suggestion = Lang.get("player-points-economy-provider.unavailable-suggestion"), provider = id)
        val points = amount.toPoints() ?: return EconomyTransactionResult.fail(Lang.get("player-points-economy-provider.invalid-amount"), code = "INVALID_AMOUNT", suggestion = Lang.get("player-points-economy-provider.invalid-amount-suggestion"), provider = id)
        return if (invokeBoolean(api, "give", player.uniqueId, points) == true) {
            EconomyTransactionResult.ok(id)
        } else {
            EconomyTransactionResult.fail(Lang.get("player-points-economy-provider.deposit-failed"), suggestion = Lang.get("player-points-economy-provider.api-status-suggestion"), provider = id)
        }
    }

    override fun withdraw(player: OfflinePlayer, amount: Double, currencyId: String?): EconomyTransactionResult {
        val api = api() ?: return EconomyTransactionResult.fail(Lang.get("player-points-economy-provider.unavailable"), suggestion = Lang.get("player-points-economy-provider.unavailable-suggestion"), provider = id)
        val points = amount.toPoints() ?: return EconomyTransactionResult.fail(Lang.get("player-points-economy-provider.invalid-amount"), code = "INVALID_AMOUNT", suggestion = Lang.get("player-points-economy-provider.invalid-amount-suggestion"), provider = id)
        if (getBalance(player, currencyId) < amount) {
            return EconomyTransactionResult.fail(Lang.get("player-points-economy-provider.insufficient"), code = "INSUFFICIENT_FUNDS", suggestion = Lang.get("player-points-economy-provider.insufficient-suggestion"), provider = id)
        }
        return if (invokeBoolean(api, "take", player.uniqueId, points) == true) {
            EconomyTransactionResult.ok(id)
        } else {
            EconomyTransactionResult.fail(Lang.get("player-points-economy-provider.withdraw-failed"), suggestion = Lang.get("player-points-economy-provider.api-status-suggestion"), provider = id)
        }
    }

    override fun setBalance(player: OfflinePlayer, amount: Double, currencyId: String?): EconomyTransactionResult {
        val api = api() ?: return EconomyTransactionResult.fail(Lang.get("player-points-economy-provider.unavailable"), suggestion = Lang.get("player-points-economy-provider.unavailable-suggestion"), provider = id)
        val points = amount.toPoints() ?: return EconomyTransactionResult.fail(Lang.get("player-points-economy-provider.invalid-amount"), code = "INVALID_AMOUNT", suggestion = Lang.get("player-points-economy-provider.invalid-amount-suggestion"), provider = id)
        return if (invokeBoolean(api, "set", player.uniqueId, points) == true) {
            EconomyTransactionResult.ok(id)
        } else {
            EconomyTransactionResult.fail(Lang.get("player-points-economy-provider.set-failed"), suggestion = Lang.get("player-points-economy-provider.api-status-suggestion"), provider = id)
        }
    }

    private fun api(): Any? {
        if (plugin.server.pluginManager.getPlugin("PlayerPoints") == null) {
            return null
        }
        return try {
            val clazz = Class.forName("org.black_ixx.playerpoints.PlayerPoints")
            val instance = clazz.getMethod("getInstance").invoke(null) ?: return null
            instance.javaClass.getMethod("getAPI").invoke(instance)
        } catch (_: Throwable) {
            null
        }
    }

    private fun invokeInt(api: Any, method: String, uuid: UUID): Int? {
        return try {
            when (val result = api.javaClass.getMethod(method, UUID::class.java).invoke(api, uuid)) {
                is Int -> result
                is Number -> result.toInt()
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun invokeBoolean(api: Any, method: String, uuid: UUID, points: Int): Boolean? {
        return try {
            when (val result = api.javaClass.getMethod(method, UUID::class.java, Int::class.javaPrimitiveType)
                .invoke(api, uuid, points)) {
                is Boolean -> result
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun Double.toPoints(): Int? {
        if (this.isNaN() || this.isInfinite() || this < 0) return null
        val rounded = kotlin.math.round(this).toInt()
        return if (rounded < 0) null else rounded
    }
}
