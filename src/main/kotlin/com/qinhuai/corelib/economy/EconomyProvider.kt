package com.qinhuai.corelib.economy

import org.bukkit.OfflinePlayer

/**
 * 经济后端（Vault / ExcellentEconomy 等）。
 * [currencyId] 对 Vault 可忽略；对 ExcellentEconomy 必填（如 money、silver、gold）。
 */
interface EconomyProvider {
    val id: String

    fun isAvailable(): Boolean

    fun getBalance(player: OfflinePlayer, currencyId: String?): Double

    fun has(player: OfflinePlayer, amount: Double, currencyId: String?): Boolean

    fun deposit(player: OfflinePlayer, amount: Double, currencyId: String?): EconomyTransactionResult

    fun withdraw(player: OfflinePlayer, amount: Double, currencyId: String?): EconomyTransactionResult

    fun setBalance(player: OfflinePlayer, amount: Double, currencyId: String?): EconomyTransactionResult
}
