package com.qinhuai.corelib.economy

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.bootstrap.StartupReporter
import com.qinhuai.corelib.debug.BridgeStatus
import com.qinhuai.corelib.debug.BridgeStatusRegistry
import com.qinhuai.corelib.debug.DiagnosticResult
import com.qinhuai.corelib.lang.Lang
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.java.JavaPlugin
import java.util.Locale

data class MoneyRequirement(
    val providerId: String?,
    val currencyId: String?,
    val operator: String,
    val amount: Double,
)

object EconomyBridge {

    private val providers = linkedMapOf<String, EconomyProvider>()

    private var defaultProviderId: String = "auto"
    private var defaultCurrencyId: String = "money"

    fun init(plugin: JavaPlugin) {
        clear()
        registerOptional(plugin, "Vault") { VaultEconomyProvider(plugin) }
        registerOptional(plugin, "ExcellentEconomy") { ExcellentEconomyProvider(plugin) }
        registerOptional(plugin, "PlayerPoints") { PlayerPointsEconomyProvider(plugin) }
        reloadFromConfig()
        logStatus()
    }

    fun reloadFromConfig() {
        val conf = QinhCoreLib.instance.config
        defaultProviderId = conf.getString("economy.default-provider", "auto")?.lowercase(Locale.ROOT) ?: "auto"
        defaultCurrencyId = conf.getString("economy.default-currency", "money")?.lowercase(Locale.ROOT) ?: "money"
    }

    fun clear() {
        providers.clear()
    }

    fun registerProvider(provider: EconomyProvider) {
        providers[normalizeId(provider.id)] = provider
        BridgeStatusRegistry.register(
            BridgeStatus(
                name = provider.id,
                available = provider.isAvailable(),
                enabled = provider.isAvailable(),
                source = "EconomyProvider",
                message = if (provider.isAvailable()) Lang.get("economy-bridge.backend-available") else Lang.get("economy-bridge.backend-unavailable"),
                recoverable = true,
            ),
        )
    }

    private fun registerOptional(plugin: JavaPlugin, pluginName: String, factory: () -> EconomyProvider) {
        val dependency = plugin.server.pluginManager.getPlugin(pluginName) ?: return
        if (!dependency.isEnabled) return
        try {
            registerProvider(factory())
        } catch (t: Throwable) {
            plugin.logger.warning(Lang.get("economy-bridge.register-failed", "plugin" to pluginName, "error" to t.message))
        }
    }

    fun providerIds(): List<String> = providers.keys.sorted()

    fun availableProviderIds(): List<String> =
        providers.values.filter { it.isAvailable() }.map { it.id }.sorted()

    fun getProvider(id: String?): EconomyProvider? {
        if (id.isNullOrBlank()) return null
        return providers[normalizeId(id)]
    }

    fun bridgeStatuses(): List<BridgeStatus> = providers.values.map { provider ->
        BridgeStatus(
            name = economyDisplayName(provider.id),
            available = provider.isAvailable(),
            enabled = provider.isAvailable(),
            source = "Economy",
            message = if (provider.isAvailable()) Lang.get("common.available") else Lang.get("common.unavailable"),
            recoverable = true,
        )
    }

    fun selectProvider(providerId: String?, currencyId: String?): EconomyProvider? {
        val normalized = normalizeId(providerId ?: defaultProviderId)
        if (normalized.isBlank() || normalized == "auto") {
            if (!currencyId.isNullOrBlank()) {
                val ee = getProvider("excellenteconomy")
                if (ee != null && ee.isAvailable()) return ee
            }
            val vault = getProvider("vault")
            if (vault != null && vault.isAvailable()) return vault
            val ee = getProvider("excellenteconomy")
            if (ee != null && ee.isAvailable()) return ee
            val pp = getProvider("playerpoints")
            if (pp != null && pp.isAvailable()) return pp
            return null
        }
        val provider = getProvider(normalized) ?: return null
        return if (provider.isAvailable()) provider else null
    }

    fun isAvailable(): Boolean = availableProviderIds().isNotEmpty()

    fun getActiveProvider(): EconomyProvider? = selectProvider(defaultProviderId, defaultCurrencyId)

    fun getDefaultProviderId(): String = defaultProviderId

    fun getDefaultCurrencyId(): String = defaultCurrencyId

    fun resolveCurrencyId(explicit: String?): String? {
        return explicit?.takeIf { it.isNotBlank() } ?: defaultCurrencyId.takeIf { it.isNotBlank() }
    }

    fun getBalance(
        player: OfflinePlayer,
        providerId: String? = null,
        currencyId: String? = null,
    ): Double {
        val provider = selectProvider(providerId, resolveCurrencyId(currencyId)) ?: return 0.0
        val currency = effectiveCurrency(provider, currencyId)
        return provider.getBalance(player, currency)
    }

    fun has(
        player: OfflinePlayer,
        amount: Double,
        providerId: String? = null,
        currencyId: String? = null,
    ): Boolean {
        val provider = selectProvider(providerId, resolveCurrencyId(currencyId)) ?: return false
        val currency = effectiveCurrency(provider, currencyId)
        return provider.has(player, amount, currency)
    }

