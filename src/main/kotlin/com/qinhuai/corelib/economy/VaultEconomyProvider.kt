package com.qinhuai.corelib.economy

import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.java.JavaPlugin

class VaultEconomyProvider(
    private val plugin: JavaPlugin,
) : EconomyProvider {

    override val id: String = "vault"

    override fun isAvailable(): Boolean = economy() != null

    override fun getBalance(player: OfflinePlayer, currencyId: String?): Double {
        val eco = economy() ?: return 0.0
        return eco.getBalance(player)
    }

    override fun has(player: OfflinePlayer, amount: Double, currencyId: String?): Boolean {
        val eco = economy() ?: return false
        return eco.has(player, amount)
    }

    override fun deposit(player: OfflinePlayer, amount: Double, currencyId: String?): EconomyTransactionResult {
        val eco = economy() ?: return EconomyTransactionResult.fail("Vault 经济不可用", suggestion = "确认 Vault 及经济实现插件是否已安装并启用", provider = id)
        val response: EconomyResponse = eco.depositPlayer(player, amount)
        return if (response.transactionSuccess()) {
            EconomyTransactionResult.ok(id)
        } else {
            EconomyTransactionResult.fail(response.errorMessage ?: "Vault 存款失败", suggestion = "检查玩家余额、经济后端状态与权限", provider = id)
        }
    }

    override fun withdraw(player: OfflinePlayer, amount: Double, currencyId: String?): EconomyTransactionResult {
        val eco = economy() ?: return EconomyTransactionResult.fail("Vault 经济不可用", suggestion = "确认 Vault 及经济实现插件是否已安装并启用", provider = id)
        if (!eco.has(player, amount)) {
            return EconomyTransactionResult.fail("余额不足", code = "INSUFFICIENT_FUNDS", suggestion = "减少消耗或补充余额", provider = id)
        }
        val response: EconomyResponse = eco.withdrawPlayer(player, amount)
        return if (response.transactionSuccess()) {
            EconomyTransactionResult.ok(id)
        } else {
            EconomyTransactionResult.fail(response.errorMessage ?: "Vault 扣款失败", suggestion = "检查经济后端响应与玩家状态", provider = id)
        }
    }

    override fun setBalance(player: OfflinePlayer, amount: Double, currencyId: String?): EconomyTransactionResult {
        val eco = economy() ?: return EconomyTransactionResult.fail("Vault 经济不可用", suggestion = "确认 Vault 及经济实现插件是否已安装并启用", provider = id)
        val current = eco.getBalance(player)
        return when {
            current == amount -> EconomyTransactionResult.ok(id)
            current < amount -> deposit(player, amount - current, null)
            else -> withdraw(player, current - amount, null)
        }
    }

    private fun economy(): Economy? {
        if (plugin.server.pluginManager.getPlugin("Vault") == null) {
            return null
        }
        return plugin.server.servicesManager.getRegistration(Economy::class.java)?.provider
    }
}
