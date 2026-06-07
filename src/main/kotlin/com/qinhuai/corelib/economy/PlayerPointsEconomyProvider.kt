package com.qinhuai.corelib.economy

import org.bukkit.OfflinePlayer
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

/**
 * PlayerPoints（反射），点券为整数，接口层仍用 Double。
 */
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
        val api = api() ?: return EconomyTransactionResult.fail("PlayerPoints 不可用", suggestion = "确认 PlayerPoints 已安装并启用", provider = id)
        val points = amount.toPoints() ?: return EconomyTransactionResult.fail("无效点券数量", code = "INVALID_AMOUNT", suggestion = "金额必须是非负有限数", provider = id)
        return if (invokeBoolean(api, "give", player.uniqueId, points) == true) {
            EconomyTransactionResult.ok(id)
        } else {
            EconomyTransactionResult.fail("PlayerPoints 发放失败", suggestion = "检查 PlayerPoints API 状态", provider = id)
        }
    }

    override fun withdraw(player: OfflinePlayer, amount: Double, currencyId: String?): EconomyTransactionResult {
        val api = api() ?: return EconomyTransactionResult.fail("PlayerPoints 不可用", suggestion = "确认 PlayerPoints 已安装并启用", provider = id)
        val points = amount.toPoints() ?: return EconomyTransactionResult.fail("无效点券数量", code = "INVALID_AMOUNT", suggestion = "金额必须是非负有限数", provider = id)
        if (getBalance(player, currencyId) < amount) {
            return EconomyTransactionResult.fail("点券不足", code = "INSUFFICIENT_FUNDS", suggestion = "减少消耗或补充点券", provider = id)
        }
        return if (invokeBoolean(api, "take", player.uniqueId, points) == true) {
            EconomyTransactionResult.ok(id)
        } else {
            EconomyTransactionResult.fail("PlayerPoints 扣除失败", suggestion = "检查 PlayerPoints API 状态", provider = id)
        }
    }

    override fun setBalance(player: OfflinePlayer, amount: Double, currencyId: String?): EconomyTransactionResult {
        val api = api() ?: return EconomyTransactionResult.fail("PlayerPoints 不可用", suggestion = "确认 PlayerPoints 已安装并启用", provider = id)
        val points = amount.toPoints() ?: return EconomyTransactionResult.fail("无效点券数量", code = "INVALID_AMOUNT", suggestion = "金额必须是非负有限数", provider = id)
        return if (invokeBoolean(api, "set", player.uniqueId, points) == true) {
            EconomyTransactionResult.ok(id)
        } else {
            EconomyTransactionResult.fail("PlayerPoints 设置失败", suggestion = "检查 PlayerPoints API 状态", provider = id)
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