    fun deposit(
        player: OfflinePlayer,
        amount: Double,
        providerId: String? = null,
        currencyId: String? = null,
    ): EconomyTransactionResult {
        val provider = selectProvider(providerId, resolveCurrencyId(currencyId))
            ?: return EconomyTransactionResult.fail(Lang.get("economy-bridge.no-provider"), code = "NO_PROVIDER", suggestion = Lang.get("economy-bridge.no-provider-suggestion"), provider = "economy")
        val currency = effectiveCurrency(provider, currencyId)
        if (provider.id == "excellenteconomy" && currency.isNullOrBlank()) {
            return EconomyTransactionResult.fail(Lang.get("economy-bridge.currency-required"), code = "CURRENCY_REQUIRED", suggestion = Lang.get("economy-bridge.currency-required-suggestion"), provider = provider.id)
        }
        return provider.deposit(player, amount, currency).copy(provider = provider.id)
    }

    fun withdraw(
        player: OfflinePlayer,
        amount: Double,
        providerId: String? = null,
        currencyId: String? = null,
    ): EconomyTransactionResult {
        val provider = selectProvider(providerId, resolveCurrencyId(currencyId))
            ?: return EconomyTransactionResult.fail(Lang.get("economy-bridge.no-provider"), code = "NO_PROVIDER", suggestion = Lang.get("economy-bridge.no-provider-suggestion"), provider = "economy")
        val currency = effectiveCurrency(provider, currencyId)
        if (provider.id == "excellenteconomy" && currency.isNullOrBlank()) {
            return EconomyTransactionResult.fail(Lang.get("economy-bridge.currency-required"), code = "CURRENCY_REQUIRED", suggestion = Lang.get("economy-bridge.currency-required-suggestion"), provider = provider.id)
        }
        return provider.withdraw(player, amount, currency).copy(provider = provider.id)
    }

    fun setBalance(
        player: OfflinePlayer,
        amount: Double,
        providerId: String? = null,
        currencyId: String? = null,
    ): EconomyTransactionResult {
        val provider = selectProvider(providerId, resolveCurrencyId(currencyId))
            ?: return EconomyTransactionResult.fail(Lang.get("economy-bridge.no-provider"), code = "NO_PROVIDER", suggestion = Lang.get("economy-bridge.no-provider-suggestion"), provider = "economy")
        val currency = effectiveCurrency(provider, currencyId)
        if (provider.id == "excellenteconomy" && currency.isNullOrBlank()) {
            return EconomyTransactionResult.fail(Lang.get("economy-bridge.currency-required"), code = "CURRENCY_REQUIRED", suggestion = Lang.get("economy-bridge.currency-required-suggestion"), provider = provider.id)
        }
        return provider.setBalance(player, amount, currency).copy(provider = provider.id)
    }

    fun parseMoneyRequirement(value: String): MoneyRequirement? {
        if (value.isBlank()) return null

        var providerId: String? = null
        var currencyId: String? = null
        var numericPart = value.trim()

        val segments = numericPart.split(":").filter { it.isNotBlank() }
        if (segments.isNotEmpty()) {
            val first = normalizeId(segments[0])
            when (first) {
                "auto", "vault", "excellenteconomy" -> {
                    providerId = first
                    numericPart = segments.drop(1).joinToString(":")
                    if (segments.size >= 2) {
                        val second = segments[1].trim()
                        if (!looksLikeComparison(second)) {
                            currencyId = second.lowercase(Locale.ROOT)
                            numericPart = segments.drop(2).joinToString(":")
                        }
                    }
                }
                else -> {
                    if (!looksLikeComparison(first) && segments.size >= 2) {
                        currencyId = first.lowercase(Locale.ROOT)
                        numericPart = segments.drop(1).joinToString(":")
                    }
                }
            }
        }

        val (operator, amount) = parseComparison(numericPart) ?: return null
        return MoneyRequirement(providerId, currencyId, operator, amount)
    }

    fun diagnose(): DiagnosticResult<List<BridgeStatus>> =
        DiagnosticResult.ok(bridgeStatuses(), Lang.get("economy-bridge.diagnose-complete"), source = "economy")

    private fun effectiveCurrency(provider: EconomyProvider, currencyId: String?): String? {
        val resolved = resolveCurrencyId(currencyId)
        return when (provider.id) {
            "vault", "playerpoints" -> null
            else -> resolved
        }
    }

    private fun parseComparison(value: String): Pair<String, Double>? {
        val trimmed = value.trim()
        val operator = when {
            trimmed.startsWith(">=") -> ">="
            trimmed.startsWith("<=") -> "<="
            trimmed.startsWith("==") -> "=="
            trimmed.startsWith(">") -> ">"
            trimmed.startsWith("<") -> "<"
            else -> ">="
        }
        val amountStr = trimmed.removePrefix(">=").removePrefix("<=").removePrefix("==")
            .removePrefix(">").removePrefix("<").trim()
        val amount = amountStr.toDoubleOrNull() ?: return null
        return operator to amount
    }

    private fun looksLikeComparison(segment: String): Boolean {
        val s = segment.trim()
        return s.startsWith(">") || s.startsWith("<") || s.startsWith("=") || s.toDoubleOrNull() != null
    }

    private fun normalizeId(id: String): String = when (id.lowercase(Locale.ROOT)) {
        "ee" -> "excellenteconomy"
        else -> id.lowercase(Locale.ROOT)
    }

    private fun logStatus() {
        availableProviderIds().forEach { id ->
            StartupReporter.hookedEconomy(economyDisplayName(id))
        }
    }

    private fun economyDisplayName(id: String): String = when (id.lowercase(Locale.ROOT)) {
        "vault" -> "Vault"
        "excellenteconomy" -> "ExcellentEconomy"
        "playerpoints" -> "PlayerPoints"
        else -> id.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }
}
